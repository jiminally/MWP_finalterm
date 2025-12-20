# 🌌 Hear Here (조용한 카페)

>청각장애인 바리스타를 위한 AI 기반 실시간 손님 감지 & 알림 시스템

![Python](https://img.shields.io/badge/Python-3.12-blue?logo=python&logoColor=white)
![Django](https://img.shields.io/badge/Django-4.2-green?logo=django&logoColor=white)
![PyTorch](https://img.shields.io/badge/PyTorch-YOLOv5-red?logo=pytorch&logoColor=white)
![MediaPipe](https://img.shields.io/badge/MediaPipe-Pose_Hands-4285F4?logo=google&logoColor=white)
![Android](https://img.shields.io/badge/Android-SDK_34-green?logo=android&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FCM-orange?logo=firebase&logoColor=white)



## 📖 프로젝트 소개

**Hear Here**는 청각장애인 바리스타가 소리 없이도 카페에서 손님을 인지하고 응대할 수 있도록 돕는 시스템입니다.







### ✨ 핵심 가치
 
🎯 **시각 중심 설계** - 청각 정보를 명확한 시각 신호로 완벽 전환  
⚡ **실시간 감지** - YOLOv5 + MediaPipe로 1초 내 손님 인지  
📱 **즉각 알림** - Firebase FCM으로 어디서나 푸시 알림 수신  




## 🚀 주요 기능

### 1. AI 기반 3가지 손님 타입 자동 감지

| 타입 | 아이콘 | 감지 방식 | 긴급도 | 색상 |
|------|--------|----------|--------|------|
| **새로운 손님** | ⭐ | YOLOv5 person + X축 방향 감지 | 보통 | 골드 |
| **손님 호출** | 🔔 | MediaPipe 손 들기 제스처 감지 | 높음 | 빨강 + 펄스 |
| **배달원** | 📦 | YOLOv5 person + motorcycle 동시 감지 | 보통 | 파랑 |

#### ⭐ 새로운 손님 감지
- **카메라**: entrance_inside (현관 안쪽)
- **기술**: YOLOv5 person 감지 + X축 좌표 추적
- **판정**: 왼쪽 → 오른쪽 30px 이상 이동 시 입장
- **알림**: "⭐ 새로운 손님! · person · 방금 전"

#### 🔔 손님 호출 감지
- **카메라**: counter (카운터 앞)
- **기술**: YOLOv5 person (3초 체류) + MediaPipe Pose & Hands
- **판정**: 중지 Y좌표 < 어깨 Y좌표 (5프레임/1초 연속)
- **알림**: "🔔 손님 호출! · person · 방금 전" (긴급!)
- **특징**: 빨간 테두리 + 펄스 애니메이션

#### 📦 배달원 감지
- **카메라**: entrance_outside (현관 바깥)
- **기술**: YOLOv5 person + motorcycle 동시 감지
- **판정**: 화면 60% 이상 이동 후 사람 사라짐
- **알림**: "📦 배달원 입장 · 배달원 · 방금 전"



### 2. 실시간 푸시 알림 & 자동 새로고침

🔔 **Firebase Cloud Messaging (FCM)**
- Django 서버에서 손님 감지 시 자동으로 FCM 푸시 전송
- 앱이 꺼져 있어도 알림 수신 (백그라운드 알림)
- 타입별 알림 메시지 자동 생성

🔄 **자동 새로고침**
- 푸시 알림 수신 시 LocalBroadcast로 MainActivity에 전달
- `loadCustomers()` 자동 호출로 최신 손님 목록 갱신
- 사용자는 아무 조작 없이 최신 정보 확인





### 3. 편리한 관리 기능

✅ **응답 완료 처리**
- 상세 화면에서 "응답 완료" 버튼 클릭
- 카드가 회색으로 변경되어 구분
- "✓ 응답 완료" 뱃지 표시

🗑️ **손님 삭제**
- 상세 화면에서 개별 삭제 가능
- 서버 API와 연동 (DELETE /api_root/Post/<id>/)
- Token 인증으로 보안 유지

📅 **일괄 삭제**
- "어제 내역 일괄 삭제" 버튼
- 오늘 이전 날짜 기록 한 번에 정리
- 데이터베이스 효율적 관리



## 🛠️ 기술 스택

### Edge System (영상 처리)
- **Python 3.12** - 메인 언어
- **PyTorch** - 딥러닝 프레임워크
- **YOLOv5** - 실시간 객체 감지
- **MediaPipe** - 손 제스처 & 포즈 인식
- **OpenCV** - 영상 처리 및 카메라 입력

### Service System (백엔드)
- **Django 4.2** - 웹 프레임워크
- **Django REST Framework** - RESTful API
- **Firebase Admin SDK** - FCM 푸시 알림

### Client System (Android)
- **Java** - 메인 언어
- **Android SDK 34** - 최신 안드로이드
- **Firebase Cloud Messaging** - 푸시 알림



## 📦 설치 및 실행

### 사전 요구사항
- Python 3.12 이상
- Android Studio (또는 에뮬레이터)
- Firebase 프로젝트 (FCM 설정)
- USB 카메라 3대 (또는 RTSP 스트림)



### 1️⃣ Service System (Django) 설치

```bash
# 저장소 클론
git clone https://github.com/jiminally/MWP_finalterm.git
cd MWP_finalterm/Service_System

# 의존성 설치
pip install django djangorestframework firebase-admin pillow --break-system-packages

# 마이그레이션
python manage.py makemigrations
python manage.py migrate

# 슈퍼유저 생성
python manage.py createsuperuser

# Token 생성
python manage.py drf_create_token your-username
```

#### Firebase 설정
1. Firebase Console에서 프로젝트 생성
2. `서비스 계정 키` JSON 다운로드
3. `Service_System/` 폴더에 저장 (예: `firebase-key.json`)
4. `Service_System/mysite/settings.py` 수정:

```python
import firebase_admin
from firebase_admin import credentials

cred = credentials.Certificate('firebase-key.json')
firebase_admin.initialize_app(cred)
```

#### 실행
```bash
python manage.py runserver
```

⚠️ **중요**: 서버를 실행한 상태로 유지하세요. Edge System이 이 서버에 데이터를 전송합니다.
다른 터미널 창에서 Edge System을 실행하세요.

Django Admin 접속: `http://127.0.0.1:8000/admin`

---

### 2️⃣ Edge System 설치

```bash
cd ../Edge_System

# 가상환경 생성 (선택사항)
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 기본 의존성 설치
pip install torch torchvision opencv-python requests --break-system-packages

# MediaPipe 설치 (손 제스처 감지용)
# ⚠️ 중요: MediaPipe 0.11+ 버전은 API 구조가 변경되어 작동하지 않습니다.
# 반드시 0.10.x 버전을 설치하세요.
pip uninstall mediapipe -y  # 기존 버전 제거 (있는 경우)
pip install mediapipe==0.10.13 --break-system-packages

# YOLOv5 클론
git clone https://github.com/ultralytics/yolov5
cd yolov5
pip install -r requirements.txt --break-system-packages
```

#### 환경 변수 설정
`changedetection.py` 파일에서 Django 서버 URL 및 Token 설정:

```python
# changedetection.py (82줄)
self.url = "http://127.0.0.1:8000/api_root/Post/"
self.username = "your-username"
self.password = "your-password"
```

#### 실행
```bash
# 단일 카메라 모드 (데모용)
python changedetection.py

# 멀티 카메라 모드 (실제 배포)
# changedetection.py에서 CAMERA_MODE 주석 처리 후 실행
```

#### 카메라 모드 설정
```python
# 데모 모드 (단일 카메라)
CAMERA_MODE = 'entrance_outside'  # 또는 'entrance_inside', 'counter'

# 실제 모드 (카메라 3대)
# CAMERA_MODE 주석 처리하고 아래 활성화
cameras = {
    'entrance_outside': 0,  # USB 카메라 ID
    'entrance_inside': 1,
    'counter': 2
}
```

---

### 3️⃣ Client System (Android) 설치

```bash
cd ../Client_System
```

#### Android Studio에서 열기
1. Android Studio 실행
2. `Open Project` → `Client_System` 폴더 선택
3. Gradle 동기화 대기

#### Firebase 설정
1. Firebase Console에서 Android 앱 추가
2. `google-services.json` 다운로드
3. `app/` 폴더에 저장

#### API URL 설정
`MainActivity.java` (22줄):

```java
private static final String API_URL = "http://10.0.2.2:8000/api_root/Post/";  // 에뮬레이터
// private static final String API_URL = "http://192.168.x.x:8000/api_root/Post/";  // 실제 기기
```

#### Token 설정
`MainActivity.java` (23줄):

```java
private static final String API_TOKEN = "your-django-token-here";
```

#### 실행
1. 에뮬레이터 또는 실제 기기 연결
2. `Run` → `Run 'app'`



## 📂 프로젝트 구조

```
MWP_finalterm/
├── Edge_System/                 # AI 영상 처리 시스템
│   ├── yolov5/                 # YOLOv5 객체 감지
│   ├── changedetection.py      # 메인 감지 로직
│   ├── hand_detection.py       # MediaPipe 손 감지
│   └── test_*.py              # 테스트 스크립트
│
├── Service_System/             # Django 백엔드
│   ├── blog/                  # 메인 앱
│   │   ├── models.py         # Post, FCMToken 모델
│   │   ├── views.py          # API 뷰
│   │   ├── serializers.py    # DRF Serializer
│   │   └── templates/blog/   # 블로그 템플릿
│   ├── mysite/               # 프로젝트 설정
│   ├── media/                # 업로드된 이미지
│   └── db.sqlite3            # 데이터베이스
│
└── Client_System/             # Android 앱
    ├── app/src/main/
    │   ├── java/...MainActivity.java        # 메인 액티비티
    │   ├── java/...MyFirebaseMessagingService.java  # FCM 서비스
    │   ├── java/...StarryBackgroundView.java        # 별 배경
    │   ├── java/...FloatingAstronautView.java       # 우주비행사
    │   └── res/                # 리소스 (레이아웃, drawable)
    └── google-services.json    # Firebase 설정
```



## 🔑 주요 기능 사용법

### 1. 새로운 손님 감지 테스트 (entrance_inside)

1. Edge System에서 `CAMERA_MODE = 'entrance_inside'` 설정
2. `python changedetection.py` 실행
3. 카메라 앞에서 **왼쪽 → 오른쪽** 이동 (30px 이상)
4. 5프레임(1초) 연속 감지 시 Django로 POST 전송
5. Android 앱에 **골드 카드** + "⭐ 새로운 손님!" 알림



### 2. 손님 호출 감지 테스트 (counter)

1. Edge System에서 `CAMERA_MODE = 'counter'` 설정
2. `python changedetection.py` 실행
3. 카메라 앞에서 **3초 이상 체류**
4. **손을 어깨보다 높이 들기** (1초 이상 유지)
5. MediaPipe가 손 제스처 감지
6. Android 앱에 **빨간 카드 + 펄스** + "🔔 손님 호출!" 알림



### 3. 배달원 감지 테스트 (entrance_outside)

**실제 환경:**
1. `CAMERA_MODE = 'entrance_outside'` 설정
2. 오토바이와 사람이 동시에 화면에 나타남
3. 사람이 화면 60% 이상 이동 후 사라지면 배달원 판정

**데모 환경 (오토바이 없을 때):**
1. **의자를 오토바이 대신 사용** (YOLOv5가 chair도 인식)
2. 사람 + 의자 동시 배치
3. 사람이 매장 안으로 이동
4. Android 앱에 **파란 카드** + "📦 배달원 입장" 알림


## 📊 시스템 흐름도

```
📹 카메라 (3대)
    ↓
🖥️ Edge System (Python)
    ├── YOLOv5 (person, motorcycle 감지)
    ├── MediaPipe (손 제스처 감지)
    └── Change Detection (타입 판정)
    ↓
📡 HTTP POST → Django
    ↓
🌐 Service System (Django)
    ├── Token 인증
    ├── DB 저장 (Post 모델)
    ├── FCM 푸시 전송
    └── REST API 제공
    ↓
🔔 FCM Push Notification
    ↓
📱 Android App
    ├── 알림 수신
    ├── 자동 새로고침 (GET /api_root/Post/)
    └── UI 업데이트 (RecyclerView)
```




## 👨‍💻 개발자

**개인 프로젝트**
- 전체 시스템 개발: [jiminally](https://github.com/jiminally)
  - Edge System (AI 영상 처리)
  - Service System (Django 백엔드)
  - Client System (Android 앱)



## 🙏 감사의 말

이 프로젝트는 청각장애인 바리스타들이 더 편안한 환경에서 일할 수 있도록 돕기 위해 시작되었습니다.

> "우주는 소리가 없는 고요한 공간입니다.  
> 청각장애인 바리스타가 경험하는 세계와 닮아있어 우주 테마를 선택하게 되었습니다.  
> 조용한 우주에서 별이 빛나듯, 작은 알림 하나가 청각장애인의 일터에 빛이 되어줍니다.  
> 소리 대신 마음으로 소통할 수 있는 환경을 통해 모두가 따뜻한 기회를 누릴 수 있는 세상을 함께 만들어가겠습니다."


