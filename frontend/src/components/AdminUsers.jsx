import { useCallback, useEffect, useState } from 'react';
import { api, errorMessage } from '../api';

const ROLES = ['ADMIN', 'REVIEWER', 'VIEWER'];
const ROLE_LABELS = { ADMIN: 'Administrator', REVIEWER: 'Reviewer', VIEWER: 'Viewer' };
const STATUS_LABELS = { PENDING: 'Awaiting approval', ACTIVE: 'Active', DISABLED: 'Disabled' };
const STATUS_ORDER = { PENDING: 0, ACTIVE: 1, DISABLED: 2 };
const SESSION_EXPIRED = 'Your session has ended. Sign in again.';

export default function AdminUsers({ currentUsername, onSignOut }) {
  const [users, setUsers] = useState(null); // null while loading
  const [pickedRole, setPickedRole] = useState({}); // role chosen for a pending user before approving
  const [busyId, setBusyId] = useState(null);
  const [message, setMessage] = useState(null); // { kind: 'ok' | 'error', text }
  const [temp, setTemp] = useState(null); // { username, temporaryPassword }

  const handleError = useCallback(
    (err, fallback) => {
      if (err.response?.status === 401) {
        onSignOut(SESSION_EXPIRED);
        return;
      }
      setMessage({ kind: 'error', text: errorMessage(err, fallback) });
    },
    [onSignOut],
  );

  const load = useCallback(async () => {
    try {
      const res = await api.get('/admin/users');
      setUsers(
        [...res.data].sort(
          (a, b) => STATUS_ORDER[a.status] - STATUS_ORDER[b.status] || a.id - b.id,
        ),
      );
    } catch (err) {
      setUsers((prev) => prev ?? []);
      handleError(err, 'Could not load users.');
    }
  }, [handleError]);

  useEffect(() => {
    load();
  }, [load]);

  const act = async (user, path, body, success) => {
    setBusyId(user.id);
    setMessage(null);
    setTemp(null);
    try {
      const res = await api.post(`/admin/users/${user.id}/${path}`, body);
      if (path === 'reset-password') setTemp(res.data);
      else if (success) setMessage({ kind: 'ok', text: success });
      await load();
    } catch (err) {
      handleError(err, 'That action failed. Try again.');
    } finally {
      setBusyId(null);
    }
  };

  const resetPassword = (user) => {
    const ok = window.confirm(
      `Reset the password for ${user.username}? They will have to choose a new one at their next sign-in.`,
    );
    if (ok) act(user, 'reset-password');
  };

  const copyTemp = async () => {
    try {
      await navigator.clipboard.writeText(temp.temporaryPassword);
      setMessage({ kind: 'ok', text: 'Copied to the clipboard.' });
    } catch {
      setMessage({
        kind: 'error',
        text: 'Could not copy. Select the password and copy it by hand.',
      });
    }
  };

  const pending = (users || []).filter((u) => u.status === 'PENDING').length;
  let summary = 'Loading users';
  if (users) {
    summary =
      pending === 0
        ? 'No accounts are waiting for approval.'
        : pending === 1
          ? '1 account is waiting for approval.'
          : `${pending} accounts are waiting for approval.`;
  }

  return (
    <div className="page">
      <h2>Users</h2>
      <p className="muted">{summary}</p>

      <div aria-live="polite">
        {message && <p className={`message ${message.kind}`}>{message.text}</p>}
      </div>

      {temp && (
        <div className="temp" role="status">
          <p>
            Temporary password for <strong>{temp.username}</strong>
          </p>
          <p className="temp-value">{temp.temporaryPassword}</p>
          <p className="muted">
            Shown once. Share it privately. They must choose a new password at their next sign-in.
          </p>
          <div className="actions">
            <button type="button" className="btn-small" onClick={copyTemp}>
              Copy
            </button>
            <button type="button" className="btn-small" onClick={() => setTemp(null)}>
              Dismiss
            </button>
          </div>
        </div>
      )}

      {users && users.length > 0 && (
        <div className="table-wrap">
          <table className="users">
            <thead>
              <tr>
                <th scope="col">User</th>
                <th scope="col">Status</th>
                <th scope="col">Role</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => {
                const isSelf = u.username === currentUsername;
                const busy = busyId === u.id;
                const isPending = u.status === 'PENDING';
                const roleValue = isPending ? pickedRole[u.id] || 'REVIEWER' : u.role;

                return (
                  <tr key={u.id}>
                    <th scope="row">
                      {u.username}
                      {isSelf && <span className="you"> (you)</span>}
                    </th>
                    <td>
                      <span className={`status status-${u.status.toLowerCase()}`}>
                        {STATUS_LABELS[u.status]}
                      </span>
                    </td>
                    <td>
                      <select
                        aria-label={`Role for ${u.username}`}
                        value={roleValue}
                        disabled={busy || isSelf}
                        onChange={(e) => {
                          const role = e.target.value;
                          if (isPending) setPickedRole((prev) => ({ ...prev, [u.id]: role }));
                          else
                            act(
                              u,
                              'role',
                              { role },
                              `${u.username} is now ${ROLE_LABELS[role].toLowerCase()}.`,
                            );
                        }}
                      >
                        {ROLES.map((r) => (
                          <option key={r} value={r}>
                            {ROLE_LABELS[r]}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <div className="actions">
                        {isPending && (
                          <button
                            type="button"
                            className="btn-small primary"
                            disabled={busy}
                            onClick={() =>
                              act(u, 'approve', { role: roleValue }, `${u.username} approved.`)
                            }
                          >
                            Approve
                          </button>
                        )}
                        {u.status === 'ACTIVE' && !isSelf && (
                          <button
                            type="button"
                            className="btn-small"
                            disabled={busy}
                            onClick={() => act(u, 'disable', undefined, `${u.username} disabled.`)}
                          >
                            Disable
                          </button>
                        )}
                        {u.status === 'DISABLED' && (
                          <button
                            type="button"
                            className="btn-small"
                            disabled={busy}
                            onClick={() => act(u, 'enable', undefined, `${u.username} enabled.`)}
                          >
                            Enable
                          </button>
                        )}
                        {!isPending && (
                          <button
                            type="button"
                            className="btn-small"
                            disabled={busy}
                            onClick={() => resetPassword(u)}
                          >
                            Reset password
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
