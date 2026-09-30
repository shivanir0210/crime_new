import React from 'react';
import { CheckCircle2, XCircle, AlertTriangle, FileText, Video, Mic, ShieldAlert } from 'lucide-react';

export default function EvidenceCard({ name, status, type }) {
  const getIcon = () => {
    switch (type) {
      case 'cctv': return Video;
      case 'witness': return Mic;
      case 'weapon': return ShieldAlert;
      default: return FileText;
    }
  };

  const IconComponent = getIcon();

  const getStatusBadge = () => {
    switch (status) {
      case 'AVAILABLE':
        return (
          <span className="ev-status status-available">
            <CheckCircle2 size={13} /> AVAILABLE
          </span>
        );
      case 'MISSING':
        return (
          <span className="ev-status status-missing">
            <XCircle size={13} /> MISSING
          </span>
        );
      case 'PENDING':
        return (
          <span className="ev-status status-pending">
            <AlertTriangle size={13} /> PENDING
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className={`cs-evidence-card ev-${status.toLowerCase()}`}>
      <div className="ev-info">
        <IconComponent size={16} className="ev-type-icon" />
        <span className="ev-name">{name}</span>
      </div>
      {getStatusBadge()}

      <style>{`
        .cs-evidence-card {
          background-color: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-radius: var(--card-radius);
          padding: 10px 14px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          transition: border-color var(--transition-fast);
        }

        .cs-evidence-card:hover {
          border-color: rgba(167, 167, 170, 0.4);
        }

        .ev-missing {
          border-left: 3px solid var(--bright-crime-red);
        }

        .ev-available {
          border-left: 3px solid var(--color-success);
        }

        .ev-pending {
          border-left: 3px solid var(--color-warning);
        }

        .ev-info {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .ev-type-icon {
          color: var(--text-secondary);
        }

        .ev-name {
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--text-bright);
          letter-spacing: 0.04em;
        }

        .ev-status {
          display: flex;
          align-items: center;
          gap: 5px;
          font-family: var(--font-subheading);
          font-weight: 700;
          font-size: 0.7rem;
          letter-spacing: 0.08em;
          padding: 3px 8px;
          border-radius: 3px;
        }

        .status-available {
          color: #34D399;
          background: rgba(16, 185, 129, 0.12);
          border: 1px solid var(--color-success);
        }

        .status-missing {
          color: #FF4D4D;
          background: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
        }

        .status-pending {
          color: #FBBF24;
          background: rgba(217, 119, 6, 0.12);
          border: 1px solid var(--color-warning);
        }
      `}</style>
    </div>
  );
}
