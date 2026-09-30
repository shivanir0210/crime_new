import React, { useState } from 'react';
import { 
  X, 
  Shield, 
  CheckCircle2, 
  AlertTriangle, 
  FileText, 
  ExternalLink, 
  UserCheck, 
  Building2, 
  BadgeCheck, 
  XCircle,
  Eye,
  Send,
  Lock,
  Sparkles
} from 'lucide-react';
import { approveOfficerRegistration, rejectOfficerRegistration, getOfficerIdDocumentUrl } from '../services/api';

export default function StationHeadRegistrationReviewModal({ 
  isOpen, 
  registration, 
  onClose, 
  onDecisionComplete 
}) {
  const [rejecting, setRejecting] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  if (!isOpen || !registration) return null;

  const handleApprove = async () => {
    setErrorMessage('');
    setActionLoading(true);
    try {
      await approveOfficerRegistration(registration.id);
      setSuccessMessage('Officer registration successfully APPROVED. Account is now active.');
      setTimeout(() => {
        if (onDecisionComplete) onDecisionComplete();
        onClose();
      }, 1200);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to approve registration');
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async (e) => {
    e.preventDefault();
    if (!rejectionReason.trim()) {
      setErrorMessage('A rejection reason is mandatory.');
      return;
    }

    setErrorMessage('');
    setActionLoading(true);
    try {
      await rejectOfficerRegistration(registration.id, rejectionReason.trim());
      setSuccessMessage('Officer registration REJECTED. Notification sent to applicant.');
      setTimeout(() => {
        if (onDecisionComplete) onDecisionComplete();
        onClose();
      }, 1200);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to reject registration');
      setActionLoading(false);
    }
  };

  const handleViewDocument = () => {
    const token = sessionStorage.getItem('crimesphere_token');
    const docUrl = getOfficerIdDocumentUrl(registration.id);
    
    // Fetch with auth token and open in new tab
    fetch(docUrl, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
      .then(res => {
        if (!res.ok) throw new Error('Document access denied');
        return res.blob();
      })
      .then(blob => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      })
      .catch(err => {
        setErrorMessage('Failed to load ID document: ' + err.message);
      });
  };

  return (
    <div className="cs-modal-overlay">
      <div className="cs-modal-container cs-panel cs-panel-accent">
        {/* Modal Header */}
        <div className="cs-modal-header">
          <div className="modal-title-wrapper">
            <UserCheck size={20} className="modal-header-icon" />
            <div>
              <h3 className="modal-main-title">OFFICER REGISTRATION VERIFICATION</h3>
              <div className="modal-sub-title">
                STATION HEAD REVIEW PORTAL • {registration.stationName || 'ASSIGNED STATION'}
              </div>
            </div>
          </div>
          <button className="modal-close-btn" onClick={onClose} disabled={actionLoading}>
            <X size={18} />
          </button>
        </div>

        {/* Modal Body */}
        <div className="cs-modal-body">
          {errorMessage && (
            <div className="auth-alert error">
              <AlertTriangle size={16} className="alert-icon" />
              <span>{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="auth-alert success">
              <CheckCircle2 size={16} className="alert-icon" />
              <span>{successMessage}</span>
            </div>
          )}

          {/* Section 1: Officer Core Dossier */}
          <div className="review-section">
            <div className="section-title">
              <Shield size={16} className="sec-title-icon" />
              <span>OFFICER APPLICATION DOSSIER</span>
            </div>

            <div className="details-grid">
              <div className="detail-item">
                <span className="d-label">FULL NAME</span>
                <span className="d-value">{registration.fullName}</span>
              </div>

              <div className="detail-item">
                <span className="d-label">POLICE ID / BADGE</span>
                <span className="d-value badge-pid">{registration.policeId}</span>
              </div>

              <div className="detail-item">
                <span className="d-label">EMAIL ADDRESS</span>
                <span className="d-value">{registration.email || 'N/A'}</span>
              </div>

              <div className="detail-item">
                <span className="d-label">PHONE NUMBER</span>
                <span className="d-value">{registration.phone || 'N/A'}</span>
              </div>

              <div className="detail-item">
                <span className="d-label">ASSIGNED STATION</span>
                <span className="d-value">{registration.stationName} ({registration.stationCode})</span>
              </div>

              <div className="detail-item">
                <span className="d-label">REQUESTED ROLE</span>
                <span className="d-value">{registration.role}</span>
              </div>

              <div className="detail-item">
                <span className="d-label">SUBMISSION DATE</span>
                <span className="d-value">
                  {registration.createdAt ? new Date(registration.createdAt).toLocaleString() : 'Recent'}
                </span>
              </div>

              <div className="detail-item">
                <span className="d-label">CURRENT STATUS</span>
                <span className="d-value status-badge">{registration.status}</span>
              </div>
            </div>
          </div>

          {/* Section 2: Uploaded Police ID Document */}
          <div className="review-section">
            <div className="section-title">
              <FileText size={16} className="sec-title-icon" />
              <span>OFFICIAL ID DOCUMENT</span>
            </div>

            <div className="doc-inspect-card">
              <div className="doc-info">
                <FileText size={28} className="doc-icon" />
                <div>
                  <div className="doc-name">Official Police Identification Card</div>
                  <div className="doc-desc">Encrypted private vault file • Station Head authorized access only</div>
                </div>
              </div>

              <button 
                type="button" 
                onClick={handleViewDocument}
                className="cs-btn-secondary doc-view-btn"
                disabled={!registration.hasIdDocument}
              >
                <Eye size={15} />
                <span>VIEW ID DOCUMENT</span>
              </button>
            </div>
          </div>

          {/* Section 3: OCR Extraction & Form Comparison Matrix */}
          <div className="review-section">
            <div className="section-title">
              <Sparkles size={16} className="sec-title-icon text-amber" />
              <span>OCR EXTRACTION & COMPARISON MATRIX</span>
              <span className={`ocr-status-pill ${registration.ocrStatus === 'PASSED' ? 'pass' : 'review'}`}>
                {registration.ocrStatus || 'PENDING'}
              </span>
            </div>

            <div className="comparison-table-wrapper">
              <table className="comparison-table">
                <thead>
                  <tr>
                    <th>FIELD</th>
                    <th>REGISTERED VALUE</th>
                    <th>OCR DETECTED VALUE</th>
                    <th>VERIFICATION RESULT</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="field-name">Full Name</td>
                    <td>{registration.fullName}</td>
                    <td className="detected-val">{registration.ocrDetectedName || '—'}</td>
                    <td>
                      {registration.nameMatches ? (
                        <span className="match-pill match"><CheckCircle2 size={13} /> MATCH</span>
                      ) : (
                        <span className="match-pill mismatch"><AlertTriangle size={13} /> MISMATCH</span>
                      )}
                    </td>
                  </tr>

                  <tr>
                    <td className="field-name">Police ID</td>
                    <td className="font-mono">{registration.policeId}</td>
                    <td className="detected-val font-mono">{registration.ocrDetectedPoliceId || '—'}</td>
                    <td>
                      {registration.policeIdMatches ? (
                        <span className="match-pill match"><CheckCircle2 size={13} /> MATCH</span>
                      ) : (
                        <span className="match-pill mismatch"><AlertTriangle size={13} /> MISMATCH</span>
                      )}
                    </td>
                  </tr>

                  <tr>
                    <td className="field-name">Police Station</td>
                    <td>{registration.stationName}</td>
                    <td className="detected-val">{registration.ocrDetectedStation || '—'}</td>
                    <td>
                      {registration.stationMatches ? (
                        <span className="match-pill match"><CheckCircle2 size={13} /> MATCH</span>
                      ) : (
                        <span className="match-pill mismatch"><AlertTriangle size={13} /> MISMATCH</span>
                      )}
                    </td>
                  </tr>

                  <tr>
                    <td className="field-name">Assigned Role</td>
                    <td>{registration.role}</td>
                    <td className="detected-val">{registration.ocrDetectedRole || '—'}</td>
                    <td>
                      {registration.roleMatches ? (
                        <span className="match-pill match"><CheckCircle2 size={13} /> MATCH</span>
                      ) : (
                        <span className="match-pill mismatch"><AlertTriangle size={13} /> MISMATCH</span>
                      )}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* Section 4: Rejection Reason Form (Conditional) */}
          {rejecting && (
            <div className="rejection-box cs-panel">
              <h4 className="rej-title">MANDATORY REJECTION REASON</h4>
              <textarea
                placeholder="Specify the reason for rejecting this registration (e.g. ID card expired, photograph mismatch, unverified police station assignment)..."
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                className="cs-textarea"
                rows={3}
                required
              />
              <div className="rej-actions">
                <button 
                  type="button" 
                  onClick={() => setRejecting(false)} 
                  className="cs-btn-secondary"
                  disabled={actionLoading}
                >
                  CANCEL
                </button>
                <button 
                  type="button" 
                  onClick={handleRejectSubmit} 
                  className="cs-btn-reject"
                  disabled={actionLoading || !rejectionReason.trim()}
                >
                  <XCircle size={16} />
                  <span>CONFIRM REJECTION</span>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Modal Actions Footer */}
        <div className="cs-modal-footer">
          <div className="footer-left">
            <Lock size={12} className="lock-icon" />
            <span>STATION ISOLATION ENFORCED • AUDIT LOG RECORDED</span>
          </div>

          {!rejecting && (
            <div className="footer-right">
              <button 
                type="button" 
                onClick={() => setRejecting(true)} 
                className="cs-btn-reject-outline"
                disabled={actionLoading}
              >
                <XCircle size={16} />
                <span>REJECT</span>
              </button>

              <button 
                type="button" 
                onClick={handleApprove} 
                className="cs-btn-approve"
                disabled={actionLoading}
              >
                <CheckCircle2 size={16} />
                <span>APPROVE OFFICER</span>
              </button>
            </div>
          )}
        </div>
      </div>

      <style>{`
        .cs-modal-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.85);
          backdrop-filter: blur(4px);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 9999;
          padding: 20px;
        }

        .cs-modal-container {
          width: 100%;
          max-width: 780px;
          max-height: 90vh;
          display: flex;
          flex-direction: column;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          box-shadow: 0 16px 50px rgba(0, 0, 0, 0.9), var(--red-glow-subtle);
          overflow: hidden;
        }

        .cs-modal-header {
          padding: 16px 20px;
          background-color: var(--bg-panel-elevated);
          border-bottom: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .modal-title-wrapper {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .modal-header-icon {
          color: var(--bright-crime-red);
        }

        .modal-main-title {
          font-family: var(--font-heading);
          font-size: 1.05rem;
          color: var(--text-bright);
          margin: 0;
          letter-spacing: 0.08em;
        }

        .modal-sub-title {
          font-family: var(--font-mono);
          font-size: 0.68rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .modal-close-btn {
          background: transparent;
          border: none;
          color: var(--text-muted);
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 4px;
          border-radius: 4px;
          transition: all var(--transition-fast);
        }

        .modal-close-btn:hover {
          color: var(--text-bright);
          background-color: rgba(255, 255, 255, 0.08);
        }

        .cs-modal-body {
          padding: 20px;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .review-section {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .section-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-family: var(--font-subheading);
          font-size: 0.76rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-primary);
        }

        .sec-title-icon {
          color: var(--bright-crime-red);
        }

        .text-amber {
          color: #f59e0b;
        }

        .details-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
          gap: 10px;
          background-color: var(--bg-primary);
          padding: 14px;
          border-radius: 4px;
          border: 1px solid var(--border-color);
        }

        .detail-item {
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .d-label {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          font-weight: 700;
          color: var(--text-muted);
        }

        .d-value {
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--text-bright);
        }

        .badge-pid {
          color: var(--bright-crime-red);
          font-family: var(--font-mono);
        }

        .status-badge {
          display: inline-block;
          color: #f59e0b;
          font-family: var(--font-mono);
          font-size: 0.75rem;
        }

        .doc-inspect-card {
          display: flex;
          align-items: center;
          justify-content: space-between;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          padding: 12px 16px;
          border-radius: 4px;
        }

        .doc-info {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .doc-icon {
          color: #3b82f6;
        }

        .doc-name {
          font-size: 0.88rem;
          font-weight: 600;
          color: var(--text-bright);
        }

        .doc-desc {
          font-size: 0.72rem;
          color: var(--text-muted);
        }

        .ocr-status-pill {
          margin-left: auto;
          font-family: var(--font-mono);
          font-size: 0.68rem;
          font-weight: 700;
          padding: 2px 8px;
          border-radius: 10px;
        }

        .ocr-status-pill.pass {
          background-color: rgba(40, 167, 69, 0.2);
          border: 1px solid #28a745;
          color: #85e89d;
        }

        .ocr-status-pill.review {
          background-color: rgba(245, 158, 11, 0.2);
          border: 1px solid #f59e0b;
          color: #fde68a;
        }

        .comparison-table-wrapper {
          border: 1px solid var(--border-color);
          border-radius: 4px;
          overflow: hidden;
        }

        .comparison-table {
          width: 100%;
          border-collapse: collapse;
          font-size: 0.8rem;
          background-color: var(--bg-primary);
        }

        .comparison-table th {
          background-color: var(--bg-panel-elevated);
          padding: 10px 12px;
          text-align: left;
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          color: var(--text-muted);
          border-bottom: 1px solid var(--border-color);
        }

        .comparison-table td {
          padding: 10px 12px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.5);
          color: var(--text-bright);
        }

        .field-name {
          font-weight: 600;
          color: var(--text-secondary);
        }

        .detected-val {
          color: #93c5fd;
        }

        .match-pill {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-family: var(--font-mono);
          font-size: 0.68rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
        }

        .match-pill.match {
          background-color: rgba(40, 167, 69, 0.15);
          border: 1px solid #28a745;
          color: #85e89d;
        }

        .match-pill.mismatch {
          background-color: rgba(239, 68, 68, 0.15);
          border: 1px solid #ef4444;
          color: #fca5a5;
        }

        .rejection-box {
          background-color: rgba(239, 68, 68, 0.08);
          border: 1px solid rgba(239, 68, 68, 0.4);
          padding: 16px;
          border-radius: 4px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .rej-title {
          font-family: var(--font-subheading);
          font-size: 0.76rem;
          color: #fca5a5;
          margin: 0;
          letter-spacing: 0.08em;
        }

        .cs-textarea {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 10px;
          color: var(--text-bright);
          font-family: var(--font-body);
          font-size: 0.85rem;
          outline: none;
          resize: vertical;
        }

        .rej-actions {
          display: flex;
          justify-content: flex-end;
          gap: 10px;
        }

        .cs-modal-footer {
          padding: 14px 20px;
          background-color: var(--bg-panel-elevated);
          border-top: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .footer-left {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-mono);
          font-size: 0.65rem;
          color: var(--text-muted);
        }

        .footer-right {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .cs-btn-approve {
          background: linear-gradient(180deg, #28a745 0%, #1e7e34 100%);
          border: 1px solid #28a745;
          border-radius: 4px;
          padding: 8px 16px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 6px;
          transition: all var(--transition-fast);
        }

        .cs-btn-approve:hover:not(:disabled) {
          box-shadow: 0 0 10px rgba(40, 167, 69, 0.4);
          transform: translateY(-1px);
        }

        .cs-btn-reject-outline {
          background: transparent;
          border: 1px solid #ef4444;
          border-radius: 4px;
          padding: 8px 14px;
          color: #fca5a5;
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 6px;
          transition: all var(--transition-fast);
        }

        .cs-btn-reject-outline:hover:not(:disabled) {
          background-color: rgba(239, 68, 68, 0.15);
        }

        .cs-btn-reject {
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
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .auth-alert {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 10px 14px;
          border-radius: 4px;
          font-size: 0.75rem;
          font-weight: 600;
        }

        .auth-alert.error {
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          color: #ff858d;
        }

        .auth-alert.success {
          background-color: rgba(40, 167, 69, 0.15);
          border: 1px solid #28a745;
          color: #85e89d;
        }
      `}</style>
    </div>
  );
}
