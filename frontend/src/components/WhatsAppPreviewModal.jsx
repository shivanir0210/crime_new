import React from 'react';
import { X, CheckCheck, Smartphone, AlertTriangle, ShieldCheck } from 'lucide-react';

export default function WhatsAppPreviewModal({ isOpen, onClose, notification }) {
  if (!isOpen || !notification) return null;

  const recipientPhone = notification.recipientPhone || '+91 9000000002';
  const recipientName = notification.recipientName || 'Assigned Officer';
  const isMock = notification.status === 'MOCK_SENT' || (notification.lastError && notification.lastError.includes('MOCK'));

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-container wa-preview-modal-container" onClick={(e) => e.stopPropagation()}>
        {/* Modal Header */}
        <div className="modal-header wa-header">
          <div className="header-title-group">
            <div className="wa-avatar-box">
              <Smartphone size={20} className="text-emerald-400" />
            </div>
            <div>
              <h3 className="modal-title text-emerald-400">WHATSAPP NOTIFICATION PREVIEW</h3>
              <p className="modal-subtitle">Simulated Cloud API Message Delivery</p>
            </div>
          </div>
          <button onClick={onClose} className="modal-close-btn">
            <X size={18} />
          </button>
        </div>

        {/* Development Mock Warning Banner */}
        <div className="wa-mock-banner">
          <AlertTriangle size={16} className="text-amber-400 shrink-0" />
          <div className="wa-mock-text">
            <strong>DEVELOPMENT MOCK — NOT ACTUALLY SENT</strong>
            <span>This is a simulated message preview stored during mock development testing (`WHATSAPP_MOCK_ENABLED=true`). No external API costs incurred.</span>
          </div>
        </div>

        {/* Phone Container Screen */}
        <div className="wa-phone-screen">
          <div className="wa-chat-header">
            <div className="wa-chat-contact-info">
              <div className="wa-contact-avatar">CS</div>
              <div>
                <div className="wa-contact-name">CrimeSphere AI Intelligence</div>
                <div className="wa-contact-number">To: {recipientPhone} ({recipientName})</div>
              </div>
            </div>
            <div className="wa-status-pill-badge">
              <span className="wa-status-dot"></span>
              <span>{notification.status}</span>
            </div>
          </div>

          <div className="wa-chat-body">
            <div className="wa-bubble">
              <div className="wa-bubble-header">
                {notification.subject || '🔎 NEW FORENSIC CLUE'}
              </div>

              <div className="wa-bubble-content">
                {notification.message ? (
                  <pre className="wa-message-pre">{notification.message}</pre>
                ) : (
                  <div>
                    <p>A new forensic report has been added to <strong>Case {notification.caseNumber || 'CS-2026-0100'}</strong>.</p>
                    <p className="mt-2"><strong>Evidence:</strong> {notification.referenceId || 'EVD-CBE-2026-0100'}</p>
                    <p><strong>Clue:</strong> Fingerprint identified on recovered object.</p>
                    <p className="mt-2 text-xs opacity-75">Please review the evidence in CrimeSphere AI.</p>
                  </div>
                )}
              </div>

              <div className="wa-bubble-footer">
                <span className="wa-timestamp">
                  {notification.createdAt ? new Date(notification.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
                </span>
                <CheckCheck size={14} className="text-sky-400" />
              </div>
            </div>
          </div>
        </div>

        {/* Delivery Details Footer */}
        <div className="wa-meta-footer">
          <div className="meta-row">
            <span>Channel:</span> <strong>WHATSAPP CLOUD API (MOCK)</strong>
          </div>
          <div className="meta-row">
            <span>Recipient:</span> <strong>{recipientPhone}</strong>
          </div>
          <div className="meta-row">
            <span>Status:</span> <strong className="text-emerald-400">{notification.status}</strong>
          </div>
          <div className="meta-row">
            <span>Message ID:</span> <span className="mono-text">{notification.externalMessageId || 'mock-wa-12345'}</span>
          </div>
        </div>
      </div>

      <style>{`
        .wa-preview-modal-container {
          max-width: 500px;
          border: 1px solid rgba(16, 185, 129, 0.3);
          background: #111827;
        }

        .wa-header {
          border-bottom: 1px solid rgba(16, 185, 129, 0.2);
        }

        .wa-avatar-box {
          background-color: rgba(16, 185, 129, 0.15);
          border: 1px solid rgba(16, 185, 129, 0.4);
          padding: 8px;
          border-radius: 6px;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .wa-mock-banner {
          background: rgba(245, 158, 11, 0.12);
          border: 1px solid rgba(245, 158, 11, 0.3);
          border-radius: 6px;
          padding: 10px 14px;
          margin: 16px 20px 0;
          display: flex;
          align-items: flex-start;
          gap: 10px;
          font-size: 0.75rem;
          color: #fef08a;
        }

        .wa-mock-text {
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .wa-mock-text strong {
          color: #fbbf24;
          letter-spacing: 0.05em;
        }

        .wa-phone-screen {
          margin: 16px 20px;
          background: #0b141a;
          border: 1px solid rgba(255, 255, 255, 0.1);
          border-radius: 12px;
          overflow: hidden;
          box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.8);
        }

        .wa-chat-header {
          background: #202c33;
          padding: 10px 14px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        }

        .wa-chat-contact-info {
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .wa-contact-avatar {
          width: 32px;
          height: 32px;
          background: #00a884;
          color: white;
          font-weight: 700;
          font-size: 0.75rem;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .wa-contact-name {
          font-size: 0.8rem;
          font-weight: 700;
          color: #e9edef;
        }

        .wa-contact-number {
          font-size: 0.68rem;
          color: #8696a0;
        }

        .wa-status-pill-badge {
          background: rgba(16, 185, 129, 0.2);
          border: 1px solid rgba(16, 185, 129, 0.4);
          color: #34d399;
          font-size: 0.65rem;
          font-weight: 700;
          padding: 2px 8px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          gap: 4px;
        }

        .wa-status-dot {
          width: 6px;
          height: 6px;
          background: #34d399;
          border-radius: 50%;
        }

        .wa-chat-body {
          padding: 16px;
          background-image: radial-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px);
          background-size: 16px 16px;
          min-height: 180px;
          display: flex;
          flex-direction: column;
          justify-content: flex-end;
        }

        .wa-bubble {
          background: #005c4b;
          color: #e9edef;
          border-radius: 8px 8px 0 8px;
          padding: 12px;
          max-width: 90%;
          align-self: flex-end;
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
          font-size: 0.8rem;
        }

        .wa-bubble-header {
          font-weight: 700;
          color: #6ee7b7;
          margin-bottom: 6px;
          font-size: 0.85rem;
        }

        .wa-message-pre {
          font-family: inherit;
          white-space: pre-wrap;
          word-break: break-word;
          font-size: 0.78rem;
          line-height: 1.4;
          margin: 0;
        }

        .wa-bubble-footer {
          display: flex;
          align-items: center;
          justify-content: flex-end;
          gap: 4px;
          margin-top: 8px;
          font-size: 0.65rem;
          color: rgba(241, 245, 249, 0.6);
        }

        .wa-meta-footer {
          padding: 12px 20px;
          background: #1f2937;
          border-top: 1px solid rgba(255, 255, 255, 0.05);
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 8px;
          font-size: 0.72rem;
          color: #9ca3af;
        }

        .meta-row {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .mono-text {
          font-family: monospace;
          color: #60a5fa;
        }
      `}</style>
    </div>
  );
}
