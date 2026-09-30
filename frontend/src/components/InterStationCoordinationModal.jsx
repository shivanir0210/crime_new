import React, { useState, useEffect, useMemo } from 'react';
import { 
  X, Send, Shield, Building2, MapPin, Mail, UserCheck, 
  CheckSquare, Square, AlertTriangle, CheckCircle2, FileText, 
  Users, AlertCircle, Eye, ShieldAlert, Sparkles 
} from 'lucide-react';
import { getPoliceStations, createInterStationRequest } from '../services/api';

export default function InterStationCoordinationModal({ 
  isOpen, 
  onClose, 
  caseData, 
  victim, 
  onSuccess 
}) {
  if (!isOpen) return null;

  const [stations, setStations] = useState([]);
  const [loadingStations, setLoadingStations] = useState(true);
  const [selectedState, setSelectedState] = useState('');
  const [selectedDistrict, setSelectedDistrict] = useState('');
  const [selectedStationId, setSelectedStationId] = useState('');
  const [reason, setReason] = useState('');
  
  // Shared data checkboxes
  const [shareCaseReference, setShareCaseReference] = useState(true);
  const [shareVictimDetails, setShareVictimDetails] = useState(true);
  const [shareIncidentSummary, setShareIncidentSummary] = useState(true);
  const [shareEvidenceSummary, setShareEvidenceSummary] = useState(false);
  const [shareWitnessInfo, setShareWitnessInfo] = useState(false);

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [successResult, setSuccessResult] = useState(null);

  // Load available police stations on mount
  useEffect(() => {
    let mounted = true;
    async function loadStations() {
      try {
        setLoadingStations(true);
        const data = await getPoliceStations();
        if (mounted) {
          setStations(data || []);
        }
      } catch (err) {
        if (mounted) {
          setError(`Failed to load police stations: ${err.message}`);
        }
      } finally {
        if (mounted) setLoadingStations(false);
      }
    }
    loadStations();
    return () => { mounted = false; };
  }, []);

  // Compute distinct states
  const availableStates = useMemo(() => {
    const states = new Set();
    stations.forEach(s => {
      if (s.state) states.add(s.state);
    });
    return Array.from(states).sort();
  }, [stations]);

  // Compute distinct districts for selected state
  const availableDistricts = useMemo(() => {
    if (!selectedState) return [];
    const districts = new Set();
    stations.forEach(s => {
      if (s.state === selectedState && s.district) {
        districts.add(s.district);
      }
    });
    return Array.from(districts).sort();
  }, [stations, selectedState]);

  // Compute available stations for selected state & district
  const filteredStations = useMemo(() => {
    if (!selectedState || !selectedDistrict) return [];
    return stations.filter(s => s.state === selectedState && s.district === selectedDistrict);
  }, [stations, selectedState, selectedDistrict]);

  // Selected station object
  const selectedStation = useMemo(() => {
    if (!selectedStationId) return null;
    return stations.find(s => s.id === selectedStationId) || null;
  }, [stations, selectedStationId]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedStationId) {
      setError('Please select a destination police station.');
      return;
    }
    if (!reason.trim()) {
      setError('Please provide a reason / justification for this inter-station request.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        caseId: caseData?.id,
        victimId: victim?.id || null,
        destinationStationId: selectedStationId,
        reason: reason.trim(),
        shareCaseReference,
        shareVictimDetails,
        shareIncidentSummary,
        shareEvidenceSummary,
        shareWitnessInfo
      };

      const result = await createInterStationRequest(payload);
      setSuccessResult(result);
      if (onSuccess) {
        onSuccess(result);
      }
    } catch (err) {
      setError(err.message || 'Failed to dispatch inter-station request.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResetAndClose = () => {
    setSelectedState('');
    setSelectedDistrict('');
    setSelectedStationId('');
    setReason('');
    setError(null);
    setSuccessResult(null);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={handleResetAndClose}>
      <div 
        className="modal-container inter-station-modal" 
        onClick={(e) => e.stopPropagation()}
        style={{
          maxWidth: '720px',
          width: '95%',
          maxHeight: '90vh',
          overflowY: 'auto',
          backgroundColor: '#0f172a',
          color: '#e2e8f0',
          borderRadius: '12px',
          border: '1px solid #334155',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.75)'
        }}
      >
        {/* Modal Header */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '1.25rem 1.5rem',
          borderBottom: '1px solid #1e293b',
          backgroundColor: '#1e293b'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{
              backgroundColor: '#3b82f620',
              color: '#60a5fa',
              padding: '8px',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Shield size={22} />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 600, color: '#f8fafc' }}>
                Inter-Station Coordination Request
              </h2>
              <p style={{ margin: '2px 0 0 0', fontSize: '0.8rem', color: '#94a3b8' }}>
                Dispatch jurisdiction transfer / inquiry to another police station with Resend notification
              </p>
            </div>
          </div>
          <button 
            onClick={handleResetAndClose}
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              padding: '6px',
              borderRadius: '6px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Modal Body */}
        <div style={{ padding: '1.5rem' }}>
          {successResult ? (
            <div style={{ textAlign: 'center', padding: '1.5rem 0' }}>
              <div style={{
                width: '64px',
                height: '64px',
                borderRadius: '50%',
                backgroundColor: '#10b98120',
                color: '#34d399',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 1rem auto'
              }}>
                <CheckCircle2 size={36} />
              </div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, color: '#f1f5f9', margin: '0 0 0.5rem 0' }}>
                Coordination Request Dispatched!
              </h3>
              <p style={{ color: '#94a3b8', fontSize: '0.9rem', maxWidth: '480px', margin: '0 auto 1.25rem auto' }}>
                Your request has been registered and securely transmitted to{' '}
                <strong style={{ color: '#60a5fa' }}>{successResult.destinationStationName}</strong>.
              </p>

              <div style={{
                backgroundColor: '#1e293b',
                padding: '1rem',
                borderRadius: '8px',
                border: '1px solid #334155',
                textAlign: 'left',
                margin: '0 auto 1.5rem auto',
                maxWidth: '520px',
                fontSize: '0.85rem'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: '#94a3b8' }}>Request Status:</span>
                  <span style={{
                    color: successResult.status === 'SENT' ? '#34d399' : '#f59e0b',
                    fontWeight: 600
                  }}>
                    {successResult.status}
                  </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: '#94a3b8' }}>Station Head Email:</span>
                  <span style={{ color: '#f8fafc', fontFamily: 'monospace' }}>{successResult.destinationStationHeadEmail}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                  <span style={{ color: '#94a3b8' }}>Email Delivery:</span>
                  <span style={{
                    color: successResult.emailDeliveryStatus === 'SENT' ? '#34d399' : '#ef4444',
                    fontWeight: 600
                  }}>
                    {successResult.emailDeliveryStatus}
                  </span>
                </div>
                {successResult.providerMessageId && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#94a3b8' }}>Resend Message ID:</span>
                    <span style={{ color: '#94a3b8', fontFamily: 'monospace' }}>{successResult.providerMessageId}</span>
                  </div>
                )}
                {successResult.emailDeliveryError && (
                  <div style={{ marginTop: '0.5rem', color: '#f87171', fontSize: '0.8rem' }}>
                    <strong>Note:</strong> {successResult.emailDeliveryError}
                  </div>
                )}
              </div>

              <button
                onClick={handleResetAndClose}
                style={{
                  backgroundColor: '#3b82f6',
                  color: '#ffffff',
                  border: 'none',
                  padding: '0.65rem 1.5rem',
                  borderRadius: '6px',
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  cursor: 'pointer'
                }}
              >
                Close & Return
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              {error && (
                <div style={{
                  backgroundColor: '#7f1d1d30',
                  border: '1px solid #ef444450',
                  color: '#fca5a5',
                  padding: '0.75rem 1rem',
                  borderRadius: '8px',
                  marginBottom: '1.25rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  fontSize: '0.875rem'
                }}>
                  <AlertCircle size={18} style={{ flexShrink: 0 }} />
                  <span>{error}</span>
                </div>
              )}

              {/* Case & Victim Context Banner */}
              <div style={{
                backgroundColor: '#1e293b80',
                border: '1px solid #334155',
                borderRadius: '8px',
                padding: '0.85rem 1rem',
                marginBottom: '1.25rem',
                display: 'flex',
                flexWrap: 'wrap',
                gap: '1rem',
                justifyContent: 'space-between',
                fontSize: '0.85rem'
              }}>
                <div>
                  <span style={{ color: '#94a3b8' }}>Case Reference: </span>
                  <strong style={{ color: '#60a5fa' }}>{caseData?.caseNumber || 'N/A'}</strong>
                  <span style={{ color: '#cbd5e1', marginLeft: '6px' }}>— {caseData?.title}</span>
                </div>
                {victim && (
                  <div>
                    <span style={{ color: '#94a3b8' }}>Subject Victim: </span>
                    <strong style={{ color: '#f8fafc' }}>{victim.fullName}</strong>
                    {victim.address && (
                      <span style={{ color: '#94a3b8', marginLeft: '6px' }}>
                        ({victim.address})
                      </span>
                    )}
                  </div>
                )}
              </div>

              {/* Step 1: Destination Police Station Cascade */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label style={{ 
                  display: 'block', 
                  fontSize: '0.875rem', 
                  fontWeight: 600, 
                  color: '#f1f5f9',
                  marginBottom: '0.5rem'
                }}>
                  Select Destination Police Station
                </label>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1.2fr', gap: '0.75rem' }}>
                  {/* State Selection */}
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', color: '#94a3b8', marginBottom: '4px' }}>
                      State
                    </label>
                    <select
                      value={selectedState}
                      onChange={(e) => {
                        setSelectedState(e.target.value);
                        setSelectedDistrict('');
                        setSelectedStationId('');
                      }}
                      disabled={loadingStations}
                      style={{
                        width: '100%',
                        backgroundColor: '#1e293b',
                        color: '#f8fafc',
                        border: '1px solid #475569',
                        borderRadius: '6px',
                        padding: '0.55rem 0.75rem',
                        fontSize: '0.85rem',
                        outline: 'none'
                      }}
                    >
                      <option value="">-- Choose State --</option>
                      {availableStates.map(st => (
                        <option key={st} value={st}>{st}</option>
                      ))}
                    </select>
                  </div>

                  {/* District Selection */}
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', color: '#94a3b8', marginBottom: '4px' }}>
                      District
                    </label>
                    <select
                      value={selectedDistrict}
                      onChange={(e) => {
                        setSelectedDistrict(e.target.value);
                        setSelectedStationId('');
                      }}
                      disabled={!selectedState || availableDistricts.length === 0}
                      style={{
                        width: '100%',
                        backgroundColor: '#1e293b',
                        color: '#f8fafc',
                        border: '1px solid #475569',
                        borderRadius: '6px',
                        padding: '0.55rem 0.75rem',
                        fontSize: '0.85rem',
                        outline: 'none'
                      }}
                    >
                      <option value="">-- Choose District --</option>
                      {availableDistricts.map(dt => (
                        <option key={dt} value={dt}>{dt}</option>
                      ))}
                    </select>
                  </div>

                  {/* Station Selection */}
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', color: '#94a3b8', marginBottom: '4px' }}>
                      Police Station
                    </label>
                    <select
                      value={selectedStationId}
                      onChange={(e) => setSelectedStationId(e.target.value)}
                      disabled={!selectedDistrict || filteredStations.length === 0}
                      style={{
                        width: '100%',
                        backgroundColor: '#1e293b',
                        color: '#f8fafc',
                        border: '1px solid #475569',
                        borderRadius: '6px',
                        padding: '0.55rem 0.75rem',
                        fontSize: '0.85rem',
                        outline: 'none'
                      }}
                    >
                      <option value="">-- Choose Station --</option>
                      {filteredStations.map(st => (
                        <option key={st.id} value={st.id}>
                          {st.stationName} ({st.stationCode})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              {/* Station Head Preview Card (Automated, Dynamic) */}
              {selectedStation && (
                <div style={{
                  backgroundColor: '#1e293b',
                  border: '1px solid #3b82f640',
                  borderRadius: '8px',
                  padding: '1rem',
                  marginBottom: '1.25rem'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#60a5fa', fontWeight: 600, fontSize: '0.875rem' }}>
                      <Building2 size={16} />
                      <span>{selectedStation.stationName}</span>
                    </div>
                    <span style={{
                      backgroundColor: '#3b82f620',
                      color: '#93c5fd',
                      fontSize: '0.75rem',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      fontFamily: 'monospace'
                    }}>
                      {selectedStation.stationCode}
                    </span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.825rem' }}>
                    <div>
                      <span style={{ color: '#94a3b8', display: 'block' }}>Station Head:</span>
                      <strong style={{ color: '#f8fafc' }}>{selectedStation.headName || 'Officer-In-Charge'}</strong>
                    </div>
                    <div>
                      <span style={{ color: '#94a3b8', display: 'block' }}>Notification Target Email:</span>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#34d399', fontFamily: 'monospace', fontWeight: 600 }}>
                        <Mail size={14} />
                        <span>{selectedStation.headEmail || 'No email configured'}</span>
                      </div>
                    </div>
                  </div>

                  <div style={{
                    marginTop: '0.75rem',
                    paddingTop: '0.75rem',
                    borderTop: '1px solid #334155',
                    fontSize: '0.75rem',
                    color: '#94a3b8',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem'
                  }}>
                    <Sparkles size={14} style={{ color: '#60a5fa' }} />
                    <span>The notification email will be dispatched automatically to this station head via Resend API.</span>
                  </div>
                </div>
              )}

              {/* Reason / Jurisdiction Notes */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label style={{ 
                  display: 'block', 
                  fontSize: '0.875rem', 
                  fontWeight: 600, 
                  color: '#f1f5f9',
                  marginBottom: '0.4rem'
                }}>
                  Reason / Purpose for Transfer Request <span style={{ color: '#ef4444' }}>*</span>
                </label>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Explain why this case / victim falls under the destination station's jurisdiction (e.g. victim residence, incident location, cross-jurisdiction lead)..."
                  rows={3}
                  required
                  style={{
                    width: '100%',
                    backgroundColor: '#1e293b',
                    color: '#f8fafc',
                    border: '1px solid #475569',
                    borderRadius: '6px',
                    padding: '0.65rem 0.75rem',
                    fontSize: '0.85rem',
                    resize: 'vertical',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
              </div>

              {/* Shared Information Permissions */}
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ 
                  display: 'block', 
                  fontSize: '0.875rem', 
                  fontWeight: 600, 
                  color: '#f1f5f9',
                  marginBottom: '0.5rem'
                }}>
                  Information to Share (Selective Disclosure)
                </label>
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '0.5rem',
                  backgroundColor: '#1e293b80',
                  padding: '0.75rem',
                  borderRadius: '8px',
                  border: '1px solid #334155'
                }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: '#cbd5e1', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={shareCaseReference}
                      onChange={(e) => setShareCaseReference(e.target.checked)}
                      style={{ accentColor: '#3b82f6' }}
                    />
                    <span>Case Reference & Title</span>
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: '#cbd5e1', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={shareVictimDetails}
                      onChange={(e) => setShareVictimDetails(e.target.checked)}
                      style={{ accentColor: '#3b82f6' }}
                    />
                    <span>Victim Details & Address</span>
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: '#cbd5e1', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={shareIncidentSummary}
                      onChange={(e) => setShareIncidentSummary(e.target.checked)}
                      style={{ accentColor: '#3b82f6' }}
                    />
                    <span>Incident Description</span>
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: '#cbd5e1', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={shareEvidenceSummary}
                      onChange={(e) => setShareEvidenceSummary(e.target.checked)}
                      style={{ accentColor: '#3b82f6' }}
                    />
                    <span>Evidence Summary</span>
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: '#cbd5e1', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={shareWitnessInfo}
                      onChange={(e) => setShareWitnessInfo(e.target.checked)}
                      style={{ accentColor: '#3b82f6' }}
                    />
                    <span>Witness Information</span>
                  </label>
                </div>
              </div>

              {/* Modal Actions */}
              <div style={{
                display: 'flex',
                justifyContent: 'flex-end',
                gap: '0.75rem',
                borderTop: '1px solid #1e293b',
                paddingTop: '1rem'
              }}>
                <button
                  type="button"
                  onClick={handleResetAndClose}
                  disabled={submitting}
                  style={{
                    backgroundColor: '#334155',
                    color: '#cbd5e1',
                    border: 'none',
                    padding: '0.65rem 1.25rem',
                    borderRadius: '6px',
                    fontWeight: 500,
                    fontSize: '0.875rem',
                    cursor: 'pointer'
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting || !selectedStationId || !reason.trim()}
                  style={{
                    backgroundColor: submitting || !selectedStationId || !reason.trim() ? '#1e3a8a' : '#2563eb',
                    color: '#ffffff',
                    border: 'none',
                    padding: '0.65rem 1.5rem',
                    borderRadius: '6px',
                    fontWeight: 600,
                    fontSize: '0.875rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    cursor: submitting || !selectedStationId || !reason.trim() ? 'not-allowed' : 'pointer',
                    opacity: submitting || !selectedStationId || !reason.trim() ? 0.7 : 1
                  }}
                >
                  {submitting ? (
                    <>
                      <span className="spinner" style={{ display: 'inline-block', width: '14px', height: '14px', border: '2px solid #fff', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
                      <span>Transmitting...</span>
                    </>
                  ) : (
                    <>
                      <Send size={16} />
                      <span>Send Coordination Request</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
