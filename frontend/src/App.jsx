import { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE = 'http://localhost:8080';
const ALLOWED_TYPES = ['image/png', 'image/jpeg'];
const MAX_SIZE_MB = 10;

// One axios instance that attaches the JWT to every request
const api = axios.create({ baseURL: API_BASE });
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

function AuthForm({ onLogin }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (mode === 'register') {
        await api.post('/auth/register', { username, password });
      }
      const res = await api.post('/auth/login', { username, password });
      localStorage.setItem('token', res.data.token);
      onLogin();
    } catch (err) {
      setError(err.response?.data?.error || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '360px', margin: '4rem auto', fontFamily: 'sans-serif', padding: '0 1rem' }}>
      <h1>Doc Intelligence Copilot</h1>
      <form onSubmit={submit} style={{ border: '1px solid #ddd', borderRadius: '8px', padding: '1.5rem' }}>
        <h3>{mode === 'login' ? 'Sign in' : 'Create account'}</h3>
        <input
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          style={{ width: '100%', padding: '0.5rem', marginBottom: '0.75rem', boxSizing: 'border-box' }}
        />
        <input
          type="password"
          placeholder="Password (min 8 characters)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          style={{ width: '100%', padding: '0.5rem', marginBottom: '0.75rem', boxSizing: 'border-box' }}
        />
        <button type="submit" disabled={loading || !username || !password} style={{ padding: '0.5rem 1rem', cursor: 'pointer' }}>
          {loading ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Register & sign in'}
        </button>
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <p style={{ fontSize: '0.9rem' }}>
          {mode === 'login' ? 'No account? ' : 'Already registered? '}
          <a href="#" onClick={(e) => { e.preventDefault(); setError(null); setMode(mode === 'login' ? 'register' : 'login'); }}>
            {mode === 'login' ? 'Register' : 'Sign in'}
          </a>
        </p>
      </form>
    </div>
  );
}

function Dashboard({ onLogout }) {
  const [cases, setCases] = useState([]);
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [inputKey, setInputKey] = useState(0);

  const fetchCases = async () => {
    try {
      const res = await api.get('/cases');
      setCases(res.data.reverse());
      setError(null);
    } catch (err) {
      if (err.response?.status === 401) { onLogout(); return; } // token missing or expired
      setError('Could not load cases. Is bpm-service running?');
    }
  };

  useEffect(() => {
    fetchCases();
  }, []);

  const handleFileChange = (e) => {
    const selected = e.target.files[0];
    setError(null);
    if (!selected) { setFile(null); return; }
    if (!ALLOWED_TYPES.includes(selected.type)) {
      setError('Only PNG or JPEG images are supported.');
      setFile(null);
      e.target.value = '';
      return;
    }
    if (selected.size > MAX_SIZE_MB * 1024 * 1024) {
      setError(`File is too large (max ${MAX_SIZE_MB} MB).`);
      setFile(null);
      e.target.value = '';
      return;
    }
    setFile(selected);
  };

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    setError(null);
    const formData = new FormData();
    formData.append('file', file);
    try {
      await api.post('/cases/process-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setFile(null);
      setInputKey((k) => k + 1);
      await fetchCases();
    } catch (err) {
      if (err.response?.status === 401) { onLogout(); return; }
      setError(err.response?.data?.error || 'Processing failed. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div style={{ maxWidth: '900px', margin: '2rem auto', fontFamily: 'sans-serif', padding: '0 1rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Doc Intelligence Copilot</h1>
        <button onClick={onLogout} style={{ padding: '0.4rem 0.9rem', cursor: 'pointer' }}>Sign out</button>
      </div>
      <p style={{ color: '#666' }}>Upload a document to classify, route, and track it.</p>

      <div style={{ border: '1px solid #ddd', borderRadius: '8px', padding: '1.5rem', marginBottom: '2rem' }}>
        <h3>Upload Document</h3>
        <input key={inputKey} type="file" accept="image/png,image/jpeg" onChange={handleFileChange} />
        <button
          onClick={handleUpload}
          disabled={!file || uploading}
          style={{ marginLeft: '1rem', padding: '0.5rem 1rem', cursor: 'pointer' }}
        >
          {uploading ? 'Processing...' : 'Upload & Process'}
        </button>
        {error && <p style={{ color: 'red' }}>{error}</p>}
      </div>

      <h3>Cases ({cases.length})</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {cases.map((c) => (
          <div
            key={c.id}
            style={{
              border: '1px solid #ddd',
              borderRadius: '8px',
              padding: '1rem',
              borderLeft: `4px solid ${c.finalStatus === 'auto_routed' ? '#22c55e' : '#f59e0b'}`
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <strong>{c.filename}</strong>
              <span style={{
                fontSize: '0.8rem',
                padding: '0.2rem 0.6rem',
                borderRadius: '12px',
                background: c.finalStatus === 'auto_routed' ? '#dcfce7' : '#fef3c7',
                color: c.finalStatus === 'auto_routed' ? '#166534' : '#92400e'
              }}>
                {c.finalStatus === 'auto_routed' ? 'Auto-Routed' : 'Needs Review'}
              </span>
            </div>
            <p style={{ margin: '0.5rem 0', color: '#444' }}>
              <strong>Category:</strong> {c.category} &nbsp;|&nbsp;
              <strong>Route:</strong> {c.route} &nbsp;|&nbsp;
              <strong>Confidence:</strong> {(c.confidence * 100).toFixed(0)}%
            </p>
            <p style={{ fontSize: '0.9rem', color: '#666', fontStyle: 'italic' }}>{c.justification}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function App() {
  const [loggedIn, setLoggedIn] = useState(!!localStorage.getItem('token'));

  const logout = () => {
    localStorage.removeItem('token');
    setLoggedIn(false);
  };

  return loggedIn ? <Dashboard onLogout={logout} /> : <AuthForm onLogin={() => setLoggedIn(true)} />;
}

export default App;