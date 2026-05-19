from __future__ import annotations

import os
import asyncio
import tempfile
import time
import unittest
from importlib.util import find_spec
from pathlib import Path
from typing import List


if find_spec("fastapi") is not None:
    from app.ocr_engine import (
        OCR_TEMP_DIR_PREFIX,
        _write_upload_to_path,
        cleanup_stale_ocr_temp_dirs,
    )
else:
    OCR_TEMP_DIR_PREFIX = "callog-ocr-"
    _write_upload_to_path = None
    cleanup_stale_ocr_temp_dirs = None


class FakeUpload:
    def __init__(self, chunks: List[bytes]) -> None:
        self._chunks = list(chunks)
        self.closed = False

    async def read(self, _size: int) -> bytes:
        if not self._chunks:
            return b""
        return self._chunks.pop(0)

    async def close(self) -> None:
        self.closed = True


class OcrResourceManagementTests(unittest.TestCase):
    @unittest.skipIf(_write_upload_to_path is None, "fastapi is not installed in the local test runner")
    def test_upload_is_streamed_to_temp_file(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir_name:
            target = Path(temp_dir_name) / "upload.png"
            upload = FakeUpload([b"hello", b"-", b"ocr"])

            loop = asyncio.get_event_loop()
            size = loop.run_until_complete(_write_upload_to_path(upload, target))

            self.assertEqual(size, 9)
            self.assertEqual(target.read_bytes(), b"hello-ocr")

    @unittest.skipIf(cleanup_stale_ocr_temp_dirs is None, "fastapi is not installed in the local test runner")
    def test_cleanup_removes_only_stale_ocr_temp_dirs(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir_name:
            root = Path(temp_dir_name)
            stale = root / f"{OCR_TEMP_DIR_PREFIX}stale"
            fresh = root / f"{OCR_TEMP_DIR_PREFIX}fresh"
            other = root / "other-temp"
            stale.mkdir()
            fresh.mkdir()
            other.mkdir()

            stale_time = time.time() - 3600
            os.utime(stale, (stale_time, stale_time))

            removed = cleanup_stale_ocr_temp_dirs(max_age_seconds=60, roots=[root])

            self.assertEqual(removed, 1)
            self.assertFalse(stale.exists())
            self.assertTrue(fresh.exists())
            self.assertTrue(other.exists())


if __name__ == "__main__":
    unittest.main()
