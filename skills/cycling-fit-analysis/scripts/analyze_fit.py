#!/usr/bin/env python3
import argparse
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
    drift = decoupling(records)
    kind, drift_text = classify_session(hr_zone_rows, drift)

    duration = session.get("total_timer_time") or session.get("total_elapsed_time")
    minutes = duration / 60 if numeric(duration) else (len(records) / 60 if records else None)
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

    lines += ["", "## 强度判断", "", f"- 本次更接近：**{kind}**"]
    if max_hr:
        lines.append(f"- 心率区间按最大心率 {max_hr} bpm 估算；如果你有实测最大心率或阈值心率，应优先使用实测值。")
    lines += [
        "- 室内骑行若速度/距离为 0 属正常记录现象，本分析优先使用心率、功率和踏频。",
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

    lines += ["", "## 踏频分布", "", "| 区间 | 时间点数 | 占比 |", "|---|---:|---:|"]
    lines.extend([f"| {name} | {count} | {pct:.1f}% |" for name, count, pct in cadence_rows])

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
        "drift_pct": drift,
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
    parser.add_argument("--no-normalize-fit-name", action="store_true", help="Do not rename today's FIT file to yyyy-MM-dd.fit.")
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
        f"drift {fmt(summary['drift_pct'], 1, '%')}"
    )


if __name__ == "__main__":
    main()
