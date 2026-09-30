import React, { useState, useEffect, useCallback } from 'react';
import Panel from './Panel';
import '../styles/ai-panel.css';
import { 
  Cpu, 
  Sparkles, 
  CheckCircle, 
  AlertTriangle, 
  RotateCw, 
  ShieldCheck, 
  Layers,
  Database,
  Lock,
  Clock,
  Activity,
  Compass,
  FileText,
  HelpCircle,
  AlertCircle,
  History,
  Tag,
  ExternalLink,
  ShieldAlert,
  ArrowRight,
  Fingerprint,
  Search,
  BookOpen,
  Check,
  CheckCheck,
  FileSearch,
  BadgeAlert,
  Info,
  Edit3,
  XCircle,
  UserCheck,
  FileCheck
} from 'lucide-react';
import { 
  sendCaseToAIService, 
  getAIServiceHealth, 
  getCaseAiHistory, 
  getCaseAiAnalysisDetail,
  retrieveSimilarKnowledge,
  indexCaseKnowledge,
  updateCaseRetrievalStatus,
  verifyCaseAiAnalysis,
  getCaseAiVerification,
  approveRecommendationItem,
  editRecommendationItem,
  rejectRecommendationItem,
  getAnalysisReviewSummary,
  getAnalysisReviews,
  getCaseEvidence
} from '../services/api';

/**
 * Utility: Parse structured multiline chunk content into key-value fields.
 * Chunks like CASE, FIR, EVIDENCE, and EVIDENCE_GAP contain "Key: Value\n" patterns.
 */
function parseChunkContent(content) {
  if (!content || typeof content !== 'string') {
    return { fields: [], raw: '' };
  }
  const lines = content.split('\n').map(l => l.trim()).filter(Boolean);
  const fields = [];
  for (const line of lines) {
    const colonIdx = line.indexOf(':');
    if (colonIdx > 0 && colonIdx < 35) {
      const label = line.slice(0, colonIdx).trim();
      const value = line.slice(colonIdx + 1).trim();
      fields.push({ label, value });
    } else {
      fields.push({ label: '', value: line });
    }
  }
  return { fields, raw: content };
}

/**
 * Utility: Standardize verification status to one of:
 * VERIFIED, PARTIALLY_VERIFIED, UNVERIFIED, CONTRADICTED, NEEDS_REVIEW
 */
function normalizeVerificationStatus(status) {
  if (!status) return 'UNVERIFIED';
  const s = String(status).toUpperCase().replace(/[\s-]/g, '_');
  if (s === 'VERIFIED' || s === 'SUPPORTED') return 'VERIFIED';
  if (s === 'PARTIALLY_VERIFIED' || s === 'PARTIALLY_SUPPORTED') return 'PARTIALLY_VERIFIED';
  if (s === 'CONTRADICTED' || s === 'CONFLICT') return 'CONTRADICTED';
  if (s === 'NEEDS_REVIEW' || s === 'VERIFICATION_REQUIRED' || s === 'UNVERIFIABLE') return 'NEEDS_REVIEW';
  return 'UNVERIFIED';
}

export default function AIInvestigationPanel({ caseId, caseNumber, onSelectEvidence }) {
  const [aiHealth, setAiHealth] = useState(null);
  const [checkingHealth, setCheckingHealth] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [loadingStep, setLoadingStep] = useState('');
  const [analysisResult, setAnalysisResult] = useState(null);
  const [historyList, setHistoryList] = useState([]);
  const [showHistory, setShowHistory] = useState(false);
  const [error, setError] = useState('');

  // Phase 10: Retrieval / RAG State
  const [activeSubTab, setActiveSubTab] = useState('REASONING'); // 'REASONING' | 'SIMILAR_CASES'
  const [searchQuery, setSearchQuery] = useState('');
  const [retrievalResults, setRetrievalResults] = useState([]);
  const [searchingSimilar, setSearchingSimilar] = useState(false);
  const [retrievalError, setRetrievalError] = useState('');
  const [indexingCase, setIndexingCase] = useState(false);
  const [indexSuccess, setIndexSuccess] = useState('');
  const [indexingStatus, setIndexingStatus] = useState('INDEXED'); // 'INDEXED' | 'NOT_INDEXED' | 'INDEXING'

  // Phase 11: Evidence Verification State
  const [verifying, setVerifying] = useState(false);
  const [verificationReport, setVerificationReport] = useState(null);
  const [verificationError, setVerificationError] = useState('');

  // Phase 12: Investigator Review State
  const [reviewSummary, setReviewSummary] = useState(null);
  const [reviewList, setReviewList] = useState([]);
  const [caseEvidenceList, setCaseEvidenceList] = useState([]);
  const [activeModal, setActiveModal] = useState(null); // 'APPROVE' | 'EDIT' | 'REJECT' | null
  const [selectedItem, setSelectedItem] = useState(null);
  const [approveComment, setApproveComment] = useState('');
  const [confirmUnsupported, setConfirmUnsupported] = useState(false);
  const [editContent, setEditContent] = useState('');
  const [editComment, setEditComment] = useState('');
  const [editEvidenceRefs, setEditEvidenceRefs] = useState([]);
  const [rejectComment, setRejectComment] = useState('');
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [reviewActionError, setReviewActionError] = useState('');

  const checkHealth = useCallback(async () => {
    setCheckingHealth(true);
    try {
      const health = await getAIServiceHealth();
      setAiHealth(health);
    } catch (e) {
      setAiHealth({ status: 'DOWN', error: e.message });
    } finally {
      setCheckingHealth(false);
    }
  }, []);

  const loadReviewSummary = useCallback(async (analysisId) => {
    if (!caseId || !analysisId) return;
    try {
      const summary = await getAnalysisReviewSummary(caseId, analysisId);
      setReviewSummary(summary);
      const reviews = await getAnalysisReviews(caseId, analysisId);
      if (Array.isArray(reviews)) {
        setReviewList(reviews);
      }
    } catch (e) {
      console.warn('Failed to load review summary or review queue:', e);
    }
  }, [caseId]);

  const loadCaseEvidence = useCallback(async () => {
    if (!caseId) return;
    try {
      const ev = await getCaseEvidence(caseId);
      if (Array.isArray(ev)) {
        setCaseEvidenceList(ev);
      }
    } catch (e) {
      console.warn('Failed to load case evidence for review picker:', e);
    }
  }, [caseId]);

  const loadVerificationReport = async (analysisId) => {
    if (!caseId || !analysisId) return;
    try {
      const report = await getCaseAiVerification(caseId, analysisId);
      if (report && report.total_items_checked > 0) {
        setVerificationReport(report);
      }
    } catch (e) {
      setVerificationReport(null);
    }
  };

  const loadHistory = useCallback(async () => {
    if (!caseId) return;
    try {
      const hist = await getCaseAiHistory(caseId);
      if (Array.isArray(hist)) {
        setHistoryList(hist);
        if (hist.length > 0 && !analysisResult) {
          const latest = await getCaseAiAnalysisDetail(caseId, hist[0].id);
          setAnalysisResult(latest);
          loadVerificationReport(hist[0].id);
          loadReviewSummary(hist[0].id);
        }
      }
    } catch (e) {
      console.warn('Failed to load analysis history:', e);
    }
  }, [caseId, analysisResult, loadReviewSummary]);

  useEffect(() => {
    checkHealth();
    loadHistory();
    loadCaseEvidence();
  }, [checkHealth, loadHistory, loadCaseEvidence]);

  const handleRunAnalysis = async () => {
    if (!caseId || analyzing) return;
    setAnalyzing(true);
    setError('');
    setVerificationReport(null);
    setLoadingStep('Retrieving relevant historical cases and context (RAG)...');

    const timer = setTimeout(() => {
      setLoadingStep('CrimeSphere AI is analyzing the case dossier with Groq LLaMA 3.3 70B...');
    }, 1200);

    try {
      const res = await sendCaseToAIService(caseId);
      setAnalysisResult(res);
      const analysisId = res?.analysis_id || res?.id;
      if (analysisId) {
        loadReviewSummary(analysisId);
      }
      loadHistory();
    } catch (err) {
      setError(err.message || 'AI analysis is temporarily unavailable. Your case data remains safe and unchanged.');
    } finally {
      clearTimeout(timer);
      setAnalyzing(false);
      setLoadingStep('');
    }
  };

  const handleVerifyFindings = async () => {
    const analysisId = analysisResult?.analysis_id || analysisResult?.id;
    if (!caseId || !analysisId || verifying) return;
    setVerifying(true);
    setVerificationError('');
    try {
      const report = await verifyCaseAiAnalysis(caseId, analysisId);
      setVerificationReport(report);
      const freshDetail = await getCaseAiAnalysisDetail(caseId, analysisId);
      setAnalysisResult(freshDetail);
      loadReviewSummary(analysisId);
    } catch (err) {
      setVerificationError(err.message || 'Failed to verify AI findings against evidence.');
    } finally {
      setVerifying(false);
    }
  };

  const reloadAnalysisAndSummary = async () => {
    const analysisId = analysisResult?.analysis_id || analysisResult?.id;
    if (analysisId) {
      const freshDetail = await getCaseAiAnalysisDetail(caseId, analysisId);
      setAnalysisResult(freshDetail);
      await loadReviewSummary(analysisId);
    }
  };

  const openApproveModal = (item, itemType) => {
    const itemId = item.recommendationItemId || item.id;
    const content = item.originalContent || item.observation || item.lead || item.action || item.description || item.content || item.gap || '';
    const verifStatus = item.verificationStatus || item.verification?.status || 'UNVERIFIED';
    setSelectedItem({ ...item, id: itemId, content, verificationStatus: verifStatus, itemType: itemType || item.itemType });
    setApproveComment('');
    setConfirmUnsupported(false);
    setReviewActionError('');
    setActiveModal('APPROVE');
  };

  const openEditModal = (item, itemType) => {
    const itemId = item.recommendationItemId || item.id;
    const originalText = item.originalContent || item.observation || item.lead || item.action || item.description || item.content || item.gap || '';
    const existingEdited = item.reviewedContent || item.review?.reviewedContent || originalText;
    const existingComment = item.reviewComment || item.review?.reviewComment || '';
    const existingRefs = item.evidenceReferences || item.evidence_references || item.references || [];
    setSelectedItem({ ...item, id: itemId, originalContent: originalText, itemType: itemType || item.itemType });
    setEditContent(existingEdited);
    setEditComment(existingComment);
    setEditEvidenceRefs(Array.isArray(existingRefs) ? [...existingRefs] : []);
    setReviewActionError('');
    setActiveModal('EDIT');
  };

  const openRejectModal = (item, itemType) => {
    const itemId = item.recommendationItemId || item.id;
    const content = item.originalContent || item.observation || item.lead || item.action || item.description || item.content || item.gap || '';
    const existingComment = item.reviewComment || item.review?.reviewComment || '';
    setSelectedItem({ ...item, id: itemId, content, itemType: itemType || item.itemType });
    setRejectComment(existingComment);
    setReviewActionError('');
    setActiveModal('REJECT');
  };

  const handleApproveSubmit = async () => {
    if (!selectedItem || !selectedItem.id) return;
    const analysisId = analysisResult?.analysis_id || analysisResult?.id;
    if (!analysisId) return;

    if (selectedItem.verification?.status === 'UNSUPPORTED' && !confirmUnsupported) {
      setReviewActionError('Explicit confirmation is required to approve an unsupported finding.');
      return;
    }

    setReviewSubmitting(true);
    setReviewActionError('');
    try {
      await approveRecommendationItem(caseId, analysisId, selectedItem.id, {
        comment: approveComment,
        confirmUnsupported: confirmUnsupported,
      });
      await reloadAnalysisAndSummary();
      setActiveModal(null);
    } catch (err) {
      setReviewActionError(err.message || 'Failed to approve finding.');
    } finally {
      setReviewSubmitting(false);
    }
  };

  const handleEditSubmit = async () => {
    if (!selectedItem || !selectedItem.id) return;
    const analysisId = analysisResult?.analysis_id || analysisResult?.id;
    if (!analysisId) return;

    if (!editContent || !editContent.trim()) {
      setReviewActionError('Edited content cannot be empty.');
      return;
    }

    setReviewSubmitting(true);
    setReviewActionError('');
    try {
      await editRecommendationItem(caseId, analysisId, selectedItem.id, {
        content: editContent.trim(),
        comment: editComment,
        evidenceReferences: editEvidenceRefs,
      });
      await reloadAnalysisAndSummary();
      setActiveModal(null);
    } catch (err) {
      setReviewActionError(err.message || 'Failed to save edited finding.');
    } finally {
      setReviewSubmitting(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!selectedItem || !selectedItem.id) return;
    const analysisId = analysisResult?.analysis_id || analysisResult?.id;
    if (!analysisId) return;

    if (!rejectComment || !rejectComment.trim()) {
      setReviewActionError('Reason for rejection is required.');
      return;
    }

    setReviewSubmitting(true);
    setReviewActionError('');
    try {
      await rejectRecommendationItem(caseId, analysisId, selectedItem.id, {
        comment: rejectComment.trim(),
      });
      await reloadAnalysisAndSummary();
      setActiveModal(null);
    } catch (err) {
      setReviewActionError(err.message || 'Failed to reject finding.');
    } finally {
      setReviewSubmitting(false);
    }
  };

  const handleSearchSimilar = async (e) => {
    if (e) e.preventDefault();
    if (!caseId || searchingSimilar) return;
    setSearchingSimilar(true);
    setRetrievalError('');
    try {
      const res = await retrieveSimilarKnowledge(caseId, searchQuery || null, 5, 0.70);
      setRetrievalResults(res.results || []);
    } catch (err) {
      setRetrievalError(err.message || 'Failed to retrieve similar knowledge chunks.');
    } finally {
      setSearchingSimilar(false);
    }
  };

  const handleIndexCase = async () => {
    if (!caseId || indexingCase) return;
    setIndexingCase(true);
    setIndexingStatus('INDEXING');
    setIndexSuccess('');
    setRetrievalError('');
    try {
      const res = await indexCaseKnowledge(caseId);
      const cnt = res.indexed_chunks_count || res.indexed_count || 0;
      setIndexSuccess(`Successfully indexed ${cnt} knowledge chunks into vector database.`);
      setIndexingStatus('INDEXED');
      setTimeout(() => setIndexSuccess(''), 5000);
      // Refresh similar cases after re-indexing
      handleSearchSimilar();
    } catch (err) {
      setIndexingStatus('NOT_INDEXED');
      setRetrievalError(err.message || 'Failed to index case into knowledge base.');
    } finally {
      setIndexingCase(false);
    }
  };

  const handleSelectHistoryItem = async (histItem) => {
    try {
      const detail = await getCaseAiAnalysisDetail(caseId, histItem.id);
      setAnalysisResult(detail);
      loadVerificationReport(histItem.id);
      loadReviewSummary(histItem.id);
      setShowHistory(false);
    } catch (e) {
      alert('Failed to load analysis record: ' + e.message);
    }
  };

  const handleEvidenceClick = (evNumber) => {
    if (onSelectEvidence) {
      onSelectEvidence(evNumber);
    } else {
      const evElement = document.getElementById(`evidence-${evNumber}`);
      if (evElement) {
        evElement.scrollIntoView({ behavior: 'smooth' });
      }
    }
  };

  const isUp = aiHealth?.status === 'UP';

  const getPriorityBadgeClass = (priority) => {
    switch (priority?.toUpperCase()) {
      case 'HIGH':
      case 'CRITICAL':
        return 'high';
      case 'LOW':
        return 'low';
      default:
        return 'medium';
    }
  };

  // Helper to render Phase 11 Verification Badge & Evidence Support Notes
  const renderVerificationWidget = (verification) => {
    if (!verification) {
      return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', paddingTop: '6px', fontSize: '0.72rem', color: '#A7A7AA', fontStyle: 'italic' }}>
          <Info size={13} style={{ color: '#71717A' }} />
          <span>Unverified against database evidence</span>
        </div>
      );
    }

    const rawStatus = verification.status || 'UNVERIFIED';
    const status = normalizeVerificationStatus(rawStatus);
    const supportLevel = verification.support_level || 'NONE';
    const statusLower = status.toLowerCase();

    let icon = <HelpCircle size={12} />;
    if (status === 'VERIFIED') icon = <CheckCircle size={12} />;
    else if (status === 'PARTIALLY_VERIFIED') icon = <AlertCircle size={12} />;
    else if (status === 'CONTRADICTED') icon = <ShieldAlert size={12} />;
    else if (status === 'NEEDS_REVIEW') icon = <AlertTriangle size={12} />;
    else if (status === 'UNVERIFIED') icon = <HelpCircle size={12} />;

    return (
      <div className="aip-verification-box">
        <div className="aip-verif-header">
          <div className="aip-verif-tag-group">
            <span className={`aip-verif-status-badge ${statusLower}`}>
              {icon}
              <span>{status.replace('_', ' ')}</span>
            </span>
            <span style={{ fontSize: '0.7rem', fontFamily: 'monospace', color: '#A7A7AA', padding: '2px 6px', background: '#17171A', border: '1px solid #2A2A2D', borderRadius: '4px' }}>
              Support: <strong style={{ color: '#F4F4F5' }}>{supportLevel}</strong>
            </span>
          </div>
          <span style={{ fontSize: '0.7rem', color: '#71717A', fontFamily: 'monospace' }}>
            Method: {verification.verification_method || 'RULE_BASED'}
          </span>
        </div>

        {verification.verification_summary && (
          <p className="aip-verif-support-note">
            {verification.verification_summary}
          </p>
        )}

        {verification.evidence && verification.evidence.length > 0 && (
          <div className="aip-verif-evidence-list">
            <span style={{ fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', color: '#A7A7AA' }}>
              Evaluated Evidence Links:
            </span>
            {verification.evidence.map((ev, eIdx) => (
              <div key={eIdx} className="aip-verif-ev-row">
                <span className={`aip-verif-ev-rel ${ev.relationship === 'SUPPORTS' ? 'supports' : 'invalid_reference'}`}>
                  {ev.relationship}
                </span>
                <strong style={{ fontFamily: 'monospace', color: '#E4E4E7' }}>{ev.evidence_number}:</strong>
                <span>{ev.support_note || ev.title}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  // Helper to render Phase 12 Investigator Review Controls & Decisions
  const renderReviewControls = (item, itemType) => {
    if (!item || !item.id) return null;

    const review = item.review;
    const decision = review?.decision || 'PENDING_REVIEW';

    let badgeIcon = <Clock size={11} style={{ color: '#FBBF24' }} />;
    let badgeText = 'PENDING REVIEW';
    let badgeClass = 'aip-status-badge warning';

    if (decision === 'APPROVED') {
      badgeIcon = <CheckCircle size={11} style={{ color: '#6EE7B7' }} />;
      badgeText = 'INVESTIGATOR APPROVED';
      badgeClass = 'aip-status-badge online';
    } else if (decision === 'EDITED') {
      badgeIcon = <Edit3 size={11} style={{ color: '#38BDF8' }} />;
      badgeText = 'INVESTIGATOR EDITED';
      badgeClass = 'aip-status-badge'
    } else if (decision === 'REJECTED') {
      badgeIcon = <XCircle size={11} style={{ color: '#FF6B6B' }} />;
      badgeText = 'REJECTED';
      badgeClass = 'aip-status-badge offline';
    }

    return (
      <div className="aip-review-controls">
        {/* Status & Reviewer Info */}
        <div className="aip-review-status-info">
          <span className={badgeClass} style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
            {badgeIcon}
            <span>{badgeText}</span>
          </span>
          {review?.reviewer && (
            <span style={{ fontSize: '0.72rem', color: '#A7A7AA' }}>
              Reviewed by: <strong style={{ color: '#F4F4F5' }}>{review.reviewer}</strong>
              {review.updatedAt && (
                <span style={{ color: '#71717A', marginLeft: '4px' }}>
                  ({new Date(review.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
                </span>
              )}
            </span>
          )}
        </div>

        {/* Action Buttons: Approve, Edit, Reject */}
        <div className="aip-review-btn-group">
          <button
            type="button"
            onClick={() => openApproveModal(item, itemType)}
            className="aip-btn aip-btn-sm aip-btn-approve"
            title="Approve this finding for investigation record"
          >
            <Check size={12} />
            <span>Approve</span>
          </button>

          <button
            type="button"
            onClick={() => openEditModal(item, itemType)}
            className="aip-btn aip-btn-sm aip-btn-edit"
            title="Modify this finding before accepting"
          >
            <Edit3 size={12} />
            <span>Edit</span>
          </button>

          <button
            type="button"
            onClick={() => openRejectModal(item, itemType)}
            className="aip-btn aip-btn-sm aip-btn-reject"
            title="Reject this finding"
          >
            <XCircle size={12} />
            <span>Reject</span>
          </button>
        </div>

        {/* Traceability: Edited Version Display */}
        {decision === 'EDITED' && review?.reviewedContent && (
          <div style={{ width: '100%', padding: '10px 14px', borderRadius: '6px', backgroundColor: '#141820', border: '1px solid rgba(14, 165, 233, 0.4)', marginTop: '6px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.7rem', fontWeight: 700, color: '#38BDF8', textTransform: 'uppercase' }}>
              <span>Investigator-Edited Version:</span>
              <span style={{ color: '#71717A', fontWeight: 400, textTransform: 'none', fontStyle: 'italic' }}>Original AI retained in audit</span>
            </div>
            <p style={{ fontSize: '0.78rem', color: '#E0F2FE', lineHeight: '1.5', margin: 0 }}>
              {review.reviewedContent}
            </p>
            {review.reviewComment && (
              <p style={{ fontSize: '0.72rem', color: 'rgba(56, 189, 248, 0.8)', fontStyle: 'italic', margin: 0, paddingTop: '4px', borderTop: '1px solid rgba(14, 165, 233, 0.2)' }}>
                Reason: {review.reviewComment}
              </p>
            )}
          </div>
        )}

        {/* Traceability: Rejected Reason Display */}
        {decision === 'REJECTED' && (
          <div style={{ width: '100%', padding: '10px 14px', borderRadius: '6px', backgroundColor: 'rgba(127, 29, 29, 0.3)', border: '1px solid rgba(239, 68, 68, 0.4)', marginTop: '6px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <span style={{ fontSize: '0.7rem', fontWeight: 700, color: '#FCA5A5', textTransform: 'uppercase' }}>
              Rejection Reason:
            </span>
            <p style={{ fontSize: '0.76rem', color: '#FECACA', fontStyle: 'italic', margin: 0 }}>
              "{review?.reviewComment || 'Finding rejected by investigator.'}"
            </p>
          </div>
        )}

        {/* Approval Comment if present */}
        {decision === 'APPROVED' && review?.reviewComment && (
          <div style={{ width: '100%', fontSize: '0.72rem', color: '#A7A7AA', fontStyle: 'italic', paddingLeft: '4px' }}>
            Investigator Note: {review.reviewComment}
          </div>
        )}
      </div>
    );
  };

  // Extract metadata for the Current Case Context Grid
  const currentCaseNum = caseNumber || analysisResult?.case_number || 'CS-2026-0100';
  const currentClassification = analysisResult?.case_type || analysisResult?.classification || 'MURDER';
  const currentTitle = analysisResult?.title || 'Test Investigation Case';
  const currentStatus = analysisResult?.status || 'PENDING_REVIEW';
  const currentNarrative = analysisResult?.summary || analysisResult?.narrative || 'Development test case for Phase 4 verification.';

  return (
    <Panel 
      title="CRIMESPHERE AI INVESTIGATION & EVIDENCE VERIFICATION" 
      subtitle="LLM-Powered Decision Support • Vector Retrieval • Evidence Verification Layer"
      icon={Sparkles}
      accent={true}
    >
      <div className="aip-container">
        {/* ==========================================================================
            AI Service Connectivity & History Header Bar
            ========================================================================== */}
        <div className="aip-header-bar">
          <div className="aip-header-info">
            <div className={`aip-status-icon-box ${isUp ? 'online' : 'offline'}`}>
              <Activity size={20} className={isUp ? 'animate-pulse' : ''} />
            </div>
            <div className="aip-header-text">
              <div className="aip-status-title-row">
                <span className="aip-status-label">AI SERVICE ENGINE:</span>
                <span className={`aip-status-badge ${isUp ? 'online' : 'offline'}`}>
                  {checkingHealth ? 'CHECKING...' : isUp ? 'ONLINE (GROQ + RAG + VERIFICATION)' : 'OFFLINE'}
                </span>
              </div>
              <span className="aip-meta-desc">
                LLaMA 3.3 70B • Dense Embeddings (384-dim) • Verification
              </span>
            </div>
          </div>

          <div className="aip-header-actions">
            {historyList.length > 0 && (
              <button
                type="button"
                onClick={() => setShowHistory(!showHistory)}
                className="aip-btn aip-btn-secondary"
              >
                <History size={13} />
                <span>History ({historyList.length})</span>
              </button>
            )}

            <button
              type="button"
              onClick={checkHealth}
              disabled={checkingHealth}
              className="aip-btn aip-btn-secondary"
              title="Ping AI Service"
            >
              <RotateCw size={13} className={checkingHealth ? 'animate-spin' : ''} />
              <span>Ping</span>
            </button>
          </div>
        </div>

        {/* ==========================================================================
            Subtab Selection Bar
            ========================================================================== */}
        <div className="aip-tab-bar">
          <button
            type="button"
            onClick={() => setActiveSubTab('REASONING')}
            className={`aip-tab-btn ${activeSubTab === 'REASONING' ? 'active' : ''}`}
          >
            <Sparkles size={15} />
            <span>AI INVESTIGATION AGENT</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveSubTab('SIMILAR_CASES');
              if (retrievalResults.length === 0) {
                handleSearchSimilar();
              }
            }}
            className={`aip-tab-btn ${activeSubTab === 'SIMILAR_CASES' ? 'active' : ''}`}
          >
            <BookOpen size={15} />
            <span>SIMILAR CASE INTELLIGENCE (RAG)</span>
          </button>
        </div>

        {/* History Dropdown Drawer */}
        {showHistory && historyList.length > 0 && (
          <div className="aip-history-panel">
            <div className="aip-history-header">
              <span style={{ fontSize: '0.78rem', fontWeight: 700, color: '#F4F4F5', textTransform: 'uppercase', letterSpacing: '0.06em', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <History size={15} style={{ color: 'var(--bright-crime-red)' }} />
                <span>Saved Investigation Analyses</span>
              </span>
              <span style={{ fontSize: '0.72rem', color: '#71717A' }}>Select to view previous version</span>
            </div>
            <div className="aip-history-list">
              {historyList.map((hist) => (
                <div
                  key={hist.id}
                  onClick={() => handleSelectHistoryItem(hist)}
                  className={`aip-history-item ${analysisResult?.analysis_id === hist.id?.toString() || analysisResult?.id === hist.id ? 'active' : ''}`}
                >
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontSize: '0.78rem', fontFamily: 'monospace', fontWeight: 700, color: '#FFFFFF' }}>
                        {hist.model_name || 'LLaMA 3.3 70B'}
                      </span>
                      <span className="aip-status-badge warning" style={{ padding: '1px 6px', fontSize: '0.66rem' }}>
                        {hist.status}
                      </span>
                    </div>
                    <div style={{ fontSize: '0.73rem', color: '#A7A7AA' }}>
                      {hist.summary || 'Investigation analysis record'}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', fontSize: '0.7rem', color: '#71717A', flexShrink: 0 }}>
                    <div>{hist.created_at ? new Date(hist.created_at).toLocaleDateString() : 'Recent'}</div>
                    <div>{hist.created_at ? new Date(hist.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Mandatory Responsible AI Notice Banner */}
        <div className="aip-notice-banner">
          <ShieldAlert size={18} className="aip-notice-icon" />
          <div>
            <span className="aip-notice-title">
              CRIMESPHERE AI RESPONSIBLE DECISION-SUPPORT & VERIFICATION NOTICE
            </span>
            <p className="aip-notice-text">
              AI recommendations are decision-support suggestions. Verification status is based on currently available case data and evidence. Investigator review is required before relying on any recommendation. Unsupported recommendations must not be treated as established facts.
            </p>
          </div>
        </div>

        {/* ==========================================================================
            SUBTAB 1: AI INVESTIGATION AGENT
            ========================================================================== */}
        {activeSubTab === 'REASONING' && (
          <>
            {/* Pre-Analysis CTA State */}
            {!analysisResult && !analyzing && (
              <div className="aip-empty-box">
                <div style={{ padding: '12px', borderRadius: '50%', backgroundColor: 'rgba(193, 18, 31, 0.15)', border: '1px solid rgba(193, 18, 31, 0.4)', color: 'var(--bright-crime-red)' }}>
                  <Compass size={32} />
                </div>
                <h3 className="aip-empty-title">
                  AI Investigation Reasoning & Verification Engine
                </h3>
                <p className="aip-empty-desc">
                  CrimeSphere AI synthesizes case facts, evaluates evidence links, and enables deterministic verification of every generated finding against authoritative case records.
                </p>

                <button
                  type="button"
                  onClick={handleRunAnalysis}
                  disabled={analyzing}
                  className="aip-btn aip-btn-primary"
                  style={{ marginTop: '8px' }}
                >
                  <Sparkles size={15} />
                  <span>Run AI Investigation Analysis</span>
                </button>
              </div>
            )}

            {/* Loading Progress State */}
            {analyzing && (
              <div className="aip-empty-box" style={{ animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite' }}>
                <div style={{ padding: '14px', borderRadius: '50%', backgroundColor: 'rgba(193, 18, 31, 0.2)', border: '1px solid var(--bright-crime-red)', color: 'var(--bright-crime-red)' }}>
                  <RotateCw size={32} className="animate-spin" />
                </div>
                <h4 className="aip-empty-title">
                  {loadingStep || 'CrimeSphere AI is analyzing the case dossier...'}
                </h4>
                <p className="aip-empty-desc">
                  Retrieving vector knowledge embeddings and executing Groq LLaMA 3.3 70B reasoning...
                </p>
              </div>
            )}

            {/* Error Banner */}
            {error && (
              <div className="aip-modal-alert error" style={{ padding: '14px 18px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  <AlertTriangle size={16} />
                  <span>AI Service Notice</span>
                </div>
                <p style={{ margin: 0, color: '#FECACA' }}>{error}</p>
              </div>
            )}

            {/* Structured Results Display */}
            {analysisResult && !analyzing && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
                {/* 1. AI Investigation Synthesis Header Card */}
                {analysisResult.summary && (
                  <div className="aip-synthesis-card">
                    <div className="aip-synthesis-header-row">
                      <div className="aip-synthesis-title-box">
                        <h3 className="aip-synthesis-title">
                          <ShieldCheck size={18} style={{ color: '#10B981' }} />
                          <span>AI INVESTIGATION SYNTHESIS</span>
                        </h3>
                        <span className={`aip-synthesis-badge ${verificationReport?.total_items_checked > 0 ? 'completed' : 'review-required'}`}>
                          {analysisResult.status || 'REVIEW_REQUIRED'}
                        </span>
                      </div>

                      <div className="aip-synthesis-actions">
                        {/* Phase 11: Verify AI Findings Button */}
                        <button
                          type="button"
                          onClick={handleVerifyFindings}
                          disabled={verifying}
                          className="aip-btn aip-btn-verify"
                          title="Verify AI observations and leads against case database evidence"
                        >
                          <FileSearch size={14} className={verifying ? 'animate-spin' : ''} />
                          <span>{verifying ? 'VERIFYING FINDINGS...' : 'VERIFY AI FINDINGS'}</span>
                        </button>

                        <button
                          type="button"
                          onClick={handleRunAnalysis}
                          disabled={analyzing}
                          className="aip-btn aip-btn-secondary"
                          title="Run fresh analysis with latest case updates"
                        >
                          <RotateCw size={13} className={analyzing ? 'animate-spin' : ''} />
                          <span>RE-RUN ANALYSIS</span>
                        </button>
                      </div>
                    </div>

                    <div className="aip-synthesis-meta-row">
                      <span className="aip-synthesis-meta-item">
                        Model: <strong style={{ color: '#F4F4F5' }}>{analysisResult.model_name || 'LLaMA 3.3 70B'}</strong>
                      </span>
                      <span className="aip-synthesis-meta-sep">•</span>
                      <span className="aip-synthesis-meta-item">
                        Version: <strong style={{ color: '#F4F4F5' }}>{analysisResult.prompt_version || 'v1'}</strong>
                      </span>
                      <span className="aip-synthesis-meta-sep">•</span>
                      <span className="aip-synthesis-meta-item">
                        Generated: <strong style={{ color: '#F4F4F5' }}>{analysisResult.created_at ? new Date(analysisResult.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Now'}</strong>
                      </span>
                    </div>

                    <div className="aip-synthesis-divider" />

                    {/* Verification Status Summary Message */}
                    {verificationReport ? (
                      <div className="aip-synthesis-verified-pillbox">
                        <span style={{ fontWeight: 600, color: '#E4E4E7', display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                          <CheckCheck size={14} style={{ color: '#10B981' }} />
                          <span>Verification Summary ({verificationReport.total_items_checked} items):</span>
                        </span>
                        {verificationReport.supported_count > 0 && (
                          <span className="aip-verif-pill supported">
                            {verificationReport.supported_count} Supported
                          </span>
                        )}
                        {verificationReport.partially_supported_count > 0 && (
                          <span className="aip-verif-pill partially">
                            {verificationReport.partially_supported_count} Partially Supported
                          </span>
                        )}
                        {verificationReport.unsupported_count > 0 && (
                          <span className="aip-verif-pill unsupported">
                            {verificationReport.unsupported_count} Unsupported
                          </span>
                        )}
                        {verificationReport.unverifiable_count > 0 && (
                          <span className="aip-verif-pill unverifiable">
                            {verificationReport.unverifiable_count} Unverifiable
                          </span>
                        )}
                        {verificationReport.reference_invalid_count > 0 && (
                          <span className="aip-verif-pill invalid">
                            {verificationReport.reference_invalid_count} Invalid References
                          </span>
                        )}
                      </div>
                    ) : (
                      <div className="aip-synthesis-message">
                        <AlertTriangle size={15} style={{ color: '#F59E0B', flexShrink: 0 }} />
                        <span>AI findings have not yet been verified against available database evidence. Click <strong>VERIFY AI FINDINGS</strong> to validate assertions.</span>
                      </div>
                    )}

                    {verificationError && (
                      <div className="aip-modal-alert error">
                        {verificationError}
                      </div>
                    )}
                  </div>
                )}

                {/* 2. AI Review Status Summary Widget (5 Cards in a 5-column grid) */}
                <div className="aip-card">
                  <div className="aip-card-header">
                    <div className="aip-card-title-group">
                      <UserCheck size={16} style={{ color: 'var(--bright-crime-red)' }} />
                      <h3 className="aip-card-title">
                        AI Review Status (Human-in-the-Loop)
                      </h3>
                    </div>
                    <div>
                      {reviewSummary?.isComplete ? (
                        <span className="aip-status-badge online" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <CheckCircle size={13} />
                          <span>Investigator review completed</span>
                        </span>
                      ) : (
                        <span className="aip-status-badge warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                          <Clock size={13} />
                          <span>{reviewSummary?.message || 'Review required by investigator'}</span>
                        </span>
                      )}
                    </div>
                  </div>

                  {/* 5 Statistics as 5 Separate Cards */}
                  <div className="aip-stats-grid">
                    <div className="aip-stat-card total">
                      <span className="aip-stat-label">TOTAL FINDINGS</span>
                      <strong className="aip-stat-value">{reviewSummary?.total ?? 0}</strong>
                    </div>
                    <div className="aip-stat-card pending">
                      <span className="aip-stat-label">PENDING REVIEW</span>
                      <strong className="aip-stat-value">{reviewSummary?.pendingReview ?? 0}</strong>
                    </div>
                    <div className="aip-stat-card approved">
                      <span className="aip-stat-label">APPROVED</span>
                      <strong className="aip-stat-value">{reviewSummary?.approved ?? 0}</strong>
                    </div>
                    <div className="aip-stat-card edited">
                      <span className="aip-stat-label">EDITED</span>
                      <strong className="aip-stat-value">{reviewSummary?.edited ?? 0}</strong>
                    </div>
                    <div className="aip-stat-card rejected">
                      <span className="aip-stat-label">REJECTED</span>
                      <strong className="aip-stat-value">{reviewSummary?.rejected ?? 0}</strong>
                    </div>
                  </div>

                  {/* Responsible AI & Decision-Support Governance Box */}
                  <div className="aip-governance-box">
                    <div className="aip-governance-title">
                      <ShieldCheck size={14} style={{ color: 'var(--bright-crime-red)' }} />
                      <span>Responsible AI & Decision-Support Governance</span>
                    </div>
                    <ul className="aip-governance-list">
                      <li className="aip-governance-item">
                        <span className="aip-governance-bullet">•</span>
                        <span>AI-generated investigation support. Review against available evidence before taking investigative action.</span>
                      </li>
                      <li className="aip-governance-item">
                        <span className="aip-governance-bullet">•</span>
                        <span>Investigator decisions are recorded separately from AI output. Original AI content remains strictly immutable.</span>
                      </li>
                      <li className="aip-governance-item">
                        <span className="aip-governance-bullet">•</span>
                        <span>Approval does not establish guilt or factual certainty.</span>
                      </li>
                    </ul>
                  </div>
                </div>

                {/* INVESTIGATOR REVIEW QUEUE */}
                <div className="aip-card aip-review-queue-card" id="investigator-review-queue">
                  <div className="aip-card-header">
                    <div className="aip-card-title-group">
                      <FileCheck size={18} style={{ color: 'var(--bright-crime-red)' }} />
                      <h3 className="aip-card-title">INVESTIGATOR REVIEW QUEUE</h3>
                    </div>
                    <div className="aip-queue-header-stats">
                      <span className="aip-status-badge online" style={{ fontFamily: 'monospace' }}>
                        {reviewSummary?.approved ?? 0} APPROVED
                      </span>
                      <span className="aip-status-badge" style={{ backgroundColor: 'rgba(56, 189, 248, 0.15)', borderColor: 'rgba(56, 189, 248, 0.4)', color: '#38BDF8', fontFamily: 'monospace' }}>
                        {reviewSummary?.edited ?? 0} EDITED
                      </span>
                      <span className="aip-status-badge offline" style={{ fontFamily: 'monospace' }}>
                        {reviewSummary?.rejected ?? 0} REJECTED
                      </span>
                      <span className="aip-status-badge warning" style={{ fontFamily: 'monospace' }}>
                        {reviewSummary?.pendingReview ?? 0} PENDING
                      </span>
                    </div>
                  </div>

                  <div className="aip-queue-info-banner">
                    <Info size={15} style={{ color: '#38BDF8', flexShrink: 0 }} />
                    <span>
                      Review AI-generated assertions individually against available database evidence. Investigator decisions are recorded separately from AI output. <strong>Original AI content remains strictly immutable.</strong>
                    </span>
                  </div>

                  <div className="aip-review-queue-stack">
                    {reviewList && reviewList.length > 0 ? (
                      reviewList.map((item, index) => {
                        const findingNum = item.findingIndex || (index + 1);
                        const decision = item.decision || 'PENDING_REVIEW';
                        const verifStatus = normalizeVerificationStatus(item.verificationStatus);
                        const evRefs = item.evidenceReferences || [];

                        return (
                          <div key={item.recommendationItemId || index} className={`aip-queue-finding-card decision-${decision.toLowerCase()}`}>
                            {/* Finding Header */}
                            <div className="aip-queue-finding-header">
                              <div className="aip-finding-id-box">
                                <span className="aip-finding-num">AI FINDING #{findingNum}</span>
                                {item.itemType && (
                                  <span className="aip-finding-type-badge">
                                    {item.itemType.replace(/_/g, ' ')}
                                  </span>
                                )}
                                {item.priority && (
                                  <span className={`aip-badge ${getPriorityBadgeClass(item.priority)}`} style={{ fontSize: '0.65rem', padding: '1px 6px' }}>
                                    {item.priority}
                                  </span>
                                )}
                              </div>

                              <div className="aip-finding-status-box">
                                <span className="aip-finding-status-label">Status:</span>
                                {decision === 'APPROVED' && (
                                  <span className="aip-status-badge online">
                                    <CheckCircle size={12} />
                                    <span>APPROVED</span>
                                  </span>
                                )}
                                {decision === 'EDITED' && (
                                  <span className="aip-status-badge" style={{ backgroundColor: 'rgba(56, 189, 248, 0.15)', borderColor: 'rgba(56, 189, 248, 0.4)', color: '#38BDF8' }}>
                                    <Edit3 size={12} />
                                    <span>EDITED</span>
                                  </span>
                                )}
                                {decision === 'REJECTED' && (
                                  <span className="aip-status-badge offline">
                                    <XCircle size={12} />
                                    <span>REJECTED</span>
                                  </span>
                                )}
                                {decision === 'PENDING_REVIEW' && (
                                  <span className="aip-status-badge warning">
                                    <Clock size={12} />
                                    <span>PENDING REVIEW</span>
                                  </span>
                                )}
                              </div>
                            </div>

                            {/* Finding Body */}
                            <div className="aip-queue-finding-body">
                              {/* Finding Label & Original Immutable AI Content */}
                              <div className="aip-finding-content-block">
                                <div className="aip-finding-section-header">
                                  <span className="aip-finding-block-label">Finding:</span>
                                  <span className="aip-immutable-tag">Immutable Original AI Output</span>
                                </div>
                                <div className="aip-finding-original-text">
                                  "{item.originalContent || 'No content available.'}"
                                </div>
                              </div>

                              {/* Context / Reasoning if present */}
                              {item.reasoning && (
                                <div className="aip-finding-reasoning">
                                  <span style={{ fontWeight: 600, color: '#A7A7AA' }}>Context / Reasoning:</span> {item.reasoning}
                                </div>
                              )}

                              {/* Investigator Reviewed Version (When EDITED) */}
                              {decision === 'EDITED' && item.reviewedContent && (
                                <div className="aip-finding-edited-box">
                                  <div className="aip-edited-header">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                      <Edit3 size={13} style={{ color: '#38BDF8' }} />
                                      <span style={{ fontWeight: 700, color: '#38BDF8', textTransform: 'uppercase', fontSize: '0.72rem' }}>
                                        Investigator Edited Finding:
                                      </span>
                                    </div>
                                    {item.reviewer && (
                                      <span style={{ fontSize: '0.7rem', color: '#A7A7AA' }}>
                                        Reviewed by: <strong style={{ color: '#F4F4F5' }}>{item.reviewer}</strong>
                                        {item.updatedAt && (
                                          <span style={{ color: '#71717A', marginLeft: '4px' }}>
                                            ({new Date(item.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
                                          </span>
                                        )}
                                      </span>
                                    )}
                                  </div>
                                  <p className="aip-edited-text">
                                    "{item.reviewedContent}"
                                  </p>
                                  {item.reviewComment && (
                                    <div className="aip-edited-reason">
                                      <span style={{ fontWeight: 600, color: '#38BDF8' }}>Modification Reason:</span> {item.reviewComment}
                                    </div>
                                  )}
                                </div>
                              )}

                              {/* Investigator Rejection Note (When REJECTED) */}
                              {decision === 'REJECTED' && (
                                <div className="aip-finding-rejected-box">
                                  <div className="aip-rejected-header">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                      <XCircle size={13} style={{ color: '#F87171' }} />
                                      <span style={{ fontWeight: 700, color: '#F87171', textTransform: 'uppercase', fontSize: '0.72rem' }}>
                                        Rejection Details:
                                      </span>
                                    </div>
                                    {item.reviewer && (
                                      <span style={{ fontSize: '0.7rem', color: '#A7A7AA' }}>
                                        Rejected by: <strong style={{ color: '#F4F4F5' }}>{item.reviewer}</strong>
                                        {item.updatedAt && (
                                          <span style={{ color: '#71717A', marginLeft: '4px' }}>
                                            ({new Date(item.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
                                          </span>
                                        )}
                                      </span>
                                    )}
                                  </div>
                                  <p className="aip-rejected-text">
                                    "{item.reviewComment || 'Finding rejected by investigator.'}"
                                  </p>
                                </div>
                              )}

                              {/* Investigator Approval Note (When APPROVED with comment) */}
                              {decision === 'APPROVED' && item.reviewComment && (
                                <div className="aip-finding-approved-box">
                                  <span style={{ fontWeight: 600, color: '#10B981' }}>Investigator Note:</span> {item.reviewComment}
                                  {item.reviewer && (
                                    <span style={{ marginLeft: '8px', color: '#71717A', fontSize: '0.7rem' }}>
                                      — {item.reviewer}
                                    </span>
                                  )}
                                </div>
                              )}

                              {/* Evidence References and Verification Result Row */}
                              <div className="aip-finding-metadata-row">
                                {/* Evidence References */}
                                <div className="aip-finding-ev-refs">
                                  <span className="aip-finding-ev-label">Evidence References:</span>
                                  {evRefs.length > 0 ? (
                                    <div className="aip-evidence-tags">
                                      {evRefs.map((ref, rIdx) => (
                                        <button
                          type="button"
                          key={rIdx}
                          onClick={() => handleEvidenceClick(ref)}
                          className="aip-evidence-tag-btn"
                          title={`Click to view database evidence ${ref}`}
                        >
                          <Tag size={11} />
                          <span>{ref}</span>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <span style={{ fontSize: '0.72rem', color: '#71717A', fontStyle: 'italic' }}>
                      None cited
                    </span>
                  )}
                </div>

                {/* Verification Result */}
                <div className="aip-finding-verif-result">
                  <span className="aip-finding-ev-label">Verification:</span>
                  <span className={`aip-verif-status-badge ${verifStatus.toLowerCase()}`}>
                    {verifStatus === 'VERIFIED' && <CheckCircle size={12} />}
                    {verifStatus === 'PARTIALLY_VERIFIED' && <AlertCircle size={12} />}
                    {verifStatus === 'UNVERIFIED' && <HelpCircle size={12} />}
                    {verifStatus === 'CONTRADICTED' && <ShieldAlert size={12} />}
                    {verifStatus === 'NEEDS_REVIEW' && <AlertTriangle size={12} />}
                    <span>{verifStatus.replace(/_/g, ' ')}</span>
                  </span>
                </div>
              </div>
            </div>

            {/* Actions Bar */}
            <div className="aip-queue-finding-actions">
              <span className="aip-actions-label">Actions:</span>
              <div className="aip-review-btn-group">
                <button
                  type="button"
                  onClick={() => openApproveModal(item)}
                  className={`aip-btn aip-btn-sm aip-btn-approve ${decision === 'APPROVED' ? 'active' : ''}`}
                  title="Approve finding for investigation record"
                >
                  <Check size={13} />
                  <span>{decision === 'APPROVED' ? 'APPROVED' : 'APPROVE'}</span>
                </button>

                <button
                  type="button"
                  onClick={() => openEditModal(item)}
                  className={`aip-btn aip-btn-sm aip-btn-edit ${decision === 'EDITED' ? 'active' : ''}`}
                  title="Edit finding and store reviewed version"
                >
                  <Edit3 size={13} />
                  <span>{decision === 'EDITED' ? 'EDITED' : 'EDIT'}</span>
                </button>

                <button
                  type="button"
                  onClick={() => openRejectModal(item)}
                  className={`aip-btn aip-btn-sm aip-btn-reject ${decision === 'REJECTED' ? 'active' : ''}`}
                  title="Reject this finding with reason"
                >
                  <XCircle size={13} />
                  <span>{decision === 'REJECTED' ? 'REJECTED' : 'REJECT'}</span>
                </button>
              </div>
            </div>
          </div>
        );
      })
    ) : (
      <div className="aip-empty-box" style={{ padding: '24px 16px' }}>
        <Clock size={28} style={{ color: '#FBBF24' }} />
        <h4 className="aip-empty-title">Loading Findings Queue...</h4>
        <p className="aip-empty-desc">
          Retrieving findings and verification decisions for this case dossier...
        </p>
      </div>
    )}
  </div>
</div>

                {/* 3. Executive Case Summary */}
                <div className="aip-card">
                  <div className="aip-card-header">
                    <div className="aip-card-title-group">
                      <FileText size={16} style={{ color: 'var(--bright-crime-red)' }} />
                      <h3 className="aip-card-title">Executive Case Summary</h3>
                    </div>
                  </div>
                  <p className="aip-summary-text">
                    {analysisResult.summary}
                  </p>
                </div>

                {/* 4. Evidence-Grounded Observations */}
                {analysisResult.key_observations && analysisResult.key_observations.length > 0 && (
                  <div className="aip-section-card">
                    <div className="aip-section-header">
                      <div className="aip-section-title-group">
                        <Fingerprint size={16} style={{ color: 'var(--bright-crime-red)' }} />
                        <h3 className="aip-section-title">
                          Evidence-Grounded Observations ({analysisResult.key_observations.length})
                        </h3>
                      </div>
                      <span className="aip-section-subtitle">Fact Checking</span>
                    </div>

                    <div className="aip-items-stack">
                      {analysisResult.key_observations.map((obs, idx) => (
                        <div key={idx} className="aip-item-card">
                          <p className="aip-item-title">
                            {obs.observation}
                          </p>
                          {obs.reasoning && (
                            <div className="aip-item-reasoning">
                              Reasoning: {obs.reasoning}
                            </div>
                          )}
                          {obs.evidence_references && obs.evidence_references.length > 0 && (
                            <div className="aip-evidence-tags">
                              <span className="aip-evidence-tag-label">Cites Evidence:</span>
                              {obs.evidence_references.map((ref, rIdx) => (
                                <button
                                  type="button"
                                  key={rIdx}
                                  onClick={() => handleEvidenceClick(ref)}
                                  className="aip-evidence-tag-btn"
                                  title={`View evidence record ${ref}`}
                                >
                                  <Tag size={11} />
                                  <span>{ref}</span>
                                </button>
                              ))}
                            </div>
                          )}

                          {/* Phase 11 Verification Details */}
                          {renderVerificationWidget(obs.verification)}

                          {/* Phase 12 Investigator Review Controls */}
                          {renderReviewControls(obs, 'OBSERVATION')}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 5. Potential Leads & Hypotheses */}
                {analysisResult.potential_leads && analysisResult.potential_leads.length > 0 && (
                  <div className="aip-section-card">
                    <div className="aip-section-header">
                      <div className="aip-section-title-group">
                        <Compass size={16} style={{ color: 'var(--bright-crime-red)' }} />
                        <h3 className="aip-section-title">
                          Investigative Leads & Hypotheses ({analysisResult.potential_leads.length})
                        </h3>
                      </div>
                      <span className="aip-section-subtitle">Hypotheses to Validate</span>
                    </div>

                    <div className="aip-grid-2col">
                      {analysisResult.potential_leads.map((lead, idx) => (
                        <div key={idx} className="aip-item-card">
                          <div className="aip-item-header">
                            <h4 className="aip-item-title">{lead.lead}</h4>
                            <span className={`aip-badge ${getPriorityBadgeClass(lead.priority)}`}>
                              {lead.priority || 'MEDIUM'}
                            </span>
                          </div>
                          {lead.basis && (
                            <p className="aip-item-reasoning" style={{ margin: 0 }}>
                              Basis: {lead.basis}
                            </p>
                          )}
                          {lead.evidence_references && lead.evidence_references.length > 0 && (
                            <div className="aip-evidence-tags">
                              <span className="aip-evidence-tag-label">Ref:</span>
                              {lead.evidence_references.map((ref, rIdx) => (
                                <button
                                  type="button"
                                  key={rIdx}
                                  onClick={() => handleEvidenceClick(ref)}
                                  className="aip-evidence-tag-btn"
                                >
                                  <Tag size={11} />
                                  <span>{ref}</span>
                                </button>
                              ))}
                            </div>
                          )}

                          {/* Phase 11 Verification Details */}
                          {renderVerificationWidget(lead.verification)}

                          {/* Phase 12 Investigator Review Controls */}
                          {renderReviewControls(lead, 'POTENTIAL_LEAD')}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 6. Recommended Actions */}
                {analysisResult.recommended_actions && analysisResult.recommended_actions.length > 0 && (
                  <div className="aip-section-card">
                    <div className="aip-section-header">
                      <div className="aip-section-title-group">
                        <ArrowRight size={16} style={{ color: 'var(--bright-crime-red)' }} />
                        <h3 className="aip-section-title">
                          Suggested Next Actions ({analysisResult.recommended_actions.length})
                        </h3>
                      </div>
                      <span className="aip-section-subtitle">Investigator Discretion</span>
                    </div>

                    <div className="aip-items-stack">
                      {analysisResult.recommended_actions.map((act, idx) => (
                        <div key={idx} className="aip-item-card">
                          <div className="aip-item-header">
                            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
                              <CheckCircle size={15} style={{ color: '#10B981', flexShrink: 0, marginTop: '2px' }} />
                              <span className="aip-item-title">{act.action}</span>
                            </div>
                            <span className={`aip-badge ${getPriorityBadgeClass(act.priority)}`}>
                              {act.priority || 'HIGH'}
                            </span>
                          </div>

                          {act.reason && (
                            <p className="aip-item-reasoning" style={{ paddingLeft: '24px', margin: 0 }}>
                              Justification: {act.reason}
                            </p>
                          )}

                          {act.evidence_references && act.evidence_references.length > 0 && (
                            <div className="aip-evidence-tags" style={{ paddingLeft: '24px' }}>
                              <span className="aip-evidence-tag-label">Cites Evidence:</span>
                              {act.evidence_references.map((ref, rIdx) => (
                                <button
                                  type="button"
                                  key={rIdx}
                                  onClick={() => handleEvidenceClick(ref)}
                                  className="aip-evidence-tag-btn"
                                >
                                  <Tag size={11} />
                                  <span>{ref}</span>
                                </button>
                              ))}
                            </div>
                          )}

                          {/* Phase 11 Verification Details */}
                          {renderVerificationWidget(act.verification)}

                          {/* Phase 12 Investigator Review Controls */}
                          {renderReviewControls(act, 'RECOMMENDED_ACTION')}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 7. Evidence Gaps & Contradictions Grid */}
                <div className="aip-grid-2col">
                  {analysisResult.evidence_gaps && analysisResult.evidence_gaps.length > 0 && (
                    <div className="aip-section-card">
                      <div className="aip-section-header">
                        <div className="aip-section-title-group">
                          <AlertTriangle size={15} style={{ color: '#FBBF24' }} />
                          <h4 className="aip-section-title">
                            Evidence Gaps ({analysisResult.evidence_gaps.length})
                          </h4>
                        </div>
                      </div>

                      <div className="aip-items-stack">
                        {analysisResult.evidence_gaps.map((gap, idx) => (
                          <div key={idx} className="aip-item-card">
                            <div className="aip-item-header">
                              <span className="aip-item-title">{gap.gap}</span>
                              <span className={`aip-badge ${getPriorityBadgeClass(gap.severity)}`}>
                                {gap.severity || 'MEDIUM'}
                              </span>
                            </div>
                            {gap.reason && (
                              <p className="aip-item-reasoning" style={{ margin: 0 }}>
                                {gap.reason}
                              </p>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="aip-section-card">
                    <div className="aip-section-header">
                      <div className="aip-section-title-group">
                        <AlertCircle size={15} style={{ color: '#FF6B6B' }} />
                        <h4 className="aip-section-title">Contradictions & Discrepancies</h4>
                      </div>
                    </div>

                    {analysisResult.contradictions && analysisResult.contradictions.length > 0 ? (
                      <div className="aip-items-stack">
                        {analysisResult.contradictions.map((contra, idx) => (
                          <div key={idx} className="aip-item-card" style={{ borderColor: 'rgba(239, 68, 68, 0.4)' }}>
                            <p style={{ fontSize: '0.8rem', color: '#FECACA', margin: 0 }}>{contra.description}</p>
                            {renderVerificationWidget(contra.verification)}
                            {renderReviewControls(contra, 'CONTRADICTION')}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div style={{ padding: '24px 16px', backgroundColor: '#17171A', border: '1px solid #2A2A2D', borderRadius: '6px', textAlign: 'center' }}>
                        <span style={{ fontSize: '0.8rem', color: '#6EE7B7', fontWeight: 600, display: 'block' }}>
                          No Direct Contradictions Detected
                        </span>
                        <span style={{ fontSize: '0.74rem', color: '#A7A7AA', marginTop: '4px', display: 'block' }}>
                          All cataloged statements and evidence timelines remain mutually consistent.
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                {/* 8. Uncertainties & Limitations */}
                <div className="aip-grid-2col">
                  <div className="aip-card">
                    <div className="aip-card-header">
                      <div className="aip-card-title-group">
                        <HelpCircle size={15} style={{ color: '#A7A7AA' }} />
                        <h4 className="aip-card-title">Investigative Uncertainties</h4>
                      </div>
                    </div>
                    <ul style={{ listStyleType: 'disc', paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '6px', margin: 0 }}>
                      {(analysisResult.uncertainties || []).map((u, idx) => (
                        <li key={idx} style={{ fontSize: '0.76rem', color: '#A7A7AA', lineHeight: '1.5' }}>{u}</li>
                      ))}
                    </ul>
                  </div>

                  <div className="aip-card">
                    <div className="aip-card-header">
                      <div className="aip-card-title-group">
                        <Lock size={15} style={{ color: '#A7A7AA' }} />
                        <h4 className="aip-card-title">Advisory Limitations</h4>
                      </div>
                    </div>
                    <ul style={{ listStyleType: 'disc', paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '6px', margin: 0 }}>
                      {(analysisResult.limitations || []).map((l, idx) => (
                        <li key={idx} style={{ fontSize: '0.76rem', color: '#A7A7AA', lineHeight: '1.5' }}>{l}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>
            )}
          </>
        )}

        {/* ==========================================================================
            SUBTAB 2: SIMILAR CASE INTELLIGENCE (RAG)
            ========================================================================== */}
        {activeSubTab === 'SIMILAR_CASES' && (
          <div className="aip-rag-container">
            {/* SECTION 1: Case Vector Indexing Status Card */}
            <section className="aip-card aip-rag-index-card">
              <div className="aip-section-header">
                <div>
                  <h3 className="aip-section-title">Case Vector Indexing Status</h3>
                  <p className="aip-section-description">
                    Index FIR, evidence items, and case narrative to make this case retrievable by authorized precinct officers.
                  </p>
                </div>
                <span className={`aip-status-badge ${indexingStatus === 'INDEXED' ? 'online' : indexingStatus === 'INDEXING' ? 'indexing' : 'warning'}`}>
                  {indexingStatus === 'INDEXING' ? (
                    <>
                      <RotateCw size={11} className="animate-spin" />
                      <span>INDEXING...</span>
                    </>
                  ) : indexingStatus === 'NOT_INDEXED' ? (
                    <span>NOT INDEXED</span>
                  ) : (
                    <span>INDEXED</span>
                  )}
                </span>
              </div>

              <div>
                <button
                  type="button"
                  onClick={handleIndexCase}
                  disabled={indexingCase}
                  className="aip-btn aip-btn-secondary"
                  title="Re-index this case into vector database"
                >
                  <Database size={14} className={indexingCase ? 'animate-spin' : ''} />
                  <span>{indexingCase ? 'INDEXING CHUNKS...' : 'RE-INDEX CASE KNOWLEDGE'}</span>
                </button>
              </div>

              {indexSuccess && (
                <div className="aip-modal-alert" style={{ backgroundColor: 'rgba(6, 78, 59, 0.4)', border: '1px solid rgba(16, 185, 129, 0.5)', color: '#6EE7B7', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <CheckCircle size={15} />
                  <span>{indexSuccess}</span>
                </div>
              )}
            </section>

            {/* SECTION 2: Custom Similarity Search Card */}
            <section className="aip-card aip-rag-search-card">
              <div className="aip-section-title">
                <Search size={16} style={{ color: 'var(--bright-crime-red)' }} />
                <span>CUSTOM SIMILARITY QUERY</span>
              </div>

              <form onSubmit={handleSearchSimilar} className="aip-search-row">
                <input
                  type="text"
                  placeholder="Enter a query about this investigation (e.g. 'gold theft midnight getaway motorcycle')..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="aip-search-input"
                />
                <button
                  type="submit"
                  disabled={searchingSimilar}
                  className="aip-btn aip-btn-search"
                >
                  <Search size={14} />
                  <span>{searchingSimilar ? 'SEARCHING...' : 'FIND MATCHES'}</span>
                </button>
              </form>

              {retrievalError && (
                <div className="aip-modal-alert error">
                  {retrievalError}
                </div>
              )}
            </section>

            {/* SECTION 3: Results Card (Similar Investigations & Historical RAG Context) */}
            <section className="aip-card aip-rag-results-card">
              <div className="aip-rag-section-header">
                <div className="aip-rag-section-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <BookOpen size={16} style={{ color: '#C084FC', flexShrink: 0 }} />
                  <h3 className="aip-section-title">
                    SIMILAR INVESTIGATIONS & HISTORICAL RAG CONTEXT
                  </h3>
                  {retrievalResults.length > 0 && (
                    <span className="aip-rag-count-badge">
                      {retrievalResults.length} MATCH{retrievalResults.length === 1 ? '' : 'ES'}
                    </span>
                  )}
                </div>
                <span className="aip-rag-threshold aip-rag-threshold-note">Threshold: ≥70% Cosine Similarity</span>
              </div>

              {searchingSimilar && (
                <div className="aip-empty-box">
                  <RotateCw size={28} className="animate-spin" style={{ color: 'var(--bright-crime-red)' }} />
                  <p className="aip-empty-desc">
                    Calculating 384-dimensional cosine similarity across authorized case chunks...
                  </p>
                </div>
              )}

              {!searchingSimilar && retrievalResults.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginTop: '6px' }}>
                  {retrievalResults.map((item, idx) => {
                    const isCurrent = item.is_current_case || (item.case_id && item.case_id === caseId);
                    const rankStr = `MATCH ${String(idx + 1).padStart(2, '0')}`;
                    const matchPercent = `${Math.round((item.similarity || 0) * 100)}% MATCH`;
                    const parsed = parseChunkContent(item.content);

                    return (
                      <div key={idx} className="aip-rag-result-card">
                        <div className="aip-result-header">
                          <div className="aip-result-meta">
                            <span className="aip-result-match-rank">{rankStr}</span>
                            <span className={`aip-result-badge ${isCurrent ? 'current' : 'historical'}`}>
                              {isCurrent ? 'CURRENT CASE CONTEXT' : 'HISTORICAL RETRIEVED CONTEXT'}
                            </span>
                            <span className="aip-result-badge source">
                              {item.source_type}
                            </span>
                          </div>
                          <span className="aip-result-similarity-badge">
                            {matchPercent}
                          </span>
                        </div>

                        <div className="aip-result-body">
                          <div className="aip-case-context-grid">
                            <div className="aip-field">
                              <span className="aip-field-label">CASE</span>
                              <strong className="aip-field-value mono">
                                {item.case_number || 'Historical Case'}
                              </strong>
                            </div>
                            <div className="aip-field">
                              <span className="aip-field-label">Classification</span>
                              <strong className="aip-field-value">
                                {item.case_type || 'CRIMINAL INVESTIGATION'}
                              </strong>
                            </div>

                            {parsed.fields.map((f, fIdx) => {
                              if (f.label && ['case', 'case number', 'classification', 'case type'].includes(f.label.toLowerCase())) {
                                return null;
                              }
                              const isLong = ['reason', 'description', 'details', 'narrative', 'notes', 'suggested verification'].some(
                                k => f.label.toLowerCase().includes(k)
                              ) || (!f.label && f.value.length > 60);

                              return (
                                <div key={fIdx} className="aip-field" style={isLong ? { gridColumn: '1 / -1' } : {}}>
                                  {f.label && <span className="aip-field-label">{f.label}</span>}
                                  <div className={`aip-field-value ${isLong ? 'box' : ''}`}>
                                    {f.value}
                                  </div>
                                </div>
                              );
                            })}
                          </div>

                          <div className="aip-field" style={{ marginTop: '8px' }}>
                            <span className="aip-field-label">Scope</span>
                            <span className="aip-field-value" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#A7A7AA' }}>
                              <Lock size={12} />
                              <span>Authorized Station Isolation Enforced</span>
                            </span>
                          </div>
                        </div>

                        <div className="aip-result-warning">
                          <AlertTriangle size={13} style={{ color: '#F59E0B', flexShrink: 0 }} />
                          <span>Untrusted reference material — decision support only</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {!searchingSimilar && retrievalResults.length === 0 && (
                <div className="aip-empty-box">
                  <BookOpen size={30} style={{ color: '#71717A' }} />
                  <h4 className="aip-empty-title">No Similar Cases Found</h4>
                  <p className="aip-empty-desc">
                    No historical cases met the 70% cosine similarity threshold for this query. Make sure historical cases have been indexed into the vector database.
                  </p>
                </div>
              )}
            </section>

            {/* SECTION 4: Current Case Context Card (Semantic Grid) */}
            <section className="aip-card aip-rag-current-case-card">
              <div className="aip-current-case-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                  <Fingerprint size={16} style={{ color: '#6EE7B7', flexShrink: 0 }} />
                  <h3 className="aip-section-title">CURRENT CASE CONTEXT</h3>
                </div>
                <span className="aip-current-case-scope" style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', fontSize: '0.72rem', color: '#A7A7AA' }}>
                  <Lock size={12} style={{ flexShrink: 0 }} />
                  <span>Authorized Station Isolation Enforced</span>
                </span>
              </div>

              <div className="aip-case-context-grid" style={{ marginTop: '4px' }}>
                <div className="aip-field">
                  <span className="aip-field-label">Case Number</span>
                  <strong className="aip-field-value mono">{currentCaseNum}</strong>
                </div>

                <div className="aip-field">
                  <span className="aip-field-label">Classification</span>
                  <strong className="aip-field-value">{currentClassification}</strong>
                </div>

                <div className="aip-field">
                  <span className="aip-field-label">Title</span>
                  <strong className="aip-field-value">{currentTitle}</strong>
                </div>

                <div className="aip-field">
                  <span className="aip-field-label">Status</span>
                  <strong className="aip-field-value">{currentStatus}</strong>
                </div>

                <div className="aip-field" style={{ gridColumn: '1 / -1' }}>
                  <span className="aip-field-label">Narrative</span>
                  <div className="aip-field-value box">
                    {currentNarrative}
                  </div>
                </div>

                <div className="aip-field" style={{ gridColumn: '1 / -1' }}>
                  <span className="aip-field-label">Scope</span>
                  <div className="aip-field-value" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#A7A7AA' }}>
                    <Lock size={12} />
                    <span>Authorized Station Isolation Enforced</span>
                  </div>
                </div>
              </div>
            </section>

            {/* SECTION 5: Responsible AI & Human-in-the-Loop Safeguards Card */}
            <section className="aip-card aip-rag-safeguards-card">
              <div className="aip-section-title" style={{ color: '#FBBF24', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ShieldAlert size={16} />
                <span>RESPONSIBLE AI & HUMAN-IN-THE-LOOP SAFEGUARDS</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '6px' }}>
                <p className="aip-section-description" style={{ color: '#E4E4E7' }}>
                  Investigator approval indicates human acceptance of the recommendation for case workflow purposes.
                </p>
                <p className="aip-section-description" style={{ color: '#D4D4D8' }}>
                  It does not establish guilt, innocence, or factual certainty.
                </p>
                <p className="aip-section-description" style={{ color: '#71717A', fontStyle: 'italic' }}>
                  AI-generated recommendations remain distinguishable from investigator-edited decisions.
                </p>
              </div>
            </section>
          </div>
        )}

        {/* ==========================================================================
            PHASE 12: INVESTIGATOR REVIEW MODALS
            ========================================================================== */}

        {/* 1. APPROVE MODAL */}
        {activeModal === 'APPROVE' && selectedItem && (
          <div className="aip-modal-overlay">
            <div className="aip-modal-card">
              <div className="aip-modal-header">
                <h3 className="aip-modal-title">
                  <CheckCircle size={18} style={{ color: '#10B981' }} />
                  <span>Approve AI Finding for Record</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setActiveModal(null)}
                  className="aip-modal-close-btn"
                >
                  <XCircle size={18} />
                </button>
              </div>

              <div className="aip-modal-body">
                <div className="aip-form-group">
                  <span className="aip-form-label">AI Finding Content</span>
                  <div style={{ padding: '10px 12px', backgroundColor: '#17171A', border: '1px solid #2A2A2D', borderRadius: '6px', fontSize: '0.78rem', color: '#E4E4E7', lineHeight: '1.5' }}>
                    {selectedItem.observation || selectedItem.lead || selectedItem.action || selectedItem.description || selectedItem.content}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.75rem', padding: '0 2px' }}>
                  <span style={{ color: '#A7A7AA' }}>Verification State:</span>
                  <span style={{ fontFamily: 'monospace', fontWeight: 700, color: '#F4F4F5' }}>
                    {selectedItem.verification?.status || 'UNVERIFIED'} ({selectedItem.verification?.support_level || 'NONE'})
                  </span>
                </div>

                {/* Warning if UNSUPPORTED */}
                {selectedItem.verification?.status === 'UNSUPPORTED' && (
                  <div className="aip-modal-alert warning">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 700 }}>
                      <AlertTriangle size={15} style={{ color: '#F59E0B' }} />
                      <span>Unsupported Finding Warning</span>
                    </div>
                    <p style={{ margin: 0 }}>
                      This finding is currently unsupported by available evidence. Explicit confirmation is required to record approval.
                    </p>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', marginTop: '4px' }}>
                      <input
                        type="checkbox"
                        checked={confirmUnsupported}
                        onChange={(e) => setConfirmUnsupported(e.target.checked)}
                        style={{ cursor: 'pointer' }}
                      />
                      <span style={{ fontWeight: 600, color: '#FFFFFF' }}>
                        I understand this finding is currently marked unsupported.
                      </span>
                    </label>
                  </div>
                )}

                <div className="aip-form-group">
                  <label className="aip-form-label">
                    Investigator Note / Comment (Optional)
                  </label>
                  <textarea
                    rows={2}
                    value={approveComment}
                    onChange={(e) => setApproveComment(e.target.value)}
                    placeholder="Add context or notes regarding this approval..."
                    className="aip-textarea"
                  />
                </div>

                {reviewActionError && (
                  <div className="aip-modal-alert error">
                    {reviewActionError}
                  </div>
                )}
              </div>

              <div className="aip-modal-footer">
                <button
                  type="button"
                  onClick={() => setActiveModal(null)}
                  className="aip-btn aip-btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleApproveSubmit}
                  disabled={reviewSubmitting || (selectedItem.verification?.status === 'UNSUPPORTED' && !confirmUnsupported)}
                  className="aip-btn aip-btn-primary"
                  style={{ background: 'linear-gradient(135deg, #065F46 0%, #047857 100%)', borderColor: '#10B981' }}
                >
                  {reviewSubmitting ? <RotateCw size={13} className="animate-spin" /> : <Check size={13} />}
                  <span>Confirm Approval</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 2. EDIT MODAL */}
        {activeModal === 'EDIT' && selectedItem && (
          <div className="aip-modal-overlay">
            <div className="aip-modal-card">
              <div className="aip-modal-header">
                <h3 className="aip-modal-title">
                  <Edit3 size={18} style={{ color: '#38BDF8' }} />
                  <span>Edit AI Finding</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setActiveModal(null)}
                  className="aip-modal-close-btn"
                >
                  <XCircle size={18} />
                </button>
              </div>

              <div className="aip-modal-body">
                {/* Original AI Finding (Read Only) */}
                <div className="aip-form-group">
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span className="aip-form-label">Original AI Finding</span>
                    <span style={{ fontSize: '0.68rem', color: '#71717A', fontFamily: 'monospace' }}>Immutable</span>
                  </div>
                  <div style={{ padding: '10px 12px', backgroundColor: '#17171A', border: '1px solid #2A2A2D', borderRadius: '6px', fontSize: '0.76rem', color: '#A1A1AA', fontStyle: 'italic', lineHeight: '1.5' }}>
                    {selectedItem.observation || selectedItem.lead || selectedItem.action || selectedItem.description || selectedItem.content}
                  </div>
                </div>

                {/* Investigator-Edited Version */}
                <div className="aip-form-group">
                  <label className="aip-form-label">
                    Investigator Edited Version <span style={{ color: '#EF4444' }}>*</span>
                  </label>
                  <textarea
                    rows={3}
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    placeholder="Enter corrected, clarified, or refined finding..."
                    className="aip-textarea"
                  />
                </div>

                {/* Case Evidence Picker */}
                <div className="aip-form-group">
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <label className="aip-form-label">Case Evidence References</label>
                    <span style={{ fontSize: '0.68rem', color: '#71717A' }}>Must belong to current case</span>
                  </div>
                  {caseEvidenceList.length > 0 ? (
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', maxHeight: '96px', overflowY: 'auto', padding: '8px', backgroundColor: '#17171A', borderRadius: '6px', border: '1px solid #2A2A2D' }}>
                      {caseEvidenceList.map((ev, eIdx) => {
                        const num = ev.evidenceNumber;
                        const isSelected = editEvidenceRefs.includes(num);
                        return (
                          <button
                            key={eIdx}
                            type="button"
                            onClick={() => {
                              if (isSelected) {
                                setEditEvidenceRefs(editEvidenceRefs.filter(r => r !== num));
                              } else {
                                setEditEvidenceRefs([...editEvidenceRefs, num]);
                              }
                            }}
                            className="aip-btn aip-btn-sm"
                            style={{
                              backgroundColor: isSelected ? 'rgba(12, 74, 110, 0.8)' : 'rgba(39, 39, 42, 0.7)',
                              borderColor: isSelected ? '#0EA5E9' : '#3F3F46',
                              color: isSelected ? '#38BDF8' : '#A1A1AA',
                              fontFamily: 'monospace',
                              fontSize: '0.68rem',
                              padding: '2px 8px'
                            }}
                          >
                            {num} {isSelected ? '(Selected)' : '+'}
                          </button>
                        );
                      })}
                    </div>
                  ) : (
                    <div style={{ fontSize: '0.72rem', color: '#71717A', fontStyle: 'italic' }}>
                      No case evidence records cataloged yet.
                    </div>
                  )}
                </div>

                {/* Comment / Reason */}
                <div className="aip-form-group">
                  <label className="aip-form-label">Reason for Modification (Recommended)</label>
                  <textarea
                    rows={2}
                    value={editComment}
                    onChange={(e) => setEditComment(e.target.value)}
                    placeholder="Explain why this finding was modified..."
                    className="aip-textarea"
                  />
                </div>

                {reviewActionError && (
                  <div className="aip-modal-alert error">
                    {reviewActionError}
                  </div>
                )}
              </div>

              <div className="aip-modal-footer">
                <button
                  type="button"
                  onClick={() => setActiveModal(null)}
                  className="aip-btn aip-btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleEditSubmit}
                  disabled={reviewSubmitting || !editContent.trim()}
                  className="aip-btn aip-btn-primary"
                  style={{ background: 'linear-gradient(135deg, #075985 0%, #0284C7 100%)', borderColor: '#38BDF8' }}
                >
                  {reviewSubmitting ? <RotateCw size={13} className="animate-spin" /> : <Edit3 size={13} />}
                  <span>Save Edited Finding</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 3. REJECT MODAL */}
        {activeModal === 'REJECT' && selectedItem && (
          <div className="aip-modal-overlay">
            <div className="aip-modal-card">
              <div className="aip-modal-header">
                <h3 className="aip-modal-title">
                  <XCircle size={18} style={{ color: '#EF4444' }} />
                  <span>Reject AI Finding</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setActiveModal(null)}
                  className="aip-modal-close-btn"
                >
                  <XCircle size={18} />
                </button>
              </div>

              <div className="aip-modal-body">
                <div className="aip-form-group">
                  <span className="aip-form-label">AI Finding Content</span>
                  <div style={{ padding: '10px 12px', backgroundColor: '#17171A', border: '1px solid #2A2A2D', borderRadius: '6px', fontSize: '0.78rem', color: '#E4E4E7', lineHeight: '1.5' }}>
                    {selectedItem.observation || selectedItem.lead || selectedItem.action || selectedItem.description || selectedItem.content}
                  </div>
                </div>

                <div className="aip-form-group">
                  <label className="aip-form-label" style={{ color: '#FCA5A5' }}>
                    Reason for Rejection <span style={{ color: '#EF4444' }}>* (Required)</span>
                  </label>
                  <textarea
                    rows={3}
                    value={rejectComment}
                    onChange={(e) => setRejectComment(e.target.value)}
                    placeholder="Explain why this finding is being rejected (e.g. not supported by evidence, outside scope)..."
                    className="aip-textarea"
                    style={{ borderColor: 'rgba(239, 68, 68, 0.4)' }}
                  />
                </div>

                {reviewActionError && (
                  <div className="aip-modal-alert error">
                    {reviewActionError}
                  </div>
                )}
              </div>

              <div className="aip-modal-footer">
                <button
                  type="button"
                  onClick={() => setActiveModal(null)}
                  className="aip-btn aip-btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleRejectSubmit}
                  disabled={reviewSubmitting || !rejectComment.trim()}
                  className="aip-btn aip-btn-primary"
                >
                  {reviewSubmitting ? <RotateCw size={13} className="animate-spin" /> : <XCircle size={13} />}
                  <span>Reject Finding</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Panel>
  );
}
