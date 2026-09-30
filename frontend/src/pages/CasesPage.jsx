import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import Panel from '../components/Panel';
import { getCases } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { 
  Briefcase, 
  Search, 
  Filter, 
  PlusCircle, 
  ArrowRight, 
  ChevronLeft, 
  ChevronRight,
  FolderOpen,
  AlertCircle,
  Clock,
  Shield
} from 'lucide-react';

export default function CasesPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { officer } = useAuth();

  const [cases, setCases] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(8);

  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [statusFilter, setStatusFilter] = useState(searchParams.get('status') || 'ALL');
  const [priorityFilter, setPriorityFilter] = useState(searchParams.get('priority') || 'ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');

  useEffect(() => {
    const s = searchParams.get('status');
    if (s) setStatusFilter(s);
  }, [searchParams]);

  const [loading, setLoading] = useState(true);

  const fetchCaseList = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getCases({
        search: search.trim(),
        status: statusFilter,
        priority: priorityFilter,
        caseType: typeFilter,
        page,
        size: pageSize,
        sort: 'updatedAt,desc',
      });

      if (data) {
        setCases(data.content || []);
        setTotalElements(data.totalElements || 0);
        setTotalPages(data.totalPages || 1);
      }
    } catch (err) {
      console.error('Failed to fetch case listings:', err);
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter, priorityFilter, typeFilter, page, pageSize]);

  useEffect(() => {
    fetchCaseList();
  }, [fetchCaseList]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchCaseList();
  };

  const getPriorityBadgeClass = (priority) => {
    switch (priority) {
      case 'CRITICAL': return 'badge-priority-critical';
      case 'HIGH': return 'badge-priority-high';
      case 'MEDIUM': return 'badge-priority-medium';
      case 'LOW': return 'badge-priority-low';
      default: return 'badge-priority-medium';
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'OPEN': return 'badge-status-open';
      case 'UNDER_INVESTIGATION': return 'badge-status-investigating';
      case 'PENDING_REVIEW': return 'badge-status-review';
      case 'ON_HOLD': return 'badge-status-hold';
      case 'CLOSED': return 'badge-status-closed';
      default: return 'badge-status-open';
    }
  };

  const formatStatus = (status) => {
    return (status || 'OPEN').replace(/_/g, ' ');
  };

  const formatType = (type) => {
    return (type || 'OTHER').replace(/_/g, ' ');
  };

  return (
    <div className="cs-dashboard-layout">
      <TopBar />
      <div className="cs-main-wrapper">
        <Sidebar />

        <main className="cs-content-area forensic-grid">
          {/* Page Header */}
          <div className="page-header flex-between">
            <div>
              <h1 className="page-title">CASE FILES DIRECTORY</h1>
              <p className="page-subtitle">
                {officer?.station?.stationName || 'STATION PRECINCT'} • ACTIVE INVESTIGATION DOSSIERS
              </p>
            </div>

            <button 
              className="btn-create-case"
              onClick={() => navigate('/cases/new')}
              title="Register New Investigation Case"
            >
              <PlusCircle size={16} />
              <span>NEW CASE</span>
            </button>
          </div>

          {/* Filter Bar Panel */}
          <div className="filters-panel cs-panel">
            <form onSubmit={handleSearchSubmit} className="search-form">
              <Search size={16} className="search-icon" />
              <input 
                type="text" 
                placeholder="Search by Case Number, Title, or Narrative..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="filter-search-input"
              />
              <button type="submit" className="search-submit-btn">SEARCH</button>
            </form>

            <div className="filter-dropdowns">
              <div className="dropdown-group">
                <label className="filter-label">STATUS</label>
                <select 
                  value={statusFilter} 
                  onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
                  className="filter-select"
                >
                  <option value="ALL">ALL STATUSES</option>
                  <option value="OPEN">OPEN</option>
                  <option value="UNDER_INVESTIGATION">UNDER INVESTIGATION</option>
                  <option value="PENDING_REVIEW">PENDING REVIEW</option>
                  <option value="ON_HOLD">ON HOLD</option>
                  <option value="CLOSED">CLOSED</option>
                </select>
              </div>

              <div className="dropdown-group">
                <label className="filter-label">PRIORITY</label>
                <select 
                  value={priorityFilter} 
                  onChange={(e) => { setPriorityFilter(e.target.value); setPage(0); }}
                  className="filter-select"
                >
                  <option value="ALL">ALL PRIORITIES</option>
                  <option value="CRITICAL">CRITICAL</option>
                  <option value="HIGH">HIGH</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="LOW">LOW</option>
                </select>
              </div>

              <div className="dropdown-group">
                <label className="filter-label">TYPE</label>
                <select 
                  value={typeFilter} 
                  onChange={(e) => { setTypeFilter(e.target.value); setPage(0); }}
                  className="filter-select"
                >
                  <option value="ALL">ALL TYPES</option>
                  <option value="MURDER">MURDER</option>
                  <option value="THEFT">THEFT</option>
                  <option value="MISSING_PERSON">MISSING PERSON</option>
                  <option value="ASSAULT">ASSAULT</option>
                  <option value="CYBER_CRIME">CYBER CRIME</option>
                  <option value="OTHER">OTHER</option>
                </select>
              </div>
            </div>
          </div>

          {/* Cases Table / Records Panel */}
          <div className="cases-table-panel cs-panel">
            <div className="table-header-row">
              <div className="col-num">CASE NUMBER</div>
              <div className="col-title">TITLE & SUMMARY</div>
              <div className="col-type">TYPE</div>
              <div className="col-priority">PRIORITY</div>
              <div className="col-status">STATUS</div>
              <div className="col-updated">LAST UPDATED</div>
              <div className="col-action">ACTION</div>
            </div>

            {loading ? (
              <div className="table-loading-row">
                <Clock className="spin-icon" size={18} />
                <span>SCANNING PRECINCT CASE REGISTRY...</span>
              </div>
            ) : cases.length === 0 ? (
              <div className="table-empty-row">
                <FolderOpen size={36} className="empty-icon" />
                <div className="empty-title">NO ACTIVE CASES FOUND</div>
                <div className="empty-desc">
                  No investigations match the current filter criteria for this police station.
                </div>
                <button className="btn-create-case" onClick={() => navigate('/cases/new')}>
                  <PlusCircle size={15} />
                  <span>REGISTER FIRST CASE</span>
                </button>
              </div>
            ) : (
              <div className="table-body">
                {cases.map((c) => (
                  <div 
                    key={c.id} 
                    className="case-row-item"
                    onClick={() => navigate(`/cases/${c.id}`)}
                  >
                    <div className="col-num">
                      <span className="case-id-badge">{c.caseNumber || 'CS-2026-XXXX'}</span>
                    </div>

                    <div className="col-title">
                      <div className="row-case-title">{c.title}</div>
                      <div className="row-case-desc">
                        {c.description ? (c.description.length > 70 ? `${c.description.substring(0, 70)}...` : c.description) : 'Initial investigation open'}
                      </div>
                    </div>

                    <div className="col-type">
                      <span className="type-tag">{formatType(c.caseType)}</span>
                    </div>

                    <div className="col-priority">
                      <span className={`priority-badge ${getPriorityBadgeClass(c.priority)}`}>
                        {c.priority || 'MEDIUM'}
                      </span>
                    </div>

                    <div className="col-status">
                      <span className={`status-badge ${getStatusBadgeClass(c.status)}`}>
                        {formatStatus(c.status)}
                      </span>
                    </div>

                    <div className="col-updated">
                      <span className="date-text">
                        {c.updatedAt ? new Date(c.updatedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Active'}
                      </span>
                    </div>

                    <div className="col-action">
                      <button 
                        className="btn-view-case"
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/cases/${c.id}`);
                        }}
                      >
                        <span>VIEW</span>
                        <ArrowRight size={13} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Pagination Footer */}
            <div className="pagination-footer">
              <div className="pagination-info">
                SHOWING <strong>{cases.length}</strong> OF <strong>{totalElements}</strong> INVESTIGATIONS
              </div>

              <div className="pagination-controls">
                <button 
                  className="page-btn"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0 || loading}
                >
                  <ChevronLeft size={16} />
                  <span>PREV</span>
                </button>

                <div className="page-indicator">
                  PAGE <strong>{totalPages === 0 ? 1 : page + 1}</strong> OF <strong>{totalPages || 1}</strong>
                </div>

                <button 
                  className="page-btn"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1 || loading}
                >
                  <span>NEXT</span>
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
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
          gap: 20px;
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

        .btn-create-case {
          display: flex;
          align-items: center;
          gap: 8px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 8px 16px;
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

        .filters-panel {
          padding: 16px;
          display: flex;
          flex-wrap: wrap;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
        }

        .search-form {
          display: flex;
          align-items: center;
          position: relative;
          flex-grow: 1;
          max-width: 480px;
        }

        .search-icon {
          position: absolute;
          left: 12px;
          color: var(--text-muted);
        }

        .filter-search-input {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px 0 0 4px;
          padding: 8px 12px 8px 36px;
          color: var(--text-bright);
          font-size: 0.85rem;
          outline: none;
          transition: border-color var(--transition-fast);
        }

        .filter-search-input:focus {
          border-color: var(--bright-crime-red);
        }

        .search-submit-btn {
          background-color: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-left: none;
          border-radius: 0 4px 4px 0;
          color: var(--text-primary);
          padding: 8px 14px;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
        }

        .search-submit-btn:hover {
          background-color: var(--primary-crimson);
          color: white;
        }

        .filter-dropdowns {
          display: flex;
          align-items: center;
          gap: 14px;
        }

        .dropdown-group {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .filter-label {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-muted);
        }

        .filter-select {
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 6px 10px;
          color: var(--text-primary);
          font-size: 0.78rem;
          font-family: var(--font-subheading);
          outline: none;
          cursor: pointer;
        }

        .filter-select:focus {
          border-color: var(--bright-crime-red);
        }

        .cases-table-panel {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          display: flex;
          flex-direction: column;
        }

        .table-header-row {
          display: grid;
          grid-template-columns: 140px 1fr 120px 100px 150px 140px 90px;
          padding: 12px 16px;
          background-color: var(--bg-panel-elevated);
          border-bottom: 1px solid var(--border-color);
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-muted);
        }

        .table-body {
          display: flex;
          flex-direction: column;
        }

        .case-row-item {
          display: grid;
          grid-template-columns: 140px 1fr 120px 100px 150px 140px 90px;
          padding: 14px 16px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.6);
          align-items: center;
          cursor: pointer;
          transition: background-color var(--transition-fast);
        }

        .case-row-item:hover {
          background-color: rgba(193, 18, 31, 0.06);
        }

        .case-id-badge {
          font-family: var(--font-mono);
          font-size: 0.8rem;
          font-weight: 700;
          color: var(--bright-crime-red);
          letter-spacing: 0.05em;
        }

        .row-case-title {
          font-family: var(--font-subheading);
          font-size: 0.88rem;
          font-weight: 700;
          color: var(--text-bright);
        }

        .row-case-desc {
          font-size: 0.72rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .type-tag {
          font-family: var(--font-mono);
          font-size: 0.7rem;
          color: var(--text-secondary);
          background-color: var(--bg-primary);
          padding: 3px 6px;
          border: 1px solid var(--border-color);
          border-radius: 3px;
        }

        .priority-badge {
          display: inline-block;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          padding: 3px 8px;
          border-radius: 3px;
          text-align: center;
        }

        .badge-priority-critical {
          background-color: rgba(193, 18, 31, 0.25);
          border: 1px solid var(--bright-crime-red);
          color: #ff4d4d;
          box-shadow: 0 0 8px rgba(193, 18, 31, 0.3);
        }

        .badge-priority-high {
          background-color: rgba(139, 0, 0, 0.25);
          border: 1px solid var(--primary-crimson);
          color: #ff858d;
        }

        .badge-priority-medium {
          background-color: rgba(255, 165, 0, 0.15);
          border: 1px solid #ffaa00;
          color: #ffc107;
        }

        .badge-priority-low {
          background-color: rgba(100, 116, 139, 0.15);
          border: 1px solid #64748b;
          color: #94a3b8;
        }

        .status-badge {
          display: inline-block;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          padding: 3px 8px;
          border-radius: 3px;
          text-align: center;
        }

        .badge-status-open {
          background-color: rgba(0, 180, 216, 0.15);
          border: 1px solid #00b4d8;
          color: #48cae4;
        }

        .badge-status-investigating {
          background-color: rgba(255, 170, 0, 0.15);
          border: 1px solid #ffaa00;
          color: #ffaa00;
        }

        .badge-status-review {
          background-color: rgba(234, 88, 12, 0.15);
          border: 1px solid #ea580c;
          color: #fb923c;
        }

        .badge-status-hold {
          background-color: rgba(147, 51, 234, 0.15);
          border: 1px solid #9333ea;
          color: #c084fc;
        }

        .badge-status-closed {
          background-color: rgba(71, 85, 105, 0.2);
          border: 1px solid #475569;
          color: #94a3b8;
        }

        .date-text {
          font-family: var(--font-mono);
          font-size: 0.72rem;
          color: var(--text-muted);
        }

        .btn-view-case {
          display: flex;
          align-items: center;
          gap: 4px;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 3px;
          padding: 5px 8px;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-view-case:hover {
          color: var(--text-bright);
          border-color: var(--bright-crime-red);
          background-color: rgba(193, 18, 31, 0.15);
        }

        .table-loading-row, .table-empty-row {
          padding: 40px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 12px;
          text-align: center;
        }

        .table-loading-row {
          font-family: var(--font-mono);
          font-size: 0.8rem;
          color: var(--text-muted);
          flex-direction: row;
        }

        .empty-icon {
          color: var(--text-muted);
        }

        .empty-title {
          font-family: var(--font-subheading);
          font-size: 0.95rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .empty-desc {
          font-size: 0.8rem;
          color: var(--text-muted);
          max-width: 400px;
        }

        .pagination-footer {
          padding: 12px 16px;
          background-color: var(--bg-panel-elevated);
          border-top: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .pagination-info {
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          color: var(--text-muted);
          letter-spacing: 0.05em;
        }

        .pagination-controls {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .page-btn {
          display: flex;
          align-items: center;
          gap: 4px;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 3px;
          padding: 4px 10px;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .page-btn:hover:not(:disabled) {
          border-color: var(--bright-crime-red);
          color: var(--text-bright);
        }

        .page-btn:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }

        .page-indicator {
          font-family: var(--font-mono);
          font-size: 0.75rem;
          color: var(--text-primary);
        }

        .spin-icon {
          animation: spin 2s linear infinite;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }

        @media (max-width: 1024px) {
          .table-header-row, .case-row-item {
            grid-template-columns: 120px 1fr 100px 90px 120px 80px;
          }
          .col-updated {
            display: none;
          }
        }
      `}</style>
    </div>
  );
}
