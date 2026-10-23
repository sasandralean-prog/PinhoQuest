#!/usr/bin/env python3
"""Validate exported flower evidence and write a SHA-256 manifest."""
from __future__ import annotations
import hashlib, json, sys, zlib
from pathlib import Path
EXPECTED = {"camellia-original.jpg": "jpeg", "camellia-sprite.png": "png", "camellia-original-vs-sprite.jpg": "jpeg", "camellia-garden-collection-screen.jpg": "jpeg"}

def inspect_png(data: bytes) -> tuple[int, int]:
    if not data.startswith(b"\x89PNG\r\n\x1a\n"): raise ValueError("invalid PNG signature")
    pos, width, height, first, ended = 8, 0, 0, True, False
    while pos + 12 <= len(data):
        length = int.from_bytes(data[pos:pos+4], "big")
        kind = data[pos+4:pos+8]
        end = pos + 12 + length
        if end > len(data): raise ValueError("truncated PNG chunk")
        payload = data[pos+8:pos+8+length]
        expected_crc = int.from_bytes(data[pos+8+length:end], "big")
        if (zlib.crc32(kind + payload) & 0xffffffff) != expected_crc: raise ValueError("PNG CRC mismatch")
        if first:
            if kind != b"IHDR" or length != 13: raise ValueError("invalid PNG IHDR")
            width, height = int.from_bytes(payload[:4], "big"), int.from_bytes(payload[4:8], "big")
            first = False
        if kind == b"IEND":
            if length != 0 or end != len(data): raise ValueError("invalid PNG IEND or trailing bytes")
            ended = True
            break
        pos = end
    if not ended or width <= 0 or height <= 0: raise ValueError("PNG missing dimensions or IEND")
    return width, height

def inspect_jpeg(data: bytes) -> tuple[int, int]:
    if len(data) < 4 or data[:2] != b"\xff\xd8" or data[-2:] != b"\xff\xd9": raise ValueError("invalid or truncated JPEG")
    sof = {0xC0,0xC1,0xC2,0xC3,0xC5,0xC6,0xC7,0xC9,0xCA,0xCB,0xCD,0xCE,0xCF}
    pos = 2
    while pos < len(data)-2:
        if data[pos] != 0xFF: pos += 1; continue
        while pos < len(data) and data[pos] == 0xFF: pos += 1
        if pos >= len(data): break
        marker = data[pos]; pos += 1
        if marker in (0xD8,0xD9,0x01) or 0xD0 <= marker <= 0xD7: continue
        if pos + 2 > len(data): raise ValueError("truncated JPEG segment header")
        length = int.from_bytes(data[pos:pos+2], "big")
        if length < 2 or pos + length > len(data): raise ValueError("invalid JPEG segment length")
        if marker in sof:
            if length < 7: raise ValueError("short JPEG frame header")
            height = int.from_bytes(data[pos+3:pos+5], "big")
            width = int.from_bytes(data[pos+5:pos+7], "big")
            if width <= 0 or height <= 0: raise ValueError("invalid JPEG dimensions")
            return width, height
        if marker == 0xDA: break
        pos += length
    raise ValueError("JPEG has no valid frame header")

def main() -> int:
    if len(sys.argv) != 2: print("usage: verify_images.py EVIDENCE_DIRECTORY", file=sys.stderr); return 2
    directory = Path(sys.argv[1]); failures = []; records = []
    for name, image_format in EXPECTED.items():
        path = directory / name
        if not path.is_file(): failures.append(name + ": missing file"); continue
        try:
            data = path.read_bytes()
            width, height = inspect_png(data) if image_format == "png" else inspect_jpeg(data)
            records.append({"file": name, "format": image_format.upper(), "width": width, "height": height, "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest()})
        except (OSError, ValueError) as exc: failures.append(name + ": " + str(exc))
    if failures:
        for failure in failures: print("EVIDENCE ERROR: " + failure, file=sys.stderr)
        return 1
    (directory / "manifest.json").write_text(json.dumps({"schemaVersion": 1, "images": records}, indent=2) + "\n", encoding="utf-8")
    for item in records: print("VERIFIED: " + item["file"] + " " + str(item["width"]) + "x" + str(item["height"]) + " sha256=" + item["sha256"])
    print("Verified all " + str(len(records)) + " flower evidence images.")
    return 0

if __name__ == "__main__": raise SystemExit(main())
