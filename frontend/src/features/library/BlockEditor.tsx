import { useState } from "react";
import type { Block, Structure } from "./libraryApi";
import { formatDuration, totalDurationSec } from "./libraryApi";

const BLOCK_TYPES = ["warmup", "steady", "intervals", "cooldown", "rest"] as const;

export function BlockEditor({
  structure,
  onChange,
  readOnly = false,
}: {
  structure: Structure;
  onChange: (s: Structure) => void;
  readOnly?: boolean;
}) {
  const updateBlock = (idx: number, b: Block) => {
    onChange({ ...structure, blocks: structure.blocks.map((x, i) => (i === idx ? b : x)) });
  };
  const remove = (idx: number) => {
    onChange({ ...structure, blocks: structure.blocks.filter((_, i) => i !== idx) });
  };
  const move = (idx: number, dir: -1 | 1) => {
    const j = idx + dir;
    if (j < 0 || j >= structure.blocks.length) return;
    const next = [...structure.blocks];
    [next[idx], next[j]] = [next[j], next[idx]];
    onChange({ ...structure, blocks: next });
  };
  const append = (type: Block["type"]) => {
    const fresh = makeBlock(type);
    onChange({ ...structure, blocks: [...structure.blocks, fresh] });
  };

  function makeBlock(type: Block["type"]): Block {
    switch (type) {
      case "warmup": return { type, durationSec: 300, powerLow: 0.45, powerHigh: 0.6 };
      case "steady": return { type, durationSec: 1200, power: 0.65 };
      case "intervals": return {
        type,
        repeats: 1,
        on: { durationSec: 240, power: 0.88 },
        off: { durationSec: 120, power: 0.55 },
      };
      case "cooldown": return { type, durationSec: 300, powerLow: 0.55, powerHigh: 0.4 };
      case "rest": return { type, durationSec: 120 };
    }
  }

  return (
    <div className="space-y-2">
      {structure.blocks.length === 0 && (
        <p className="text-sm text-slate-500 italic">No blocks yet. Add one below.</p>
      )}
      {structure.blocks.map((b, i) => (
        <div key={i} className="border border-slate-200 rounded p-2">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-xs uppercase tracking-wide text-slate-400 font-mono w-6">#{i + 1}</span>
            <span className="text-sm font-medium text-slate-700">{b.type}</span>
            <span className="text-xs text-slate-500 ml-auto">
              {formatDuration(durationOf(b))}
            </span>
            {!readOnly && (
              <div className="flex gap-1">
                <button type="button" onClick={() => move(i, -1)} className="text-xs text-slate-400 hover:text-slate-700">↑</button>
                <button type="button" onClick={() => move(i, 1)} className="text-xs text-slate-400 hover:text-slate-700">↓</button>
                <button type="button" onClick={() => remove(i)} className="text-xs text-red-400 hover:text-red-700">×</button>
              </div>
            )}
          </div>
          <BlockFields block={b} onChange={(nb) => updateBlock(i, nb)} readOnly={readOnly} />
        </div>
      ))}
      {!readOnly && (
        <div className="flex flex-wrap gap-1 pt-2">
          {BLOCK_TYPES.map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => append(t)}
              className="text-xs px-2 py-1 border border-dashed border-slate-300 rounded hover:bg-slate-50"
            >
              + {t}
            </button>
          ))}
          <span className="text-xs text-slate-400 ml-auto self-center">
            total {formatDuration(totalDurationSec(structure))}
          </span>
        </div>
      )}
    </div>
  );
}

function durationOf(b: Block): number {
  switch (b.type) {
    case "warmup":
    case "steady":
    case "cooldown":
    case "rest":
      return b.durationSec;
    case "intervals":
      return b.repeats * (b.on.durationSec + b.off.durationSec);
  }
}

function BlockFields({
  block,
  onChange,
  readOnly,
}: {
  block: Block;
  onChange: (b: Block) => void;
  readOnly: boolean;
}) {
  const ro = readOnly;
  switch (block.type) {
    case "warmup":
      return (
        <div className="grid grid-cols-3 gap-2">
          <NumberField label="duration (sec)" value={block.durationSec} readOnly={ro} onChange={(v) => onChange({ ...block, durationSec: v })} />
          <NumberField label="powerLow" step={0.01} value={block.powerLow} readOnly={ro} onChange={(v) => onChange({ ...block, powerLow: v })} />
          <NumberField label="powerHigh" step={0.01} value={block.powerHigh} readOnly={ro} onChange={(v) => onChange({ ...block, powerHigh: v })} />
        </div>
      );
    case "steady":
      return (
        <div className="grid grid-cols-2 gap-2">
          <NumberField label="duration (sec)" value={block.durationSec} readOnly={ro} onChange={(v) => onChange({ ...block, durationSec: v })} />
          <NumberField label="power" step={0.01} value={block.power} readOnly={ro} onChange={(v) => onChange({ ...block, power: v })} />
        </div>
      );
    case "cooldown":
      return (
        <div className="grid grid-cols-3 gap-2">
          <NumberField label="duration (sec)" value={block.durationSec} readOnly={ro} onChange={(v) => onChange({ ...block, durationSec: v })} />
          <NumberField label="powerLow" step={0.01} value={block.powerLow} readOnly={ro} onChange={(v) => onChange({ ...block, powerLow: v })} />
          <NumberField label="powerHigh" step={0.01} value={block.powerHigh} readOnly={ro} onChange={(v) => onChange({ ...block, powerHigh: v })} />
        </div>
      );
    case "rest":
      return (
        <NumberField label="duration (sec)" value={block.durationSec} readOnly={ro} onChange={(v) => onChange({ ...block, durationSec: v })} />
      );
    case "intervals":
      return (
        <div className="space-y-2">
          <div className="grid grid-cols-2 gap-2">
            <NumberField label="repeats" value={block.repeats} readOnly={ro} onChange={(v) => onChange({ ...block, repeats: v })} />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <p className="text-xs text-slate-500 mb-1">on</p>
              <div className="grid grid-cols-2 gap-1">
                <NumberField label="duration" value={block.on.durationSec} readOnly={ro} onChange={(v) => onChange({ ...block, on: { ...block.on, durationSec: v } })} />
                <NumberField label="power" step={0.01} value={block.on.power} readOnly={ro} onChange={(v) => onChange({ ...block, on: { ...block.on, power: v } })} />
              </div>
            </div>
            <div>
              <p className="text-xs text-slate-500 mb-1">off</p>
              <div className="grid grid-cols-2 gap-1">
                <NumberField label="duration" value={block.off.durationSec} readOnly={ro} onChange={(v) => onChange({ ...block, off: { ...block.off, durationSec: v } })} />
                <NumberField label="power" step={0.01} value={block.off.power} readOnly={ro} onChange={(v) => onChange({ ...block, off: { ...block.off, power: v } })} />
              </div>
            </div>
          </div>
        </div>
      );
    default:
      return null;
  }
}

function NumberField({
  label,
  value,
  onChange,
  readOnly,
  step = 1,
}: {
  label: string;
  value: number;
  onChange: (v: number) => void;
  readOnly: boolean;
  step?: number;
}) {
  const [text, setText] = useState(String(value));
  return (
    <label className="block">
      <span className="text-[10px] uppercase text-slate-400 block">{label}</span>
      <input
        type="number"
        step={step}
        readOnly={readOnly}
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          const n = Number(e.target.value);
          if (Number.isFinite(n)) onChange(n);
        }}
        onBlur={() => setText(String(value))}
        className="w-full px-2 py-1 border border-slate-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:bg-slate-50"
      />
    </label>
  );
}
