import { useState } from 'react';
import { api, errorMessage, setToken } from '../api';

export default function AuthScreen({ onSignedIn, notice }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const isRegister = mode === 'register';

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (isRegister) await api.post('/auth/register', { username, password });
      const res = await api.post('/auth/login', { username, password });
      setToken(res.data.token);
      onSignedIn();
    } catch (err) {
      setError(errorMessage(err, 'Something went wrong. Try again.'));
    } finally {
      setBusy(false);
    }
  };

  const switchMode = () => {
    setError(null);
    setMode(isRegister ? 'login' : 'register');
  };

  return (
    <div className="auth">
      <aside className="auth-brand">
        <p className="brand-name">Doc Intelligence Copilot</p>
        <h1>Every incoming document, read and routed, with the policy rule behind the decision.</h1>
        <p className="auth-sub">
          Upload a scan. The system classifies it, checks it against your policies, and either
          routes it or holds it for a person to review.
        </p>
      </aside>

      <main className="auth-form-wrap">
        <form className="auth-form" onSubmit={submit}>
          <h2>{isRegister ? 'Create an account' : 'Sign in'}</h2>
          {notice && (
            <p className="note" role="status">
              {notice}
            </p>
          )}

          <label htmlFor="username">Username</label>
          <input
            id="username"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />

          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            minLength={isRegister ? 8 : undefined}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          {isRegister && <p className="hint">Use at least 8 characters.</p>}

          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}

          <button className="btn-primary" type="submit" disabled={busy || !username || !password}>
            {busy ? 'Please wait' : isRegister ? 'Create account' : 'Sign in'}
          </button>

          <p className="switch">
            {isRegister ? 'Already have an account? ' : 'New here? '}
            <button type="button" className="link" onClick={switchMode}>
              {isRegister ? 'Sign in' : 'Create an account'}
            </button>
          </p>
        </form>
      </main>
    </div>
  );
}
