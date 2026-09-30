import React from 'react';
import StatusBadge from './StatusBadge';
import { AlertCircle, Clock, FileWarning } from 'lucide-react';

export default function CaseCard({ id, title, priority, evidenceGaps, lastUpdated, isSelected, onClick }) {
  return (
    <div 
      className={`cs-case-card ${isSelected ? 'selected' : ''}`}
      onClick={onClick}
    >
      <div className="case-card-header">
        <span className="case-id">{id}</span>
        <StatusBadge type={priority} label={`${priority} PRIORITY`} />
      </div>

      <div className="case-title">{title}</div>

      <div className="case-card-footer">
        <div className="gap-count">
          <FileWarning size={14} className="gap-icon" />
          <span>Evidence Gaps: <strong>{evidenceGaps}</strong></span>
        </div>
        {lastUpdated && (
          <div className="updated-time">
            <Clock size={12} />
            <span>{lastUpdated}</span>
          </div>
        )}
      </div>

      <style>{`
        .cs-case-card {
          background-color: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-radius: var(--card-radius);
          padding: 12px 14px;
          cursor: pointer;
          transition: all var(--transition-fast);
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .cs-case-card:hover {
          border-color: rgba(193, 18, 31, 0.4);
          background-color: #1A1A1E;
        }

        .cs-case-card.selected {
          border-color: var(--bright-crime-red);
          background: linear-gradient(90deg, rgba(193, 18, 31, 0.12) 0%, var(--bg-panel-elevated) 100%);
          box-shadow: var(--red-glow-subtle);
        }

        .case-card-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .case-id {
          font-family: var(--font-subheading);
          font-weight: 700;
          font-size: 0.85rem;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .case-title {
          font-family: var(--font-heading);
          font-size: 0.95rem;
          font-weight: 600;
          color: var(--text-primary);
          letter-spacing: 0.03em;
        }

        .case-card-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding-top: 6px;
          border-top: 1px dashed rgba(42, 42, 45, 0.6);
          font-size: 0.72rem;
          color: var(--text-secondary);
        }

        .gap-count {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .gap-icon {
          color: var(--bright-crime-red);
        }

        .updated-time {
          display: flex;
          align-items: center;
          gap: 4px;
          color: var(--text-muted);
        }
      `}</style>
    </div>
  );
}
