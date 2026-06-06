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
        title="Version history"
        description="Each PUT that changes structure writes a new row. Click a row to view a frozen snapshot."
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
        <Card>
          <table className="w-full text-sm">
            <thead className="text-xs uppercase text-slate-400">
              <tr>
                <th className="text-left py-2 pr-3">Version</th>
                <th className="text-left py-2 pr-3">Change note</th>
                <th className="text-left py-2 pr-3">Created at</th>
                <th className="text-left py-2 pr-3">Open</th>
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
                      View snapshot
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <p className="text-xs text-slate-400 mt-2">
        v1 is the original create. Subsequent versions are written on every PUT that changes structure_json.
      </p>
    </>
  );
}
