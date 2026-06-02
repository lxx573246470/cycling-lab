import { createRootRoute, createRoute, createRouter, Outlet, redirect } from "@tanstack/react-router";
import { LoginPage } from "@/features/auth/LoginPage";
import { DashboardPage } from "@/features/dashboard/DashboardPage";
import { AppLayout } from "@/app/AppLayout";
import { useAuthStore } from "@/features/auth/authStore";

const rootRoute = createRootRoute({
  component: () => <Outlet />,
});

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: "/login",
  component: LoginPage,
  beforeLoad: () => {
    const { accessToken } = useAuthStore.getState();
    if (accessToken) {
      throw redirect({ to: "/" });
    }
  },
});

const protectedLayout = createRoute({
  getParentRoute: () => rootRoute,
  id: "_protected",
  component: AppLayout,
  beforeLoad: () => {
    const { accessToken } = useAuthStore.getState();
    if (!accessToken) {
      throw redirect({ to: "/login" });
    }
  },
});

const indexRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/",
  component: DashboardPage,
});

protectedLayout.addChildren([indexRoute]);
const routeTree = rootRoute.addChildren([loginRoute, protectedLayout]);

const router = createRouter({ routeTree });

export { routeTree, router };
