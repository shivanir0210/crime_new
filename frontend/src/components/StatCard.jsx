import React from 'react';

export default function StatCard({ title, value, icon: Icon, isCritical, subtitle }) {
  return (
    <div className={`cs-stat-card ${isCritical ? 'critical' : ''}`}>
      <div className="stat-card-top">
        <span className="stat-title">{title}</span>
        {Icon && <Icon size={18} className={`stat-icon ${isCritical ? 'critical-icon' : ''}`} />}
      </div>
      <div className="stat-value-container">
        <span className={`stat-value ${isCritical ? 'critical-value' : ''}`}>{value}</span>
      </div>
      {subtitle && <div className="stat-subtitle">{subtitle}</div>}

      <style>{`
        .cs-stat-card {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: var(--panel-radius);
          padding: 16px;
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          position: relative;
          transition: all var(--transition-normal);
        }

        .cs-stat-card:hover {
          border-color: rgba(193, 18, 31, 0.4);
          transform: translateY(-2px);
        }

        .cs-stat-card.critical {
          border-top: 2px solid var(--bright-crime-red);
          background: linear-gradient(180deg, rgba(193, 18, 31, 0.08) 0%, var(--bg-panel) 100%);
        }

        .stat-card-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 8px;
        }

        .stat-title {
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.12em;
          color: var(--text-secondary);
          text-transform: uppercase;
        }

        .stat-icon {
          color: var(--text-muted);
        }

        .critical-icon {
          color: var(--bright-crime-red);
        }

        .stat-value-container {
          margin-top: 4px;
        }

        .stat-value {
          font-family: var(--font-heading);
          font-size: 2.2rem;
          font-weight: 700;
          color: var(--text-bright);
          line-height: 1;
          letter-spacing: 0.02em;
        }

        .critical-value {
          color: var(--bright-crime-red);
          text-shadow: 0 0 12px rgba(193, 18, 31, 0.4);
        }

        .stat-subtitle {
          font-size: 0.7rem;
          color: var(--text-muted);
          margin-top: 8px;
        }
      `}</style>
    </div>
  );
}
