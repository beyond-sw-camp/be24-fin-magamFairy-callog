# Document Analysis Service

FastAPI service for document layout parsing and OCR. It receives a PDF or
image, renders pages, detects layout blocks with `layout-parser + Detectron2`,
saves page/block crops, and returns structured JSON. The same server also
provides `/ocr`, so Callog can run layout parsing and OCR through one analysis
service instead of two separate HTTP services.

This service uses `layoutparser.Detectron2LayoutModel` as the layout detection
engine. The default model is PubLayNet, loaded from Hugging Face because the
old `lp://PubLayNet/...` Dropbox links used by layout-parser may return deleted
file pages:

```text
https://huggingface.co/layoutparser/detectron2/resolve/main/PubLayNet/faster_rcnn_R_50_FPN_3x/config.yml
https://huggingface.co/layoutparser/detectron2/resolve/main/PubLayNet/faster_rcnn_R_50_FPN_3x/model_final.pth
```

Default labels:

```text
0 Text
1 Title
2 List
3 Table
4 Figure
```

## API

### Health

```bash
curl http://localhost:8001/health
```

### OCR

```bash
curl -X POST http://localhost:8001/ocr \
  -F "file=@sample.png"
```

The OCR response matches the former standalone OCR service contract:

```json
{
  "text": "recognized text",
  "lines": [],
  "pageCount": 1
}
```

### Synchronous analysis

```bash
curl -X POST http://localhost:8001/v1/layout/analyze \
  -F "document_id=doc-001" \
  -F "file=@sample.pdf"
```

### Asynchronous job

```bash
curl -X POST http://localhost:8001/v1/layout/jobs \
  -F "document_id=doc-001" \
  -F "callback_url=http://your-server.example/api/layout-callback" \
  -F "file=@sample.pdf"
```

Then poll:

```bash
curl http://localhost:8001/v1/layout/jobs/{job_id}
curl http://localhost:8001/v1/layout/jobs/{job_id}/result
```

## Response shape

Each block contains:

- `type`: `Text`, `Title`, `List`, `Table`, `Figure`, or model-specific label.
- `bbox`: `[x1, y1, x2, y2]` in rendered page pixels.
- `analysis_hint`: `ocr`, `embedded_text`, `table`, `figure`, `layout`, or
  `full_page`.
- `crop_url`: URL for the cropped block image.
- `reading_order`: XY-cut reading order that keeps full-width headers before
  multi-column content and orders columns left-to-right.
- `metadata.source`: `layout_parser`, `pdf_native`, or `page_fallback`.
- `metadata.role`: `semantic_region`, `ocr_region`, `image_region`, or
  `coverage_guard`.
- `metadata.ocr`: downstream routing metadata with `recommended`, `priority`,
  `mode`, and a human-readable `reason`.
- `metadata.bbox_normalized`: `[x1, y1, x2, y2]` normalized to page width and
  height for OCR/image-analysis services that resize page images.
- `metadata.source=visual_text_merged`: line/paragraph-level OCR regions
  generated from nearby `visual_text` candidates. These blocks include
  `metadata.merge_type`, `metadata.child_candidate_ids`, and `metadata.children`
  resolved to the original block ids in the final response.

Each page also contains `clean_blocks`. This is the non-overlapping,
server-facing layout view built from the final downstream targets:
deduplicated OCR targets, table targets, and figure targets that do not cover
the same area as selected text/table regions. Use `clean_blocks` for UI
overlays, human review, and parser contract checks. Keep `blocks` as the full
audit/debug candidate list because it intentionally includes layout-parser,
visual text, PDF-native, merge, and fallback candidates.

Each page also contains `layout_regions`, a compact reading-order layout result
derived from `clean_blocks`. A region includes `region_type`
(`text_region`, `image_region`, `table_region`, or `layout_region`), `route`,
`bbox`, `bbox_normalized`, `source`, `layout_role`, `crop_url`, and the source
`block_id`. `metadata.layout_summary` gives region counts, route counts, and
the layout strategy used on the page.

For PDF input, the service renders each page to an image, runs layout-parser on
that rendered page, runs visual text detection and merge on the same page image,
and also extracts PDF-native text and image blocks with PyMuPDF. A `Page` block
is included for every page as a full-page coverage guard, so downstream OCR or
inspection can recover content if a detector misses a region.

PDF-native text blocks include the extracted `text` payload and use
`analysis_hint=embedded_text`, so OCR workers can skip duplicate OCR for text
already present in the source PDF. Overlapping layout-parser text regions are
kept for structure, but marked in `metadata.ocr.redundant_with`.

For raster image input and rendered PDF pages, the service adds `visual_text`
blocks detected with an OpenCV morphology heuristic. This compensates for
PubLayNet misclassifying large rendered text inside ad creatives, screenshots,
or PDF page images as `Figure`. When a layout-parser `Figure` overlaps visual
text heavily, its label is refined to `Text` and the original label is kept in
`metadata.original_type`.

The service also adds `visual_graphic` figure blocks for significant non-text
visual regions after masking out visual text candidates. This keeps advertising
or presentation images useful for an image-analysis server without routing
every decorative pixel or full-page background as a figure.
By default, image-analysis targets are not returned to downstream services
(`INCLUDE_IMAGE_TARGETS=false`). The visual graphic regions can still be used
internally to keep OCR text areas clean, but only text OCR targets are expected
for the current Callog integration.

The service also adds `visual_text_merged` blocks for OCR. These are extra
blocks, not replacements: the original `visual_text` candidates remain in the
response for debugging and fallback recovery. The merge pass joins candidates
on the same line when y-position, height, and x-gap are compatible, then joins
nearby lines into paragraphs when vertical gap, x-start, and text height are
compatible. Short CTA-like lines and title/body height breaks are kept separate.

The response also contains downstream-ready target lists:

- `ocr_targets`: minimal OCR work queue. `visual_text_merged` targets are
  prioritized first, their child `visual_text` blocks are excluded, and
  overlapping layout-parser text candidates are removed to avoid duplicate OCR.
- `image_targets`: optional figure/image crops for an image understanding server.
  Tiny PDF-native decorative images and large context figure boxes that mostly
  cover selected OCR/table regions are filtered out.
- `table_targets`: table crops for table extraction or table-specific OCR.

Recommended downstream handling:

- Send `ocr_targets` to the same analysis server's `/ocr` endpoint in the
  returned order, or to a separately deployed OCR server when comparing
  runtime behavior.
- Send `image_targets` to the image analysis server only when
  `INCLUDE_IMAGE_TARGETS=true` or the request explicitly enables it.
- Send `table_targets` to the table extraction server in the returned order.
- Keep `blocks` for audit/debug, not as a direct OCR queue.
- Use `pages[].clean_blocks` when drawing overlays or inspecting the final
  page-level layout. It is the representative result; `blocks` is the raw
  evidence set.
- Use `pages[].layout_regions` when an integration needs one unified layout
  list instead of separate OCR/image/table target arrays.
- Use `analysis_hint=embedded_text` directly as text; do not OCR it again.
- Use `metadata.role=semantic_region` for document structure labels from
  layout-parser.
- Use `type=Page` / `metadata.role=coverage_guard` only as a fallback when
  region-level extraction is insufficient.

## AHP quality gate

The repository includes a repeatable quality gate that models five functional
expert committees and applies AHP weighting to score service readiness:

- API integration committee
- Layout-parser runtime committee
- OCR readiness committee
- Reliability and safety committee
- Verification and operations committee

Run:

```bash
python -m tools.quality_gate
python -m unittest discover
python -m tools.source_quality_gate
python -m tools.render_clean_overlays
```

The target score is 95/100. A passing gate means the AHP weighted score is at
least 95 and the AHP consistency ratio is within the accepted 0.10 threshold.

## Run with Docker

For a full handoff guide for another local Docker machine, see
[`docs/local-docker-runbook.md`](docs/local-docker-runbook.md).
For frontend/backend/OCR integration contracts, see
[`docs/integration-flow.md`](docs/integration-flow.md).

```bash
docker compose up --build
```

The Dockerfile uses a Linux PyTorch runtime, installs Detectron2 from the
official wheel index for CUDA 11.3 / Torch 1.10, and installs PaddleOCR for the
co-located OCR endpoint. It defaults to CPU execution through
`LAYOUT_MODEL_DEVICE=cpu` and `OCR_USE_GPU=false`. If this PC has an NVIDIA GPU
and Docker GPU support, tune those values deliberately after a CPU baseline
passes.

## Runtime notes

- Use WSL2 or Docker on Windows. Native Windows Detectron2 installation is
  fragile.
- The first request downloads the layout model weights unless they are already
  cached in the container.
- Store returned `crop_url` values in downstream systems instead of embedding
  base64 images in JSON.
- OCR uses `OCR_CANDIDATE_STRATEGY=first` by default so each received image is
  sent through PaddleOCR once. Use `auto` or `all` only for accuracy comparison
  runs because those modes can evaluate up to three preprocessed candidates per
  image.
- OCR models are loaded lazily on the first `/ocr` request by default. Set
  `OCR_LOAD_MODEL_ON_STARTUP=true` only when startup latency is less important
  than first-request latency.
