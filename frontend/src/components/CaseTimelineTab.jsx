import React, { useState } from 'react';
import { 
  Clock, 
  RefreshCw, 
  Filter, 
  FileBadge, 
  FolderLock, 
  Eye, 
  Users, 
  UserX, 
  ShieldAlert, 
  Cpu, 
  CheckCircle2,
  Calendar
} from 'lucide-react';

export default function CaseTimelineTab({
  timelineEvents = [],
  loading = false,
  onRefresh
}) {
  const [filterType, setFilterType] = useState('ALL');

  const filteredEvents = timelineEvents.filter(evt => {
    if (filterType === 'ALL') return true;
    if (filterType === 'FIR') return evt.eventType === 'FIR' || evt.eventType === 'INCIDENT';
    if (filterType === 'EVIDENCE') return evt.eventType === 'EVIDENCE';
    if (filterType === 'WITNESS') return evt.eventType === 'WITNESS';
    if (filterType === 'SUSPECT') return evt.eventType === 'SUSPECT';
    if (filterType === 'CASE_UPDATE') return evt.eventType === 'CASE_INITIATION' || evt.eventType === 'STATUS_CHANGE';
    return true;
  });

  const getEventIcon = (type) => {
    switch (type) {
      case 'INCIDENT':
      case 'FIR':
        return <FileBadge size={16} className="text-amber" />;
      case 'EVIDENCE':
        return <FolderLock size={16} className="text-emerald" />;
      case 'WITNESS':
        return <Eye size={16} className="text-cyan" />;
      case 'SUSPECT':
        return <Users size={16} className="text-crimson" />;
      case 'VICTIM':
        return <UserX size={16} className="text-purple" />;
      case 'AI_ANALYSIS':
        return <Cpu size={16} className="text-blue" />;
      default:
        return <Clock size={16} className="text-muted" />;
    }
  };

  return (
    <div className="case-timeline-tab-container">
      {/* Timeline Controls Header */}
      <div className="timeline-header-bar">
        <div className="title-group">
          <Clock size={18} className="text-amber" />
          <span className="tab-title">CHRONOLOGICAL INVESTIGATION TIMELINE</span>
          <span className="event-count-tag">{timelineEvents.length} TOTAL EVENTS</span>
        </div>

        <div className="controls-group">
          {/* Category Filters */}
          <div className="filter-chips">
            <button 
              className={`chip-btn ${filterType === 'ALL' ? 'active' : ''}`}
              onClick={() => setFilterType('ALL')}
            >
              ALL ({timelineEvents.length})
            </button>
            <button 
              className={`chip-btn ${filterType === 'FIR' ? 'active' : ''}`}
              onClick={() => setFilterType('FIR')}
            >
              FIR & CRIME
            </button>
            <button 
              className={`chip-btn ${filterType === 'EVIDENCE' ? 'active' : ''}`}
              onClick={() => setFilterType('EVIDENCE')}
            >
              EVIDENCE
            </button>
            <button 
              className={`chip-btn ${filterType === 'WITNESS' ? 'active' : ''}`}
              onClick={() => setFilterType('WITNESS')}
            >
              WITNESSES
            </button>
            <button 
              className={`chip-btn ${filterType === 'SUSPECT' ? 'active' : ''}`}
              onClick={() => setFilterType('SUSPECT')}
            >
              SUSPECTS
            </button>
          </div>

          <button className="refresh-timeline-btn" onClick={onRefresh} title="Reload Timeline">
            <RefreshCw size={14} className={loading ? 'spin-icon' : ''} />
            <span>REFRESH</span>
          </button>
        </div>
      </div>

      {/* Timeline Content */}
      <div className="timeline-body">
        {loading ? (
          <div className="timeline-loading">
            <RefreshCw size={24} className="spin-icon text-amber" />
            <span>Loading investigation chronological milestones...</span>
          </div>
        ) : filteredEvents.length === 0 ? (
          <div className="timeline-empty">
            <Calendar size={36} className="text-muted" />
            <div className="empty-title">NO TIMELINE EVENTS RECORDED</div>
            <div className="empty-desc">
              Investigation activities such as FIR filing, witness interviews, evidence collection, and suspect updates will be automatically reconstructed here in chronological sequence.
            </div>
          </div>
        ) : (
          <div className="timeline-flow">
            {filteredEvents.map((evt, idx) => (
              <div key={evt.id || idx} className="flow-item">
                {/* Timeline Spine */}
                <div className="flow-spine">
                  <div className={`node-circle circle-${evt.badgeColor || 'blue'}`}>
                    {getEventIcon(evt.eventType)}
                  </div>
                  {idx < filteredEvents.length - 1 && <div className="flow-line"></div>}
                </div>

                {/* Event Card */}
                <div className="flow-card">
                  <div className="flow-card-header">
                    <div className="event-meta-top">
                      <span className={`event-type-pill pill-${evt.badgeColor || 'blue'}`}>
                        {evt.eventType}
                      </span>
                      <span className="event-timestamp">
                        {evt.timestamp ? new Date(evt.timestamp).toLocaleString('en-IN', {
                          weekday: 'short',
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        }) : 'Undated Milestone'}
                      </span>
                    </div>
                    <div className="event-title">{evt.title}</div>
                  </div>

                  <div className="flow-card-body">
                    <p className="event-description">{evt.description}</p>
                    
                    {/* Metadata tags */}
                    {evt.metadata && Object.keys(evt.metadata).length > 0 && (
                      <div className="event-metadata-tags">
                        {Object.entries(evt.metadata).map(([k, v]) => v ? (
                          <span key={k} className="meta-tag">
                            <span className="meta-key">{k}:</span> {v}
                          </span>
                        ) : null)}
                      </div>
                    )}

                    {evt.actor && (
                      <div className="event-actor-line">
                        <span>Recorded by: <strong>{evt.actor}</strong></span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <style>{`
        .case-timeline-tab-container {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 6px;
          overflow: hidden;
        }
        .timeline-header-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 14px 18px;
          background: #181c26;
          border-bottom: 1px solid #232733;
          flex-wrap: wrap;
          gap: 12px;
        }
        .title-group {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .tab-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.8rem;
          font-weight: 700;
          color: #f1f3f9;
          letter-spacing: 0.08em;
        }
        .event-count-tag {
          font-family: var(--font-mono, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          background: rgba(245, 158, 11, 0.15);
          color: #f59e0b;
          border: 1px solid rgba(245, 158, 11, 0.3);
          padding: 2px 6px;
          border-radius: 3px;
        }
        .controls-group {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }
        .filter-chips {
          display: flex;
          gap: 6px;
          flex-wrap: wrap;
        }
        .chip-btn {
          background: #1e2432;
          border: 1px solid #2d364a;
          color: #8c93a8;
          padding: 4px 10px;
          border-radius: 4px;
          font-size: 0.7rem;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.15s;
        }
        .chip-btn:hover {
          color: #fff;
          background: #283042;
        }
        .chip-btn.active {
          background: rgba(245, 158, 11, 0.15);
          border-color: #f59e0b;
          color: #f1f3f9;
        }
        .refresh-timeline-btn {
          display: flex;
          align-items: center;
          gap: 6px;
          background: #252a38;
          color: #cbd5e1;
          border: 1px solid #333a4d;
          padding: 5px 10px;
          border-radius: 4px;
          font-size: 0.72rem;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.15s;
        }
        .refresh-timeline-btn:hover {
          background: #333a4d;
          color: #fff;
        }
        .spin-icon {
          animation: spin 1s linear infinite;
        }
        @keyframes spin { 100% { transform: rotate(360deg); } }

        .timeline-body {
          padding: 24px;
        }
        .timeline-loading, .timeline-empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 48px 20px;
          text-align: center;
          gap: 12px;
          color: #8c93a8;
        }
        .empty-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.85rem;
          font-weight: 700;
          color: #e2e8f0;
          letter-spacing: 0.08em;
        }
        .empty-desc {
          font-size: 0.78rem;
          color: #64748b;
          max-width: 480px;
          line-height: 1.4;
        }

        .timeline-flow {
          display: flex;
          flex-direction: column;
        }
        .flow-item {
          display: flex;
          gap: 16px;
        }
        .flow-spine {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 36px;
          flex-shrink: 0;
        }
        .node-circle {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 1.5px solid;
          background: #141720;
          z-index: 2;
        }
        .circle-red { border-color: #e63946; background: rgba(230, 57, 70, 0.1); }
        .circle-blue { border-color: #3b82f6; background: rgba(59, 130, 246, 0.1); }
        .circle-amber { border-color: #f59e0b; background: rgba(245, 158, 11, 0.1); }
        .circle-emerald { border-color: #10b981; background: rgba(16, 185, 129, 0.1); }
        .circle-purple { border-color: #a855f7; background: rgba(168, 85, 247, 0.1); }
        .circle-cyan { border-color: #06b6d4; background: rgba(6, 182, 212, 0.1); }

        .flow-line {
          width: 2px;
          background: #232733;
          flex-grow: 1;
          margin: 4px 0;
        }
        .flow-card {
          flex-grow: 1;
          background: #181c26;
          border: 1px solid #232733;
          border-radius: 6px;
          padding: 14px 16px;
          margin-bottom: 20px;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }
        .flow-card-header {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .event-meta-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 8px;
        }
        .event-type-pill {
          font-family: var(--font-mono, monospace);
          font-size: 0.62rem;
          font-weight: 700;
          padding: 1px 6px;
          border-radius: 3px;
        }
        .pill-red { background: rgba(230, 57, 70, 0.15); color: #e63946; }
        .pill-blue { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }
        .pill-amber { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
        .pill-emerald { background: rgba(16, 185, 129, 0.15); color: #34d399; }
        .pill-purple { background: rgba(168, 85, 247, 0.15); color: #c084fc; }
        .pill-cyan { background: rgba(6, 182, 212, 0.15); color: #22d3ee; }

        .event-timestamp {
          font-family: var(--font-mono, monospace);
          font-size: 0.7rem;
          color: #8c93a8;
        }
        .event-title {
          font-size: 0.88rem;
          font-weight: 700;
          color: #f1f3f9;
        }
        .event-description {
          font-size: 0.78rem;
          color: #cbd5e1;
          margin: 0;
          line-height: 1.4;
        }
        .event-metadata-tags {
          display: flex;
          gap: 6px;
          flex-wrap: wrap;
          margin-top: 6px;
        }
        .meta-tag {
          font-family: var(--font-mono, monospace);
          font-size: 0.68rem;
          background: #141720;
          border: 1px solid #283042;
          padding: 2px 6px;
          border-radius: 3px;
          color: #94a3b8;
        }
        .meta-key {
          color: #64748b;
        }
        .event-actor-line {
          font-size: 0.7rem;
          color: #64748b;
          margin-top: 4px;
        }
        .event-actor-line strong {
          color: #94a3b8;
        }
      `}</style>
    </div>
  );
}
