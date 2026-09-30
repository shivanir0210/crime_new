import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import { createCase } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { 
  FilePlus2, 
  ArrowLeft, 
  ShieldAlert, 
  AlertTriangle, 
  CheckCircle2, 
  Lock,
  Building2,
  UserCheck
} from 'lucide-react';

export default function CreateCasePage() {
  const navigate = useNavigate();
  const { officer } = useAuth();

  const [title, setTitle] = useState('');
  const [caseType, setCaseType] = useState('MURDER');
  const [priority, setPriority] = useState('HIGH');
  const [description, setDescription] = useState('');

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setErrorMessage('Case title is mandatory');
      return;
    }

    setErrorMessage('');
    setLoading(true);

    try {
      const payload = {
        title: title.trim(),
        caseType,
        priority,
        description: description.trim(),
      };

      const result = await createCase(payload);
      if (result && result.id) {
        navigate(`/cases/${result.id}`, { replace: true });
      } else {
        navigate('/cases', { replace: true });
      }
    } catch (err) {
      setErrorMessage(err.message || 'Failed to register investigation case.');
      setLoading(false);
    }
  };

  return (
    <div className="cs-dashboard-layout">
      <TopBar />
      <div className="cs-main-wrapper">
        <Sidebar />

        <main className="cs-content-area forensic-grid">
          {/* Header */}
          <div className="page-header flex-between">
            <div className="header-nav-title">
              <button className="btn-back" onClick={() => navigate('/cases')}>
                <ArrowLeft size={16} />
                <span>CASE DIRECTORY</span>
              </button>
              <h1 className="page-title">REGISTER NEW INVESTIGATION CASE</h1>
            </div>
          </div>

          <div className="create-case-container">
            {/* Form Panel */}
            <div className="create-case-card cs-panel cs-panel-accent">
              <div className="card-top-badge">
                <FilePlus2 size={18} className="badge-icon" />
                <span>OFFICIAL LAW ENFORCEMENT CASE INITIATION FORM</span>
              </div>

              <form onSubmit={handleSubmit} className="case-form">
                {errorMessage && (
                  <div className="form-alert error">
                    <AlertTriangle size={16} />
                    <span>{errorMessage}</span>
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label label-system">
                    CASE TITLE / INCIDENT DESIGNATION <span className="req">*</span>
                  </label>
                  <input 
                    type="text" 
                    placeholder="e.g. Homicide Investigation near Sector 14"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="case-input"
                    disabled={loading}
                    required
                  />
                </div>

                <div className="form-row-grid">
                  <div className="form-group">
                    <label className="form-label label-system">
                      CRIME CLASSIFICATION / TYPE <span className="req">*</span>
                    </label>
                    <select 
                      value={caseType}
                      onChange={(e) => setCaseType(e.target.value)}
                      className="case-select"
                      disabled={loading}
                    >
                      <option value="MURDER">MURDER (HOMICIDE)</option>
                      <option value="THEFT">THEFT / BURGLARY</option>
                      <option value="MISSING_PERSON">MISSING PERSON</option>
                      <option value="ASSAULT">ARMED ASSAULT / VIOLENCE</option>
                      <option value="CYBER_CRIME">CYBER FRAUD / BREACH</option>
                      <option value="OTHER">OTHER INVESTIGATION</option>
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label label-system">
                      INITIAL INVESTIGATION PRIORITY <span className="req">*</span>
                    </label>
                    <select 
                      value={priority}
                      onChange={(e) => setPriority(e.target.value)}
                      className="case-select"
                      disabled={loading}
                    >
                      <option value="CRITICAL">CRITICAL (IMMEDIATE RED ALERT)</option>
                      <option value="HIGH">HIGH (ACTIVE INVESTIGATION)</option>
                      <option value="MEDIUM">MEDIUM (STANDARD PRIORITY)</option>
                      <option value="LOW">LOW (ROUTINE INQUIRY)</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label label-system">
                    PRELIMINARY INCIDENT NARRATIVE & OBSERVATIONS
                  </label>
                  <textarea 
                    placeholder="Enter initial case summary, location details, incident overview, and early findings..."
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    rows={6}
                    className="case-textarea"
                    disabled={loading}
                  />
                </div>

                {/* System Auto-binding metadata note */}
                <div className="security-binding-box">
                  <div className="binding-header">
                    <Lock size={13} className="lock-ic" />
                    <span>AUTOMATIC SERVER-SIDE DATA INTEGRITY BINDINGS</span>
                  </div>
                  <div className="binding-grid">
                    <div className="binding-item">
                      <Building2 size={13} className="bind-icon" />
                      <div>
                        <div className="bind-label">ASSIGNED STATION</div>
                        <div className="bind-val">{officer?.station?.stationName || 'COIMBATORE CENTRAL PS'}</div>
                      </div>
                    </div>
                    <div className="binding-item">
                      <UserCheck size={13} className="bind-icon" />
                      <div>
                        <div className="bind-label">PRIMARY INVESTIGATOR</div>
                        <div className="bind-val">{officer?.fullName || 'CURRENT OFFICER'} ({officer?.policeId || 'TN-POL'})</div>
                      </div>
                    </div>
                  </div>
                  <div className="binding-footer-text">
                    Unique Case Number (CS-2026-XXXX) and initial OPEN status are assigned automatically upon submission.
                  </div>
                </div>

                <div className="form-action-row">
                  <button 
                    type="button" 
                    className="btn-cancel"
                    onClick={() => navigate('/cases')}
                    disabled={loading}
                  >
                    CANCEL
                  </button>
                  <button 
                    type="submit" 
                    className="btn-submit-case"
                    disabled={loading}
                  >
                    <FilePlus2 size={16} />
                    <span>{loading ? 'REGISTERING INVESTIGATION...' : 'REGISTER CASE DOSSIER'}</span>
                  </button>
                </div>
              </form>
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
        }

        .header-nav-title {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .btn-back {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          background: transparent;
          border: none;
          color: var(--text-muted);
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          width: fit-content;
          padding: 0;
          transition: color var(--transition-fast);
        }

        .btn-back:hover {
          color: var(--bright-crime-red);
        }

        .page-title {
          font-size: 1.4rem;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .create-case-container {
          max-width: 800px;
          width: 100%;
          margin: 0 auto;
        }

        .create-case-card {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6);
        }

        .card-top-badge {
          padding: 14px 20px;
          background-color: var(--bg-panel-elevated);
          border-bottom: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          gap: 10px;
          font-family: var(--font-subheading);
          font-size: 0.78rem;
          font-weight: 700;
          letter-spacing: 0.12em;
          color: var(--text-bright);
        }

        .badge-icon {
          color: var(--bright-crime-red);
        }

        .case-form {
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .form-alert.error {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 10px 14px;
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          color: #ff858d;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 600;
        }

        .form-group {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .form-label {
          color: var(--text-secondary);
          font-size: 0.75rem;
        }

        .req {
          color: var(--bright-crime-red);
        }

        .case-input, .case-select, .case-textarea {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 10px 12px;
          color: var(--text-bright);
          font-family: var(--font-body);
          font-size: 0.9rem;
          outline: none;
          transition: border-color var(--transition-fast);
        }

        .case-input:focus, .case-select:focus, .case-textarea:focus {
          border-color: var(--bright-crime-red);
          box-shadow: 0 0 8px rgba(193, 18, 31, 0.25);
        }

        .form-row-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 16px;
        }

        .case-textarea {
          resize: vertical;
          min-height: 120px;
          line-height: 1.5;
        }

        .security-binding-box {
          background-color: rgba(18, 18, 20, 0.9);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 14px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .binding-header {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-secondary);
        }

        .lock-ic {
          color: var(--bright-crime-red);
        }

        .binding-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
          padding: 8px 0;
          border-top: 1px solid rgba(42, 42, 45, 0.5);
          border-bottom: 1px solid rgba(42, 42, 45, 0.5);
        }

        .binding-item {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .bind-icon {
          color: var(--text-muted);
          flex-shrink: 0;
        }

        .bind-label {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          color: var(--text-muted);
        }

        .bind-val {
          font-family: var(--font-body);
          font-size: 0.8rem;
          font-weight: 600;
          color: var(--text-bright);
        }

        .binding-footer-text {
          font-family: var(--font-mono);
          font-size: 0.68rem;
          color: var(--text-muted);
        }

        .form-action-row {
          display: flex;
          align-items: center;
          justify-content: flex-end;
          gap: 14px;
          margin-top: 10px;
        }

        .btn-cancel {
          background: transparent;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 10px 18px;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          cursor: pointer;
        }

        .btn-cancel:hover {
          color: var(--text-bright);
          border-color: var(--text-secondary);
        }

        .btn-submit-case {
          display: flex;
          align-items: center;
          gap: 8px;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 10px 22px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.95rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          transition: all var(--transition-fast);
          box-shadow: var(--red-glow-subtle);
        }

        .btn-submit-case:hover:not(:disabled) {
          box-shadow: var(--red-glow-medium);
          transform: translateY(-1px);
        }

        .btn-submit-case:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        @media (max-width: 650px) {
          .form-row-grid, .binding-grid {
            grid-template-columns: 1fr;
          }
        }
      `}</style>
    </div>
  );
}
