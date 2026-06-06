import { describe, it, expect } from "vitest";
import { reviewApi, reviewCreateSchema, reviewUpdateSchema } from "./reviewApi";

describe("reviewApi shape", () => {
  it("exposes the six endpoints", () => {
    expect(typeof reviewApi.list).toBe("function");
    expect(typeof reviewApi.get).toBe("function");
    expect(typeof reviewApi.byWeek).toBe("function");
    expect(typeof reviewApi.create).toBe("function");
    expect(typeof reviewApi.update).toBe("function");
    expect(typeof reviewApi.delete).toBe("function");
  });
});

describe("reviewCreateSchema", () => {
  it("requires scope + title + contentMd", () => {
    const r = reviewCreateSchema.safeParse({
      scope: "WEEK",
      title: "W22",
      contentMd: "ok",
    });
    expect(r.success).toBe(true);
  });
  it("rejects empty title", () => {
    const r = reviewCreateSchema.safeParse({
      scope: "WEEK",
      title: "",
      contentMd: "x",
    });
    expect(r.success).toBe(false);
  });
});

describe("reviewUpdateSchema", () => {
  it("requires year + week + title + contentMd", () => {
    const r = reviewUpdateSchema.safeParse({
      isoYear: 2026,
      isoWeek: 22,
      title: "x",
      contentMd: "y",
    });
    expect(r.success).toBe(true);
  });
});