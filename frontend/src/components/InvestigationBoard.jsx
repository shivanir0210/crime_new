import React, { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import Panel from './Panel';
import { 
  Network, 
  UserMinus, 
  UserX, 
  Video, 
  FileText, 
  Eye, 
  MapPin, 
  Cpu, 
  Smartphone, 
  FileCheck, 
  Shield, 
  Disc, 
  Radio, 
  Briefcase,
  AlertCircle,
  ExternalLink,
  Layers
} from 'lucide-react';

export default function InvestigationBoard({ 
  caseData, 
  fir, 
  victims = [], 
  suspects = [], 
  witnesses = [], 
  evidence = [], 
  loading = false,
  onSelectNode
}) {
  const navigate = useNavigate();

  // Dynamically extract nodes and connections from the selected case data
  const { nodes, connections } = useMemo(() => {
    if (!caseData || !caseData.id) {
      return { nodes: [], connections: [] };
    }

    const rawNodes = [];
    const rawLinks = [];

    // 1. Root Case Node
    const caseNodeId = 'node-case';
    rawNodes.push({
      id: caseNodeId,
      type: 'CASE',
      title: caseData.caseNumber || 'CASE DOSSIER',
      subtitle: caseData.title || 'Active Investigation',
      icon: Briefcase,
      color: '#38BDF8',
      tab: 'OVERVIEW',
      category: 'core'
    });

    // 2. FIR Node
    if (fir && (fir.firNumber || fir.incidentDetails)) {
      const firNodeId = 'node-fir';
      rawNodes.push({
        id: firNodeId,
        type: 'FIR',
        title: fir.firNumber || 'REGISTERED FIR',
        subtitle: fir.actsSections || 'IPC Sections',
        icon: FileCheck,
        color: '#60A5FA',
        tab: 'FIR',
        category: 'core'
      });
      rawLinks.push({ from: caseNodeId, to: firNodeId, stroke: 'rgba(56, 189, 248, 0.6)', width: 2 });
    }

    // 3. Location Node (from FIR or Case)
    const locationName = fir?.incidentLocation || caseData?.location || (caseData?.title?.includes('at') ? caseData.title.split('at')[1].trim() : null);
    if (locationName) {
      const locNodeId = 'node-location';
      rawNodes.push({
        id: locNodeId,
        type: 'LOCATION',
        title: locationName.length > 24 ? locationName.slice(0, 22) + '...' : locationName,
        subtitle: 'Crime Scene / GIS Map',
        icon: MapPin,
        color: '#38BDF8',
        customPath: `/location-intelligence/${caseData.id}`,
        tab: 'FIR',
        category: 'environment'
      });
      rawLinks.push({ from: caseNodeId, to: locNodeId, stroke: 'rgba(56, 189, 248, 0.4)', width: 1.5, dashed: true });
    }

    // 4. Officer Node
    if (caseData.createdBy && caseData.createdBy.fullName) {
      const officerNodeId = 'node-officer';
      rawNodes.push({
        id: officerNodeId,
        type: 'OFFICER',
        title: caseData.createdBy.fullName,
        subtitle: `Badge: ${caseData.createdBy.policeId || 'Lead'}`,
        icon: Shield,
        color: '#10B981',
        tab: 'OVERVIEW',
        category: 'authority'
      });
      rawLinks.push({ from: caseNodeId, to: officerNodeId, stroke: 'rgba(16, 185, 129, 0.5)', width: 1.5 });
    }

    // 5. Victim Nodes
    victims.slice(0, 2).forEach((v, idx) => {
      const vNodeId = `node-victim-${v.id || idx}`;
      const title = v.name ? (v.age ? `${v.name} (Age ${v.age})` : v.name) : `Victim #${idx + 1}`;
      rawNodes.push({
        id: vNodeId,
        type: 'VICTIM',
        title: title.length > 24 ? title.slice(0, 22) + '...' : title,
        subtitle: v.gender || v.relationshipToAccused || 'Identified Complainant',
        icon: UserMinus,
        color: '#06B6D4',
        tab: 'VICTIMS',
        category: 'victim'
      });
      rawLinks.push({ from: caseNodeId, to: vNodeId, stroke: 'rgba(6, 182, 212, 0.6)', width: 2 });
    });

    // 6. Suspect Nodes
    suspects.slice(0, 2).forEach((s, idx) => {
      const sNodeId = `node-suspect-${s.id || idx}`;
      const title = s.name ? (s.alias ? `${s.name} ("${s.alias}")` : s.name) : `Suspect #${idx + 1}`;
      rawNodes.push({
        id: sNodeId,
        type: 'SUSPECT',
        title: title.length > 24 ? title.slice(0, 22) + '...' : title,
        subtitle: s.status ? `Status: ${s.status}` : 'Person of Interest',
        icon: UserX,
        color: '#C1121F',
        tab: 'SUSPECTS',
        category: 'suspect'
      });
      rawLinks.push({ from: caseNodeId, to: sNodeId, stroke: 'rgba(193, 18, 31, 0.7)', width: 2 });
    });

    // 7. Witness Nodes
    witnesses.slice(0, 2).forEach((w, idx) => {
      const wNodeId = `node-witness-${w.id || idx}`;
      const title = w.name ? (w.statementSummary ? `${w.name}` : w.name) : `Witness #${idx + 1}`;
      rawNodes.push({
        id: wNodeId,
        type: 'WITNESS',
        title: title.length > 24 ? title.slice(0, 22) + '...' : title,
        subtitle: w.statementSummary ? (w.statementSummary.slice(0, 20) + '...') : 'Recorded Testimony',
        icon: Eye,
        color: '#FBBF24',
        tab: 'WITNESSES',
        category: 'witness'
      });
      rawLinks.push({ from: caseNodeId, to: wNodeId, stroke: 'rgba(251, 191, 36, 0.5)', width: 1.5, dashed: true });
      // Connect witness to first suspect if exists
      if (suspects.length > 0) {
        rawLinks.push({ 
          from: wNodeId, 
          to: `node-suspect-${suspects[0].id || 0}`, 
          stroke: 'rgba(245, 158, 11, 0.5)', 
          width: 1.5, 
          dashed: true 
        });
      }
    });

    // 8. Evidence Nodes (Categorized into CCTV, VIDEO, AUDIO, FORENSIC REPORT, DIGITAL DEVICE, EVIDENCE)
    evidence.slice(0, 4).forEach((ev, idx) => {
      const evNodeId = `node-evidence-${ev.id || idx}`;
      const titleLower = (ev.title || '').toLowerCase();
      const typeLower = (ev.evidenceType || '').toLowerCase();
      
      let nodeType = 'EVIDENCE';
      let icon = FileText;
      let color = '#10B981';

      if (titleLower.includes('cctv') || typeLower.includes('cctv') || titleLower.includes('camera')) {
        nodeType = 'CCTV';
        icon = Video;
        color = '#06B6D4';
      } else if (titleLower.includes('video') || typeLower.includes('video')) {
        nodeType = 'VIDEO';
        icon = Video;
        color = '#38BDF8';
      } else if (titleLower.includes('audio') || titleLower.includes('call') || titleLower.includes('cdr') || typeLower.includes('audio')) {
        nodeType = 'AUDIO';
        icon = Radio;
        color = '#F59E0B';
      } else if (titleLower.includes('lab') || titleLower.includes('autopsy') || titleLower.includes('ballistics') || titleLower.includes('forensic') || typeLower.includes('forensic')) {
        nodeType = 'FORENSIC REPORT';
        icon = Disc;
        color = '#EC4899';
      } else if (titleLower.includes('phone') || titleLower.includes('mobile') || titleLower.includes('laptop') || titleLower.includes('device') || typeLower.includes('digital')) {
        nodeType = 'DIGITAL DEVICE';
        icon = Smartphone;
        color = '#A855F7';
      }

      const displayTitle = ev.title || `Evidence #${idx + 1}`;
      rawNodes.push({
        id: evNodeId,
        type: nodeType,
        title: displayTitle.length > 24 ? displayTitle.slice(0, 22) + '...' : displayTitle,
        subtitle: ev.evidenceNumber || ev.status || 'Vault Custody',
        icon: icon,
        color: color,
        tab: 'EVIDENCE',
        category: 'evidence'
      });

      // Connect evidence to Case
      rawLinks.push({ from: caseNodeId, to: evNodeId, stroke: 'rgba(16, 185, 129, 0.4)', width: 1.5 });

      // Connect CCTV/Video to Location if exists
      if (nodeType === 'CCTV' && locationName) {
        rawLinks.push({ from: evNodeId, to: 'node-location', stroke: 'rgba(6, 182, 212, 0.4)', width: 1.5, dashed: true });
      }

      // Connect Forensic/Digital to Suspect if exists
      if ((nodeType === 'FORENSIC REPORT' || nodeType === 'DIGITAL DEVICE') && suspects.length > 0) {
        rawLinks.push({ 
          from: evNodeId, 
          to: `node-suspect-${suspects[0].id || 0}`, 
          stroke: 'rgba(236, 72, 153, 0.5)', 
          width: 1.5 
        });
      }
    });

    // 9. Position calculation algorithm (Clean Responsive Layout)
    const count = rawNodes.length;
    const positionedNodes = rawNodes.map((node, index) => {
      let x = 50;
      let y = 50;

      if (count === 1) {
        x = 50; y = 50;
      } else if (node.id === caseNodeId) {
        // Case node in center top
        x = 50; y = 20;
      } else {
        // Distribute remaining nodes in 2 layers / arc
        const otherNodes = rawNodes.filter(n => n.id !== caseNodeId);
        const otherIndex = otherNodes.findIndex(n => n.id === node.id);
        const totalOther = otherNodes.length;

        if (totalOther <= 3) {
          // Single bottom row
          const step = 80 / (totalOther + 1);
          x = 10 + (otherIndex + 1) * step;
          y = 75;
        } else if (totalOther <= 6) {
          // Two rows (Mid and Bottom)
          const isMidRow = otherIndex < Math.ceil(totalOther / 2);
          const rowNodes = isMidRow ? Math.ceil(totalOther / 2) : Math.floor(totalOther / 2);
          const colIndex = isMidRow ? otherIndex : otherIndex - Math.ceil(totalOther / 2);
          const step = 80 / (rowNodes + 1);
          x = 10 + (colIndex + 1) * step;
          y = isMidRow ? 50 : 82;
        } else {
          // Three rows (Upper, Mid, Bottom)
          const rowIdx = Math.floor(otherIndex / 3);
          const colIdx = otherIndex % 3;
          x = 18 + colIdx * 32;
          y = 42 + rowIdx * 25;
        }
      }

      return {
        ...node,
        x: `${Math.round(x)}%`,
        y: `${Math.round(y)}%`,
        rawX: x,
        rawY: y
      };
    });

    return { nodes: positionedNodes, connections: rawLinks };
  }, [caseData, fir, victims, suspects, witnesses, evidence]);

  const handleNodeClick = (node) => {
    if (onSelectNode) {
      onSelectNode(node);
    } else if (node.customPath) {
      navigate(node.customPath);
    } else if (caseData && caseData.id && node.tab) {
      navigate(`/cases/${caseData.id}?tab=${node.tab}`);
    }
  };

  const caseIdDisplay = caseData?.caseNumber || (caseData?.id ? `CASE #${caseData.id.slice(0, 8).toUpperCase()}` : 'NO CASE SELECTED');

  return (
    <Panel 
      title="INVESTIGATION LINK BOARD" 
      subtitle={`Entity Relationship Matrix — ${caseIdDisplay}`}
      icon={Network}
      action={
        <div className="board-header-tag">
          <span className="live-dot"></span>
          <span>{loading ? 'SYNCING MATRIX...' : `${nodes.length} CASE ENTITIES`}</span>
        </div>
      }
    >
      <div className="investigation-board-container forensic-grid">
        {loading ? (
          <div className="board-loading-state">
            <div className="spinner-glow"></div>
            <span>ANALYZING CASE TOPOLOGY...</span>
          </div>
        ) : !caseData || nodes.length === 0 ? (
          <div className="board-empty-state">
            <Layers size={32} className="empty-icon" />
            <div className="empty-title">NO CASE SELECTED</div>
            <div className="empty-desc">Select an active investigation from the case table to inspect entity relationships.</div>
          </div>
        ) : (
          <>
            {/* SVG Connector Lines */}
            <svg className="board-svg-canvas">
              {connections.map((link, idx) => {
                const fromNode = nodes.find(n => n.id === link.from);
                const toNode = nodes.find(n => n.id === link.to);
                if (!fromNode || !toNode) return null;

                return (
                  <line 
                    key={idx}
                    x1={fromNode.x} 
                    y1={fromNode.y} 
                    x2={toNode.x} 
                    y2={toNode.y} 
                    stroke={link.stroke || 'rgba(193, 18, 31, 0.5)'} 
                    strokeWidth={link.width || 1.5} 
                    strokeDasharray={link.dashed ? '4 3' : 'none'} 
                  />
                );
              })}
            </svg>

            {/* Node Cards */}
            {nodes.map((node) => {
              const Icon = node.icon;
              return (
                <div 
                  key={node.id} 
                  className="board-node-card"
                  style={{ left: node.x, top: node.y }}
                  onClick={() => handleNodeClick(node)}
                  title={`Click to view ${node.type} records in Case Workspace`}
                >
                  <div className="node-badge" style={{ borderColor: node.color, color: node.color }}>
                    <Icon size={13} />
                    <span>{node.type}</span>
                  </div>
                  <div className="node-title">{node.title}</div>
                  {node.subtitle && <div className="node-subtitle">{node.subtitle}</div>}
                  <div className="node-hover-arrow">
                    <ExternalLink size={10} />
                  </div>
                </div>
              );
            })}
          </>
        )}
      </div>

      <style>{`
        .board-header-tag {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          color: var(--bright-crime-red);
          background: rgba(193, 18, 31, 0.12);
          border: 1px solid rgba(193, 18, 31, 0.4);
          padding: 2px 8px;
          border-radius: 3px;
        }

        .live-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background-color: var(--bright-crime-red);
          animation: pulse 1.5s infinite;
        }

        .investigation-board-container {
          min-height: 400px;
          height: 420px;
          position: relative;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: var(--card-radius);
          overflow: hidden;
        }

        .board-svg-canvas {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          pointer-events: none;
          z-index: 1;
        }

        .board-loading-state,
        .board-empty-state {
          height: 100%;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 10px;
          color: var(--text-muted);
          font-family: var(--font-subheading);
          text-align: center;
          padding: 20px;
        }

        .empty-icon {
          color: var(--text-muted);
          opacity: 0.5;
        }

        .empty-title {
          font-family: var(--font-subheading);
          font-size: 0.85rem;
          font-weight: 700;
          color: var(--text-secondary);
          letter-spacing: 0.08em;
        }

        .empty-desc {
          font-size: 0.75rem;
          max-width: 320px;
          color: var(--text-muted);
          line-height: 1.4;
        }

        .spinner-glow {
          width: 28px;
          height: 28px;
          border: 2px solid rgba(193, 18, 31, 0.2);
          border-top-color: var(--bright-crime-red);
          border-radius: 50%;
          animation: spin 0.8s linear infinite;
        }

        .board-node-card {
          position: absolute;
          transform: translate(-50%, -50%);
          z-index: 2;
          background-color: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 8px 10px;
          width: 175px;
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.7);
          transition: all var(--transition-fast);
          cursor: pointer;
          user-select: none;
        }

        .board-node-card:hover {
          border-color: var(--bright-crime-red);
          box-shadow: 0 0 12px rgba(193, 18, 31, 0.35);
          transform: translate(-50%, -52%);
          z-index: 10;
        }

        .node-badge {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          font-family: var(--font-subheading);
          font-size: 0.62rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          border: 1px solid;
          padding: 2px 6px;
          border-radius: 2px;
          background: rgba(18, 18, 20, 0.85);
          margin-bottom: 4px;
        }

        .node-title {
          font-family: var(--font-heading);
          font-size: 0.78rem;
          color: var(--text-bright);
          font-weight: 600;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .node-subtitle {
          font-family: var(--font-mono);
          font-size: 0.65rem;
          color: var(--text-muted);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          margin-top: 2px;
        }

        .node-hover-arrow {
          position: absolute;
          top: 6px;
          right: 6px;
          opacity: 0;
          color: var(--bright-crime-red);
          transition: opacity var(--transition-fast);
        }

        .board-node-card:hover .node-hover-arrow {
          opacity: 1;
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
      `}</style>
    </Panel>
  );
}

