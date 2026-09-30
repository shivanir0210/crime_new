import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BrainCircuit, GitCompare, X, Briefcase, ChevronRight, Loader2 } from 'lucide-react';
import { getCases } from '../services/api';

export default function CaseSelectorModal({ isOpen, onClose, mode = 'AI_INVESTIGATION' }) {
  const navigate = useNavigate();
  const [cases, setCases] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isOpen) {
      setLoading(true);
      getCases({ size: 10, sort: 'updatedAt,desc' })
        .then((res) => {
          setCases(res?.content || []);
        })
        .catch((err) => {
          console.error('Failed to load cases in selector modal:', err);
        })
        .finally(() => setLoading(false));
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const isAi = mode === 'AI_INVESTIGATION';
  const title = isAi ? 'LAUNCH AI INVESTIGATION' : 'SIMILAR CASES RAG SEARCH';
  const subtitle = isAi 
    ? 'Select an active case workspace to run neural case reasoning, hypothesis analysis, and evidence checks'
    : 'Select a target case to retrieve cross-case intelligence, similar modus operandi, and knowledge base matches';
  const Icon = isAi ? BrainCircuit : GitCompare;
  const targetTab = isAi ? 'AI_INVESTIGATION' : 'OVERVIEW';

  const handleSelectCase = (caseId) => {
    onClose();
    navigate(`/cases/${caseId}?tab=${targetTab}`);
  };

  return (
    <div className="selector-modal-backdrop" onClick={onClose}>
      <div className="selector-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="selector-header">
          <div className="selector-title-wrap">
            <Icon size={22} className="selector-icon" />
            <div>
              <div className="selector-title">{title}</div>
              <div className="selector-subtitle">{subtitle}</div>
            </div>
          </div>
          <button className="close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="selector-body">
          {loading ? (
            <div className="loading-state">
              <Loader2 size={24} className="spin-icon" />
              <span>Loading station cases...</span>
            </div>
          ) : cases.length === 0 ? (
            <div className="empty-state">
              <Briefcase size={32} className="empty-icon" />
              <div className="empty-text">No cases currently registered in your station.</div>
            </div>
          ) : (
            <div className="case-list">
              {cases.map((c) => (
                <div 
                  key={c.id} 
                  className="case-item"
                  onClick={() => handleSelectCase(c.id)}
                >
                  <div className="case-item-info">
                    <div className="case-num-line">
                      <span className="case-num">{c.caseNumber}</span>
                      <span className={`case-badge badge-${(c.priority || 'MEDIUM').toLowerCase()}`}>
                        {c.priority}
                      </span>
                      <span className="case-type">{(c.caseType || 'OTHER').replace(/_/g, ' ')}</span>
                    </div>
                    <div className="case-title">{c.title}</div>
                  </div>
                  <div className="action-col">
                    <span className="select-text">ENTER WORKSPACE</span>
                    <ChevronRight size={16} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="selector-footer">
          <span>Click a case to enter its dedicated investigation workspace</span>
          <button className="cancel-btn" onClick={onClose}>CANCEL</button>
        </div>

        <style>{`
          .selector-modal-backdrop {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.8);
            backdrop-filter: blur(4px);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
            padding: 20px;
          }
          .selector-modal-content {
            background: #121418;
            border: 1px solid #2a2e39;
            border-radius: 8px;
            width: 100%;
            max-width: 680px;
            max-height: 80vh;
            display: flex;
            flex-direction: column;
            box-shadow: 0 12px 36px rgba(0, 0, 0, 0.85);
          }
          .selector-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 16px 20px;
            border-bottom: 1px solid #232733;
            background: #161920;
          }
          .selector-title-wrap {
            display: flex;
            align-items: center;
            gap: 12px;
          }
          .selector-icon {
            color: var(--bright-crime-red, #e63946);
          }
          .selector-title {
            font-family: var(--font-heading, monospace);
            font-size: 0.95rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            color: #f1f3f9;
          }
          .selector-subtitle {
            font-size: 0.75rem;
            color: #8c93a8;
            margin-top: 2px;
          }
          .close-btn {
            background: transparent;
            border: none;
            color: #8c93a8;
            cursor: pointer;
            padding: 4px;
            border-radius: 4px;
          }
          .close-btn:hover {
            color: #fff;
            background: #252a38;
          }
          .selector-body {
            padding: 16px 20px;
            overflow-y: auto;
            flex-grow: 1;
          }
          .loading-state, .empty-state {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 40px;
            color: #8c93a8;
            gap: 12px;
          }
          .spin-icon {
            animation: spin 1s linear infinite;
            color: #e63946;
          }
          @keyframes spin { 100% { transform: rotate(360deg); } }
          .case-list {
            display: flex;
            flex-direction: column;
            gap: 8px;
          }
          .case-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px 14px;
            background: #181c24;
            border: 1px solid #232733;
            border-radius: 6px;
            cursor: pointer;
            transition: all 0.15s;
          }
          .case-item:hover {
            border-color: #e63946;
            background: #1e2330;
            transform: translateY(-1px);
          }
          .case-item-info {
            display: flex;
            flex-direction: column;
            gap: 4px;
          }
          .case-num-line {
            display: flex;
            align-items: center;
            gap: 8px;
          }
          .case-num {
            font-family: var(--font-mono, monospace);
            font-size: 0.8rem;
            font-weight: 700;
            color: #e63946;
          }
          .case-badge {
            font-size: 0.65rem;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 3px;
          }
          .badge-critical { background: rgba(230, 57, 70, 0.2); color: #e63946; border: 1px solid rgba(230, 57, 70, 0.4); }
          .badge-high { background: rgba(249, 115, 22, 0.2); color: #f97316; border: 1px solid rgba(249, 115, 22, 0.4); }
          .badge-medium { background: rgba(234, 179, 8, 0.2); color: #eab308; border: 1px solid rgba(234, 179, 8, 0.4); }
          .badge-low { background: rgba(34, 197, 94, 0.2); color: #22c55e; border: 1px solid rgba(34, 197, 94, 0.4); }
          .case-type {
            font-size: 0.7rem;
            color: #64748b;
          }
          .case-title {
            font-size: 0.85rem;
            font-weight: 600;
            color: #f1f3f9;
          }
          .action-col {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 0.72rem;
            font-weight: 700;
            color: #64748b;
            font-family: var(--font-mono, monospace);
          }
          .case-item:hover .action-col {
            color: #e63946;
          }
          .selector-footer {
            padding: 12px 20px;
            background: #161920;
            border-top: 1px solid #232733;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 0.75rem;
            color: #64748b;
          }
          .cancel-btn {
            background: #252a38;
            color: #f1f3f9;
            border: 1px solid #333a4d;
            padding: 6px 14px;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: 600;
            cursor: pointer;
          }
          .cancel-btn:hover {
            background: #333a4d;
          }
        `}</style>
      </div>
    </div>
  );
}
