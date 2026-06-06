import { useState } from "react";
import { useNavigate, useRouter } from "@tanstack/react-router";
import { useMutation } from "@tanstack/react-query";
import { ErrorBanner, Field, PageHeader, Spinner } from "@/components/ui";
import { reviewApi } from "./reviewApi";

/**
 * Empty-state form to bootstrap a brand-new review. We bind scope + (year,
 * week) up-front; the user can then write the Markdown on the resulting
 * detail page. Defaults to a weekly review for the current ISO week.
 */
export function ReviewNewPage() {
  const router = useRouter();
  const navigate = useNavigate();
  const [scope, setScope] = useState<"WEEK" | "PHASE">("WEEK");
  const now = currentIsoWeek();
  const [year, setYear] = useState<number>(now.year);
  const [week, setWeek] = useState<number>(now.week);
  const [title, setTitle] = useState<string>(`Week ${now.week} review`);

  const create = useMutation({
    mutationFn: () =>
      reviewApi.create({
        scope,
        isoYear: scope === "WEEK" ? year : undefined,
        isoWeek: scope === "WEEK" ? week : undefined,
        title,
        contentMd: "## Highlights\n\n- \n\n## Lowlights\n\n- \n\n## Next week\n\n- ",
      }),
    onSuccess: (r) => {
      router.invalidate();
      navigate({ to: "/reviews/$id" as any, params: { id: r.id } as any });
    },
  });

  return (
    <>
      <PageHeader
        title="New review"
        description="Start a fresh review. For weekly reviews we need the (year, week) pair; the Markdown editor opens immediately after creation."
      />

      {create.error && <ErrorBanner message={(create.error as Error).message} />}

      <div className="bg-white border border-slate-200 rounded-lg p-4 space-y-3 max-w-md">
        <div>
          <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
            Scope
          </label>
          <div className="flex gap-2 mt-1">
            <ScopeButton active={scope === "WEEK"} onClick={() => setScope("WEEK")}>
              Weekly
            </ScopeButton>
            <ScopeButton active={scope === "PHASE"} onClick={() => setScope("PHASE")}>
              Phase
            </ScopeButton>
          </div>
        </div>

        <Field label="Title">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded"
          />
        </Field>

        {scope === "WEEK" && (
          <div className="grid grid-cols-2 gap-2">
            <Field label="ISO year">
              <input
                type="number"
                value={year}
                min={2000}
                max={2100}
                onChange={(e) => setYear(parseInt(e.target.value, 10) || year)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded"
              />
            </Field>
            <Field label="ISO week">
              <input
                type="number"
                value={week}
                min={1}
                max={53}
                onChange={(e) => setWeek(parseInt(e.target.value, 10) || week)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded"
              />
            </Field>
          </div>
        )}

        <button
          type="button"
          disabled={create.isPending || title.trim().length === 0}
          onClick={() => create.mutate()}
          className="px-3 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
        >
          {create.isPending ? <Spinner /> : "Create review"}
        </button>
      </div>
    </>
  );
}

function ScopeButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-3 py-1.5 text-sm rounded border ${
        active
          ? "bg-brand-500 text-white border-brand-500"
          : "border-slate-300 hover:bg-slate-50 text-slate-600"
      }`}
    >
      {children}
    </button>
  );
}

function currentIsoWeek(): { year: number; week: number } {
  const d = new Date();
  // ISO week-based year: shift to nearest Thursday.
  const target = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()));
  const dayNr = (target.getUTCDay() + 6) % 7;
  target.setUTCDate(target.getUTCDate() - dayNr + 3);
  const firstThursday = new Date(Date.UTC(target.getUTCFullYear(), 0, 4));
  const week =
    1 +
    Math.round(
      ((target.getTime() - firstThursday.getTime()) / 86400000 -
        3 +
        ((firstThursday.getUTCDay() + 6) % 7)) /
        7,
    );
  return { year: target.getUTCFullYear(), week };
}