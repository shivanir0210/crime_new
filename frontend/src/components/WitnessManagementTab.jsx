import React, { useState } from 'react';
import { 
  Eye, 
  Plus, 
  Edit3, 
  Trash2, 
  Phone, 
  MapPin, 
  Briefcase, 
  Calendar, 
  CheckCircle, 
  X, 
  Loader2,
  ShieldCheck,
  UserCheck
} from 'lucide-react';
import { createCaseWitness, updateCaseWitness, deleteCaseWitness } from '../services/api';

export default function WitnessManagementTab({
  caseId,
  witnesses = [],
  loading = false,
  onWitnessesUpdated
}) {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingWitnessId, setEditingWitnessId] = useState(null);

  // Form states
  const [wName, setWName] = useState('');
  const [wAge, setWAge] = useState('');
  const [wGender, setWGender] = useState('MALE');
  const [wPhone, setWPhone] = useState('');
  const [wAddress, setWAddress] = useState('');
  const [wOccupation, setWOccupation] = useState('');
  const [wType, setWType] = useState('EYEWITNESS');
  const [wReliability, setWReliability] = useState('HIGH');
  const [wStatement, setWStatement] = useState('');
  const [wDate, setWDate] = useState('');

  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  const handleOpenAdd = () => {
    setEditingWitnessId(null);
    setWName('');
    setWAge('');
    setWGender('MALE');
    setWPhone('');
    setWAddress('');
    setWOccupation('');
    setWType('EYEWITNESS');
    setWReliability('HIGH');
    setWStatement('');
    setWDate(new Date().toISOString().substring(0, 16));
    setFormError('');
    setIsModalOpen(true);
  };

  const handleOpenEdit = (w) => {
    setEditingWitnessId(w.id);
    setWName(w.fullName || '');
    setWAge(w.age || '');
    setWGender(w.gender || 'MALE');
    setWPhone(w.phone || '');
    setWAddress(w.address || '');
    setWOccupation(w.occupation || '');
    setWType(w.witnessType || 'EYEWITNESS');
    setWReliability(w.reliabilityScore || 'HIGH');
    setWStatement(w.statementSummary || '');
    setWDate(w.interviewDate ? new Date(w.interviewDate).toISOString().substring(0, 16) : '');
    setFormError('');
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!wName.trim()) {
      setFormError('Witness full name is required');
      return;
    }
    if (!wStatement.trim()) {
      setFormError('Witness statement summary is required');
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      const payload = {
        fullName: wName.trim(),
        age: wAge ? parseInt(wAge, 10) : null,
        gender: wGender,
        phone: wPhone.trim() || null,
        address: wAddress.trim() || null,
        occupation: wOccupation.trim() || null,
        witnessType: wType,
        reliabilityScore: wReliability,
        statementSummary: wStatement.trim(),
        interviewDate: wDate ? new Date(wDate).toISOString() : new Date().toISOString()
      };

      if (editingWitnessId) {
        await updateCaseWitness(caseId, editingWitnessId, payload);
      } else {
        await createCaseWitness(caseId, payload);
      }

      setIsModalOpen(false);
      if (onWitnessesUpdated) {
        onWitnessesUpdated();
      }
    } catch (err) {
      setFormError(err.message || 'Failed to save witness statement');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (witnessId) => {
    if (!window.confirm('Are you sure you want to delete this witness statement record?')) return;
    try {
      await deleteCaseWitness(caseId, witnessId);
      if (onWitnessesUpdated) {
        onWitnessesUpdated();
      }
    } catch (err) {
      alert(err.message || 'Failed to delete witness');
    }
  };

  return (
    <div className="witnesses-tab-container">
      {/* Header bar */}
      <div className="tab-header-bar">
        <div className="title-group">
          <Eye size={18} className="text-cyan" />
          <span className="tab-title">CASE WITNESSES & FORMAL TESTIMONIES</span>
          <span className="count-tag">{witnesses.length} RECORDED</span>
        </div>

        <button className="add-witness-btn" onClick={handleOpenAdd}>
          <Plus size={14} />
          <span>RECORD WITNESS STATEMENT</span>
        </button>
      </div>

      {/* Witnesses Grid / List */}
      <div className="tab-content-area">
        {loading ? (
          <div className="loading-state">
            <Loader2 size={24} className="spin-icon text-cyan" />
            <span>Retrieving recorded witness dossiers...</span>
          </div>
        ) : witnesses.length === 0 ? (
          <div className="empty-state">
            <Eye size={36} className="text-muted" />
            <div className="empty-title">NO WITNESS STATEMENTS RECORDED</div>
            <div className="empty-desc">
              Record eyewitness accounts, character testimonies, or expert statements. All statements are preserved in the case timeline and connected in the relationship intelligence graph.
            </div>
            <button className="add-witness-btn mt-3" onClick={handleOpenAdd}>
              <Plus size={14} />
              <span>RECORD FIRST WITNESS</span>
            </button>
          </div>
        ) : (
          <div className="witnesses-grid">
            {witnesses.map((w) => (
              <div key={w.id} className="witness-card">
                <div className="card-header">
                  <div className="name-col">
                    <span className="witness-name">{w.fullName}</span>
                    <div className="pills-row">
                      <span className="type-badge">{(w.witnessType || 'EYEWITNESS').replace(/_/g, ' ')}</span>
                      <span className={`reliability-badge rel-${(w.reliabilityScore || 'MEDIUM').toLowerCase()}`}>
                        {w.reliabilityScore || 'MEDIUM'} RELIABILITY
                      </span>
                    </div>
                  </div>
                  <div className="card-actions">
                    <button className="icon-action-btn" onClick={() => handleOpenEdit(w)} title="Edit Statement">
                      <Edit3 size={14} />
                    </button>
                    <button className="icon-action-btn text-danger" onClick={() => handleDelete(w.id)} title="Delete Witness">
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>

                <div className="card-body">
                  {/* Meta Details Strip */}
                  <div className="meta-strip">
                    {w.occupation && (
                      <span className="meta-detail">
                        <Briefcase size={12} /> {w.occupation}
                      </span>
                    )}
                    {w.phone && (
                      <span className="meta-detail">
                        <Phone size={12} /> {w.phone}
                      </span>
                    )}
                    {w.address && (
                      <span className="meta-detail">
                        <MapPin size={12} /> {w.address}
                      </span>
                    )}
                    {w.interviewDate && (
                      <span className="meta-detail">
                        <Calendar size={12} /> Interviewed: {new Date(w.interviewDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                      </span>
                    )}
                  </div>

                  {/* Statement Box */}
                  <div className="statement-box">
                    <div className="statement-label">STATEMENT TESTIMONY:</div>
                    <p className="statement-text">"{w.statementSummary}"</p>
                  </div>
                </div>

                {w.createdBy && (
                  <div className="card-footer">
                    <UserCheck size={12} className="text-muted" />
                    <span>Recorded by: {w.createdBy.fullName || w.createdBy.policeId}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add / Edit Witness Modal */}
      {isModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title-wrap">
                <Eye size={18} className="text-cyan" />
                <span className="modal-title">
                  {editingWitnessId ? 'EDIT WITNESS DOSSIER' : 'RECORD NEW WITNESS STATEMENT'}
                </span>
              </div>
              <button className="close-btn" onClick={() => setIsModalOpen(false)}>
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSave} className="modal-form">
              {formError && (
                <div className="form-error-banner">{formError}</div>
              )}

              <div className="form-grid-2">
                <div className="form-group">
                  <label>FULL NAME *</label>
                  <input
                    type="text"
                    value={wName}
                    onChange={(e) => setWName(e.target.value)}
                    placeholder="e.g. R. Natarajan"
                    required
                  />
                </div>

                <div className="form-group">
                  <label>OCCUPATION</label>
                  <input
                    type="text"
                    value={wOccupation}
                    onChange={(e) => setWOccupation(e.target.value)}
                    placeholder="e.g. Store Manager / Night Watchman"
                  />
                </div>

                <div className="form-group">
                  <label>PHONE CONTACT</label>
                  <input
                    type="text"
                    value={wPhone}
                    onChange={(e) => setWPhone(e.target.value)}
                    placeholder="e.g. 9842109876"
                  />
                </div>

                <div className="form-group">
                  <label>AGE & GENDER</label>
                  <div className="age-gender-row">
                    <input
                      type="number"
                      value={wAge}
                      onChange={(e) => setWAge(e.target.value)}
                      placeholder="Age"
                      style={{ width: '80px' }}
                    />
                    <select
                      value={wGender}
                      onChange={(e) => setWGender(e.target.value)}
                    >
                      <option value="MALE">MALE</option>
                      <option value="FEMALE">FEMALE</option>
                      <option value="OTHER">OTHER</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label>WITNESS CLASSIFICATION</label>
                  <select
                    value={wType}
                    onChange={(e) => setWType(e.target.value)}
                  >
                    <option value="EYEWITNESS">EYEWITNESS (Direct Observation)</option>
                    <option value="MATERIAL_WITNESS">MATERIAL WITNESS (Substantive Facts)</option>
                    <option value="CHARACTER_WITNESS">CHARACTER WITNESS</option>
                    <option value="EXPERT_WITNESS">EXPERT WITNESS (Technical/Medical)</option>
                    <option value="INFORMANT">CONFIDENTIAL INFORMANT</option>
                    <option value="OTHER">OTHER WITNESS</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>RELIABILITY RATING</label>
                  <select
                    value={wReliability}
                    onChange={(e) => setWReliability(e.target.value)}
                  >
                    <option value="HIGH">HIGH (Verified & Corroborated)</option>
                    <option value="MEDIUM">MEDIUM (Plausible, Pending Checks)</option>
                    <option value="LOW">LOW (Inconsistent Details)</option>
                    <option value="UNVERIFIED">UNVERIFIED (Preliminary)</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label>RESIDENTIAL / WORK ADDRESS</label>
                <input
                  type="text"
                  value={wAddress}
                  onChange={(e) => setWAddress(e.target.value)}
                  placeholder="Address or general vicinity"
                />
              </div>

              <div className="form-group">
                <label>STATEMENT SUMMARY / TESTIMONY *</label>
                <textarea
                  value={wStatement}
                  onChange={(e) => setWStatement(e.target.value)}
                  rows={4}
                  placeholder="Record factual observations, statements made, identification of suspects or timeline of events observed..."
                  required
                />
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="cancel-btn"
                  onClick={() => setIsModalOpen(false)}
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  className="save-btn"
                  disabled={saving}
                >
                  {saving ? 'RECORDING...' : editingWitnessId ? 'UPDATE RECORD' : 'SAVE WITNESS RECORD'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <style>{`
        .witnesses-tab-container {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 6px;
          overflow: hidden;
        }
        .tab-header-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 14px 18px;
          background: #181c26;
          border-bottom: 1px solid #232733;
          flex-wrap: wrap;
          gap: 12px;
        }
        .title-group {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .tab-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.8rem;
          font-weight: 700;
          color: #f1f3f9;
          letter-spacing: 0.08em;
        }
        .count-tag {
          font-family: var(--font-mono, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          background: rgba(6, 182, 212, 0.15);
          color: #06b6d4;
          border: 1px solid rgba(6, 182, 212, 0.3);
          padding: 2px 6px;
          border-radius: 3px;
        }
        .add-witness-btn {
          display: flex;
          align-items: center;
          gap: 6px;
          background: #c1121f;
          color: #fff;
          border: none;
          padding: 6px 12px;
          border-radius: 4px;
          font-size: 0.72rem;
          font-weight: 700;
          cursor: pointer;
          transition: all 0.15s;
        }
        .add-witness-btn:hover {
          background: #e63946;
        }
        .tab-content-area {
          padding: 20px;
        }
        .loading-state, .empty-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: 48px 20px;
          text-align: center;
          gap: 12px;
          color: #8c93a8;
        }
        .empty-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.85rem;
          font-weight: 700;
          color: #e2e8f0;
          letter-spacing: 0.08em;
        }
        .empty-desc {
          font-size: 0.78rem;
          color: #64748b;
          max-width: 460px;
          line-height: 1.4;
        }
        .witnesses-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
          gap: 16px;
        }
        .witness-card {
          background: #181c26;
          border: 1px solid #232733;
          border-radius: 6px;
          overflow: hidden;
          display: flex;
          flex-direction: column;
          border-left: 3px solid #06b6d4;
        }
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          padding: 12px 14px;
          background: #1c212e;
          border-bottom: 1px solid #232733;
        }
        .name-col {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .witness-name {
          font-size: 0.92rem;
          font-weight: 700;
          color: #f1f3f9;
        }
        .pills-row {
          display: flex;
          align-items: center;
          gap: 6px;
          flex-wrap: wrap;
        }
        .type-badge {
          font-family: var(--font-mono, monospace);
          font-size: 0.62rem;
          font-weight: 700;
          background: rgba(6, 182, 212, 0.15);
          color: #22d3ee;
          padding: 2px 6px;
          border-radius: 3px;
        }
        .reliability-badge {
          font-family: var(--font-mono, monospace);
          font-size: 0.6rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
        }
        .rel-high { background: rgba(16, 185, 129, 0.15); color: #10b981; }
        .rel-medium { background: rgba(234, 179, 8, 0.15); color: #eab308; }
        .rel-low { background: rgba(239, 68, 68, 0.15); color: #ef4444; }
        .card-actions {
          display: flex;
          gap: 4px;
        }
        .icon-action-btn {
          background: transparent;
          border: none;
          color: #8c93a8;
          padding: 4px;
          border-radius: 3px;
          cursor: pointer;
          transition: all 0.15s;
        }
        .icon-action-btn:hover {
          color: #fff;
          background: #252a38;
        }
        .text-danger:hover {
          color: #e63946;
        }
        .card-body {
          padding: 12px 14px;
          display: flex;
          flex-direction: column;
          gap: 10px;
          flex-grow: 1;
        }
        .meta-strip {
          display: flex;
          flex-wrap: wrap;
          gap: 12px;
          font-size: 0.72rem;
          color: #8c93a8;
        }
        .meta-detail {
          display: flex;
          align-items: center;
          gap: 4px;
        }
        .statement-box {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 4px;
          padding: 10px 12px;
        }
        .statement-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.62rem;
          font-weight: 700;
          color: #64748b;
          letter-spacing: 0.05em;
          margin-bottom: 4px;
        }
        .statement-text {
          font-size: 0.78rem;
          color: #cbd5e1;
          font-style: italic;
          margin: 0;
          line-height: 1.4;
        }
        .card-footer {
          padding: 8px 14px;
          background: #141720;
          border-top: 1px solid #232733;
          font-size: 0.68rem;
          color: #64748b;
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .modal-backdrop {
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
        .modal-content {
          background: #121418;
          border: 1px solid #2a2e39;
          border-radius: 8px;
          width: 100%;
          max-width: 600px;
          max-height: 90vh;
          overflow-y: auto;
          box-shadow: 0 12px 36px rgba(0, 0, 0, 0.85);
        }
        .modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 14px 18px;
          background: #161920;
          border-bottom: 1px solid #232733;
        }
        .modal-title-wrap {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .modal-title {
          font-family: var(--font-heading, monospace);
          font-size: 0.85rem;
          font-weight: 700;
          color: #f1f3f9;
        }
        .close-btn {
          background: transparent;
          border: none;
          color: #8c93a8;
          cursor: pointer;
        }
        .modal-form {
          padding: 18px;
          display: flex;
          flex-direction: column;
          gap: 14px;
        }
        .form-grid-2 {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
        }
        @media (max-width: 540px) {
          .form-grid-2 { grid-template-columns: 1fr; }
        }
        .form-group {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .form-group label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.68rem;
          font-weight: 700;
          color: #8c93a8;
          letter-spacing: 0.05em;
        }
        .form-group input, .form-group select, .form-group textarea {
          background: #181c26;
          border: 1px solid #283042;
          border-radius: 4px;
          padding: 8px 10px;
          color: #f1f3f9;
          font-size: 0.8rem;
        }
        .form-group input:focus, .form-group select:focus, .form-group textarea:focus {
          outline: none;
          border-color: #06b6d4;
        }
        .age-gender-row {
          display: flex;
          gap: 8px;
        }
        .age-gender-row select {
          flex-grow: 1;
        }
        .modal-actions {
          display: flex;
          justify-content: flex-end;
          gap: 10px;
          margin-top: 8px;
        }
        .cancel-btn {
          background: #252a38;
          color: #cbd5e1;
          border: 1px solid #333a4d;
          padding: 7px 16px;
          border-radius: 4px;
          font-size: 0.75rem;
          font-weight: 600;
          cursor: pointer;
        }
        .save-btn {
          background: #0891b2;
          color: #fff;
          border: none;
          padding: 7px 18px;
          border-radius: 4px;
          font-size: 0.75rem;
          font-weight: 700;
          cursor: pointer;
        }
        .save-btn:hover {
          background: #06b6d4;
        }
        .form-error-banner {
          background: rgba(230, 57, 70, 0.15);
          border: 1px solid rgba(230, 57, 70, 0.4);
          color: #e63946;
          padding: 8px 12px;
          border-radius: 4px;
          font-size: 0.75rem;
        }
      `}</style>
    </div>
  );
}
