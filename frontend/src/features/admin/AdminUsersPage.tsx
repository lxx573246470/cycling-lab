import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthStore } from "@/features/auth/authStore";
import { ErrorBanner, PageHeader, Spinner } from "@/components/ui";
import { adminApi, type AdminUserDto, type UserRole, type UserStatus } from "./adminApi";

/**
 * Admin user management. The /api/v1/admin/** endpoints are gated on
 * ROLE_ADMIN, so we also gate the page client-side on the local user
 * role: non-admins see a "forbidden" hint instead of a confusing 403.
 */
export function AdminUsersPage() {
  const me = useAuthStore((s) => s.user);
  const qc = useQueryClient();
  const [q, setQ] = useState("");
  const [role, setRole] = useState<"" | UserRole>("");
  const [status, setStatus] = useState<"" | UserStatus>("");

  const list = useQuery({
    queryKey: ["admin", "users", { q, role, status }],
    queryFn: () =>
      adminApi.list({
        q: q || undefined,
        role: role || undefined,
        status: status || undefined,
        page: 0,
        size: 100,
      }),
  });

  const patch = useMutation({
    mutationFn: (args: { id: string; role: UserRole; status: UserStatus }) =>
      adminApi.patch(args.id, { role: args.role, status: args.status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "users"] }),
  });

  const remove = useMutation({
    mutationFn: (id: string) => adminApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin", "users"] }),
  });

  const isAdmin = me?.role === "ADMIN";

  if (!isAdmin) {
    return (
      <div className="p-6 border border-amber-200 bg-amber-50 text-amber-800 rounded">
        You are signed in as a regular user. The admin console is only
        available to users with the ADMIN role.
      </div>
    );
  }

  return (
    <>
      <PageHeader
        title="Admin / users"
        description="Search, filter, and manage user accounts. Promote a user to ADMIN to grant access to this page; set status to DISABLED to suspend login without losing data."
      />

      {patch.error && <ErrorBanner message={(patch.error as Error).message} />}
      {remove.error && <ErrorBanner message={(remove.error as Error).message} />}
      {list.error && <ErrorBanner message={(list.error as Error).message} />}
      {list.isLoading && <Spinner />}

      <div className="flex gap-2 mb-3">
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Email or display name"
          className="flex-1 px-3 py-1.5 text-sm border border-slate-300 rounded"
        />
        <select
          value={role}
          onChange={(e) => setRole(e.target.value as "" | UserRole)}
          className="px-3 py-1.5 text-sm border border-slate-300 rounded"
        >
          <option value="">All roles</option>
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as "" | UserStatus)}
          className="px-3 py-1.5 text-sm border border-slate-300 rounded"
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">ACTIVE</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </div>

      <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="text-left px-3 py-2">Email</th>
              <th className="text-left px-3 py-2">Display name</th>
              <th className="text-left px-3 py-2">Role</th>
              <th className="text-left px-3 py-2">Status</th>
              <th className="text-left px-3 py-2">Created</th>
              <th className="text-right px-3 py-2">Actions</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.content.map((u) => (
              <UserRow
                key={u.id}
                user={u}
                isMe={me?.id === u.id}
                onChangeRole={(r) => patch.mutate({ id: u.id, role: r, status: u.status })}
                onChangeStatus={(s) => patch.mutate({ id: u.id, role: u.role, status: s })}
                onDelete={() => {
                  if (confirm(`Delete ${u.email}? This wipes their data.`)) remove.mutate(u.id);
                }}
              />
            ))}
            {list.data && list.data.content.length === 0 && (
              <tr>
                <td colSpan={6} className="text-center text-slate-500 py-6">
                  No users match the current filter.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  );
}

function UserRow({
  user,
  isMe,
  onChangeRole,
  onChangeStatus,
  onDelete,
}: {
  user: AdminUserDto;
  isMe: boolean;
  onChangeRole: (r: UserRole) => void;
  onChangeStatus: (s: UserStatus) => void;
  onDelete: () => void;
}) {
  const createdAt = useMemo(
    () => new Date(user.createdAt).toLocaleDateString(),
    [user.createdAt],
  );
  return (
    <tr className="border-t border-slate-100">
      <td className="px-3 py-2 text-slate-700 font-mono text-xs">{user.email}</td>
      <td className="px-3 py-2 text-slate-900">{user.displayName}</td>
      <td className="px-3 py-2">
        <select
          value={user.role}
          onChange={(e) => onChangeRole(e.target.value as UserRole)}
          className="px-2 py-0.5 text-xs border border-slate-300 rounded"
        >
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </td>
      <td className="px-3 py-2">
        <select
          value={user.status}
          onChange={(e) => onChangeStatus(e.target.value as UserStatus)}
          className="px-2 py-0.5 text-xs border border-slate-300 rounded"
        >
          <option value="ACTIVE">ACTIVE</option>
          <option value="DISABLED">DISABLED</option>
        </select>
      </td>
      <td className="px-3 py-2 text-slate-500 text-xs">{createdAt}</td>
      <td className="px-3 py-2 text-right">
        <button
          type="button"
          onClick={onDelete}
          disabled={isMe}
          title={isMe ? "You cannot delete your own account" : undefined}
          className="text-xs px-2 py-1 rounded border border-slate-300 hover:bg-slate-50 text-slate-500 disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Delete
        </button>
      </td>
    </tr>
  );
}