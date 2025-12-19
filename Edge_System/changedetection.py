import os
import cv2
import pathlib
import requests
from datetime import datetime
from hand_detection import HandDetector

class ChangeDetection:
    result_prev = []
    HOST = 'http://127.0.0.1:8000'
    username = 'jimin'
    password = 'Q@12121212'
    token = ''
    title = ''
    text = ''
    detection_count = {}
    
    # ═══════════════════════════════════════════════
    # 🎥 카메라 모드 설정 (여기만 바꾸면 됨!)
    # ═══════════════════════════════════════════════
    CAMERA_MODE = "entrance_inside"  # "entrance_outside", "entrance_inside", "counter"
    
    # entrance_outside: 문 밖 카메라 (배달원 감지)
    #   - person + motorcycle 감지 → 배달원
    #   - x축 이동 감지 (왼→오 = 입장)
    #   - 배달원 입장 알림
    
    # entrance_inside: 문 안 카메라 (입장/퇴장 감지)
    #   - x축 이동 감지 (왼→오 = 입장, 오→왼 = 퇴장)
    #   - 일반 손님 입장 알림
    #   - 퇴장은 통계만 기록
    
    # counter: 카운터 카메라 (손님 호출 감지)
    #   - 5프레임 감지 → 15프레임 체류 → 손 들기 감지
    #   - MediaPipe로 손 들기 감지
    #   - 손님 호출 알림
    # ═══════════════════════════════════════════════
    
    # 필터링할 객체 목록
    FILTER_OBJECTS = ['person', 'motorcycle', 'motorbike', 'bicycle', 'chair']
    DELIVERY_VEHICLES = ['motorcycle', 'motorbike', 'chair']  # 테스트: chair 추가!
    
    # 임계값 설정
    DETECTION_THRESHOLD = 5       # 사람 감지 임계값 (1초)
    STAY_TIME_THRESHOLD = 15      # 체류 시간 체크 (3초 = 15프레임)
    HAND_RAISE_THRESHOLD = 5      # 손 들기 감지 (1초 = 5프레임)
    
    # 방향 감지 설정
    DIRECTION_AXIS = 'x'          # 'x' 축 사용 (왼→오 = 입장)
    DIRECTION_THRESHOLD = 30      # 30픽셀 이상 이동해야 방향 인식
    
    consecutive_detections = {}
    already_posted = {}
    
    # 사람별 추적 정보 (counter 모드에서만 사용)
    person_tracking = {}
    
    # 위치 추적 (entrance 모드에서 방향 감지용)
    person_positions = {}
    
    # 배달원 상태 추적 (entrance_outside 모드)
    delivery_state = {
        'detected': False,           # person + motorcycle 감지됨
        'person_last_x': None,       # person의 마지막 x 좌표
        'motorcycle_present': False, # motorcycle 여전히 있음
        'frames_since_detected': 0   # 배달원 감지 후 경과 프레임
    }
    
    # MediaPipe HandDetector (counter 모드에서만 사용)
    hand_detector = None
    
    def __init__(self, names):
        self.result_prev = [0 for i in range(len(names))]
        
        for name in names:
            self.consecutive_detections[name] = 0
            self.already_posted[name] = False
            self.person_tracking[name] = {
                'stay_frames': 0,
                'hand_raised_frames': 0,
                'checking_hand': False
            }
        
        # 위치 추적 초기화
        self.person_positions = {}
        
        # 배달원 상태 초기화
        self.delivery_state = {
            'detected': False,
            'person_last_x': None,
            'motorcycle_present': False,
            'frames_since_detected': 0
        }
        
        # counter 모드일 때만 MediaPipe 초기화
        if self.CAMERA_MODE == "counter":
            self.hand_detector = HandDetector()
            print("🙌 MediaPipe Hand Detector initialized (Counter Mode)")
        elif self.CAMERA_MODE == "entrance_outside":
            print("🏍️ Entrance Outside Mode - Delivery Detection")
        elif self.CAMERA_MODE == "entrance_inside":
            print("🚪 Entrance Inside Mode - Entry/Exit Detection")
        else:
            print(f"⚠️ Unknown mode: {self.CAMERA_MODE}")
        
        try:
            res = requests.post(
                self.HOST + '/api-token-auth/',
                json={
                    'username': self.username,
                    'password': self.password,
                },
                timeout=5
            )
            res.raise_for_status()
            self.token = res.json()['token']
            print(f"✅ Token obtained: {self.token}")
        except Exception as e:
            print(f"⚠️ API 토큰 요청 실패: {e}")
            print(f"⚠️ 알림 기능 없이 계속 진행합니다...")
            self.token = ''
        
        print(f"🎥 Camera Mode: {self.CAMERA_MODE.upper()}")
        print(f"🔍 Filtering objects: {self.FILTER_OBJECTS}")
        print(f"🎯 Detection threshold: {self.DETECTION_THRESHOLD}프레임")
        
        if self.CAMERA_MODE == "counter":
            print(f"⏱️  Stay time threshold: {self.STAY_TIME_THRESHOLD}프레임")
            print(f"🙋 Hand raise threshold: {self.HAND_RAISE_THRESHOLD}프레임")
        elif self.CAMERA_MODE in ["entrance_outside", "entrance_inside"]:
            print(f"➡️  Direction threshold: {self.DIRECTION_THRESHOLD}px (x-axis)")
    
    def detect_delivery_person(self, detected_names):
        """
        배달원 감지: person + 배달 수단(motorcycle, bicycle 등)
        
        Args:
            detected_names: 현재 프레임에서 감지된 객체 이름 리스트
        
        Returns:
            bool: 배달원으로 판정되면 True
        """
        has_person = 'person' in detected_names
        has_vehicle = any(vehicle in detected_names for vehicle in self.DELIVERY_VEHICLES)
        return has_person and has_vehicle
    
    def detect_direction(self, obj_name, bbox):
        """
        객체의 이동 방향 감지 (x축 기준)
        
        Args:
            obj_name: 객체 이름 (예: 'person', 'delivery')
            bbox: [x1, y1, x2, y2] 바운딩 박스
        
        Returns:
            str: "entering" (왼→오), "leaving" (오→왼), None (이동 불충분)
        """
        if bbox is None or len(bbox) < 4:
            return None
        
        # bbox 중심 x좌표 계산
        center_x = (bbox[0] + bbox[2]) / 2
        
        # 첫 감지 시 저장만
        if obj_name not in self.person_positions:
            self.person_positions[obj_name] = center_x
            return None
        
        # 이전 위치와 비교
        prev_x = self.person_positions[obj_name]
        movement = center_x - prev_x
        
        # 이동 거리가 임계값 이상일 때만 방향 판정
        direction = None
        if abs(movement) > self.DIRECTION_THRESHOLD:
            if movement > 0:
                direction = "entering"  # x 증가 = 왼→오 = 입장
            else:
                direction = "leaving"   # x 감소 = 오→왼 = 퇴장
        
        # 현재 위치 업데이트
        self.person_positions[obj_name] = center_x
        
        return direction

    def add(self, names, detected_current, save_dir, image, bboxes=None):
        """
        객체 감지 결과 처리 및 알림
        
        Args:
            names: 객체 이름 리스트
            detected_current: 현재 프레임 감지 결과 [0 or 1]
            save_dir: 저장 디렉토리
            image: 현재 프레임 이미지
            bboxes: 바운딩 박스 리스트 [[x1, y1, x2, y2], ...] (방향 감지용)
        """
        self.title = ''
        self.text = ''
        change_flag = 0
        customer_type = 'new'  # 기본값


        # ═══════════════════════════════════════════════
        # 🎨 MediaPipe 디버깅 시각화 (비활성화)
        # ═══════════════════════════════════════════════
        # 시각화 없이 손 들기 감지만 수행 (성능 향상)
        # if self.CAMERA_MODE == "counter":
        #     if self.hand_detector is not None:
        #         try:
        #             hand_raised, visualized = self.hand_detector.detect_hand_raised(
        #                 image.copy(), 
        #                 visualize=True
        #             )
        #             cv2.imshow('MediaPipe Debug', visualized)
        #             cv2.waitKey(1)
        #         except Exception as e:
        #             print(f"⚠️ 시각화 에러: {e}")
        # ═══════════════════════════════════════════════



        
        for i in range(len(detected_current)):
            obj_name = names[i]
            
            if obj_name not in self.FILTER_OBJECTS:
                continue
            
            # 딕셔너리 초기화 확인
            if obj_name not in self.consecutive_detections:
                self.consecutive_detections[obj_name] = 0
                self.already_posted[obj_name] = False
                self.person_tracking[obj_name] = {
                    'stay_frames': 0,
                    'hand_raised_frames': 0,
                    'checking_hand': False
                }
            
            # ═══════════════════════════════════════════
            # 현재 프레임에서 감지됨
            # ═══════════════════════════════════════════
            if detected_current[i] == 1:
                self.consecutive_detections[obj_name] += 1
                
                # ─────────────────────────────────────────
                # 🏍️ ENTRANCE OUTSIDE MODE (문 밖 카메라 - 배달원 감지)
                # ─────────────────────────────────────────
                if self.CAMERA_MODE == "entrance_outside":
                    
                    # 현재 프레임에서 감지된 객체 확인
                    detected_names = [names[j] for j, v in enumerate(detected_current) if v == 1]
                    has_person = 'person' in detected_names
                    has_motorcycle = any(vehicle in detected_names for vehicle in self.DELIVERY_VEHICLES)
                    
                    # ───── Step 1: person + motorcycle 함께 감지 ─────
                    if has_person and has_motorcycle:
                        
                        if not self.delivery_state['detected']:
                            print("🏍️ 배달원 감지! (person + motorcycle) [Outside]")
                        
                        self.delivery_state['detected'] = True
                        self.delivery_state['motorcycle_present'] = True
                        self.delivery_state['frames_since_detected'] += 1
                        
                        # person bbox 찾아서 x 좌표 기록
                        if bboxes is not None and 'person' in bboxes and len(bboxes['person']) > 0:
                            person_bbox = bboxes['person'][0]  # 첫 번째 person
                            self.delivery_state['person_last_x'] = (person_bbox[0] + person_bbox[2]) / 2
                            
                            print(f"🏍️ 배달원 대기 중... (프레임: {self.delivery_state['frames_since_detected']}, "
                                  f"person x: {self.delivery_state['person_last_x']:.0f}) [Outside]")
                        else:
                            print(f"🏍️ 배달원 대기 중... (프레임: {self.delivery_state['frames_since_detected']}) [Outside]")
                    
                    # ───── Step 2: motorcycle만 남음 (person 사라짐) ─────
                    elif not has_person and has_motorcycle:
                        
                        if self.delivery_state['detected']:
                            # person이 오른쪽으로 사라졌는지 확인 (화면 60% 이상 위치)
                            if self.delivery_state['person_last_x'] is not None and \
                               self.delivery_state['person_last_x'] > image.shape[1] * 0.6:
                                
                                if not self.already_posted.get('delivery', False):
                                    change_flag = 1
                                    customer_type = 'delivery'
                                    self.title = "배달원"
                                    self.text = "배달원 입장!"
                                    self.already_posted['delivery'] = True
                                    print(f"🏍️ 배달원 입장 확정! (person 사라짐 x={self.delivery_state['person_last_x']:.0f}, "
                                          f"motorcycle 남음) [Outside]")
                            elif self.delivery_state['person_last_x'] is not None:
                                print(f"⚠️ person 사라졌지만 왼쪽으로 이동 (x={self.delivery_state['person_last_x']:.0f}) [Outside]")
                            else:
                                print(f"⚠️ person 사라졌지만 위치 정보 없음 [Outside]")
                            
                            # 상태 리셋
                            self.delivery_state = {
                                'detected': False,
                                'person_last_x': None,
                                'motorcycle_present': False,
                                'frames_since_detected': 0
                            }
                    
                    # ───── Step 3: 완전 리셋 (아무것도 없음) ─────
                    elif not has_motorcycle and self.delivery_state['detected']:
                        print("🔄 배달원 상태 리셋 (motorcycle 사라짐) [Outside]")
                        self.delivery_state = {
                            'detected': False,
                            'person_last_x': None,
                            'motorcycle_present': False,
                            'frames_since_detected': 0
                        }
                    
                    # 감지 중 표시 (일반 객체)
                    if self.consecutive_detections[obj_name] < self.DETECTION_THRESHOLD:
                        if not (has_person and has_motorcycle):  # 배달원 감지 중이 아닐 때만
                            print(f"📈 {obj_name} 감지 중... ({self.consecutive_detections[obj_name]}/{self.DETECTION_THRESHOLD}) [Outside]")
                
                # ─────────────────────────────────────────
                # 🚪 ENTRANCE INSIDE MODE (문 안 카메라 - 입장/퇴장)
                # ─────────────────────────────────────────
                elif self.CAMERA_MODE == "entrance_inside":
                    
                    # person만 처리
                    if obj_name == 'person':
                        
                        # 감지 중 표시
                        if self.consecutive_detections[obj_name] < self.DETECTION_THRESHOLD:
                            print(f"📈 {obj_name} 감지 중... ({self.consecutive_detections[obj_name]}/{self.DETECTION_THRESHOLD}) [Inside]")
                        
                        # 5프레임 도달 시 방향 감지
                        elif self.consecutive_detections[obj_name] >= self.DETECTION_THRESHOLD:
                            
                            # bbox로 방향 감지
                            if bboxes is not None and 'person' in bboxes and len(bboxes['person']) > 0:
                                person_bbox = bboxes['person'][0]  # 첫 번째 person
                                direction = self.detect_direction('person', person_bbox)
                                
                                # 입장 (왼→오)
                                if direction == "entering":
                                    if not self.already_posted.get('person', False):
                                        change_flag = 1
                                        customer_type = 'new'
                                        self.title = "손님"
                                        self.text = "새로운 손님 입장!"
                                        self.already_posted['person'] = True
                                        print(f"👤 새 손님 입장! (x축 이동 감지) [Inside]")
                                
                                # 퇴장 (오→왼)
                                elif direction == "leaving":
                                    print(f"🚪 {obj_name} 퇴장 감지 (통계만 기록) [Inside]")
                                    # already_posted 리셋 (다음 입장을 위해)
                                    self.already_posted = {}
                                    self.person_positions = {}
                
                # ─────────────────────────────────────────
                # 🍽️ COUNTER MODE (카운터 카메라)
                # ─────────────────────────────────────────
                elif self.CAMERA_MODE == "counter":
                    
                    # 임계값 도달
                    if self.consecutive_detections[obj_name] >= self.DETECTION_THRESHOLD:
                        
                        # 아직 포스팅 안 했으면
                        if not self.already_posted[obj_name]:
                            
                            # 체류 시간 증가
                            self.person_tracking[obj_name]['stay_frames'] += 1
                            
                            # 손 체크 시작 조건: 일정 시간 이상 머뭄
                            if self.person_tracking[obj_name]['stay_frames'] >= self.STAY_TIME_THRESHOLD:
                                self.person_tracking[obj_name]['checking_hand'] = True
                            
                            # 손 체크 중이면 MediaPipe 실행
                            if self.person_tracking[obj_name]['checking_hand']:
                                hand_raised, _ = self.hand_detector.detect_hand_raised(image, visualize=False)
                                
                                if hand_raised:
                                    self.person_tracking[obj_name]['hand_raised_frames'] += 1
                                    print(f"🙋 {obj_name} 손 들기 감지 중... ({self.person_tracking[obj_name]['hand_raised_frames']}/{self.HAND_RAISE_THRESHOLD}) [Counter]")
                                    
                                    # 손 들기 임계값 도달 → 호출!
                                    if self.person_tracking[obj_name]['hand_raised_frames'] >= self.HAND_RAISE_THRESHOLD:
                                        change_flag = 1
                                        customer_type = 'call'
                                        self.title = obj_name
                                        self.text = "손님 호출!"
                                        self.already_posted[obj_name] = True
                                        print(f"🔔 {obj_name} 손님 호출! - 포스팅 진행 (Counter Mode)")
                                else:
                                    # 손 안 들었으면 리셋
                                    self.person_tracking[obj_name]['hand_raised_frames'] = 0
                            
                            # 일정 시간 지났는데도 손 안 들면 손 체크만 리셋
                            if (self.person_tracking[obj_name]['stay_frames'] > self.STAY_TIME_THRESHOLD + 10 
                                and self.person_tracking[obj_name]['hand_raised_frames'] < self.HAND_RAISE_THRESHOLD):
                                print(f"⏭️  {obj_name} 손 안 듦 - 다시 체크 시작 [Counter]")
                                
                                # Detection은 유지! (이미 person 확정됨, Detection 5프레임 낭비 방지)
                                # self.consecutive_detections[obj_name] = 0  ← 제거!
                                
                                # 손 체크만 리셋하고 즉시 재시작
                                self.person_tracking[obj_name]['stay_frames'] = self.STAY_TIME_THRESHOLD
                                self.person_tracking[obj_name]['hand_raised_frames'] = 0
                                self.person_tracking[obj_name]['checking_hand'] = True
                        
                    elif self.consecutive_detections[obj_name] < self.DETECTION_THRESHOLD:
                        print(f"📈 {obj_name} 감지 중... ({self.consecutive_detections[obj_name]}/{self.DETECTION_THRESHOLD}) [Counter]")
            
            # ═══════════════════════════════════════════
            # 현재 프레임에서 감지 안 됨
            # ═══════════════════════════════════════════
            else:
                if self.consecutive_detections[obj_name] > 0:
                    print(f"🔄 {obj_name} 감지 중단 - 카운트 리셋 [{self.CAMERA_MODE.title()}]")
                
                self.consecutive_detections[obj_name] = 0
                self.already_posted[obj_name] = False
                self.person_tracking[obj_name] = {
                    'stay_frames': 0,
                    'hand_raised_frames': 0,
                    'checking_hand': False
                }
        
        self.result_prev = detected_current[:]
        
        if change_flag == 1:
            if self.title not in self.detection_count:
                self.detection_count[self.title] = 0
            self.detection_count[self.title] += 1
            
            print("\n" + "="*50)
            print(f"📊 Detection Statistics ({self.CAMERA_MODE.upper()} MODE):")
            print("="*50)
            for obj, count in sorted(self.detection_count.items()):
                print(f"   {obj}: {count}회")
            print("="*50 + "\n")
            
            self.send(save_dir, image, customer_type)

    def send(self, save_dir, image, customer_type='new'):
        now = datetime.now()
        
        today = datetime.now()
        save_path = pathlib.Path(os.getcwd()) / save_dir / 'detected' / str(today.year) / str(today.month) / str(today.day)
        pathlib.Path(save_path).mkdir(parents=True, exist_ok=True)
        full_path = save_path / '{0}-{1}-{2}-{3}.jpg'.format(today.hour, today.minute, today.second, today.microsecond)
        
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)
        
        print(f"📸 이미지 저장됨: {full_path}")
        
        # 토큰이 없으면 로컬 저장만 하고 종료
        if not self.token:
            print(f"⚠️ API 토큰 없음 - 로컬 저장만 완료 (Type: {customer_type})")
            return
        
        headers = {
            'Authorization': 'Token ' + self.token
        }
        
        data = {
            'title': self.title,
            'text': self.text,
            'author': '1',
            'customer_type': customer_type
        }
        
        files = {
            'image': open(str(full_path), 'rb')
        }
        
        try:
            res = requests.post(
                self.HOST + '/api_root/Post/',
                data=data,
                files=files,
                headers=headers
            )
            print(f"Post response: {res.status_code}")
            
            if res.status_code == 201:
                print(f"✅ Successfully posted: {self.title} (Type: {customer_type}) [{self.CAMERA_MODE.upper()}]")
                
                notification_data = {
                    'title': self.title,
                    'text': self.text,
                    'customer_type': customer_type
                }
                
                fcm_res = requests.post(
                    self.HOST + '/api/send-notification/',
                    json=notification_data,
                    headers=headers
                )
                
                if fcm_res.status_code == 200:
                    fcm_result = fcm_res.json()
                    print(f"🔔 FCM 전송 성공: {fcm_result['success_count']}개 디바이스")
                else:
                    print(f"⚠️  FCM 전송 실패: {fcm_res.text}")
            else:
                print(f"❌ Error: {res.text}")
                
        except Exception as e:
            print(f"❌ Exception: {e}")
        finally:
            files['image'].close()
    
    # 소멸자: MediaPipe 리소스 정리
    def __del__(self):
        if self.hand_detector:
            self.hand_detector.close()