import { useMemo } from "react";
import type { TrainingSample } from "./trainingFileApi";

/**
 * Renders a two-line SVG chart: power (left axis) and heart rate (right
 * axis), both vs elapsed seconds. No chart library — pure SVG, kept light
 * because M3 ships it inline in the detail page.
 */
export function TrainingChart({ samples }: { samples: TrainingSample[] }) {
  const data = useMemo(() => {
    if (!samples || samples.length === 0) return null;
    const maxT = Math.max(60, samples[samples.length - 1].tOffsetSec);
    let maxPower = 0;
    let maxHr = 0;
    for (const s of samples) {
      if (s.power != null && s.power > maxPower) maxPower = s.power;
      if (s.hr != null && s.hr > maxHr) maxHr = s.hr;
    }
    if (maxPower < 1) maxPower = 1;
    if (maxHr < 1) maxHr = 1;
    return { maxT, maxPower, maxHr };
  }, [samples]);

  if (!data) {
    return <p className="text-sm text-slate-500 italic">没有采样数据。</p>;
  }

  const w = 480;
  const h = 180;
  const padL = 40;
  const padR = 36;
  const padT = 10;
  const padB = 22;
  const x = (t: number) => padL + (t / data.maxT) * (w - padL - padR);
  const yPower = (p: number) => h - padB - (p / data.maxPower) * (h - padT - padB);
  const yHr = (bpm: number) => h - padB - (bpm / data.maxHr) * (h - padT - padB);

  // Build polyline strings, skipping gaps where data is missing.
  const powerPath = buildPath(samples, x, yPower, "power");
  const hrPath = buildPath(samples, x, yHr, "hr");

  const durationLabel = formatTotalDuration(samples[samples.length - 1].tOffsetSec);

  return (
    <div>
      <svg viewBox={`0 0 ${w} ${h}`} className="w-full h-48">
        <g>
          {[0, 0.25, 0.5, 0.75, 1.0].map((f) => (
            <line
              key={f}
              x1={padL}
              x2={w - padR}
              y1={padT + f * (h - padT - padB)}
              y2={padT + f * (h - padT - padB)}
              stroke="#e2e8f0"
              strokeWidth={1}
            />
          ))}
        </g>
        <text x={4} y={padT + 8} className="text-[10px]" fill="#475569">
          {data.maxPower} W
        </text>
        <text x={4} y={h - padB} className="text-[10px]" fill="#475569">
          0 W
        </text>
        <text x={w - padR + 4} y={padT + 8} className="text-[10px]" fill="#475569">
          {data.maxHr} bpm
        </text>
        <text x={w - padR + 4} y={h - padB} className="text-[10px]" fill="#475569">
          0
        </text>
        <path d={hrPath} fill="none" stroke="#ef4444" strokeWidth={1.2} />
        <path d={powerPath} fill="none" stroke="#2563eb" strokeWidth={1.2} />
        <text x={padL} y={h - 6} className="text-[10px]" fill="#475569">
          0:00
        </text>
        <text
          x={w - padR}
          y={h - 6}
          className="text-[10px]"
          fill="#475569"
          textAnchor="end"
        >
          {durationLabel}
        </text>
      </svg>
      <div className="flex gap-4 mt-1 text-xs text-slate-500">
        <span>
          <span className="inline-block w-2 h-2 bg-blue-600 rounded-full mr-1" />
          功率
        </span>
        <span>
          <span className="inline-block w-2 h-2 bg-red-500 rounded-full mr-1" />
          心率
        </span>
      </div>
    </div>
  );
}

function buildPath(
  samples: TrainingSample[],
  x: (t: number) => number,
  y: (v: number) => number,
  field: "power" | "hr",
): string {
  const out: string[] = [];
  let pen = "M";
  for (const s of samples) {
    const v = field === "power" ? s.power : s.hr;
    if (v == null) {
      pen = "M";
      continue;
    }
    out.push(`${pen}${x(s.tOffsetSec).toFixed(1)},${y(v).toFixed(1)}`);
    pen = "L";
  }
  return out.join(" ");
}

function formatTotalDuration(sec: number): string {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  return `${m}:${s.toString().padStart(2, "0")}`;
}
