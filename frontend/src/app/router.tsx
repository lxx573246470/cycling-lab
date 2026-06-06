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
import { WorkoutsListPage } from "@/features/workout/WorkoutsListPage";
import { ReviewsListPage } from "@/features/review/ReviewsListPage";
import { ReviewDetailPage } from "@/features/review/ReviewDetailPage";
import { ReviewNewPage } from "@/features/review/ReviewNewPage";
import { AdminUsersPage } from "@/features/admin/AdminUsersPage";
import { TrainingsListPage } from "@/features/training/TrainingsListPage";
import { TrainingDetailPage } from "@/features/training/TrainingDetailPage";
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

const workoutsListRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/workouts",
  component: WorkoutsListPage,
});

const trainingsListRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/trainings",
  component: TrainingsListPage,
});

const reviewsListRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/reviews",
  component: ReviewsListPage,
});

const reviewNewRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/reviews/new",
  component: ReviewNewPage,
});

const reviewDetailRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/reviews/$id",
  component: ReviewDetailPage,
});

const adminUsersRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/admin/users",
  component: AdminUsersPage,
});

const trainingDetailRoute = createRoute({
  getParentRoute: () => protectedLayout,
  path: "/trainings/$id",
  component: TrainingDetailPage,
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
  workoutsListRoute,
  trainingsListRoute,
  trainingDetailRoute,
  reviewsListRoute,
  reviewNewRoute,
  reviewDetailRoute,
  adminUsersRoute,
]);

const routeTree = rootRoute.addChildren([loginRoute, protectedLayout]);

const router = createRouter({ routeTree });

export { routeTree, router };