---
name: cycling-fit-analysis
description: Analyze cycling or indoor-cycling FIT activity files and generate reusable Markdown training notes. Use when Codex is asked to analyze bike training data, parse .fit workout files, produce training guidance from heart rate/power/cadence, or create notes that can later be synced into Obsidian.
---

# Cycling FIT Analysis

## Workflow

Use `scripts/analyze_fit.py` for FIT files whenever possible. It parses session and 1-second record data, calculates heart-rate zones, cadence/power distributions, heart-rate drift, best rolling power, and writes a Chinese Markdown training note.

When the parsed activity date is today, the script also normalizes the source FIT filename in place:

- First file for the day: `yyyy-MM-dd.fit`
- Additional files for the same day: `yyyy-MM-dd-1.fit`, `yyyy-MM-dd-2.fit`, etc.
- Existing canonical names are left unchanged.
- Past activity files are not renamed.

Run from the skill directory or call the script by absolute path:

```bash
python3 scripts/analyze_fit.py /path/to/activity.fit --age 31 --height-cm 176 --weight-kg 70 --out-dir /path/to/output
```

Optional arguments:

- `--athlete-name`: Add a name to the note title/frontmatter.
- `--max-hr`: Use known max heart rate instead of `220 - age`.
- `--ftp`: Add FTP-based power-zone context when available.
- `--obsidian-dir`: Also write a copy of the Markdown note into an Obsidian vault/folder.
- `--note-title`: Override the generated note title.
- `--no-normalize-fit-name`: Keep the uploaded FIT filename unchanged.

## Analysis Guidance

Interpret the generated metrics, then add practical coaching context:

- If most time is in heart-rate Z3/Z4, call it tempo/threshold-leaning rather than easy aerobic work.
- If heart-rate drift is under about 5%, aerobic durability for that effort was stable; above 5-7% suggests heat, dehydration, fatigue, or pacing drift.
- For indoor cycling, expect speed and distance to be missing or zero; prioritize heart rate, power, cadence, elapsed time, calories, and stability.
- Use estimated max heart rate only as a placeholder. Recommend a real max-HR, lactate-threshold-HR, or FTP test before prescribing precise zones.
- Separate advice into immediate observations, risks/opportunities, and the next 1-4 weeks of training.

## Output Style

Return the path to the generated Markdown note and summarize the key findings in chat. Keep the note useful for Obsidian: include YAML frontmatter, concise sections, and stable headings.

When the user asks to sync with Obsidian, use `--obsidian-dir` if they provide a vault/folder path. If no path is known, create the Markdown locally and ask for the Obsidian folder for future automatic copies.
