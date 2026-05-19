from __future__ import annotations

import unittest
from importlib.util import find_spec


if find_spec("pydantic") is not None:
    from app.schemas import BlockResult, PageResult
    from app.target_builder import attach_clean_blocks, build_downstream_targets
else:
    BlockResult = None
    PageResult = None
    attach_clean_blocks = None
    build_downstream_targets = None


def block(
    block_id: str,
    block_type: str,
    source: str,
    analysis_hint: str,
    priority: int,
    reading_order: int,
    *,
    children=None,
    area_ratio: float = 0.01,
    bbox=None,
) -> BlockResult:
    metadata = {
        "source": source,
        "bbox_normalized": [0.1, 0.1, 0.2, 0.2],
        "area_ratio": area_ratio,
        "ocr": {
            "recommended": analysis_hint in {"ocr", "table"},
            "priority": priority,
            "mode": "text_ocr" if analysis_hint == "ocr" else "table_ocr",
        },
    }
    if children is not None:
        metadata["children"] = children

    return BlockResult(
        id=block_id,
        page=1,
        type=block_type,
        bbox=bbox or [10.0, 10.0, 100.0, 100.0],
        score=0.9,
        reading_order=reading_order,
        analysis_hint=analysis_hint,
        crop_path=f"job/crops/{block_id}.png",
        crop_url=f"http://localhost/assets/job/crops/{block_id}.png",
        metadata=metadata,
    )


class TargetBuilderTests(unittest.TestCase):
    @unittest.skipIf(find_spec("pydantic") is None, "pydantic is not installed in the local test runner")
    def test_builds_minimal_downstream_targets(self) -> None:
        page = PageResult(
            page=1,
            width=1000,
            height=1000,
            image_path="job/pages/page-001.png",
            image_url="http://localhost/assets/job/pages/page-001.png",
            blocks=[
                block("p1-b1", "Page", "page_fallback", "full_page", 5, 1),
                block(
                    "p1-b2",
                    "Text",
                    "visual_text_merged",
                    "ocr",
                    100,
                    2,
                    children=["p1-b3", "p1-b4"],
                ),
                block("p1-b3", "Text", "visual_text", "ocr", 95, 3),
                block("p1-b4", "Text", "visual_text", "ocr", 95, 4),
                block(
                    "p1-b5",
                    "Text",
                    "visual_text",
                    "ocr",
                    95,
                    5,
                    bbox=[200.0, 200.0, 300.0, 260.0],
                ),
                block(
                    "p1-b6",
                    "Table",
                    "layout_parser",
                    "table",
                    70,
                    6,
                    bbox=[320.0, 320.0, 460.0, 430.0],
                ),
                block(
                    "p1-b7",
                    "Figure",
                    "layout_parser",
                    "figure",
                    25,
                    7,
                    area_ratio=0.12,
                    bbox=[520.0, 520.0, 760.0, 760.0],
                ),
                block("p1-b8", "Figure", "pdf_native", "figure", 25, 8, area_ratio=0.001),
            ],
        )

        targets = build_downstream_targets([page])
        attach_clean_blocks([page], targets)

        self.assertEqual([target.block_id for target in targets["ocr_targets"]], ["p1-b2", "p1-b5"])
        self.assertEqual(targets["ocr_targets"][0].id, "ocr-0001")
        self.assertEqual(targets["ocr_targets"][0].route, "ocr_server")
        self.assertEqual(targets["ocr_targets"][0].child_block_ids, ["p1-b3", "p1-b4"])
        self.assertEqual([target.block_id for target in targets["table_targets"]], ["p1-b6"])
        self.assertEqual(targets["table_targets"][0].route, "table_extraction_server")
        self.assertEqual([target.block_id for target in targets["image_targets"]], ["p1-b7"])
        self.assertEqual(targets["image_targets"][0].route, "image_analysis_server")
        self.assertEqual(
            [region["region_type"] for region in page.layout_regions],
            ["text_region", "text_region", "table_region", "image_region"],
        )
        self.assertEqual(page.metadata["layout_summary"]["route_counts"]["ocr_server"], 2)

    @unittest.skipIf(find_spec("pydantic") is None, "pydantic is not installed in the local test runner")
    def test_clean_blocks_exclude_overlapping_image_context(self) -> None:
        page = PageResult(
            page=1,
            width=1000,
            height=1000,
            image_path="job/pages/page-001.png",
            image_url="http://localhost/assets/job/pages/page-001.png",
            blocks=[
                block("p1-b1", "Text", "visual_text", "ocr", 95, 1),
                block("p1-b2", "Text", "visual_text", "ocr", 95, 2),
                block("p1-b3", "Figure", "layout_parser", "figure", 25, 3, area_ratio=0.70),
            ],
        )
        page.blocks[0].bbox = [100.0, 100.0, 400.0, 200.0]
        page.blocks[1].bbox = [100.0, 240.0, 400.0, 340.0]
        page.blocks[2].bbox = [0.0, 0.0, 800.0, 800.0]

        targets = build_downstream_targets([page])
        attach_clean_blocks([page], targets)

        self.assertEqual([block.id for block in page.clean_blocks], ["p1-b1", "p1-b2"])
        self.assertEqual(targets["image_targets"], [])

    @unittest.skipIf(find_spec("pydantic") is None, "pydantic is not installed in the local test runner")
    def test_ocr_targets_prefer_semantic_layout_over_contained_visual_text(self) -> None:
        page = PageResult(
            page=1,
            width=1000,
            height=1000,
            image_path="job/pages/page-001.png",
            image_url="http://localhost/assets/job/pages/page-001.png",
            blocks=[
                block(
                    "p1-b1",
                    "Title",
                    "layout_parser",
                    "ocr",
                    80,
                    1,
                    bbox=[90.0, 80.0, 700.0, 170.0],
                ),
                block(
                    "p1-b2",
                    "Text",
                    "visual_text_merged",
                    "ocr",
                    100,
                    2,
                    bbox=[95.0, 82.0, 690.0, 168.0],
                ),
                block(
                    "p1-b3",
                    "Text",
                    "visual_text",
                    "ocr",
                    95,
                    3,
                    bbox=[100.0, 300.0, 500.0, 360.0],
                ),
            ],
        )

        targets = build_downstream_targets([page])

        self.assertEqual([target.block_id for target in targets["ocr_targets"]], ["p1-b1", "p1-b3"])

    @unittest.skipIf(find_spec("pydantic") is None, "pydantic is not installed in the local test runner")
    def test_visual_text_inside_graphic_routes_to_image_target(self) -> None:
        page = PageResult(
            page=1,
            width=1000,
            height=1000,
            image_path="job/pages/page-001.png",
            image_url="http://localhost/assets/job/pages/page-001.png",
            blocks=[
                block(
                    "p1-b1",
                    "Text",
                    "visual_text",
                    "ocr",
                    95,
                    1,
                    bbox=[610.0, 620.0, 930.0, 800.0],
                ),
                block(
                    "p1-b2",
                    "Figure",
                    "visual_graphic",
                    "figure",
                    25,
                    2,
                    area_ratio=0.12,
                    bbox=[580.0, 590.0, 960.0, 850.0],
                ),
            ],
        )

        targets = build_downstream_targets([page])
        attach_clean_blocks([page], targets)

        self.assertEqual(targets["ocr_targets"], [])
        self.assertEqual([target.block_id for target in targets["image_targets"]], ["p1-b2"])
        self.assertEqual([region["region_type"] for region in page.layout_regions], ["image_region"])

    @unittest.skipIf(find_spec("pydantic") is None, "pydantic is not installed in the local test runner")
    def test_can_suppress_image_targets_while_using_them_as_ocr_filter(self) -> None:
        page = PageResult(
            page=1,
            width=1000,
            height=1000,
            image_path="job/pages/page-001.png",
            image_url="http://localhost/assets/job/pages/page-001.png",
            blocks=[
                block(
                    "p1-b1",
                    "Text",
                    "visual_text",
                    "ocr",
                    95,
                    1,
                    bbox=[610.0, 620.0, 930.0, 800.0],
                ),
                block(
                    "p1-b2",
                    "Figure",
                    "visual_graphic",
                    "figure",
                    25,
                    2,
                    area_ratio=0.12,
                    bbox=[580.0, 590.0, 960.0, 850.0],
                ),
            ],
        )

        targets = build_downstream_targets([page], include_image_targets=False)
        attach_clean_blocks([page], targets)

        self.assertEqual(targets["ocr_targets"], [])
        self.assertEqual(targets["image_targets"], [])
        self.assertEqual(page.clean_blocks, [])


if __name__ == "__main__":
    unittest.main()
