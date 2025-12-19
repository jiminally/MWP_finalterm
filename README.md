## 🚀 Edge System 설치 가이드

### 사전 요구사항

- **Python**: 3.8 이상 (권장: 3.10 이상)
- **PyTorch**: 1.8.0 이상
- **CUDA**: GPU 사용 시 (선택사항)

### 1단계: 저장소 클론

```bash
# 저장소 클론
git clone <repository-url>
cd Edge_System
```

### 2단계: Python 가상환경 생성 (권장)

```bash
# 가상환경 생성
python -m venv venv

# 가상환경 활성화
# macOS/Linux:
source venv/bin/activate
# Windows:
# venv\Scripts\activate
```

### 3단계: PyTorch 설치

**CPU 버전:**
```bash
pip install torch torchvision torchaudio
```

**GPU 버전 (CUDA 11.8):**
```bash
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
```

**GPU 버전 (CUDA 12.1):**
```bash
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121
```

> 💡 **참고**: PyTorch 공식 사이트에서 시스템에 맞는 명령어 확인: https://pytorch.org/get-started/locally/

### 4단계: YOLOv5 의존성 설치

```bash
# 기본 의존성 설치
pip install -r requirements.txt
```

주요 패키지:
- `ultralytics>=8.2.64` - YOLOv5/YOLOv8 통합 라이브러리
- `opencv-python>=4.1.1` - 이미지/비디오 처리
- `numpy>=1.23.5` - 수치 연산
- `pillow>=10.3.0` - 이미지 처리
- `matplotlib>=3.3` - 시각화
- 기타 필수 패키지들

### 5단계: MediaPipe 설치 (필수)

이 프로젝트는 **MediaPipe 0.10.x**를 사용합니다. `counter` 모드에서 손 제스처 감지를 위해 필요합니다.

**⚠️ 중요**: MediaPipe 최신 버전(0.11+)은 API 구조가 변경되어 호환되지 않습니다. 반드시 0.10.x 버전을 설치해야 합니다.

```bash
# MediaPipe 0.10.x 설치 (권장)
pip install mediapipe==0.10.13

# 또는 사용 가능한 최신 0.10.x 버전 설치
pip install "mediapipe>=0.10.13,<0.11.0"
```

**사용 가능한 버전**: 0.10.13, 0.10.14, 0.10.15, 0.10.18, 0.10.20, 0.10.21, 0.10.30, 0.10.31

> ⚠️ **참고**: 
> - `counter` 모드에서만 MediaPipe가 필수입니다
> - `entrance_outside`, `entrance_inside` 모드에서는 MediaPipe 없이도 동작합니다

### 6단계: 설치 확인

```bash
# Python에서 패키지 import 테스트
python -c "import torch; print(f'✅ PyTorch: {torch.__version__}')"
python -c "import cv2; print(f'✅ OpenCV: {cv2.__version__}')"
python -c "from ultralytics import YOLO; print('✅ YOLOv5 설치 완료')"
python -c "from mediapipe.python.solutions import pose, hands; print('✅ MediaPipe 설치 완료')"
```

### 7단계: YOLOv5 모델 다운로드 (선택사항)

프로젝트에 포함된 `yolov5s.pt`를 사용하거나, 다른 모델을 다운로드할 수 있습니다:

```bash
# YOLOv5 모델 다운로드 스크립트 실행
bash data/scripts/download_weights.sh

# 또는 직접 다운로드
wget https://github.com/ultralytics/yolov5/releases/download/v7.0/yolov5s.pt
```

### 문제 해결

#### MediaPipe import 오류
```bash
# 기존 MediaPipe 제거 후 재설치
pip uninstall mediapipe -y
pip install mediapipe==0.10.13
```

#### PyTorch CUDA 오류
```bash
# CUDA 버전 확인
python -c "import torch; print(torch.cuda.is_available())"

# CPU 버전으로 재설치
pip uninstall torch torchvision torchaudio
pip install torch torchvision torchaudio
```

#### 의존성 충돌
```bash
# 가상환경 재생성
deactivate
rm -rf venv
python -m venv venv
source venv/bin/activate  # macOS/Linux
# 또는 venv\Scripts\activate  # Windows
pip install --upgrade pip
pip install -r requirements.txt
pip install mediapipe==0.10.13
```

### 설치 완료 체크리스트

- [ ] Python 3.8+ 설치됨
- [ ] PyTorch 설치 및 동작 확인
- [ ] `requirements.txt` 패키지 설치 완료
- [ ] MediaPipe 0.10.x 설치 완료
- [ ] 모든 import 테스트 통과
- [ ] YOLOv5 모델 파일 존재 (`yolov5s.pt`)

### 다음 단계

설치가 완료되면 `changedetection.py`에서 카메라 모드를 설정하고 실행하세요:

```python
# changedetection.py에서 모드 설정
CAMERA_MODE = "counter"  # 또는 "entrance_outside", "entrance_inside"
```

실행:
```bash
python detect.py --source 0  # 웹캠 사용
```

---

**참고**: 
- 전체 설치 시간: 약 5-10분 (인터넷 속도에 따라 다름)
- 디스크 공간: 약 2-3GB (PyTorch + CUDA 포함 시 더 많음)
- GPU 사용 시 CUDA 드라이버가 설치되어 있어야 합니다