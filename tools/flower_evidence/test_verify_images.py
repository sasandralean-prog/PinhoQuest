"""Tests for validating floral evidence files and checksums."""
from __future__ import annotations
import contextlib, hashlib, io, json, struct, sys, tempfile, unittest, zlib
from pathlib import Path
from unittest.mock import patch
from verify_images import main, inspect_png, inspect_jpeg

def png_fixture() -> bytes:
    def chunk(kind: bytes, payload: bytes) -> bytes:
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xffffffff)
    ihdr = struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0)
    idat = zlib.compress(b"\x00\x00\x00\x00\x00")
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")

def jpeg_fixture() -> bytes:
    sof_payload = bytes([8, 0, 1, 0, 1, 3, 1, 0x11, 0, 2, 0x11, 0, 3, 0x11, 0])
    sof = b"\xff\xc0" + struct.pack(">H", len(sof_payload) + 2) + sof_payload
    sos_payload = bytes([3, 1, 0, 2, 0x11, 3, 0x11, 0, 0x3f, 0])
    sos = b"\xff\xda" + struct.pack(">H", len(sos_payload) + 2) + sos_payload
    return b"\xff\xd8" + sof + sos + b"\x00\x11\x22\x33" + b"\xff\xd9"

class VerifyFlowerEvidenceTest(unittest.TestCase):
    def test_inspects_png_crc_and_jpeg_frame_dimensions(self) -> None:
        self.assertEqual((1, 1), inspect_png(png_fixture()))
        self.assertEqual((1, 1), inspect_jpeg(jpeg_fixture()))
        damaged = bytearray(png_fixture())
        damaged[29] ^= 1
        with self.assertRaises(ValueError): inspect_png(bytes(damaged))

    def test_writes_manifest_for_all_expected_files(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            for name in ("camellia-original.jpg", "camellia-original-vs-sprite.jpg", "camellia-garden-collection-screen.jpg"):
                (root / name).write_bytes(jpeg_fixture())
            (root / "camellia-sprite.png").write_bytes(png_fixture())
            with patch.object(sys, "argv", ["verify_images.py", str(root)]), contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, main())
            manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
            self.assertEqual(4, len(manifest["images"]))
            for item in manifest["images"]:
                self.assertEqual(hashlib.sha256((root / item["file"]).read_bytes()).hexdigest(), item["sha256"])

    def test_fails_on_missing_or_invalid_images(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "camellia-original.jpg").write_bytes(b"not a JPEG")
            with patch.object(sys, "argv", ["verify_images.py", str(root)]), contextlib.redirect_stderr(io.StringIO()):
                self.assertEqual(1, main())
            self.assertFalse((root / "manifest.json").exists())

if __name__ == "__main__": unittest.main()
