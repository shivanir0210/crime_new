import React from 'react';

export default function Panel({ title, subtitle, icon: Icon, action, accent, children, className = '' }) {
  return (
    <div className={`cs-panel ${accent ? 'cs-panel-accent' : ''} ${className}`}>
      {(title || Icon || action) && (
        <div className="panel-header">
          <div className="panel-title-area">
            {Icon && <Icon size={18} className="panel-icon" />}
            <div>
              {title && <h3 className="panel-title">{title}</h3>}
              {subtitle && <p className="panel-subtitle">{subtitle}</p>}
            </div>
          </div>
          {action && <div className="panel-action">{action}</div>}
        </div>
      )}
      <div className="panel-body">
        {children}
      </div>

      <style>{`
        .cs-panel {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: var(--panel-radius);
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }

        .panel-header {
          padding: 12px 16px;
          border-bottom: 1px solid var(--border-color);
          background-color: var(--bg-panel-elevated);
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .panel-title-area {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .panel-icon {
          color: var(--bright-crime-red);
        }

        .panel-title {
          font-size: 0.95rem;
          font-family: var(--font-heading);
          letter-spacing: 0.06em;
          color: var(--text-bright);
          margin: 0;
          line-height: 1.2;
        }

        .panel-subtitle {
          font-size: 0.72rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .panel-body {
          padding: 16px;
          flex-grow: 1;
        }
      `}</style>
    </div>
  );
}
