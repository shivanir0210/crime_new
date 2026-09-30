import React from 'react';
import { 
  Building2, 
  UserCheck, 
  Tag, 
  Clock, 
  Copy, 
  Check, 
  AlertTriangle, 
  ShieldAlert, 
  Edit3, 
  Activity,
  Award
} from 'lucide-react';

export default function CaseHeader({
  caseData,
  caseState,
  onEditCase,
  onStatusChange,
  onPriorityChange
}) {
  const [copied, setCopied] = React.useState(false);

  if (!caseData) return null;

  const handleCopyNumber = () => {
    if (caseData.caseNumber) {
      navigator.clipboard.writeText(caseData.caseNumber);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Compute dynamic Case Risk Score (0-100) based on priority, type, and investigation state
  const computeRiskScore = () => {
    let score = 30; // base score
    if (caseData.priority === 'CRITICAL') score += 40;
    else if (caseData.priority === 'HIGH') score += 25;
    else if (caseData.priority === 'MEDIUM') score += 15;
    else score += 5;

    const type = (caseData.caseType || '').toUpperCase();
    if (type.includes('MURDER') || type.includes('HOMICIDE')) score += 20;
    else if (type.includes('CYBER') || type.includes('THEFT') || type.includes('ROBBERY')) score += 12;

    if (caseState?.missingAreas?.length > 2) score += 8;

    return Math.min(100, Math.max(10, score));
  };

  const riskScore = computeRiskScore();
  const getRiskLevel = (s) => {
    if (s >= 75) return { label: 'CRITICAL RISK', colorClass: 'risk-critical' };
    if (s >= 50) return { label: 'HIGH RISK', colorClass: 'risk-high' };
    if (s >= 30) return { label: 'MODERATE RISK', colorClass: 'risk-medium' };
    return { label: 'LOW RISK', colorClass: 'risk-low' };
  };
  const riskInfo = getRiskLevel(riskScore);

  const progress = caseState?.progressPercentage ?? 40;

  return (
    <div className="case-workspace-header">
      <div className="header-top-row">
        {/* Case Title & Identification */}
        <div className="case-id-group">
          <div className="case-number-badge-wrap">
            <span className="case-number-tag">{caseData.caseNumber}</span>
            <button 
              className="copy-btn" 
              onClick={handleCopyNumber}
              title="Copy Case Number"
            >
              {copied ? <Check size={13} className="copy-ok" /> : <Copy size={13} />}
            </button>
            <span className="case-type-tag">{(caseData.caseType || 'OTHER').replace(/_/g, ' ')}</span>
            <span className="station-pill">
              <Building2 size={12} />
              {caseData.station?.stationName || 'PRECINCT HEADQUARTERS'}
            </span>
          </div>
          <div className="title-row">
            <h1 className="case-title">{caseData.title}</h1>
            {onEditCase && (
              <button className="edit-title-btn" onClick={onEditCase} title="Edit Case Details">
                <Edit3 size={15} />
              </button>
            )}
          </div>
        </div>

        {/* Dynamic Risk Score & Investigation Progress Gauges */}
        <div className="header-metrics-group">
          {/* Risk Score Widget */}
          <div className={`metric-tile risk-tile ${riskInfo.colorClass}`}>
            <div className="metric-header">
              <ShieldAlert size={14} className="risk-icon" />
              <span className="metric-label">CASE RISK SCORE</span>
            </div>
            <div className="metric-score-wrap">
              <span className="score-number">{riskScore}</span>
              <span className="score-max">/100</span>
              <span className="risk-pill">{riskInfo.label}</span>
            </div>
            <div className="score-bar-bg">
              <div className="score-bar-fill" style={{ width: `${riskScore}%` }}></div>
            </div>
          </div>

          {/* Investigation Progress Widget */}
          <div className="metric-tile progress-tile">
            <div className="metric-header">
              <Activity size={14} className="progress-icon" />
              <span className="metric-label">INVESTIGATION PROGRESS</span>
            </div>
            <div className="metric-score-wrap">
              <span className="score-number">{progress}%</span>
              <span className="progress-stage-pill">{caseState?.stateLabel || 'IN PROGRESS'}</span>
            </div>
            <div className="score-bar-bg">
              <div className="score-bar-fill progress-fill" style={{ width: `${progress}%` }}></div>
            </div>
          </div>
        </div>
      </div>

      {/* Controller & Meta Bar */}
      <div className="header-meta-bar">
        <div className="meta-left">
          <div className="meta-info-item">
            <UserCheck size={14} className="meta-icon" />
            <span className="meta-lbl">ASSIGNED OFFICER:</span>
            <span className="meta-val highlight-officer">
              {caseData.createdBy?.fullName || 'STATION OFFICER'} ({caseData.createdBy?.policeId || 'TN-POL'})
            </span>
          </div>

          <div className="meta-info-item">
            <Clock size={14} className="meta-icon" />
            <span className="meta-lbl">FILED:</span>
            <span className="meta-val">
              {caseData.createdAt ? new Date(caseData.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'N/A'}
            </span>
          </div>
        </div>

        {/* Quick Controllers for Status & Priority */}
        <div className="meta-controllers">
          <div className="ctrl-item">
            <span className="ctrl-tag">STATUS:</span>
            <select
              value={caseData.status}
              onChange={(e) => onStatusChange && onStatusChange(e.target.value)}
              className={`case-select status-select status-${(caseData.status || 'open').toLowerCase()}`}
            >
              <option value="OPEN">OPEN</option>
              <option value="UNDER_INVESTIGATION">UNDER INVESTIGATION</option>
              <option value="PENDING_REVIEW">PENDING REVIEW</option>
              <option value="ON_HOLD">ON HOLD</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>

          <div className="ctrl-item">
            <span className="ctrl-tag">PRIORITY:</span>
            <select
              value={caseData.priority}
              onChange={(e) => onPriorityChange && onPriorityChange(e.target.value)}
              className={`case-select priority-select priority-${(caseData.priority || 'medium').toLowerCase()}`}
            >
              <option value="CRITICAL">CRITICAL (RED ALERT)</option>
              <option value="HIGH">HIGH PRIORITY</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="LOW">LOW</option>
            </select>
          </div>
        </div>
      </div>

      <style>{`
        .case-workspace-header {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 8px;
          padding: 20px;
          margin-bottom: 24px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
        }
        .header-top-row {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          gap: 20px;
          margin-bottom: 16px;
        }
        @media (max-width: 900px) {
          .header-top-row {
            flex-direction: column;
          }
        }
        .case-id-group {
          flex-grow: 1;
        }
        .case-number-badge-wrap {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
          margin-bottom: 6px;
        }
        .case-number-tag {
          font-family: var(--font-mono, monospace);
          font-size: 0.85rem;
          font-weight: 700;
          color: #fff;
          background: #c1121f;
          padding: 2px 8px;
          border-radius: 4px;
          letter-spacing: 0.05em;
        }
        .copy-btn {
          background: #1f2430;
          border: 1px solid #2d3444;
          color: #8c93a8;
          padding: 3px 6px;
          border-radius: 3px;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.15s;
        }
        .copy-btn:hover {
          color: #fff;
          background: #2b3345;
        }
        .copy-ok {
          color: #10b981;
        }
        .case-type-tag {
          font-family: var(--font-mono, monospace);
          font-size: 0.72rem;
          background: rgba(45, 52, 68, 0.6);
          color: #a0aec0;
          padding: 2px 8px;
          border-radius: 3px;
          border: 1px solid #2d3444;
        }
        .station-pill {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 0.72rem;
          color: #60a5fa;
          background: rgba(96, 165, 250, 0.1);
          border: 1px solid rgba(96, 165, 250, 0.25);
          padding: 2px 8px;
          border-radius: 3px;
        }
        .title-row {
          display: flex;
          align-items: center;
          gap: 10px;
        }
        .case-title {
          font-family: var(--font-heading, sans-serif);
          font-size: 1.4rem;
          font-weight: 700;
          color: #f1f3f9;
          margin: 0;
          line-height: 1.3;
        }
        .edit-title-btn {
          background: transparent;
          border: 1px solid transparent;
          color: #64748b;
          cursor: pointer;
          padding: 4px;
          border-radius: 4px;
          transition: all 0.15s;
        }
        .edit-title-btn:hover {
          color: #fff;
          background: #1f2430;
          border-color: #2d3444;
        }
        .header-metrics-group {
          display: flex;
          gap: 14px;
          flex-shrink: 0;
        }
        @media (max-width: 600px) {
          .header-metrics-group {
            width: 100%;
            flex-direction: column;
          }
        }
        .metric-tile {
          background: #181c26;
          border: 1px solid #232733;
          border-radius: 6px;
          padding: 10px 14px;
          min-width: 180px;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .metric-header {
          display: flex;
          align-items: center;
          gap: 6px;
        }
        .metric-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          color: #8c93a8;
          letter-spacing: 0.08em;
        }
        .risk-icon { color: #e63946; }
        .progress-icon { color: #3b82f6; }
        .metric-score-wrap {
          display: flex;
          align-items: baseline;
          gap: 6px;
        }
        .score-number {
          font-family: var(--font-mono, monospace);
          font-size: 1.25rem;
          font-weight: 800;
          color: #fff;
        }
        .score-max {
          font-family: var(--font-mono, monospace);
          font-size: 0.75rem;
          color: #64748b;
        }
        .risk-pill {
          font-size: 0.65rem;
          font-weight: 700;
          padding: 1px 6px;
          border-radius: 3px;
          margin-left: auto;
          letter-spacing: 0.04em;
        }
        .risk-tile.risk-critical .risk-pill {
          background: rgba(230, 57, 70, 0.2);
          color: #e63946;
          border: 1px solid rgba(230, 57, 70, 0.5);
        }
        .risk-tile.risk-high .risk-pill {
          background: rgba(249, 115, 22, 0.2);
          color: #f97316;
          border: 1px solid rgba(249, 115, 22, 0.5);
        }
        .risk-tile.risk-medium .risk-pill {
          background: rgba(234, 179, 8, 0.2);
          color: #eab308;
          border: 1px solid rgba(234, 179, 8, 0.5);
        }
        .progress-stage-pill {
          font-size: 0.65rem;
          font-weight: 600;
          color: #60a5fa;
          background: rgba(96, 165, 250, 0.15);
          border: 1px solid rgba(96, 165, 250, 0.3);
          padding: 1px 6px;
          border-radius: 3px;
          margin-left: auto;
          white-space: nowrap;
        }
        .score-bar-bg {
          height: 4px;
          background: #252b3b;
          border-radius: 2px;
          overflow: hidden;
        }
        .score-bar-fill {
          height: 100%;
          background: linear-gradient(90deg, #f97316, #e63946);
          border-radius: 2px;
          transition: width 0.3s;
        }
        .progress-fill {
          background: linear-gradient(90deg, #3b82f6, #10b981);
        }
        .header-meta-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 12px;
          padding-top: 14px;
          border-top: 1px solid #1f2432;
        }
        .meta-left {
          display: flex;
          align-items: center;
          gap: 16px;
          flex-wrap: wrap;
        }
        .meta-info-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.78rem;
        }
        .meta-icon {
          color: #64748b;
        }
        .meta-lbl {
          color: #64748b;
          font-weight: 600;
          font-size: 0.72rem;
          letter-spacing: 0.04em;
        }
        .meta-val {
          color: #d1d5db;
        }
        .highlight-officer {
          color: #f1f3f9;
          font-weight: 600;
        }
        .meta-controllers {
          display: flex;
          align-items: center;
          gap: 12px;
        }
        .ctrl-item {
          display: flex;
          align-items: center;
          gap: 6px;
        }
        .ctrl-tag {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 700;
          color: #8c93a8;
          letter-spacing: 0.06em;
        }
        .case-select {
          font-family: var(--font-mono, monospace);
          font-size: 0.75rem;
          font-weight: 700;
          padding: 4px 10px;
          border-radius: 4px;
          border: 1px solid #2a3142;
          background: #181c26;
          color: #f1f3f9;
          cursor: pointer;
        }
        .priority-select.priority-critical {
          color: #e63946;
          border-color: rgba(230, 57, 70, 0.4);
          background: rgba(230, 57, 70, 0.08);
        }
        .priority-select.priority-high {
          color: #f97316;
          border-color: rgba(249, 115, 22, 0.4);
        }
        .status-select.status-under_investigation {
          color: #60a5fa;
          border-color: rgba(96, 165, 250, 0.4);
        }
      `}</style>
    </div>
  );
}
