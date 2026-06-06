import { useMemo } from "react";
import type { Block, Structure } from "./libraryApi";

/**
 * Renders a power vs time curve for a {@link Structure}. Pure SVG, no chart
 * library — M1 is intentionally dependency-light. Each block is a step
 * segment at its power level; for intervals the on/off are repeated.
 */
export function PowerCurvePreview({ structure, ftp = 200 }: { structure: Structure; ftp?: number }) {
  const segments = useMemo(() => expandSegments(structure), [structure]);
  if (segments.length === 0) {
    return <p className="text-sm text-slate-500 italic">No blocks.</p>;
  }
  const totalSec = segments[segments.length - 1].endSec;
  const w = 480;
  const h = 160;
  const padL = 36;
  const padR = 8;
  const padT = 8;
  const padB = 22;
  const x = (s: number) => padL + (s / totalSec) * (w - padL - padR);
  const y = (power: number) => h - padB - power * (h - padT - padB);

  const watts = (p: number) => Math.round(p * ftp);

  return (
    <div>
      <svg viewBox={`0 0 ${w} ${h}`} className="w-full h-40">
        {/* gridlines at 50%, 75%, 100%, 125% FTP */}
        {[0.5, 0.75, 1.0, 1.25].map((g) => (
          <g key={g}>
            <line x1={padL} x2={w - padR} y1={y(g)} y2={y(g)} stroke="#e2e8f0" strokeDasharray="2 2" />
            <text x={2} y={y(g) + 3} fontSize="9" fill="#94a3b8">{Math.round(g * 100)}%</text>
          </g>
        ))}
        {/* the workout shape */}
        <polyline
          points={segments
            .map((s) => `${x(s.startSec)},${y(s.power)}`)
            .join(" ")}
          fill="none"
          stroke="#1d6fd8"
          strokeWidth="2"
        />
        {/* block labels */}
        {segments.filter((s, i) => i === 0 || s.label !== segments[i - 1].label || s.startSec === 0).map((s) => (
          <text
            key={s.startSec}
            x={x(s.startSec)}
            y={y(s.power) - 4}
            fontSize="9"
            fill="#1d6fd8"
          >
            {s.label} · {watts(s.power)}W
          </text>
        ))}
        {/* x-axis ticks */}
        {[0, 0.25, 0.5, 0.75, 1.0].map((t) => (
          <text key={t} x={x(totalSec * t)} y={h - 6} fontSize="9" fill="#94a3b8" textAnchor="middle">
            {Math.round((totalSec * t) / 60)}m
          </text>
        ))}
      </svg>
      <p className="text-xs text-slate-500 mt-1">
        Showing power over time assuming FTP={ftp}W. Total{" "}
        {Math.round(totalSec / 60)} min.
      </p>
    </div>
  );
}

interface Segment {
  startSec: number;
  endSec: number;
  power: number;
  label: string;
}

function expandSegments(structure: Structure): Segment[] {
  const out: Segment[] = [];
  let cursor = 0;
  for (const b of structure.blocks) {
    out.push(...segmentsFor(b, cursor));
    cursor = endOf(b, cursor);
  }
  return out;
}

function segmentsFor(b: Block, cursor: number): Segment[] {
  switch (b.type) {
    case "warmup":
    case "cooldown":
      return [{
        startSec: cursor,
        endSec: cursor + b.durationSec,
        power: (b.powerLow + b.powerHigh) / 2,
        label: b.type,
      }];
    case "steady":
      return [{
        startSec: cursor,
        endSec: cursor + b.durationSec,
        power: b.power,
        label: "steady",
      }];
    case "rest":
      return [{
        startSec: cursor,
        endSec: cursor + b.durationSec,
        power: 0.4,
        label: "rest",
      }];
    case "intervals": {
      const segs: Segment[] = [];
      const cycle = b.on.durationSec + b.off.durationSec;
      for (let r = 0; r < b.repeats; r++) {
        const start = cursor + r * cycle;
        segs.push({
          startSec: start,
          endSec: start + b.on.durationSec,
          power: b.on.power,
          label: r === 0 ? "on" : "",
        });
        segs.push({
          startSec: start + b.on.durationSec,
          endSec: start + cycle,
          power: b.off.power,
          label: r === 0 ? "off" : "",
        });
      }
      return segs;
    }
    default: return [];
  }
}

function endOf(b: Block, cursor: number): number {
  switch (b.type) {
    case "warmup":
    case "steady":
    case "cooldown":
    case "rest":
      return cursor + b.durationSec;
    case "intervals":
      return cursor + b.repeats * (b.on.durationSec + b.off.durationSec);
    default: return cursor;
  }
}
