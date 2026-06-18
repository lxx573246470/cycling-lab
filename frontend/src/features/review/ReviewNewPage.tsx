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
  const [title, setTitle] = useState<string>(`${now.year} 年 W${now.week} 周复盘`);

  const create = useMutation({
    mutationFn: () =>
      reviewApi.create({
        scope,
        isoYear: scope === "WEEK" ? year : undefined,
        isoWeek: scope === "WEEK" ? week : undefined,
        title,
        contentMd: "## 亮点\n\n- \n\n## 问题\n\n- \n\n## 下周计划\n\n- ",
      }),
    onSuccess: (r) => {
      router.invalidate();
      navigate({ to: "/reviews/$id" as any, params: { id: r.id } as any });
    },
  });

  return (
    <>
      <PageHeader
        title="新建复盘"
        description="创建新的复盘。周复盘需要填写 ISO 年和周数，创建后会进入 Markdown 编辑器。"
      />

      {create.error && <ErrorBanner message={(create.error as Error).message} />}

      <div className="bg-white border border-slate-200 rounded-lg p-4 space-y-3 max-w-md">
        <div>
          <label className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
            范围
          </label>
          <div className="flex gap-2 mt-1">
            <ScopeButton active={scope === "WEEK"} onClick={() => setScope("WEEK")}>
              周复盘
            </ScopeButton>
            <ScopeButton active={scope === "PHASE"} onClick={() => setScope("PHASE")}>
              阶段复盘
            </ScopeButton>
          </div>
        </div>

        <Field label="标题">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded"
          />
        </Field>

        {scope === "WEEK" && (
          <div className="grid grid-cols-2 gap-2">
            <Field label="ISO 年">
              <input
                type="number"
                value={year}
                min={2000}
                max={2100}
                onChange={(e) => setYear(parseInt(e.target.value, 10) || year)}
                className="w-full px-3 py-2 text-sm border border-slate-300 rounded"
              />
            </Field>
            <Field label="ISO 周">
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
          {create.isPending ? <Spinner /> : "创建复盘"}
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
