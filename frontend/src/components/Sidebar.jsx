import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Briefcase, 
  Search, 
  ShieldAlert, 
  Globe, 
  BrainCircuit, 
  GitCompare, 
  Cpu, 
  Bell, 
  History, 
  Settings, 
  Lock,
  Building2,
  UserCheck,
  MapPin
} from 'lucide-react';
import NotificationPreferencesModal from './NotificationPreferencesModal';
import AuditLogsModal from './AuditLogsModal';
import CaseSelectorModal from './CaseSelectorModal';

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();

  const [isPrefModalOpen, setIsPrefModalOpen] = useState(false);
  const [isAuditModalOpen, setIsAuditModalOpen] = useState(false);
  const [selectorConfig, setSelectorConfig] = useState({ isOpen: false, mode: 'AI_INVESTIGATION' });

  const navSections = [
    {
      title: 'COMMAND',
      items: [
        { label: 'Dashboard', icon: LayoutDashboard, path: '/dashboard' },
        { label: 'All Cases', icon: Briefcase, path: '/cases' },
        { label: 'Investigation Queue', icon: Search, path: '/cases?status=UNDER_INVESTIGATION' },
      ]
    },
    {
      title: 'INTELLIGENCE',
      items: [
        { label: 'Location Intelligence', icon: MapPin, path: '/location-intelligence' },
        { label: 'Evidence Gap Analysis', icon: ShieldAlert, path: '/evidence-gaps' },
        { label: 'Public News', icon: Globe, path: '/public-news' },
        { label: 'Similar Cases', icon: GitCompare, action: 'open_rag' },
      ]
    },
    {
      title: 'SYSTEM & OPERATIONS',
      items: [
        { label: 'Inter-Station Coordination', icon: Building2, path: '/inter-station-requests' },
        { label: 'Officer Approvals', icon: UserCheck, path: '/officer-approvals' },
        { label: 'Analytics', icon: Cpu, path: '/dashboard' },
        { label: 'Notifications', icon: Bell, action: 'open_settings' },
        { label: 'Audit Logs', icon: History, action: 'open_audit' },
        { label: 'Settings', icon: Settings, action: 'open_settings' },
      ]
    }
  ];

  const handleItemClick = (item) => {
    if (item.action === 'open_settings') {
      setIsPrefModalOpen(true);
    } else if (item.action === 'open_audit') {
      setIsAuditModalOpen(true);
    } else if (item.action === 'open_ai') {
      setSelectorConfig({ isOpen: true, mode: 'AI_INVESTIGATION' });
    } else if (item.action === 'open_rag') {
      setSelectorConfig({ isOpen: true, mode: 'SIMILAR_CASES' });
    } else if (item.path) {
      navigate(item.path);
    }
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-content">
        {navSections.map((section, idx) => (
          <div key={idx} className="nav-section">
            <div className="section-header">{section.title}</div>
            <div className="section-items">
              {section.items.map((item, itemIdx) => {
                const Icon = item.icon;
                const fullCurrent = location.pathname + location.search;
                const isCurrent = item.path 
                  ? (fullCurrent === item.path || 
                     (item.path === '/dashboard' && location.pathname === '/') || 
                     (item.path === '/cases' && location.pathname === '/cases' && !location.search) ||
                     (item.path === '/location-intelligence' && location.pathname.startsWith('/location-intelligence')))
                  : false;

                return (
                  <button
                    key={itemIdx}
                    className={`nav-item ${isCurrent ? 'active' : ''}`}
                    onClick={() => handleItemClick(item)}
                    title={item.label}
                  >
                    <Icon size={16} className="nav-icon" />
                    <span className="nav-label">{item.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      <NotificationPreferencesModal 
        isOpen={isPrefModalOpen}
        onClose={() => setIsPrefModalOpen(false)}
      />

      <AuditLogsModal
        isOpen={isAuditModalOpen}
        onClose={() => setIsAuditModalOpen(false)}
      />

      <CaseSelectorModal
        isOpen={selectorConfig.isOpen}
        mode={selectorConfig.mode}
        onClose={() => setSelectorConfig({ isOpen: false, mode: 'AI_INVESTIGATION' })}
      />

      {/* Security Level Indicator Footer */}
      <div className="sidebar-footer">
        <div className="security-badge">
          <Lock size={12} className="lock-icon" />
          <span>SECURITY LEVEL: POLICE-CONFIDENTIAL</span>
        </div>
      </div>

      <style>{`
        .sidebar {
          width: var(--sidebar-width);
          flex-shrink: 0;
          background-color: var(--bg-secondary);
          border-right: 1px solid var(--border-color);
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          height: calc(100vh - var(--topbar-height));
          position: sticky;
          top: var(--topbar-height);
          user-select: none;
        }

        @media (max-width: 768px) {
          .sidebar {
            width: 100%;
            height: auto;
            position: static;
            border-right: none;
            border-bottom: 1px solid var(--border-color);
          }
        }

        .sidebar-content {
          padding: 16px 12px;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .nav-section {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .section-header {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.15em;
          color: var(--text-muted);
          padding: 0 8px 4px 8px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.5);
        }

        .section-items {
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .nav-item {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 8px 10px;
          background: transparent;
          border: 1px solid transparent;
          border-radius: 4px;
          color: var(--text-secondary);
          cursor: pointer;
          font-family: var(--font-body);
          font-size: 0.85rem;
          text-align: left;
          transition: all var(--transition-fast);
          width: 100%;
        }

        .nav-item:hover {
          background-color: var(--bg-panel);
          color: var(--text-bright);
          border-color: rgba(42, 42, 45, 0.8);
        }

        .nav-item.active {
          background-color: rgba(193, 18, 31, 0.12);
          border: 1px solid var(--bright-crime-red);
          color: var(--text-bright);
          box-shadow: var(--red-glow-subtle);
        }

        .nav-item.active .nav-icon {
          color: var(--bright-crime-red);
        }

        .nav-icon {
          color: var(--text-muted);
          flex-shrink: 0;
          transition: color var(--transition-fast);
        }

        .nav-item:hover .nav-icon {
          color: var(--text-primary);
        }

        .nav-label {
          flex-grow: 1;
        }

        .sidebar-footer {
          padding: 12px;
          border-top: 1px solid var(--border-color);
          background-color: var(--bg-primary);
        }

        .security-badge {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 0.65rem;
          font-family: var(--font-subheading);
          color: var(--text-muted);
          letter-spacing: 0.05em;
          background: rgba(18, 18, 20, 0.9);
          padding: 6px 8px;
          border: 1px solid var(--border-color);
          border-radius: 3px;
        }

        .lock-icon {
          color: var(--bright-crime-red);
        }
      `}</style>
    </aside>
  );
}
