import { useState } from 'react';
import Workspace from './Workspace';
import AdminUsers from './AdminUsers';
import ChangePassword from './ChangePassword';

const ROLE_LABELS = { ADMIN: 'Administrator', REVIEWER: 'Reviewer', VIEWER: 'Viewer' };

export default function Shell({ session, onSignOut, onSessionChange }) {
  const [view, setView] = useState('documents');
  const forced = session.mustChangePassword; // an admin reset this password: nothing else is allowed yet
  const isAdmin = session.role === 'ADMIN';
  const canUpload = session.role === 'ADMIN' || session.role === 'REVIEWER';

  const navItems = [
    { id: 'documents', label: 'Documents' },
    ...(isAdmin ? [{ id: 'users', label: 'Users' }] : []),
    { id: 'password', label: 'Password' },
  ];

  let content;
  if (forced) {
    content = <ChangePassword forced onDone={onSessionChange} />;
  } else if (view === 'users' && isAdmin) {
    content = <AdminUsers currentUsername={session.username} onSignOut={onSignOut} />;
  } else if (view === 'password') {
    content = <ChangePassword onDone={() => setView('documents')} />;
  } else {
    content = <Workspace canUpload={canUpload} onSignOut={onSignOut} />;
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-left">
          <span className="brand-name">Doc Intelligence Copilot</span>
          {!forced && (
            <nav className="nav" aria-label="Main">
              {navItems.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  aria-current={view === item.id ? 'page' : undefined}
                  onClick={() => setView(item.id)}
                >
                  {item.label}
                </button>
              ))}
            </nav>
          )}
        </div>
        <div className="topbar-right">
          <span className="who">
            {session.username}
            <span className="role-tag">{ROLE_LABELS[session.role] || session.role}</span>
          </span>
          <button type="button" className="btn-quiet" onClick={() => onSignOut()}>
            Sign out
          </button>
        </div>
      </header>
      <main className="content">{content}</main>
    </div>
  );
}
