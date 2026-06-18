#!/usr/bin/env python3
"""Build the cycling training-load summary from Markdown training notes."""

from __future__ import annotations

import argparse
import csv
import re
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = PROJECT_ROOT / "training" / "load-summary.csv"
LOAD_POLICY = "historical_ftp_locked"


@dataclass(frozen=True)
class NoteLoad:
    date: date
    duration_min: float | None
    tss: float
    ftp_used: int | None
    tss_source: str
    note_path: Path


def read_profile_ftp() -> int | None:
    profile = PROJECT_ROOT / "profile" / "rider-profile.md"
    if not profile.exists():
        return None
    text = profile.read_text(encoding="utf-8")
    match = re.search(r"FTP[：:]\s*(\d+(?:\.\d+)?)", text)
    return int(round(float(match.group(1)))) if match else None


def first_float(patterns: list[str], text: str) -> float | None:
    for pattern in patterns:
        match = re.search(pattern, text, flags=re.IGNORECASE | re.MULTILINE)
        if match:
            return float(match.group(1))
    return None


def parse_date(text: str) -> date | None:
    match = re.search(r"^date:\s*(\d{4}-\d{2}-\d{2})\s*$", text, flags=re.MULTILINE)
    if not match:
        return None
    return datetime.strptime(match.group(1), "%Y-%m-%d").date()


def parse_duration_min(text: str) -> float | None:
    minutes = first_float(
        [
            r"主动计时时长\s*\|\s*(\d+(?:\.\d+)?)\s*分钟",
            r"-\s*(?:时长|骑行时间)[：:]\s*(\d+(?:\.\d+)?)\s*分钟",
            r"\|\s*时长\s*\|\s*(\d+(?:\.\d+)?)\s*分钟",
        ],
        text,
    )
    if minutes is not None:
        return minutes

    match = re.search(r"移动时间\s+(\d+):(\d{2})(?::(\d{2}))?", text)
    if not match:
        return None
    first = int(match.group(1))
    second = int(match.group(2))
    third = int(match.group(3) or 0)
    if match.group(3):
        return first * 60 + second + third / 60
    return first + second / 60


def parse_np(text: str) -> float | None:
    return first_float(
        [
            r"平均功率[：:]\s*\d+(?:\.\d+)?\s*W[；;]\s*NP(?:（估算）)?[：:]\s*(\d+(?:\.\d+)?)\s*W",
            r"估算标准化功率\s*(\d+(?:\.\d+)?)\s*W",
            r"\|\s*(?:估算\s*)?NP\s*\|\s*(\d+(?:\.\d+)?)\s*W\s*\|",
            r"\|\s*平均功率\s*/\s*NP\s*/\s*最高功率\s*\|\s*\d+(?:\.\d+)?\s*/\s*(\d+(?:\.\d+)?)\s*/",
            r"\bNP\s*(?:（估算）)?[：:]\s*(\d+(?:\.\d+)?)\s*W",
        ],
        text,
    )


def parse_if(text: str) -> float | None:
    return first_float(
        [
            r"IF（强度因子）[：:]\s*(\d+(?:\.\d+)?)",
            r"\|\s*IF\s*/\s*TSS\s*\|\s*(\d+(?:\.\d+)?)\s*/",
            r"\bIF\s*(\d+(?:\.\d+)?)",
        ],
        text,
    )


def parse_recorded_tss(text: str) -> float | None:
    return first_float(
        [
            r"TSS（训练压力）[：:]\s*(\d+(?:\.\d+)?)",
            r"\|\s*IF\s*/\s*TSS\s*\|\s*\d+(?:\.\d+)?\s*/\s*(\d+(?:\.\d+)?)\s*\|",
            r"估算\s*TSS\s*(\d+(?:\.\d+)?)",
        ],
        text,
    )


def parse_ftp_used(text: str, npower: float | None, if_value: float | None, profile_ftp: int | None) -> tuple[int | None, str]:
    explicit = first_float([r"FTP\s*参考值[：:]\s*(\d+(?:\.\d+)?)\s*W"], text)
    if explicit:
        return int(round(explicit)), "note-explicit"

    pct = first_float([r"NP\s*/\s*FTP[：:\s|]*(\d+(?:\.\d+)?)%"], text)
    if npower and pct:
        return round_to_nearest_five(npower / (pct / 100)), "note-inferred-np-percent"

    if npower and if_value:
        return round_to_nearest_five(npower / if_value), "note-inferred-if"

    return profile_ftp, "profile-fallback" if profile_ftp else "unknown"


def round_to_nearest_five(value: float) -> int:
    return int(round(value / 5) * 5)


def parse_note(path: Path, profile_ftp: int | None) -> NoteLoad | None:
    text = path.read_text(encoding="utf-8")
    if re.search(r"^type:\s*cycling-training-day-analysis\s*$", text, flags=re.MULTILINE):
        return None

    activity_date = parse_date(text)
    if activity_date is None:
        return None

    duration_min = parse_duration_min(text)
    npower = parse_np(text)
    if_value = parse_if(text)
    recorded_tss = parse_recorded_tss(text)
    ftp_used, ftp_source = parse_ftp_used(text, npower, if_value, profile_ftp)

    if recorded_tss is not None:
        return NoteLoad(
            date=activity_date,
            duration_min=duration_min,
            tss=recorded_tss,
            ftp_used=ftp_used,
            tss_source=f"recorded:{ftp_source}",
            note_path=path,
        )

    if duration_min and npower and ftp_used:
        intensity_factor = npower / ftp_used
        tss = (duration_min / 60) * (intensity_factor**2) * 100
        return NoteLoad(
            date=activity_date,
            duration_min=duration_min,
            tss=tss,
            ftp_used=ftp_used,
            tss_source=f"calculated:{ftp_source}",
            note_path=path,
        )

    return None


def iter_note_loads(notes_root: Path) -> list[NoteLoad]:
    profile_ftp = read_profile_ftp()
    loads = []
    for path in sorted(notes_root.glob("**/notes/*.md")):
        parsed = parse_note(path, profile_ftp)
        if parsed:
            loads.append(parsed)
    return loads


def daterange(start: date, end: date):
    current = start
    while current <= end:
        yield current
        current += timedelta(days=1)


def fmt_num(value: float | None, digits: int = 1) -> str:
    if value is None:
        return ""
    return f"{value:.{digits}f}".rstrip("0").rstrip(".")


def build_rows(loads: list[NoteLoad], through_date: date | None) -> list[dict[str, str]]:
    if not loads:
        return []

    by_date: dict[date, list[NoteLoad]] = {}
    for load in loads:
        by_date.setdefault(load.date, []).append(load)

    start = min(by_date)
    end = max(max(by_date), through_date) if through_date else max(by_date)
    ctl = 0.0
    atl = 0.0
    rows = []

    for current in daterange(start, end):
        day_loads = by_date.get(current, [])
        tss = sum(load.tss for load in day_loads)
        duration = sum(load.duration_min or 0 for load in day_loads) if day_loads else 0
        ctl += (tss - ctl) / 42
        atl += (tss - atl) / 7
        tsb = ctl - atl
        ftp_values = sorted({load.ftp_used for load in day_loads if load.ftp_used})
        tss_sources = sorted({load.tss_source for load in day_loads})
        source_notes = ";".join(str(load.note_path.relative_to(PROJECT_ROOT)) for load in day_loads)

        rows.append(
            {
                "date": current.isoformat(),
                "iso_week": f"{current.isocalendar().year}-W{current.isocalendar().week:02d}",
                "activity_count": str(len(day_loads)),
                "duration_min": fmt_num(duration),
                "tss": fmt_num(tss),
                "ctl": fmt_num(ctl),
                "atl": fmt_num(atl),
                "tsb": fmt_num(tsb),
                "ftp_used": "/".join(str(value) for value in ftp_values),
                "ftp_policy": LOAD_POLICY,
                "tss_source": ";".join(tss_sources),
                "source_notes": source_notes,
            }
        )

    return rows


def write_csv(path: Path, rows: list[dict[str, str]]) -> None:
    fieldnames = [
        "date",
        "iso_week",
        "activity_count",
        "duration_min",
        "tss",
        "ctl",
        "atl",
        "tsb",
        "ftp_used",
        "ftp_policy",
        "tss_source",
        "source_notes",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--notes-root", type=Path, default=PROJECT_ROOT / "training", help="Root that contains YYYY/week-NN/notes files.")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="CSV output path.")
    parser.add_argument("--through-date", help="Optional final date to roll rest days through, YYYY-MM-DD.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    through_date = datetime.strptime(args.through_date, "%Y-%m-%d").date() if args.through_date else None
    loads = iter_note_loads(args.notes_root)
    rows = build_rows(loads, through_date)
    write_csv(args.output, rows)
    print(f"Wrote {len(rows)} rows to {args.output}")
    print(f"Included {len(loads)} training notes with TSS or enough power data to calculate TSS.")


if __name__ == "__main__":
    main()
