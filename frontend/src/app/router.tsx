import { createRootRoute, createRoute, createRouter, Outlet, redirect } from "@tanstack/react-router";
import { LoginPage } from "@/features/auth/LoginPage";
import { DashboardPage } from "@/features/dashboard/DashboardPage";
import { ProfilePage } from "@/features/profile/ProfilePage";
import { ProfileZonesPage } from "@/features/profile/ProfileZonesPage";
import { LibraryListPage } from "@/features/library/LibraryListPage";
import { LibraryNewPage } from "@/features/library/LibraryNewPage";
import { LibraryDetailPage } from "@/features/library/LibraryDetailPage";
import { LibraryVersionsPage } from "@/features/library/LibraryVersionsPage";
import { LibraryVersionPage } from "@/features/library/LibraryVersionPage";
import { PlanListPage } from "@/features/plan/PlanListPage";
import { PlanNewPage } from "@/features/plan/PlanNewPage";
import { PlanEditPage } from "@/features/plan/PlanEditPage";
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

const profileRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/profile",
  component: ProfilePage,
});

const profileZonesRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/profile/zones",
  component: ProfileZonesPage,
});

const libraryListRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/library",
  component: LibraryListPage,
});

const libraryNewRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/library/new",
  component: LibraryNewPage,
});

const libraryDetailRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/library/$id",
  component: LibraryDetailPage,
});

const libraryVersionsRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/library/$id/versions",
  component: LibraryVersionsPage,
});

const libraryVersionRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/library/$id/versions/$version",
  component: LibraryVersionPage,
});

const planListRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/plans",
  component: PlanListPage,
});

const planNewRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/plans/new",
  component: PlanNewPage,
});

const planEditRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/plans/$id",
  component: PlanEditPage,
});

protectedLayout.addChildren([
  indexRoute,
  profileRoute,
  profileZonesRoute,
  libraryListRoute,
  libraryNewRoute,
  libraryDetailRoute,
  libraryVersionsRoute,
  libraryVersionRoute,
  planListRoute,
  planNewRoute,
  planEditRoute,
]);

const routeTree = rootRoute.addChildren([loginRoute, protectedLayout]);

const router = createRouter({ routeTree });

export { routeTree, router };