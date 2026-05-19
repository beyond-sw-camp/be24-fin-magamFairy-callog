from __future__ import annotations

import logging
from pathlib import Path
from typing import Optional

import httpx
from fastapi import BackgroundTasks, FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.encoders import jsonable_encoder
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

from app.jobs import jobs
from app.layout_detector import detector
from app.ocr_engine import (
    cleanup_stale_ocr_temp_dirs,
    ocr_router,
    release_ocr_model,
    start_ocr_job_workers,
    stop_ocr_job_workers,
    warm_up_ocr_model,
)
from app.processor import process_document
from app.schemas import HealthResponse, JobAccepted, JobStatusResponse
from app.settings import settings
from app.storage import ensure_storage, new_job_id, save_upload


ensure_storage()
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/assets", StaticFiles(directory=str(settings.results_dir)), name="assets")
app.include_router(ocr_router)


@app.on_event("startup")
def startup() -> None:
    ensure_storage()
    cleanup_stale_ocr_temp_dirs()
    start_ocr_job_workers()
    if settings.load_model_on_startup:
        detector.load()
    if settings.ocr_load_model_on_startup:
        warm_up_ocr_model()


@app.on_event("shutdown")
def shutdown() -> None:
    stop_ocr_job_workers()
    release_ocr_model()


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        app=settings.app_name,
        model_config=settings.layout_model_config,
        model_device=settings.layout_model_device,
    )


@app.post("/v1/layout/analyze")
async def analyze(
    request: Request,
    file: UploadFile = File(...),
    document_id: Optional[str] = Form(default=None),
    include_image_targets: Optional[bool] = Form(default=None),
) -> JSONResponse:
    job_id = new_job_id()
    resolved_document_id = document_id or job_id
    source_path = await save_upload(file, job_id)
    result = process_document(
        job_id=job_id,
        document_id=resolved_document_id,
        source_path=source_path,
        request_base_url=_request_base_url(request),
        include_image_targets=include_image_targets,
    )
    return JSONResponse(jsonable_encoder(result))


@app.post("/v1/layout/jobs", response_model=JobAccepted, status_code=202)
async def create_job(
    request: Request,
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    document_id: Optional[str] = Form(default=None),
    callback_url: Optional[str] = Form(default=None),
    include_image_targets: Optional[bool] = Form(default=None),
) -> JobAccepted:
    job_id = new_job_id()
    resolved_document_id = document_id or job_id
    source_path = await save_upload(file, job_id)
    jobs.create(job_id, resolved_document_id)

    background_tasks.add_task(
        _run_job,
        job_id,
        resolved_document_id,
        source_path,
        _request_base_url(request),
        callback_url,
        include_image_targets,
    )

    return JobAccepted(
        job_id=job_id,
        document_id=resolved_document_id,
        status="queued",
        status_url=f"{_request_base_url(request)}/v1/layout/jobs/{job_id}",
        result_url=f"{_request_base_url(request)}/v1/layout/jobs/{job_id}/result",
    )


@app.get("/v1/layout/jobs/{job_id}", response_model=JobStatusResponse)
def get_job(job_id: str) -> JobStatusResponse:
    record = jobs.get(job_id)
    if record is None:
        raise HTTPException(status_code=404, detail="Job not found")

    return JobStatusResponse(
        job_id=record.job_id,
        document_id=record.document_id,
        status=record.status,
        created_at=record.created_at,
        updated_at=record.updated_at,
        error=record.error,
    )


@app.get("/v1/layout/jobs/{job_id}/result")
def get_job_result(job_id: str) -> JSONResponse:
    record = jobs.get(job_id)
    if record is None:
        raise HTTPException(status_code=404, detail="Job not found")
    if record.status != "completed" or record.result is None:
        raise HTTPException(status_code=409, detail=f"Job is {record.status}")

    return JSONResponse(jsonable_encoder(record.result))


def _run_job(
    job_id: str,
    document_id: str,
    source_path: Path,
    request_base_url: str,
    callback_url: Optional[str],
    include_image_targets: Optional[bool],
) -> None:
    try:
        jobs.mark_running(job_id)
        result = process_document(
            job_id=job_id,
            document_id=document_id,
            source_path=source_path,
            request_base_url=request_base_url,
            include_image_targets=include_image_targets,
        )
        jobs.mark_completed(job_id, result)
        if callback_url:
            _send_callback(callback_url, jsonable_encoder(result))
    except Exception as exc:
        logger.exception("Layout job failed: %s", job_id)
        error = str(exc) or repr(exc)
        jobs.mark_failed(job_id, error)
        if callback_url:
            _send_callback(
                callback_url,
                {
                    "job_id": job_id,
                    "document_id": document_id,
                    "status": "failed",
                    "error": error,
                },
            )


def _send_callback(callback_url: str, payload: dict) -> None:
    with httpx.Client(timeout=settings.callback_timeout_seconds) as client:
        client.post(callback_url, json=payload)


def _request_base_url(request: Request) -> str:
    return str(request.base_url).rstrip("/")
