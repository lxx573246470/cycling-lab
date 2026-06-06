import { useEffect, useMemo, useState } from "react";
import { useParams } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { ErrorBanner, PageHeader, Spinner } from "@/components/ui";
import { reviewApi, type ReviewDto } from "./reviewApi";

export function ReviewDetailPage() {
  const { id } = useParams({ from: "/reviews/$id" as any });
  const qc = useQueryClient();
  const q = useQuery({
    queryKey: ["reviews", "detail", id],
    queryFn: () => reviewApi.get(id),
    enabled: Boolean(id),
  });
  const save = useMutation({
    mutationFn: (body: { title: string; contentMd: string; isoYear: number; isoWeek: number }) =>
      reviewApi.update(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["reviews"] });
    },
  });
  const remove = useMutation({
    mutationFn: () => reviewApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["reviews", "list"] }),
  });

  if (q.isLoading) return <Spinner />;
  if (q.error) return <ErrorBanner message={(q.error as Error).message} />;
  if (!q.data) return null;
  const r = q.data;

  return (
    <ReviewEditor
      review={r}
      onSave={(body) => save.mutate(body)}
      onDelete={() => {
        if (confirm(`Delete "${r.title}"?`)) remove.mutate();
      }}
      saving={save.isPending}
    />
  );
}

/**
 * Editor with a live preview. Editing happens in a textarea; the right
 * pane is a GFM-aware render of the same content. We deliberately avoid a
 * full WYSIWYG editor — the Markdown round-trip keeps things stable and
 * easy to export to Obsidian later.
 */
export function ReviewEditor({
  review,
  onSave,
  onDelete,
  saving,
}: {
  review: ReviewDto;
  onSave: (body: { title: string; contentMd: string; isoYear: number; isoWeek: number }) => void;
  onDelete: () => void;
  saving: boolean;
}) {
  const [title, setTitle] = useState(review.title);
  const [contentMd, setContentMd] = useState(review.contentMd);
  const dirty = useMemo(
    () => title !== review.title || contentMd !== review.contentMd,
    [title, contentMd, review],
  );

  // Reset local state when the source review changes.
  useEffect(() => {
    setTitle(review.title);
    setContentMd(review.contentMd);
  }, [review.id, review.title, review.contentMd]);

  const onCmdS = (e: React.KeyboardEvent) => {
    if ((e.metaKey || e.ctrlKey) && e.key === "s") {
      e.preventDefault();
      if (dirty) submit();
    }
  };

  const submit = () => {
    onSave({
      title: title.trim() || "Untitled",
      contentMd,
      isoYear: review.isoYear ?? 0,
      isoWeek: review.isoWeek ?? 0,
    });
  };

  return (
    <div onKeyDown={onCmdS}>
      <PageHeader
        title={title || "Untitled review"}
        description={
          review.isoYear != null && review.isoWeek != null
            ? `ISO ${review.isoYear}-W${Math.min(review.isoWeek, 53)
                .toString()
                .padStart(2, "0")} \u00b7 ${review.scope} \u00b7 last updated ${new Date(
                review.updatedAt,
              ).toLocaleString()}`
            : `${review.scope} review`
        }
        actions={
          <div className="flex gap-2">
            <button
              type="button"
              onClick={onDelete}
              className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50 text-slate-500"
            >
              Delete
            </button>
            <button
              type="button"
              onClick={submit}
              disabled={!dirty || saving}
              className="px-3 py-1.5 text-sm rounded bg-brand-500 hover:bg-brand-600 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {saving ? "Saving..." : "Save"}
            </button>
          </div>
        }
      />

      <div className="mb-3">
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Review title"
          className="w-full px-3 py-2 text-sm border border-slate-300 rounded"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
        <div>
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
            Markdown
          </div>
          <textarea
            value={contentMd}
            onChange={(e) => setContentMd(e.target.value)}
            placeholder="# Highlights&#10;&#10;- Z2 base&#10;- Sweet spot 2x12"
            className="w-full h-[calc(100vh-280px)] min-h-[420px] p-3 font-mono text-sm border border-slate-300 rounded resize-y"
          />
          <p className="text-[10px] text-slate-400 mt-1">
            Tip: Ctrl/⌘+S to save. Supports GFM tables, task lists, and code blocks.
          </p>
        </div>
        <div>
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
            Preview
          </div>
          <div className="prose prose-slate max-w-none bg-white border border-slate-200 rounded p-4 h-[calc(100vh-280px)] min-h-[420px] overflow-auto">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{contentMd || "*Start typing...*"}</ReactMarkdown>
          </div>
        </div>
      </div>
    </div>
  );
}