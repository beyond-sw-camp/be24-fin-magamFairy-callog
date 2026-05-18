# Local Docker Runbook

이 문서는 `document-analysis-service`를 다른 컴퓨터의 로컬 Docker 환경에서 동일하게 실행하기 위한 운영 기준입니다. Kubernetes 배포는 범위에서 제외합니다.

프론트/백엔드/분석 서버 연결의 실제 데이터 흐름은 [`integration-flow.md`](integration-flow.md)를 기준으로 합니다.

## 역할

이 서비스는 PDF 또는 이미지를 받아서 문서를 구조화합니다.

- PDF 페이지 렌더링
- layout-parser + Detectron2 레이아웃 검출
- 텍스트 전용 OCR canvas 생성
- `/ocr` endpoint에서 PaddleOCR 기반 OCR 수행
- figure/image crop 생성
- table crop 생성
- downstream 서버가 사용할 JSON, crop URL 반환

현재 Callog 통합은 layout-parser와 OCR을 이 서비스 하나에서 제공합니다. 백엔드는 `ocr_targets`의 `PageTextCanvas` 이미지를 다운로드한 뒤 같은 서버의 `/ocr` endpoint로 전달합니다.

## 준비 환경

공통 요구사항:

- Docker Desktop 또는 Docker Engine
- Docker Compose v2
- 인터넷 연결
- 최소 8GB RAM, 권장 16GB 이상
- 여유 디스크 10GB 이상
- 이 프로젝트 폴더 전체

Windows 권장 구성:

- Windows 10/11
- Docker Desktop with WSL2 backend
- WSL2 활성화

GPU 실행 요구사항:

- NVIDIA GPU
- 최신 NVIDIA 드라이버
- Docker Desktop에서 WSL2 GPU 지원 활성화
- 컨테이너에서 `nvidia-smi`가 동작해야 함

GPU 확인:

```powershell
docker run --rm --gpus all nvidia/cuda:11.8.0-base-ubuntu22.04 nvidia-smi
```

위 명령이 실패하면 GPU 모드는 아직 준비되지 않은 상태입니다. 이 경우 CPU 모드로 먼저 실행합니다.

## 폴더 구성

필수 파일:

```text
Dockerfile
docker-compose.yml
docker-compose.gpu.yml
requirements.txt
app/
```

런타임 생성 폴더:

```text
storage/
  uploads/
  results/
  model_cache/
```

`storage/model_cache`에는 layout-parser 모델 config/weight가 캐시됩니다. 다른 컴퓨터가 인터넷 접근이 느리거나 제한적이면 기존 PC의 `storage/model_cache`를 함께 복사하면 첫 요청 시간을 줄일 수 있습니다.

## 최초 실행

프로젝트 루트로 이동합니다.

```powershell
cd C:\path\to\Quantom
```

CPU 모드:

```powershell
docker compose down --remove-orphans
docker compose up -d --build
```

GPU 모드:

```powershell
docker compose -f docker-compose.yml -f docker-compose.gpu.yml down --remove-orphans
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up -d --build
```

상태 확인:

```powershell
docker compose ps
Invoke-RestMethod -Uri http://localhost:8001/health | ConvertTo-Json -Depth 5
```

CPU 정상 응답:

```json
{
  "status": "ok",
  "app": "document-analysis-service",
  "model_device": "cpu"
}
```

GPU 정상 응답:

```json
{
  "status": "ok",
  "app": "document-analysis-service",
  "model_device": "cuda"
}
```

첫 layout 분석 요청과 첫 OCR 요청은 모델 다운로드와 로딩 때문에 느릴 수 있습니다. 운영 전 샘플 파일로 warm-up 1회를 수행하는 것을 권장합니다.

## 분석 요청

PowerShell 예시:

```powershell
curl.exe -X POST "http://localhost:8001/v1/layout/analyze" `
  -F "document_id=sample-001" `
  -F "file=@C:\path\to\sample.pdf"
```

Linux/macOS 예시:

```bash
curl -X POST "http://localhost:8001/v1/layout/analyze" \
  -F "document_id=sample-001" \
  -F "file=@/path/to/sample.pdf"
```

OCR 요청:

```powershell
curl.exe -X POST "http://localhost:8001/ocr" `
  -F "file=@C:\path\to\sample.png"
```

지원 입력:

- `.pdf`
- `.png`
- `.jpg`, `.jpeg`
- `.tif`, `.tiff`
- `.bmp`
- `.webp`

기본 업로드 제한은 `MAX_UPLOAD_MB=100`입니다.

## 응답에서 사용할 필드

다운스트림 연동은 아래 세 배열만 우선 사용합니다.

```text
ocr_targets
image_targets
table_targets
```

같은 분석 서버의 `/ocr` endpoint로 보낼 대상:

```json
{
  "target_type": "ocr",
  "route": "ocr_server",
  "type": "PageTextCanvas",
  "source": "page_text_canvas",
  "crop_url": "http://localhost:8001/assets/{job_id}/clean_crops/page-001/page-001-text-canvas.png"
}
```

`PageTextCanvas`는 한 페이지의 텍스트 영역만 모은 OCR 입력 이미지입니다. 큰 canvas는 조건부 다운스케일링됩니다.

관련 metadata:

```text
metadata.canvas_size                  원본 compact canvas 크기
metadata.ocr_input_size               실제 OCR endpoint로 보낼 이미지 크기
metadata.ocr_input_scale              OCR 입력 축소 비율
metadata.ocr_input_downscale.applied  축소 적용 여부
metadata.original_canvas_path         원본 compact canvas 보관 경로
metadata.canvas_region_map            원본 페이지 좌표와 canvas 좌표 매핑
```

이미지 분석 서버로 보낼 대상:

```text
image_targets[].crop_url
```

표 추출 서버로 보낼 대상:

```text
table_targets[].crop_url
```

`blocks`는 감사/debug 용도입니다. 직접 OCR 큐로 쓰지 않습니다.

## 주요 환경변수

현재 `docker-compose.yml`에 기본값이 명시되어 있습니다.

```text
PORT: 8001
STORAGE_DIR: /service/storage
RENDER_DPI: 200
LAYOUT_MODEL_DEVICE: cpu 또는 cuda
LAYOUT_SCORE_THRESHOLD: 0.50
MAX_UPLOAD_MB: 100
```

OCR canvas 다운스케일링:

```text
OCR_TEXT_CANVAS_MAX_LONG_SIDE=1600
OCR_TEXT_CANVAS_MAX_PIXELS=2000000
OCR_TEXT_CANVAS_MIN_TEXT_HEIGHT=20
OCR_TEXT_CANVAS_KEEP_ORIGINAL=true
OCR_TEMP_ROOT=/service/storage/ocr-temp
PADDLE_OCR_BASE_DIR=/service/storage/paddleocr
OCR_CANDIDATE_STRATEGY=auto
OCR_LOAD_MODEL_ON_STARTUP=false
```

정책:

- layout-parser 입력 이미지는 줄이지 않습니다.
- OCR endpoint로 보내는 text canvas만 조건부 축소합니다.
- 작은 글자가 20px 아래로 내려갈 것으로 예상되면 과도하게 줄이지 않습니다.
- 5% 미만의 미세 축소는 실익이 낮아 skip합니다.

## 운영 명령

로그 확인:

```powershell
docker compose logs -f document-analysis-service
```

중지:

```powershell
docker compose down
```

재시작:

```powershell
docker compose restart
```

코드 변경 후 재빌드:

```powershell
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up -d --build
```

CPU로 강제 전환:

```powershell
docker compose up -d --force-recreate
```

GPU로 전환:

```powershell
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up -d --force-recreate
```

## 검증 절차

1. Health 확인

```powershell
Invoke-RestMethod -Uri http://localhost:8001/health | ConvertTo-Json -Depth 5
```

2. 샘플 PDF 또는 이미지 1개 분석

```powershell
curl.exe -X POST "http://localhost:8001/v1/layout/analyze" `
  -F "document_id=smoke-test" `
  -F "file=@C:\path\to\sample.pdf" `
  -o smoke-result.json
```

3. 결과 폴더 확인

```text
storage/results/{job_id}/layout_result.json
storage/results/{job_id}/result.json
storage/results/{job_id}/clean_crops/
storage/results/{job_id}/pages/
```

4. 응답 기준 확인

- `status`가 `completed`
- `ocr_targets`가 페이지 수만큼 존재
- `ocr_targets[].crop_url` 접근 가능
- `image_targets`가 필요한 figure만 포함
- `table_targets`가 표가 있을 때만 포함

## 외부 서버에서 접근할 때

다른 컴퓨터가 이 서비스를 호출해야 하면 서버 PC의 IP를 사용합니다.

예:

```text
http://192.168.0.25:8001
```

이 경우 `PUBLIC_BASE_URL`도 외부에서 접근 가능한 주소로 바꾸는 것이 좋습니다.

```yaml
PUBLIC_BASE_URL: http://192.168.0.25:8001
```

`PUBLIC_BASE_URL`이 `localhost`로 남아 있으면 응답의 `crop_url`도 `localhost`로 내려가므로, 다른 컴퓨터의 백엔드가 crop 이미지를 가져오지 못합니다.

## 장애 대응

모델 다운로드 실패:

- 인터넷 연결 확인
- Hugging Face 접근 가능 여부 확인
- 기존 PC의 `storage/model_cache` 복사

GPU가 `cuda`로 뜨지 않음:

- `docker run --rm --gpus all nvidia/cuda:11.8.0-base-ubuntu22.04 nvidia-smi` 확인
- Docker Desktop WSL2 backend 확인
- NVIDIA 드라이버 업데이트
- 우선 CPU 모드로 실행

업로드가 413으로 실패:

- `MAX_UPLOAD_MB` 증가

crop URL이 다른 서버에서 열리지 않음:

- `PUBLIC_BASE_URL`을 서버 PC IP로 설정
- 방화벽에서 8001 포트 허용

결과가 너무 많이 쌓임:

- `storage/results`와 `storage/uploads`는 운영 정책에 따라 주기적으로 정리
- `storage/model_cache`는 삭제하지 않는 것을 권장

## 현재 기준 성능

검증 PC 기준, `source` 8개 파일 25페이지 처리:

```text
CPU: 약 100초
GPU: 약 39초
GPU 속도 향상: 약 2.6배
품질 게이트: 99.09 / 100
```

문서 특성에 따라 속도는 달라집니다. PDF 렌더링, PDF-native 추출, crop 저장 비중이 큰 파일은 GPU 효과가 작을 수 있습니다.
