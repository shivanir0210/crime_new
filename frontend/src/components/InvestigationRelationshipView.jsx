import React, { useState } from 'react';
import { 
  Briefcase, 
  FileBadge, 
  UserX, 
  Users, 
  Eye, 
  FolderLock, 
  MapPin, 
  ArrowRight, 
  Link2, 
  Filter, 
  Info,
  CheckCircle,
  AlertCircle
} from 'lucide-react';

export default function InvestigationRelationshipView({
  caseData,
  firData = null,
  victims = [],
  suspects = [],
  witnesses = [],
  evidenceList = [],
  onSelectEntity
}) {
  const [activeFilter, setActiveFilter] = useState('ALL');

  if (!caseData) return null;

  // Extract location from FIR or evidence
  const locationName = firData?.incidentLocation || 
    evidenceList.find(e => e.collectedLocation)?.collectedLocation || null;

  const totalLinked = (firData ? 1 : 0) + victims.length + suspects.length + witnesses.length + evidenceList.length + (locationName ? 1 : 0);

  if (totalLinked === 0) {
    return (
      <div className="relationship-empty-box">
        <Link2 size={36} className="empty-icon text-muted" />
        <div className="empty-title">NO RELATIONSHIPS ESTABLISHED</div>
        <div className="empty-subtitle">
          This case has no linked FIR, victims, suspects, witnesses, or evidence cataloged yet.
          Register case entities in their respective tabs to automatically construct the connected intelligence graph.
        </div>
      </div>
    );
  }

  // Filter items
  const showFir = (activeFilter === 'ALL' || activeFilter === 'INCIDENT') && firData;
  const showLocation = (activeFilter === 'ALL' || activeFilter === 'INCIDENT') && locationName;
  const showVictims = (activeFilter === 'ALL' || activeFilter === 'PEOPLE') && victims.length > 0;
  const showSuspects = (activeFilter === 'ALL' || activeFilter === 'PEOPLE') && suspects.length > 0;
  const showWitnesses = (activeFilter === 'ALL' || activeFilter === 'PEOPLE') && witnesses.length > 0;
  const showEvidence = (activeFilter === 'ALL' || activeFilter === 'EVIDENCE') && evidenceList.length > 0;

  return (
    <div className="relationship-view-wrapper">
      <div className="rel-header-bar">
        <div className="rel-title-group">
          <Link2 size={18} className="text-crimson" />
          <span className="rel-title">INVESTIGATION RELATIONSHIP NETWORK</span>
          <span className="rel-count-tag">{totalLinked} NODES LINKED</span>
        </div>

        {/* Filter Pills */}
        <div className="rel-filter-pills">
          <button 
            className={`rel-filter-btn ${activeFilter === 'ALL' ? 'active' : ''}`}
            onClick={() => setActiveFilter('ALL')}
          >
            ALL ENTITIES ({totalLinked})
          </button>
          <button 
            className={`rel-filter-btn ${activeFilter === 'PEOPLE' ? 'active' : ''}`}
            onClick={() => setActiveFilter('PEOPLE')}
          >
            PEOPLE ({victims.length + suspects.length + witnesses.length})
          </button>
          <button 
            className={`rel-filter-btn ${activeFilter === 'EVIDENCE' ? 'active' : ''}`}
            onClick={() => setActiveFilter('EVIDENCE')}
          >
            EVIDENCE ({evidenceList.length})
          </button>
          <button 
            className={`rel-filter-btn ${activeFilter === 'INCIDENT' ? 'active' : ''}`}
            onClick={() => setActiveFilter('INCIDENT')}
          >
            INCIDENT / SCENE ({firData ? 1 : 0 + (locationName ? 1 : 0)})
          </button>
        </div>
      </div>

      {/* Network Graph Container */}
      <div className="rel-graph-board">
        {/* Central Core Case Node */}
        <div className="central-core-node">
          <div className="core-node-inner">
            <div className="core-icon-wrap">
              <Briefcase size={22} className="text-crimson" />
            </div>
            <div className="core-info">
              <div className="core-label">PRIMARY CASE DOSSIER</div>
              <div className="core-number">{caseData.caseNumber}</div>
              <div className="core-title">{caseData.title}</div>
              <div className="core-badge-row">
                <span className="core-badge badge-priority">{caseData.priority}</span>
                <span className="core-badge badge-type">{(caseData.caseType || 'OTHER').replace(/_/g, ' ')}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Connected Branches Grid */}
        <div className="rel-branches-grid">
          {/* Branch 1: FIR Record */}
          {showFir && (
            <div className="rel-group-card group-fir">
              <div className="group-card-header">
                <FileBadge size={15} className="text-amber" />
                <span>FIRST INFORMATION REPORT</span>
                <span className="rel-link-tag">PRIMARY COMPLAINT</span>
              </div>
              <div className="group-card-body">
                <div className="entity-item">
                  <div className="entity-name">{firData.firNumber}</div>
                  <div className="entity-detail">
                    Sections: {firData.sectionsOfLaw || 'IPC General'}
                  </div>
                  {firData.complainantName && (
                    <div className="entity-sub">Complainant: {firData.complainantName}</div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Branch 2: Incident Location / Scene */}
          {showLocation && (
            <div className="rel-group-card group-location">
              <div className="group-card-header">
                <MapPin size={15} className="text-emerald" />
                <span>INCIDENT SCENE & GIS</span>
                <span className="rel-link-tag">LOCATION</span>
              </div>
              <div className="group-card-body">
                <div className="entity-item">
                  <div className="entity-name">{locationName}</div>
                  <div className="entity-detail">Primary crime scene perimeter & GIS coordinates</div>
                  <button 
                    onClick={() => window.location.href = `/location-intelligence/${caseData.id}`}
                    style={{
                      marginTop: '8px',
                      background: 'rgba(6, 182, 212, 0.12)',
                      border: '1px solid rgba(6, 182, 212, 0.4)',
                      color: '#06B6D4',
                      padding: '4px 8px',
                      borderRadius: '3px',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '4px'
                    }}
                  >
                    <MapPin size={12} />
                    Open Location Intelligence ↗
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Branch 3: Victims */}
          {showVictims && (
            <div className="rel-group-card group-victims">
              <div className="group-card-header">
                <UserX size={15} className="text-purple" />
                <span>VICTIMS ({victims.length})</span>
                <span className="rel-link-tag">AFFECTED PARTIES</span>
              </div>
              <div className="group-card-body">
                {victims.map((v, i) => (
                  <div key={v.id || i} className="entity-item">
                    <div className="entity-name-row">
                      <span className="entity-name">{v.fullName}</span>
                      <span className={`status-pill pill-${(v.status || 'ACTIVE').toLowerCase()}`}>
                        {v.status}
                      </span>
                    </div>
                    <div className="entity-detail">
                      {v.age ? `${v.age} yrs • ` : ''}{v.gender || 'Unknown'} • {v.relationshipToCase || 'Victim'}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Branch 4: Suspects */}
          {showSuspects && (
            <div className="rel-group-card group-suspects">
              <div className="group-card-header">
                <Users size={15} className="text-crimson" />
                <span>SUSPECTS ({suspects.length})</span>
                <span className="rel-link-tag">PERSONS OF INTEREST</span>
              </div>
              <div className="group-card-body">
                {suspects.map((s, i) => (
                  <div key={s.id || i} className="entity-item">
                    <div className="entity-name-row">
                      <span className="entity-name">{s.fullName}</span>
                      {s.alias && <span className="alias-tag">"{s.alias}"</span>}
                      <span className="status-pill pill-warn">
                        {(s.investigationStatus || 'UNDER_INVESTIGATION').replace(/_/g, ' ')}
                      </span>
                    </div>
                    {s.occupation && (
                      <div className="entity-detail">Occupation: {s.occupation}</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Branch 5: Witnesses */}
          {showWitnesses && (
            <div className="rel-group-card group-witnesses">
              <div className="group-card-header">
                <Eye size={15} className="text-cyan" />
                <span>WITNESSES ({witnesses.length})</span>
                <span className="rel-link-tag">TESTIMONIES</span>
              </div>
              <div className="group-card-body">
                {witnesses.map((w, i) => (
                  <div key={w.id || i} className="entity-item">
                    <div className="entity-name-row">
                      <span className="entity-name">{w.fullName}</span>
                      <span className="witness-type-pill">{(w.witnessType || 'EYEWITNESS').replace(/_/g, ' ')}</span>
                      <span className="credibility-pill">{w.reliabilityScore || 'MEDIUM'}</span>
                    </div>
                    {w.statementSummary && (
                      <div className="entity-statement">"{w.statementSummary.substring(0, 90)}..."</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Branch 6: Physical & Forensic Evidence */}
          {showEvidence && (
            <div className="rel-group-card group-evidence">
              <div className="group-card-header">
                <FolderLock size={15} className="text-blue" />
                <span>EVIDENCE ARTIFACTS ({evidenceList.length})</span>
                <span className="rel-link-tag">FORENSIC CHAIN</span>
              </div>
              <div className="group-card-body">
                {evidenceList.map((e, i) => (
                  <div key={e.id || i} className="entity-item">
                    <div className="entity-name-row">
                      <span className="entity-name">{e.title}</span>
                      <span className="evidence-type-pill">{e.evidenceType}</span>
                      <span className={`status-pill ${e.status === 'VERIFIED' ? 'pill-success' : 'pill-info'}`}>
                        {e.status}
                      </span>
                    </div>
                    {e.collectedLocation && (
                      <div className="entity-detail">Scene: {e.collectedLocation}</div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      <style>{`
        .relationship-view-wrapper {
          background: #141720;
          border: 1px solid #232733;
          border-radius: 6px;
          overflow: hidden;
          margin-top: 20px;
        }
        .rel-header-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 14px 18px;
          background: #181c26;
          border-bottom: 1px solid #232733;
          flex-wrap: wrap;
          gap: 12px;
        }
        .rel-title-group {
          display: flex;
          align-items: center;
          gap: 8px;
        }
        .rel-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.8rem;
          font-weight: 700;
          color: #f1f3f9;
          letter-spacing: 0.08em;
        }
        .rel-count-tag {
          font-family: var(--font-mono, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          background: rgba(230, 57, 70, 0.15);
          color: #e63946;
          border: 1px solid rgba(230, 57, 70, 0.3);
          padding: 2px 6px;
          border-radius: 3px;
        }
        .rel-filter-pills {
          display: flex;
          gap: 6px;
          flex-wrap: wrap;
        }
        .rel-filter-btn {
          background: #1e2432;
          border: 1px solid #2d364a;
          color: #8c93a8;
          padding: 4px 10px;
          border-radius: 4px;
          font-size: 0.7rem;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.15s;
        }
        .rel-filter-btn:hover {
          color: #fff;
          background: #283042;
        }
        .rel-filter-btn.active {
          background: rgba(193, 18, 31, 0.15);
          border-color: #c1121f;
          color: #fff;
        }

        .rel-graph-board {
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 24px;
        }

        .central-core-node {
          background: linear-gradient(135deg, #181c28 0%, #1a1520 100%);
          border: 1.5px solid rgba(193, 18, 31, 0.5);
          border-radius: 8px;
          padding: 16px 20px;
          box-shadow: 0 4px 20px rgba(193, 18, 31, 0.15);
          max-width: 600px;
          margin: 0 auto;
          width: 100%;
        }
        .core-node-inner {
          display: flex;
          align-items: center;
          gap: 16px;
        }
        .core-icon-wrap {
          background: rgba(193, 18, 31, 0.15);
          border: 1px solid rgba(193, 18, 31, 0.3);
          border-radius: 6px;
          padding: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .core-info {
          flex-grow: 1;
        }
        .core-label {
          font-family: var(--font-subheading, monospace);
          font-size: 0.65rem;
          font-weight: 700;
          color: #e63946;
          letter-spacing: 0.1em;
        }
        .core-number {
          font-family: var(--font-mono, monospace);
          font-size: 1.1rem;
          font-weight: 800;
          color: #fff;
        }
        .core-title {
          font-size: 0.85rem;
          color: #cbd5e1;
          margin-bottom: 6px;
        }
        .core-badge-row {
          display: flex;
          gap: 6px;
        }
        .core-badge {
          font-size: 0.65rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
        }
        .badge-priority {
          background: rgba(230, 57, 70, 0.2);
          color: #e63946;
          border: 1px solid rgba(230, 57, 70, 0.4);
        }
        .badge-type {
          background: rgba(100, 116, 139, 0.2);
          color: #cbd5e1;
          border: 1px solid rgba(100, 116, 139, 0.4);
        }

        .rel-branches-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
          gap: 16px;
        }

        .rel-group-card {
          background: #181c26;
          border: 1px solid #232733;
          border-radius: 6px;
          overflow: hidden;
          display: flex;
          flex-direction: column;
        }
        .group-fir { border-top: 3px solid #f59e0b; }
        .group-location { border-top: 3px solid #10b981; }
        .group-victims { border-top: 3px solid #a855f7; }
        .group-suspects { border-top: 3px solid #e63946; }
        .group-witnesses { border-top: 3px solid #06b6d4; }
        .group-evidence { border-top: 3px solid #3b82f6; }

        .group-card-header {
          padding: 10px 14px;
          background: #1c212e;
          border-bottom: 1px solid #232733;
          display: flex;
          align-items: center;
          gap: 8px;
          font-family: var(--font-subheading, monospace);
          font-size: 0.7rem;
          font-weight: 700;
          color: #f1f3f9;
          letter-spacing: 0.05em;
        }
        .rel-link-tag {
          margin-left: auto;
          font-size: 0.6rem;
          color: #8c93a8;
          font-weight: 600;
        }
        .group-card-body {
          padding: 12px;
          display: flex;
          flex-direction: column;
          gap: 10px;
          max-height: 260px;
          overflow-y: auto;
        }
        .entity-item {
          padding: 8px 10px;
          background: #141720;
          border: 1px solid #232733;
          border-radius: 4px;
          display: flex;
          flex-direction: column;
          gap: 3px;
        }
        .entity-name-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 6px;
          flex-wrap: wrap;
        }
        .entity-name {
          font-size: 0.8rem;
          font-weight: 600;
          color: #f1f3f9;
        }
        .alias-tag {
          font-size: 0.7rem;
          color: #eab308;
          font-style: italic;
        }
        .entity-detail {
          font-size: 0.72rem;
          color: #8c93a8;
        }
        .entity-sub {
          font-size: 0.68rem;
          color: #64748b;
        }
        .entity-statement {
          font-size: 0.7rem;
          color: #cbd5e1;
          font-style: italic;
          margin-top: 2px;
        }

        .status-pill {
          font-size: 0.62rem;
          font-weight: 700;
          padding: 1px 5px;
          border-radius: 2px;
        }
        .pill-deceased { background: rgba(230, 57, 70, 0.2); color: #e63946; }
        .pill-active { background: rgba(59, 130, 246, 0.2); color: #60a5fa; }
        .pill-warn { background: rgba(249, 115, 22, 0.2); color: #fb923c; }
        .pill-success { background: rgba(16, 185, 129, 0.2); color: #34d399; }
        .pill-info { background: rgba(59, 130, 246, 0.2); color: #60a5fa; }
        
        .witness-type-pill {
          font-size: 0.62rem;
          font-weight: 600;
          background: rgba(6, 182, 212, 0.15);
          color: #22d3ee;
          padding: 1px 5px;
          border-radius: 2px;
        }
        .credibility-pill {
          font-size: 0.6rem;
          font-weight: 700;
          background: rgba(16, 185, 129, 0.15);
          color: #10b981;
          padding: 1px 4px;
          border-radius: 2px;
        }
        .evidence-type-pill {
          font-size: 0.62rem;
          color: #94a3b8;
          background: #1e2330;
          padding: 1px 5px;
          border-radius: 2px;
        }

        .relationship-empty-box {
          background: #141720;
          border: 1px dashed #283042;
          border-radius: 6px;
          padding: 40px 20px;
          display: flex;
          flex-direction: column;
          align-items: center;
          text-align: center;
          gap: 12px;
          margin-top: 20px;
        }
        .empty-title {
          font-family: var(--font-subheading, monospace);
          font-size: 0.85rem;
          font-weight: 700;
          color: #e2e8f0;
          letter-spacing: 0.08em;
        }
        .empty-subtitle {
          font-size: 0.78rem;
          color: #8c93a8;
          max-width: 520px;
          line-height: 1.4;
        }
      `}</style>
    </div>
  );
}
