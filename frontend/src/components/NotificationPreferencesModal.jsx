import React, { useState, useEffect } from 'react';
import { Settings, Mail, MessageSquare, Bell, ShieldCheck, Check, AlertTriangle, X, RefreshCw } from 'lucide-react';
import { getNotificationPreferences, updateNotificationPreferences } from '../services/api';

export default function NotificationPreferencesModal({ isOpen, onClose }) {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [emailEnabled, setEmailEnabled] = useState(true);
  const [whatsappEnabled, setWhatsappEnabled] = useState(false);
  const [caseCreatedEnabled, setCaseCreatedEnabled] = useState(true);
  const [caseStatusEnabled, setCaseStatusEnabled] = useState(true);
  const [casePriorityEnabled, setCasePriorityEnabled] = useState(true);
  const [aiReviewEnabled, setAiReviewEnabled] = useState(true);
  const [evidenceStatusEnabled, setEvidenceStatusEnabled] = useState(true);

  useEffect(() => {
    if (isOpen) {
      fetchPreferences();
    }
  }, [isOpen]);

  const fetchPreferences = async () => {
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const data = await getNotificationPreferences();
      if (data) {
        setEmailEnabled(data.emailEnabled ?? true);
        setWhatsappEnabled(data.whatsappEnabled ?? false);
        setCaseCreatedEnabled(data.caseCreatedEnabled ?? true);
        setCaseStatusEnabled(data.caseStatusEnabled ?? true);
        setCasePriorityEnabled(data.casePriorityEnabled ?? true);
        setAiReviewEnabled(data.aiReviewEnabled ?? true);
        setEvidenceStatusEnabled(data.evidenceStatusEnabled ?? true);
      }
    } catch (err) {
      setError(err.message || 'Failed to load notification preferences');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      await updateNotificationPreferences({
        emailEnabled,
        whatsappEnabled,
        caseCreatedEnabled,
        caseStatusEnabled,
        casePriorityEnabled,
        aiReviewEnabled,
        evidenceStatusEnabled,
      });
      setSuccess('Notification preferences securely updated.');
      setTimeout(() => {
        setSuccess('');
        onClose();
      }, 1200);
    } catch (err) {
      setError(err.message || 'Failed to update preferences');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="modal-header">
          <div className="modal-title-group">
            <div className="modal-icon-badge">
              <Settings size={20} />
            </div>
            <div>
              <h2 className="modal-title">NOTIFICATION PREFERENCES</h2>
              <p className="modal-subtitle">Configure Alert Channels & Event Triggers</p>
            </div>
          </div>
          <button className="btn-close" onClick={onClose} title="Close">
            <X size={18} />
          </button>
        </div>

        {/* Security Policy Notice Banner */}
        <div className="security-notice-banner">
          <ShieldCheck size={16} className="security-icon" />
          <div className="security-notice-text">
            <strong>MANDATORY COMMAND SECURITY POLICY:</strong> Station Head Login Alerts (<code>LOGIN_ALERT</code>) are strictly enforced by system policy and cannot be silenced.
          </div>
        </div>

        {error && (
          <div className="alert-box alert-error">
            <AlertTriangle size={15} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div className="alert-box alert-success">
            <Check size={15} />
            <span>{success}</span>
          </div>
        )}

        {loading ? (
          <div className="loading-state">
            <RefreshCw size={24} className="spin-icon" />
            <span>Loading preferences...</span>
          </div>
        ) : (
          <form onSubmit={handleSave} className="preferences-form">
            {/* Section 1: Channels */}
            <div className="pref-section">
              <div className="section-title">DELIVERY CHANNELS</div>

              <label className="pref-toggle-card">
                <div className="toggle-left">
                  <div className="channel-icon-badge email-badge">
                    <Mail size={16} />
                  </div>
                  <div>
                    <div className="toggle-label">Email Notifications</div>
                    <div className="toggle-desc">Receive case alerts, AI reviews, and status transitions via official departmental email.</div>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={emailEnabled}
                  onChange={(e) => setEmailEnabled(e.target.checked)}
                  className="cs-switch"
                />
              </label>

              <label className="pref-toggle-card">
                <div className="toggle-left">
                  <div className="channel-icon-badge whatsapp-badge">
                    <MessageSquare size={16} />
                  </div>
                  <div>
                    <div className="toggle-label">WhatsApp Cloud Alerts</div>
                    <div className="toggle-desc">Receive critical high-priority updates directly on configured WhatsApp device.</div>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={whatsappEnabled}
                  onChange={(e) => setWhatsappEnabled(e.target.checked)}
                  className="cs-switch"
                />
              </label>
            </div>

            {/* Section 2: Case Events */}
            <div className="pref-section">
              <div className="section-title">CASE EVENT TRIGGERS</div>

              <label className="pref-toggle-row">
                <div className="row-text">
                  <span className="row-label">Case Created</span>
                  <span className="row-desc">Alert when a new criminal case dossier is initiated at your precinct.</span>
                </div>
                <input
                  type="checkbox"
                  checked={caseCreatedEnabled}
                  onChange={(e) => setCaseCreatedEnabled(e.target.checked)}
                  className="cs-switch"
                />
              </label>

              <label className="pref-toggle-row">
                <div className="row-text">
                  <span className="row-label">Case Status Transitions</span>
                  <span className="row-desc">Alert when status changes (e.g. Under Investigation, Pending Review, Closed).</span>
                </div>
                <input
                  type="checkbox"
                  checked={caseStatusEnabled}
                  onChange={(e) => setCaseStatusEnabled(e.target.checked)}
                  className="cs-switch"
                />
              </label>

              <label className="pref-toggle-row">
                <div className="row-text">
                  <span className="row-label">Case Priority Escalations</span>
                  <span className="row-desc">Immediate alerts when priority escalates to HIGH or CRITICAL.</span>
                </div>
                <input
                  type="checkbox"
                  checked={casePriorityEnabled}
                  onChange={(e) => setCasePriorityEnabled(e.target.checked)}
                  className="cs-switch"
                />
              </label>

              <label className="pref-toggle-row">
                <div className="row-text">
                  <span className="row-label">AI Investigation Reviews</span>
                  <span className="row-desc">Alert when Groq AI produces new findings requiring investigator validation.</span>
                </div>
                <input
                  type="checkbox"
                  checked={aiReviewEnabled}
                  onChange={(e) => setAiReviewEnabled(e.target.checked)}
                  className="cs-switch"
                />
              </label>

              <label className="pref-toggle-row">
                <div className="row-text">
                  <span className="row-label">Evidence Custody & Status</span>
                  <span className="row-desc">Alert when evidence items are verified, verified with lab, or updated.</span>
                </div>
                <input
                  type="checkbox"
                  checked={evidenceStatusEnabled}
                  onChange={(e) => setEvidenceStatusEnabled(e.target.checked)}
                  className="cs-switch"
                />
              </label>
            </div>

            {/* Actions */}
            <div className="modal-actions">
              <button type="button" className="btn-cancel" onClick={onClose} disabled={saving}>
                CANCEL
              </button>
              <button type="submit" className="btn-save" disabled={saving}>
                {saving ? (
                  <>
                    <RefreshCw size={14} className="spin-icon" />
                    <span>SAVING...</span>
                  </>
                ) : (
                  <>
                    <Check size={14} />
                    <span>SAVE PREFERENCES</span>
                  </>
                )}
              </button>
            </div>
          </form>
        )}

        <style>{`
          .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.82);
            backdrop-filter: blur(5px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1000;
            padding: 20px;
          }

          .modal-container {
            background-color: var(--bg-panel);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            width: 100%;
            max-width: 580px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.8), var(--red-glow-subtle);
            overflow: hidden;
            display: flex;
            flex-direction: column;
            max-height: 90vh;
          }

          .modal-header {
            padding: 16px 20px;
            border-bottom: 1px solid var(--border-color);
            display: flex;
            align-items: center;
            justify-content: space-between;
            background-color: var(--bg-secondary);
          }

          .modal-title-group {
            display: flex;
            align-items: center;
            gap: 12px;
          }

          .modal-icon-badge {
            background-color: rgba(193, 18, 31, 0.15);
            border: 1px solid var(--bright-crime-red);
            color: var(--bright-crime-red);
            padding: 8px;
            border-radius: 6px;
            display: flex;
          }

          .modal-title {
            font-family: var(--font-heading);
            font-size: 1.05rem;
            font-weight: 700;
            color: var(--text-bright);
            letter-spacing: 0.05em;
            margin: 0;
          }

          .modal-subtitle {
            font-size: 0.72rem;
            color: var(--text-muted);
            margin: 0;
          }

          .btn-close {
            background: transparent;
            border: none;
            color: var(--text-muted);
            cursor: pointer;
            padding: 6px;
            border-radius: 4px;
            transition: all var(--transition-fast);
          }

          .btn-close:hover {
            color: var(--text-bright);
            background-color: rgba(255, 255, 255, 0.05);
          }

          .security-notice-banner {
            background-color: rgba(26, 35, 126, 0.25);
            border-bottom: 1px solid rgba(63, 81, 181, 0.35);
            padding: 10px 16px;
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 0.72rem;
            color: #9fa8da;
          }

          .security-icon {
            color: #5c6bc0;
            flex-shrink: 0;
          }

          .security-notice-text code {
            background: rgba(0, 0, 0, 0.35);
            padding: 1px 4px;
            border-radius: 3px;
            color: #c5cae9;
          }

          .alert-box {
            padding: 10px 16px;
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 0.8rem;
          }

          .alert-error {
            background-color: rgba(193, 18, 31, 0.15);
            color: #ff8a80;
            border-bottom: 1px solid rgba(193, 18, 31, 0.3);
          }

          .alert-success {
            background-color: rgba(46, 125, 50, 0.15);
            color: #a5d6a7;
            border-bottom: 1px solid rgba(46, 125, 50, 0.3);
          }

          .loading-state {
            padding: 40px;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 12px;
            color: var(--text-muted);
            font-size: 0.85rem;
          }

          .spin-icon {
            animation: spin 1s linear infinite;
          }

          @keyframes spin {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
          }

          .preferences-form {
            padding: 16px 20px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 18px;
          }

          .pref-section {
            display: flex;
            flex-direction: column;
            gap: 10px;
          }

          .section-title {
            font-family: var(--font-subheading);
            font-size: 0.7rem;
            font-weight: 700;
            color: var(--text-muted);
            letter-spacing: 0.12em;
          }

          .pref-toggle-card {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px 14px;
            background-color: var(--bg-secondary);
            border: 1px solid var(--border-color);
            border-radius: 6px;
            cursor: pointer;
            transition: all var(--transition-fast);
          }

          .pref-toggle-card:hover {
            border-color: rgba(193, 18, 31, 0.4);
            background-color: rgba(193, 18, 31, 0.04);
          }

          .toggle-left {
            display: flex;
            align-items: center;
            gap: 12px;
          }

          .channel-icon-badge {
            padding: 8px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
          }

          .email-badge {
            background-color: rgba(30, 136, 229, 0.15);
            border: 1px solid rgba(30, 136, 229, 0.4);
            color: #64b5f6;
          }

          .whatsapp-badge {
            background-color: rgba(46, 125, 50, 0.15);
            border: 1px solid rgba(46, 125, 50, 0.4);
            color: #81c784;
          }

          .toggle-label {
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text-bright);
          }

          .toggle-desc {
            font-size: 0.72rem;
            color: var(--text-muted);
            margin-top: 2px;
            max-width: 400px;
          }

          .pref-toggle-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 12px;
            background-color: rgba(18, 18, 20, 0.5);
            border: 1px solid rgba(42, 42, 45, 0.5);
            border-radius: 4px;
            cursor: pointer;
            transition: all var(--transition-fast);
          }

          .pref-toggle-row:hover {
            background-color: rgba(42, 42, 45, 0.4);
          }

          .row-text {
            display: flex;
            flex-direction: column;
          }

          .row-label {
            font-size: 0.82rem;
            font-weight: 600;
            color: var(--text-primary);
          }

          .row-desc {
            font-size: 0.7rem;
            color: var(--text-muted);
          }

          .cs-switch {
            appearance: none;
            width: 38px;
            height: 20px;
            background-color: #27272a;
            border: 1px solid #3f3f46;
            border-radius: 12px;
            position: relative;
            cursor: pointer;
            outline: none;
            transition: all 0.2s ease;
            flex-shrink: 0;
          }

          .cs-switch:checked {
            background-color: var(--bright-crime-red);
            border-color: var(--bright-crime-red);
            box-shadow: var(--red-glow-subtle);
          }

          .cs-switch::before {
            content: '';
            position: absolute;
            width: 14px;
            height: 14px;
            border-radius: 50%;
            top: 2px;
            left: 2px;
            background-color: white;
            transition: all 0.2s ease;
          }

          .cs-switch:checked::before {
            left: 20px;
          }

          .modal-actions {
            display: flex;
            align-items: center;
            justify-content: flex-end;
            gap: 10px;
            margin-top: 10px;
            padding-top: 14px;
            border-top: 1px solid var(--border-color);
          }

          .btn-cancel {
            padding: 8px 16px;
            background-color: transparent;
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.75rem;
            font-weight: 600;
            letter-spacing: 0.05em;
            transition: all var(--transition-fast);
          }

          .btn-cancel:hover {
            background-color: rgba(255, 255, 255, 0.05);
            color: var(--text-bright);
          }

          .btn-save {
            padding: 8px 16px;
            background-color: var(--bright-crime-red);
            border: 1px solid var(--bright-crime-red);
            color: white;
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.75rem;
            font-weight: 600;
            letter-spacing: 0.05em;
            display: flex;
            align-items: center;
            gap: 6px;
            box-shadow: var(--red-glow-subtle);
            transition: all var(--transition-fast);
          }

          .btn-save:hover:not(:disabled) {
            background-color: var(--color-danger-hover);
          }

          .btn-save:disabled {
            opacity: 0.5;
            cursor: not-allowed;
          }
        `}</style>
      </div>
    </div>
  );
}
