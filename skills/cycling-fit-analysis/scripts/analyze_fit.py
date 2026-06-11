#!/usr/bin/env python3
import argparse
import json
import math
import re
import shutil
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from statistics import mean, median

try:
    from fitparse import FitFile
except ImportError as exc:
    raise SystemExit("Missing dependency: fitparse. Install with `python3 -m pip install fitparse`.") from exc


def numeric(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def avg(values):
    vals = [v for v in values if numeric(v)]
    return mean(vals) if vals else None


def calculate_normalized_power(records, window=30):
    values = [float(r.get("power")) for r in records if numeric(r.get("power"))]
    if len(values) < window:
        return None

    rolling = []
    total = sum(values[:window])
    rolling.append(total / window)
    for index in range(window, len(values)):
        total += values[index] - values[index - window]
        rolling.append(total / window)

    return (mean([value**4 for value in rolling])) ** 0.25


def training_load_metrics(duration_s, avg_power_value, normalized_power_value, ftp):
    if not numeric(duration_s):
        duration_s = None
    work_kj = avg_power_value * duration_s / 1000 if numeric(avg_power_value) and duration_s else None
    intensity_factor = normalized_power_value / ftp if numeric(normalized_power_value) and ftp else None
    variability_index = normalized_power_value / avg_power_value if numeric(normalized_power_value) and numeric(avg_power_value) and avg_power_value > 0 else None
    tss = (duration_s / 3600) * (intensity_factor**2) * 100 if duration_s and intensity_factor else None
    return {
        "work_kj": work_kj,
        "if": intensity_factor,
        "vi": variability_index,
        "tss": tss,
    }


def fmt(value, digits=1, suffix=""):
    if value is None:
        return "无数据"
    if isinstance(value, float):
        text = f"{value:.{digits}f}"
        if digits > 0:
            text = text.rstrip("0").rstrip(".")
    else:
        text = str(value)
    return text + suffix


def slugify(text):
    text = re.sub(r"[\\/:*?\"<>|]+", "-", text).strip()
    text = re.sub(r"\s+", " ", text)
    return text or "cycling-fit-analysis"


def best_rolling(records, field, window):
    values = [r.get(field) for r in records]
    queue = []
    total = 0
    count = 0
    best = None
    best_i = 0
    for i, value in enumerate(values):
        queue.append(value)
        if numeric(value):
            total += value
            count += 1
        if len(queue) > window:
            old = queue.pop(0)
            if numeric(old):
                total -= old
                count -= 1
        if len(queue) == window and count == window:
            current = total / window
            if best is None or current > best:
                best = current
                best_i = i - window + 1
    return best, best_i


def time_at(records, start_index):
    if not records:
        return "0:00"
    start_index = max(0, min(start_index, len(records) - 1))
    first = records[0].get("timestamp")
    current = records[start_index].get("timestamp")
    if first and current:
        seconds = int((current - first).total_seconds())
    else:
        seconds = start_index
    return f"{seconds // 60}:{seconds % 60:02d}"


def elapsed_seconds(record, first_timestamp):
    timestamp = record.get("timestamp")
    if first_timestamp and timestamp:
        return max(0, (timestamp - first_timestamp).total_seconds())
    return 0


def split_markdown_row(line):
    text = line.strip()
    if not text.startswith("|") or not text.endswith("|"):
        return []
    return [cell.strip() for cell in text.strip("|").split("|")]


def is_markdown_separator(cells):
    return bool(cells) and all(re.fullmatch(r":?-{3,}:?", cell.strip()) for cell in cells)


def parse_duration_seconds(text):
    if not text:
        return None
    normalized = str(text).replace("×", "x").replace("－", "-").replace("—", "-")
    total = 0.0
    matched = False

    repeated = re.compile(r"(\d+(?:\.\d+)?)\s*x\s*(\d+(?:\.\d+)?)\s*(秒|sec|secs|second|seconds|分钟|分|min|mins|minute|minutes)", re.IGNORECASE)
    for match in repeated.finditer(normalized):
        count = float(match.group(1))
        value = float(match.group(2))
        unit = match.group(3).lower()
        total += count * value * (1 if unit in {"秒", "sec", "secs", "second", "seconds"} else 60)
        matched = True
    normalized = repeated.sub(" ", normalized)

    single = re.compile(r"(\d+(?:\.\d+)?)(?:\s*(?:-|到|至|~)\s*(\d+(?:\.\d+)?))?\s*(秒|sec|secs|second|seconds|分钟|分|min|mins|minute|minutes)", re.IGNORECASE)
    for match in single.finditer(normalized):
        value = float(match.group(2) or match.group(1))
        unit = match.group(3).lower()
        total += value * (1 if unit in {"秒", "sec", "secs", "second", "seconds"} else 60)
        matched = True

    return int(round(total)) if matched and total > 0 else None


def parse_power_target(text, ftp):
    if not text:
        return None
    normalized = str(text).replace("－", "-").replace("—", "-")
    ranges = []
    percent_pattern = re.compile(r"(\d+(?:\.\d+)?)(?:\s*(?:-|到|至|~)\s*(\d+(?:\.\d+)?))?\s*%\s*(?:FTP)?", re.IGNORECASE)
    for match in percent_pattern.finditer(normalized):
        low = float(match.group(1))
        high = float(match.group(2) or match.group(1))
        if ftp:
            ranges.append((min(low, high) / 100 * ftp, max(low, high) / 100 * ftp))
    watt_pattern = re.compile(r"(\d+(?:\.\d+)?)(?:\s*(?:-|到|至|~)\s*(\d+(?:\.\d+)?))?\s*W\b", re.IGNORECASE)
    for match in watt_pattern.finditer(normalized):
        low = float(match.group(1))
        high = float(match.group(2) or match.group(1))
        ranges.append((min(low, high), max(low, high)))
    if not ranges:
        return None
    return min(low for low, _ in ranges), max(high for _, high in ranges)


def find_plan_file(start):
    if not hasattr(start, "strftime"):
        return None
    project_root = Path(__file__).resolve().parents[3]
    date_text = start.strftime("%Y-%m-%d")
    week = start.isocalendar().week
    candidates = sorted((project_root / "plans" / str(start.year) / f"week-{week}").glob(f"{date_text}*.md"))
    candidates = [path for path in candidates if "weekly-plan" not in path.name]
    if candidates:
        return candidates[0]

    candidates = sorted((project_root / "plans").glob(f"**/{date_text}*.md"))
    candidates = [path for path in candidates if "weekly-plan" not in path.name]
    return candidates[0] if candidates else None


def parse_plan_segments(path):
    if not path:
        return []
    plan_path = Path(path).expanduser().resolve()
    if not plan_path.exists():
        return []
    lines = plan_path.read_text(encoding="utf-8").splitlines()
    for index, line in enumerate(lines):
        headers = split_markdown_row(line)
        if not headers or not any("阶段" in header for header in headers) or not any("时间" in header for header in headers):
            continue
        if index + 1 >= len(lines) or not is_markdown_separator(split_markdown_row(lines[index + 1])):
            continue

        stage_i = next(i for i, header in enumerate(headers) if "阶段" in header)
        time_i = next(i for i, header in enumerate(headers) if "时间" in header)
        intensity_i = next((i for i, header in enumerate(headers) if "强度" in header or "功率" in header or "Zwift" in header), None)
        note_i = next((i for i, header in enumerate(headers) if "执行" in header or "说明" in header or "目标" in header), None)

        segments = []
        start_s = 0
        for row_line in lines[index + 2 :]:
            cells = split_markdown_row(row_line)
            if not cells or is_markdown_separator(cells):
                break
            if max(stage_i, time_i) >= len(cells):
                continue
            duration = parse_duration_seconds(cells[time_i])
            if not duration:
                continue
            intensity = cells[intensity_i] if intensity_i is not None and intensity_i < len(cells) else ""
            note = cells[note_i] if note_i is not None and note_i < len(cells) else ""
            segments.append(
                {
                    "name": cells[stage_i],
                    "duration_s": duration,
                    "start_s": start_s,
                    "end_s": start_s + duration,
                    "intensity": intensity,
                    "note": note,
                }
            )
            start_s += duration
        if segments:
            return segments
    return []


def extract_plan_goal(path):
    if not path:
        return None
    plan_path = Path(path).expanduser().resolve()
    if not plan_path.exists():
        return None
    lines = plan_path.read_text(encoding="utf-8").splitlines()
    in_goal = False
    goal_lines = []
    for line in lines:
        stripped = line.strip()
        if re.fullmatch(r"##\s+(目标|Ride Objective|Objective).*", stripped, flags=re.IGNORECASE):
            in_goal = True
            continue
        if in_goal and stripped.startswith("## "):
            break
        if in_goal and stripped and not stripped.startswith("|"):
            goal_lines.append(stripped.lstrip("- ").strip())
    if not goal_lines:
        return None
    return " ".join(goal_lines[:3])


def records_between(records, start_s, end_s):
    if not records:
        return []
    first = records[0].get("timestamp")
    return [record for record in records if start_s <= elapsed_seconds(record, first) < end_s]


def evaluate_segment(avg_power_value, target_range):
    if avg_power_value is None or not target_range:
        return "只看趋势"
    low, high = target_range
    tolerance = max(3, (high - low) * 0.1)
    if avg_power_value < low - tolerance:
        return "低于计划"
    if avg_power_value > high + tolerance:
        return "高于计划"
    return "贴近计划"


def planned_segment_rows(records, plan_segments, ftp):
    rows = []
    for segment in plan_segments:
        chunk = records_between(records, segment["start_s"], segment["end_s"])
        target_range = parse_power_target(" ".join([segment.get("intensity", ""), segment.get("note", "")]), ftp)
        p = avg([r.get("power") for r in chunk])
        hr = avg([r.get("heart_rate") for r in chunk])
        cad = avg([r.get("cadence") for r in chunk])
        rows.append(
            {
                "name": segment["name"],
                "time": f"{segment['start_s'] // 60:02d}-{math.ceil(segment['end_s'] / 60):02d} min",
                "planned": segment.get("intensity") or segment.get("note") or "未写明",
                "duration_min": len(chunk) / 60 if chunk else 0,
                "power": p,
                "heart_rate": hr,
                "cadence": cad,
                "drift": decoupling(chunk),
                "target_range": target_range,
                "verdict": evaluate_segment(p, target_range),
            }
        )
    return rows


def clamp(value, low=0, high=10):
    return max(low, min(high, value))


def score_from_drift(drift):
    if drift is None:
        return 6.0, "心率漂移数据不足"
    absolute = abs(drift)
    if absolute < 5:
        return 9.0, f"心率漂移 {fmt(drift, 1, '%')}，稳定"
    if absolute < 7:
        return 8.0, f"心率漂移 {fmt(drift, 1, '%')}，可接受"
    if absolute < 10:
        return 6.5, f"心率漂移 {fmt(drift, 1, '%')}，略偏高"
    if absolute < 15:
        return 5.0, f"心率漂移 {fmt(drift, 1, '%')}，偏高"
    return 4.0, f"心率漂移 {fmt(drift, 1, '%')}，明显偏高"


def is_hr_control_segment(row):
    """Select steady planned work segments where HR drift is interpretable."""
    if not row or row.get("duration_min", 0) < 5:
        return False
    if not numeric(row.get("drift")) or not numeric(row.get("power")) or not numeric(row.get("heart_rate")):
        return False

    name = str(row.get("name", "")).lower()
    planned = str(row.get("planned", "")).lower()
    excluded_words = [
        "热身",
        "warmup",
        "warm-up",
        "恢复",
        "轻踩",
        "放松",
        "cooldown",
        "cool-down",
        "recovery",
    ]
    if any(word in name or word in planned for word in excluded_words):
        return False

    target_range = row.get("target_range")
    if not target_range:
        return True
    low, high = target_range
    if not numeric(low) or not numeric(high):
        return True
    return high - low <= 25


def score_from_segment_hr_control(segment_metrics, whole_ride_drift):
    control_rows = [row for row in segment_metrics if is_hr_control_segment(row)]
    if not control_rows:
        score, note = score_from_drift(whole_ride_drift)
        return score, f"{note}（整场前后半粗算；本次计划没有足够可比的稳态分段）"

    abs_drifts = [abs(row["drift"]) for row in control_rows]
    median_abs = median(abs_drifts)
    if median_abs < 5:
        score = 9.0
        label = "稳定"
    elif median_abs < 7:
        score = 8.0
        label = "可接受"
    elif median_abs < 10:
        score = 6.5
        label = "略偏高"
    elif median_abs < 15:
        score = 5.0
        label = "偏高"
    else:
        score = 4.0
        label = "明显偏高"

    examples = "；".join(f"{row['name']} {fmt(row['drift'], 1, '%')}" for row in control_rows[:4])
    if len(control_rows) > 4:
        examples += f"；另 {len(control_rows) - 4} 段"
    return score, f"可控训练段心率漂移中位数 {median_abs:.1f}%，{label}（{examples}）"


def score_from_cadence(cadence_values):
    moving = [value for value in cadence_values if numeric(value) and value >= 1]
    if not moving:
        return 6.0, "踏频数据不足"
    stable_pct = sum(1 for value in moving if 85 <= value <= 100) / len(moving) * 100
    if stable_pct >= 70:
        score = 9.0
    elif stable_pct >= 55:
        score = 8.0
    elif stable_pct >= 40:
        score = 7.0
    elif stable_pct >= 25:
        score = 6.0
    else:
        score = 5.0
    return score, f"85-100 rpm 踏频占移动时间 {stable_pct:.1f}%"


def score_from_completion(minutes, plan_segments):
    if not minutes or not plan_segments:
        return 6.0, "缺少计划时长，完成度按中性处理"
    planned_minutes = sum(segment["duration_s"] for segment in plan_segments) / 60
    if planned_minutes <= 0:
        return 6.0, "计划时长无效"
    ratio = minutes / planned_minutes
    if ratio < 1:
        score = 10 * ratio
    elif ratio <= 1.1:
        score = 10
    else:
        score = 10 - min(3, (ratio - 1.1) * 10)
    return clamp(score), f"实际 {fmt(minutes, 1, ' 分钟')} / 计划 {fmt(planned_minutes, 1, ' 分钟')}"


def score_from_segments(segment_metrics):
    if not segment_metrics:
        return 6.0, "没有计划分段，无法评价分段贴合度"
    values = []
    weights = []
    for row in segment_metrics:
        verdict = row["verdict"]
        if verdict == "贴近计划":
            score = 9.0
        elif verdict in {"低于计划", "高于计划"}:
            score = 6.0
        else:
            score = 7.0
        values.append(score)
        weights.append(max(row["duration_min"], 0.5))
    weighted = sum(value * weight for value, weight in zip(values, weights)) / sum(weights)
    close = sum(1 for row in segment_metrics if row["verdict"] == "贴近计划")
    off = sum(1 for row in segment_metrics if row["verdict"] in {"低于计划", "高于计划"})
    return weighted, f"{close} 个阶段贴近计划，{off} 个阶段偏离功率目标"


def training_goal_score(minutes, plan_segments, segment_metrics, drift, cadence_values, plan_goal):
    completion_score, completion_note = score_from_completion(minutes, plan_segments)
    segment_score, segment_note = score_from_segments(segment_metrics)
    if plan_segments:
        drift_score, drift_note = score_from_segment_hr_control(segment_metrics, drift)
    else:
        drift_score, drift_note = score_from_drift(drift)
    cadence_score, cadence_note = score_from_cadence(cadence_values)

    if plan_segments:
        score = completion_score * 0.25 + segment_score * 0.35 + drift_score * 0.25 + cadence_score * 0.15
    else:
        score = segment_score * 0.20 + drift_score * 0.45 + cadence_score * 0.35
    score = round(clamp(score), 1)

    if score >= 8.5:
        label = "高度达成"
    elif score >= 7:
        label = "基本达成"
    elif score >= 6:
        label = "部分达成"
    else:
        label = "偏离计划"

    notes = [
        f"完成度：{completion_note}",
        f"分段执行：{segment_note}",
        f"心率控制：{drift_note}",
        f"踏频稳定：{cadence_note}",
    ]
    if plan_goal:
        notes.insert(0, f"计划目标：{plan_goal}")
    return {"score": score, "label": label, "notes": notes}


def parse_fit(path):
    fit = FitFile(str(path))
    messages = defaultdict(list)
    for msg in fit.get_messages():
        data = {field.name: field.value for field in msg}
        messages[msg.name].append(data)
    records = [r for r in messages.get("record", []) if r.get("timestamp")]
    records.sort(key=lambda r: r["timestamp"])
    return messages, records


def activity_start(messages, records):
    session = messages.get("session", [{}])[0]
    file_id = messages.get("file_id", [{}])[0]
    return session.get("start_time") or file_id.get("time_created") or (records[0].get("timestamp") if records else None)


def is_canonical_fit_name(path, date_text):
    return re.fullmatch(rf"{re.escape(date_text)}(?:-\d+)?\.fit", path.name, flags=re.IGNORECASE) is not None


def normalize_today_fit_name(path, start):
    if path.suffix.lower() != ".fit" or not hasattr(start, "date"):
        return path

    today = datetime.now(start.tzinfo).date() if getattr(start, "tzinfo", None) else datetime.now().date()
    if start.date() != today:
        return path

    date_text = start.strftime("%Y-%m-%d")
    if is_canonical_fit_name(path, date_text):
        return path

    target = path.with_name(f"{date_text}.fit")
    if target.exists() and target.resolve() != path:
        index = 1
        while True:
            target = path.with_name(f"{date_text}-{index}.fit")
            if not target.exists() or target.resolve() == path:
                break
            index += 1

    path.rename(target)
    return target


def zone_counts(values, zones):
    total = len(values)
    rows = []
    for name, low, high in zones:
        count = sum(1 for value in values if low <= value < high)
        pct = count / total * 100 if total else 0
        rows.append((name, count, pct))
    return rows


def ftp_power_zone_rows(power_values, ftp):
    if not ftp:
        return []
    zones = [
        ("Z1 主动恢复 <55% FTP", 0, ftp * 0.55),
        ("Z2 耐力 55-75% FTP", ftp * 0.55, ftp * 0.75),
        ("Z3 节奏 75-90% FTP", ftp * 0.75, ftp * 0.90),
        ("Z4 阈值 90-105% FTP", ftp * 0.90, ftp * 1.05),
        ("Z5 VO2max 105-120% FTP", ftp * 1.05, ftp * 1.20),
        ("Z6 无氧 120-150% FTP", ftp * 1.20, ftp * 1.50),
        ("Z7 神经肌肉 >=150% FTP", ftp * 1.50, 100_000),
    ]
    return zone_counts(power_values, zones)


def segment_rows(records, seconds=600):
    rows = []
    for start in range(0, len(records), seconds):
        end = min(len(records), start + seconds)
        chunk = records[start:end]
        rows.append(
            (
                f"{start // 60:02d}-{math.ceil(end / 60):02d} min",
                avg([r.get("power") for r in chunk]),
                avg([r.get("heart_rate") for r in chunk]),
                avg([r.get("cadence") for r in chunk]),
            )
        )
    return rows


def decoupling(records):
    if len(records) < 120:
        return None
    mid = len(records) // 2
    first = records[:mid]
    second = records[mid:]
    p1 = avg([r.get("power") for r in first])
    p2 = avg([r.get("power") for r in second])
    h1 = avg([r.get("heart_rate") for r in first])
    h2 = avg([r.get("heart_rate") for r in second])
    if not all([p1, p2, h1, h2]):
        return None
    return ((h2 / p2) / (h1 / p1) - 1) * 100


def classify_session(hr_zone_rows, drift):
    z3 = next((pct for name, _, pct in hr_zone_rows if name.startswith("Z3")), 0)
    z4 = next((pct for name, _, pct in hr_zone_rows if name.startswith("Z4")), 0)
    z5 = next((pct for name, _, pct in hr_zone_rows if name.startswith("Z5")), 0)
    if z5 > 5:
        kind = "高强度间歇或比赛型训练"
    elif z4 > 35:
        kind = "偏阈值/节奏的持续训练"
    elif z3 + z4 > 50:
        kind = "中等偏上的节奏有氧训练"
    else:
        kind = "轻中强度有氧或恢复训练"
    drift_text = "心率漂移较稳定" if drift is not None and abs(drift) < 5 else "心率漂移偏高或数据不足"
    return kind, drift_text


def write_note(args, messages, records):
    session = messages.get("session", [{}])[0]
    file_id = messages.get("file_id", [{}])[0]
    start = activity_start(messages, records)
    created = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    date_text = start.strftime("%Y-%m-%d") if hasattr(start, "strftime") else "unknown-date"
    title = args.note_title or f"{date_text} 室内骑行训练分析"
    plan_path = Path(args.plan_file).expanduser().resolve() if args.plan_file else find_plan_file(start)
    plan_segments = parse_plan_segments(plan_path)
    plan_goal = extract_plan_goal(plan_path) if plan_segments else None

    hr_values = [r.get("heart_rate") for r in records if numeric(r.get("heart_rate"))]
    power_values = [r.get("power") for r in records if numeric(r.get("power"))]
    cadence_values = [r.get("cadence") for r in records if numeric(r.get("cadence"))]
    max_hr = args.max_hr or (220 - args.age if args.age else None)

    hr_zones = []
    if max_hr:
        hr_zones = [
            ("Z1 恢复 <60%", 0, max_hr * 0.60),
            ("Z2 有氧 60-70%", max_hr * 0.60, max_hr * 0.70),
            ("Z3 节奏 70-80%", max_hr * 0.70, max_hr * 0.80),
            ("Z4 阈值 80-90%", max_hr * 0.80, max_hr * 0.90),
            ("Z5 高强度 >=90%", max_hr * 0.90, 10_000),
        ]
    hr_zone_rows = zone_counts(hr_values, hr_zones) if hr_zones else []
    cadence_rows = zone_counts(cadence_values, [("0 停踩", 0, 1), ("<70 rpm", 1, 70), ("70-85 rpm", 70, 85), ("85-100 rpm", 85, 101), (">100 rpm", 101, 10_000)])
    power_rows = zone_counts(power_values, [("0-50 W", 0, 50), ("50-100 W", 50, 100), ("100-125 W", 100, 125), ("125-150 W", 125, 150), ("150-175 W", 150, 175), ("175-200 W", 175, 200), ("200-250 W", 200, 250), (">=250 W", 250, 10_000)])
    ftp_zone_rows = ftp_power_zone_rows(power_values, args.ftp)
    drift = decoupling(records)
    kind, drift_text = classify_session(hr_zone_rows, drift)
    if plan_segments and drift is not None:
        drift_text = "整场前后半粗算；结构化变功率课仅供背景参考"

    duration = session.get("total_timer_time") or session.get("total_elapsed_time")
    minutes = duration / 60 if numeric(duration) else (len(records) / 60 if records else None)
    duration_s = duration if numeric(duration) else (len(records) if records else None)
    weight = args.weight_kg
    avg_power = session.get("avg_power") or avg(power_values)
    npower = session.get("normalized_power")
    npower_estimated = False
    if not numeric(npower):
        npower = calculate_normalized_power(records)
        npower_estimated = npower is not None
    avg_hr = session.get("avg_heart_rate") or avg(hr_values)
    max_seen_hr = session.get("max_heart_rate") or (max(hr_values) if hr_values else None)
    avg_cad = session.get("avg_cadence") or avg(cadence_values)
    np_label = "NP（估算）" if npower_estimated else "NP"
    load_metrics = training_load_metrics(duration_s, avg_power, npower, args.ftp)
    segment_metrics = planned_segment_rows(records, plan_segments, args.ftp) if plan_segments else []
    goal_score = training_goal_score(minutes, plan_segments, segment_metrics, drift, cadence_values, plan_goal)

    lines = [
        "---",
        f'title: "{title}"',
        f"date: {date_text}",
        "type: cycling-training-analysis",
        f"created: {created}",
        "tags:",
        "  - cycling",
        "  - training",
        "  - fit",
        "---",
        "",
        f"# {title}",
        "",
        "## 基本信息",
        "",
        f"- 运动类型：{session.get('sub_sport') or session.get('sport') or 'cycling'}",
        f"- 设备：{file_id.get('product_name') or '未知'}",
        f"- 年龄/身高/体重：{args.age or '未知'} 岁 / {args.height_cm or '未知'} cm / {args.weight_kg or '未知'} kg",
        f"- 时长：{fmt(minutes, 1, ' 分钟')}",
        f"- 消耗：{fmt(session.get('total_calories'), 0, ' kcal')}",
        "",
        "## 核心指标",
        "",
        f"- 平均心率：{fmt(avg_hr, 0, ' bpm')}；最高心率：{fmt(max_seen_hr, 0, ' bpm')}",
        f"- 平均功率：{fmt(avg_power, 0, ' W')}；{np_label}：{fmt(npower, 0, ' W')}；最高功率：{fmt(session.get('max_power') or (max(power_values) if power_values else None), 0, ' W')}",
        f"- 平均踏频：{fmt(avg_cad, 0, ' rpm')}；最高踏频：{fmt(session.get('max_cadence') or (max(cadence_values) if cadence_values else None), 0, ' rpm')}",
    ]
    if plan_path and plan_segments:
        lines.insert(lines.index("## 核心指标") - 1, f"- 对照计划：{plan_path}")
    elif args.plan_file:
        lines.insert(lines.index("## 核心指标") - 1, f"- 对照计划：{args.plan_file}（未解析到包含“阶段/时间”的计划表）")
    if weight and avg_power:
        lines.append(f"- 平均功率体重比：{fmt(avg_power / weight, 2, ' W/kg')}")
    if weight and npower:
        lines.append(f"- NP 功率体重比：{fmt(npower / weight, 2, ' W/kg')}")
    if drift is not None:
        lines.append(f"- 心率漂移：{fmt(drift, 1, '%')}（{drift_text}）")
    if args.ftp and avg_power:
        lines.append(f"- 平均功率 / FTP：{fmt(avg_power / args.ftp * 100, 1, '%')}")
    if args.ftp and npower:
        lines.append(f"- NP / FTP：{fmt(npower / args.ftp * 100, 1, '%')}")
    if numeric(load_metrics["if"]):
        lines.append(f"- IF（强度因子）：{fmt(load_metrics['if'], 2)}")
    if numeric(load_metrics["tss"]):
        lines.append(f"- TSS（训练压力）：{fmt(load_metrics['tss'], 0)}")
    if numeric(load_metrics["vi"]):
        lines.append(f"- VI（变异指数）：{fmt(load_metrics['vi'], 2)}")
    if numeric(load_metrics["work_kj"]):
        lines.append(f"- 机械功：{fmt(load_metrics['work_kj'], 0, ' kJ')}")

    lines += ["", "## 强度判断", "", f"- 本次更接近：**{kind}**"]
    if max_hr:
        lines.append(f"- 心率区间按最大心率 {max_hr} bpm 估算；如果你有实测最大心率或阈值心率，应优先使用实测值。")
    lines += [
        "- 室内骑行若速度/距离为 0 属正常记录现象，本分析优先使用心率、功率和踏频。",
        "",
        "## 训练目标匹配评分",
        "",
        f"- 评分：**{fmt(goal_score['score'], 1, '/10')}**（{goal_score['label']}）",
    ]
    lines.extend([f"- {note}" for note in goal_score["notes"]])
    if not plan_segments:
        lines.append("- 未找到可解析的每日计划分段，评分主要依据整场心率漂移和踏频稳定性，可信度低于计划内训练。")
    lines += [
        "",
        "## 心率区间",
        "",
        "| 区间 | 时间点数 | 占比 |",
        "|---|---:|---:|",
    ]
    if hr_zone_rows:
        lines.extend([f"| {name} | {count} | {pct:.1f}% |" for name, count, pct in hr_zone_rows])
    else:
        lines.append("| 无最大心率信息，未计算 | 0 | 0% |")

    lines += ["", "## 功率分布", "", "| 区间 | 时间点数 | 占比 |", "|---|---:|---:|"]
    lines.extend([f"| {name} | {count} | {pct:.1f}% |" for name, count, pct in power_rows])

    if ftp_zone_rows:
        lines += ["", "## FTP 功率区间", "", "| 区间 | 时间点数 | 占比 |", "|---|---:|---:|"]
        lines.extend([f"| {name} | {count} | {pct:.1f}% |" for name, count, pct in ftp_zone_rows])

    lines += ["", "## 踏频分布", "", "| 区间 | 时间点数 | 占比 |", "|---|---:|---:|"]
    lines.extend([f"| {name} | {count} | {pct:.1f}% |" for name, count, pct in cadence_rows])

    if plan_segments:
        lines += [
            "",
            "## 计划分段执行分析",
            "",
            "| 计划阶段 | 时间窗 | 计划强度 | 实际时长 | 平均功率 | 平均心率 | 平均踏频 | 段内心率漂移 | 执行判断 |",
            "|---|---:|---|---:|---:|---:|---:|---:|---|",
        ]
        for row in segment_metrics:
            lines.append(
                f"| {row['name']} | {row['time']} | {row['planned']} | {fmt(row['duration_min'], 1, ' 分钟')} | "
                f"{fmt(row['power'], 1, ' W')} | {fmt(row['heart_rate'], 1, ' bpm')} | {fmt(row['cadence'], 1, ' rpm')} | "
                f"{fmt(row['drift'], 1, '%')} | {row['verdict']} |"
            )
        planned_duration = sum(segment["duration_s"] for segment in plan_segments) / 60
        if minutes and minutes < planned_duration - 2:
            lines += ["", f"- 实际记录比计划分段总时长少约 {fmt(planned_duration - minutes, 1, ' 分钟')}，后续阶段可能没有完整执行。"]
        elif minutes and minutes > planned_duration + 2:
            lines += ["", f"- 实际记录比计划分段总时长多约 {fmt(minutes - planned_duration, 1, ' 分钟')}，计划外时间需结合体感判断是否属于额外热身、收尾或自由骑。"]

    lines += ["", "## 10 分钟分段", "", "| 分段 | 平均功率 | 平均心率 | 平均踏频 |", "|---|---:|---:|---:|"]
    for label, p, hr, cad in segment_rows(records):
        lines.append(f"| {label} | {fmt(p, 1, ' W')} | {fmt(hr, 1, ' bpm')} | {fmt(cad, 1, ' rpm')} |")

    lines += ["", "## 最佳滚动功率", "", "| 时间窗 | 功率 | 出现时间 |", "|---|---:|---:|"]
    for window in [5, 30, 60, 300, 600, 1200, 1800]:
        if len(records) >= window:
            best, index = best_rolling(records, "power", window)
            label = f"{window} 秒" if window < 60 else f"{window // 60} 分钟"
            lines.append(f"| {label} | {fmt(best, 1, ' W')} | {time_at(records, index)} |")

    lines += [
        "",
        "## 训练建议",
        "",
        "- 如果目标是提升基础耐力，把大多数骑行控制在能完整说话的轻松强度，避免每次都骑到中高心率。",
        "- 每周保留 1-2 次明确质量课，例如节奏骑、阈值间歇或短 VO2max 间歇，其余训练以低强度有氧和恢复为主。",
        "- 室内骑行优先保证风扇、补水和电解质；高温会显著抬高心率，使同一功率看起来更吃力。",
        "- 建议尽快补充 FTP、实测最大心率或阈值心率，后续分析会更精准。",
    ]

    out_dir = Path(args.out_dir).expanduser().resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / f"{slugify(title)}.md"
    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    copied = None
    if args.obsidian_dir:
        obsidian_dir = Path(args.obsidian_dir).expanduser().resolve()
        obsidian_dir.mkdir(parents=True, exist_ok=True)
        copied = obsidian_dir / out_path.name
        shutil.copy2(out_path, copied)
    return out_path, copied, {
        "title": title,
        "kind": kind,
        "duration_min": minutes,
        "avg_hr": avg_hr,
        "max_hr": max_seen_hr,
        "avg_power": avg_power,
        "normalized_power": npower,
        "normalized_power_estimated": npower_estimated,
        "intensity_factor": load_metrics["if"],
        "training_stress_score": load_metrics["tss"],
        "variability_index": load_metrics["vi"],
        "work_kj": load_metrics["work_kj"],
        "drift_pct": drift,
        "plan_file": str(plan_path) if plan_path and plan_segments else None,
        "plan_segments": len(plan_segments),
        "goal_score": goal_score["score"],
    }


def main():
    parser = argparse.ArgumentParser(description="Analyze a cycling FIT file and generate a Markdown training note.")
    parser.add_argument("fit_file")
    parser.add_argument("--age", type=int)
    parser.add_argument("--height-cm", type=float)
    parser.add_argument("--weight-kg", type=float)
    parser.add_argument("--athlete-name")
    parser.add_argument("--max-hr", type=int)
    parser.add_argument("--ftp", type=float)
    parser.add_argument("--out-dir", default=".")
    parser.add_argument("--obsidian-dir")
    parser.add_argument("--note-title")
    parser.add_argument("--plan-file", help="Markdown daily plan for planned-stage segment analysis. If omitted, auto-detects plans/YYYY/week-NN/YYYY-MM-DD*.md when available.")
    parser.add_argument("--no-normalize-fit-name", action="store_true", help="Do not rename today's FIT file to yyyy-MM-dd.fit.")
    parser.add_argument("--json-out", help="Also write a comprehensive JSON metrics file at this path (used by the Cycling Lab backend).")
    args = parser.parse_args()

    fit_path = Path(args.fit_file).expanduser().resolve()
    if not fit_path.exists():
        raise SystemExit(f"FIT file not found: {fit_path}")
    messages, records = parse_fit(fit_path)
    normalized_fit_path = fit_path
    if not args.no_normalize_fit_name:
        normalized_fit_path = normalize_today_fit_name(fit_path, activity_start(messages, records))
    out_path, copied, summary = write_note(args, messages, records)
    if normalized_fit_path != fit_path:
        print(f"Normalized FIT file: {normalized_fit_path}")
    print(f"Markdown note: {out_path}")
    if copied:
        print(f"Obsidian copy: {copied}")
    print(
        "Summary: "
        f"{summary['kind']}; "
        f"{fmt(summary['duration_min'], 1, ' min')}; "
        f"avg HR {fmt(summary['avg_hr'], 0, ' bpm')}; "
        f"avg power {fmt(summary['avg_power'], 0, ' W')}; "
        f"IF {fmt(summary['intensity_factor'], 2)}; "
        f"TSS {fmt(summary['training_stress_score'], 0)}; "
        f"drift {fmt(summary['drift_pct'], 1, '%')}; "
        f"plan segments {summary['plan_segments']}; "
        f"score {fmt(summary['goal_score'], 1, '/10')}"
    )


if __name__ == "__main__":
    main()
