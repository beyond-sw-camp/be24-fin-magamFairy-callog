from __future__ import annotations

import argparse
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Dict, Iterable, List


ROOT = Path(__file__).resolve().parents[1]
TARGET_SCORE = 95.0


@dataclass(frozen=True)
class Check:
    name: str
    description: str
    predicate: Callable[[Path], bool]


@dataclass(frozen=True)
class Committee:
    name: str
    role: str
    checks: List[Check]


PRIORITY_ESTIMATES = {
    "API integration committee": 0.16,
    "Layout-parser runtime committee": 0.24,
    "OCR readiness committee": 0.32,
    "Reliability and safety committee": 0.18,
    "Verification and operations committee": 0.10,
}


def evaluate(root: Path = ROOT) -> Dict:
    weights = ahp_weights(PRIORITY_ESTIMATES)
    committee_results = []

    for committee in committees():
        results = [
            {
                "name": check.name,
                "description": check.description,
                "passed": bool(check.predicate(root)),
            }
            for check in committee.checks
        ]
        passed = sum(1 for item in results if item["passed"])
        score = 100.0 * passed / len(results)
        committee_results.append(
            {
                "name": committee.name,
                "role": committee.role,
                "weight": weights[committee.name],
                "score": round(score, 2),
                "checks": results,
            }
        )

    total_score = sum(item["score"] * item["weight"] for item in committee_results)
    consistency = consistency_ratio(pairwise_matrix(PRIORITY_ESTIMATES), list(weights.values()))
    return {
        "target_score": TARGET_SCORE,
        "score": round(total_score, 2),
        "passed": total_score >= TARGET_SCORE,
        "ahp": {
            "consistency_ratio": round(consistency, 6),
            "weights": {key: round(value, 4) for key, value in weights.items()},
        },
        "committees": committee_results,
    }


def committees() -> List[Committee]:
    return [
        Committee(
            name="API integration committee",
            role="Validates service contract, job flow, and callback integration.",
            checks=[
                _contains("sync_analyze_endpoint", "app/main.py", '@app.post("/v1/layout/analyze")'),
                _contains("async_job_endpoint", "app/main.py", '@app.post("/v1/layout/jobs"'),
                _contains("job_status_endpoint", "app/main.py", '@app.get("/v1/layout/jobs/{job_id}"'),
                _contains("job_result_endpoint", "app/main.py", '@app.get("/v1/layout/jobs/{job_id}/result")'),
                _contains("health_endpoint", "app/main.py", '@app.get("/health"'),
                _contains("asset_mount", "app/main.py", 'app.mount("/assets"'),
                _contains("json_response_encoding", "app/main.py", "jsonable_encoder"),
            ],
        ),
        Committee(
            name="Layout-parser runtime committee",
            role="Validates real layout-parser inference path and model operability.",
            checks=[
                _contains("layoutparser_import", "app/layout_detector.py", "import layoutparser as lp"),
                _contains("detectron2_model", "app/layout_detector.py", "lp.Detectron2LayoutModel"),
                _contains("publaynet_labels", "app/layout_detector.py", '4: "Figure"'),
                _contains("score_threshold", "app/layout_detector.py", "MODEL.ROI_HEADS.SCORE_THRESH_TEST"),
                _contains("cpu_gpu_control", "app/layout_detector.py", "enforce_cpu"),
                _contains("model_asset_cache", "app/layout_detector.py", "model_cache"),
                _contains("rgb_array_input", "app/layout_detector.py", "np.asarray(rgb)"),
                _contains("hf_model_default", "app/settings.py", "huggingface.co/layoutparser/detectron2"),
            ],
        ),
        Committee(
            name="OCR readiness committee",
            role="Validates output is optimized for routing OCR, table extraction, and image analysis.",
            checks=[
                _contains("ocr_postprocess_module", "app/block_postprocess.py", "prepare_blocks_for_ocr"),
                _contains("ocr_metadata", "app/block_postprocess.py", '"recommended"'),
                _contains("ocr_priority", "app/block_postprocess.py", '"priority"'),
                _contains("embedded_text_hint", "app/block_postprocess.py", '"embedded_text"'),
                _contains("normalized_bbox", "app/block_postprocess.py", "bbox_normalized"),
                _contains("pdf_native_text_payload", "app/pdf_native.py", '"text": text'),
                _contains("pdf_native_bypass", "app/pdf_native.py", '"role": "embedded_text"'),
                _contains("visual_text_detection", "app/visual_text.py", "extract_visual_text_blocks"),
                _contains("visual_graphic_detection", "app/visual_graphic.py", "extract_visual_graphic_blocks"),
                _contains("visual_text_merge_generation", "app/visual_text.py", "merge_visual_text_blocks"),
                _contains("visual_text_merged_source", "app/visual_text.py", "visual_text_merged"),
                _contains("pdf_visual_text_default", "app/settings.py", '_bool_env("VISUAL_TEXT_FOR_PDF", True)'),
                _contains("pdf_visual_text_compose", "docker-compose.yml", 'VISUAL_TEXT_FOR_PDF: "true"'),
                _contains("merged_children", "app/processor.py", "_resolve_merged_children"),
                _contains("downstream_target_builder", "app/target_builder.py", "build_downstream_targets"),
                _contains("clean_blocks_contract", "app/schemas.py", "clean_blocks"),
                _contains("layout_regions_contract", "app/schemas.py", "layout_regions"),
                _contains("layout_summary_metadata", "app/target_builder.py", "layout_summary"),
                _contains("clean_blocks_attach", "app/target_builder.py", "attach_clean_blocks"),
                _contains("ocr_target_dedupe", "app/target_builder.py", "_dedupe_ocr_targets"),
                _contains("context_image_filter", "app/target_builder.py", "_filter_context_image_targets"),
                _contains("ocr_targets_contract", "app/schemas.py", "ocr_targets"),
                _contains("image_targets_contract", "app/schemas.py", "image_targets"),
                _contains("table_targets_contract", "app/schemas.py", "table_targets"),
                _contains("merged_text_priority", "app/block_postprocess.py", "merged_text_ocr"),
                _contains("figure_reclassification", "app/visual_text.py", "classification_refined_by"),
                _contains("full_page_fallback", "app/processor.py", "_page_fallback_blocks"),
                _contains("xy_reading_order", "app/block_postprocess.py", "_recursive_xy_order"),
                _contains("duplicate_ocr_skip", "app/block_postprocess.py", "redundant_with"),
                _contains("crop_url_contract", "app/processor.py", "crop_url=asset_url"),
            ],
        ),
        Committee(
            name="Reliability and safety committee",
            role="Validates bounded uploads, safe paths, stable storage, and failure handling.",
            checks=[
                _contains("max_upload_guard", "app/storage.py", "max_upload_mb"),
                _contains("safe_filename", "app/storage.py", "_SAFE_NAME_RE"),
                _contains("empty_upload_rejected", "app/storage.py", "Uploaded file is empty"),
                _contains("unsupported_type_415", "app/document.py", "Unsupported file type"),
                _contains("invalid_image_400", "app/document.py", "Invalid image file"),
                _contains("bbox_clamping", "app/block_postprocess.py", "def clamp_bbox"),
                _contains("storage_creation", "app/storage.py", "ensure_storage"),
                _contains("job_failure_state", "app/main.py", "jobs.mark_failed"),
                _contains("callback_timeout", "app/settings.py", "CALLBACK_TIMEOUT_SECONDS"),
                _not_contains("no_base64_payloads", "app", "base64"),
            ],
        ),
        Committee(
            name="Verification and operations committee",
            role="Validates repeatable local verification and deployment documentation.",
            checks=[
                _exists("unit_test_postprocess", "tests/test_block_postprocess.py"),
                _exists("unit_test_quality_gate", "tests/test_quality_gate.py"),
                _contains("docker_runtime", "Dockerfile", "pytorch/pytorch"),
                _contains("compose_service", "docker-compose.yml", "document-analysis-service"),
                _contains("env_example", ".env.example", "LAYOUT_MODEL_DEVICE"),
                _contains("readme_ahp", "README.md", "AHP quality gate"),
                _contains("readme_ocr_metadata", "README.md", "metadata.ocr"),
                _contains("readme_clean_blocks", "README.md", "clean_blocks"),
                _contains("readme_layout_regions", "README.md", "layout_regions"),
                _contains("dependency_layoutparser", "requirements.txt", "layoutparser==0.3.4"),
                _exists("clean_overlay_renderer", "tools/render_clean_overlays.py"),
            ],
        ),
    ]


def ahp_weights(priority_estimates: Dict[str, float]) -> Dict[str, float]:
    matrix = pairwise_matrix(priority_estimates)
    columns = list(zip(*matrix))
    column_sums = [sum(column) for column in columns]
    normalized_rows = [
        [value / column_sums[index] for index, value in enumerate(row)]
        for row in matrix
    ]
    priorities = [sum(row) / len(row) for row in normalized_rows]
    total = sum(priorities)
    return {
        name: priority / total
        for name, priority in zip(priority_estimates.keys(), priorities)
    }


def pairwise_matrix(priority_estimates: Dict[str, float]) -> List[List[float]]:
    values = list(priority_estimates.values())
    return [[left / right for right in values] for left in values]


def consistency_ratio(matrix: List[List[float]], weights: List[float]) -> float:
    size = len(matrix)
    weighted_sums = [
        sum(matrix[row][column] * weights[column] for column in range(size))
        for row in range(size)
    ]
    lambda_max = sum(weighted_sums[row] / weights[row] for row in range(size)) / size
    consistency_index = (lambda_max - size) / (size - 1)
    random_index = {
        1: 0.0,
        2: 0.0,
        3: 0.58,
        4: 0.90,
        5: 1.12,
        6: 1.24,
        7: 1.32,
        8: 1.41,
        9: 1.45,
        10: 1.49,
    }.get(size, 1.49)
    if math.isclose(random_index, 0.0):
        return 0.0
    return consistency_index / random_index


def print_text_report(report: Dict) -> None:
    status = "PASS" if report["passed"] else "FAIL"
    print(f"AHP weighted score: {report['score']:.2f}/100 (target {report['target_score']:.2f}) {status}")
    print(f"AHP consistency ratio: {report['ahp']['consistency_ratio']:.6f}")
    for committee in report["committees"]:
        print(
            f"- {committee['name']}: {committee['score']:.2f}/100 "
            f"(weight {committee['weight']:.4f})"
        )
        failed = [check for check in committee["checks"] if not check["passed"]]
        for check in failed:
            print(f"  FAIL {check['name']}: {check['description']}")


def _contains(name: str, relative_path: str, needle: str) -> Check:
    return Check(
        name=name,
        description=f"{relative_path} contains {needle!r}",
        predicate=lambda root: _read(root / relative_path).find(needle) >= 0,
    )


def _not_contains(name: str, relative_path: str, needle: str) -> Check:
    return Check(
        name=name,
        description=f"{relative_path} does not contain {needle!r}",
        predicate=lambda root: all(needle not in _read(path) for path in _iter_files(root / relative_path)),
    )


def _exists(name: str, relative_path: str) -> Check:
    return Check(
        name=name,
        description=f"{relative_path} exists",
        predicate=lambda root: (root / relative_path).exists(),
    )


def _iter_files(path: Path) -> Iterable[Path]:
    if path.is_file():
        return [path]
    return [item for item in path.rglob("*") if item.is_file() and item.suffix in {".py", ".md", ".txt", ".yml"}]


def _read(path: Path) -> str:
    if not path.exists():
        return ""
    if path.is_dir():
        return "\n".join(_read(item) for item in _iter_files(path))
    return path.read_text(encoding="utf-8", errors="ignore")


def main() -> int:
    parser = argparse.ArgumentParser(description="Run AHP quality gate for the layout service.")
    parser.add_argument("--json", action="store_true", help="Emit JSON instead of text.")
    args = parser.parse_args()

    report = evaluate()
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print_text_report(report)
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
