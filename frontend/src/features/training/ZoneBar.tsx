/**
 * Stacked horizontal bar showing the % time spent in each zone, with a
 * tiny table underneath for raw counts. Pure inline SVG so we don't need
 * a chart library. The {@code rows} shape matches what the backend stores
 * in the JSONB columns: {@code {name, count, pct}}.
 */
export function ZoneBar({
  rows,
  unit,
}: {
  rows: Array<{ name?: string; count?: number; pct?: number }> | null | undefined;
  unit: string;
}) {
  if (!rows || rows.length === 0) {
    return <p className="text-sm text-slate-500 italic">没有分区数据。</p>;
  }
  const total = rows.reduce((s, r) => s + (r.pct ?? 0), 0) || 1;
  const palette = ["#dbeafe", "#bfdbfe", "#93c5fd", "#60a5fa", "#3b82f6", "#2563eb", "#1d4ed8", "#1e3a8a"];

  return (
    <div>
      <div className="flex h-3 w-full rounded overflow-hidden border border-slate-200">
        {rows.map((r, i) => {
          const pct = ((r.pct ?? 0) / total) * 100;
          if (pct <= 0) return null;
          return (
            <div
              key={i}
              style={{ width: `${pct}%`, backgroundColor: palette[i % palette.length] }}
              title={`${r.name ?? "Z" + i}: ${pct.toFixed(1)}%`}
            />
          );
        })}
      </div>
      <table className="w-full text-xs mt-3">
        <thead>
          <tr className="text-slate-500">
            <th className="text-left py-0.5">区间</th>
            <th className="text-right py-0.5">时间</th>
            <th className="text-right py-0.5">%</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} className="border-t border-slate-100">
              <td className="py-0.5 text-slate-700">
                <span
                  className="inline-block w-2 h-2 rounded-sm mr-1.5 align-middle"
                  style={{ backgroundColor: palette[i % palette.length] }}
                />
                {r.name ?? `Z${i}`}
              </td>
              <td className="py-0.5 text-right font-mono text-slate-700">
                {r.count != null ? `${r.count}秒` : "-"}
              </td>
              <td className="py-0.5 text-right font-mono text-slate-900">
                {(r.pct ?? 0).toFixed(1)}%
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="text-[10px] text-slate-400 mt-2">单位：{unit}</p>
    </div>
  );
}
