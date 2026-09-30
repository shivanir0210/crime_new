import React from 'react';
import { 
  ShieldCheck, 
  AlertCircle, 
  CheckCircle2, 
  Clock, 
  Activity,
  Info,
  Layers
} from 'lucide-react';

const STATE_CONFIG = {
  CASE_CREATED: {
    badgeBg: 'rgba(39, 39, 42, 0.8)',
    border: 'rgba(82, 82, 91, 0.6)',
    color: '#D4D4D8',
    dot: '#A1A1AA',
    description: 'Case file opened. Initial registration pending first information report.'
  },
  FIR_REGISTERED: {
    badgeBg: 'rgba(217, 119, 6, 0.15)',
    border: 'rgba(217, 119, 6, 0.5)',
    color: '#FBBF24',
    dot: '#F59E0B',
    description: 'First Information Report (FIR) registered. Establishing complainant and incident baseline.'
  },
  VICTIM_IDENTIFIED: {
    badgeBg: 'rgba(59, 130, 246, 0.15)',
    border: 'rgba(59, 130, 246, 0.5)',
    color: '#93C5FD',
    dot: '#3B82F6',
    description: 'Victim/complainant recorded. Investigating harm, statement, and potential witnesses.'
  },
  SUSPECT_IDENTIFIED: {
    badgeBg: 'rgba(168, 85, 247, 0.15)',
    border: 'rgba(168, 85, 247, 0.5)',
    color: '#C084FC',
    dot: '#A855F7',
    description: 'Suspect or person of interest logged in case database.'
  },
  EVIDENCE_COLLECTED: {
    badgeBg: 'rgba(99, 102, 241, 0.15)',
    border: 'rgba(99, 102, 241, 0.5)',
    color: '#A5B4FC',
    dot: '#6366F1',
    description: 'Physical/digital evidence collected and securely cataloged.'
  },
  EVIDENCE_UNDER_REVIEW: {
    badgeBg: 'rgba(249, 115, 22, 0.15)',
    border: 'rgba(249, 115, 22, 0.5)',
    color: '#FB923C',
    dot: '#F97316',
    description: 'Evidence artifacts currently undergoing forensic / chain-of-custody review.'
  },
  EVIDENCE_VERIFIED: {
    badgeBg: 'rgba(20, 184, 166, 0.15)',
    border: 'rgba(20, 184, 166, 0.5)',
    color: '#2DD4BF',
    dot: '#14B8A6',
    description: 'Key evidence items verified with tamper-evident digital hashes.'
  },
  INVESTIGATION_READY: {
    badgeBg: 'rgba(16, 185, 129, 0.15)',
    border: 'rgba(16, 185, 129, 0.6)',
    color: '#6EE7B7',
    dot: '#10B981',
    description: 'Minimum investigation requirements satisfied. Case ready for AI-assisted analytical workflows.'
  },
  PENDING_INVESTIGATOR_REVIEW: {
    badgeBg: 'rgba(193, 18, 31, 0.18)',
    border: 'rgba(193, 18, 31, 0.6)',
    color: '#FF6B6B',
    dot: '#C1121F',
    description: 'Investigator review required to resolve missing critical case items.'
  }
};

export default function InvestigationStateWidget({ caseState, loading }) {
  if (loading && !caseState) {
    return (
      <div className="isw-container">
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <div style={{ width: 34, height: 34, borderRadius: 8, background: '#2A2A2D' }} />
          <div style={{ height: 20, width: 220, background: '#2A2A2D', borderRadius: 4 }} />
        </div>
        <div style={{ height: 40, background: '#17171A', borderRadius: 6, marginBottom: 16 }} />
        <div style={{ height: 80, background: '#17171A', borderRadius: 6 }} />
      </div>
    );
  }

  if (!caseState) {
    return null;
  }

  const {
    currentState = 'CASE_CREATED',
    stateLabel = 'Case Created',
    progressPercentage = 0,
    completedAreas = [],
    missingAreas = [],
    lastEvaluatedAt
  } = caseState;

  const currentConfig = STATE_CONFIG[currentState] || STATE_CONFIG.CASE_CREATED;

  return (
    <div className="isw-container">
      {/* HEADER ROW */}
      <div className="isw-header">
        <div className="isw-header-left">
          {/* Small compact icon container (approx 34px) */}
          <div className="isw-icon-box">
            <Activity size={20} />
          </div>
          
          <div className="isw-title-area">
            <div className="isw-title-row">
              <h3 className="isw-title">INVESTIGATION STATE &amp; COMPLETENESS</h3>
              <span className="isw-tag-rule">RULE-BASED</span>
            </div>
            <p className="isw-subtitle">Deterministic evaluation of active case database inputs</p>
          </div>
        </div>

        {/* Status Badge */}
        <div 
          className="isw-badge"
          style={{
            backgroundColor: currentConfig.badgeBg,
            borderColor: currentConfig.border,
            color: currentConfig.color
          }}
        >
          <span 
            className="isw-badge-dot" 
            style={{ backgroundColor: currentConfig.dot }} 
          />
          <span>{stateLabel}</span>
        </div>
      </div>

      {/* STATUS DESCRIPTION MESSAGE */}
      <div className="isw-description-box">
        <Info size={16} className="isw-desc-icon" />
        <span>{currentConfig.description}</span>
      </div>

      {/* COMPLETENESS SUMMARY CARDS */}
      <div className="isw-summary-grid">
        {/* Card 1: Investigation Completeness */}
        <div className="isw-summary-card">
          <div className="isw-card-header">
            <span className="isw-card-label">INVESTIGATION COMPLETENESS</span>
            <span className="isw-card-value">{progressPercentage}%</span>
          </div>

          {/* Progress Bar */}
          <div className="isw-progress-track">
            <div 
              className={`isw-progress-fill ${progressPercentage >= 100 ? 'complete' : ''}`}
              style={{ width: `${Math.min(100, Math.max(0, progressPercentage))}%` }}
            />
          </div>
        </div>

        {/* Card 2: Missing / Pending Areas */}
        <div className="isw-summary-card">
          <div className="isw-card-header">
            <span className="isw-card-label">MISSING / PENDING AREAS</span>
            <span className="isw-card-value" style={{ color: missingAreas.length > 0 ? '#F59E0B' : '#6EE7B7' }}>
              {missingAreas.length}
            </span>
          </div>
          <div style={{ fontSize: '0.73rem', color: 'var(--text-secondary)' }}>
            {missingAreas.length > 0 ? 'Requires investigator resolution' : 'All required categories populated'}
          </div>
        </div>
      </div>

      {/* CHECKLISTS: COMPLETED VS MISSING AREAS */}
      <div className="isw-checklist-grid">
        {/* Completed Areas Box */}
        <div className="isw-list-box completed-box">
          <div className="isw-list-title">
            <CheckCircle2 size={15} />
            <span>COMPLETED AREAS ({completedAreas.length})</span>
          </div>

          <div className="isw-items-container">
            {completedAreas.length > 0 ? (
              completedAreas.map((area, idx) => (
                <div key={idx} className="isw-item-row completed-row">
                  <span style={{ color: '#10B981', fontWeight: 'bold' }}>✓</span>
                  <span>{area}</span>
                </div>
              ))
            ) : (
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', italic: 'true' }}>
                No completed areas recorded yet
              </div>
            )}
          </div>
        </div>

        {/* Missing / Pending Areas Box */}
        <div className="isw-list-box missing-box">
          <div className="isw-list-title">
            <AlertCircle size={15} />
            <span>MISSING / PENDING AREAS ({missingAreas.length})</span>
          </div>

          <div className="isw-items-container">
            {missingAreas.length > 0 ? (
              missingAreas.map((area, idx) => (
                <div key={idx} className="isw-item-row missing-row">
                  <div className="missing-header">
                    <span style={{ color: '#F59E0B', fontWeight: 'bold' }}>⚠</span>
                    <span>{area}</span>
                  </div>
                  <div className="missing-status">Status: Missing or pending verification</div>
                </div>
              ))
            ) : (
              <div className="isw-item-row completed-row" style={{ color: '#6EE7B7' }}>
                <ShieldCheck size={14} />
                <span>All core investigation areas satisfied</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* FOOTER METADATA */}
      <div className="isw-footer">
        <div className="isw-footer-left">
          <Clock size={13} />
          <span>
            Last Evaluated:{' '}
            {lastEvaluatedAt 
              ? new Date(lastEvaluatedAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'medium' })
              : 'Real-time calculation'
            }
          </span>
        </div>
        <span>Decision Support System</span>
      </div>
    </div>
  );
}
