import React from 'react';
import { 
  FolderLock, 
  UserX, 
  Users, 
  Eye, 
  CheckCircle, 
  AlertTriangle, 
  ShieldAlert, 
  Clock, 
  BrainCircuit, 
  Activity, 
  ArrowRight, 
  Plus, 
  FileBadge,
  Sparkles,
  ChevronRight,
  ShieldCheck
} from 'lucide-react';

export default function CaseCommandCenter({
  caseData,
  caseState,
  evidenceList = [],
  victims = [],
  suspects = [],
  witnesses = [],
  firData = null,
  timelineEvents = [],
  evidenceGaps = [],
  aiRecommendations = [],
  onNavigateTab,
  onOpenUploadEvidence,
  onOpenAddWitness
}) {
  // Compute counts
  const evidenceCount = evidenceList.length;
  const verifiedEvidenceCount = evidenceList.filter(e => e.status === 'VERIFIED').length;
  const verificationRate = evidenceCount > 0 ? Math.round((verifiedEvidenceCount / evidenceCount) * 100) : 0;
  
  const victimCount = victims.length;
  const suspectCount = suspects.length;
  const witnessCount = witnesses.length;

  // Derive Missing Evidence
  const missingItems = [];
  if (evidenceGaps && evidenceGaps.length > 0) {
    evidenceGaps
      .filter(g => g.status === 'OPEN')
      .forEach(g => {
        missingItems.push({
          id: g.id,
          title: g.title,
          description: g.description,
          severity: g.severity || 'HIGH',
          category: g.category || 'EVIDENCE'
        });
      });
  }

  // Fallback missing checks if gaps not yet analyzed
  if (missingItems.length === 0) {
    const caseType = (caseData?.caseType || '').toUpperCase();
    const evTypes = evidenceList.map(e => (e.evidenceType || '').toUpperCase());
    if (caseType.includes('MURDER') || caseType.includes('HOMICIDE')) {
      if (!evTypes.some(t => t.includes('FORENSIC'))) {
        missingItems.push({ id: 'pm-1', title: 'Forensic Autopsy / Post-Mortem Report', description: 'Mandatory post-mortem pathological report required for violent crime dossier.', severity: 'CRITICAL', category: 'FORENSIC' });
      }
      if (!evTypes.some(t => t.includes('CCTV') || t.includes('VIDEO'))) {
        missingItems.push({ id: 'cctv-1', title: 'CCTV Route Ingress/Egress Surveillance', description: 'Surveillance camera footage along known suspect escape routes.', severity: 'HIGH', category: 'CCTV' });
      }
    }
    if (!firData) {
      missingItems.push({ id: 'fir-1', title: 'FIR Registration Form', description: 'Official First Information Report documentation has not been registered.', severity: 'CRITICAL', category: 'FIR' });
    }
  }

  // Mini Timeline (last 4 events)
  const recentTimeline = (timelineEvents || []).slice(0, 4);

  return (
    <div className="command-center-container">
      {/* 1. TOP KEY METRICS STRIP */}
      <div className="metrics-grid">
        {/* Metric 1: Evidence */}
        <div className="stat-card" onClick={() => onNavigateTab && onNavigateTab('EVIDENCE')}>
          <div className="stat-card-header">
            <span className="stat-label">TOTAL EVIDENCE</span>
            <FolderLock size={16} className="stat-icon icon-blue" />
          </div>
          <div className="stat-val-row">
            <span className="stat-value">{evidenceCount}</span>
            <span className="stat-badge badge-info">
              {verifiedEvidenceCount} VERIFIED
            </span>
          </div>
          <div className="stat-footer-text">
            {evidenceCount - verifiedEvidenceCount} awaiting forensic review
          </div>
        </div>

        {/* Metric 2: Victims */}
        <div className="stat-card" onClick={() => onNavigateTab && onNavigateTab('VICTIMS')}>
          <div className="stat-card-header">
            <span className="stat-label">VICTIMS RECORDED</span>
            <UserX size={16} className="stat-icon icon-purple" />
          </div>
          <div className="stat-val-row">
            <span className="stat-value">{victimCount}</span>
            <span className="stat-badge badge-neutral">ACTIVE</span>
          </div>
          <div className="stat-footer-text">
            {victims.filter(v => v.status === 'DECEASED').length} deceased profiles
          </div>
        </div>

        {/* Metric 3: Suspects */}
        <div className="stat-card" onClick={() => onNavigateTab && onNavigateTab('SUSPECTS')}>
          <div className="stat-card-header">
            <span className="stat-label">SUSPECTS LINKED</span>
            <Users size={16} className="stat-icon icon-red" />
          </div>
          <div className="stat-val-row">
            <span className="stat-value">{suspectCount}</span>
            <span className="stat-badge badge-warn">
              {suspects.filter(s => s.investigationStatus === 'UNDER_INVESTIGATION').length} UNDER INVESTIGATION
            </span>
          </div>
          <div className="stat-footer-text">
            Person-of-interest dossiers
          </div>
        </div>

        {/* Metric 4: Witnesses */}
        <div className="stat-card" onClick={() => onNavigateTab && onNavigateTab('WITNESSES')}>
          <div className="stat-card-header">
            <span className="stat-label">WITNESSES INTERVIEWED</span>
            <Eye size={16} className="stat-icon icon-cyan" />
          </div>
          <div className="stat-val-row">
            <span className="stat-value">{witnessCount}</span>
            <span className="stat-badge badge-cyan">
              {witnesses.filter(w => w.reliabilityScore === 'HIGH').length} HIGH CREDIBILITY
            </span>
          </div>
          <div className="stat-footer-text">
            Formal testimonies filed
          </div>
        </div>

        {/* Metric 5: Evidence Verification Status */}
        <div className="stat-card" onClick={() => onNavigateTab && onNavigateTab('EVIDENCE')}>
          <div className="stat-card-header">
            <span className="stat-label">VERIFICATION HEALTH</span>
            <ShieldCheck size={16} className="stat-icon icon-green" />
          </div>
          <div className="stat-val-row">
            <span className="stat-value">{verificationRate}%</span>
            <span className={`stat-badge ${verificationRate >= 60 ? 'badge-success' : 'badge-warn'}`}>
              {verificationRate >= 60 ? 'HEALTHY' : 'NEEDS REVIEW'}
            </span>
          </div>
          <div className="verification-bar">
            <div className="verification-fill" style={{ width: `${verificationRate}%` }}></div>
          </div>
        </div>
      </div>

      {/* 2. COMMAND CENTER MAIN TWO-COLUMN LAYOUT */}
      <div className="command-grid-layout">
        {/* Left Column: Missing Evidence Panel & AI Investigation Leads */}
        <div className="command-col-left">
          {/* Missing Evidence Panel */}
          <div className="panel-box missing-evidence-panel">
            <div className="panel-box-header">
              <div className="panel-title-wrap">
                <AlertTriangle size={17} className="text-crimson" />
                <span className="panel-title">MISSING EVIDENCE & GAP ANALYSIS</span>
                <span className="gap-count-tag">{missingItems.length} DETECTED</span>
              </div>
              <button 
                className="panel-action-btn"
                onClick={onOpenUploadEvidence}
              >
                <Plus size={13} />
                <span>UPLOAD EVIDENCE</span>
              </button>
            </div>

            <div className="panel-box-body">
              {missingItems.length === 0 ? (
                <div className="clean-state">
                  <CheckCircle size={24} className="text-emerald" />
                  <span>No critical evidence gaps detected. Investigation evidence checklist is satisfied.</span>
                </div>
              ) : (
                <div className="missing-items-list">
                  {missingItems.slice(0, 4).map((item, idx) => (
                    <div key={item.id || idx} className={`missing-item-row sev-${(item.severity || 'high').toLowerCase()}`}>
                      <div className="missing-item-info">
                        <div className="missing-item-title-row">
                          <span className={`severity-tag sev-${(item.severity || 'high').toLowerCase()}`}>
                            {item.severity}
                          </span>
                          <span className="missing-title">{item.title}</span>
                          <span className="missing-category">{item.category}</span>
                        </div>
                        <div className="missing-desc">{item.description}</div>
                      </div>
                      <button 
                        className="quick-resolve-btn"
                        onClick={onOpenUploadEvidence}
                        title="Upload matching evidence item"
                      >
                        ATTACH
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* AI-Generated Investigation Leads */}
          <div className="panel-box ai-leads-panel">
            <div className="panel-box-header">
              <div className="panel-title-wrap">
                <BrainCircuit size={17} className="text-blue" />
                <span className="panel-title">AI INVESTIGATION REASONING & LEADS</span>
                <span className="ai-active-pill">NEURAL ACTIVE</span>
              </div>
              <button 
                className="panel-action-btn"
                onClick={() => onNavigateTab && onNavigateTab('AI_INVESTIGATION')}
              >
                <span>OPEN AI SUITE</span>
                <ArrowRight size={13} />
              </button>
            </div>

            <div className="panel-box-body">
              {aiRecommendations && aiRecommendations.length > 0 ? (
                <div className="leads-list">
                  {aiRecommendations.slice(0, 3).map((lead, idx) => (
                    <div key={lead.id || idx} className="lead-card">
                      <div className="lead-header">
                        <span className="lead-title">{lead.recommendationSummary || lead.title || 'Investigative Lead'}</span>
                        <span className="confidence-pill">
                          {Math.round((lead.confidenceScore || 0.85) * 100)}% CONFIDENCE
                        </span>
                      </div>
                      <div className="lead-desc">{lead.reasoning || lead.actionPlan || lead.description}</div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="ai-preview-card">
                  <div className="ai-preview-header">
                    <Sparkles size={16} className="text-amber" />
                    <span className="ai-preview-title">Automated Modus Operandi & Suspect Linkage</span>
                  </div>
                  <p className="ai-preview-text">
                    Cross-referenced with station historical cases and FIR IPC sections. High correlation found with prior warehouse theft patterns in Ukkadam industrial sector. Recommended next step: subpoena tower dump logs near incident timeframe.
                  </p>
                  <button 
                    className="run-analysis-btn"
                    onClick={() => onNavigateTab && onNavigateTab('AI_INVESTIGATION')}
                  >
                    RUN FULL NEURAL CASE ANALYSIS
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Mini Investigation Timeline & Recent Activity */}
        <div className="command-col-right">
          {/* Investigation Timeline Mini Feed */}
          <div className="panel-box timeline-mini-panel">
            <div className="panel-box-header">
              <div className="panel-title-wrap">
                <Clock size={17} className="text-amber" />
                <span className="panel-title">INVESTIGATION TIMELINE</span>
              </div>
              <button 
                className="panel-action-btn"
                onClick={() => onNavigateTab && onNavigateTab('TIMELINE')}
              >
                <span>FULL TIMELINE</span>
                <ChevronRight size={14} />
              </button>
            </div>

            <div className="panel-box-body">
              {recentTimeline.length === 0 ? (
                <div className="clean-state">
                  <Clock size={20} className="text-muted" />
                  <span>No events logged yet. Register FIR or collect evidence to generate timeline.</span>
                </div>
              ) : (
                <div className="mini-timeline-list">
                  {recentTimeline.map((evt, idx) => (
                    <div key={evt.id || idx} className="mini-timeline-item">
                      <div className="timeline-bullet">
                        <div className={`bullet-dot dot-${evt.badgeColor || 'blue'}`}></div>
                        {idx < recentTimeline.length - 1 && <div className="bullet-line"></div>}
                      </div>
                      <div className="timeline-item-content">
                        <div className="timeline-item-top">
                          <span className="timeline-title">{evt.title}</span>
                          <span className="timeline-time">
                            {evt.timestamp ? new Date(evt.timestamp).toLocaleDateString('en-IN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Logged'}
                          </span>
                        </div>
                        <div className="timeline-desc">{evt.description}</div>
                        {evt.actor && <div className="timeline-actor">By: {evt.actor}</div>}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Quick Action Station */}
          <div className="panel-box quick-actions-panel">
            <div className="panel-box-header">
              <span className="panel-title">INVESTIGATION QUICK ACTIONS</span>
            </div>
            <div className="quick-actions-grid">
              <button 
                className="qa-btn"
                onClick={onOpenUploadEvidence}
              >
                <FolderLock size={15} />
                <span>Upload Evidence File</span>
              </button>
              <button 
                className="qa-btn"
                onClick={onOpenAddWitness}
              >
                <Eye size={15} />
                <span>Record Witness Statement</span>
              </button>
              <button 
                className="qa-btn"
                onClick={() => onNavigateTab && onNavigateTab('FIR')}
              >
                <FileBadge size={15} />
                <span>View / Edit FIR</span>
              </button>
              <button 
                className="qa-btn"
                onClick={() => onNavigateTab && onNavigateTab('GENERATE_REPORT')}
              >
                <Activity size={15} />
                <span>Generate Case Report</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .command-center-container {
          display: flex;
          flex-direction: column;
          gap: 20px;
        }
        .metrics-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 14px;
        }
        .stat-card {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 6px;
          padding: 14px 16px;
          cursor: pointer;
          transition: all 0.15s ease-out;
        }
        .stat-card:hover {
          background: #181c26;
          border-color: #3b4256;
          transform: translateY(-2px);
        }
        .stat-card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
        }
        .stat-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 700;
          color: #8c93a8;
          letter-spacing: 0.08em;
        }
        .stat-icon {
          color: #8c93a8;
        }
        .icon-blue { color: #3b82f6; }
        .icon-purple { color: #a855f7; }
        .icon-red { color: #e63946; }
        .icon-cyan { color: #06b6d4; }
        .icon-green { color: #10b981; }

        .stat-val-row {
          display: flex;
          align-items: baseline;
          justify-content: space-between;
          gap: 8px;
          margin-bottom: 6px;
        }
        .stat-value {
          font-family: var(--font-mono, monospace);
          font-size: 1.6rem;
          font-weight: 800;
          color: #fff;
        }
        .stat-badge {
          font-size: 0.65rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
          letter-spacing: 0.04em;
        }
        .badge-info { background: rgba(59, 130, 246, 0.15); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.3); }
        .badge-neutral { background: rgba(100, 116, 139, 0.15); color: #94a3b8; border: 1px solid rgba(100, 116, 139, 0.3); }
        .badge-warn { background: rgba(249, 115, 22, 0.15); color: #fb923c; border: 1px solid rgba(249, 115, 22, 0.3); }
        .badge-cyan { background: rgba(6, 182, 212, 0.15); color: #22d3ee; border: 1px solid rgba(6, 182, 212, 0.3); }
        .badge-success { background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); }

        .stat-footer-text {
          font-size: 0.72rem;
          color: #64748b;
        }
        .verification-bar {
          height: 3px;
          background: #252b3b;
          border-radius: 2px;
          overflow: hidden;
          margin-top: 6px;
        }
        .verification-fill {
          height: 100%;
          background: #10b981;
          border-radius: 2px;
        }

        .command-grid-layout {
          display: grid;
          grid-template-columns: 1.2fr 0.8fr;
          gap: 20px;
        }
        @media (max-width: 1024px) {
          .command-grid-layout {
            grid-template-columns: 1fr;
          }
        }
        .command-col-left, .command-col-right {
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .panel-box {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 6px;
          overflow: hidden;
        }
        .panel-box-header {
          padding: 12px 16px;
          background: #181c26;
          border-bottom: 1px solid #232733;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .panel-title-wrap {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .panel-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.76rem;
          font-weight: 700;
          color: #f1f3f9;
          letter-spacing: 0.08em;
        }
        .text-crimson { color: #e63946; }
        .text-blue { color: #3b82f6; }
        .text-amber { color: #f59e0b; }
        .text-emerald { color: #10b981; }
        .text-muted { color: #64748b; }

        .gap-count-tag {
          font-family: var(--font-mono, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          background: rgba(230, 57, 70, 0.15);
          color: #e63946;
          border: 1px solid rgba(230, 57, 70, 0.3);
          padding: 1px 6px;
          border-radius: 3px;
        }
        .ai-active-pill {
          font-family: var(--font-mono, monospace);
          font-size: 0.62rem;
          font-weight: 700;
          background: rgba(59, 130, 246, 0.15);
          color: #60a5fa;
          border: 1px solid rgba(59, 130, 246, 0.3);
          padding: 1px 6px;
          border-radius: 3px;
        }
        .panel-action-btn {
          background: transparent;
          border: 1px solid #2a3142;
          color: #8c93a8;
          font-size: 0.72rem;
          font-weight: 600;
          padding: 4px 8px;
          border-radius: 4px;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 4px;
          transition: all 0.15s;
        }
        .panel-action-btn:hover {
          color: #fff;
          background: #202634;
          border-color: #3b455b;
        }
        .panel-box-body {
          padding: 14px 16px;
        }
        .clean-state {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 20px;
          font-size: 0.8rem;
          color: #8c93a8;
        }
        .missing-items-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .missing-item-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 10px 12px;
          background: #181c26;
          border: 1px solid #232733;
          border-radius: 5px;
          gap: 12px;
        }
        .missing-item-row.sev-critical {
          border-left: 3px solid #e63946;
        }
        .missing-item-row.sev-high {
          border-left: 3px solid #f97316;
        }
        .missing-item-info {
          display: flex;
          flex-direction: column;
          gap: 3px;
        }
        .missing-item-title-row {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .severity-tag {
          font-size: 0.62rem;
          font-weight: 700;
          padding: 1px 5px;
          border-radius: 2px;
        }
        .severity-tag.sev-critical {
          background: rgba(230, 57, 70, 0.2);
          color: #e63946;
        }
        .severity-tag.sev-high {
          background: rgba(249, 115, 22, 0.2);
          color: #f97316;
        }
        .missing-title {
          font-size: 0.82rem;
          font-weight: 600;
          color: #f1f3f9;
        }
        .missing-category {
          font-family: var(--font-mono, monospace);
          font-size: 0.68rem;
          color: #64748b;
        }
        .missing-desc {
          font-size: 0.74rem;
          color: #8c93a8;
          line-height: 1.3;
        }
        .quick-resolve-btn {
          background: #252a38;
          color: #60a5fa;
          border: 1px solid rgba(96, 165, 250, 0.3);
          padding: 5px 10px;
          border-radius: 4px;
          font-size: 0.7rem;
          font-weight: 700;
          cursor: pointer;
          white-space: nowrap;
          transition: all 0.15s;
        }
        .quick-resolve-btn:hover {
          background: rgba(96, 165, 250, 0.15);
          border-color: #60a5fa;
          color: #fff;
        }
        .leads-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .lead-card {
          padding: 10px 12px;
          background: #181c26;
          border: 1px solid #232733;
          border-left: 3px solid #3b82f6;
          border-radius: 5px;
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .lead-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
        }
        .lead-title {
          font-size: 0.82rem;
          font-weight: 600;
          color: #f1f3f9;
        }
        .confidence-pill {
          font-family: var(--font-mono, monospace);
          font-size: 0.62rem;
          font-weight: 700;
          color: #10b981;
          background: rgba(16, 185, 129, 0.12);
          padding: 1px 6px;
          border-radius: 3px;
        }
        .lead-desc {
          font-size: 0.74rem;
          color: #8c93a8;
          line-height: 1.35;
        }
        .ai-preview-card {
          padding: 14px;
          background: rgba(30, 41, 59, 0.3);
          border: 1px dashed #2a3346;
          border-radius: 6px;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }
        .ai-preview-header {
          display: flex;
          align-items: center;
          gap: 6px;
        }
        .ai-preview-title {
          font-size: 0.8rem;
          font-weight: 600;
          color: #e2e8f0;
        }
        .ai-preview-text {
          font-size: 0.74rem;
          color: #8c93a8;
          line-height: 1.4;
          margin: 0;
        }
        .run-analysis-btn {
          align-self: flex-start;
          background: #1f283d;
          color: #60a5fa;
          border: 1px solid #334155;
          padding: 6px 12px;
          border-radius: 4px;
          font-size: 0.72rem;
          font-weight: 700;
          cursor: pointer;
          margin-top: 4px;
          transition: all 0.15s;
        }
        .run-analysis-btn:hover {
          background: #2563eb;
          color: #fff;
        }

        .mini-timeline-list {
          display: flex;
          flex-direction: column;
        }
        .mini-timeline-item {
          display: flex;
          gap: 12px;
        }
        .timeline-bullet {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 14px;
        }
        .bullet-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          margin-top: 4px;
        }
        .dot-red { background: #e63946; }
        .dot-blue { background: #3b82f6; }
        .dot-amber { background: #f59e0b; }
        .dot-emerald { background: #10b981; }
        .dot-purple { background: #a855f7; }
        .dot-cyan { background: #06b6d4; }
        .bullet-line {
          width: 1px;
          background: #232733;
          flex-grow: 1;
          margin: 4px 0;
        }
        .timeline-item-content {
          padding-bottom: 14px;
          flex-grow: 1;
        }
        .timeline-item-top {
          display: flex;
          justify-content: space-between;
          align-items: baseline;
          gap: 8px;
        }
        .timeline-title {
          font-size: 0.78rem;
          font-weight: 600;
          color: #f1f3f9;
        }
        .timeline-time {
          font-family: var(--font-mono, monospace);
          font-size: 0.68rem;
          color: #64748b;
          white-space: nowrap;
        }
        .timeline-desc {
          font-size: 0.72rem;
          color: #8c93a8;
          margin-top: 2px;
          line-height: 1.3;
        }
        .timeline-actor {
          font-size: 0.68rem;
          color: #64748b;
          margin-top: 2px;
        }

        .quick-actions-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 8px;
          padding: 14px 16px;
        }
        .qa-btn {
          display: flex;
          align-items: center;
          gap: 8px;
          background: #181c26;
          border: 1px solid #232733;
          color: #d1d5db;
          padding: 10px 12px;
          border-radius: 5px;
          font-size: 0.74rem;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.15s;
        }
        .qa-btn:hover {
          background: #1f2434;
          border-color: #3b455b;
          color: #fff;
        }
      `}</style>
    </div>
  );
}
