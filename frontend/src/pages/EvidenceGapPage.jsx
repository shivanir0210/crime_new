import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import EvidenceGapDashboard from '../components/EvidenceGapDashboard';
import { getCases, getEvidenceGaps } from '../services/api';
import { 
  ShieldAlert, 
  Briefcase, 
  AlertTriangle, 
  ChevronRight, 
  Search,
  ExternalLink,
  FolderOpen
} from 'lucide-react';

export default function EvidenceGapPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const caseIdFromUrl = searchParams.get('caseId') || '';
  const [selectedCaseId, setSelectedCaseId] = useState(caseIdFromUrl);
  const [casesList, setCasesList] = useState([]);
  const [loadingCases, setLoadingCases] = useState(true);

  const [gaps, setGaps] = useState([]);
  const [loadingGaps, setLoadingGaps] = useState(false);
  const [fetchError, setFetchError] = useState('');

  // Fetch station cases for dropdown selection
  useEffect(() => {
    async function loadCases() {
      try {
        setLoadingCases(true);
        const data = await getCases({ size: 100, sort: 'updatedAt,desc' });
        const list = data?.content || [];
        setCasesList(list);

        // If no case ID in URL but cases exist, select first case
        if (!caseIdFromUrl && list.length > 0) {
          setSelectedCaseId(list[0].id);
          setSearchParams({ caseId: list[0].id });
        }
      } catch (err) {
        console.error('Failed to load station cases for evidence gap analysis:', err);
      } finally {
        setLoadingCases(false);
      }
    }
    loadCases();
  }, []);

  // Update selected case ID if URL changes
  useEffect(() => {
    if (caseIdFromUrl && caseIdFromUrl !== selectedCaseId) {
      setSelectedCaseId(caseIdFromUrl);
    }
  }, [caseIdFromUrl]);

  // Fetch gaps for selected case
  const fetchGapsForCase = useCallback(async (cId, showLoading = true) => {
    if (!cId) {
      setGaps([]);
      return;
    }
    try {
      if (showLoading) setLoadingGaps(true);
      setFetchError('');
      const gapsData = await getEvidenceGaps(cId);
      setGaps(gapsData || []);
    } catch (err) {
      console.error('Failed to fetch evidence gaps:', err);
      setFetchError(err.message || 'Failed to load evidence gaps for selected case');
      setGaps([]);
    } finally {
      if (showLoading) setLoadingGaps(false);
    }
  }, []);

  const handleGapUpdated = useCallback((updatedGap) => {
    if (updatedGap) {
      if (Array.isArray(updatedGap)) {
        setGaps(updatedGap);
      } else if (updatedGap.id) {
        setGaps(prevGaps => prevGaps.map(g => g.id === updatedGap.id ? { ...g, ...updatedGap } : g));
      }
    }
    if (selectedCaseId) {
      fetchGapsForCase(selectedCaseId, false);
    }
  }, [selectedCaseId, fetchGapsForCase]);

  useEffect(() => {
    if (selectedCaseId) {
      fetchGapsForCase(selectedCaseId);
    }
  }, [selectedCaseId, fetchGapsForCase]);

  // Handle case dropdown change
  const handleCaseChange = (e) => {
    const newCaseId = e.target.value;
    setSelectedCaseId(newCaseId);
    if (newCaseId) {
      setSearchParams({ caseId: newCaseId });
    } else {
      setSearchParams({});
    }
  };

  const selectedCaseObj = casesList.find(c => c.id === selectedCaseId);

  return (
    <div className="eg-page-wrapper">
      <TopBar />

      <div className="eg-page-body">
        <Sidebar />

        <main className="eg-page-content">
          {/* Top Bar Navigation / Case Selector Banner */}
          <div className="eg-selector-banner">
            <div className="eg-selector-left">
              <div className="eg-selector-icon">
                <ShieldAlert size={20} />
              </div>
              <div>
                <h1 className="eg-page-title">EVIDENCE GAP INTELLIGENCE</h1>
                <p className="eg-page-subtitle">Station-wide investigation completeness &amp; gap monitoring</p>
              </div>
            </div>

            {/* Case Selection Dropdown */}
            <div className="eg-selector-controls">
              <label htmlFor="eg-case-select" className="eg-case-label">
                <Briefcase size={14} />
                <span>Active Case:</span>
              </label>
              <select
                id="eg-case-select"
                value={selectedCaseId}
                onChange={handleCaseChange}
                className="eg-case-dropdown"
                disabled={loadingCases}
              >
                <option value="">-- Select a Case --</option>
                {casesList.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.caseNumber} — {c.title} ({c.priority} PRIORITY)
                  </option>
                ))}
              </select>

              {selectedCaseId && (
                <button
                  onClick={() => navigate(`/cases/${selectedCaseId}`)}
                  className="eg-dossier-btn"
                  title="Open full case dossier"
                >
                  <span>Open Dossier</span>
                  <ExternalLink size={13} />
                </button>
              )}
            </div>
          </div>

          {/* Error Alert */}
          {fetchError && (
            <div className="eg-alert eg-alert-error" style={{ marginBottom: 20 }}>
              <AlertTriangle size={18} />
              <div>
                <strong>API Request Failed:</strong> {fetchError}
              </div>
            </div>
          )}

          {/* Main Content Area */}
          {!selectedCaseId ? (
            /* No Case Selected State */
            <div className="eg-no-case-card">
              <div className="eg-no-case-icon">
                <FolderOpen size={48} />
              </div>
              <h2 className="eg-no-case-title">Select or open a case to view evidence gaps.</h2>
              <p className="eg-no-case-desc">
                Choose an active case from your station inventory above to run completeness rules, monitor missing forensic artifacts, and resolve investigation gaps.
              </p>

              {/* Station Cases Grid Preview */}
              {casesList.length > 0 && (
                <div className="eg-cases-grid-preview">
                  <div className="eg-preview-header">AVAILABLE STATION CASES:</div>
                  <div className="eg-preview-cards">
                    {casesList.slice(0, 6).map(c => (
                      <div
                        key={c.id}
                        onClick={() => {
                          setSelectedCaseId(c.id);
                          setSearchParams({ caseId: c.id });
                        }}
                        className="eg-preview-card"
                      >
                        <div className="eg-card-top">
                          <span className="eg-card-number">{c.caseNumber}</span>
                          <span className={`eg-card-priority prio-${c.priority}`}>{c.priority}</span>
                        </div>
                        <div className="eg-card-title">{c.title}</div>
                        <div className="eg-card-footer">
                          <span>Status: {c.status}</span>
                          <ChevronRight size={14} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            /* Evidence Gap Dashboard Component */
            <EvidenceGapDashboard
              caseId={selectedCaseId}
              gaps={gaps}
              loading={loadingGaps}
              onGapsUpdated={handleGapUpdated}
            />
          )}
        </main>
      </div>

      <style>{`
        .eg-page-wrapper {
          min-height: 100vh;
          display: flex;
          flex-direction: column;
          background-color: var(--bg-primary, #080808);
          color: var(--text-primary, #F5F5F5);
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
          overflow-x: hidden;
        }

        .eg-page-body {
          display: flex;
          flex: 1;
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
        }

        @media (max-width: 768px) {
          .eg-page-body {
            flex-direction: column;
          }
        }

        .eg-page-content {
          flex: 1;
          padding: 24px;
          max-width: 1400px;
          margin: 0 auto;
          width: 100%;
          min-width: 0;
          box-sizing: border-box;
        }

        @media (max-width: 768px) {
          .eg-page-content {
            padding: 14px;
          }
        }

        .eg-selector-banner {
          background-color: var(--bg-panel, #121214);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: var(--panel-radius, 8px);
          padding: 16px 20px;
          margin-bottom: 24px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 16px;
          flex-wrap: wrap;
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
        }

        .eg-selector-left {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .eg-selector-icon {
          width: 40px;
          height: 40px;
          border-radius: 8px;
          background: rgba(139, 0, 0, 0.25);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: var(--bright-crime-red, #C1121F);
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .eg-page-title {
          font-family: var(--font-heading);
          font-size: 1.1rem;
          font-weight: 700;
          color: var(--text-bright, #FFFFFF);
          letter-spacing: 0.06em;
          margin: 0;
          text-transform: uppercase;
        }

        .eg-page-subtitle {
          font-size: 0.76rem;
          color: var(--text-secondary, #A7A7AA);
          margin-top: 2px;
        }

        .eg-selector-controls {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
        }

        .eg-case-label {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.78rem;
          font-family: var(--font-subheading);
          font-weight: 700;
          color: var(--text-secondary, #A7A7AA);
          text-transform: uppercase;
          letter-spacing: 0.06em;
        }

        .eg-case-dropdown {
          background-color: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 6px;
          padding: 8px 14px;
          font-size: 0.82rem;
          color: var(--text-bright, #FFFFFF);
          font-family: var(--font-body);
          outline: none;
          min-width: 260px;
          cursor: pointer;
          transition: border-color 0.2s;
        }

        .eg-case-dropdown:focus {
          border-color: var(--bright-crime-red, #C1121F);
          box-shadow: 0 0 0 2px rgba(193, 18, 31, 0.25);
        }

        .eg-dossier-btn {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 8px 14px;
          background-color: rgba(42, 42, 45, 0.8);
          border: 1px solid rgba(111, 111, 115, 0.5);
          color: var(--text-primary, #F5F5F5);
          border-radius: 6px;
          font-size: 0.78rem;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s;
        }

        .eg-dossier-btn:hover {
          background-color: var(--bg-panel-elevated, #17171A);
          border-color: var(--bright-crime-red, #C1121F);
          color: var(--bright-crime-red, #C1121F);
        }

        /* No Case Selected State */
        .eg-no-case-card {
          background-color: var(--bg-panel, #121214);
          border: 1px dashed var(--border-color, #2A2A2D);
          border-radius: var(--panel-radius, 8px);
          padding: 48px 24px;
          text-align: center;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
        }

        .eg-no-case-icon {
          color: var(--bright-crime-red, #C1121F);
          opacity: 0.7;
          margin-bottom: 12px;
        }

        .eg-no-case-title {
          font-family: var(--font-heading);
          font-size: 1.1rem;
          font-weight: 700;
          color: var(--text-bright, #FFFFFF);
          margin-bottom: 8px;
        }

        .eg-no-case-desc {
          font-size: 0.82rem;
          color: var(--text-secondary, #A7A7AA);
          max-width: 540px;
          margin-bottom: 32px;
          line-height: 1.5;
        }

        .eg-cases-grid-preview {
          width: 100%;
          max-width: 900px;
        }

        .eg-preview-header {
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.12em;
          color: var(--text-muted, #6F6F73);
          margin-bottom: 12px;
          text-align: left;
        }

        .eg-preview-cards {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 12px;
        }

        @media (max-width: 768px) {
          .eg-preview-cards {
            grid-template-columns: repeat(2, 1fr);
          }
        }

        @media (max-width: 480px) {
          .eg-preview-cards {
            grid-template-columns: 1fr;
          }
        }

        .eg-preview-card {
          background-color: var(--bg-panel-elevated, #17171A);
          border: 1px solid var(--border-color, #2A2A2D);
          border-radius: 6px;
          padding: 14px;
          text-align: left;
          cursor: pointer;
          transition: all 0.2s;
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .eg-preview-card:hover {
          border-color: var(--bright-crime-red, #C1121F);
          transform: translateY(-2px);
          box-shadow: 0 4px 14px rgba(193, 18, 31, 0.2);
        }

        .eg-card-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .eg-card-number {
          font-family: var(--font-mono);
          font-size: 0.78rem;
          font-weight: 700;
          color: var(--text-bright, #FFFFFF);
        }

        .eg-card-priority {
          font-size: 0.65rem;
          font-family: var(--font-subheading);
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
          text-transform: uppercase;
        }

        .prio-CRITICAL { background: rgba(193, 18, 31, 0.2); color: #FF4D4D; border: 1px solid #C1121F; }
        .prio-HIGH { background: rgba(249, 115, 22, 0.2); color: #FB923C; border: 1px solid #F97316; }
        .prio-MEDIUM { background: rgba(245, 158, 11, 0.2); color: #FBBF24; border: 1px solid #F59E0B; }
        .prio-LOW { background: rgba(59, 130, 246, 0.2); color: #93C5FD; border: 1px solid #3B82F6; }

        .eg-card-title {
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--text-primary, #F5F5F5);
        }

        .eg-card-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 0.72rem;
          color: var(--text-muted, #6F6F73);
          border-top: 1px dashed rgba(42, 42, 45, 0.6);
          padding-top: 6px;
          margin-top: 4px;
        }
      `}</style>
    </div>
  );
}
