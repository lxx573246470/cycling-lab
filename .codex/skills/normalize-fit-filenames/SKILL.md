---
name: normalize-fit-filenames
description: Normalize cycling FIT source filenames before training analysis or summary generation. Use when Codex imports, analyzes, summarizes, or links raw .fit activity files in cycling-lab, especially files named with export timestamps or unclear upload names such as activity.fit, 2026-06-25-21-37-29.fit, or Garmin/Zwift export names.
---

# Normalize FIT Filenames

## Workflow

Run this skill before calling `scripts/analyze_fit.py` or writing a training summary from a raw `.fit` file.

1. Identify the raw FIT file path.
2. Run the bundled script from the repository root:

```bash
python3 .codex/skills/normalize-fit-filenames/scripts/normalize_fit_filename.py path/to/activity.fit
```

3. Use the script's `normalized_path` output for all subsequent analysis commands and note links.
4. If a training note is created or edited, link the normalized source file in the note, for example:

```markdown
- 源 FIT：[2026-06-25.fit](../fit/2026-06-25.fit)
```

## Naming Rules

- Read the activity date from FIT metadata, preferring session `start_time`, then file_id `time_created`, then the first record timestamp.
- Rename non-canonical files to `YYYY-MM-DD.fit` in the same directory.
- If that file already exists, use `YYYY-MM-DD-1.fit`, `YYYY-MM-DD-2.fit`, and so on.
- Treat `YYYY-MM-DD.fit` and `YYYY-MM-DD-N.fit` as already canonical for that activity date.
- Never overwrite an existing different file.
- If the FIT date cannot be read, do not rename; report the blocker and continue with the original path only if analysis can still proceed.

## Required Follow-Through

- Pass the normalized path into `scripts/analyze_fit.py`.
- Update any created note's source FIT link to the normalized filename.
- Search for stale references to the old filename before finalizing:

```bash
rg "old-fit-file-name" training plans review
```

- If git status shows an add/delete pair for the old and new FIT paths, stage only those paths if needed so the worktree reflects the intended rename/add state.
