from __future__ import annotations

import argparse
import json
import mimetypes
import re
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


SUPPORTED_SUFFIXES = {".pdf", ".png", ".jpg", ".jpeg", ".tif", ".tiff", ".bmp"}


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Benchmark the layout API against a source directory.",
    )
    parser.add_argument("--base-url", default="http://localhost:8001")
    parser.add_argument("--source-dir", default="source")
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--run-label", required=True)
    parser.add_argument("--warmup-file")
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    source_dir = Path(args.source_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    health = _get_json(f"{base_url}/health")
    files = _discover_files(source_dir)
    if not files:
        raise SystemExit(f"No supported files found under {source_dir}")

    warmup_summary = None
    if args.warmup_file:
        warmup_path = Path(args.warmup_file)
        if warmup_path.exists():
            warmup_summary = _post_file(
                base_url=base_url,
                path=warmup_path,
                document_id=f"{args.run_label}-warmup-{warmup_path.stem}",
                output_path=output_dir / "_warmup_response.json",
            )

    summaries: list[dict[str, Any]] = []
    batch_started = time.perf_counter()
    for index, path in enumerate(files, start=1):
        rel = path.relative_to(source_dir).as_posix()
        kind = path.parent.name or "source"
        output_name = f"{index:02d}_{_safe_name(kind)}_{_safe_name(path.stem)}.json"
        summary = _post_file(
            base_url=base_url,
            path=path,
            document_id=f"{args.run_label}-{index:02d}-{path.stem}",
            output_path=output_dir / output_name,
        )
        summary["file"] = str(path)
        summary["relative_file"] = rel
        summary["result_json"] = str(output_dir / output_name)
        summaries.append(summary)
        print(
            f"{index:02d}/{len(files):02d} {rel} "
            f"{summary['elapsed_seconds']:.3f}s "
            f"pages={summary.get('pages', 0)} "
            f"ocr={summary.get('ocr_targets', 0)} "
            f"image={summary.get('image_targets', 0)} "
            f"table={summary.get('table_targets', 0)}",
            flush=True,
        )

    total_elapsed = round(time.perf_counter() - batch_started, 3)
    aggregate = _aggregate(args.run_label, health, warmup_summary, summaries, total_elapsed)
    (output_dir / "summary.json").write_text(
        json.dumps(aggregate, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(json.dumps(aggregate["totals"], ensure_ascii=False, indent=2), flush=True)


def _discover_files(source_dir: Path) -> list[Path]:
    return sorted(
        [
            path
            for path in source_dir.rglob("*")
            if path.is_file() and path.suffix.lower() in SUPPORTED_SUFFIXES
        ],
        key=lambda path: path.relative_to(source_dir).as_posix().lower(),
    )


def _get_json(url: str) -> dict[str, Any]:
    with urllib.request.urlopen(url, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def _post_file(
    *,
    base_url: str,
    path: Path,
    document_id: str,
    output_path: Path,
) -> dict[str, Any]:
    body, content_type = _multipart_body(path, document_id)
    request = urllib.request.Request(
        f"{base_url}/v1/layout/analyze",
        data=body,
        headers={"Content-Type": content_type},
        method="POST",
    )
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=1800) as response:
            raw = response.read()
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        output_path.write_bytes(raw)
        return {
            "ok": False,
            "status_code": exc.code,
            "elapsed_seconds": round(time.perf_counter() - started, 3),
            "error": raw.decode("utf-8", errors="replace"),
        }

    elapsed = round(time.perf_counter() - started, 3)
    output_path.write_bytes(raw)
    data = json.loads(raw.decode("utf-8"))
    summary = _summarize_response(data)
    summary.update(
        {
            "ok": True,
            "status_code": 200,
            "elapsed_seconds": elapsed,
            "job_id": data.get("job_id"),
            "document_id": data.get("document_id"),
        }
    )
    return summary


def _multipart_body(path: Path, document_id: str) -> tuple[bytes, str]:
    boundary = f"----quantom-benchmark-{time.time_ns()}"
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


def _summarize_response(data: dict[str, Any]) -> dict[str, Any]:
    pages = data.get("pages") or []
    targets = data.get("targets") or {}
    ocr_targets = targets.get("ocr") or data.get("ocr_targets") or []
    image_targets = targets.get("image") or data.get("image_targets") or []
    table_targets = targets.get("table") or data.get("table_targets") or []

    canvas_ratios = []
    canvas_sizes = []
    ocr_input_sizes = []
    downscaled_canvas_count = 0
    canvas_source_counts: dict[str, int] = {}
    layout_region_count = 0
    clean_block_count = 0
    for page in pages:
        layout_region_count += len(page.get("layout_regions") or [])
        clean_block_count += len(page.get("clean_blocks") or [])
        canvas = page.get("ocr_text_canvas") or {}
        ratio = canvas.get("compression_ratio")
        if isinstance(ratio, (int, float)):
            canvas_ratios.append(float(ratio))
        size = canvas.get("canvas_size")
        if isinstance(size, list) and len(size) == 2:
            canvas_sizes.append(size)

    for target in ocr_targets:
        metadata = target.get("metadata") or {}
        ratio = metadata.get("compression_ratio")
        if isinstance(ratio, (int, float)):
            canvas_ratios.append(float(ratio))
        size = metadata.get("canvas_size")
        if isinstance(size, list) and len(size) == 2:
            canvas_sizes.append(size)
        ocr_input_size = metadata.get("ocr_input_size")
        if isinstance(ocr_input_size, list) and len(ocr_input_size) == 2:
            ocr_input_sizes.append(ocr_input_size)
        if (metadata.get("ocr_input_downscale") or {}).get("applied") is True:
            downscaled_canvas_count += 1
        for item in (
            metadata.get("canvas_region_map", [])
        ):
            source = item.get("source") or "unknown"
            canvas_source_counts[source] = canvas_source_counts.get(source, 0) + 1

    return {
        "pages": len(pages),
        "ocr_targets": len(ocr_targets),
        "image_targets": len(image_targets),
        "table_targets": len(table_targets),
        "layout_regions": layout_region_count,
        "clean_blocks": clean_block_count,
        "avg_canvas_compression_ratio": _avg(canvas_ratios),
        "min_canvas_compression_ratio": min(canvas_ratios) if canvas_ratios else None,
        "max_canvas_compression_ratio": max(canvas_ratios) if canvas_ratios else None,
        "canvas_sizes": canvas_sizes,
        "ocr_input_sizes": ocr_input_sizes,
        "downscaled_canvas_count": downscaled_canvas_count,
        "canvas_source_counts": canvas_source_counts,
    }


def _aggregate(
    run_label: str,
    health: dict[str, Any],
    warmup_summary: dict[str, Any] | None,
    summaries: list[dict[str, Any]],
    total_elapsed: float,
) -> dict[str, Any]:
    elapsed_values = [item["elapsed_seconds"] for item in summaries if item.get("ok")]
    pages = sum(item.get("pages", 0) for item in summaries)
    ocr_targets = sum(item.get("ocr_targets", 0) for item in summaries)
    image_targets = sum(item.get("image_targets", 0) for item in summaries)
    table_targets = sum(item.get("table_targets", 0) for item in summaries)
    downscaled_canvas_count = sum(item.get("downscaled_canvas_count", 0) for item in summaries)
    ratios = [
        item["avg_canvas_compression_ratio"]
        for item in summaries
        if isinstance(item.get("avg_canvas_compression_ratio"), (int, float))
    ]
    totals = {
        "run_label": run_label,
        "device": health.get("model_device"),
        "file_count": len(summaries),
        "successful_files": sum(1 for item in summaries if item.get("ok")),
        "failed_files": sum(1 for item in summaries if not item.get("ok")),
        "pages": pages,
        "ocr_targets": ocr_targets,
        "image_targets": image_targets,
        "table_targets": table_targets,
        "downscaled_canvas_count": downscaled_canvas_count,
        "sum_file_elapsed_seconds": round(sum(elapsed_values), 3),
        "wall_elapsed_seconds": total_elapsed,
        "avg_seconds_per_file": _avg(elapsed_values),
        "avg_seconds_per_page": round(sum(elapsed_values) / pages, 3) if pages else None,
        "avg_canvas_compression_ratio": _avg(ratios),
    }
    return {
        "health": health,
        "warmup": warmup_summary,
        "totals": totals,
        "files": summaries,
    }


def _avg(values: list[float]) -> float | None:
    if not values:
        return None
    return round(sum(values) / len(values), 6)


def _safe_name(value: str) -> str:
    cleaned = re.sub(r"[^0-9A-Za-z가-힣._-]+", "_", value).strip("._-")
    return cleaned or "file"


if __name__ == "__main__":
    main()
