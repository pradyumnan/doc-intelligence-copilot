import { useState } from 'react';
import { api, errorMessage, setToken } from '../api';

export default function AuthScreen({ onSignedIn, notice }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const [busy, setBusy] = useState(false);
  const isRegister = mode === 'register';

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      if (isRegister) {
        await api.post('/auth/register', { username, password });
        // New accounts cannot sign in until an administrator approves them
        setInfo('Account created. An administrator must approve it before you can sign in.');
        setMode('login');
        setPassword('');
        return;
      }
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
    setInfo(null);
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
          <h2>{isRegister ? 'Request an account' : 'Sign in'}</h2>
          {notice && (
            <p className="note" role="status">
              {notice}
            </p>
          )}
          {info && (
            <p className="note" role="status">
              {info}
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
          {isRegister && (
            <p className="hint">
              At least 8 characters. An administrator approves new accounts before they can sign in.
            </p>
          )}

          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}

          <button className="btn-primary" type="submit" disabled={busy || !username || !password}>
            {busy ? 'Please wait' : isRegister ? 'Request account' : 'Sign in'}
          </button>

          <p className="switch">
            {isRegister ? 'Already approved? ' : 'Need an account? '}
            <button type="button" className="link" onClick={switchMode}>
              {isRegister ? 'Sign in' : 'Request one'}
            </button>
          </p>
        </form>
      </main>
    </div>
  );
}
