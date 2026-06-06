import { Link, useParams } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { libraryApi, parseStructure } from "./libraryApi";
import { BlockEditor } from "./BlockEditor";
import { PowerCurvePreview } from "./PowerCurvePreview";
import { Card, ErrorBanner, PageHeader, Spinner } from "@/components/ui";

export function LibraryVersionPage() {
  const { id, version } = useParams({ strict: false }) as { id: string; version: string };
  const query = useQuery({
    queryKey: ["library", "version", id, version],
    queryFn: () => libraryApi.version(id, Number(version)),
    enabled: !!id && !!version,
  });

  return (
    <>
      <PageHeader
        title={`v${version} snapshot`}
        description="Read-only view of a historical version. Edits always go through the latest version."
        actions={
          <Link
            to={"/library/$id" as any}
            params={{ id } as any}
            className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
          >
            ← Back to template
          </Link>
        }
      />
      {query.isLoading && <Spinner />}
      {query.error && <ErrorBanner message={(query.error as Error).message} />}
      {query.data && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Card title="Blocks (read-only)">
            <BlockEditor
              structure={parseStructure(query.data.structureJson) ?? { blocks: [] }}
              readOnly
              onChange={() => undefined}
            />
          </Card>
          <Card title="Power curve">
            <PowerCurvePreview structure={parseStructure(query.data.structureJson) ?? { blocks: [] }} />
            {query.data.changeNote && (
              <p className="text-sm text-slate-500 mt-2">Change note: {query.data.changeNote}</p>
            )}
            <p className="text-xs text-slate-400 mt-1">
              Saved {new Date(query.data.createdAt).toLocaleString()}
            </p>
          </Card>
        </div>
      )}
    </>
  );
}
