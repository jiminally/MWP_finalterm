"""
MediaPipe Pose를 이용한 손 제스처 감지 모듈
- 손이 어깨 위로 올라가는 제스처 인식
- counter 모드에서만 사용
"""

import cv2
import numpy as np

# MediaPipe import 시도 (최신 버전 호환)
MEDIAPIPE_AVAILABLE = False
mp_pose = None
mp_hands = None
mp_drawing = None
mp_drawing_styles = None

import_error_messages = []

try:
    # 방법 1: 최신 MediaPipe 직접 경로 import (가장 안정적)
    from mediapipe.python.solutions import pose as mp_pose
    from mediapipe.python.solutions import hands as mp_hands
    from mediapipe.python.solutions import drawing_utils as mp_drawing
    from mediapipe.python.solutions import drawing_styles as mp_drawing_styles
    MEDIAPIPE_AVAILABLE = True
    print("✅ MediaPipe imported (direct path method)")
except Exception as e:
    import_error_messages.append(f"Method 1 (direct path): {type(e).__name__}: {e}")
    try:
        # 방법 2: 최신 MediaPipe solutions import 방식
        from mediapipe import solutions
        mp_pose = solutions.pose
        mp_hands = solutions.hands
        mp_drawing = solutions.drawing_utils
        mp_drawing_styles = solutions.drawing_styles
        MEDIAPIPE_AVAILABLE = True
        print("✅ MediaPipe imported (solutions import method)")
    except Exception as e:
        import_error_messages.append(f"Method 2 (solutions): {type(e).__name__}: {e}")
        try:
            # 방법 3: 표준 mediapipe as mp 방식
            import mediapipe as mp
            if hasattr(mp, 'solutions'):
                mp_pose = mp.solutions.pose
                mp_hands = mp.solutions.hands
                mp_drawing = mp.solutions.drawing_utils
                mp_drawing_styles = mp.solutions.drawing_styles
                MEDIAPIPE_AVAILABLE = True
                print("✅ MediaPipe imported (mp.solutions method)")
            else:
                # mp는 있지만 solutions가 없는 경우 - 구조 확인
                print(f"⚠️ mediapipe 모듈은 있지만 solutions 속성이 없습니다.")
                print(f"   mediapipe 속성: {[x for x in dir(mp) if not x.startswith('_')][:10]}")
                raise AttributeError("mp.solutions not available")
        except Exception as e:
            import_error_messages.append(f"Method 3 (mp.solutions): {type(e).__name__}: {e}")
            MEDIAPIPE_AVAILABLE = False
            print("⚠️ MediaPipe import failed - 모든 방법 실패")
            print("   시도한 방법들:")
            for msg in import_error_messages:
                print(f"   - {msg}")
            print("\n   ⚠️ 중요: MediaPipe 최신 버전(0.11+)은 구조가 변경되었습니다!")
            print("   현재 설치된 버전은 'tasks' API만 지원합니다.")
            print("\n   해결 방법 (구버전 설치 - 권장):")
            print("   1. pip uninstall mediapipe -y")
            print("   2. pip install mediapipe==0.10.8")
            print("\n   또는 최신 버전 유지 시 코드를 tasks API로 재작성 필요")

class HandDetector:
    """손 제스처 감지 클래스 (Pose + Hands)"""
    
    def __init__(self):
        """MediaPipe Pose와 Hands 초기화"""
        if not MEDIAPIPE_AVAILABLE:
            print("❌ MediaPipe not available - hand detection disabled")
            self.pose = None
            self.hands = None
            return
            
        try:
            # Pose 초기화
            self.pose = mp_pose.Pose(
                static_image_mode=False,
                model_complexity=1,
                min_detection_confidence=0.5,
                min_tracking_confidence=0.5
            )
            
            # Hands 초기화
            self.hands = mp_hands.Hands(
                static_image_mode=False,
                max_num_hands=2,
                min_detection_confidence=0.5,
                min_tracking_confidence=0.5
            )
            
            print("✅ MediaPipe Pose + Hands initialized")
        except Exception as e:
            print(f"❌ MediaPipe initialization failed: {e}")
            self.pose = None
            self.hands = None
        
    def detect_hand_raised(self, image, visualize=True):
        """
        손 들기 제스처 감지
        
        판정 기준: 중지 끝(Hands landmark 12)이 어깨(Pose landmark 11 or 12)보다 위에 있으면 손 들기
        
        Args:
            image: BGR 이미지
            visualize: True면 랜드마크 그리기
        Returns:
            tuple: (bool: 손을 들었으면 True, image: 랜드마크 그려진 이미지)
        """
        if self.pose is None or self.hands is None:
            return False, image
            
        try:
            # BGR → RGB 변환
            image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
            image_height, image_width = image.shape[:2]
            
            # 시각화를 위해 이미지 복사
            annotated_image = image.copy()
            
            # ──────────────────────────────────────
            # 1. Pose로 어깨 위치 찾기
            # ──────────────────────────────────────
            pose_results = self.pose.process(image_rgb)
            
            if not pose_results.pose_landmarks:
                return False, image
            
            # Pose 랜드마크 그리기
            if visualize:
                mp_drawing.draw_landmarks(
                    annotated_image,
                    pose_results.pose_landmarks,
                    mp_pose.POSE_CONNECTIONS,
                    landmark_drawing_spec=mp_drawing_styles.get_default_pose_landmarks_style()
                )
            
            # 왼쪽 어깨(11), 오른쪽 어깨(12)
            left_shoulder = pose_results.pose_landmarks.landmark[11]
            right_shoulder = pose_results.pose_landmarks.landmark[12]
            
            # 어깨 평균 y좌표 (픽셀)
            shoulder_y = ((left_shoulder.y + right_shoulder.y) / 2) * image_height
            shoulder_x = ((left_shoulder.x + right_shoulder.x) / 2) * image_width
            
            # 어깨 라인 그리기 (빨간 선)
            if visualize:
                cv2.line(annotated_image, 
                         (0, int(shoulder_y)), 
                         (image_width, int(shoulder_y)), 
                         (0, 0, 255), 2)
                cv2.putText(annotated_image, 
                           f"Shoulder: {shoulder_y:.0f}", 
                           (10, int(shoulder_y) - 10),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
            
            # ──────────────────────────────────────
            # 2. Hands로 손 위치 찾기
            # ──────────────────────────────────────
            hands_results = self.hands.process(image_rgb)
            
            if not hands_results.multi_hand_landmarks:
                # Pose만 있을 때도 이미지 반환
                return False, annotated_image if visualize else image
            
            # Hands 랜드마크 그리기
            if visualize:
                for hand_landmarks in hands_results.multi_hand_landmarks:
                    mp_drawing.draw_landmarks(
                        annotated_image,
                        hand_landmarks,
                        mp_hands.HAND_CONNECTIONS,
                        mp_drawing_styles.get_default_hand_landmarks_style(),
                        mp_drawing_styles.get_default_hand_connections_style()
                    )
            
            # 각 손 체크
            hand_raised = False
            for hand_landmarks in hands_results.multi_hand_landmarks:
                # 중지 끝 (landmark 12)
                middle_finger_tip = hand_landmarks.landmark[12]
                middle_y = middle_finger_tip.y * image_height
                middle_x = middle_finger_tip.x * image_width
                
                # 중지 위치 표시 (초록 원)
                if visualize:
                    cv2.circle(annotated_image, 
                              (int(middle_x), int(middle_y)), 
                              10, (0, 255, 0), -1)
                    cv2.putText(annotated_image, 
                               f"Finger: {middle_y:.0f}", 
                               (int(middle_x) + 15, int(middle_y)),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
                
                # 디버깅 출력
                print(f"🖐️  어깨: {shoulder_y:.1f}, 중지: {middle_y:.1f}, 차이: {shoulder_y - middle_y:.1f}")
                
                # ──────────────────────────────────────
                # 3. 판정: 중지가 어깨보다 위에?
                # ──────────────────────────────────────
                if middle_y < shoulder_y:
                    print(f"✅ 손 들기 감지! (중지가 어깨보다 {shoulder_y - middle_y:.1f}픽셀 위)")
                    hand_raised = True
                    
                    # 판정 결과 표시 (큰 텍스트)
                    if visualize:
                        cv2.putText(annotated_image, 
                                   "HAND RAISED!", 
                                   (50, 50),
                                   cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 255, 0), 3)
            
            return hand_raised, annotated_image if visualize else image
            
        except Exception as e:
            print(f"⚠️ Hand detection error: {e}")
            return False, image
    
    def close(self):
        """리소스 정리"""
        if self.pose is not None:
            try:
                self.pose.close()
            except:
                pass
        
        if self.hands is not None:
            try:
                self.hands.close()
            except:
                pass