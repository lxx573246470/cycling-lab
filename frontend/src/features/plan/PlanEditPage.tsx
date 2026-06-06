import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorBanner, Field, PageHeader, Spinner } from "@/components/ui";
import { libraryApi, type WorkoutTemplateListItem } from "@/features/library/libraryApi";
import {
  DAILY_STATUSES,
  formatWeekRange,
  planApi,
  weekdayLabel,
  type DailyPlan,
  type DailyStatus,
  type WeeklyPlanDto,
} from "./planApi";

const inputCls =
  "w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent";
const statusColors: Record<DailyStatus, string> = {
  PLANNED: "bg-slate-100 text-slate-700",
  DONE: "bg-emerald-100 text-emerald-800",
  PARTIAL: "bg-amber-100 text-amber-800",
  SKIPPED: "bg-rose-100 text-rose-700",
  RESCHEDULED: "bg-violet-100 text-violet-800",
};

export function PlanEditPage() {
  const { id } = useParams({ from: "/plans/$id" as any }) as { id: string };
  const navigate = useNavigate();
  const qc = useQueryClient();

  const plan = useQuery({
    queryKey: ["plans", "week", id],
    queryFn: () => planApi.get(id),
  });

  // templates for the picker (only need ids + names)
  const templates = useQuery({
    queryKey: ["library", "templates", "all-for-picker"],
    queryFn: async () => {
      // fetch up to 100 active templates; the picker doesn't paginate
      const r = await libraryApi.list({ page: 0, size: 100, archived: false });
      return r.content;
    },
  });

  const updatePlan = useMutation({
    mutationFn: (body: { title?: string | null; goalMd?: string | null }) => planApi.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["plans", "week", id] }),
  });

  const updateDay = useMutation({
    mutationFn: ({ dayId, body }: { dayId: string; body: Parameters<typeof planApi.updateDay>[2] }) =>
      planApi.updateDay(id, dayId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["plans", "week", id] }),
  });

  const remove = useMutation({
    mutationFn: () => planApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["plans"] });
      navigate({ to: "/plans" as any });
    },
  });

  if (plan.isLoading) return <Spinner />;
  if (plan.error) return <ErrorBanner message={(plan.error as Error).message} />;
  if (!plan.data) return null;
  const p = plan.data;

  return (
    <div className="space-y-6">
      <PageHeader
        title={`${p.isoYear} · W${String(p.isoWeek).padStart(2, "0")}`}
        description={formatWeekRange(p.weekStart, p.weekEnd)}
        actions={
          <div className="flex gap-2">
            <Link
              to={"/plans" as any}
              className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
            >
              Back
            </Link>
            <button
              type="button"
              onClick={() => {
                if (confirm("Delete this weekly plan? This also removes the 7 day cards.")) {
                  remove.mutate();
                }
              }}
              className="px-3 py-1.5 text-sm rounded border border-rose-200 text-rose-700 hover:bg-rose-50"
            >
              Delete
            </button>
          </div>
        }
      />

      <PlanHeader
        plan={p}
        onSave={(title, goalMd) => updatePlan.mutate({ title, goalMd })}
        saving={updatePlan.isPending}
      />

      <ProgressBar progress={p.progress} />

      <div className="space-y-3">
        {p.days.map((d) => (
          <DayCard
            key={d.id}
            day={d}
            templates={templates.data ?? []}
            onSave={(body) => updateDay.mutate({ dayId: d.id, body })}
            saving={updateDay.isPending && updateDay.variables?.dayId === d.id}
          />
        ))}
      </div>
    </div>
  );
}

function PlanHeader({
  plan,
  onSave,
  saving,
}: {
  plan: WeeklyPlanDto;
  onSave: (title: string | null, goalMd: string | null) => void;
  saving: boolean;
}) {
  const [title, setTitle] = useState(plan.title ?? "");
  const [goalMd, setGoalMd] = useState(plan.goalMd ?? "");

  // keep local state in sync if the plan changes (e.g. after a save)
  useEffect(() => {
    setTitle(plan.title ?? "");
    setGoalMd(plan.goalMd ?? "");
  }, [plan.id, plan.title, plan.goalMd]);

  const dirty = (title.trim() || null) !== (plan.title ?? null) ||
    (goalMd.trim() || null) !== (plan.goalMd ?? null);

  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 space-y-3">
      <Field label="Title" hint="optional">
        <input
          className={inputCls}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g. Build week"
        />
      </Field>
      <Field label="Goal (Markdown)" hint="optional">
        <textarea
          className={`${inputCls} min-h-[80px]`}
          value={goalMd}
          onChange={(e) => setGoalMd(e.target.value)}
          placeholder="Weekly goal in markdown"
        />
      </Field>
      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => onSave(title.trim() || null, goalMd.trim() || null)}
          disabled={!dirty || saving}
          className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
        >
          {saving ? "Saving…" : "Save header"}
        </button>
      </div>
    </div>
  );
}

function ProgressBar({ progress }: { progress: WeeklyPlanDto["progress"] }) {
  if (progress.total === 0) return null;
  const seg = (n: number, color: string) =>
    n > 0 ? (
      <div
        key={color}
        className={`${color} h-2`}
        style={{ width: `${(n / progress.total) * 100}%` }}
        title={`${n}`}
      />
    ) : null;
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-3 flex items-center gap-3">
      <div className="flex-1 h-2 bg-slate-100 rounded overflow-hidden flex">
        {seg(progress.done, "bg-emerald-500")}
        {seg(progress.partial, "bg-amber-500")}
        {seg(progress.planned, "bg-slate-300")}
        {seg(progress.skipped, "bg-rose-500")}
        {seg(progress.rescheduled, "bg-violet-500")}
      </div>
      <span className="text-xs text-slate-500 whitespace-nowrap">
        {progress.done}/{progress.total} done
      </span>
    </div>
  );
}

function DayCard({
  day,
  templates,
  onSave,
  saving,
}: {
  day: DailyPlan;
  templates: WorkoutTemplateListItem[];
  onSave: (body: Parameters<typeof planApi.updateDay>[2]) => void;
  saving: boolean;
}) {
  const [targetText, setTargetText] = useState(day.targetText ?? "");
  const [notesMd, setNotesMd] = useState(day.notesMd ?? "");
  const [status, setStatus] = useState<DailyStatus>(day.status);
  const [templateId, setTemplateId] = useState<string>(day.templateId ?? "");

  // re-sync when the day changes
  useEffect(() => {
    setTargetText(day.targetText ?? "");
    setNotesMd(day.notesMd ?? "");
    setStatus(day.status);
    setTemplateId(day.templateId ?? "");
  }, [day.id, day.targetText, day.notesMd, day.status, day.templateId]);

  const dirty =
    (targetText.trim() || null) !== (day.targetText ?? null) ||
    (notesMd.trim() || null) !== (day.notesMd ?? null) ||
    status !== day.status ||
    (templateId || null) !== (day.templateId ?? null);

  const templateLabel = useMemo(() => {
    if (!day.templateId) return null;
    const t = templates.find((x) => x.id === day.templateId);
    return t?.name ?? day.templateName ?? "(template)";
  }, [day.templateId, day.templateName, templates]);

  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4">
      <div className="flex items-center justify-between mb-3">
        <div>
          <div className="text-sm font-semibold text-slate-700">
            {weekdayLabel(day.weekday)} · {day.date}
          </div>
          {templateLabel && (
            <div className="text-xs text-slate-400 mt-0.5">
              Plan: {templateLabel}
              {day.templateVersion ? ` (v${day.templateVersion})` : ""}
            </div>
          )}
        </div>
        <span className={`text-xs px-2 py-0.5 rounded ${statusColors[status]}`}>
          {status}
        </span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
        <Field label="Target" hint="optional">
          <input
            className={inputCls}
            value={targetText}
            onChange={(e) => setTargetText(e.target.value)}
            placeholder="e.g. 60min Z2 endurance"
          />
        </Field>
        <Field label="Workout template" hint="optional">
          <select
            className={inputCls}
            value={templateId}
            onChange={(e) => setTemplateId(e.target.value)}
          >
            <option value="">— none —</option>
            {templates.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name} · {t.category}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Status">
          <select
            className={inputCls}
            value={status}
            onChange={(e) => setStatus(e.target.value as DailyStatus)}
          >
            {DAILY_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Notes" hint="optional">
          <textarea
            className={`${inputCls} min-h-[80px]`}
            value={notesMd}
            onChange={(e) => setNotesMd(e.target.value)}
            placeholder="Session notes (markdown)"
          />
        </Field>
      </div>

      <div className="flex justify-end mt-3">
        <button
          type="button"
          onClick={() => {
            const body: Parameters<typeof planApi.updateDay>[2] = {
              targetText: targetText.trim() || null,
              notesMd: notesMd.trim() || null,
              status,
            };
            // The DTO distinguishes "leave alone" from "set to null" via the
            // templateIdPresent sentinel. Here we always re-send the chosen
            // id (or clear via the flag) so the server sees an explicit value.
            if ((templateId || null) !== (day.templateId ?? null)) {
              if (templateId) {
                body.templateId = templateId;
              } else {
                body.templateIdPresent = true;
              }
            }
            onSave(body);
          }}
          disabled={!dirty || saving}
          className="px-4 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50"
        >
          {saving ? "Saving…" : "Save day"}
        </button>
      </div>
    </div>
  );
}