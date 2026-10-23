#!/usr/bin/env python3
"""Reconstruct verified flower screenshot binaries emitted by Android instrumentation logs."""

from __future__ import annotations

import base64
import hashlib
import sys
from pathlib import Path

EXPECTED = {
    "camellia-original.jpg",
    "camellia-sprite.png",
    "camellia-original-vs-sprite.png",
    "camellia-garden-collection-screen.png",
}
MARKER = "PQFLOWER|"


def payload_from_log_line(line: str) -> str | None:
    marker_at = line.find(MARKER)
    return line[marker_at:].strip() if marker_at >= 0 else None


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: extract_logcat.py LOGCAT_FILE OUTPUT_DIRECTORY", file=sys.stderr)
        return 2

    log_path = Path(sys.argv[1])
    output_dir = Path(sys.argv[2])
    if not log_path.is_file():
        print(f"logcat capture does not exist: {log_path}", file=sys.stderr)
        return 2

    records: dict[str, dict] = {}
    for line in log_path.read_text(encoding="utf-8", errors="replace").splitlines():
        payload = payload_from_log_line(line)
        if payload is None:
            continue
        fields = payload.split("|", 5)
        if len(fields) < 3 or fields[0] != "PQFLOWER":
            continue
        kind = fields[1]
        if kind == "BEGIN" and len(fields) == 6:
            _, _, name, chunks_text, size_text, digest = fields
            if name not in EXPECTED:
                print(f"ignoring unexpected evidence filename: {name}", file=sys.stderr)
                continue
            try:
                chunks = int(chunks_text)
                size = int(size_text)
            except ValueError:
                print(f"invalid header for {name}", file=sys.stderr)
                continue
            records[name] = {
                "chunks_expected": chunks,
                "size_expected": size,
                "sha256_expected": digest,
                "chunks": {},
            }
        elif kind == "DATA" and len(fields) == 5:
            _, _, name, index_text, chunk = fields
            record = records.get(name)
            if record is None:
                continue
            try:
                index = int(index_text)
            except ValueError:
                continue
            record["chunks"][index] = chunk

    output_dir.mkdir(parents=True, exist_ok=True)
    failures: list[str] = []
    recovered: list[str] = []
    for name in sorted(EXPECTED):
        record = records.get(name)
        if record is None:
            failures.append(f"{name}: no log header found")
            continue
        expected_count = record["chunks_expected"]
        chunks = record["chunks"]
        missing = sorted(set(range(expected_count)) - set(chunks))
        if missing:
            failures.append(f"{name}: missing {len(missing)} of {expected_count} base64 chunks")
            continue
        try:
            encoded = "".join(chunks[i] for i in range(expected_count))
            raw = base64.b64decode(encoded, validate=True)
        except (ValueError, KeyError) as exc:
            failures.append(f"{name}: invalid base64 data ({exc})")
            continue
        if len(raw) != record["size_expected"]:
            failures.append(f"{name}: byte length {len(raw)} != {record['size_expected']}")
            continue
        digest = hashlib.sha256(raw).hexdigest()
        if digest != record["sha256_expected"]:
            failures.append(f"{name}: SHA-256 mismatch")
            continue
        expected_signature = b"\xff\xd8\xff" if name.endswith(".jpg") else b"\x89PNG\r\n\x1a\n"
        if not raw.startswith(expected_signature):
            failures.append(f"{name}: contents do not have the expected image signature")
            continue
        destination = output_dir / name
        destination.write_bytes(raw)
        recovered.append(f"{destination} ({len(raw)} bytes, sha256={digest})")

    for item in recovered:
        print(f"RECOVERED: {item}")
    if failures:
        for item in failures:
            print(f"EVIDENCE ERROR: {item}", file=sys.stderr)
        return 1
    print(f"Verified all {len(recovered)} flower evidence images.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
