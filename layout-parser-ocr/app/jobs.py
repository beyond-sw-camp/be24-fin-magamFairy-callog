from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from threading import Lock
from typing import Dict, Optional

from app.schemas import DocumentResult


@dataclass
class JobRecord:
    job_id: str
    document_id: str
    status: str
    created_at: datetime
    updated_at: datetime
    result: Optional[DocumentResult] = None
    error: Optional[str] = None


class JobStore:
    def __init__(self) -> None:
        self._records: Dict[str, JobRecord] = {}
        self._lock = Lock()

    def create(self, job_id: str, document_id: str) -> JobRecord:
        now = datetime.now(timezone.utc)
        record = JobRecord(
            job_id=job_id,
            document_id=document_id,
            status="queued",
            created_at=now,
            updated_at=now,
        )
        with self._lock:
            self._records[job_id] = record
        return record

    def mark_running(self, job_id: str) -> None:
        self._update(job_id, status="running")

    def mark_completed(self, job_id: str, result: DocumentResult) -> None:
        self._update(job_id, status="completed", result=result, error=None)

    def mark_failed(self, job_id: str, error: str) -> None:
        self._update(job_id, status="failed", error=error)

    def get(self, job_id: str) -> Optional[JobRecord]:
        with self._lock:
            return self._records.get(job_id)

    def _update(self, job_id: str, **changes: object) -> None:
        with self._lock:
            record = self._records[job_id]
            for key, value in changes.items():
                setattr(record, key, value)
            record.updated_at = datetime.now(timezone.utc)


jobs = JobStore()
