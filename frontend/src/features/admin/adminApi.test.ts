import { describe, it, expect } from "vitest";
import {
  adminApi,
  adminUserPatchSchema,
  userRoleSchema,
  userStatusSchema,
} from "./adminApi";

describe("adminApi shape", () => {
  it("exposes the four endpoints", () => {
    expect(typeof adminApi.list).toBe("function");
    expect(typeof adminApi.get).toBe("function");
    expect(typeof adminApi.patch).toBe("function");
    expect(typeof adminApi.delete).toBe("function");
  });
});

describe("adminUserPatchSchema", () => {
  it("requires role + status", () => {
    expect(adminUserPatchSchema.safeParse({ role: "ADMIN", status: "ACTIVE" }).success).toBe(true);
    expect(adminUserPatchSchema.safeParse({ role: "USER" }).success).toBe(false);
    expect(adminUserPatchSchema.safeParse({ status: "ACTIVE" }).success).toBe(false);
  });
});

describe("enums", () => {
  it("userRoleSchema covers USER + ADMIN", () => {
    expect(userRoleSchema.parse("USER")).toBe("USER");
    expect(userRoleSchema.parse("ADMIN")).toBe("ADMIN");
    expect(userRoleSchema.safeParse("ROOT").success).toBe(false);
  });
  it("userStatusSchema covers ACTIVE + DISABLED", () => {
    expect(userStatusSchema.parse("ACTIVE")).toBe("ACTIVE");
    expect(userStatusSchema.parse("DISABLED")).toBe("DISABLED");
    expect(userStatusSchema.safeParse("BANNED").success).toBe(false);
  });
});