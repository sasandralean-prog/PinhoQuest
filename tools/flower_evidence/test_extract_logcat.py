"""Unit tests for reconstructing images from instrumentation logcat messages."""

from __future__ import annotations

import base64
import contextlib
import hashlib
import io
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from extract_logcat import main


class ExtractLogcatTest(unittest.TestCase):
    def test_reconstructs_all_images_from_numbered_chunks(self) -> None:
        image_bytes = {
            "camellia-original.jpg": b"\xff\xd8\xffsynthetic-jpeg-fixture",
            "camellia-sprite.png": b"\x89PNG\r\n\x1a\nsynthetic-sprite-fixture",
            "camellia-original-vs-sprite.jpg": b"\xff\xd8\xffcomparison-fixture",
            "camellia-garden-collection-screen.jpg": b"\xff\xd8\xffscreenshot-fixture",
        }
        lines: list[str] = []
        for name, data in image_bytes.items():
            encoded = base64.b64encode(data).decode("ascii")
            # Split even small fixtures to exercise ordering and chunk reassembly.
            chunks = [encoded[i:i + 8] for i in range(0, len(encoded), 8)]
            digest = hashlib.sha256(data).hexdigest()
            lines.append(
                f"I/PqFlowerEvidence( 42): PQFLOWER|BEGIN|{name}|{len(chunks)}|{len(data)}|{digest}"
            )
            for index, chunk in reversed(list(enumerate(chunks))):
                lines.append(f"I/PqFlowerEvidence( 42): PQFLOWER|DATA|{name}|{index}|{chunk}")

        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            log_file = root / "logcat.txt"
            output_dir = root / "decoded"
            log_file.write_text("\n".join(lines), encoding="utf-8")
            argv = ["extract_logcat.py", str(log_file), str(output_dir)]
            with patch.object(sys, "argv", argv), contextlib.redirect_stdout(io.StringIO()):
                result = main()

            self.assertEqual(0, result)
            for name, expected in image_bytes.items():
                self.assertEqual(expected, (output_dir / name).read_bytes())

    def test_rejects_missing_chunks_and_bad_checksum(self) -> None:
        data = b"\x89PNG\r\n\x1a\nfixture"
        encoded = base64.b64encode(data).decode("ascii")
        digest = "0" * 64
        lines = [
            f"I/PqFlowerEvidence( 42): PQFLOWER|BEGIN|camellia-sprite.png|2|{len(data)}|{digest}",
            f"I/PqFlowerEvidence( 42): PQFLOWER|DATA|camellia-sprite.png|0|{encoded}",
        ]

        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            log_file = root / "logcat.txt"
            output_dir = root / "decoded"
            log_file.write_text("\n".join(lines), encoding="utf-8")
            argv = ["extract_logcat.py", str(log_file), str(output_dir)]
            with patch.object(sys, "argv", argv), contextlib.redirect_stderr(io.StringIO()):
                result = main()

            self.assertEqual(1, result)
            self.assertFalse((output_dir / "camellia-sprite.png").exists())


if __name__ == "__main__":
    unittest.main()
