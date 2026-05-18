from __future__ import annotations

import argparse
import json
import mimetypes
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Upload a document to the layout service and build/dispatch downstream "
            "OCR, image-analysis, and table-extraction batch payloads."
        )
    )
    parser.add_argument("--layout-url", default="http://localhost:8001")
    parser.add_argument("--file", help="PDF or image path to upload to the layout service.")
    parser.add_argument("--document-id", default=None)
    parser.add_argument(
        "--result-json",
        help="Use an existing layout result JSON instead of uploading a file.",
    )
    parser.add_argument("--output", default="storage/last_dispatch_payloads.json")
    parser.add_argument("--ocr-url", help="OCR server batch endpoint.")
    parser.add_argument("--image-url", help="Image-analysis server batch endpoint.")
    parser.add_argument("--table-url", help="Table-extraction server batch endpoint.")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Only write payloads locally; do not POST to downstream servers.",
    )
    args = parser.parse_args()

    if not args.file and not args.result_json:
        raise SystemExit("Pass --file or --result-json.")

    if args.result_json:
        layout_result = json.loads(Path(args.result_json).read_text(encoding="utf-8"))
    else:
        layout_result = _upload_to_layout_service(
            layout_url=args.layout_url.rstrip("/"),
            path=Path(args.file),
            document_id=args.document_id,
        )

    payloads = build_dispatch_payloads(layout_result)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payloads, ensure_ascii=False, indent=2), encoding="utf-8")

    dispatch_results: dict[str, Any] = {}
    if not args.dry_run:
        endpoint_map = {
            "ocr": args.ocr_url,
            "image": args.image_url,
            "table": args.table_url,
        }
        for kind, endpoint in endpoint_map.items():
            if not endpoint:
                continue
            dispatch_results[kind] = _post_json(endpoint, payloads[kind])

    print(
        json.dumps(
            {
                "document_id": payloads["document"]["document_id"],
                "layout_job_id": payloads["document"]["layout_job_id"],
                "payload_file": str(output_path),
                "targets": {
                    "ocr": len(payloads["ocr"]["targets"]),
                    "image": len(payloads["image"]["targets"]),
                    "table": len(payloads["table"]["targets"]),
                },
                "dispatched": dispatch_results,
            },
            ensure_ascii=False,
            indent=2,
        )
    )


def build_dispatch_payloads(layout_result: dict[str, Any]) -> dict[str, Any]:
    document = {
        "document_id": layout_result.get("document_id"),
        "layout_job_id": layout_result.get("job_id"),
        "layout_status": layout_result.get("status"),
        "layout_result_url": layout_result.get("layout_result_url"),
        "created_at": layout_result.get("created_at"),
        "completed_at": layout_result.get("completed_at"),
    }
    page_index = {
        page.get("page"): {
            "page": page.get("page"),
            "width": page.get("width"),
            "height": page.get("height"),
            "image_url": page.get("image_url"),
            "image_path": page.get("image_path"),
            "ocr_text_canvas": page.get("metadata", {}).get("ocr_text_canvas"),
            "layout_summary": page.get("metadata", {}).get("layout_summary"),
        }
        for page in layout_result.get("pages", [])
    }

    return {
        "document": document,
        "ocr": {
            **document,
            "task_type": "ocr_batch",
            "targets": [
                _ocr_target_payload(target, page_index.get(target.get("page"), {}))
                for target in layout_result.get("ocr_targets", [])
            ],
        },
        "image": {
            **document,
            "task_type": "image_analysis_batch",
            "targets": [
                _crop_target_payload("figure_crop", target, page_index.get(target.get("page"), {}))
                for target in layout_result.get("image_targets", [])
            ],
        },
        "table": {
            **document,
            "task_type": "table_extraction_batch",
            "targets": [
                _crop_target_payload("table_crop", target, page_index.get(target.get("page"), {}))
                for target in layout_result.get("table_targets", [])
            ],
        },
    }


def _ocr_target_payload(target: dict[str, Any], page: dict[str, Any]) -> dict[str, Any]:
    metadata = target.get("metadata") or {}
    scale = metadata.get("ocr_input_scale") or 1.0
    return {
        "target_id": target.get("id"),
        "block_id": target.get("block_id"),
        "page": target.get("page"),
        "reading_order": target.get("reading_order"),
        "priority": target.get("priority"),
        "route": target.get("route"),
        "type": target.get("type"),
        "source": target.get("source"),
        "bbox": target.get("bbox"),
        "bbox_normalized": target.get("bbox_normalized"),
        "input": {
            "kind": "page_text_canvas",
            "image_url": target.get("crop_url"),
            "image_path": target.get("crop_path"),
            "image_size": metadata.get("ocr_input_size") or metadata.get("canvas_size"),
            "coordinate_space": "ocr_input_image",
            "scale_to_canvas": scale,
            "canvas_size": metadata.get("canvas_size"),
            "original_canvas_path": metadata.get("original_canvas_path"),
            "original_canvas_url": metadata.get("original_canvas_url"),
            "original_page_size": metadata.get("original_page_size"),
            "page_image_url": target.get("page_image_url") or page.get("image_url"),
        },
        "mapping": {
            "ocr_box_to_canvas": (
                "Divide OCR output coordinates by scale_to_canvas when scale_to_canvas < 1."
            ),
            "canvas_to_original_page": (
                "Find the canvas_region_map item containing the OCR box center, then offset "
                "from canvas_bbox to original_bbox."
            ),
            "canvas_region_map": metadata.get("canvas_region_map", []),
        },
        "expected_response": {
            "document_id": "same as request",
            "layout_job_id": "same as request",
            "target_id": target.get("id"),
            "page": target.get("page"),
            "text": "recognized text",
            "words": [
                {
                    "text": "word",
                    "bbox": [0, 0, 10, 10],
                    "coordinate_space": "ocr_input_image",
                    "confidence": 0.99,
                }
            ],
        },
        "metadata": metadata,
    }


def _crop_target_payload(
    kind: str,
    target: dict[str, Any],
    page: dict[str, Any],
) -> dict[str, Any]:
    return {
        "target_id": target.get("id"),
        "block_id": target.get("block_id"),
        "page": target.get("page"),
        "reading_order": target.get("reading_order"),
        "priority": target.get("priority"),
        "route": target.get("route"),
        "type": target.get("type"),
        "source": target.get("source"),
        "bbox": target.get("bbox"),
        "bbox_normalized": target.get("bbox_normalized"),
        "input": {
            "kind": kind,
            "image_url": target.get("crop_url"),
            "image_path": target.get("crop_path"),
            "page_image_url": target.get("page_image_url") or page.get("image_url"),
            "page_size": [page.get("width"), page.get("height")],
            "coordinate_space": "original_page_image",
        },
        "expected_response": {
            "document_id": "same as request",
            "layout_job_id": "same as request",
            "target_id": target.get("id"),
            "page": target.get("page"),
            "result": {},
        },
        "metadata": target.get("metadata") or {},
    }


def _upload_to_layout_service(
    *,
    layout_url: str,
    path: Path,
    document_id: str | None,
) -> dict[str, Any]:
    body, content_type = _multipart_body(path, document_id or path.stem)
    request = urllib.request.Request(
        f"{layout_url}/v1/layout/analyze",
        data=body,
        headers={"Content-Type": content_type},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=1800) as response:
        return json.loads(response.read().decode("utf-8"))


def _multipart_body(path: Path, document_id: str) -> tuple[bytes, str]:
    boundary = f"----quantom-layout-flow-{time.time_ns()}"
    content_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
    chunks = [
        f"--{boundary}\r\n".encode(),
        b'Content-Disposition: form-data; name="document_id"\r\n\r\n',
        document_id.encode("utf-8"),
        b"\r\n",
        f"--{boundary}\r\n".encode(),
        (
            f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
            f"Content-Type: {content_type}\r\n\r\n"
        ).encode("utf-8"),
        path.read_bytes(),
        b"\r\n",
        f"--{boundary}--\r\n".encode(),
    ]
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def _post_json(url: str, payload: dict[str, Any]) -> dict[str, Any]:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=300) as response:
            raw = response.read()
            return {
                "status_code": response.status,
                "body": raw.decode("utf-8", errors="replace"),
            }
    except urllib.error.HTTPError as exc:
        return {
            "status_code": exc.code,
            "body": exc.read().decode("utf-8", errors="replace"),
        }


if __name__ == "__main__":
    main()
