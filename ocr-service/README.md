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

## Receipt/image tuning

The service preprocesses images before OCR by default:

- EXIF rotation correction
- upscale small images to a 2400px long edge
- grayscale, autocontrast, contrast boost, and sharpening

You can tune it before running the server:

```powershell
$env:OCR_TARGET_LONG_EDGE = "3000"
$env:OCR_CONTRAST = "2.0"
$env:OCR_SHARPNESS = "1.8"
.\run-local.ps1
```

Set `$env:OCR_PREPROCESS = "false"` to compare against the raw OCR result.
