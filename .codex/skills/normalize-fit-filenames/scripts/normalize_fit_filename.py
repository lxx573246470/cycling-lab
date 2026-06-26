#!/usr/bin/env python3
"""Normalize a cycling FIT filename to a stable activity-date based name."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

try:
    from fitparse import FitFile
except ImportError as exc:  # pragma: no cover - environment guard
    raise SystemExit("Missing dependency: fitparse. Install with `python3 -m pip install fitparse`.") from exc


def read_activity_start(path: Path):
    fit = FitFile(str(path))
    first_record_timestamp = None
    session_start = None
    file_created = None

    for msg in fit.get_messages():
        values = {field.name: field.value for field in msg}
        if msg.name == "session" and session_start is None:
            session_start = values.get("start_time")
        elif msg.name == "file_id" and file_created is None:
            file_created = values.get("time_created")
        elif msg.name == "record" and first_record_timestamp is None:
            first_record_timestamp = values.get("timestamp")

        if session_start and file_created and first_record_timestamp:
            break

    return session_start or file_created or first_record_timestamp


def is_canonical(path: Path, date_text: str) -> bool:
    pattern = rf"{re.escape(date_text)}(?:-\d+)?\.fit"
    return re.fullmatch(pattern, path.name, flags=re.IGNORECASE) is not None


def next_available_target(path: Path, date_text: str) -> Path:
    target = path.with_name(f"{date_text}.fit")
    if not target.exists() or target.resolve() == path.resolve():
        return target

    index = 1
    while True:
        target = path.with_name(f"{date_text}-{index}.fit")
        if not target.exists() or target.resolve() == path.resolve():
            return target
        index += 1


def normalize(path: Path, dry_run: bool = False) -> dict[str, object]:
    if not path.exists():
        raise SystemExit(f"FIT file not found: {path}")
    if path.suffix.lower() != ".fit":
        raise SystemExit(f"Not a .fit file: {path}")

    start = read_activity_start(path)
    if not hasattr(start, "strftime"):
        return {
            "status": "blocked",
            "reason": "activity_date_not_found",
            "original_path": str(path),
            "normalized_path": str(path),
            "renamed": False,
        }

    date_text = start.strftime("%Y-%m-%d")
    if is_canonical(path, date_text):
        return {
            "status": "ok",
            "activity_date": date_text,
            "original_path": str(path),
            "normalized_path": str(path),
            "renamed": False,
        }

    target = next_available_target(path, date_text)
    if not dry_run:
        path.rename(target)

    return {
        "status": "ok",
        "activity_date": date_text,
        "original_path": str(path),
        "normalized_path": str(target),
        "renamed": True,
        "dry_run": dry_run,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Normalize a FIT filename to YYYY-MM-DD.fit or YYYY-MM-DD-N.fit.")
    parser.add_argument("fit_file", type=Path)
    parser.add_argument("--dry-run", action="store_true", help="Print the target path without renaming.")
    args = parser.parse_args()

    result = normalize(args.fit_file, dry_run=args.dry_run)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
