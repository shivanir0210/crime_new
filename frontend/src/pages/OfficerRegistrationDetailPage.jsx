import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { 
  Shield, 
  UserCheck, 
  Clock, 
  CheckCircle2, 
  AlertTriangle, 
  FileText, 
  ExternalLink, 
  Building2, 
  XCircle,
  Eye,
  Send,
  Lock,
  Sparkles,
  ArrowLeft,
  Mail,
  Phone,
  Calendar,
  BadgeCheck,
  RefreshCw,
  FileCheck2
} from 'lucide-react';
import Topbar from '../components/Topbar';
import Sidebar from '../components/Sidebar';
import { 
  getOfficerRegistrationDetail, 
  approveOfficerRegistration, 
  rejectOfficerRegistration, 
  getOfficerIdDocumentUrl 
} from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function OfficerRegistrationDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { officer: currentOfficer } = useAuth();

  const [registration, setRegistration] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Rejection modal state
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');

  // ID Document preview state
  const [docLoading, setDocLoading] = useState(false);
  const [docBlobUrl, setDocBlobUrl] = useState(null);
  const [docError, setDocError] = useState('');

  useEffect(() => {
    loadRegistration();
  }, [id]);

  const loadRegistration = async () => {
    try {
      setLoading(true);
      setErrorMessage('');
      const data = await getOfficerRegistrationDetail(id);
      setRegistration(data);
      loadDocumentPreview(id);
    } catch (err) {
      console.error('Failed to load registration details:', err);
      setErrorMessage(err.message || 'Failed to load registration details. You may not have authorization for this police station.');
    } finally {
      setLoading(false);
    }
  };

  const loadDocumentPreview = async (regId) => {
    const token = sessionStorage.getItem('crimesphere_token');
    const docUrl = getOfficerIdDocumentUrl(regId);
    setDocLoading(true);
    setDocError('');

    try {
      const res = await fetch(docUrl, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      if (!res.ok) {
        throw new Error(res.status === 403 ? 'Station authorization required to view ID document.' : 'Unable to preview ID document.');
      }

      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      setDocBlobUrl(url);
    } catch (err) {
      setDocError(err.message);
    } finally {
      setDocLoading(false);
    }
  };

  const handleApprove = async () => {
    setErrorMessage('');
    setActionLoading(true);
    try {
      await approveOfficerRegistration(id);
      setSuccessMessage('Officer account has been successfully APPROVED and activated. The officer may now log in.');
      setTimeout(() => {
        navigate('/officer-approvals');
      }, 1500);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to approve registration');
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async (e) => {
    e.preventDefault();
    if (!rejectionReason.trim()) {
      setErrorMessage('Rejection reason is mandatory.');
      return;
    }

    setErrorMessage('');
    setActionLoading(true);
    try {
      await rejectOfficerRegistration(id, rejectionReason.trim());
      setSuccessMessage('Officer registration has been REJECTED. Notification dispatched to applicant.');
      setIsRejectModalOpen(false);
      setTimeout(() => {
        navigate('/officer-approvals');
      }, 1500);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to reject registration');
      setActionLoading(false);
    }
  };

  const openDocumentInNewTab = () => {
    if (docBlobUrl) {
      window.open(docBlobUrl, '_blank');
    }
  };

  const isPending = registration && (registration.status === 'PENDING_STATION_APPROVAL' || registration.status === 'PENDING_VERIFICATION');

  return (
    <div className="cs-app-layout forensic-grid">
      <Topbar />

      <div className="cs-main-container">
        <Sidebar />

        <main className="cs-main-content">
          {/* Header Bar */}
          <div className="page-header-bar cs-panel">
            <div className="header-titles">
              <div className="title-row">
                <Link to="/officer-approvals" className="cs-back-link">
                  <ArrowLeft size={16} />
                  <span>ALL REGISTRATIONS</span>
                </Link>
                <span className="title-divider">/</span>
                <UserCheck size={22} className="title-icon text-red" />
                <h1 className="page-title">
                  {registration ? `${registration.fullName} (${registration.policeId})` : 'OFFICER REGISTRATION REVIEW'}
                </h1>
                {registration && (
                  <span className={`status-pill status-${(registration.status || 'PENDING').toLowerCase()}`}>
                    {registration.status}
                  </span>
                )}
              </div>
              <p className="page-desc">
                Examine submitted police credentials, compare OCR machine-extracted fields against applicant claims, and authorize or deny account access.
              </p>
            </div>

            <div className="header-actions">
              <button 
                onClick={loadRegistration} 
                className="cs-btn-secondary"
                disabled={loading || actionLoading}
              >
                <RefreshCw size={14} className={loading ? 'spinning' : ''} />
                <span>REFRESH</span>
              </button>
            </div>
          </div>

          {/* Feedback Alerts */}
          {errorMessage && (
            <div className="auth-alert error mb-4">
              <AlertTriangle size={18} className="alert-icon" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="auth-alert success mb-4">
              <CheckCircle2 size={18} className="alert-icon" />
              <span>{successMessage}</span>
            </div>
          )}

          {loading ? (
            <div className="loading-container cs-panel">
              <RefreshCw size={28} className="spinning" />
              <p>Loading officer registration details and security credentials...</p>
            </div>
          ) : !registration ? (
            <div className="empty-container cs-panel">
              <AlertTriangle size={36} className="text-amber" />
              <h3>Registration Not Found</h3>
              <p>The requested officer registration record could not be loaded or is not assigned to your station.</p>
              <Link to="/officer-approvals" className="cs-btn-secondary mt-3">
                RETURN TO LIST
              </Link>
            </div>
          ) : (
            <div className="review-grid-layout">
              {/* Left Column: Officer Details & OCR Verification */}
              <div className="review-left-col">
                {/* 1. Officer Dossier Card */}
                <div className="cs-panel cs-panel-accent review-card">
                  <div className="card-header">
                    <div className="card-title">
                      <Shield size={18} className="text-red" />
                      <span>OFFICER APPLICATION DOSSIER</span>
                    </div>
                    <span className="station-tag">
                      <Building2 size={13} />
                      <span>{registration.stationName || 'Police Station'}</span>
                    </span>
                  </div>

                  <div className="details-grid-2">
                    <div className="detail-box">
                      <span className="box-label">FULL NAME</span>
                      <strong className="box-value">{registration.fullName}</strong>
                    </div>

                    <div className="detail-box">
                      <span className="box-label">POLICE ID / BADGE</span>
                      <strong className="box-value badge-pid">{registration.policeId}</strong>
                    </div>

                    <div className="detail-box">
                      <span className="box-label">EMAIL ADDRESS</span>
                      <div className="box-value-icon">
                        <Mail size={14} className="text-muted" />
                        <span>{registration.email || '—'}</span>
                      </div>
                    </div>

                    <div className="detail-box">
                      <span className="box-label">PHONE NUMBER</span>
                      <div className="box-value-icon">
                        <Phone size={14} className="text-muted" />
                        <span>{registration.phone || '—'}</span>
                      </div>
                    </div>

                    <div className="detail-box">
                      <span className="box-label">ASSIGNED STATION</span>
                      <div className="box-value-icon">
                        <Building2 size={14} className="text-muted" />
                        <span>{registration.stationName} ({registration.stationCode})</span>
                      </div>
                    </div>

                    <div className="detail-box">
                      <span className="box-label">REQUESTED ROLE</span>
                      <span className="role-pill">{registration.role}</span>
                    </div>

                    <div className="detail-box">
                      <span className="box-label">SUBMISSION DATE</span>
                      <div className="box-value-icon">
                        <Calendar size={14} className="text-muted" />
                        <span>{registration.createdAt ? new Date(registration.createdAt).toLocaleString() : '—'}</span>
                      </div>
                    </div>

                    <div className="detail-box">
                      <span className="box-label">CURRENT STATUS</span>
                      <span className={`status-pill status-${(registration.status || 'PENDING').toLowerCase()}`}>
                        {registration.status}
                      </span>
                    </div>
                  </div>

                  {registration.rejectionReason && (
                    <div className="rejection-reason-box">
                      <strong className="rejection-title">REJECTION REASON ON RECORD:</strong>
                      <p className="rejection-text">{registration.rejectionReason}</p>
                    </div>
                  )}

                  {registration.approvedByPoliceId && (
                    <div className="approval-meta-box">
                      <CheckCircle2 size={16} className="text-green" />
                      <span>
                        Reviewed by <strong>{registration.approvedByName || registration.approvedByPoliceId}</strong> on {registration.approvedAt ? new Date(registration.approvedAt).toLocaleString() : '—'}
                      </span>
                    </div>
                  )}
                </div>

                {/* 2. Automated OCR Extraction & Field Matching */}
                <div className="cs-panel review-card">
                  <div className="card-header">
                    <div className="card-title">
                      <Sparkles size={18} className="text-blue" />
                      <span>AUTOMATED OCR VERIFICATION ANALYSIS</span>
                    </div>
                    <span className={`ocr-badge ${registration.ocrStatus === 'PASSED' ? 'pass' : 'review'}`}>
                      {registration.ocrStatus === 'PASSED' ? (
                        <>
                          <CheckCircle2 size={14} />
                          <span>PASSED (100% MATCH)</span>
                        </>
                      ) : (
                        <>
                          <AlertTriangle size={14} />
                          <span>REVIEW REQUIRED</span>
                        </>
                      )}
                    </span>
                  </div>

                  <p className="ocr-desc">
                    CrimeSphere AI OCR engine inspected the uploaded credentials and cross-checked detected strings with submitted application data.
                  </p>

                  <div className="ocr-comparison-table-wrapper">
                    <table className="ocr-comparison-table">
                      <thead>
                        <tr>
                          <th>FIELD</th>
                          <th>APPLICANT CLAIM</th>
                          <th>OCR EXTRACTED VALUE</th>
                          <th style={{ textAlign: 'center' }}>MATCH</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td className="field-name">Officer Name</td>
                          <td>{registration.fullName}</td>
                          <td className="font-mono">{registration.ocrDetectedName || '<Not Detected>'}</td>
                          <td style={{ textAlign: 'center' }}>
                            {registration.nameMatches ? (
                              <span className="match-tag ok">✓ MATCH</span>
                            ) : (
                              <span className="match-tag warn">⚠ REVIEW</span>
                            )}
                          </td>
                        </tr>

                        <tr>
                          <td className="field-name">Police ID / Badge</td>
                          <td className="badge-pid">{registration.policeId}</td>
                          <td className="font-mono">{registration.ocrDetectedPoliceId || '<Not Detected>'}</td>
                          <td style={{ textAlign: 'center' }}>
                            {registration.policeIdMatches ? (
                              <span className="match-tag ok">✓ MATCH</span>
                            ) : (
                              <span className="match-tag warn">⚠ REVIEW</span>
                            )}
                          </td>
                        </tr>

                        <tr>
                          <td className="field-name">Police Station</td>
                          <td>{registration.stationName}</td>
                          <td className="font-mono">{registration.ocrDetectedStation || '<Not Detected>'}</td>
                          <td style={{ textAlign: 'center' }}>
                            {registration.stationMatches ? (
                              <span className="match-tag ok">✓ MATCH</span>
                            ) : (
                              <span className="match-tag warn">⚠ REVIEW</span>
                            )}
                          </td>
                        </tr>

                        <tr>
                          <td className="field-name">Assigned Role</td>
                          <td>{registration.role}</td>
                          <td className="font-mono">{registration.ocrDetectedRole || '<Not Detected>'}</td>
                          <td style={{ textAlign: 'center' }}>
                            {registration.roleMatches ? (
                              <span className="match-tag ok">✓ MATCH</span>
                            ) : (
                              <span className="match-tag warn">⚠ REVIEW</span>
                            )}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  {registration.ocrRawText && (
                    <div className="ocr-raw-section">
                      <span className="ocr-raw-label">RAW OCR DETECTED TEXT:</span>
                      <pre className="ocr-raw-box">{registration.ocrRawText}</pre>
                    </div>
                  )}
                </div>
              </div>

              {/* Right Column: ID Document Preview & Decision Box */}
              <div className="review-right-col">
                {/* 3. Secure ID Document Viewer */}
                <div className="cs-panel review-card doc-card">
                  <div className="card-header">
                    <div className="card-title">
                      <Lock size={16} className="text-amber" />
                      <span>OFFICIAL ID CREDENTIAL DOCUMENT</span>
                    </div>
                    {docBlobUrl && (
                      <button onClick={openDocumentInNewTab} className="btn-popout" title="Open document in full window">
                        <ExternalLink size={14} />
                        <span>EXPAND</span>
                      </button>
                    )}
                  </div>

                  <div className="doc-preview-area">
                    {docLoading ? (
                      <div className="doc-loading">
                        <RefreshCw size={24} className="spinning" />
                        <span>Decrypting ID Document Stream...</span>
                      </div>
                    ) : docError ? (
                      <div className="doc-error">
                        <AlertTriangle size={32} className="text-red" />
                        <p>{docError}</p>
                      </div>
                    ) : docBlobUrl ? (
                      <div className="doc-embed-wrapper">
                        <iframe 
                          src={docBlobUrl} 
                          title="Official Police ID Document" 
                          className="doc-iframe"
                        />
                      </div>
                    ) : (
                      <div className="doc-empty">
                        <FileText size={36} className="text-muted" />
                        <p>No document available for preview</p>
                      </div>
                    )}
                  </div>

                  <div className="doc-footer-notice">
                    <Shield size={13} />
                    <span>RESTRICTED ACCESS • PROTECTED BY STATION HEAD AUTHORIZATION</span>
                  </div>
                </div>

                {/* 4. Station Head Decision Action Panel */}
                <div className="cs-panel cs-panel-accent decision-card">
                  <div className="card-title">
                    <UserCheck size={18} className="text-red" />
                    <span>STATION HEAD AUTHORIZATION DECISION</span>
                  </div>

                  <p className="decision-desc">
                    {isPending ? (
                      'Review the credentials above and authorize activation or reject the registration with justification.'
                    ) : (
                      `This officer registration has already been resolved with status ${registration.status}.`
                    )}
                  </p>

                  {isPending ? (
                    <div className="decision-actions-grid">
                      <button
                        onClick={handleApprove}
                        className="cs-btn-approve"
                        disabled={actionLoading}
                      >
                        <CheckCircle2 size={16} />
                        <span>{actionLoading ? 'PROCESSING...' : 'APPROVE & ACTIVATE ACCOUNT'}</span>
                      </button>

                      <button
                        onClick={() => setIsRejectModalOpen(true)}
                        className="cs-btn-reject"
                        disabled={actionLoading}
                      >
                        <XCircle size={16} />
                        <span>REJECT REGISTRATION</span>
                      </button>
                    </div>
                  ) : (
                    <div className="decision-resolved-badge">
                      <CheckCircle2 size={18} className="text-green" />
                      <span>Account status is <strong>{registration.status}</strong></span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Rejection Modal */}
          {isRejectModalOpen && (
            <div className="modal-overlay" onClick={() => setIsRejectModalOpen(false)}>
              <div className="modal-container" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                  <div className="modal-title text-red">
                    <XCircle size={20} />
                    <h3>Reject Officer Registration</h3>
                  </div>
                  <button className="close-btn" onClick={() => setIsRejectModalOpen(false)}>✕</button>
                </div>

                <p className="modal-sub">
                  Please provide a clear justification for denying access to Officer <strong>{registration?.fullName}</strong> ({registration?.policeId}). This reason will be logged in the audit trail and sent to the applicant.
                </p>

                <form onSubmit={handleRejectSubmit}>
                  <div className="form-group">
                    <label className="form-label">REJECTION REASON (MANDATORY) *</label>
                    <textarea
                      value={rejectionReason}
                      onChange={(e) => setRejectionReason(e.target.value)}
                      placeholder="e.g. ID card credentials mismatch, invalid service badge number, unauthorized station registration..."
                      rows={4}
                      className="cs-input"
                      required
                    />
                  </div>

                  <div className="modal-actions">
                    <button
                      type="button"
                      onClick={() => setIsRejectModalOpen(false)}
                      className="cs-btn-secondary"
                      disabled={actionLoading}
                    >
                      CANCEL
                    </button>
                    <button
                      type="submit"
                      className="cs-btn-danger"
                      disabled={actionLoading || !rejectionReason.trim()}
                    >
                      <Send size={15} />
                      <span>{actionLoading ? 'REJECTING...' : 'CONFIRM REJECTION'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </main>
      </div>

      <style>{`
        .cs-back-link {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          color: var(--text-secondary);
          text-decoration: none;
          font-size: 0.82rem;
          font-family: var(--font-subheading);
          font-weight: 600;
          transition: color var(--transition-fast);
        }

        .cs-back-link:hover {
          color: var(--bright-crime-red);
        }

        .title-divider {
          color: var(--border-color);
          margin: 0 4px;
        }

        .review-grid-layout {
          display: grid;
          grid-template-columns: 1.15fr 0.85fr;
          gap: 20px;
          margin-top: 16px;
        }

        @media (max-width: 1100px) {
          .review-grid-layout {
            grid-template-columns: 1fr;
          }
        }

        .review-left-col, .review-right-col {
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .review-card {
          padding: 20px;
          background: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 8px;
        }

        .card-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          border-bottom: 1px solid var(--border-color);
          padding-bottom: 14px;
          margin-bottom: 16px;
        }

        .card-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-family: var(--font-heading);
          font-size: 0.95rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          color: var(--text-bright);
        }

        .station-tag {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          background: rgba(193, 18, 31, 0.12);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: #ff6b6b;
          font-size: 0.75rem;
          font-weight: 600;
          padding: 3px 10px;
          border-radius: 4px;
        }

        .details-grid-2 {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 16px;
        }

        @media (max-width: 600px) {
          .details-grid-2 {
            grid-template-columns: 1fr;
          }
        }

        .detail-box {
          background: rgba(18, 18, 20, 0.7);
          border: 1px solid var(--border-color);
          padding: 12px;
          border-radius: 6px;
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .box-label {
          font-size: 0.7rem;
          font-family: var(--font-subheading);
          color: var(--text-muted);
          font-weight: 700;
          letter-spacing: 0.05em;
        }

        .box-value {
          font-size: 0.95rem;
          color: var(--text-bright);
        }

        .box-value-icon {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.88rem;
          color: var(--text-primary);
        }

        .rejection-reason-box {
          margin-top: 16px;
          background: rgba(193, 18, 31, 0.08);
          border: 1px solid rgba(193, 18, 31, 0.3);
          border-radius: 6px;
          padding: 12px 14px;
        }

        .rejection-title {
          display: block;
          font-size: 0.75rem;
          font-family: var(--font-subheading);
          color: #ff6b6b;
          margin-bottom: 4px;
        }

        .rejection-text {
          margin: 0;
          color: var(--text-bright);
          font-size: 0.88rem;
          line-height: 1.4;
        }

        .approval-meta-box {
          margin-top: 16px;
          background: rgba(34, 197, 94, 0.08);
          border: 1px solid rgba(34, 197, 94, 0.3);
          border-radius: 6px;
          padding: 10px 14px;
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 0.85rem;
          color: #86efac;
        }

        .ocr-desc {
          color: var(--text-secondary);
          font-size: 0.85rem;
          margin: 0 0 16px 0;
          line-height: 1.5;
        }

        .ocr-comparison-table-wrapper {
          overflow-x: auto;
          border: 1px solid var(--border-color);
          border-radius: 6px;
        }

        .ocr-comparison-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 0.85rem;
        }

        .ocr-comparison-table th {
          background: rgba(18, 18, 20, 0.9);
          padding: 10px 14px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          color: var(--text-muted);
          text-align: left;
          border-bottom: 1px solid var(--border-color);
        }

        .ocr-comparison-table td {
          padding: 10px 14px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.5);
          color: var(--text-primary);
        }

        .ocr-comparison-table tr:last-child td {
          border-bottom: none;
        }

        .field-name {
          font-weight: 600;
          color: var(--text-bright);
        }

        .match-tag {
          display: inline-block;
          font-size: 0.72rem;
          font-weight: 700;
          padding: 3px 8px;
          border-radius: 4px;
        }

        .match-tag.ok {
          background: rgba(34, 197, 94, 0.15);
          color: #4ade80;
          border: 1px solid rgba(34, 197, 94, 0.4);
        }

        .match-tag.warn {
          background: rgba(245, 158, 11, 0.15);
          color: #fbbf24;
          border: 1px solid rgba(245, 158, 11, 0.4);
        }

        .ocr-raw-section {
          margin-top: 16px;
        }

        .ocr-raw-label {
          font-size: 0.7rem;
          font-family: var(--font-subheading);
          color: var(--text-muted);
          font-weight: 700;
          display: block;
          margin-bottom: 6px;
        }

        .ocr-raw-box {
          background: #09090b;
          border: 1px solid var(--border-color);
          border-radius: 6px;
          padding: 10px 12px;
          font-family: var(--font-mono);
          font-size: 0.75rem;
          color: #94a3b8;
          max-height: 120px;
          overflow-y: auto;
          white-space: pre-wrap;
          word-break: break-word;
          margin: 0;
        }

        .doc-preview-area {
          min-height: 400px;
          background: #09090b;
          border: 1px dashed var(--border-color);
          border-radius: 6px;
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;
          margin-bottom: 12px;
        }

        .doc-iframe {
          width: 100%;
          height: 420px;
          border: none;
          background: #ffffff;
        }

        .doc-embed-wrapper {
          width: 100%;
          height: 100%;
        }

        .doc-loading, .doc-error, .doc-empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 12px;
          color: var(--text-muted);
          padding: 24px;
          text-align: center;
        }

        .doc-footer-notice {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.7rem;
          font-family: var(--font-subheading);
          color: var(--text-muted);
        }

        .btn-popout {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          background: rgba(42, 42, 45, 0.6);
          border: 1px solid var(--border-color);
          color: var(--text-secondary);
          padding: 4px 8px;
          border-radius: 4px;
          font-size: 0.72rem;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-popout:hover {
          color: var(--text-bright);
          border-color: var(--bright-crime-red);
        }

        .decision-card {
          display: flex;
          flex-direction: column;
          gap: 14px;
        }

        .decision-desc {
          margin: 0;
          color: var(--text-secondary);
          font-size: 0.85rem;
          line-height: 1.4;
        }

        .decision-actions-grid {
          display: grid;
          grid-template-columns: 1.3fr 1fr;
          gap: 12px;
        }

        @media (max-width: 600px) {
          .decision-actions-grid {
            grid-template-columns: 1fr;
          }
        }

        .cs-btn-approve {
          background: #16a34a;
          color: #ffffff;
          border: none;
          padding: 10px 16px;
          border-radius: 6px;
          font-family: var(--font-subheading);
          font-weight: 700;
          font-size: 0.85rem;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          transition: background var(--transition-fast);
        }

        .cs-btn-approve:hover:not(:disabled) {
          background: #15803d;
        }

        .cs-btn-reject {
          background: rgba(220, 38, 38, 0.15);
          color: #f87171;
          border: 1px solid rgba(220, 38, 38, 0.4);
          padding: 10px 16px;
          border-radius: 6px;
          font-family: var(--font-subheading);
          font-weight: 700;
          font-size: 0.85rem;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          transition: all var(--transition-fast);
        }

        .cs-btn-reject:hover:not(:disabled) {
          background: rgba(220, 38, 38, 0.25);
          border-color: #ef4444;
        }

        .decision-resolved-badge {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px;
          background: rgba(18, 18, 20, 0.8);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          font-size: 0.88rem;
          color: var(--text-bright);
        }

        .modal-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.75);
          backdrop-filter: blur(4px);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 9999;
          padding: 20px;
        }

        .modal-container {
          background: #111827;
          border: 1px solid var(--border-color);
          border-radius: 8px;
          max-width: 500px;
          width: 100%;
          padding: 24px;
          box-shadow: 0 12px 30px rgba(0, 0, 0, 0.8);
        }

        .modal-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 12px;
        }

        .modal-title {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .modal-title h3 {
          margin: 0;
          font-size: 1.1rem;
          font-weight: 700;
        }

        .close-btn {
          background: none;
          border: none;
          color: var(--text-muted);
          font-size: 1.1rem;
          cursor: pointer;
        }

        .modal-sub {
          color: var(--text-secondary);
          font-size: 0.85rem;
          margin: 0 0 16px 0;
          line-height: 1.5;
        }

        .modal-actions {
          display: flex;
          justify-content: flex-end;
          gap: 10px;
          margin-top: 20px;
        }

        .cs-btn-danger {
          background: #dc2626;
          color: #ffffff;
          border: none;
          padding: 8px 16px;
          border-radius: 6px;
          font-family: var(--font-subheading);
          font-weight: 700;
          font-size: 0.85rem;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .cs-btn-danger:hover:not(:disabled) {
          background: #b91c1c;
        }

        .cs-btn-danger:disabled {
          opacity: 0.5;
          cursor: not-allowed;
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
