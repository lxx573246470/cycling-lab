import { useNavigate } from "@tanstack/react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ErrorBanner, Field, PageHeader } from "@/components/ui";
import {
  currentIsoWeek,
  isoDatesOf,
  planApi,
  weeklyPlanCreateSchema,
  type WeeklyPlanCreateInput,
  type WeeklyPlanDto,
} from "./planApi";

const inputCls =
  "w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent";

export function PlanNewPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const current = currentIsoWeek();

  const { register, handleSubmit, formState, watch } = useForm<WeeklyPlanCreateInput>({
    resolver: zodResolver(weeklyPlanCreateSchema),
    defaultValues: {
      isoYear: current.year,
      isoWeek: current.week,
      title: "",
      goalMd: "",
    },
  });

  const watchedYear = watch("isoYear");
  const watchedWeek = watch("isoWeek");

  const create = useMutation({
    mutationFn: (v: WeeklyPlanCreateInput) => planApi.create(v),
    onSuccess: (created: WeeklyPlanDto) => {
      qc.invalidateQueries({ queryKey: ["plans"] });
      navigate({ to: "/plans/$id" as any, params: { id: created.id } as any });
    },
  });

  const onSubmit = handleSubmit((values) => {
    create.mutate({
      ...values,
      title: values.title?.trim() ? values.title.trim() : null,
      goalMd: values.goalMd?.trim() ? values.goalMd.trim() : null,
    });
  });

  return (
    <form onSubmit={onSubmit} className="space-y-6">
      <PageHeader
        title="新建周计划"
        description="选择 ISO 年和周，系统会创建 7 天的空白卡片，之后可以逐日填写。"
        actions={
          <button
            type="submit"
            disabled={create.isPending}
            className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
          >
            {create.isPending ? "创建中…" : "创建"}
          </button>
        }
      />

      {create.error && <ErrorBanner message={create.error.message} />}

      <div className="bg-white border border-slate-200 rounded-lg p-4 space-y-3 max-w-xl">
        <div className="grid grid-cols-2 gap-3">
          <Field label="ISO 年" error={formState.errors.isoYear?.message}>
            <input
              type="number"
              min={2000}
              max={2100}
              className={inputCls}
              {...register("isoYear", { valueAsNumber: true })}
            />
          </Field>
          <Field label="ISO 周" error={formState.errors.isoWeek?.message}>
            <input
              type="number"
              min={1}
              max={53}
              className={inputCls}
              {...register("isoWeek", { valueAsNumber: true })}
            />
          </Field>
        </div>
        <Field label="标题" hint="可选" error={formState.errors.title?.message}>
          <input
            className={inputCls}
            placeholder="训练构建周 / 减量周 / 恢复周"
            {...register("title")}
          />
        </Field>
        <Field label="目标（Markdown）" hint="可选" error={formState.errors.goalMd?.message}>
          <textarea
            className={`${inputCls} min-h-[100px]`}
            placeholder="- 6 小时 Z2 耐力&#10;- 1 次甜点区间歇&#10;- 1 天休息"
            {...register("goalMd")}
          />
        </Field>
        <p className="text-xs text-slate-400">{previewLabel(watchedYear, watchedWeek)}</p>
      </div>
    </form>
  );
}

function previewLabel(year: number | undefined, week: number | undefined): string {
  if (!year || !week) return "选择年份和周数后会显示日期范围。";
  try {
    const dates = isoDatesOf(year, week);
    return `覆盖 ${dates[0]}（周一）到 ${dates[6]}（周日）。`;
  } catch {
    return `${year} 年第 ${week} 周在日历中不存在。`;
  }
}
