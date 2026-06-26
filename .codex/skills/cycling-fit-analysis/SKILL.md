---
name: cycling-fit-analysis
description: Analyze cycling or indoor-cycling FIT activity files and generate reusable Markdown training notes. Use when Codex is asked to analyze bike training data, parse .fit workout files, produce training guidance from heart rate/power/cadence, or create notes that can later be synced into Obsidian.
---

# Cycling FIT Analysis

## Workflow

Use `scripts/analyze_fit.py` for FIT files whenever possible. It parses session and 1-second record data, calculates heart-rate zones, cadence/power distributions, heart-rate drift, best rolling power, plan-stage execution, and writes a Chinese Markdown training note.

Before analyzing any raw FIT file in the cycling-lab repository, use `$normalize-fit-filenames` to rename the source file to a stable activity-date filename. Pass the normalized path to `scripts/analyze_fit.py` and link that normalized source file from the generated note.

Prefer planned-stage analysis over whole-ride-only analysis. If a matching daily plan exists under `plans/YYYY/week-NN/YYYY-MM-DD*.md`, the script auto-detects it and slices the FIT records by the plan's `阶段` / `时间` table. If the plan is elsewhere, pass `--plan-file /path/to/daily-plan.md`. Use the whole-ride summary only as context; the coaching interpretation should discuss each planned segment such as warmup, main sets, recovery valleys, cooldown, and any plan-shortfall or extra unplanned riding.

Every planned-workout summary should include a 10-point training-goal match score, for example `7.5/10`. Base the score on the stated plan goal, completion versus planned duration, segment power adherence, heart-rate drift/control, and cadence stability. Explain the main reasons for the score and the most important deduction instead of presenting the number alone.

The analyzer may also normalize today's FIT filename in place, but do not rely on that behavior for historical uploads. `$normalize-fit-filenames` is the required pre-analysis normalization step for all raw FIT files.

Run from the skill directory or call the script by absolute path:

```bash
python3 scripts/analyze_fit.py /path/to/activity.fit --age 31 --height-cm 176 --weight-kg 70 --out-dir /path/to/output
```

When analyzing a planned workout explicitly, include the plan file:

```bash
python3 scripts/analyze_fit.py /path/to/activity.fit --age 31 --height-cm 176 --weight-kg 70 --ftp 200 --plan-file plans/2026/week-24/2026-06-09-controlled-low-z2.md --out-dir /path/to/output
```

Optional arguments:

- `--athlete-name`: Add a name to the note title/frontmatter.
- `--max-hr`: Use known max heart rate instead of `220 - age`.
- `--ftp`: Add FTP-based power-zone context when available.
- `--obsidian-dir`: Also write a copy of the Markdown note into an Obsidian vault/folder.
- `--note-title`: Override the generated note title.
- `--plan-file`: Use a specific Markdown daily plan for planned-stage segment analysis. If omitted, the script tries to auto-detect the plan by activity date.
- `--no-normalize-fit-name`: Keep the uploaded FIT filename unchanged.

## Analysis Guidance

Interpret the generated metrics, then add practical coaching context:

- If most time is in heart-rate Z3/Z4, call it tempo/threshold-leaning rather than easy aerobic work.
- If heart-rate drift is under about 5%, aerobic durability for that effort was stable; above 5-7% suggests heat, dehydration, fatigue, or pacing drift.
- For planned workouts, analyze warmup, each work interval, each recovery segment, and cooldown separately. Compare actual average power/heart rate/cadence against that segment's target before making whole-ride claims.
- Tie the conclusion back to the rider's current goal and the specific daily/weekly plan. A good score means the workout served the intended goal, not merely that the total average looked good.
- For indoor cycling, expect speed and distance to be missing or zero; prioritize heart rate, power, cadence, elapsed time, calories, and stability.
- Use estimated max heart rate only as a placeholder. Recommend a real max-HR, lactate-threshold-HR, or FTP test before prescribing precise zones.
- Separate advice into immediate observations, risks/opportunities, and the next 1-4 weeks of training.

## Output Style

Return the path to the generated Markdown note and summarize the key findings in chat. Keep the note useful for Obsidian: include YAML frontmatter, concise sections, and stable headings.

When the user asks to sync with Obsidian, use `--obsidian-dir` if they provide a vault/folder path. If no path is known, create the Markdown locally and ask for the Obsidian folder for future automatic copies.
