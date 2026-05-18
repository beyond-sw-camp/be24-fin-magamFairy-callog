from __future__ import annotations

from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class BlockResult(BaseModel):
    id: str
    page: int
    type: str
    bbox: List[float] = Field(description="[x1, y1, x2, y2] in page image pixels")
    score: float
    reading_order: int
    analysis_hint: str
    crop_path: str
    crop_url: str
    text: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


class PageResult(BaseModel):
    page: int
    width: int
    height: int
    image_path: str
    image_url: str
    blocks: List[BlockResult]
    clean_blocks: List[BlockResult] = Field(default_factory=list)
    layout_regions: List[Dict[str, Any]] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class DownstreamTarget(BaseModel):
    id: str
    target_type: str
    route: str
    block_id: str
    page: int
    type: str
    source: str
    priority: int
    reading_order: int
    bbox: List[float]
    bbox_normalized: List[float] = Field(default_factory=list)
    crop_path: str
    crop_url: str
    page_image_path: str
    page_image_url: str
    text: Optional[str] = None
    child_block_ids: List[str] = Field(default_factory=list)
    related_block_ids: List[str] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class DocumentResult(BaseModel):
    job_id: str
    document_id: str
    status: str
    pages: List[PageResult]
    ocr_targets: List[DownstreamTarget] = Field(default_factory=list)
    image_targets: List[DownstreamTarget] = Field(default_factory=list)
    table_targets: List[DownstreamTarget] = Field(default_factory=list)
    layout_result_path: Optional[str] = None
    layout_result_url: Optional[str] = None
    created_at: datetime
    completed_at: datetime


class JobAccepted(BaseModel):
    job_id: str
    document_id: str
    status: str
    status_url: str
    result_url: str


class JobStatusResponse(BaseModel):
    job_id: str
    document_id: str
    status: str
    created_at: datetime
    updated_at: datetime
    error: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    app: str
    model_config: str
    model_device: str
