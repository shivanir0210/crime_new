import React from 'react';
import { Clock, CheckCircle, AlertCircle, FileText, Upload, Brain } from 'lucide-react';

export default function Timeline({ caseId = 'CASE-1024', events = [] }) {
  const defaultEvents = [
    { time: '09:20', label: 'FIR REGISTERED', status: 'completed', icon: FileText },
    { time: '10:15', label: 'VICTIM PROFILE CREATED', status: 'completed', icon: CheckCircle },
    { time: '11:05', label: 'EVIDENCE UPLOADED', status: 'completed', icon: Upload },
    { time: '12:10', label: 'CASE ANALYSIS PENDING', status: 'active', isImportant: true, icon: Brain },
  ];

  const items = events.length > 0 ? events : defaultEvents;

  return (
    <div className="cs-timeline-container">
      <div className="timeline-case-badge">
        <Clock size={14} className="badge-icon" />
        <span>TIMELINE LOG: {caseId}</span>
      </div>

      <div className="timeline-list">
        {items.map((item, idx) => {
          const Icon = item.icon || Clock;
          return (
            <div 
              key={idx} 
              className={`timeline-item ${item.isImportant ? 'important' : ''} ${item.status}`}
            >
              <div className="timeline-node">
                <span className="node-dot"></span>
                {idx < items.length - 1 && <div className="timeline-connector"></div>}
              </div>

              <div className="timeline-content">
                <div className="timeline-time">{item.time}</div>
                <div className="timeline-title-row">
                  <Icon size={14} className="timeline-icon" />
                  <span className="timeline-label">{item.label}</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <style>{`
        .cs-timeline-container {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }

        .timeline-case-badge {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-secondary);
          background-color: var(--bg-panel-elevated);
          padding: 6px 10px;
          border: 1px solid var(--border-color);
          border-radius: 3px;
        }

        .badge-icon {
          color: var(--bright-crime-red);
        }

        .timeline-list {
          display: flex;
          flex-direction: column;
          padding-left: 6px;
        }

        .timeline-item {
          display: flex;
          gap: 14px;
          position: relative;
          padding-bottom: 16px;
        }

        .timeline-item:last-child {
          padding-bottom: 0;
        }

        .timeline-node {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 14px;
          flex-shrink: 0;
        }

        .node-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          background-color: var(--border-color);
          border: 2px solid var(--bg-panel);
          margin-top: 4px;
          z-index: 2;
          transition: all var(--transition-fast);
        }

        .timeline-item.completed .node-dot {
          background-color: var(--color-success);
          box-shadow: 0 0 6px rgba(16, 185, 129, 0.4);
        }

        .timeline-item.important .node-dot {
          background-color: var(--bright-crime-red);
          box-shadow: 0 0 8px rgba(193, 18, 31, 0.8);
          animation: pulse-red 1.8s infinite;
        }

        .timeline-connector {
          width: 2px;
          flex-grow: 1;
          background-color: var(--border-color);
          margin-top: 2px;
          margin-bottom: -4px;
        }

        .timeline-content {
          background: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          padding: 8px 12px;
          border-radius: 4px;
          flex-grow: 1;
        }

        .timeline-item.important .timeline-content {
          border-color: rgba(193, 18, 31, 0.5);
          background: linear-gradient(90deg, rgba(193, 18, 31, 0.1) 0%, var(--bg-panel-elevated) 100%);
        }

        .timeline-time {
          font-family: var(--font-mono);
          font-size: 0.7rem;
          color: var(--bright-crime-red);
          font-weight: 700;
          letter-spacing: 0.05em;
        }

        .timeline-title-row {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-top: 2px;
        }

        .timeline-icon {
          color: var(--text-secondary);
        }

        .timeline-item.important .timeline-icon {
          color: var(--bright-crime-red);
        }

        .timeline-label {
          font-family: var(--font-heading);
          font-size: 0.82rem;
          font-weight: 600;
          letter-spacing: 0.04em;
          color: var(--text-bright);
        }
      `}</style>
    </div>
  );
}
