import { describe, expect, it, beforeEach } from "vitest";
import { useAuthStore } from "@/features/auth/authStore";

describe("authStore", () => {
  beforeEach(() => {
    useAuthStore.getState().clear();
  });

  it("starts empty", () => {
    const s = useAuthStore.getState();
    expect(s.accessToken).toBeNull();
    expect(s.refreshToken).toBeNull();
    expect(s.user).toBeNull();
  });

  it("setSession populates all fields", () => {
    useAuthStore.getState().setSession({
      accessToken: "a",
      refreshToken: "r",
      user: { id: "u1", email: "x@y.z", displayName: "x", role: "USER" },
    });
    const s = useAuthStore.getState();
    expect(s.accessToken).toBe("a");
    expect(s.refreshToken).toBe("r");
    expect(s.user?.id).toBe("u1");
  });

  it("clear empties the store", () => {
    useAuthStore.getState().setSession({
      accessToken: "a",
      refreshToken: "r",
      user: { id: "u1", email: "x@y.z", displayName: "x", role: "USER" },
    });
    useAuthStore.getState().clear();
    const s = useAuthStore.getState();
    expect(s.accessToken).toBeNull();
    expect(s.refreshToken).toBeNull();
    expect(s.user).toBeNull();
  });
});
