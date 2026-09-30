import React from 'react';
import { History, X, Shield, Clock, UserCheck } from 'lucide-react';

export default function AuditLogsModal({ isOpen, onClose }) {
  if (!isOpen) return null;

  const mockAuditEntries = [
    {
      id: '1',
      action: 'CASE_ACCESS',
      actor: 'Inspector K. Raman (TN-POL-1001)',
      target: 'Case CS-2026-0001 (Homicide at Gandhi Park)',
      timestamp: new Date(Date.now() - 1000 * 60 * 15).toLocaleString(),
      status: 'AUTHORIZED',
      ip: '10.128.4.22'
    },
    {
      id: '2',
      action: 'EVIDENCE_VERIFIED',
      actor: 'Forensic Officer D. Selvam (TN-POL-1003)',
      target: 'Item EVD-001 (Post-Mortem Autopsy Report)',
      timestamp: new Date(Date.now() - 1000 * 60 * 65).toLocaleString(),
      status: 'VERIFIED_HASH_MATCH',
      ip: '10.128.4.18'
    },
    {
      id: '3',
      action: 'AI_INVESTIGATION_RUN',
      actor: 'CrimeSphere Reasoning Agent v2.4',
      target: 'Lead Evaluation for CS-2026-0001',
      timestamp: new Date(Date.now() - 1000 * 60 * 180).toLocaleString(),
      status: 'COMPLETED',
      ip: '127.0.0.1'
    },
    {
      id: '4',
      action: 'WITNESS_STATEMENT_LOGGED',
      actor: 'Inspector K. Raman (TN-POL-1001)',
      target: 'Witness R. Natarajan (Eyewitness Testimony)',
      timestamp: new Date(Date.now() - 1000 * 60 * 320).toLocaleString(),
      status: 'SEALED_EVIDENCE',
      ip: '10.128.4.22'
    },
    {
      id: '5',
      action: 'STATION_ISOLATION_CHECK',
      actor: 'Security Enforcer Daemon',
      target: 'Pre-flight Tenant Security Check (TN-POLICE-CBE)',
      timestamp: new Date(Date.now() - 1000 * 60 * 720).toLocaleString(),
      status: 'PASSED',
      ip: 'INTERNAL'
    }
  ];

  return (
    <div className="audit-modal-backdrop" onClick={onClose}>
      <div className="audit-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="audit-header">
          <div className="audit-title-wrap">
            <History size={20} className="audit-icon" />
            <div>
              <div className="audit-title">STATION AUDIT TRAIL & INTEGRITY LOGS</div>
              <div className="audit-subtitle">Immutable chronological activity records for chain of custody and forensic oversight</div>
            </div>
          </div>
          <button className="close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="audit-body">
          <div className="audit-table-wrap">
            <table className="audit-table">
              <thead>
                <tr>
                  <th>TIMESTAMP</th>
                  <th>ACTION</th>
                  <th>OFFICER / ACTOR</th>
                  <th>TARGET RECORD</th>
                  <th>STATUS</th>
                  <th>ORIGIN IP</th>
                </tr>
              </thead>
              <tbody>
                {mockAuditEntries.map((log) => (
                  <tr key={log.id}>
                    <td className="log-time">{log.timestamp}</td>
                    <td>
                      <span className="log-action-tag">{log.action}</span>
                    </td>
                    <td className="log-actor">{log.actor}</td>
                    <td className="log-target">{log.target}</td>
                    <td>
                      <span className="log-status-badge">{log.status}</span>
                    </td>
                    <td className="log-ip">{log.ip}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="audit-footer">
          <div className="footer-note">
            <Shield size={14} className="shield-icon" />
            <span>Chain-of-custody cryptographic verification: 256-bit SHA digest sealed</span>
          </div>
          <button className="close-action-btn" onClick={onClose}>
            CLOSE LOGS
          </button>
        </div>

        <style>{`
          .audit-modal-backdrop {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.75);
            backdrop-filter: blur(4px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
            padding: 20px;
          }
          .audit-modal-content {
            background: #121418;
            border: 1px solid #2a2e39;
            border-radius: 8px;
            width: 100%;
            max-width: 960px;
            max-height: 85vh;
            display: flex;
            flex-direction: column;
            box-shadow: 0 12px 36px rgba(0, 0, 0, 0.8), 0 0 20px rgba(193, 18, 31, 0.15);
            animation: modalFadeIn 0.2s ease-out;
          }
          @keyframes modalFadeIn {
            from { opacity: 0; transform: scale(0.97); }
            to { opacity: 1; transform: scale(1); }
          }
          .audit-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 16px 20px;
            border-bottom: 1px solid #232733;
            background: #161920;
          }
          .audit-title-wrap {
            display: flex;
            align-items: center;
            gap: 12px;
          }
          .audit-icon {
            color: var(--bright-crime-red, #e63946);
          }
          .audit-title {
            font-family: var(--font-heading, monospace);
            font-size: 0.95rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            color: #f1f3f9;
          }
          .audit-subtitle {
            font-size: 0.75rem;
            color: #8c93a8;
          }
          .close-btn {
            background: transparent;
            border: none;
            color: #8c93a8;
            cursor: pointer;
            padding: 4px;
            border-radius: 4px;
            transition: all 0.15s;
          }
          .close-btn:hover {
            color: #fff;
            background: #252a38;
          }
          .audit-body {
            padding: 20px;
            overflow-y: auto;
            flex-grow: 1;
          }
          .audit-table-wrap {
            overflow-x: auto;
            border: 1px solid #232733;
            border-radius: 6px;
          }
          .audit-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 0.8rem;
          }
          .audit-table th {
            background: #181c24;
            color: #8c93a8;
            font-weight: 600;
            text-align: left;
            padding: 10px 14px;
            border-bottom: 1px solid #232733;
            font-family: var(--font-subheading, monospace);
            font-size: 0.72rem;
            letter-spacing: 0.06em;
          }
          .audit-table td {
            padding: 12px 14px;
            border-bottom: 1px solid #1c202a;
            color: #d0d5e2;
          }
          .audit-table tr:hover td {
            background: rgba(255, 255, 255, 0.02);
          }
          .log-time {
            font-family: var(--font-mono, monospace);
            color: #8c93a8;
            font-size: 0.75rem;
            white-space: nowrap;
          }
          .log-action-tag {
            background: rgba(30, 41, 59, 0.8);
            color: #60a5fa;
            border: 1px solid rgba(96, 165, 250, 0.3);
            border-radius: 3px;
            padding: 2px 6px;
            font-family: var(--font-mono, monospace);
            font-size: 0.7rem;
            font-weight: 600;
          }
          .log-actor {
            font-weight: 500;
            color: #e2e8f0;
          }
          .log-target {
            color: #94a3b8;
          }
          .log-status-badge {
            background: rgba(16, 185, 129, 0.12);
            color: #10b981;
            border: 1px solid rgba(16, 185, 129, 0.3);
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 0.68rem;
            font-weight: 600;
            font-family: var(--font-mono, monospace);
          }
          .log-ip {
            font-family: var(--font-mono, monospace);
            color: #64748b;
            font-size: 0.72rem;
          }
          .audit-footer {
            padding: 14px 20px;
            background: #161920;
            border-top: 1px solid #232733;
            display: flex;
            justify-content: space-between;
            align-items: center;
          }
          .footer-note {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 0.75rem;
            color: #64748b;
          }
          .shield-icon {
            color: #10b981;
          }
          .close-action-btn {
            background: #252a38;
            color: #f1f3f9;
            border: 1px solid #333a4d;
            padding: 7px 16px;
            border-radius: 4px;
            font-size: 0.78rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.15s;
          }
          .close-action-btn:hover {
            background: #333a4d;
          }
        `}</style>
      </div>
    </div>
  );
}
