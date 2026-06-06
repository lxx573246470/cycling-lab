import { describe, it, expect } from "vitest";
import { totalDurationSec, formatDuration, parseStructure, CATEGORIES, type Structure } from "./libraryApi";

describe("libraryApi helpers", () => {
  it("totalDurationSec sums warmup + steady + cooldown", () => {
    const s: Structure = {
      blocks: [
        { type: "warmup", durationSec: 600, powerLow: 0.45, powerHigh: 0.6 },
        { type: "steady", durationSec: 1800, power: 0.65 },
        { type: "cooldown", durationSec: 300, powerLow: 0.55, powerHigh: 0.4 },
      ],
    };
    expect(totalDurationSec(s)).toBe(2700);
  });

  it("totalDurationSec multiplies intervals by repeats", () => {
    const s: Structure = {
      blocks: [
        {
          type: "intervals",
          repeats: 3,
          on: { durationSec: 240, power: 0.88 },
          off: { durationSec: 120, power: 0.55 },
        },
      ],
    };
    expect(totalDurationSec(s)).toBe(3 * (240 + 120));
  });

  it("formatDuration formats human-readable strings", () => {
    expect(formatDuration(30)).toBe("30s");
    expect(formatDuration(60)).toBe("1m");
    expect(formatDuration(90)).toBe("1m30s");
    expect(formatDuration(3600)).toBe("1h");
    expect(formatDuration(4500)).toBe("1h15m");
  });

  it("parseStructure accepts valid JSON", () => {
    const json = JSON.stringify({
      blocks: [{ type: "steady", durationSec: 600, power: 0.65 }],
    });
    expect(parseStructure(json)).not.toBeNull();
  });

  it("parseStructure rejects invalid JSON", () => {
    expect(parseStructure("not json")).toBeNull();
  });

  it("CATEGORIES contains the 6 expected codes", () => {
    const codes = CATEGORIES.map((c) => c.code);
    expect(codes).toEqual([
      "endurance", "recovery", "intervals", "outdoor", "testing", "strength",
    ]);
  });
});
