import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "@tanstack/react-router";
import { CATEGORIES, libraryApi, type WorkoutTemplateListItem } from "./libraryApi";
import { ErrorBanner, PageHeader, Spinner } from "@/components/ui";
import { formatDuration } from "./libraryApi";

export function LibraryListPage() {
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<string>("");
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const query = useQuery({
    queryKey: ["library", "list", { q: search, category }],
    queryFn: () =>
      libraryApi.list({
        q: search || undefined,
        category: category || undefined,
        archived: false,
        page: 0,
        size: 50,
      }),
  });

  const counts = useQuery({
    queryKey: ["library", "category-counts"],
    queryFn: () => libraryApi.categoryCounts(),
  });

  return (
    <>
      <PageHeader
        title="Workout library"
        description="Reusable workout templates. 6 categories, versioned, with structure you can re-use in weekly plans."
        actions={
          <Link
            to={"/library/new" as any}
            className="px-3 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium"
          >
            + New
          </Link>
        }
      />

      <div className="flex gap-2 mb-4">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by name…"
          className="flex-1 px-3 py-1.5 border border-slate-300 rounded text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
        />
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          className="px-3 py-1.5 border border-slate-300 rounded text-sm"
        >
          <option value="">All categories</option>
          {CATEGORIES.map((c) => (
            <option key={c.code} value={c.code}>
              {c.label}
            </option>
          ))}
        </select>
      </div>

      {query.isLoading && <Spinner />}
      {query.error && <ErrorBanner message={(query.error as Error).message} />}

      <div className="flex gap-6">
        <aside className="w-40 shrink-0 space-y-1">
          <button
            type="button"
            onClick={() => setCategory("")}
            className={`w-full text-left px-3 py-1.5 text-sm rounded ${
              !category ? "bg-brand-50 text-brand-700 font-medium" : "hover:bg-slate-100"
            }`}
          >
            All <span className="text-xs text-slate-400 ml-1">({totalCount(counts.data)})</span>
          </button>
          {CATEGORIES.map((c) => (
            <button
              key={c.code}
              type="button"
              onClick={() => setCategory(c.code)}
              className={`w-full text-left px-3 py-1.5 text-sm rounded ${
                category === c.code ? "bg-brand-50 text-brand-700 font-medium" : "hover:bg-slate-100"
              }`}
            >
              {c.label}{" "}
              <span className="text-xs text-slate-400 ml-1">
                ({counts.data?.[c.code] ?? 0})
              </span>
            </button>
          ))}
        </aside>

        <div className="flex-1 min-w-0">
          {query.data && query.data.content.length === 0 && (
            <div className="text-sm text-slate-500 p-6 border border-dashed border-slate-300 rounded text-center">
              No templates yet. <Link to={"/library/new" as any} className="text-brand-600 hover:underline">Create one</Link>.
            </div>
          )}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {query.data?.content.map((item) => (
              <TemplateCard
                key={item.id}
                item={item}
                onOpen={() => navigate({ to: "/library/$id" as any, params: { id: item.id } as any })}
                onDuplicate={async () => {
                  await libraryApi.duplicate(item.id);
                  queryClient.invalidateQueries({ queryKey: ["library"] });
                }}
                onArchive={async () => {
                  if (!confirm(`Archive "${item.name}"?`)) return;
                  await libraryApi.archive(item.id);
                  queryClient.invalidateQueries({ queryKey: ["library"] });
                }}
              />
            ))}
          </div>
        </div>
      </div>
    </>
  );
}

function totalCount(counts?: Record<string, number>): number {
  if (!counts) return 0;
  return Object.values(counts).reduce((a, b) => a + b, 0);
}

function TemplateCard({
  item,
  onOpen,
  onDuplicate,
  onArchive,
}: {
  item: WorkoutTemplateListItem;
  onOpen: () => void;
  onDuplicate: () => void;
  onArchive: () => void;
}) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 group">
      <div className="flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <button
            type="button"
            onClick={onOpen}
            className="text-left w-full"
          >
            <div className="text-base font-semibold text-slate-900 truncate">{item.name}</div>
            <div className="text-xs text-slate-400 mt-0.5">
              {item.category} • {formatDuration(item.totalDurationSec)} • {item.blockCount} blocks
            </div>
          </button>
        </div>
        {item.intensity && (
          <span className="ml-2 text-xs px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 shrink-0">
            {item.intensity}
          </span>
        )}
      </div>
      {item.tags.length > 0 && (
        <div className="flex flex-wrap gap-1 mt-2">
          {item.tags.map((t) => (
            <span key={t} className="text-xs px-1.5 py-0.5 rounded bg-brand-50 text-brand-700">
              {t}
            </span>
          ))}
        </div>
      )}
      <div className="flex gap-2 mt-3 opacity-0 group-hover:opacity-100 transition-opacity">
        <button type="button" onClick={onOpen} className="text-xs text-slate-500 hover:text-slate-700">
          Open
        </button>
        <button type="button" onClick={onDuplicate} className="text-xs text-slate-500 hover:text-slate-700">
          Duplicate
        </button>
        <button type="button" onClick={onArchive} className="text-xs text-red-500 hover:text-red-700 ml-auto">
          Archive
        </button>
      </div>
    </div>
  );
}
