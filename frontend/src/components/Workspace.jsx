import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { api, errorMessage } from '../api';
import ConfidenceMeter from './ConfidenceMeter';

const ALLOWED_TYPES = ['image/png', 'image/jpeg'];
const MAX_MB = 10;
const SESSION_EXPIRED = 'Your session expired. Sign in again.';

const CATEGORY_LABELS = {
  invoice: 'Invoice',
  loan_application: 'Loan application',
  kyc: 'KYC document',
  contract: 'Contract',
  other: 'Other document',
};
const FILTERS = [
  { id: 'all', name: 'All' },
  { id: 'review', name: 'Needs review' },
  { id: 'routed', name: 'Auto-routed' },
];

const categoryLabel = (c) => CATEGORY_LABELS[c] || c;
const isRouted = (c) => c.finalStatus === 'auto_routed';
const percent = (v) => (typeof v === 'number' ? `${Math.round(v * 100)}%` : 'n/a');

function formatTime(iso, withYear = false) {
  if (!iso) return '';
  return new Date(iso).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    ...(withYear ? { year: 'numeric' } : {}),
    hour: 'numeric',
    minute: '2-digit',
  });
}

function UploadZone({ onFile, busy }) {
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef(null);

  const pick = (file) => {
    if (file) onFile(file);
    if (inputRef.current) inputRef.current.value = '';
  };

  return (
    <div
      className={`drop ${dragging ? 'is-dragging' : ''} ${busy ? 'is-busy' : ''}`}
      onDragOver={(e) => {
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragging(false);
        if (!busy) pick(e.dataTransfer.files[0]);
      }}
    >
      <input
        ref={inputRef}
        id="file-input"
        className="visually-hidden"
        type="file"
        accept="image/png,image/jpeg"
        disabled={busy}
        onChange={(e) => pick(e.target.files[0])}
      />
      <label htmlFor="file-input" className="drop-label">
        {busy ? (
          <span>
            <span className="spin" aria-hidden="true" />
            Reading and routing your document
          </span>
        ) : (
          <>
            <strong>Add a document</strong>
            <span>Drop a PNG or JPEG here, or choose a file. Up to {MAX_MB} MB.</span>
          </>
        )}
      </label>
    </div>
  );
}

function Detail({ item, onBack }) {
  const routed = isRouted(item);
  const rule = /rules?\s*(\d+)/i.exec(item.justification || '');

  return (
    <article className="detail-body">
      <button type="button" className="link back" onClick={onBack}>
        Back to documents
      </button>

      <header>
        <h2>{item.filename}</h2>
        <p className="muted">
          Case {item.id}, received {formatTime(item.createdAt, true)}
        </p>
      </header>

      <section className={`verdict ${routed ? 'is-routed' : 'is-review'}`}>
        <p className="verdict-title">
          {routed ? `Sent to ${item.route} automatically` : 'Held for your review'}
        </p>
        <p>
          {routed
            ? 'Confidence cleared the review cutoff, so nobody needs to look at it.'
            : `Confidence is below the review cutoff. Suggested queue: ${item.route}.`}
        </p>
      </section>

      <ConfidenceMeter key={item.id} value={item.confidence} />

      <dl className="facts">
        <div>
          <dt>Document type</dt>
          <dd>{categoryLabel(item.category)}</dd>
        </div>
        <div>
          <dt>{routed ? 'Routed to' : 'Suggested queue'}</dt>
          <dd>{item.route}</dd>
        </div>
      </dl>

      <section>
        <h3>Why the system chose this</h3>
        <blockquote className="reason">{item.justification}</blockquote>
        {rule && <p className="muted">Cites policy rule {rule[1]}.</p>}
      </section>
    </article>
  );
}

export default function Workspace({ onSignOut }) {
  const [cases, setCases] = useState(null); // null while the first load is running
  const [filter, setFilter] = useState('all');
  const [selectedId, setSelectedId] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState(null); // { kind: 'ok' | 'error', text }

  const loadCases = useCallback(async () => {
    try {
      const res = await api.get('/cases');
      setCases([...res.data].sort((a, b) => b.id - a.id));
    } catch (err) {
      if (err.response?.status === 401) {
        onSignOut(SESSION_EXPIRED);
        return;
      }
      setCases((prev) => prev ?? []);
      setMessage({ kind: 'error', text: errorMessage(err, 'Could not load documents.') });
    }
  }, [onSignOut]);

  useEffect(() => {
    loadCases();
  }, [loadCases]);

  // Success messages fade out on their own; errors stay until the next action
  useEffect(() => {
    if (message?.kind !== 'ok') return undefined;
    const id = setTimeout(() => setMessage(null), 5000);
    return () => clearTimeout(id);
  }, [message]);

  const handleFile = async (file) => {
    setMessage(null);
    if (!ALLOWED_TYPES.includes(file.type)) {
      setMessage({ kind: 'error', text: 'Only PNG or JPEG images are supported.' });
      return;
    }
    if (file.size > MAX_MB * 1024 * 1024) {
      const size = (file.size / (1024 * 1024)).toFixed(1);
      setMessage({ kind: 'error', text: `That file is ${size} MB. The limit is ${MAX_MB} MB.` });
      return;
    }

    const form = new FormData();
    form.append('file', file);
    setUploading(true);
    try {
      const res = await api.post('/cases/process-image', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      await loadCases();
      setFilter('all');
      setSelectedId(res.data.id);
      setMessage({
        kind: 'ok',
        text: isRouted(res.data) ? `Routed to ${res.data.route}.` : 'Held for your review.',
      });
    } catch (err) {
      if (err.response?.status === 401) {
        onSignOut(SESSION_EXPIRED);
        return;
      }
      setMessage({ kind: 'error', text: errorMessage(err, 'Processing failed. Try again.') });
    } finally {
      setUploading(false);
    }
  };

  const counts = useMemo(() => {
    const list = cases || [];
    const routed = list.filter(isRouted).length;
    return { all: list.length, routed, review: list.length - routed };
  }, [cases]);

  const visible = useMemo(
    () =>
      (cases || []).filter(
        (c) => filter === 'all' || (filter === 'routed' ? isRouted(c) : !isRouted(c)),
      ),
    [cases, filter],
  );

  const selected = (cases || []).find((c) => c.id === selectedId) || null;

  let summary = 'Loading documents';
  if (cases) {
    if (counts.all === 0) summary = 'No documents yet.';
    else {
      const noun = counts.all === 1 ? 'document' : 'documents';
      const review =
        counts.review === 0
          ? 'None need review.'
          : counts.review === 1
            ? '1 needs your review.'
            : `${counts.review} need your review.`;
      summary = `${counts.all} ${noun} processed. ${review}`;
    }
  }

  let emptyState = null;
  if (cases && visible.length === 0) {
    if (counts.all === 0) {
      emptyState = {
        title: 'Nothing to review yet',
        body: 'Add a PNG or JPEG scan above to see how the system classifies and routes it.',
      };
    } else if (filter === 'review') {
      emptyState = { title: 'Nothing waiting on you', body: 'Every document so far was routed automatically.' };
    } else {
      emptyState = { title: 'No auto-routed documents', body: 'Every document so far needs a person to look at it.' };
    }
  }

  return (
    <div className="app">
      <header className="topbar">
        <span className="brand-name">Doc Intelligence Copilot</span>
        <button type="button" className="btn-quiet" onClick={() => onSignOut()}>
          Sign out
        </button>
      </header>

      <div className={`workspace ${selected ? 'has-selection' : ''}`}>
        <section className="queue" aria-label="Documents">
          <div className="queue-head">
            <p className="summary">{summary}</p>

            <div className="filters" role="group" aria-label="Filter documents">
              {FILTERS.map((f) => (
                <button
                  key={f.id}
                  type="button"
                  aria-pressed={filter === f.id}
                  onClick={() => setFilter(f.id)}
                >
                  {f.name}
                  <span className="count">{counts[f.id]}</span>
                </button>
              ))}
            </div>

            <UploadZone onFile={handleFile} busy={uploading} />

            <div aria-live="polite">
              {message && <p className={`message ${message.kind}`}>{message.text}</p>}
            </div>
          </div>

          {emptyState && (
            <div className="empty">
              <strong>{emptyState.title}</strong>
              {emptyState.body}
            </div>
          )}

          <ul className="case-list">
            {visible.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  className={`case-row ${isRouted(c) ? 'is-routed' : 'is-review'} ${
                    c.id === selectedId ? 'is-selected' : ''
                  }`}
                  aria-current={c.id === selectedId ? 'true' : undefined}
                  onClick={() => setSelectedId(c.id)}
                >
                  <span className="case-top">
                    <span className="case-name">{c.filename}</span>
                    <span className="case-conf">{percent(c.confidence)}</span>
                  </span>
                  <span className="case-mid">
                    {categoryLabel(c.category)}, {isRouted(c) ? `sent to ${c.route}` : `suggested: ${c.route}`}
                  </span>
                  <span className="case-bottom">
                    <span className="case-status">{isRouted(c) ? 'Auto-routed' : 'Needs review'}</span>
                    <span className="case-time">{formatTime(c.createdAt)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="detail" aria-label="Document details">
          {selected ? (
            <Detail item={selected} onBack={() => setSelectedId(null)} />
          ) : (
            <p className="detail-empty">Select a document to see how it was routed and why.</p>
          )}
        </section>
      </div>
    </div>
  );
}
