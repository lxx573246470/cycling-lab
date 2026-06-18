# Training Load Model

## Policy

- 单次训练的 TSS、IF、NP/FTP 使用训练发生当时的 FTP。
- FTP 后续变化时，不重算历史 TSS。
- 每条负荷记录保留 `ftp_used`，用于说明当时采用的 FTP。
- `CTL`、`ATL`、`TSB` 基于历史已经锁定的每日 TSS 继续滚动计算。

## Files

- `training/load-summary.csv`：每日训练负荷汇总。
- `scripts/update_training_load.py`：从 `training/**/notes/*.md` 重新生成负荷汇总。

## Columns

- `date`：训练日期；无训练日也会保留，用于滚动 CTL/ATL。
- `iso_week`：ISO 周。
- `activity_count`：当天纳入负荷计算的训练笔记数量。
- `duration_min`：当天纳入计算的训练总分钟数。
- `tss`：当天 TSS 总和。
- `ctl`：42 天模型的长期训练负荷。
- `atl`：7 天模型的短期训练负荷。
- `tsb`：`CTL - ATL`，用于观察新鲜度。
- `ftp_used`：当天 TSS 使用的 FTP；多次训练且 FTP 不同时用 `/` 分隔。
- `ftp_policy`：固定为 `historical_ftp_locked`。
- `tss_source`：`recorded` 表示笔记已记录 TSS，`calculated` 表示由时长、NP、FTP 推算。
- `source_notes`：参与当天负荷计算的训练笔记路径。

## Update

```bash
python3 scripts/update_training_load.py
```

如果要把无训练日滚动到某个日期：

```bash
python3 scripts/update_training_load.py --through-date 2026-06-17
```
