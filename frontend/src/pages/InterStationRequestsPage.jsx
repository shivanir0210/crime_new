import React, { useState, useEffect, useCallback } from 'react';
import TopBar from '../components/TopBar';
import Sidebar from '../components/Sidebar';
import { 
  getSentInterStationRequests, 
  getReceivedInterStationRequests, 
  acceptInterStationRequest, 
  rejectInterStationRequest, 
  respondToInterStationRequest 
} from '../services/api';
import { 
  Building2, 
  Send, 
  Inbox, 
  CheckCircle, 
  XCircle, 
  Clock, 
  AlertTriangle, 
  ShieldAlert, 
  Search, 
  RefreshCw, 
  FileText, 
  User, 
  MapPin, 
  Mail, 
  CheckSquare, 
  MessageSquare, 
  ArrowRight, 
  Info,
  ShieldCheck,
  CheckCircle2,
  X
} from 'lucide-react';

export default function InterStationRequestsPage() {
  const [activeTab, setActiveTab] = useState('received'); // 'received' | 'sent'
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  
  const [receivedRequests, setReceivedRequests] = useState([]);
  const [sentRequests, setSentRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Action modals
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [modalType, setModalType] = useState(null); // 'reject' | 'respond' | 'details'
  const [modalNotes, setModalNotes] = useState('');

  const loadRequests = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [received, sent] = await Promise.all([
        getReceivedInterStationRequests().catch(err => {
          console.error('Failed to load received requests:', err);
          return [];
        }),
        getSentInterStationRequests().catch(err => {
          console.error('Failed to load sent requests:', err);
          return [];
        })
      ]);
      setReceivedRequests(received || []);
      setSentRequests(sent || []);
    } catch (err) {
      setError(`Error loading coordination requests: ${err.message}`);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  const activeList = activeTab === 'received' ? receivedRequests : sentRequests;

  const filteredRequests = activeList.filter(req => {
    if (statusFilter !== 'ALL' && req.status !== statusFilter) {
      return false;
    }
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      (req.caseNumber && req.caseNumber.toLowerCase().includes(q)) ||
      (req.caseTitle && req.caseTitle.toLowerCase().includes(q)) ||
      (req.victimName && req.victimName.toLowerCase().includes(q)) ||
      (req.sourceStationName && req.sourceStationName.toLowerCase().includes(q)) ||
      (req.destinationStationName && req.destinationStationName.toLowerCase().includes(q)) ||
      (req.reason && req.reason.toLowerCase().includes(q))
    );
  });

  // Handlers for workflow actions
  const handleAccept = async (reqId) => {
    try {
      setActionLoading(true);
      setError('');
      await acceptInterStationRequest(reqId);
      setSuccessMessage('Inter-station coordination request accepted successfully.');
      await loadRequests();
    } catch (err) {
      setError(`Failed to accept request: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async (e) => {
    e.preventDefault();
    if (!selectedRequest) return;
    try {
      setActionLoading(true);
      setError('');
      await rejectInterStationRequest(selectedRequest.id, modalNotes);
      setSuccessMessage('Coordination request rejected.');
      setModalType(null);
      setSelectedRequest(null);
      setModalNotes('');
      await loadRequests();
    } catch (err) {
      setError(`Failed to reject request: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRespondSubmit = async (e) => {
    e.preventDefault();
    if (!selectedRequest) return;
    try {
      setActionLoading(true);
      setError('');
      await respondToInterStationRequest(selectedRequest.id, modalNotes);
      setSuccessMessage('Coordination response dispatched and request marked completed.');
      setModalType(null);
      setSelectedRequest(null);
      setModalNotes('');
      await loadRequests();
    } catch (err) {
      setError(`Failed to submit response: ${err.message}`);
    } finally {
      setActionLoading(false);
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return 'N/A';
    try {
      return new Date(dateStr).toLocaleString('en-US', {
        month: 'short',
        day: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateStr;
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'SENT':
        return (
          <span style={{
            backgroundColor: '#3b82f620',
            color: '#60a5fa',
            border: '1px solid #3b82f650',
            padding: '3px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <Clock size={12} /> PENDING REVIEW
          </span>
        );
      case 'ACCEPTED':
        return (
          <span style={{
            backgroundColor: '#10b98120',
            color: '#34d399',
            border: '1px solid #10b98150',
            padding: '3px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <CheckCircle2 size={12} /> ACCEPTED (IN PROGRESS)
          </span>
        );
      case 'COMPLETED':
        return (
          <span style={{
            backgroundColor: '#05966920',
            color: '#10b981',
            border: '1px solid #05966950',
            padding: '3px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <ShieldCheck size={12} /> COMPLETED
          </span>
        );
      case 'REJECTED':
        return (
          <span style={{
            backgroundColor: '#ef444420',
            color: '#f87171',
            border: '1px solid #ef444450',
            padding: '3px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <XCircle size={12} /> REJECTED
          </span>
        );
      case 'DELIVERY_FAILED':
        return (
          <span style={{
            backgroundColor: '#f59e0b20',
            color: '#fbbf24',
            border: '1px solid #f59e0b50',
            padding: '3px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px'
          }}>
            <AlertTriangle size={12} /> DELIVERY FAILED
          </span>
        );
      default:
        return (
          <span style={{
            backgroundColor: '#47556920',
            color: '#94a3b8',
            border: '1px solid #47556950',
            padding: '3px 8px',
            borderRadius: '4px',
            fontSize: '0.75rem',
            fontWeight: 600
          }}>
            {status}
          </span>
        );
    }
  };

  return (
    <div className="app-container" style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#0b0f19', color: '#f8fafc' }}>
      <Sidebar />
      <div className="main-content" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflowX: 'hidden' }}>
        <TopBar />
        
        <div style={{ padding: '1.75rem 2rem', maxWidth: '1400px', margin: '0 auto', width: '100%', boxSizing: 'border-box' }}>
          
          {/* Header Banner */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            marginBottom: '1.5rem',
            flexWrap: 'wrap',
            gap: '1rem'
          }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.35rem' }}>
                <div style={{
                  backgroundColor: '#3b82f620',
                  color: '#60a5fa',
                  padding: '8px',
                  borderRadius: '8px'
                }}>
                  <Building2 size={24} />
                </div>
                <h1 style={{ fontSize: '1.6rem', fontWeight: 700, margin: 0, color: '#f8fafc' }}>
                  Inter-Station Coordination
                </h1>
              </div>
              <p style={{ color: '#94a3b8', margin: 0, fontSize: '0.9rem' }}>
                Manage cross-jurisdictional crime case transfers, victim inquiries, and police station requests.
              </p>
            </div>

            <button
              onClick={loadRequests}
              disabled={loading}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                backgroundColor: '#1e293b',
                color: '#cbd5e1',
                border: '1px solid #334155',
                padding: '0.55rem 1rem',
                borderRadius: '6px',
                fontSize: '0.875rem',
                cursor: 'pointer'
              }}
            >
              <RefreshCw size={15} className={loading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
          </div>

          {/* Feedback Messages */}
          {error && (
            <div style={{
              backgroundColor: '#7f1d1d30',
              border: '1px solid #ef444450',
              color: '#fca5a5',
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <span>{error}</span>
              <button onClick={() => setError('')} style={{ background: 'none', border: 'none', color: '#fca5a5', cursor: 'pointer' }}>
                <X size={16} />
              </button>
            </div>
          )}

          {successMessage && (
            <div style={{
              backgroundColor: '#064e3b30',
              border: '1px solid #10b98150',
              color: '#6ee7b7',
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}>
              <span>{successMessage}</span>
              <button onClick={() => setSuccessMessage('')} style={{ background: 'none', border: 'none', color: '#6ee7b7', cursor: 'pointer' }}>
                <X size={16} />
              </button>
            </div>
          )}

          {/* Tabs & Controls */}
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid #1e293b',
            marginBottom: '1.5rem',
            flexWrap: 'wrap',
            gap: '1rem'
          }}>
            {/* Primary Tab Navigation */}
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                onClick={() => { setActiveTab('received'); setStatusFilter('ALL'); }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.75rem 1.25rem',
                  backgroundColor: 'transparent',
                  border: 'none',
                  borderBottom: activeTab === 'received' ? '2px solid #3b82f6' : '2px solid transparent',
                  color: activeTab === 'received' ? '#60a5fa' : '#94a3b8',
                  fontWeight: activeTab === 'received' ? 600 : 500,
                  fontSize: '0.95rem',
                  cursor: 'pointer'
                }}
              >
                <Inbox size={18} />
                <span>Received Requests</span>
                <span style={{
                  backgroundColor: activeTab === 'received' ? '#3b82f620' : '#1e293b',
                  color: activeTab === 'received' ? '#93c5fd' : '#64748b',
                  padding: '2px 8px',
                  borderRadius: '12px',
                  fontSize: '0.75rem'
                }}>
                  {receivedRequests.length}
                </span>
              </button>

              <button
                onClick={() => { setActiveTab('sent'); setStatusFilter('ALL'); }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.75rem 1.25rem',
                  backgroundColor: 'transparent',
                  border: 'none',
                  borderBottom: activeTab === 'sent' ? '2px solid #3b82f6' : '2px solid transparent',
                  color: activeTab === 'sent' ? '#60a5fa' : '#94a3b8',
                  fontWeight: activeTab === 'sent' ? 600 : 500,
                  fontSize: '0.95rem',
                  cursor: 'pointer'
                }}
              >
                <Send size={18} />
                <span>Sent Requests</span>
                <span style={{
                  backgroundColor: activeTab === 'sent' ? '#3b82f620' : '#1e293b',
                  color: activeTab === 'sent' ? '#93c5fd' : '#64748b',
                  padding: '2px 8px',
                  borderRadius: '12px',
                  fontSize: '0.75rem'
                }}>
                  {sentRequests.length}
                </span>
              </button>
            </div>

            {/* Search Input */}
            <div style={{ position: 'relative', width: '280px' }}>
              <Search size={16} style={{ position: 'absolute', left: '10px', top: '10px', color: '#64748b' }} />
              <input
                type="text"
                placeholder="Search case, victim, station..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{
                  width: '100%',
                  backgroundColor: '#1e293b',
                  border: '1px solid #334155',
                  borderRadius: '6px',
                  padding: '0.5rem 0.75rem 0.5rem 2rem',
                  color: '#f8fafc',
                  fontSize: '0.85rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>
          </div>

          {/* Status Filter Pills */}
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
            {['ALL', 'SENT', 'ACCEPTED', 'COMPLETED', 'REJECTED'].map(st => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                style={{
                  backgroundColor: statusFilter === st ? '#3b82f6' : '#1e293b',
                  color: statusFilter === st ? '#ffffff' : '#94a3b8',
                  border: '1px solid',
                  borderColor: statusFilter === st ? '#3b82f6' : '#334155',
                  borderRadius: '20px',
                  padding: '0.3rem 0.85rem',
                  fontSize: '0.78rem',
                  fontWeight: 500,
                  cursor: 'pointer'
                }}
              >
                {st === 'ALL' ? 'All Requests' : st === 'SENT' ? 'Pending' : st}
              </button>
            ))}
          </div>

          {/* Requests Content List */}
          {loading ? (
            <div style={{ textAlign: 'center', padding: '3rem 0', color: '#94a3b8' }}>
              <RefreshCw size={28} className="spin" style={{ margin: '0 auto 1rem auto' }} />
              <p>Loading inter-station requests...</p>
            </div>
          ) : filteredRequests.length === 0 ? (
            <div style={{
              textAlign: 'center',
              padding: '3.5rem 1rem',
              backgroundColor: '#1e293b40',
              borderRadius: '12px',
              border: '1px dashed #334155'
            }}>
              <Inbox size={40} style={{ color: '#475569', margin: '0 auto 1rem auto' }} />
              <h3 style={{ fontSize: '1.1rem', color: '#cbd5e1', margin: '0 0 0.5rem 0' }}>
                No coordination requests found
              </h3>
              <p style={{ color: '#64748b', fontSize: '0.85rem', margin: 0 }}>
                {activeTab === 'received' 
                  ? 'No incoming coordination requests from other police stations matching the current criteria.' 
                  : 'You have not sent any inter-station coordination requests yet.'}
              </p>
            </div>
          ) : (
            <div style={{ display: 'grid', gap: '1rem' }}>
              {filteredRequests.map(req => (
                <div
                  key={req.id}
                  style={{
                    backgroundColor: '#111827',
                    border: '1px solid #1f2937',
                    borderRadius: '10px',
                    padding: '1.25rem',
                    transition: 'border-color 0.2s',
                    position: 'relative'
                  }}
                >
                  {/* Top line: Status & Station Routing */}
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '0.75rem',
                    flexWrap: 'wrap',
                    gap: '0.5rem'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      {getStatusBadge(req.status)}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: '#94a3b8' }}>
                        <span style={{ color: '#f8fafc', fontWeight: 600 }}>{req.sourceStationName}</span>
                        <ArrowRight size={14} style={{ color: '#60a5fa' }} />
                        <span style={{ color: '#60a5fa', fontWeight: 600 }}>{req.destinationStationName}</span>
                      </div>
                    </div>

                    <div style={{ fontSize: '0.8rem', color: '#64748b' }}>
                      Requested: {formatDate(req.createdAt)}
                    </div>
                  </div>

                  {/* Case & Victim Context */}
                  <div style={{
                    backgroundColor: '#1f293780',
                    borderRadius: '6px',
                    padding: '0.75rem 1rem',
                    marginBottom: '0.85rem',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                    gap: '0.75rem',
                    fontSize: '0.85rem'
                  }}>
                    <div>
                      <span style={{ color: '#94a3b8', display: 'block', fontSize: '0.75rem' }}>Case Reference:</span>
                      <strong style={{ color: '#60a5fa' }}>{req.caseNumber || 'N/A'}</strong>
                      <span style={{ color: '#cbd5e1', marginLeft: '6px' }}>— {req.caseTitle || 'No Title'}</span>
                    </div>

                    {req.victimName && (
                      <div>
                        <span style={{ color: '#94a3b8', display: 'block', fontSize: '0.75rem' }}>Subject Victim:</span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#f8fafc', fontWeight: 500 }}>
                          <User size={14} style={{ color: '#94a3b8' }} />
                          <span>{req.victimName}</span>
                          {req.victimAddress && <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>({req.victimAddress})</span>}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Reason & Shared Data Badges */}
                  <div style={{ marginBottom: '0.85rem' }}>
                    <div style={{ fontSize: '0.85rem', color: '#e2e8f0', marginBottom: '0.5rem' }}>
                      <strong style={{ color: '#94a3b8' }}>Reason: </strong>
                      {req.reason}
                    </div>

                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      {req.shareCaseReference && (
                        <span style={{ backgroundColor: '#1e293b', color: '#94a3b8', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem' }}>
                          ✓ Case Details
                        </span>
                      )}
                      {req.shareVictimDetails && (
                        <span style={{ backgroundColor: '#1e293b', color: '#94a3b8', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem' }}>
                          ✓ Victim Info
                        </span>
                      )}
                      {req.shareIncidentSummary && (
                        <span style={{ backgroundColor: '#1e293b', color: '#94a3b8', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem' }}>
                          ✓ Incident Summary
                        </span>
                      )}
                      {req.shareEvidenceSummary && (
                        <span style={{ backgroundColor: '#1e293b', color: '#94a3b8', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem' }}>
                          ✓ Evidence
                        </span>
                      )}
                      {req.shareWitnessInfo && (
                        <span style={{ backgroundColor: '#1e293b', color: '#94a3b8', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem' }}>
                          ✓ Witnesses
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Response Notes (if responded) */}
                  {req.responseNotes && (
                    <div style={{
                      backgroundColor: '#1e293b',
                      borderLeft: '3px solid #3b82f6',
                      padding: '0.65rem 0.85rem',
                      borderRadius: '0 6px 6px 0',
                      marginBottom: '0.85rem',
                      fontSize: '0.85rem'
                    }}>
                      <span style={{ color: '#60a5fa', fontWeight: 600, display: 'block', fontSize: '0.75rem', marginBottom: '2px' }}>
                        Response / Resolution Notes (by {req.respondedByPoliceId || 'Officer'} on {formatDate(req.respondedAt)}):
                      </span>
                      <p style={{ margin: 0, color: '#f8fafc' }}>{req.responseNotes}</p>
                    </div>
                  )}

                  {/* Email Delivery Meta */}
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    paddingTop: '0.65rem',
                    borderTop: '1px solid #1e293b',
                    fontSize: '0.8rem',
                    color: '#64748b',
                    flexWrap: 'wrap',
                    gap: '0.5rem'
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <Mail size={13} />
                        Station Head: <span style={{ color: '#94a3b8' }}>{req.destinationStationHeadEmail || 'N/A'}</span>
                      </span>
                      <span style={{ color: req.emailDeliveryStatus === 'SENT' ? '#34d399' : '#f59e0b' }}>
                        Email: {req.emailDeliveryStatus}
                      </span>
                    </div>

                    {/* Action Controls for Destination Station Officers */}
                    {activeTab === 'received' && (
                      <div style={{ display: 'flex', gap: '0.5rem' }}>
                        {req.status === 'SENT' && (
                          <>
                            <button
                              onClick={() => handleAccept(req.id)}
                              disabled={actionLoading}
                              style={{
                                backgroundColor: '#10b981',
                                color: '#ffffff',
                                border: 'none',
                                padding: '0.35rem 0.85rem',
                                borderRadius: '4px',
                                fontSize: '0.8rem',
                                fontWeight: 600,
                                cursor: 'pointer'
                              }}
                            >
                              Accept Request
                            </button>
                            <button
                              onClick={() => { setSelectedRequest(req); setModalType('reject'); setModalNotes(''); }}
                              disabled={actionLoading}
                              style={{
                                backgroundColor: '#ef444420',
                                color: '#f87171',
                                border: '1px solid #ef444450',
                                padding: '0.35rem 0.85rem',
                                borderRadius: '4px',
                                fontSize: '0.8rem',
                                fontWeight: 600,
                                cursor: 'pointer'
                              }}
                            >
                              Reject
                            </button>
                          </>
                        )}

                        {req.status === 'ACCEPTED' && (
                          <button
                            onClick={() => { setSelectedRequest(req); setModalType('respond'); setModalNotes(''); }}
                            disabled={actionLoading}
                            style={{
                              backgroundColor: '#2563eb',
                              color: '#ffffff',
                              border: 'none',
                              padding: '0.35rem 0.85rem',
                              borderRadius: '4px',
                              fontSize: '0.8rem',
                              fontWeight: 600,
                              cursor: 'pointer'
                            }}
                          >
                            Respond & Complete
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Reject Modal */}
      {modalType === 'reject' && selectedRequest && (
        <div className="modal-overlay" onClick={() => setModalType(null)}>
          <div className="modal-container" onClick={(e) => e.stopPropagation()} style={{
            maxWidth: '500px',
            backgroundColor: '#0f172a',
            color: '#f8fafc',
            border: '1px solid #334155',
            borderRadius: '10px',
            padding: '1.5rem'
          }}>
            <h3 style={{ margin: '0 0 0.5rem 0', color: '#f87171', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <XCircle size={20} />
              Reject Coordination Request
            </h3>
            <p style={{ fontSize: '0.85rem', color: '#94a3b8', margin: '0 0 1rem 0' }}>
              Provide the justification / feedback for rejecting this transfer from {selectedRequest.sourceStationName}.
            </p>
            <form onSubmit={handleRejectSubmit}>
              <textarea
                value={modalNotes}
                onChange={(e) => setModalNotes(e.target.value)}
                placeholder="E.g. Address falls under another jurisdiction, insufficient evidence provided..."
                rows={4}
                required
                style={{
                  width: '100%',
                  backgroundColor: '#1e293b',
                  color: '#f8fafc',
                  border: '1px solid #475569',
                  borderRadius: '6px',
                  padding: '0.65rem',
                  fontSize: '0.85rem',
                  outline: 'none',
                  boxSizing: 'border-box',
                  marginBottom: '1rem'
                }}
              />
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  style={{
                    backgroundColor: '#334155',
                    color: '#cbd5e1',
                    border: 'none',
                    padding: '0.5rem 1rem',
                    borderRadius: '6px',
                    cursor: 'pointer'
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading || !modalNotes.trim()}
                  style={{
                    backgroundColor: '#ef4444',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.5rem 1.25rem',
                    borderRadius: '6px',
                    fontWeight: 600,
                    cursor: actionLoading || !modalNotes.trim() ? 'not-allowed' : 'pointer'
                  }}
                >
                  {actionLoading ? 'Rejecting...' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Respond Modal */}
      {modalType === 'respond' && selectedRequest && (
        <div className="modal-overlay" onClick={() => setModalType(null)}>
          <div className="modal-container" onClick={(e) => e.stopPropagation()} style={{
            maxWidth: '550px',
            backgroundColor: '#0f172a',
            color: '#f8fafc',
            border: '1px solid #334155',
            borderRadius: '10px',
            padding: '1.5rem'
          }}>
            <h3 style={{ margin: '0 0 0.5rem 0', color: '#60a5fa', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <MessageSquare size={20} />
              Submit Coordination Response
            </h3>
            <p style={{ fontSize: '0.85rem', color: '#94a3b8', margin: '0 0 1rem 0' }}>
              Enter investigation outcome / statement verification findings to send back to {selectedRequest.sourceStationName}.
            </p>
            <form onSubmit={handleRespondSubmit}>
              <textarea
                value={modalNotes}
                onChange={(e) => setModalNotes(e.target.value)}
                placeholder="E.g. Victim statement recorded at local station, verified residency, documents attached to case..."
                rows={4}
                required
                style={{
                  width: '100%',
                  backgroundColor: '#1e293b',
                  color: '#f8fafc',
                  border: '1px solid #475569',
                  borderRadius: '6px',
                  padding: '0.65rem',
                  fontSize: '0.85rem',
                  outline: 'none',
                  boxSizing: 'border-box',
                  marginBottom: '1rem'
                }}
              />
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                <button
                  type="button"
                  onClick={() => setModalType(null)}
                  style={{
                    backgroundColor: '#334155',
                    color: '#cbd5e1',
                    border: 'none',
                    padding: '0.5rem 1rem',
                    borderRadius: '6px',
                    cursor: 'pointer'
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading || !modalNotes.trim()}
                  style={{
                    backgroundColor: '#2563eb',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.5rem 1.25rem',
                    borderRadius: '6px',
                    fontWeight: 600,
                    cursor: actionLoading || !modalNotes.trim() ? 'not-allowed' : 'pointer'
                  }}
                >
                  {actionLoading ? 'Submitting...' : 'Submit & Complete'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
