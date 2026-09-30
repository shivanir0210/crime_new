import React, { useState, useEffect } from 'react';
import { 
  Shield, 
  UserCheck, 
  Clock, 
  CheckCircle2, 
  AlertTriangle, 
  Search, 
  Building2, 
  FileText, 
  ArrowUpDown, 
  RefreshCw,
  Eye,
  Filter,
  BadgeAlert,
  Sparkles
} from 'lucide-react';
import Topbar from '../components/Topbar';
import Sidebar from '../components/Sidebar';
import StationHeadRegistrationReviewModal from '../components/StationHeadRegistrationReviewModal';
import { getPendingOfficerRegistrations } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function OfficerRegistrationsPage() {
  const { officer } = useAuth();

  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedReg, setSelectedReg] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    setLoading(true);
    getPendingOfficerRegistrations()
      .then((data) => {
        if (Array.isArray(data)) {
          setRegistrations(data);
        }
      })
      .catch((err) => {
        console.error('Failed to load pending registrations:', err);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [refreshKey]);

  const handleOpenReview = (reg) => {
    setSelectedReg(reg);
    setIsModalOpen(true);
  };

  const filteredRegistrations = registrations.filter((reg) => {
    const matchesSearch =
      (reg.fullName && reg.fullName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (reg.policeId && reg.policeId.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (reg.email && reg.email.toLowerCase().includes(searchTerm.toLowerCase()));

    if (!matchesSearch) return false;

    if (statusFilter === 'ALL') return true;
    if (statusFilter === 'PENDING') return reg.status === 'PENDING_STATION_APPROVAL' || reg.status === 'PENDING_VERIFICATION';
    if (statusFilter === 'PASSED') return reg.ocrStatus === 'PASSED';
    if (statusFilter === 'REVIEW') return reg.ocrStatus === 'REVIEW_REQUIRED';
    if (statusFilter === 'REJECTED') return reg.status === 'REJECTED';

    return true;
  });

  // Calculate statistics
  const pendingCount = registrations.filter((r) => r.status === 'PENDING_STATION_APPROVAL' || r.status === 'PENDING_VERIFICATION').length;
  const ocrPassedCount = registrations.filter((r) => r.ocrStatus === 'PASSED').length;
  const ocrReviewCount = registrations.filter((r) => r.ocrStatus === 'REVIEW_REQUIRED').length;
  const rejectedCount = registrations.filter((r) => r.status === 'REJECTED').length;

  return (
    <div className="cs-app-layout forensic-grid">
      <Topbar />

      <div className="cs-main-container">
        <Sidebar />

        <main className="cs-main-content">
          {/* Header Banner */}
          <div className="page-header-bar cs-panel">
            <div className="header-titles">
              <div className="title-row">
                <UserCheck size={26} className="title-icon text-red" />
                <h1 className="page-title">TRUSTED OFFICER REGISTRATIONS</h1>
                <span className="station-badge">
                  <Building2 size={13} />
                  <span>{officer?.station?.stationName || 'STATION HEAD APPROVALS'}</span>
                </span>
              </div>
              <p className="page-desc">
                Review and authenticate new officer registrations, examine uploaded police credentials, verify OCR data matching, and approve or reject accounts.
              </p>
            </div>

            <div className="header-actions">
              <button 
                onClick={() => setRefreshKey((k) => k + 1)} 
                className="cs-btn-secondary"
                disabled={loading}
              >
                <RefreshCw size={14} className={loading ? 'spinning' : ''} />
                <span>REFRESH LIST</span>
              </button>
            </div>
          </div>

          {/* Metric Stats Cards */}
          <div className="stats-row-grid">
            <div className="stat-card cs-panel">
              <div className="stat-meta">
                <Clock size={20} className="stat-icon text-amber" />
                <span className="stat-label">AWAITING APPROVAL</span>
              </div>
              <div className="stat-number text-amber">{pendingCount}</div>
              <div className="stat-foot">Requires Station Head review</div>
            </div>

            <div className="stat-card cs-panel">
              <div className="stat-meta">
                <CheckCircle2 size={20} className="stat-icon text-green" />
                <span className="stat-label">OCR VERIFIED</span>
              </div>
              <div className="stat-number text-green">{ocrPassedCount}</div>
              <div className="stat-foot">100% ID document data match</div>
            </div>

            <div className="stat-card cs-panel">
              <div className="stat-meta">
                <Sparkles size={20} className="stat-icon text-blue" />
                <span className="stat-label">REVIEW REQUIRED</span>
              </div>
              <div className="stat-number text-blue">{ocrReviewCount}</div>
              <div className="stat-foot">Minor field differences flagged</div>
            </div>

            <div className="stat-card cs-panel">
              <div className="stat-meta">
                <AlertTriangle size={20} className="stat-icon text-red" />
                <span className="stat-label">REJECTED APPLICATIONS</span>
              </div>
              <div className="stat-number text-red">{rejectedCount}</div>
              <div className="stat-foot">Access denied records</div>
            </div>
          </div>

          {/* Filter & Search Bar */}
          <div className="control-bar cs-panel">
            <div className="search-box">
              <Search size={16} className="search-icon" />
              <input
                type="text"
                placeholder="Search by Officer Name, Police ID, or Email..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="search-input"
              />
            </div>

            <div className="filter-tabs">
              {[
                { id: 'ALL', label: 'ALL APPLICATIONS' },
                { id: 'PENDING', label: `PENDING (${pendingCount})` },
                { id: 'PASSED', label: 'OCR PASSED' },
                { id: 'REVIEW', label: 'REVIEW REQUIRED' },
                { id: 'REJECTED', label: 'REJECTED' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  className={`filter-tab ${statusFilter === tab.id ? 'active' : ''}`}
                  onClick={() => setStatusFilter(tab.id)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          {/* Table of Registrations */}
          <div className="table-container cs-panel cs-panel-accent">
            {loading ? (
              <div className="table-loading">
                <RefreshCw size={24} className="spinning" />
                <span>LOADING PENDING REGISTRATIONS...</span>
              </div>
            ) : filteredRegistrations.length === 0 ? (
              <div className="table-empty">
                <UserCheck size={40} className="empty-icon" />
                <h3>NO OFFICER REGISTRATIONS FOUND</h3>
                <p>There are currently no pending officer registrations matching your filter criteria for this station.</p>
              </div>
            ) : (
              <table className="cs-table">
                <thead>
                  <tr>
                    <th>OFFICER NAME</th>
                    <th>POLICE ID</th>
                    <th>EMAIL / CONTACT</th>
                    <th>ASSIGNED ROLE</th>
                    <th>OCR VERIFICATION</th>
                    <th>STATUS</th>
                    <th>SUBMISSION DATE</th>
                    <th style={{ textAlign: 'right' }}>ACTION</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredRegistrations.map((reg) => (
                    <tr key={reg.id} className="table-row-hover">
                      <td>
                        <div className="officer-name-cell">
                          <span className="officer-name">{reg.fullName}</span>
                          <span className="officer-station">{reg.stationName}</span>
                        </div>
                      </td>

                      <td>
                        <span className="badge-police-id font-mono">{reg.policeId}</span>
                      </td>

                      <td>
                        <div className="contact-cell">
                          <span className="contact-email">{reg.email || '—'}</span>
                          <span className="contact-phone">{reg.phone || '—'}</span>
                        </div>
                      </td>

                      <td>
                        <span className="role-pill">{reg.role}</span>
                      </td>

                      <td>
                        <div className="ocr-cell">
                          <span className={`ocr-badge ${reg.ocrStatus === 'PASSED' ? 'pass' : 'review'}`}>
                            {reg.ocrStatus === 'PASSED' ? (
                              <><CheckCircle2 size={12} /> PASSED</>
                            ) : (
                              <><AlertTriangle size={12} /> REVIEW REQUIRED</>
                            )}
                          </span>
                          <div className="match-flags">
                            <span className={`mini-flag ${reg.nameMatches ? 'ok' : 'warn'}`} title="Name Match">
                              N
                            </span>
                            <span className={`mini-flag ${reg.policeIdMatches ? 'ok' : 'warn'}`} title="PID Match">
                              ID
                            </span>
                            <span className={`mini-flag ${reg.stationMatches ? 'ok' : 'warn'}`} title="Station Match">
                              ST
                            </span>
                            <span className={`mini-flag ${reg.roleMatches ? 'ok' : 'warn'}`} title="Role Match">
                              R
                            </span>
                          </div>
                        </div>
                      </td>

                      <td>
                        <span className={`status-pill status-${(reg.status || 'PENDING').toLowerCase()}`}>
                          {reg.status}
                        </span>
                      </td>

                      <td className="date-cell">
                        {reg.createdAt ? new Date(reg.createdAt).toLocaleDateString() : '—'}
                      </td>

                      <td style={{ textAlign: 'right' }}>
                        <button
                          onClick={() => handleOpenReview(reg)}
                          className="cs-btn-review"
                        >
                          <Eye size={14} />
                          <span>REVIEW & VERIFY</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </main>
      </div>

      <StationHeadRegistrationReviewModal
        isOpen={isModalOpen}
        registration={selectedReg}
        onClose={() => {
          setIsModalOpen(false);
          setSelectedReg(null);
        }}
        onDecisionComplete={() => setRefreshKey((k) => k + 1)}
      />

      <style>{`
        .page-header-bar {
          padding: 18px 24px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          margin-bottom: 20px;
        }

        .title-row {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .page-title {
          font-family: var(--font-heading);
          font-size: 1.35rem;
          letter-spacing: 0.08em;
          color: var(--text-bright);
          margin: 0;
        }

        .text-red {
          color: var(--bright-crime-red);
        }

        .text-green {
          color: #28a745;
        }

        .text-amber {
          color: #f59e0b;
        }

        .text-blue {
          color: #3b82f6;
        }

        .station-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: #ff858d;
          font-family: var(--font-mono);
          font-size: 0.72rem;
          font-weight: 700;
          padding: 3px 10px;
          border-radius: 4px;
        }

        .page-desc {
          font-size: 0.82rem;
          color: var(--text-secondary);
          margin: 6px 0 0 0;
          max-width: 800px;
          line-height: 1.4;
        }

        .stats-row-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 16px;
          margin-bottom: 20px;
        }

        @media (max-width: 992px) {
          .stats-row-grid {
            grid-template-columns: repeat(2, 1fr);
          }
        }

        .stat-card {
          padding: 16px 20px;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .stat-meta {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .stat-label {
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          color: var(--text-muted);
          letter-spacing: 0.08em;
        }

        .stat-number {
          font-family: var(--font-heading);
          font-size: 1.8rem;
          font-weight: 700;
          line-height: 1;
        }

        .stat-foot {
          font-family: var(--font-mono);
          font-size: 0.65rem;
          color: var(--text-muted);
        }

        .control-bar {
          padding: 14px 20px;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          margin-bottom: 20px;
          flex-wrap: wrap;
        }

        .search-box {
          position: relative;
          flex: 1;
          min-width: 260px;
        }

        .search-icon {
          position: absolute;
          left: 12px;
          top: 50%;
          transform: translateY(-50%);
          color: var(--text-muted);
        }

        .search-input {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 8px 12px 8px 36px;
          color: var(--text-bright);
          font-size: 0.85rem;
          outline: none;
        }

        .search-input:focus {
          border-color: var(--bright-crime-red);
        }

        .filter-tabs {
          display: flex;
          align-items: center;
          gap: 6px;
          flex-wrap: wrap;
        }

        .filter-tab {
          background-color: transparent;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 6px 12px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 600;
          color: var(--text-secondary);
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .filter-tab.active {
          background-color: rgba(193, 18, 31, 0.15);
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        .table-container {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          overflow: hidden;
        }

        .cs-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 0.85rem;
        }

        .cs-table th {
          background-color: var(--bg-panel-elevated);
          padding: 12px 16px;
          text-align: left;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          color: var(--text-muted);
          border-bottom: 1px solid var(--border-color);
          letter-spacing: 0.08em;
        }

        .cs-table td {
          padding: 14px 16px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.5);
          color: var(--text-primary);
        }

        .table-row-hover:hover {
          background-color: rgba(255, 255, 255, 0.02);
        }

        .officer-name-cell {
          display: flex;
          flex-direction: column;
        }

        .officer-name {
          font-weight: 600;
          color: var(--text-bright);
        }

        .officer-station {
          font-size: 0.72rem;
          color: var(--text-muted);
        }

        .badge-police-id {
          color: var(--bright-crime-red);
          font-weight: 700;
          font-size: 0.85rem;
        }

        .contact-cell {
          display: flex;
          flex-direction: column;
          font-size: 0.78rem;
        }

        .contact-email {
          color: var(--text-primary);
        }

        .contact-phone {
          color: var(--text-muted);
          font-size: 0.72rem;
        }

        .role-pill {
          display: inline-block;
          background-color: rgba(59, 130, 246, 0.12);
          border: 1px solid rgba(59, 130, 246, 0.3);
          color: #93c5fd;
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 0.75rem;
          font-family: var(--font-mono);
        }

        .ocr-cell {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .ocr-badge {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-family: var(--font-mono);
          font-size: 0.68rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
          width: fit-content;
        }

        .ocr-badge.pass {
          background-color: rgba(40, 167, 69, 0.15);
          border: 1px solid #28a745;
          color: #85e89d;
        }

        .ocr-badge.review {
          background-color: rgba(245, 158, 11, 0.15);
          border: 1px solid #f59e0b;
          color: #fde68a;
        }

        .match-flags {
          display: flex;
          gap: 3px;
        }

        .mini-flag {
          font-family: var(--font-mono);
          font-size: 0.6rem;
          font-weight: 700;
          padding: 1px 4px;
          border-radius: 2px;
        }

        .mini-flag.ok {
          background-color: rgba(40, 167, 69, 0.2);
          color: #28a745;
        }

        .mini-flag.warn {
          background-color: rgba(239, 68, 68, 0.2);
          color: #ef4444;
        }

        .status-pill {
          display: inline-block;
          padding: 3px 8px;
          border-radius: 4px;
          font-family: var(--font-mono);
          font-size: 0.7rem;
          font-weight: 700;
        }

        .status-pending_station_approval,
        .status-pending_verification {
          background-color: rgba(245, 158, 11, 0.15);
          border: 1px solid #f59e0b;
          color: #fde68a;
        }

        .status-active {
          background-color: rgba(40, 167, 69, 0.15);
          border: 1px solid #28a745;
          color: #85e89d;
        }

        .status-rejected {
          background-color: rgba(239, 68, 68, 0.15);
          border: 1px solid #ef4444;
          color: #fca5a5;
        }

        .date-cell {
          font-family: var(--font-mono);
          font-size: 0.75rem;
          color: var(--text-muted);
        }

        .cs-btn-review {
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 6px 12px;
          color: var(--text-bright);
          font-family: var(--font-heading);
          font-size: 0.78rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          transition: all var(--transition-fast);
        }

        .cs-btn-review:hover {
          background-color: var(--bright-crime-red);
          color: white;
          box-shadow: 0 0 10px rgba(193, 18, 31, 0.4);
        }

        .table-loading, .table-empty {
          padding: 48px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 12px;
          color: var(--text-muted);
          text-align: center;
        }

        .table-empty h3 {
          color: var(--text-primary);
          margin: 0;
          font-family: var(--font-heading);
        }

        .table-empty p {
          max-width: 440px;
          font-size: 0.85rem;
          margin: 0;
        }

        .spinning {
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
