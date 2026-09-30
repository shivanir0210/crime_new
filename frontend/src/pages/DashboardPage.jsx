import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import Panel from '../components/Panel';
import StatCard from '../components/StatCard';
import InvestigationBoard from '../components/InvestigationBoard';
import { 
  getDashboardStats, 
  getCases, 
  getCaseById, 
  getCaseFIR, 
  getCaseVictims, 
  getCaseSuspects, 
  getCaseWitnesses, 
  getCaseEvidence,
  getPendingOfficerRegistrations
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import { 
  Briefcase, 
  AlertTriangle, 
  FileWarning, 
  Clock, 
  FolderSearch, 
  PlusCircle,
  ExternalLink,
  ShieldCheck,
  Activity,
  Layers,
  FileCheck2,
  AlertCircle,
  CheckCircle2,
  ChevronRight,
  TrendingUp,
  History,
  ShieldAlert,
  User,
  Calendar,
  UserCheck
} from 'lucide-react';

export default function DashboardPage() {
  const navigate = useNavigate();
  const { officer } = useAuth();

  const [stats, setStats] = useState({
    activeCases: 0,
    highPriorityCases: 0,
    criticalCases: 0,
    pendingReview: 0,
    onHoldCases: 0,
    totalCases: 0,
    stationName: 'COIMBATORE CENTRAL POLICE STATION',
    stationCode: 'TN-POLICE-CBE',
    underInvestigationCases: 0,
    filedCases: 0,
    closedCases: 0,
    totalEvidence: 0,
    verifiedEvidence: 0,
    pendingEvidence: 0,
    evidenceGaps: 0,
    recentActivities: [],
    attentionCases: []
  });

  const [recentCases, setRecentCases] = useState([]);
  const [pendingRegistrations, setPendingRegistrations] = useState([]);
  const [loadingRegistrations, setLoadingRegistrations] = useState(false);
  const [selectedCase, setSelectedCase] = useState(null);
  const [selectedCaseEntities, setSelectedCaseEntities] = useState({
    fir: null,
    victims: [],
    suspects: [],
    witnesses: [],
    evidence: []
  });
  const [loading, setLoading] = useState(true);
  const [boardLoading, setBoardLoading] = useState(false);

  // 1. Initial Dashboard Load
  useEffect(() => {
    let isMounted = true;

    async function loadDashboardData() {
      try {
        setLoading(true);
        setLoadingRegistrations(true);
        const [statsData, casesData, regData] = await Promise.all([
          getDashboardStats().catch(() => null),
          getCases({ page: 0, size: 8, sort: 'updatedAt,desc' }).catch(() => null),
          getPendingOfficerRegistrations().catch(() => [])
        ]);

        if (isMounted) {
          if (statsData) {
            setStats(prev => ({ ...prev, ...statsData }));
          }
          if (casesData && casesData.content) {
            setRecentCases(casesData.content);
            if (casesData.content.length > 0) {
              const defaultCase = casesData.content[0];
              setSelectedCase(defaultCase);
              loadCaseEntityData(defaultCase.id);
            }
          }
          if (Array.isArray(regData)) {
            setPendingRegistrations(regData.filter(r => r.status === 'PENDING_STATION_APPROVAL' || r.status === 'PENDING_VERIFICATION'));
          }
        }
      } catch (err) {
        console.error('Failed to load dashboard metrics:', err);
      } finally {
        if (isMounted) {
          setLoading(false);
          setLoadingRegistrations(false);
        }
      }
    }

    loadDashboardData();
    return () => { isMounted = false; };
  }, []);

  // 2. Load entities for the selected case
  async function loadCaseEntityData(caseId) {
    if (!caseId) return;
    try {
      setBoardLoading(true);
      const [firRes, victimsRes, suspectsRes, witnessesRes, evidenceRes] = await Promise.all([
        getCaseFIR(caseId).catch(() => null),
        getCaseVictims(caseId).catch(() => []),
        getCaseSuspects(caseId).catch(() => []),
        getCaseWitnesses(caseId).catch(() => []),
        getCaseEvidence(caseId).catch(() => [])
      ]);

      setSelectedCaseEntities({
        fir: firRes,
        victims: Array.isArray(victimsRes) ? victimsRes : [],
        suspects: Array.isArray(suspectsRes) ? suspectsRes : [],
        witnesses: Array.isArray(witnessesRes) ? witnessesRes : [],
        evidence: Array.isArray(evidenceRes) ? evidenceRes : []
      });
    } catch (err) {
      console.warn('Failed to load entity data for selected case graph:', err);
    } finally {
      setBoardLoading(false);
    }
  }

  const handleSelectCase = (caseItem) => {
    setSelectedCase(caseItem);
    loadCaseEntityData(caseItem.id);
  };

  const stationDisplayName = stats.stationName || officer?.station?.stationName || 'COIMBATORE CENTRAL POLICE STATION';

  // Compute workload numbers
  const totalWorkload = (stats.underInvestigationCases || 0) + (stats.pendingReview || 0) + (stats.filedCases || 0) + (stats.closedCases || 0) || (stats.totalCases || 1);
  const underInvPct = Math.round(((stats.underInvestigationCases || 0) / totalWorkload) * 100) || 0;
  const pendingRevPct = Math.round(((stats.pendingReview || 0) / totalWorkload) * 100) || 0;
  const filedPct = Math.round(((stats.filedCases || 0) / totalWorkload) * 100) || 0;
  const closedPct = Math.round(((stats.closedCases || 0) / totalWorkload) * 100) || 0;

  // Format activity action names
  const formatAction = (act) => {
    if (!act) return 'INVESTIGATION UPDATE';
    return act.replace(/_/g, ' ').toUpperCase();
  };

  // Format relative timestamp
  const formatTimeAgo = (dateStr) => {
    if (!dateStr) return 'Recently';
    const d = new Date(dateStr);
    const diffMs = Date.now() - d.getTime();
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  const renderPriorityBadge = (priority) => {
    const p = (priority || 'MEDIUM').toUpperCase();
    const colorMap = {
      CRITICAL: { bg: 'rgba(193, 18, 31, 0.2)', border: 'var(--bright-crime-red)', color: '#FF6B6B' },
      HIGH: { bg: 'rgba(239, 68, 68, 0.15)', border: '#EF4444', color: '#F87171' },
      MEDIUM: { bg: 'rgba(245, 158, 11, 0.15)', border: '#F59E0B', color: '#FBBF24' },
      LOW: { bg: 'rgba(16, 185, 129, 0.15)', border: '#10B981', color: '#34D399' }
    };
    const style = colorMap[p] || colorMap.MEDIUM;
    return (
      <span style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
        padding: '2px 7px',
        borderRadius: '3px',
        fontFamily: 'var(--font-subheading)',
        fontSize: '0.65rem',
        fontWeight: 700,
        letterSpacing: '0.06em',
        backgroundColor: style.bg,
        border: `1px solid ${style.border}`,
        color: style.color
      }}>
        <span style={{ width: '4px', height: '4px', borderRadius: '50%', backgroundColor: style.color }}></span>
        {p}
      </span>
    );
  };

  const renderStatusBadge = (status) => {
    const s = (status || 'UNDER_INVESTIGATION').toUpperCase();
    const colorMap = {
      UNDER_INVESTIGATION: { bg: 'rgba(193, 18, 31, 0.15)', border: 'var(--bright-crime-red)', color: '#FF6B6B' },
      PENDING_REVIEW: { bg: 'rgba(245, 158, 11, 0.15)', border: '#F59E0B', color: '#FBBF24' },
      FILED: { bg: 'rgba(56, 189, 248, 0.15)', border: '#38BDF8', color: '#38BDF8' },
      CLOSED: { bg: 'rgba(16, 185, 129, 0.15)', border: '#10B981', color: '#34D399' },
      ON_HOLD: { bg: 'rgba(167, 167, 170, 0.15)', border: '#71717A', color: '#A7A7AA' }
    };
    const style = colorMap[s] || { bg: 'rgba(56, 189, 248, 0.15)', border: '#38BDF8', color: '#38BDF8' };
    return (
      <span style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
        padding: '2px 7px',
        borderRadius: '3px',
        fontFamily: 'var(--font-subheading)',
        fontSize: '0.65rem',
        fontWeight: 700,
        letterSpacing: '0.06em',
        backgroundColor: style.bg,
        border: `1px solid ${style.border}`,
        color: style.color
      }}>
        {s.replace(/_/g, ' ')}
      </span>
    );
  };

  return (
    <div className="cs-dashboard-layout">
      <TopBar />
      <div className="cs-main-wrapper">
        <Sidebar />
        
        <main className="cs-content-area forensic-grid">
          {/* Dashboard Header */}
          <div className="page-header flex-between">
            <div>
              <h1 className="page-title">INVESTIGATION COMMAND CENTER</h1>
              <p className="page-subtitle">
                {stationDisplayName.toUpperCase()} · AUTHENTICATED INVESTIGATOR TERMINAL
              </p>
            </div>
            <div className="page-header-actions">
              <button 
                className="btn-create-case"
                onClick={() => navigate('/cases/new')}
                title="Register New Investigation Case"
              >
                <PlusCircle size={15} />
                <span>NEW CASE</span>
              </button>
            </div>
          </div>

          {/* 1. Station-Wide Summary Stat Cards */}
          <div className="stats-grid">
            <StatCard 
              title="ACTIVE CASES" 
              value={loading ? '--' : String(stats.activeCases || 0).padStart(2, '0')} 
              icon={Briefcase} 
              subtitle="Total Active Station Cases" 
            />
            <StatCard 
              title="HIGH PRIORITY" 
              value={loading ? '--' : String(stats.highPriorityCases || 0).padStart(2, '0')} 
              icon={AlertTriangle} 
              isCritical={true} 
              subtitle="High-Priority Investigations" 
            />
            <StatCard 
              title="CRITICAL INCIDENTS" 
              value={loading ? '--' : String(stats.criticalCases || 0).padStart(2, '0')} 
              icon={FileWarning} 
              isCritical={true} 
              subtitle="Critical-Priority Emergencies" 
            />
            <StatCard 
              title="PENDING REVIEW" 
              value={loading ? '--' : String(stats.pendingReview || 0).padStart(2, '0')} 
              icon={Clock} 
              subtitle="Cases Awaiting Station Sign-Off" 
            />
          </div>

          {/* 1.5. Station Head Pending Officer Registrations Section */}
          {(officer?.role === 'STATION_HEAD' || officer?.role === 'SUPER_ADMIN' || pendingRegistrations.length > 0) && (
            <Panel 
              title="PENDING OFFICER REGISTRATIONS" 
              subtitle={`Officer onboarding applications awaiting Station Head authorization — ${stationDisplayName}`}
              icon={UserCheck}
              accent={true}
            >
              {loadingRegistrations ? (
                <div className="panel-loading">LOADING PENDING OFFICER REGISTRATIONS...</div>
              ) : pendingRegistrations.length === 0 ? (
                <div className="empty-cases-banner">
                  <div className="empty-title">NO PENDING REGISTRATIONS</div>
                  <div className="empty-desc">There are currently no officer onboarding applications awaiting approval for this station.</div>
                </div>
              ) : (
                <div className="cases-table-container">
                  <table className="station-cases-table">
                    <thead>
                      <tr>
                        <th>OFFICER NAME</th>
                        <th>POLICE ID</th>
                        <th>STATION</th>
                        <th>ROLE</th>
                        <th>REGISTRATION DATE</th>
                        <th>OCR STATUS</th>
                        <th>CURRENT STATUS</th>
                        <th style={{ textAlign: 'right' }}>ACTION</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingRegistrations.map((reg) => (
                        <tr 
                          key={reg.id} 
                          className="case-row"
                          onClick={() => navigate(`/station-head/registrations/${reg.id}`)}
                          title="Click to review officer credentials and authorize account"
                        >
                          <td className="case-title-cell">
                            <span className="case-title-text" style={{ fontWeight: 600, color: '#f8fafc' }}>{reg.fullName}</span>
                          </td>
                          <td className="case-id-cell">
                            <span className="case-num-tag font-mono">{reg.policeId}</span>
                          </td>
                          <td className="case-meta-cell">
                            {reg.stationName || stationDisplayName}
                          </td>
                          <td>
                            <span className="role-badge-dash">{reg.role}</span>
                          </td>
                          <td className="case-meta-cell">
                            {reg.createdAt ? new Date(reg.createdAt).toLocaleDateString() : '—'}
                          </td>
                          <td>
                            <span className={`ocr-badge-dash ${reg.ocrStatus === 'PASSED' ? 'pass' : 'review'}`}>
                              {reg.ocrStatus === 'PASSED' ? '✓ PASSED' : '⚠ REVIEW'}
                            </span>
                          </td>
                          <td>
                            <span className={`status-badge-dash status-${(reg.status || 'PENDING').toLowerCase()}`}>
                              {reg.status}
                            </span>
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button 
                              className="btn-review-action"
                              onClick={(e) => {
                                e.stopPropagation();
                                navigate(`/station-head/registrations/${reg.id}`);
                              }}
                              title="Review application"
                            >
                              <span>REVIEW</span>
                              <ExternalLink size={12} />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <div className="table-footer-bar">
                    <span className="table-count-text">
                      Showing {pendingRegistrations.length} registration(s) requiring Station Head action
                    </span>
                    <button 
                      className="btn-view-all-table"
                      onClick={() => navigate('/officer-approvals')}
                    >
                      <span>VIEW ALL OFFICER REGISTRATIONS</span>
                      <ExternalLink size={13} />
                    </button>
                  </div>
                </div>
              )}
            </Panel>
          )}

          {/* 2. Recent Station Investigations Table */}
          <Panel 
            title="RECENT STATION INVESTIGATIONS" 
            subtitle="Station-Isolated Case Files — Click row to inspect graph, or click Case ID to open full dossier"
            icon={FolderSearch}
            accent={true}
          >
            {loading ? (
              <div className="panel-loading">LOADING RECENT STATION INVESTIGATIONS...</div>
            ) : recentCases.length === 0 ? (
              <div className="empty-cases-banner">
                <div className="empty-title">NO ACTIVE CASES REGISTERED</div>
                <div className="empty-desc">No investigation dossiers registered under this police precinct yet.</div>
                <button className="btn-create-subtle" onClick={() => navigate('/cases/new')}>
                  <PlusCircle size={14} />
                  <span>REGISTER FIRST CASE</span>
                </button>
              </div>
            ) : (
              <div className="cases-table-container">
                <table className="station-cases-table">
                  <thead>
                    <tr>
                      <th>CASE ID</th>
                      <th>CASE TITLE</th>
                      <th>PRIORITY</th>
                      <th>STATUS</th>
                      <th>FILED DATE</th>
                      <th>ASSIGNED OFFICER</th>
                      <th>LAST UPDATED</th>
                      <th style={{ textAlign: 'right' }}>ACTION</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentCases.map((c) => {
                      const isSelected = selectedCase?.id === c.id;
                      return (
                        <tr 
                          key={c.id} 
                          className={`case-row ${isSelected ? 'selected-row' : ''}`}
                          onClick={() => handleSelectCase(c)}
                          title="Click to view Entity Relationship Matrix on Link Board"
                        >
                          <td className="case-id-cell">
                            <span className="case-num-tag">{c.caseNumber || c.id.slice(0, 8)}</span>
                          </td>
                          <td className="case-title-cell">
                            <span className="case-title-text">{c.title}</span>
                          </td>
                          <td>
                            {renderPriorityBadge(c.priority)}
                          </td>
                          <td>
                            {renderStatusBadge(c.status)}
                          </td>
                          <td className="case-meta-cell">
                            {c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '—'}
                          </td>
                          <td className="case-officer-cell">
                            <div className="officer-badge-mini">
                              <User size={12} />
                              <span>{c.createdBy?.fullName || 'Assigned Lead'}</span>
                            </div>
                          </td>
                          <td className="case-meta-cell">
                            {formatTimeAgo(c.updatedAt || c.createdAt)}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button 
                              className="btn-open-dossier"
                              onClick={(e) => {
                                e.stopPropagation();
                                navigate(`/cases/${c.id}`);
                              }}
                              title="Open complete investigation dossier"
                            >
                              <span>VIEW</span>
                              <ExternalLink size={12} />
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
                <div className="table-footer-bar">
                  <span className="table-count-text">
                    Showing {recentCases.length} active investigations
                  </span>
                  <button 
                    className="btn-view-all-table"
                    onClick={() => navigate('/cases')}
                  >
                    <span>VIEW ALL CASE DOSSIERS</span>
                    <ExternalLink size={13} />
                  </button>
                </div>
              </div>
            )}
          </Panel>

          {/* 3. Investigation Link Board (Dynamic Entity Relationship Matrix) */}
          <InvestigationBoard 
            caseData={selectedCase}
            fir={selectedCaseEntities.fir}
            victims={selectedCaseEntities.victims}
            suspects={selectedCaseEntities.suspects}
            witnesses={selectedCaseEntities.witnesses}
            evidence={selectedCaseEntities.evidence}
            loading={boardLoading}
          />

          {/* 4. Two-Column Grid: Case Workload & Evidence Overview */}
          <div className="two-col-grid">
            {/* Case Workload */}
            <Panel 
              title="CASE WORKLOAD" 
              subtitle="Station Distribution by Investigation State"
              icon={TrendingUp}
            >
              <div className="workload-container">
                <div className="workload-item">
                  <div className="workload-header">
                    <span className="workload-label">Under Investigation</span>
                    <strong className="workload-count">{stats.underInvestigationCases || 0}</strong>
                  </div>
                  <div className="progress-track">
                    <div className="progress-fill fill-investigating" style={{ width: `${underInvPct}%` }}></div>
                  </div>
                </div>

                <div className="workload-item">
                  <div className="workload-header">
                    <span className="workload-label">Pending Review</span>
                    <strong className="workload-count">{stats.pendingReview || 0}</strong>
                  </div>
                  <div className="progress-track">
                    <div className="progress-fill fill-pending" style={{ width: `${pendingRevPct}%` }}></div>
                  </div>
                </div>

                <div className="workload-item">
                  <div className="workload-header">
                    <span className="workload-label">Filed</span>
                    <strong className="workload-count">{stats.filedCases || 0}</strong>
                  </div>
                  <div className="progress-track">
                    <div className="progress-fill fill-filed" style={{ width: `${filedPct}%` }}></div>
                  </div>
                </div>

                <div className="workload-item">
                  <div className="workload-header">
                    <span className="workload-label">Closed</span>
                    <strong className="workload-count">{stats.closedCases || 0}</strong>
                  </div>
                  <div className="progress-track">
                    <div className="progress-fill fill-closed" style={{ width: `${closedPct}%` }}></div>
                  </div>
                </div>
              </div>
            </Panel>

            {/* Evidence Overview */}
            <Panel 
              title="EVIDENCE OVERVIEW" 
              subtitle="Station Vault Chain-of-Custody & Gap Metrics"
              icon={FileCheck2}
            >
              <div className="evidence-overview-grid">
                <div className="ev-stat-card">
                  <span className="ev-stat-label">TOTAL EVIDENCE</span>
                  <strong className="ev-stat-num">{stats.totalEvidence || 0}</strong>
                  <span className="ev-stat-sub">Vault cataloged items</span>
                </div>

                <div className="ev-stat-card verified">
                  <span className="ev-stat-label">VERIFIED EVIDENCE</span>
                  <strong className="ev-stat-num green">{stats.verifiedEvidence || 0}</strong>
                  <span className="ev-stat-sub">SHA-256 integrity verified</span>
                </div>

                <div className="ev-stat-card pending">
                  <span className="ev-stat-label">PENDING VERIFICATION</span>
                  <strong className="ev-stat-num amber">{stats.pendingEvidence || 0}</strong>
                  <span className="ev-stat-sub">Awaiting forensic hash sign-off</span>
                </div>

                <div className="ev-stat-card gaps">
                  <span className="ev-stat-label">EVIDENCE GAPS</span>
                  <strong className="ev-stat-num red">{stats.evidenceGaps || 0}</strong>
                  <span className="ev-stat-sub">Open missing item flags</span>
                </div>
              </div>
            </Panel>
          </div>

          {/* 5. Two-Column Grid: Recent Activity & Cases Requiring Attention */}
          <div className="two-col-grid">
            {/* Recent Activity */}
            <Panel 
              title="RECENT ACTIVITY" 
              subtitle="Station Audit Log & Investigation Event Stream"
              icon={History}
            >
              <div className="activity-list">
                {stats.recentActivities && stats.recentActivities.length > 0 ? (
                  stats.recentActivities.map((act) => (
                    <div key={act.id} className="activity-item">
                      <div className="activity-dot"></div>
                      <div className="activity-main">
                        <div className="activity-header-row">
                          <span className="activity-action-tag">{formatAction(act.action)}</span>
                          <span className="activity-time">{formatTimeAgo(act.createdAt)}</span>
                        </div>
                        <div className="activity-details">{act.details || act.action}</div>
                        <div className="activity-actor-row">
                          <span>Officer: <strong>{act.actorName}</strong> ({act.actorBadge})</span>
                          <span className="activity-status-pill">{act.status}</span>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="activity-empty">
                    <Activity size={20} className="empty-icon-subtle" />
                    <span>No recent activity logs recorded under this precinct.</span>
                  </div>
                )}
              </div>
            </Panel>

            {/* Cases Requiring Attention */}
            <Panel 
              title="CASES REQUIRING ATTENTION" 
              subtitle="High Risk, Critical Priority & Escalation Queue"
              icon={ShieldAlert}
            >
              <div className="attention-list">
                {stats.attentionCases && stats.attentionCases.length > 0 ? (
                  stats.attentionCases.map((att) => (
                    <div 
                      key={att.id} 
                      className="attention-item"
                      onClick={() => navigate(`/cases/${att.id}`)}
                    >
                      <div className="attention-left">
                        <div className="attention-title-row">
                          <span className="attention-case-num">{att.caseNumber}</span>
                          {renderPriorityBadge(att.priority)}
                          {renderStatusBadge(att.status)}
                        </div>
                        <div className="attention-title-text">{att.title}</div>
                        <div className="attention-reason">{att.reason}</div>
                      </div>
                      <button className="btn-attention-action">
                        <span>DOSSIER</span>
                        <ChevronRight size={14} />
                      </button>
                    </div>
                  ))
                ) : (
                  <div className="activity-empty">
                    <CheckCircle2 size={20} className="empty-icon-green" />
                    <span>All station cases are currently up to date with zero open escalations.</span>
                  </div>
                )}
              </div>
            </Panel>
          </div>
        </main>
      </div>

      <style>{`
        .cs-dashboard-layout {
          min-height: 100vh;
          display: flex;
          flex-direction: column;
          background-color: var(--bg-primary);
        }

        .cs-main-wrapper {
          display: flex;
          flex-grow: 1;
        }

        .cs-content-area {
          flex-grow: 1;
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 22px;
          overflow-y: auto;
          max-width: calc(100vw - var(--sidebar-width));
        }

        .page-header {
          padding-bottom: 12px;
          border-bottom: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .page-title {
          font-size: 1.4rem;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .page-subtitle {
          font-size: 0.75rem;
          font-family: var(--font-subheading);
          color: var(--text-secondary);
          margin-top: 2px;
          letter-spacing: 0.08em;
        }

        .page-header-actions {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .btn-create-case {
          display: flex;
          align-items: center;
          gap: 8px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 8px 14px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
          box-shadow: var(--red-glow-subtle);
        }

        .btn-create-case:hover {
          box-shadow: var(--red-glow-medium);
          transform: translateY(-1px);
        }

        .stats-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 16px;
        }

        .cases-table-container {
          overflow-x: auto;
          background: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
        }

        .station-cases-table {
          width: 100%;
          border-collapse: collapse;
          text-align: left;
          font-size: 0.82rem;
        }

        .station-cases-table th {
          background-color: var(--bg-secondary);
          color: var(--text-muted);
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          padding: 10px 14px;
          border-bottom: 1px solid var(--border-color);
        }

        .station-cases-table td {
          padding: 10px 14px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.4);
          vertical-align: middle;
        }

        .case-row {
          cursor: pointer;
          transition: background-color var(--transition-fast);
        }

        .case-row:hover {
          background-color: var(--bg-panel);
        }

        .case-row.selected-row {
          background-color: rgba(193, 18, 31, 0.12);
          border-left: 3px solid var(--bright-crime-red);
        }

        .case-num-tag {
          font-family: var(--font-mono);
          font-weight: 700;
          color: var(--text-bright);
          background: rgba(18, 18, 20, 0.8);
          padding: 3px 8px;
          border-radius: 3px;
          border: 1px solid var(--border-color);
          font-size: 0.78rem;
        }

        .case-title-text {
          font-weight: 600;
          color: var(--text-primary);
        }

        .case-meta-cell {
          font-family: var(--font-mono);
          font-size: 0.72rem;
          color: var(--text-muted);
        }

        .officer-badge-mini {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          font-size: 0.75rem;
          color: var(--text-secondary);
        }

        .btn-open-dossier {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          background-color: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          color: var(--text-secondary);
          padding: 4px 8px;
          border-radius: 3px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-open-dossier:hover {
          color: var(--text-bright);
          border-color: var(--bright-crime-red);
          background-color: var(--bright-crime-red);
        }

        .table-footer-bar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 10px 14px;
          background-color: var(--bg-secondary);
          border-top: 1px solid var(--border-color);
        }

        .table-count-text {
          font-family: var(--font-mono);
          font-size: 0.72rem;
          color: var(--text-muted);
        }

        .btn-view-all-table {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          background: transparent;
          border: none;
          color: var(--bright-crime-red);
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
        }

        .btn-view-all-table:hover {
          text-decoration: underline;
        }

        .two-col-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 20px;
        }

        .workload-container {
          display: flex;
          flex-direction: column;
          gap: 14px;
        }

        .workload-item {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .workload-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 0.78rem;
        }

        .workload-label {
          color: var(--text-secondary);
          font-family: var(--font-heading);
        }

        .workload-count {
          font-family: var(--font-mono);
          color: var(--text-bright);
        }

        .progress-track {
          width: 100%;
          height: 6px;
          background-color: var(--bg-primary);
          border-radius: 3px;
          overflow: hidden;
          border: 1px solid var(--border-color);
        }

        .progress-fill {
          height: 100%;
          border-radius: 3px;
          transition: width 0.4s ease;
        }

        .fill-investigating { background-color: var(--bright-crime-red); }
        .fill-pending { background-color: #F59E0B; }
        .fill-filed { background-color: #38BDF8; }
        .fill-closed { background-color: #10B981; }

        .evidence-overview-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
        }

        .ev-stat-card {
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 12px;
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .ev-stat-card.verified { border-left: 3px solid #10B981; }
        .ev-stat-card.pending { border-left: 3px solid #F59E0B; }
        .ev-stat-card.gaps { border-left: 3px solid var(--bright-crime-red); }

        .ev-stat-label {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          color: var(--text-muted);
          letter-spacing: 0.08em;
        }

        .ev-stat-num {
          font-family: var(--font-mono);
          font-size: 1.5rem;
          font-weight: 700;
          color: var(--text-bright);
        }

        .ev-stat-num.green { color: #10B981; }
        .ev-stat-num.amber { color: #F59E0B; }
        .ev-stat-num.red { color: var(--bright-crime-red); }

        .ev-stat-sub {
          font-size: 0.68rem;
          color: var(--text-muted);
        }

        .activity-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
          max-height: 280px;
          overflow-y: auto;
        }

        .activity-item {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 8px 10px;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
        }

        .activity-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background-color: var(--bright-crime-red);
          margin-top: 5px;
          flex-shrink: 0;
        }

        .activity-main {
          flex-grow: 1;
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .activity-header-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .activity-action-tag {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          color: var(--text-bright);
          letter-spacing: 0.06em;
        }

        .activity-time {
          font-family: var(--font-mono);
          font-size: 0.65rem;
          color: var(--text-muted);
        }

        .activity-details {
          font-size: 0.74rem;
          color: var(--text-secondary);
        }

        .activity-actor-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 0.68rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .activity-status-pill {
          font-family: var(--font-mono);
          font-size: 0.6rem;
          background: rgba(16, 185, 129, 0.15);
          color: #10B981;
          padding: 1px 5px;
          border-radius: 2px;
        }

        .activity-empty {
          padding: 30px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          color: var(--text-muted);
          font-size: 0.75rem;
        }

        .empty-icon-subtle { color: var(--text-muted); opacity: 0.6; }
        .empty-icon-green { color: #10B981; }

        .attention-list {
          display: flex;
          flex-direction: column;
          gap: 10px;
          max-height: 280px;
          overflow-y: auto;
        }

        .attention-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 10px 12px;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-left: 3px solid var(--bright-crime-red);
          border-radius: 4px;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .attention-item:hover {
          background-color: var(--bg-panel);
          border-color: var(--bright-crime-red);
        }

        .attention-left {
          display: flex;
          flex-direction: column;
          gap: 3px;
          min-width: 0;
        }

        .attention-title-row {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .attention-case-num {
          font-family: var(--font-mono);
          font-weight: 700;
          font-size: 0.75rem;
          color: var(--text-bright);
        }

        .attention-title-text {
          font-size: 0.8rem;
          font-weight: 600;
          color: var(--text-primary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .attention-reason {
          font-size: 0.7rem;
          color: #F87171;
          font-style: italic;
        }

        .btn-attention-action {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          color: var(--text-bright);
          padding: 5px 8px;
          border-radius: 3px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          cursor: pointer;
          flex-shrink: 0;
        }

        .btn-attention-action:hover {
          background-color: var(--bright-crime-red);
          color: white;
        }

        .panel-loading {
          padding: 30px;
          text-align: center;
          font-family: var(--font-mono);
          font-size: 0.75rem;
          color: var(--text-muted);
        }

        .empty-cases-banner {
          padding: 24px 16px;
          text-align: center;
          background-color: var(--bg-primary);
          border: 1px dashed var(--border-color);
          border-radius: 4px;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 10px;
        }

        .empty-title {
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .empty-desc {
          font-size: 0.75rem;
          color: var(--text-muted);
        }

        .btn-create-subtle {
          display: flex;
          align-items: center;
          gap: 6px;
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          color: var(--text-bright);
          padding: 6px 12px;
          border-radius: 4px;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 600;
          cursor: pointer;
          margin-top: 4px;
          transition: all var(--transition-fast);
        }

        .btn-create-subtle:hover {
          background-color: var(--bright-crime-red);
          color: white;
        }

        .btn-review-action {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          background-color: rgba(34, 197, 94, 0.15);
          border: 1px solid rgba(34, 197, 94, 0.4);
          color: #86efac;
          padding: 4px 10px;
          border-radius: 4px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-review-action:hover {
          background-color: #16a34a;
          color: #ffffff;
        }

        .role-badge-dash {
          background: rgba(59, 130, 246, 0.15);
          color: #93c5fd;
          border: 1px solid rgba(59, 130, 246, 0.3);
          padding: 2px 7px;
          border-radius: 4px;
          font-size: 0.72rem;
          font-weight: 600;
        }

        .ocr-badge-dash {
          padding: 2px 7px;
          border-radius: 4px;
          font-size: 0.72rem;
          font-weight: 700;
        }

        .ocr-badge-dash.pass {
          background: rgba(34, 197, 94, 0.15);
          color: #4ade80;
          border: 1px solid rgba(34, 197, 94, 0.3);
        }

        .ocr-badge-dash.review {
          background: rgba(245, 158, 11, 0.15);
          color: #fbbf24;
          border: 1px solid rgba(245, 158, 11, 0.3);
        }

        .status-badge-dash {
          padding: 2px 7px;
          border-radius: 4px;
          font-size: 0.72rem;
          font-weight: 700;
        }

        .status-badge-dash.status-pending_station_approval,
        .status-badge-dash.status-pending_verification,
        .status-badge-dash.status-pending {
          background: rgba(245, 158, 11, 0.15);
          color: #fbbf24;
          border: 1px solid rgba(245, 158, 11, 0.3);
        }

        .status-badge-dash.status-active {
          background: rgba(34, 197, 94, 0.15);
          color: #4ade80;
          border: 1px solid rgba(34, 197, 94, 0.3);
        }

        .status-badge-dash.status-rejected {
          background: rgba(239, 68, 68, 0.15);
          color: #f87171;
          border: 1px solid rgba(239, 68, 68, 0.3);
        }

        @media (max-width: 1200px) {
          .stats-grid {
            grid-template-columns: repeat(2, 1fr);
          }
          .two-col-grid {
            grid-template-columns: 1fr;
          }
        }
      `}</style>
    </div>
  );
}

