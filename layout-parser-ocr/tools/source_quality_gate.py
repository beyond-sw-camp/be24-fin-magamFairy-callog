from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Dict, Iterable, List, Tuple


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_RESULTS_DIR = ROOT / "storage" / "source_validation_final"
TARGET_SCORE = 95.0


def evaluate(results_dir: Path = DEFAULT_RESULTS_DIR) -> Dict:
    documents = [_evaluate_document(path) for path in sorted(results_dir.glob("*.json"))]
    if not documents:
        return {
            "target_score": TARGET_SCORE,
            "score": 0.0,
            "passed": False,
            "documents": [],
            "reason": "No validation result JSON files found.",
        }

    score = sum(document["score"] for document in documents) / len(documents)
    return {
        "target_score": TARGET_SCORE,
        "score": round(score, 2),
        "passed": score >= TARGET_SCORE,
        "documents": documents,
    }


def _evaluate_document(path: Path) -> Dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    pages = list(data.get("pages", []))
    blocks = [block for page in pages for block in page.get("blocks", [])]
    clean_blocks = [block for page in pages for block in page.get("clean_blocks", [])]
    layout_regions = [region for page in pages for region in page.get("layout_regions", [])]
    sources = Counter(block.get("metadata", {}).get("source", "") for block in blocks)
    hints = Counter(block.get("analysis_hint", "") for block in blocks)
    types = Counter(block.get("type", "") for block in blocks)

    checks = [
        _check_parse_complete(data, pages, blocks),
        _check_block_contract(pages, blocks),
        _check_downstream_targets(data),
        _check_clean_blocks(data, pages, blocks, clean_blocks),
        _check_layout_regions(pages, clean_blocks, layout_regions),
        _check_crop_assets_exist(blocks),
        _check_page_coverage(pages, sources),
        _check_text_routing(blocks, sources, hints),
        _check_visual_text_merge(path, sources),
        _check_graphic_routing(path, types, hints),
        _check_large_visual_text_noise(clean_blocks),
    ]

    weighted_score = sum(score * weight for _, score, weight, _ in checks) / sum(
        weight for _, _, weight, _ in checks
    )
    return {
        "file": path.name,
        "job_id": data.get("job_id"),
        "pages": len(pages),
        "blocks": len(blocks),
        "clean_blocks": len(clean_blocks),
        "layout_regions": len(layout_regions),
        "score": round(weighted_score, 2),
        "passed": weighted_score >= TARGET_SCORE,
        "source_counts": dict(sources),
        "type_counts": dict(types),
        "analysis_hints": dict(hints),
        "checks": [
            {
                "name": name,
                "score": round(score, 2),
                "weight": weight,
                "details": details,
            }
            for name, score, weight, details in checks
        ],
    }


def _check_parse_complete(data: Dict, pages: List[Dict], blocks: List[Dict]) -> Tuple[str, float, int, str]:
    ok = data.get("status") == "completed" and bool(pages) and bool(blocks)
    return (
        "parse_complete",
        100.0 if ok else 0.0,
        20,
        f"status={data.get('status')} pages={len(pages)} blocks={len(blocks)}",
    )


def _check_block_contract(pages: List[Dict], blocks: List[Dict]) -> Tuple[str, float, int, str]:
    required = ["id", "page", "type", "bbox", "score", "reading_order", "analysis_hint", "crop_url", "metadata"]
    invalid = []
    for block in blocks:
        missing = [field for field in required if field not in block]
        bbox = block.get("bbox", [])
        metadata = block.get("metadata", {})
        if missing or len(bbox) != 4 or "bbox_normalized" not in metadata or "ocr" not in metadata:
            invalid.append(block.get("id", "<unknown>"))
    score = 100.0 if not invalid else max(0.0, 100.0 - len(invalid) * 8.0)
    return (
        "block_contract",
        score,
        15,
        f"invalid_blocks={invalid[:10]} total_invalid={len(invalid)}",
    )


def _check_downstream_targets(data: Dict) -> Tuple[str, float, int, str]:
    ocr_targets = list(data.get("ocr_targets", []))
    image_targets = list(data.get("image_targets", []))
    table_targets = list(data.get("table_targets", []))
    required = ["id", "target_type", "route", "block_id", "page", "priority", "crop_url", "bbox"]
    invalid = []
    for target in ocr_targets + image_targets + table_targets:
        missing = [field for field in required if field not in target]
        if missing:
            invalid.append({"id": target.get("id"), "missing": missing})

    routes_ok = all(target.get("route") == "ocr_server" for target in ocr_targets)
    routes_ok = routes_ok and all(target.get("route") == "image_analysis_server" for target in image_targets)
    routes_ok = routes_ok and all(target.get("route") == "table_extraction_server" for target in table_targets)
    has_work = bool(ocr_targets) and bool(image_targets)
    score = 100.0 if not invalid and routes_ok and has_work else 70.0
    return (
        "downstream_targets",
        score,
        20,
        f"ocr={len(ocr_targets)} image={len(image_targets)} table={len(table_targets)} "
        f"routes_ok={routes_ok} invalid={invalid[:5]}",
    )


def _check_clean_blocks(
    data: Dict,
    pages: List[Dict],
    blocks: List[Dict],
    clean_blocks: List[Dict],
) -> Tuple[str, float, int, str]:
    target_block_ids = {
        target.get("block_id")
        for key in ("ocr_targets", "image_targets", "table_targets")
        for target in data.get(key, [])
    }
    target_block_ids.update(
        child_id
        for key in ("ocr_targets", "image_targets", "table_targets")
        for target in data.get(key, [])
        for child_id in target.get("child_block_ids", [])
    )
    clean_ids = {block.get("id") for block in clean_blocks}
    missing_pages = [
        page.get("page")
        for page in pages
        if not page.get("clean_blocks")
    ]
    outside_targets = sorted(str(block_id) for block_id in clean_ids if block_id not in target_block_ids)
    severe_overlaps = _severe_clean_overlaps(pages)
    raw_count = max(1, len(blocks))
    clean_ratio = len(clean_blocks) / raw_count

    score = 100.0
    if missing_pages:
        score -= 70.0
    if outside_targets:
        score -= min(25.0, len(outside_targets) * 5.0)
    if len(blocks) >= 10 and len(clean_blocks) >= len(blocks):
        score -= 30.0
    elif len(blocks) >= 10 and clean_ratio > 0.85:
        score -= 10.0
    if severe_overlaps:
        score -= min(45.0, len(severe_overlaps) * 8.0)

    return (
        "clean_blocks_contract",
        max(0.0, score),
        25,
        f"clean={len(clean_blocks)} raw={len(blocks)} ratio={clean_ratio:.2f} "
        f"missing_pages={missing_pages} outside_targets={outside_targets[:10]} "
        f"severe_overlaps={severe_overlaps[:8]} total_severe={len(severe_overlaps)}",
    )


def _check_layout_regions(
    pages: List[Dict],
    clean_blocks: List[Dict],
    layout_regions: List[Dict],
) -> Tuple[str, float, int, str]:
    required = ["id", "block_id", "region_type", "route", "bbox", "reading_order", "crop_url", "layout_role"]
    invalid = []
    for region in layout_regions:
        missing = [field for field in required if field not in region]
        if missing:
            invalid.append({"id": region.get("id"), "missing": missing})

    missing_summaries = [
        page.get("page")
        for page in pages
        if "layout_summary" not in page.get("metadata", {})
    ]
    clean_ids = {block.get("id") for block in clean_blocks}
    region_block_ids = {region.get("block_id") for region in layout_regions}
    missing_regions = sorted(str(block_id) for block_id in clean_ids if block_id not in region_block_ids)

    score = 100.0
    if len(layout_regions) != len(clean_blocks):
        score -= 35.0
    if invalid:
        score -= min(35.0, len(invalid) * 5.0)
    if missing_summaries:
        score -= 20.0
    if missing_regions:
        score -= min(25.0, len(missing_regions) * 5.0)

    return (
        "layout_regions_contract",
        max(0.0, score),
        20,
        f"regions={len(layout_regions)} clean={len(clean_blocks)} invalid={invalid[:5]} "
        f"missing_summaries={missing_summaries} missing_regions={missing_regions[:10]}",
    )


def _check_crop_assets_exist(blocks: List[Dict]) -> Tuple[str, float, int, str]:
    missing = []
    for block in blocks:
        crop_path = block.get("crop_path")
        if not crop_path:
            missing.append(block.get("id", "<unknown>"))
            continue
        if not (ROOT / "storage" / "results" / crop_path).exists():
            missing.append(block.get("id", "<unknown>"))
    score = 100.0 if not missing else max(0.0, 100.0 - len(missing) * 5.0)
    return (
        "crop_assets",
        score,
        15,
        f"missing_assets={missing[:10]} total_missing={len(missing)}",
    )


def _check_page_coverage(pages: List[Dict], sources: Counter) -> Tuple[str, float, int, str]:
    has_page_guard = sources.get("page_fallback", 0) >= len(pages)
    return (
        "page_coverage_guard",
        100.0 if has_page_guard else 40.0,
        10,
        f"page_fallback={sources.get('page_fallback', 0)} pages={len(pages)}",
    )


def _check_text_routing(blocks: List[Dict], sources: Counter, hints: Counter) -> Tuple[str, float, int, str]:
    ocr_recommended = sum(
        1 for block in blocks if block.get("metadata", {}).get("ocr", {}).get("recommended") is True
    )
    has_visual_or_native_text = sources.get("visual_text", 0) > 0 or sources.get("pdf_native", 0) > 0
    has_ocr_or_embedded = hints.get("ocr", 0) > 0 or hints.get("embedded_text", 0) > 0
    ok = ocr_recommended > 0 and has_visual_or_native_text and has_ocr_or_embedded
    return (
        "text_routing",
        100.0 if ok else 50.0,
        15,
        f"ocr_recommended={ocr_recommended} visual_text={sources.get('visual_text', 0)} "
        f"pdf_native={sources.get('pdf_native', 0)} hints={dict(hints)}",
    )


def _check_visual_text_merge(path: Path, sources: Counter) -> Tuple[str, float, int, str]:
    merged = sources.get("visual_text_merged", 0)
    visual = sources.get("visual_text", 0)
    if path.suffix.lower() != ".json":
        return ("visual_text_merge", 0.0, 10, "unexpected extension")
    expected = path.stem != "B2B" or visual >= 20
    ok = merged > 0 if expected else True
    return (
        "visual_text_merge",
        100.0 if ok else 70.0,
        10,
        f"visual_text={visual} visual_text_merged={merged}",
    )


def _check_graphic_routing(path: Path, types: Counter, hints: Counter) -> Tuple[str, float, int, str]:
    has_graphic_route = types.get("Figure", 0) > 0 or hints.get("figure", 0) > 0 or types.get("Table", 0) > 0
    return (
        "graphic_table_routing",
        100.0 if has_graphic_route else 75.0,
        10,
        f"types={dict(types)} hints={dict(hints)} source={path.name}",
    )


def _check_large_visual_text_noise(blocks: List[Dict]) -> Tuple[str, float, int, str]:
    large = [
        {
            "id": block.get("id"),
            "area_ratio": block.get("metadata", {}).get("area_ratio"),
        }
        for block in blocks
        if block.get("metadata", {}).get("source") == "visual_text"
        and float(block.get("metadata", {}).get("area_ratio", 0.0)) >= 0.18
    ]
    suspicious = [
        {
            "id": block.get("id"),
            "area_ratio": block.get("metadata", {}).get("area_ratio"),
        }
        for block in blocks
        if block.get("metadata", {}).get("source") == "visual_text"
        and float(block.get("metadata", {}).get("area_ratio", 0.0)) >= 0.08
    ]
    if large:
        score = 60.0
    elif len(suspicious) <= 1:
        score = 100.0
    else:
        score = max(80.0, 100.0 - (len(suspicious) - 1) * 5.0)
    return (
        "large_visual_text_noise",
        score,
        5,
        f"large={large} suspicious={suspicious}",
    )


def _severe_clean_overlaps(pages: List[Dict]) -> List[Dict]:
    severe = []
    for page in pages:
        clean_blocks = list(page.get("clean_blocks", []))
        for left_index, left in enumerate(clean_blocks):
            for right in clean_blocks[left_index + 1 :]:
                left_type = str(left.get("type", "")).lower()
                right_type = str(right.get("type", "")).lower()
                if "page" in {left_type, right_type}:
                    continue

                left_area = _area(left.get("bbox", []))
                right_area = _area(right.get("bbox", []))
                if left_area <= 0 or right_area <= 0:
                    continue

                overlap = _intersection_area(left.get("bbox", []), right.get("bbox", []))
                if overlap <= 0:
                    continue

                overlap_min = overlap / min(left_area, right_area)
                overlap_left = overlap / left_area
                overlap_right = overlap / right_area
                overlap_max = overlap / max(left_area, right_area)
                left_region = _region_type(left)
                right_region = _region_type(right)
                if left_region != right_region and overlap_max < 0.35:
                    continue
                if overlap_min < 0.55 and overlap_left < 0.70 and overlap_right < 0.70:
                    continue

                severe.append(
                    {
                        "page": page.get("page"),
                        "left": left.get("id"),
                        "right": right.get("id"),
                        "overlap_min": round(overlap_min, 3),
                    }
                )
    return severe


def _region_type(block: Dict) -> str:
    block_type = str(block.get("type", "")).lower()
    hint = str(block.get("analysis_hint", "")).lower()
    if hint == "figure" or block_type == "figure":
        return "image_region"
    if hint == "table" or block_type == "table":
        return "table_region"
    if hint in {"ocr", "embedded_text"} or block_type in {"text", "title", "list"}:
        return "text_region"
    return "layout_region"


def _area(box: List[float]) -> float:
    if len(box) != 4:
        return 0.0
    return max(0.0, float(box[2]) - float(box[0])) * max(0.0, float(box[3]) - float(box[1]))


def _intersection_area(left: List[float], right: List[float]) -> float:
    if len(left) != 4 or len(right) != 4:
        return 0.0
    x1 = max(float(left[0]), float(right[0]))
    y1 = max(float(left[1]), float(right[1]))
    x2 = min(float(left[2]), float(right[2]))
    y2 = min(float(left[3]), float(right[3]))
    return max(0.0, x2 - x1) * max(0.0, y2 - y1)


def print_report(report: Dict) -> None:
    status = "PASS" if report["passed"] else "FAIL"
    print(f"Source dataset quality score: {report['score']:.2f}/100 target={report['target_score']:.2f} {status}")
    for document in report["documents"]:
        doc_status = "PASS" if document["passed"] else "FAIL"
        print(
            f"- {document['file']}: {document['score']:.2f}/100 {doc_status} "
            f"pages={document['pages']} blocks={document['blocks']} "
            f"clean={document['clean_blocks']} layout={document['layout_regions']}"
        )
        for check in document["checks"]:
            if check["score"] < 100.0:
                print(f"  {check['name']}: {check['score']:.2f} {check['details']}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Evaluate parser output quality on the source dataset.")
    parser.add_argument(
        "--results-dir",
        type=Path,
        default=DEFAULT_RESULTS_DIR,
        help="Directory containing parser response JSON files.",
    )
    parser.add_argument("--json", action="store_true", help="Emit JSON instead of text.")
    args = parser.parse_args()

    report = evaluate(args.results_dir)
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print_report(report)
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
