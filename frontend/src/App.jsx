import { useCallback, useState } from 'react';
import AuthScreen from './components/AuthScreen';
import Workspace from './components/Workspace';
import { clearToken, getToken } from './api';

export default function App() {
  const [signedIn, setSignedIn] = useState(!!getToken());
  const [notice, setNotice] = useState(null);

  const signOut = useCallback((reason) => {
    clearToken();
    setNotice(reason || null);
    setSignedIn(false);
  }, []);

  if (signedIn) return <Workspace onSignOut={signOut} />;

  return (
    <AuthScreen
      notice={notice}
      onSignedIn={() => {
        setNotice(null);
        setSignedIn(true);
      }}
    />
  );
}
