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
        title={`v${version} 快照`}
        description="历史版本只读查看；编辑始终在最新版本上进行。"
        actions={
          <Link
            to={"/library/$id" as any}
            params={{ id } as any}
            className="px-3 py-1.5 text-sm rounded border border-slate-300 hover:bg-slate-50"
          >
            ← 返回模板
          </Link>
        }
      />
      {query.isLoading && <Spinner />}
      {query.error && <ErrorBanner message={(query.error as Error).message} />}
      {query.data && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Card title="训练区块（只读）">
            <BlockEditor
              structure={parseStructure(query.data.structureJson) ?? { blocks: [] }}
              readOnly
              onChange={() => undefined}
            />
          </Card>
          <Card title="功率曲线">
            <PowerCurvePreview structure={parseStructure(query.data.structureJson) ?? { blocks: [] }} />
            {query.data.changeNote && (
              <p className="text-sm text-slate-500 mt-2">变更说明：{query.data.changeNote}</p>
            )}
            <p className="text-xs text-slate-400 mt-1">
              保存于 {new Date(query.data.createdAt).toLocaleString()}
            </p>
          </Card>
        </div>
      )}
    </>
  );
}
