import { useEffect, type ReactNode } from "react";
import { useAuthStore } from "@/features/auth/authStore";
import { fetchMe } from "@/features/auth/authApi";

export function AuthBootstrap({ children }: { children: ReactNode }) {
  const { accessToken, user, setSession, clear } = useAuthStore();

  useEffect(() => {
    if (!accessToken) return;
    if (user) return;
    let cancelled = false;
    fetchMe()
      .then((me) => {
        if (cancelled) return;
        const { refreshToken } = useAuthStore.getState();
        if (refreshToken) {
          setSession({ accessToken, refreshToken, user: me });
        }
      })
      .catch(() => {
        if (!cancelled) clear();
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, user, setSession, clear]);

  return <>{children}</>;
}
