import { useQuery } from "@tanstack/react-query";
import { PageHeader, Spinner, ErrorBanner } from "@/components/ui";
import { workoutFileApi, formatBytes, type WorkoutFileSummary } from "./workoutFileApi";

export function WorkoutsListPage() {
  const q = useQuery({
    queryKey: ["workout-files", "page"],
    queryFn: () => workoutFileApi.list({ page: 0, size: 50 }),
  });

  return (
    <>
      <PageHeader
        title="Generated workouts"
        description="ZWO files you've created from the library or ad-hoc structures. Each file is a self-contained snapshot you can import into Zwift, TrainingPeaks, Rouvy, etc."
      />

      {q.error && <ErrorBanner message={(q.error as Error).message} />}
      {q.isLoading && <Spinner />}

      {q.data && q.data.content.length === 0 && (
        <div className="text-sm text-slate-500 p-6 border border-dashed border-slate-300 rounded text-center">
          No generated files yet. Go to the{" "}
          <a href="/library" className="text-brand-600 hover:underline">
            workout library
          </a>{" "}
          and click "Generate .zwo" on a template, or use the new-workout form.
        </div>
      )}

      <div className="space-y-2">
        {q.data?.content.map((f) => (
          <WorkoutFileRow key={f.id} file={f} />
        ))}
      </div>
    </>
  );
}

function WorkoutFileRow({ file }: { file: WorkoutFileSummary }) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 flex items-center justify-between">
      <div className="min-w-0 flex-1">
        <div className="text-sm font-semibold text-slate-900 truncate">
          {file.name}
        </div>
        <div className="text-xs text-slate-500 mt-0.5">
          {file.format} · {formatBytes(file.sizeBytes)} · {file.sportType}
        </div>
        <div className="text-xs text-slate-400 mt-0.5">
          created {new Date(file.createdAt).toLocaleString()}
          {file.sourceTemplateId && " · from library template"}
        </div>
        {file.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1.5">
            {file.tags.map((t) => (
              <span
                key={t}
                className="text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600"
              >
                {t}
              </span>
            ))}
          </div>
        )}
      </div>
      <button
        type="button"
        onClick={() => workoutFileApi.download(file.id, `${file.name}.zwo`)}
        className="text-sm px-3 py-1.5 rounded border border-slate-300 hover:bg-slate-50 ml-4"
      >
        Download .zwo
      </button>
    </div>
  );
}