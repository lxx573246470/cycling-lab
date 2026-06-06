import { useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ErrorBanner, PageHeader, Spinner } from "@/components/ui";
import { reviewApi, type ReviewDto } from "./reviewApi";

export function ReviewsListPage() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const q = useQuery({
    queryKey: ["reviews", "list"],
    queryFn: () => reviewApi.list({ page: 0, size: 50 }),
  });
  const remove = useMutation({
    mutationFn: (id: string) => reviewApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["reviews", "list"] }),
  });

  return (
    <>
      <PageHeader
        title="Reviews"
        description="Weekly and phase retrospectives. Each review is a Markdown document with optional structured metrics (TSS, distance, hours, etc.)."
      />

      {q.error && <ErrorBanner message={(q.error as Error).message} />}
      {q.isLoading && <Spinner />}

      {q.data && q.data.content.length === 0 && (
        <div className="text-sm text-slate-500 p-6 border border-dashed border-slate-300 rounded text-center">
          No reviews yet. Open a weekly plan and use "Write weekly review" to start one.
        </div>
      )}

      <div className="space-y-2">
        {q.data?.content.map((r) => (
          <ReviewRow
            key={r.id}
            review={r}
            onOpen={() =>
              navigate({ to: "/reviews/$id" as any, params: { id: r.id } as any })
            }
            onDelete={() => {
              if (confirm(`Delete "${r.title}"?`)) remove.mutate(r.id);
            }}
          />
        ))}
      </div>
    </>
  );
}

function ReviewRow({
  review,
  onOpen,
  onDelete,
}: {
  review: ReviewDto;
  onOpen: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 flex items-center justify-between">
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onOpen}
            className="text-sm font-semibold text-brand-700 hover:underline truncate text-left"
          >
            {review.title}
          </button>
          <span className="text-[10px] font-semibold uppercase tracking-wide px-1.5 py-0.5 rounded border bg-slate-50 text-slate-700 border-slate-200">
            {review.scope}
          </span>
          {review.isoYear != null && review.isoWeek != null && (
            <span className="text-xs text-slate-500">
              ISO {review.isoYear}-W{Math.min(review.isoWeek, 53).toString().padStart(2, "0")}
            </span>
          )}
        </div>
        <div className="text-xs text-slate-400 mt-0.5">
          updated {new Date(review.updatedAt).toLocaleString()}
        </div>
        <div className="text-xs text-slate-500 mt-1 line-clamp-2">
          {snippet(review.contentMd)}
        </div>
      </div>
      <button
        type="button"
        onClick={onDelete}
        className="text-sm px-3 py-1.5 rounded border border-slate-300 hover:bg-slate-50 ml-4 text-slate-500"
      >
        Delete
      </button>
    </div>
  );
}

function snippet(md: string): string {
  // Strip headings, links, and other markup for a clean preview line.
  return md
    .replace(/^#+\s*/gm, "")
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/[*_`]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 240);
}