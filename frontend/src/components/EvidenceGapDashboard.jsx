import React, { useState } from 'react';
import { 
  AlertTriangle, 
  CheckCircle, 
  RotateCw, 
  ShieldAlert, 
  Info,
  Filter,
  Check,
  RefreshCw,
  FileQuestion,
  UserCheck
} from 'lucide-react';
import { analyzeEvidenceGaps, updateEvidenceGapStatus } from '../services/api';

const SEVERITY_CONFIG = {
  CRITICAL: {
    label: 'CRITICAL',
    badgeClass: 'eg-badge-sev-CRITICAL',
    cardClass: 'sev-CRITICAL'
  },
  HIGH: {
    label: 'HIGH',
    badgeClass: 'eg-badge-sev-HIGH',
    cardClass: 'sev-HIGH'
  },
  MEDIUM: {
    label: 'MEDIUM',
    badgeClass: 'eg-badge-sev-MEDIUM',
    cardClass: 'sev-MEDIUM'
  },
  LOW: {
    label: 'LOW',
    badgeClass: 'eg-badge-sev-LOW',
    cardClass: 'sev-LOW'
  }
};

const STATUS_CONFIG = {
  OPEN: {
    label: 'OPEN',
    badgeClass: 'eg-badge-stat-OPEN'
  },
  ACKNOWLEDGED: {
    label: 'ACKNOWLEDGED',
    badgeClass: 'eg-badge-stat-ACKNOWLEDGED'
  },
  RESOLVED: {
    label: 'RESOLVED',
    badgeClass: 'eg-badge-stat-RESOLVED'
  },
  NOT_APPLICABLE: {
    label: 'NOT APPLICABLE',
    badgeClass: 'eg-badge-stat-NOT_APPLICABLE'
  }
};

export default function EvidenceGapDashboard({
  caseId,
  gaps = [],
  loading = false,
  onGapsUpdated
}) {
  const [analyzing, setAnalyzing] = useState(false);
  const [updatingGapId, setUpdatingGapId] = useState(null);
  const [actionError, setActionError] = useState('');
  const [actionSuccess, setActionSuccess] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [filterSeverity, setFilterSeverity] = useState('ALL');

  // Trigger automated analysis API
  const handleRunAnalysis = async (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    try {
      setAnalyzing(true);
      setActionError('');
      setActionSuccess('');
      const updatedGaps = await analyzeEvidenceGaps(caseId);
      setActionSuccess('Evidence gap analysis updated successfully.');
      if (onGapsUpdated) {
        onGapsUpdated(updatedGaps);
      }
      setTimeout(() => setActionSuccess(''), 4000);
    } catch (err) {
      setActionError(err.message || 'Failed to analyze evidence gaps');
    } finally {
      setAnalyzing(false);
    }
  };

  // Update single gap status API
  const handleUpdateStatus = async (e, gapId, newStatus) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    try {
      setUpdatingGapId(gapId);
      setActionError('');
      const updatedGap = await updateEvidenceGapStatus(caseId, gapId, newStatus);
      if (onGapsUpdated) {
        onGapsUpdated(updatedGap || { id: gapId, status: newStatus });
      }
    } catch (err) {
      setActionError(err.message || 'Failed to update gap status');
    } finally {
      setUpdatingGapId(null);
    }
  };

  // Filtered gaps list
  const filteredGaps = gaps.filter(gap => {
    if (filterStatus !== 'ALL' && gap.status !== filterStatus) return false;
    if (filterSeverity !== 'ALL' && gap.severity !== filterSeverity) return false;
    return true;
  });

  // Summary statistics
  const totalCount = gaps.length;
  const openCount = gaps.filter(g => g.status === 'OPEN').length;
  const ackCount = gaps.filter(g => g.status === 'ACKNOWLEDGED').length;
  const resolvedCount = gaps.filter(g => g.status === 'RESOLVED').length;
  const naCount = gaps.filter(g => g.status === 'NOT_APPLICABLE').length;

  return (
    <div className="eg-container">
      {/* Page Header */}
      <div className="eg-header">
        <div className="eg-header-title-group">
          <div className="eg-icon-badge">
            <ShieldAlert size={22} />
          </div>
          <div className="eg-title-text">
            <h2>EVIDENCE GAP ANALYSIS</h2>
            <p>Identifies missing investigation artifacts and records to support case completeness</p>
          </div>
        </div>

        {/* Primary Action Button */}
        <div>
          <button
            type="button"
            onClick={(e) => handleRunAnalysis(e)}
            disabled={analyzing || loading}
            className="eg-btn-primary"
            title="Run rule-based evidence gap detection engine"
          >
            <RotateCw size={15} className={analyzing ? 'spin-icon' : ''} />
            <span>{analyzing ? 'ANALYZING Completeness...' : 'RUN EVIDENCE GAP ANALYSIS'}</span>
          </button>
        </div>
      </div>

      {/* Alert Notifications */}
      {actionSuccess && (
        <div className="eg-alert eg-alert-success">
          <CheckCircle size={16} />
          <span>{actionSuccess}</span>
        </div>
      )}

      {actionError && (
        <div className="eg-alert eg-alert-error">
          <AlertTriangle size={16} />
          <span>{actionError}</span>
        </div>
      )}

      {/* Summary KPI Cards Grid */}
      <div className="eg-summary-grid">
        <div className="eg-kpi-card total">
          <span className="eg-kpi-label">TOTAL GAPS</span>
          <span className="eg-kpi-value">{totalCount}</span>
        </div>
        <div className="eg-kpi-card open">
          <span className="eg-kpi-label">OPEN</span>
          <span className="eg-kpi-value">{openCount}</span>
        </div>
        <div className="eg-kpi-card ack">
          <span className="eg-kpi-label">ACKNOWLEDGED</span>
          <span className="eg-kpi-value">{ackCount}</span>
        </div>
        <div className="eg-kpi-card resolved">
          <span className="eg-kpi-label">RESOLVED</span>
          <span className="eg-kpi-value">{resolvedCount}</span>
        </div>
        <div className="eg-kpi-card na">
          <span className="eg-kpi-label">NOT APPLICABLE</span>
          <span className="eg-kpi-value">{naCount}</span>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="eg-toolbar">
        <div className="eg-filter-group">
          <div className="eg-filter-label">
            <Filter size={14} />
            <span>FILTER BY:</span>
          </div>

          {/* Status Select Dropdown */}
          <div className="eg-select-item">
            <label htmlFor="eg-status-filter">Status:</label>
            <select
              id="eg-status-filter"
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="eg-select"
            >
              <option value="ALL">All Statuses</option>
              <option value="OPEN">Open Only</option>
              <option value="ACKNOWLEDGED">Acknowledged Only</option>
              <option value="RESOLVED">Resolved Only</option>
              <option value="NOT_APPLICABLE">Not Applicable</option>
            </select>
          </div>

          {/* Severity Select Dropdown */}
          <div className="eg-select-item">
            <label htmlFor="eg-severity-filter">Severity:</label>
            <select
              id="eg-severity-filter"
              value={filterSeverity}
              onChange={(e) => setFilterSeverity(e.target.value)}
              className="eg-select"
            >
              <option value="ALL">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>

        <div className="eg-count-badge">
          Showing {filteredGaps.length} of {totalCount} items
        </div>
      </div>

      {/* Loading State */}
      {loading && filteredGaps.length === 0 ? (
        <div className="eg-empty-state">
          <RotateCw size={32} className="spin-icon text-red-500" style={{ color: 'var(--bright-crime-red)' }} />
          <div className="eg-empty-title">Loading Evidence Gaps...</div>
          <div className="eg-empty-desc">Fetching investigation gap analysis from backend decision engine.</div>
        </div>
      ) : filteredGaps.length === 0 ? (
        /* Empty State */
        <div className="eg-empty-state">
          <CheckCircle size={36} style={{ color: '#10B981', opacity: 0.8 }} />
          <div className="eg-empty-title">No evidence gaps found for current filter.</div>
          <div className="eg-empty-desc">
            All required evidence categories meet completeness requirements or match your filter parameters.
            Click &quot;RUN EVIDENCE GAP ANALYSIS&quot; to re-evaluate.
          </div>
        </div>
      ) : (
        /* Gap Cards List */
        <div className="eg-cards-list">
          {filteredGaps.map((gap) => {
            const sevInfo = SEVERITY_CONFIG[gap.severity] || SEVERITY_CONFIG.MEDIUM;
            const statInfo = STATUS_CONFIG[gap.status] || STATUS_CONFIG.OPEN;
            const isUpdating = updatingGapId === gap.id;

            return (
              <div
                key={gap.id}
                className={`eg-gap-card ${sevInfo.cardClass}`}
              >
                <div className="eg-gap-main">
                  {/* Gap Details */}
                  <div className="eg-gap-details">
                    <h3 className="eg-gap-title">{gap.title}</h3>

                    {/* Badges Row with Spacing & Distinct Elements */}
                    <div className="eg-badge-row">
                      {/* Category Badge */}
                      <span className="eg-badge eg-badge-category">
                        {gap.category}
                      </span>

                      {/* Severity Badge */}
                      <span className={`eg-badge ${sevInfo.badgeClass}`}>
                        <span className="eg-badge-dot" />
                        {sevInfo.label}
                      </span>

                      {/* Status Badge */}
                      <span className={`eg-badge ${statInfo.badgeClass}`}>
                        {statInfo.label}
                      </span>
                    </div>

                    {/* Description */}
                    {gap.description && (
                      <p className="eg-gap-description">{gap.description}</p>
                    )}

                    {/* Finding / Reason */}
                    {gap.reason && (
                      <div className="eg-gap-reason">
                        <Info size={14} style={{ color: '#F59E0B', flexShrink: 0, marginTop: 2 }} />
                        <span><strong>Finding:</strong> {gap.reason}</span>
                      </div>
                    )}

                    {/* Resolved Metadata */}
                    {gap.resolvedAt && (
                      <div className="eg-gap-meta">
                        <CheckCircle size={13} />
                        <span>
                          Resolved on {new Date(gap.resolvedAt).toLocaleDateString()}{' '}
                          {gap.resolvedByName && `by ${gap.resolvedByName}`}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Action Buttons */}
                  <div className="eg-gap-actions">
                    {gap.status === 'OPEN' && (
                      <button
                        type="button"
                        onClick={(e) => handleUpdateStatus(e, gap.id, 'ACKNOWLEDGED')}
                        disabled={isUpdating}
                        className="eg-btn-action eg-btn-ack"
                        title="Acknowledge this potential gap"
                      >
                        {isUpdating ? 'Updating...' : 'Acknowledge'}
                      </button>
                    )}

                    {(gap.status === 'OPEN' || gap.status === 'ACKNOWLEDGED') && (
                      <>
                        <button
                          type="button"
                          onClick={(e) => handleUpdateStatus(e, gap.id, 'RESOLVED')}
                          disabled={isUpdating}
                          className="eg-btn-action eg-btn-resolve"
                          title="Mark gap as resolved"
                        >
                          {isUpdating ? 'Updating...' : 'Resolve'}
                        </button>

                        <button
                          type="button"
                          onClick={(e) => handleUpdateStatus(e, gap.id, 'NOT_APPLICABLE')}
                          disabled={isUpdating}
                          className="eg-btn-action eg-btn-na"
                          title="Mark gap as not applicable to this case"
                        >
                          {isUpdating ? 'Updating...' : 'Not Applicable'}
                        </button>
                      </>
                    )}

                    {(gap.status === 'RESOLVED' || gap.status === 'NOT_APPLICABLE') && (
                      <button
                        type="button"
                        onClick={(e) => handleUpdateStatus(e, gap.id, 'OPEN')}
                        disabled={isUpdating}
                        className="eg-btn-action eg-btn-reopen"
                        title="Reopen investigation gap"
                      >
                        {isUpdating ? 'Updating...' : 'Reopen'}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Advisory Footer */}
      <div className="eg-footer">
        <span>Deterministic Rule Engine • Phase 7 Investigation Completeness</span>
        <span>Investigator Discretion Applies</span>
      </div>
    </div>
  );
}
