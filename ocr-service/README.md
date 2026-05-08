# Callog OCR Service

Small local FastAPI service that wraps PaddleOCR for Korean image and scanned PDF text extraction.

## Local run on Windows

Requires Python 3.10, 3.11, or 3.12 64-bit. If your default Python is newer, install a compatible runtime:

```powershell
py install 3.12
```

```powershell
.\run-local.ps1
```

The service starts at `http://localhost:8000`.

If `python` points to the Microsoft Store alias instead of a real Python install, turn off:

```text
Settings > Apps > Advanced app settings > App execution aliases > python.exe / python3.exe
```

Then install Python from `python.org` and check `Add python.exe to PATH`.

## Manual run

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8000
```

## API

```powershell
curl.exe -F "file=@sample.png" http://localhost:8000/ocr
```

The response includes the merged `text`, per-line OCR results, and the processed page count.

## Image OCR tuning

The service runs multiple local OCR candidates and chooses the best readable result:

- EXIF rotation correction
- original RGB with upscale when the image is small
- soft grayscale/autocontrast fallback
- stronger high-contrast fallback
- bounding-box based line grouping and paragraph spacing

Angle classification is off by default because it can damage normal horizontal Korean text.

You can tune it before running the server:

```powershell
$env:OCR_TARGET_LONG_EDGE = "3000"
$env:OCR_DET_LIMIT_SIDE_LEN = "3000"
$env:OCR_CONTRAST = "2.0"
$env:OCR_SHARPNESS = "1.8"
$env:OCR_MIN_SCORE = "0.42"
$env:OCR_TEMP_ROOT = "C:\temp\callog-ocr"
.\run-local.ps1
```

Set `$env:OCR_PREPROCESS = "false"` to compare against the raw OCR result.
Set `$env:OCR_USE_ANGLE_CLS = "true"` only when rotated text is a stronger concern than Korean recognition quality.
