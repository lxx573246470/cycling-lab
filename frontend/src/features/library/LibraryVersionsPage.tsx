import { Link, useParams } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { libraryApi } from "./libraryApi";
import { Card, ErrorBanner, PageHeader, Spinner } from "@/components/ui";

export function LibraryVersionsPage() {
  const { id } = useParams({ strict: false }) as { id: string };
  const query = useQuery({
    queryKey: ["library", "versions", id],
    queryFn: () => libraryApi.versions(id),
    enabled: !!id,
  });

  return (
    <>
      <PageHeader
        title="版本历史"
        description="每次保存结构变更都会生成一个新版本，可以打开查看当时的只读快照。"
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
        <Card>
          <table className="w-full text-sm">
            <thead className="text-xs uppercase text-slate-400">
              <tr>
                <th className="text-left py-2 pr-3">版本</th>
                <th className="text-left py-2 pr-3">变更说明</th>
                <th className="text-left py-2 pr-3">创建时间</th>
                <th className="text-left py-2 pr-3">打开</th>
              </tr>
            </thead>
            <tbody>
              {query.data.map((v) => (
                <tr key={v.version} className="border-t border-slate-100">
                  <td className="py-2 pr-3 font-mono">v{v.version}</td>
                  <td className="py-2 pr-3">{v.changeNote ?? <span className="text-slate-400">—</span>}</td>
                  <td className="py-2 pr-3 text-slate-500">{new Date(v.createdAt).toLocaleString()}</td>
                  <td className="py-2 pr-3">
                    <Link
                      to={"/library/$id/versions/$version" as any}
                      params={{ id, version: String(v.version) } as any}
                      className="text-brand-600 hover:underline text-xs"
                    >
                      查看快照
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <p className="text-xs text-slate-400 mt-2">
        v1 是初始创建版本；之后每次修改 structure_json 都会写入新版本。
      </p>
    </>
  );
}
