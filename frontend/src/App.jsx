import { useCallback, useEffect, useState } from 'react';
import AuthScreen from './components/AuthScreen';
import Shell from './components/Shell';
import { api, clearToken, getToken } from './api';

const HEARTBEAT_MS = 30000;

export default function App() {
  const [session, setSession] = useState(null); // { username, role, mustChangePassword }
  const [checking, setChecking] = useState(!!getToken());
  const [notice, setNotice] = useState(null);

  // Ask the server who the token belongs to. The server, not the browser, decides role and status.
  const loadSession = useCallback(async () => {
    try {
      const res = await api.get('/auth/me');
      setSession(res.data);
      setNotice(null);
    } catch (err) {
      setSession(null);
      if (err.response) {
        clearToken(); // expired token, or the account was disabled
        setNotice('Your session has ended. Sign in again.');
      } else {
        setNotice("Can't reach the server. Check that bpm-service is running.");
      }
    } finally {
      setChecking(false);
    }
  }, []);

  useEffect(() => {
    if (getToken()) loadSession();
  }, [loadSession]);

  const signOut = useCallback((reason) => {
    clearToken();
    setSession(null);
    setNotice(reason || null);
  }, []);

  // While signed in, re-check with the server every 30 seconds and whenever the tab regains focus.
  // Clicking around a loaded page makes no API call, so without this a disabled account would keep
  // seeing its data until it happened to fetch something. Now it is signed out within about 30 seconds,
  // and a changed role or a forced password change shows up the same way.
  const signedIn = !!session;
  useEffect(() => {
    if (!signedIn) return undefined;

    const check = async () => {
      try {
        const res = await api.get('/auth/me');
        setSession((prev) =>
          prev &&
          prev.username === res.data.username &&
          prev.role === res.data.role &&
          prev.mustChangePassword === res.data.mustChangePassword
            ? prev
            : res.data,
        );
      } catch (err) {
        if (err.response?.status === 401) {
          signOut('Your session has ended or your account was disabled. Sign in again.');
        }
      }
    };

    const id = setInterval(check, HEARTBEAT_MS);
    window.addEventListener('focus', check);
    return () => {
      clearInterval(id);
      window.removeEventListener('focus', check);
    };
  }, [signedIn, signOut]);

  if (checking) {
    return (
      <p className="boot" role="status">
        Loading
      </p>
    );
  }

  if (!session) {
    return (
      <AuthScreen
        notice={notice}
        onSignedIn={() => {
          setChecking(true);
          loadSession();
        }}
      />
    );
  }

  return <Shell session={session} onSignOut={signOut} onSessionChange={loadSession} />;
}
