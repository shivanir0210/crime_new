import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert } from 'lucide-react';

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="cs-loading-screen forensic-grid scanline-overlay">
        <div className="loading-card">
          <ShieldAlert className="loading-icon animate-pulse" size={36} />
          <div className="loading-text">VERIFYING POLICE SECURITY CREDENTIALS...</div>
          <div className="loading-sub">CRIMESPHERE AI ACCESS PROTOCOL</div>
        </div>

        <style>{`
          .cs-loading-screen {
            min-height: 100vh;
            width: 100vw;
            display: flex;
            align-items: center;
            justify-content: center;
            background-color: var(--bg-primary);
          }
          .loading-card {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 14px;
            padding: 30px 40px;
            background-color: var(--bg-panel);
            border: 1px solid var(--border-color);
            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.8), var(--red-glow-subtle);
            border-radius: 4px;
          }
          .loading-icon {
            color: var(--bright-crime-red);
          }
          .loading-text {
            font-family: var(--font-subheading);
            font-size: 0.85rem;
            font-weight: 700;
            letter-spacing: 0.15em;
            color: var(--text-bright);
          }
          .loading-sub {
            font-family: var(--font-mono);
            font-size: 0.68rem;
            color: var(--text-muted);
            letter-spacing: 0.1em;
          }
          .animate-pulse {
            animation: pulse 1.5s infinite ease-in-out;
          }
          @keyframes pulse {
            0%, 100% { opacity: 0.4; transform: scale(0.96); }
            50% { opacity: 1; transform: scale(1.04); }
          }
        `}</style>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}
