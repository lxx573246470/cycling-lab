import { describe, it, expect } from "vitest";
import {
  formatBytes,
  formatDuration,
  formatOffset,
  trainingFileApi,
} from "./trainingFileApi";

describe("format helpers", () => {
  it("formats bytes", () => {
    expect(formatBytes(500)).toBe("500 B");
    expect(formatBytes(2048)).toBe("2.0 KB");
    expect(formatBytes(2 * 1024 * 1024)).toBe("2.00 MB");
  });

  it("formats duration", () => {
    expect(formatDuration(0)).toBe("-");
    expect(formatDuration(60)).toBe("1m 00s");
    expect(formatDuration(3600 + 30 * 60 + 15)).toBe("1h 30m");
    expect(formatDuration(3300)).toBe("55m 00s");
  });

  it("formats offset as m:ss", () => {
    expect(formatOffset(0)).toBe("0:00");
    expect(formatOffset(60)).toBe("1:00");
    expect(formatOffset(75)).toBe("1:15");
    expect(formatOffset(3600 + 5)).toBe("60:05");
  });
});

describe("trainingFileApi shape", () => {
  it("exposes the five endpoints", () => {
    expect(typeof trainingFileApi.list).toBe("function");
    expect(typeof trainingFileApi.get).toBe("function");
    expect(typeof trainingFileApi.upload).toBe("function");
    expect(typeof trainingFileApi.delete).toBe("function");
    expect(typeof trainingFileApi.samples).toBe("function");
  });
});