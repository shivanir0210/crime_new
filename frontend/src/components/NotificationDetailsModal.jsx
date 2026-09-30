import React, { useState } from 'react';
import { X, Mail, RefreshCw, AlertTriangle, CheckCircle, Clock, Info, ShieldAlert, Send } from 'lucide-react';
import { retryNotification } from '../services/api';

export default function NotificationDetailsModal({ isOpen, onClose, notification, onRetrySuccess }) {
  if (!isOpen || !notification) return null;

  const [currentNotif, setCurrentNotif] = useState(notification);
  const [isRetrying, setIsRetrying] = useState(false);
  const [retryFeedback, setRetryFeedback] = useState(null);

  const isFailed = currentNotif.status === 'FAILED';
  const isSent = currentNotif.status === 'SENT' || currentNotif.status === 'MOCK_SENT';
  const recipientEmail = currentNotif.recipientEmail || 'N/A';
  const lastErrorMsg = currentNotif.lastError || 'Email provider unconfigured or SMTP connection failed';

  const handleRetry = async () => {
    if (isRetrying) return;
    setIsRetrying(true);
    setRetryFeedback(null);
    try {
      const updated = await retryNotification(currentNotif.id);
      setCurrentNotif(updated);
      if (updated.status === 'SENT' || updated.status === 'MOCK_SENT') {
        setRetryFeedback({ type: 'success', text: 'Email notification re-dispatched successfully!' });
      } else {
        setRetryFeedback({ type: 'error', text: `Retry completed, but status remains FAILED: ${updated.lastError || 'Unknown error'}` });
      }
      if (onRetrySuccess) {
        onRetrySuccess(updated);
      }
    } catch (err) {
      setRetryFeedback({ type: 'error', text: `Retry request failed: ${err.message || 'Server error'}` });
    } finally {
      setIsRetrying(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-container notif-details-modal-container" onClick={(e) => e.stopPropagation()}>
        {/* Modal Header */}
        <div className="modal-header notif-header">
          <div className="header-title-group">
            <div className="notif-avatar-box">
              <Mail size={20} className={isFailed ? 'text-crimson' : 'text-blue-400'} />
            </div>
            <div>
              <div className="header-title-row">
                <h3 className="modal-title">NOTIFICATION DELIVERY DETAILS</h3>
                <span className="dev-test-badge">Development/Test Email</span>
              </div>
              <p className="modal-subtitle">Channel Delivery & SMTP Status Log</p>
            </div>
          </div>
          <button onClick={onClose} className="modal-close-btn" title="Close">
            <X size={18} />
          </button>
        </div>

        {/* Status Banner */}
        {isFailed ? (
          <div className="notif-status-banner banner-failed">
            <AlertTriangle size={18} className="banner-icon-failed" />
            <div className="banner-text">
              <div className="banner-title">EMAIL DELIVERY FAILED</div>
              <div className="banner-error-msg">{lastErrorMsg}</div>
            </div>
          </div>
        ) : isSent ? (
          <div className="notif-status-banner banner-success">
            <CheckCircle size={18} className="banner-icon-success" />
            <div className="banner-text">
              <div className="banner-title">EMAIL DELIVERED SUCCESSFULLY</div>
              <div className="banner-subtext">Dispatched to mail provider queue.</div>
            </div>
          </div>
        ) : (
          <div className="notif-status-banner banner-sending">
            <RefreshCw size={18} className="spin-icon text-amber-400" />
            <div className="banner-text">
              <div className="banner-title">DISPATCH IN PROGRESS</div>
              <div className="banner-subtext">System is attempting delivery.</div>
            </div>
          </div>
        )}

        {/* Feedback Alert if Retry Triggered */}
        {retryFeedback && (
          <div className={`retry-feedback-banner ${retryFeedback.type === 'success' ? 'feedback-success' : 'feedback-error'}`}>
            {retryFeedback.type === 'success' ? <CheckCircle size={15} /> : <ShieldAlert size={15} />}
            <span>{retryFeedback.text}</span>
          </div>
        )}

        {/* Details Grid */}
        <div className="notif-details-grid">
          <div className="details-card">
            <span className="details-label">Channel</span>
            <span className="details-value-pill pill-channel">
              <Mail size={12} />
              {currentNotif.channel || 'EMAIL'}
            </span>
          </div>

          <div className="details-card">
            <span className="details-label">Delivery Status</span>
            <span className={`details-value-pill pill-status-${(currentNotif.status || '').toLowerCase()}`}>
              {currentNotif.status}
            </span>
          </div>

          <div className="details-card">
            <span className="details-label">Recipient Address</span>
            <span className="details-value mono-text" title={recipientEmail}>{recipientEmail}</span>
          </div>

          <div className="details-card">
            <span className="details-label">Case Number</span>
            <span className="details-value case-tag">{currentNotif.caseNumber || 'N/A'}</span>
          </div>

          <div className="details-card">
            <span className="details-label">Attempt Count</span>
            <span className="details-value">{currentNotif.attemptCount || 1}</span>
          </div>

          <div className="details-card">
            <span className="details-label">Created At</span>
            <span className="details-value">{formatDate(currentNotif.createdAt)}</span>
          </div>
        </div>

        {/* Message Subject & Body */}
        <div className="notif-content-box">
          <div className="content-box-header">
            <Info size={14} />
            <span>NOTIFICATION SUBJECT & MESSAGE</span>
          </div>
          <div className="content-subject">{currentNotif.subject || 'Case Alert'}</div>
          {currentNotif.message && (
            <div className="content-message">{currentNotif.message}</div>
          )}
        </div>

        {/* Actions Footer */}
        <div className="modal-actions-footer">
          {currentNotif.channel === 'EMAIL' && (
            <button
              className={`btn-retry-email ${isRetrying ? 'btn-disabled' : ''}`}
              onClick={handleRetry}
              disabled={isRetrying}
              title="Re-send email to mail server"
            >
              <RefreshCw size={14} className={isRetrying ? 'spin-icon' : ''} />
              <span>{isRetrying ? 'Retrying Email...' : 'Retry Email'}</span>
            </button>
          )}

          <button className="btn-secondary-close" onClick={onClose}>
            Close
          </button>
        </div>
      </div>

      <style>{`
        .notif-details-modal-container {
          max-width: 520px;
          border: 1px solid var(--border-color);
          background: var(--bg-panel);
          border-radius: 8px;
          overflow: hidden;
          box-shadow: 0 12px 36px rgba(0, 0, 0, 0.8), var(--red-glow-subtle);
        }

        .notif-header {
          padding: 14px 18px;
          background-color: var(--bg-secondary);
          border-bottom: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .header-title-row {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;
        }

        .dev-test-badge {
          background-color: rgba(30, 136, 229, 0.2);
          border: 1px solid rgba(30, 136, 229, 0.5);
          color: #64b5f6;
          font-size: 0.62rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 4px;
          text-transform: uppercase;
          letter-spacing: 0.04em;
        }

        .notif-avatar-box {
          background-color: rgba(30, 136, 229, 0.15);
          border: 1px solid rgba(30, 136, 229, 0.3);
          padding: 8px;
          border-radius: 6px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .text-crimson { color: var(--bright-crime-red); }

        .notif-status-banner {
          margin: 16px 18px 0;
          padding: 12px 14px;
          border-radius: 6px;
          display: flex;
          align-items: flex-start;
          gap: 10px;
          font-size: 0.78rem;
        }

        .banner-failed {
          background: rgba(193, 18, 31, 0.12);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: #ff8a80;
        }

        .banner-icon-failed {
          color: var(--bright-crime-red);
          flex-shrink: 0;
          margin-top: 2px;
        }

        .banner-success {
          background: rgba(46, 125, 50, 0.12);
          border: 1px solid rgba(46, 125, 50, 0.4);
          color: #a5d6a7;
        }

        .banner-icon-success {
          color: var(--color-success);
          flex-shrink: 0;
          margin-top: 2px;
        }

        .banner-sending {
          background: rgba(255, 179, 0, 0.12);
          border: 1px solid rgba(255, 179, 0, 0.4);
          color: #ffe082;
        }

        .banner-title {
          font-weight: 700;
          letter-spacing: 0.05em;
          font-family: var(--font-subheading);
          margin-bottom: 2px;
        }

        .banner-error-msg {
          font-size: 0.72rem;
          opacity: 0.9;
          word-break: break-word;
          line-height: 1.3;
        }

        .banner-subtext {
          font-size: 0.72rem;
          opacity: 0.85;
        }

        .retry-feedback-banner {
          margin: 12px 18px 0;
          padding: 8px 12px;
          border-radius: 4px;
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 0.75rem;
          font-weight: 600;
        }

        .feedback-success {
          background: rgba(46, 125, 50, 0.2);
          border: 1px solid var(--color-success);
          color: #a5d6a7;
        }

        .feedback-error {
          background: rgba(193, 18, 31, 0.2);
          border: 1px solid var(--bright-crime-red);
          color: #ff8a80;
        }

        .notif-details-grid {
          margin: 16px 18px;
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 10px;
        }

        .details-card {
          background: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          padding: 8px 12px;
          border-radius: 5px;
          display: flex;
          flex-direction: column;
          gap: 3px;
        }

        .details-label {
          font-size: 0.65rem;
          color: var(--text-muted);
          text-transform: uppercase;
          letter-spacing: 0.05em;
        }

        .details-value {
          font-size: 0.78rem;
          color: var(--text-bright);
          font-weight: 600;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .mono-text {
          font-family: var(--font-mono);
          color: #64b5f6;
        }

        .case-tag {
          color: var(--bright-crime-red);
          font-family: var(--font-subheading);
        }

        .details-value-pill {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: 0.68rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
          width: fit-content;
          text-transform: uppercase;
        }

        .pill-channel {
          background-color: rgba(30, 136, 229, 0.15);
          color: #64b5f6;
          border: 1px solid rgba(30, 136, 229, 0.3);
        }

        .pill-status-sent {
          background-color: rgba(46, 125, 50, 0.2);
          color: #a5d6a7;
          border: 1px solid rgba(46, 125, 50, 0.4);
        }

        .pill-status-failed {
          background-color: rgba(193, 18, 31, 0.2);
          color: #ff8a80;
          border: 1px solid rgba(193, 18, 31, 0.4);
        }

        .pill-status-retrying, .pill-status-sending {
          background-color: rgba(255, 179, 0, 0.2);
          color: #ffe082;
          border: 1px solid rgba(255, 179, 0, 0.4);
        }

        .notif-content-box {
          margin: 0 18px 16px;
          background: rgba(18, 18, 20, 0.8);
          border: 1px solid var(--border-color);
          border-radius: 6px;
          padding: 12px;
        }

        .content-box-header {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.68rem;
          color: var(--text-muted);
          font-weight: 700;
          letter-spacing: 0.05em;
          margin-bottom: 6px;
        }

        .content-subject {
          font-size: 0.8rem;
          font-weight: 700;
          color: var(--text-bright);
          margin-bottom: 4px;
        }

        .content-message {
          font-size: 0.74rem;
          color: var(--text-secondary);
          line-height: 1.4;
          white-space: pre-wrap;
          word-break: break-word;
        }

        .modal-actions-footer {
          padding: 12px 18px;
          background-color: var(--bg-secondary);
          border-top: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: flex-end;
          gap: 10px;
        }

        .btn-retry-email {
          background-color: var(--bright-crime-red);
          color: white;
          border: none;
          padding: 7px 14px;
          border-radius: 4px;
          font-size: 0.78rem;
          font-weight: 700;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 6px;
          transition: background var(--transition-fast);
        }

        .btn-retry-email:hover:not(:disabled) {
          background-color: #d90429;
          box-shadow: var(--red-glow-subtle);
        }

        .btn-disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .btn-secondary-close {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          color: var(--text-secondary);
          padding: 7px 14px;
          border-radius: 4px;
          font-size: 0.78rem;
          font-weight: 600;
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .btn-secondary-close:hover {
          color: var(--text-bright);
          border-color: var(--text-muted);
        }

        .spin-icon {
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
