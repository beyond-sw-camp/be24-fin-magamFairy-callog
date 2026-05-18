from __future__ import annotations

import json
import re
import shutil
from pathlib import Path
from typing import Any, Dict, Optional
from uuid import uuid4

from fastapi import HTTPException, UploadFile

from app.settings import settings


_SAFE_NAME_RE = re.compile(r"[^A-Za-z0-9._-]+")


def ensure_storage() -> None:
    settings.uploads_dir.mkdir(parents=True, exist_ok=True)
    settings.results_dir.mkdir(parents=True, exist_ok=True)


def new_job_id() -> str:
    return uuid4().hex


def safe_filename(name: Optional[str], fallback: str = "document") -> str:
    if not name:
        return fallback
    cleaned = _SAFE_NAME_RE.sub("_", Path(name).name).strip("._")
    return cleaned or fallback


async def save_upload(file: UploadFile, job_id: str) -> Path:
    ensure_storage()

    upload_dir = settings.uploads_dir / job_id
    upload_dir.mkdir(parents=True, exist_ok=True)
    upload_path = upload_dir / safe_filename(file.filename)

    max_bytes = settings.max_upload_mb * 1024 * 1024
    size = 0

    with upload_path.open("wb") as out:
        while True:
            chunk = await file.read(1024 * 1024)
            if not chunk:
                break
            size += len(chunk)
            if size > max_bytes:
                shutil.rmtree(upload_dir, ignore_errors=True)
                raise HTTPException(
                    status_code=413,
                    detail=f"Upload exceeds MAX_UPLOAD_MB={settings.max_upload_mb}",
                )
            out.write(chunk)

    if size == 0:
        shutil.rmtree(upload_dir, ignore_errors=True)
        raise HTTPException(status_code=400, detail="Uploaded file is empty")

    return upload_path


def result_job_dir(job_id: str) -> Path:
    path = settings.results_dir / job_id
    path.mkdir(parents=True, exist_ok=True)
    return path


def write_result_json(job_id: str, data: Dict[str, Any]) -> Path:
    result_path = result_job_dir(job_id) / "result.json"
    with result_path.open("w", encoding="utf-8") as out:
        json.dump(data, out, ensure_ascii=False, indent=2, default=str)
    return result_path


def write_job_json(job_id: str, filename: str, data: Dict[str, Any]) -> Path:
    result_path = result_job_dir(job_id) / safe_filename(filename, fallback="data.json")
    with result_path.open("w", encoding="utf-8") as out:
        json.dump(data, out, ensure_ascii=False, indent=2, default=str)
    return result_path


def asset_url(base_url: str, relative_path: str) -> str:
    public_base = settings.public_base_url or base_url.rstrip("/")
    normalized_path = relative_path.replace("\\", "/")
    return f"{public_base}/assets/{normalized_path}"
