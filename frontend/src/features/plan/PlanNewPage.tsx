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
        title="New weekly plan"
        description="Pick the ISO year + week. Seven empty day cards will be created so you can fill them in afterwards."
        actions={
          <button
            type="submit"
            disabled={create.isPending}
            className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
          >
            {create.isPending ? "Creating…" : "Create"}
          </button>
        }
      />

      {create.error && <ErrorBanner message={create.error.message} />}

      <div className="bg-white border border-slate-200 rounded-lg p-4 space-y-3 max-w-xl">
        <div className="grid grid-cols-2 gap-3">
          <Field label="ISO year" error={formState.errors.isoYear?.message}>
            <input
              type="number"
              min={2000}
              max={2100}
              className={inputCls}
              {...register("isoYear", { valueAsNumber: true })}
            />
          </Field>
          <Field label="ISO week" error={formState.errors.isoWeek?.message}>
            <input
              type="number"
              min={1}
              max={53}
              className={inputCls}
              {...register("isoWeek", { valueAsNumber: true })}
            />
          </Field>
        </div>
        <Field label="Title" hint="optional" error={formState.errors.title?.message}>
          <input
            className={inputCls}
            placeholder="Build week · taper · recovery"
            {...register("title")}
          />
        </Field>
        <Field label="Goal (Markdown)" hint="optional" error={formState.errors.goalMd?.message}>
          <textarea
            className={`${inputCls} min-h-[100px]`}
            placeholder="- 6h Z2 endurance&#10;- 1x sweet-spot&#10;- 1x rest day"
            {...register("goalMd")}
          />
        </Field>
        <p className="text-xs text-slate-400">{previewLabel(watchedYear, watchedWeek)}</p>
      </div>
    </form>
  );
}

function previewLabel(year: number | undefined, week: number | undefined): string {
  if (!year || !week) return "Pick a year and week to see the date range.";
  try {
    const dates = isoDatesOf(year, week);
    return `Covers ${dates[0]} (Mon) to ${dates[6]} (Sun).`;
  } catch {
    return `Week ${week} of ${year} does not exist on the calendar.`;
  }
}