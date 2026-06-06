import { describe, it, expect } from "vitest";
import { formatBytes, workoutFileApi } from "./workoutFileApi";

describe("formatBytes", () => {
  it("formats B / KB / MB", () => {
    expect(formatBytes(500)).toBe("500 B");
    expect(formatBytes(2048)).toBe("2.0 KB");
    expect(formatBytes(2 * 1024 * 1024)).toBe("2.00 MB");
  });
});

describe("workoutFileApi shape", () => {
  it("exposes the five endpoints", () => {
    expect(typeof workoutFileApi.list).toBe("function");
    expect(typeof workoutFileApi.get).toBe("function");
    expect(typeof workoutFileApi.create).toBe("function");
    expect(typeof workoutFileApi.delete).toBe("function");
    expect(typeof workoutFileApi.download).toBe("function");
  });
});