from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_RESULTS_DIR = ROOT / "storage" / "source_validation_final"

COLORS: Dict[str, Tuple[int, int, int]] = {
    "Text": (220, 30, 30),
    "Title": (220, 30, 30),
    "List": (220, 90, 20),
    "Figure": (20, 110, 255),
    "Table": (0, 150, 150),
    "Page": (120, 120, 120),
}


def render_overlays(results_dir: Path = DEFAULT_RESULTS_DIR, block_field: str = "clean_blocks") -> List[Path]:
    output_dir = results_dir / f"{block_field}_overlays"
    output_dir.mkdir(parents=True, exist_ok=True)

    rendered: List[Path] = []
    for result_path in sorted(results_dir.glob("*.json")):
        data = json.loads(result_path.read_text(encoding="utf-8"))
        for page in data.get("pages", []):
            image_path = ROOT / "storage" / "results" / page.get("image_path", "")
            if not image_path.exists():
                continue

            with Image.open(image_path) as image:
                overlay = image.convert("RGB")
                draw = ImageDraw.Draw(overlay)
                blocks = list(page.get(block_field, []))
                _draw_blocks(draw, blocks)
                output_path = output_dir / f"{result_path.stem}_page-{int(page.get('page', 1)):03d}.png"
                overlay.save(output_path)
                rendered.append(output_path)
    return rendered


def _draw_blocks(draw: ImageDraw.ImageDraw, blocks: Iterable[Dict]) -> None:
    font = ImageFont.load_default()
    for block in blocks:
        bbox = [float(value) for value in block.get("bbox", [])]
        if len(bbox) != 4:
            continue

        block_type = str(block.get("type", "Block"))
        source = str(block.get("metadata", {}).get("source", "unknown"))
        color = COLORS.get(block_type, (30, 180, 60))
        width = 3 if source in {"visual_text_merged", "layout_parser"} else 2
        draw.rectangle(bbox, outline=color, width=width)

        label = f"{block.get('id')} {block_type} {source}"
        label_bbox = draw.textbbox((bbox[0], bbox[1]), label, font=font)
        pad = 2
        background = (
            label_bbox[0] - pad,
            label_bbox[1] - pad,
            label_bbox[2] + pad,
            label_bbox[3] + pad,
        )
        draw.rectangle(background, fill=(255, 255, 255))
        draw.text((bbox[0], bbox[1]), label, fill=color, font=font)


def main() -> int:
    parser = argparse.ArgumentParser(description="Render clean non-overlapping parser overlays.")
    parser.add_argument(
        "--results-dir",
        type=Path,
        default=DEFAULT_RESULTS_DIR,
        help="Directory containing parser response JSON files.",
    )
    parser.add_argument(
        "--block-field",
        choices=("clean_blocks", "blocks"),
        default="clean_blocks",
        help="Page block field to draw.",
    )
    args = parser.parse_args()

    paths = render_overlays(args.results_dir, args.block_field)
    print(f"Rendered {len(paths)} overlays to {args.results_dir / (args.block_field + '_overlays')}")
    for path in paths:
        print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
