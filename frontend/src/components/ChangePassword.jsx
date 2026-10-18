import { useState } from 'react';
import { api, errorMessage } from '../api';

export default function ChangePassword({ forced = false, onDone }) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [changed, setChanged] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    if (next.length < 8) {
      setError('The new password must be at least 8 characters.');
      return;
    }
    if (next !== confirm) {
      setError('The new passwords do not match.');
      return;
    }
    if (next === current) {
      setError('The new password must be different from the current one.');
      return;
    }

    setBusy(true);
    try {
      await api.post('/auth/change-password', { currentPassword: current, newPassword: next });
      if (forced)
        onDone(); // reload the session; the server now lets this user in
      else setChanged(true);
    } catch (err) {
      setError(errorMessage(err, 'Could not change the password. Try again.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="page page-narrow">
      <h2>{forced ? 'Choose a new password' : 'Change password'}</h2>

      {forced && (
        <p className="note">
          An administrator reset your password. Enter the temporary password you were given, then
          choose a new one to continue.
        </p>
      )}

      {changed ? (
        <>
          <p className="message ok" role="status">
            Your password was changed.
          </p>
          <button type="button" className="btn-quiet" onClick={onDone}>
            Back to documents
          </button>
        </>
      ) : (
        <form className="form" onSubmit={submit}>
          <label htmlFor="current">{forced ? 'Temporary password' : 'Current password'}</label>
          <input
            id="current"
            type="password"
            autoComplete="current-password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            required
          />

          <label htmlFor="next">New password</label>
          <input
            id="next"
            type="password"
            autoComplete="new-password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
            required
          />

          <label htmlFor="confirm">Confirm new password</label>
          <input
            id="confirm"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
          />

          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}

          <button
            className="btn-primary"
            type="submit"
            disabled={busy || !current || !next || !confirm}
          >
            {busy ? 'Please wait' : 'Change password'}
          </button>
        </form>
      )}
    </div>
  );
}
