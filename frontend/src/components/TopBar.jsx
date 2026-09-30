import React, { useState, useEffect, useRef } from 'react';
import { Shield, Bell, UserCheck, Database, LogOut, Mail, MessageSquare, Settings, CheckCircle, AlertCircle, RefreshCw, ChevronRight, X, CheckCheck } from 'lucide-react';
import { getBackendHealth, getAIServiceHealth, getDatabaseHealth, getNotifications, getUnreadNotificationCount, markNotificationRead, markAllNotificationsRead } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import NotificationPreferencesModal from './NotificationPreferencesModal';
import WhatsAppPreviewModal from './WhatsAppPreviewModal';
import NotificationDetailsModal from './NotificationDetailsModal';

export default function TopBar() {
  const { officer, station, logout } = useAuth();
  const navigate = useNavigate();

  const [currentTime, setCurrentTime] = useState('');
  const [backendStatus, setBackendStatus] = useState('CHECKING');
  const [dbStatus, setDbStatus] = useState('CHECKING');
  const [aiStatus, setAiStatus] = useState('CHECKING');

  // Notification Center state
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [isPrefModalOpen, setIsPrefModalOpen] = useState(false);
  const [selectedWaNotif, setSelectedWaNotif] = useState(null);
  const [isWaModalOpen, setIsWaModalOpen] = useState(false);
  const [selectedDetailsNotif, setSelectedDetailsNotif] = useState(null);
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [loadingNotifs, setLoadingNotifs] = useState(false);
  const notifDropdownRef = useRef(null);

  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      setCurrentTime(now.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' }) + ' IST');
    };
    updateClock();
    const timer = setInterval(updateClock, 1000);

    // Initial Health Checks
    getBackendHealth()
      .then(res => setBackendStatus(res?.status === 'UP' ? 'ONLINE' : 'OFFLINE'))
      .catch(() => setBackendStatus('OFFLINE'));
    getDatabaseHealth()
      .then(res => setDbStatus(res?.database === 'connected' ? 'CONNECTED' : 'DISCONNECTED'))
      .catch(() => setDbStatus('DISCONNECTED'));
    getAIServiceHealth()
      .then(res => setAiStatus(res?.status === 'UP' ? 'ONLINE' : 'OFFLINE'))
      .catch(() => setAiStatus('OFFLINE'));

    // Fetch initial notifications
    fetchNotifications();
    const notifInterval = setInterval(fetchNotifications, 5000);

    const handleNotifUpdateEvent = () => {
      fetchNotifications();
    };
    window.addEventListener('crimesphere:notification-updated', handleNotifUpdateEvent);

    return () => {
      clearInterval(timer);
      clearInterval(notifInterval);
      window.removeEventListener('crimesphere:notification-updated', handleNotifUpdateEvent);
    };
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (notifDropdownRef.current && !notifDropdownRef.current.contains(event.target)) {
        setIsNotifOpen(false);
      }
    }
    if (isNotifOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isNotifOpen]);

  const fetchNotifications = async () => {
    try {
      setLoadingNotifs(true);
      const [res, unreadRes] = await Promise.all([
        getNotifications({ page: 0, size: 8 }),
        getUnreadNotificationCount().catch(() => null)
      ]);
      if (res && res.content) {
        setNotifications(res.content);
        if (unreadRes && typeof unreadRes.unreadCount === 'number') {
          setUnreadCount(unreadRes.unreadCount);
        } else {
          const count = res.content.filter(n => !n.isRead && n.status !== 'FAILED').length;
          setUnreadCount(count);
        }
      }
    } catch (e) {
      // Non-blocking
    } finally {
      setLoadingNotifs(false);
    }
  };

  const handleNotifClick = async (notif) => {
    if (!notif.isRead) {
      try {
        await markNotificationRead(notif.id);
        fetchNotifications();
      } catch (e) {
        // silent catch
      }
    }
    if (notif.channel === 'WHATSAPP') {
      setSelectedWaNotif(notif);
      setIsWaModalOpen(true);
      return;
    }
    if (notif.channel === 'EMAIL' || notif.status === 'FAILED') {
      setSelectedDetailsNotif(notif);
      setIsDetailsModalOpen(true);
      return;
    }
    if (notif.caseId) {
      setIsNotifOpen(false);
      navigate(`/cases/${notif.caseId}`);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead();
      fetchNotifications();
    } catch (e) {
      // silent catch
    }
  };

  const formatRelativeTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays}d ago`;
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const displayName = officer?.fullName || 'INSP. V. RATHORE';
  const displayRole = officer?.role ? `${officer.role} • ${officer.policeId}` : 'SENIOR INVESTIGATOR';
  const displayStation = officer?.station?.stationName || station?.stationName || 'COIMBATORE CENTRAL PS';

  return (
    <header className="topbar">
      <div className="topbar-left">
        <div className="brand-logo-container">
          <Shield className="brand-icon" size={24} />
        </div>
        <div className="brand-text">
          <div className="brand-title">CRIMESPHERE <span className="brand-title-accent">AI</span></div>
          <div className="brand-subtitle">CRIME INTELLIGENCE SYSTEM</div>
        </div>
      </div>

      <div className="topbar-center">
        <div className="active-investigation-badge">
          <span className="investigation-label">SYSTEM TERMINAL:</span>
          <span className="case-id-tag">{displayStation || 'STATION ACTIVE'}</span>
        </div>
      </div>

      <div className="topbar-right">
        {/* Database Status Indicator */}
        <div className="system-status-indicator" title={`Database: ${dbStatus}`}>
          <Database size={13} className={dbStatus === 'CONNECTED' ? 'db-icon-connected' : 'db-icon-disconnected'} />
          <span className="status-text">DB {dbStatus}</span>
        </div>

        {/* System Status Indicators */}
        <div className="system-status-indicator" title={`Backend: ${backendStatus} | AI: ${aiStatus}`}>
          <span className={backendStatus === 'ONLINE' ? 'pulse-dot-green' : 'pulse-dot-red'}></span>
          <span className="status-text">SYSTEM {backendStatus}</span>
        </div>

        {/* Live Clock */}
        <div className="system-time">
          {currentTime || '00:00:00 IST'}
        </div>

        {/* Notification Center Dropdown */}
        <div className="notif-wrapper" ref={notifDropdownRef}>
          <button 
            className={`icon-btn ${isNotifOpen ? 'btn-active' : ''}`}
            title="Alerts & Notifications"
            onClick={() => setIsNotifOpen(!isNotifOpen)}
          >
            <Bell size={18} />
            {unreadCount > 0 && (
              <span className="notification-badge">{unreadCount > 9 ? '9+' : unreadCount}</span>
            )}
          </button>

          {isNotifOpen && (
            <div className="notif-dropdown">
              <div className="notif-dropdown-header">
                <div className="notif-title-row">
                  <span className="notif-dropdown-title">NOTIFICATIONS & ALERTS</span>
                  <div className="notif-header-actions">
                    {unreadCount > 0 && (
                      <button
                        className="btn-icon-subtle"
                        onClick={handleMarkAllRead}
                        title="Mark all as read"
                      >
                        <CheckCheck size={13} />
                      </button>
                    )}
                    <button 
                      className="btn-icon-subtle" 
                      onClick={fetchNotifications} 
                      title="Refresh"
                    >
                      <RefreshCw size={13} className={loadingNotifs ? 'spin-icon' : ''} />
                    </button>
                    <button 
                      className="btn-icon-subtle" 
                      onClick={() => { setIsNotifOpen(false); setIsPrefModalOpen(true); }}
                      title="Notification Preferences"
                    >
                      <Settings size={13} />
                    </button>
                  </div>
                </div>
                <div className="notif-dropdown-subtitle">
                  {notifications.length} recent system alert{notifications.length === 1 ? '' : 's'}
                </div>
              </div>

              <div className="notif-list">
                {notifications.length === 0 ? (
                  <div className="notif-empty-state">
                    <Bell size={24} className="empty-bell" />
                    <span>No notifications received</span>
                  </div>
                ) : (
                  notifications.map((notif) => (
                    <div 
                      key={notif.id} 
                      className={`notif-item ${notif.isRead ? 'notif-read' : 'notif-unread'}`}
                      onClick={() => handleNotifClick(notif)}
                    >
                      <div className="notif-item-top">
                        <span className="notif-type-tag">
                          {(notif.notificationType || '').replace(/_/g, ' ')}
                        </span>
                        <div className="notif-badges-row">
                          <span 
                            className={`channel-pill ${notif.channel === 'EMAIL' ? 'pill-email' : notif.channel === 'WHATSAPP' ? 'pill-whatsapp' : 'pill-inapp'}`}
                            title={notif.channel === 'EMAIL' ? 'Email delivery channel — Click for details & retry' : `${notif.channel} Channel`}
                            onClick={(e) => {
                              if (notif.channel === 'EMAIL') {
                                e.stopPropagation();
                                setSelectedDetailsNotif(notif);
                                setIsDetailsModalOpen(true);
                              }
                            }}
                          >
                            {notif.channel === 'EMAIL' ? <Mail size={10} /> : notif.channel === 'WHATSAPP' ? <MessageSquare size={10} /> : <Bell size={10} />}
                            {notif.channel}
                          </span>
                          <span 
                            className={`status-pill pill-${(notif.status || '').toLowerCase()}`}
                            title={notif.status === 'FAILED' ? `Failed: ${notif.lastError || 'Click to view details & retry'}` : `Status: ${notif.status}`}
                            onClick={(e) => {
                              if (notif.status === 'FAILED' || notif.channel === 'EMAIL') {
                                e.stopPropagation();
                                setSelectedDetailsNotif(notif);
                                setIsDetailsModalOpen(true);
                              }
                            }}
                          >
                            {notif.status}
                          </span>
                        </div>
                      </div>

                      {notif.caseNumber && (
                        <div className="notif-case-link">
                          <span>CASE {notif.caseNumber}</span>
                          <ChevronRight size={12} />
                        </div>
                      )}

                      <div className="notif-message-text">
                        {notif.subject ? notif.subject : (notif.message ? notif.message.split('\n')[0] : 'Notification update.')}
                      </div>

                      <div className="notif-time-row">
                        <span>{formatRelativeTime(notif.createdAt)}</span>
                        {notif.status === 'FAILED' ? (
                          <button 
                            className="btn-inline-retry"
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedDetailsNotif(notif);
                              setIsDetailsModalOpen(true);
                            }}
                            title="Click to view failure reason and retry email"
                          >
                            <RefreshCw size={10} />
                            <span>Details & Retry</span>
                          </button>
                        ) : notif.recipientName ? (
                          <span className="notif-recipient-hint">{notif.recipientName}</span>
                        ) : null}
                      </div>
                    </div>
                  ))
                )}
              </div>

              <div className="notif-dropdown-footer">
                <button 
                  className="btn-open-settings"
                  onClick={() => { setIsNotifOpen(false); setIsPrefModalOpen(true); }}
                >
                  <Settings size={13} />
                  <span>Configure Preferences</span>
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Officer Profile Badge with dynamic data */}
        <div className="officer-profile-badge" title={`Station: ${displayStation}`}>
          <div className="avatar-placeholder">
            <UserCheck size={16} />
          </div>
          <div className="officer-info">
            <span className="officer-name">{displayName}</span>
            <span className="officer-rank">{displayRole}</span>
          </div>
        </div>

        {/* Secure Logout Button */}
        <button onClick={handleLogout} className="icon-btn logout-btn" title="Secure Logout">
          <LogOut size={16} />
        </button>
      </div>

      {/* Notification Preferences Modal */}
      <NotificationPreferencesModal 
        isOpen={isPrefModalOpen}
        onClose={() => setIsPrefModalOpen(false)}
      />

      {/* WhatsApp Preview Modal */}
      <WhatsAppPreviewModal
        isOpen={isWaModalOpen}
        onClose={() => setIsWaModalOpen(false)}
        notification={selectedWaNotif}
      />

      {/* Notification Delivery & Retry Modal */}
      <NotificationDetailsModal
        isOpen={isDetailsModalOpen}
        onClose={() => setIsDetailsModalOpen(false)}
        notification={selectedDetailsNotif}
        onRetrySuccess={() => fetchNotifications()}
      />

      <style>{`
        .topbar {
          min-height: var(--topbar-height);
          height: auto;
          background-color: var(--bg-secondary);
          border-bottom: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 8px 16px;
          position: sticky;
          top: 0;
          z-index: 100;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
          width: 100%;
          max-width: 100%;
          min-width: 0;
          box-sizing: border-box;
          flex-wrap: wrap;
          gap: 10px;
        }

        .topbar-left {
          display: flex;
          align-items: center;
          gap: 12px;
          min-width: 0;
          max-width: 100%;
        }

        .brand-logo-container {
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          color: var(--bright-crime-red);
          padding: 8px;
          border-radius: var(--card-radius);
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: var(--red-glow-subtle);
          flex-shrink: 0;
        }

        .brand-title {
          font-family: var(--font-heading);
          font-size: 1.25rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
          line-height: 1.1;
        }

        .brand-title-accent {
          color: var(--bright-crime-red);
        }

        .brand-subtitle {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          letter-spacing: 0.18em;
          color: var(--text-secondary);
          text-transform: uppercase;
        }

        .topbar-center {
          display: flex;
          align-items: center;
          min-width: 0;
        }

        .active-investigation-badge {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          padding: 6px 14px;
          border-radius: 4px;
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 0.8rem;
        }

        .investigation-label {
          color: var(--text-muted);
          font-weight: 600;
          letter-spacing: 0.05em;
        }

        .case-id-tag {
          color: var(--bright-crime-red);
          font-weight: 700;
          letter-spacing: 0.06em;
          font-family: var(--font-subheading);
        }

        .topbar-right {
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
          min-width: 0;
        }

        @media (max-width: 900px) {
          .topbar-center { display: none; }
          .system-time { display: none; }
        }

        @media (max-width: 600px) {
          .system-status-indicator { display: none; }
          .officer-profile-badge { max-width: 140px; }
        }

        .system-status-indicator {
          display: flex;
          align-items: center;
          gap: 8px;
          background-color: rgba(18, 18, 20, 0.8);
          border: 1px solid var(--border-color);
          padding: 6px 10px;
          border-radius: 4px;
          font-size: 0.72rem;
        }

        .db-icon-connected {
          color: var(--color-success);
        }

        .db-icon-disconnected {
          color: var(--bright-crime-red);
        }

        .status-text {
          font-family: var(--font-subheading);
          font-weight: 600;
          letter-spacing: 0.08em;
          color: var(--text-secondary);
        }

        .system-time {
          font-family: var(--font-mono);
          font-size: 0.85rem;
          color: var(--text-primary);
          background-color: var(--bg-panel);
          padding: 6px 10px;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          letter-spacing: 0.05em;
        }

        .icon-btn {
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          color: var(--text-secondary);
          padding: 8px;
          border-radius: 4px;
          cursor: pointer;
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all var(--transition-fast);
        }

        .icon-btn:hover {
          color: var(--text-bright);
          border-color: var(--bright-crime-red);
        }

        .logout-btn:hover {
          color: var(--bright-crime-red);
          border-color: var(--bright-crime-red);
          background-color: rgba(193, 18, 31, 0.15);
        }

        .notification-badge {
          position: absolute;
          top: -4px;
          right: -4px;
          background-color: var(--bright-crime-red);
          color: white;
          font-size: 0.65rem;
          font-weight: 700;
          width: 16px;
          height: 16px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .officer-profile-badge {
          display: flex;
          align-items: center;
          gap: 10px;
          background-color: var(--bg-panel-elevated);
          border: 1px solid var(--border-color);
          padding: 4px 12px 4px 6px;
          border-radius: 4px;
          max-width: 220px;
        }

        .avatar-placeholder {
          background-color: rgba(139, 0, 0, 0.3);
          border: 1px solid var(--primary-crimson);
          color: var(--text-bright);
          width: 30px;
          height: 30px;
          border-radius: 3px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .officer-info {
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }

        .officer-name {
          font-family: var(--font-subheading);
          font-weight: 700;
          font-size: 0.75rem;
          letter-spacing: 0.05em;
          color: var(--text-bright);
          line-height: 1.1;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .officer-rank {
          font-size: 0.6rem;
          color: var(--text-muted);
          letter-spacing: 0.04em;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        /* Notification Center Styles */
        .notif-wrapper {
          position: relative;
        }

        .btn-active {
          color: var(--bright-crime-red);
          border-color: var(--bright-crime-red);
          background-color: rgba(193, 18, 31, 0.12);
        }

        .notif-dropdown {
          position: absolute;
          top: calc(100% + 8px);
          right: 0;
          width: 360px;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 8px;
          box-shadow: 0 10px 30px rgba(0, 0, 0, 0.8), var(--red-glow-subtle);
          z-index: 1000;
          overflow: hidden;
          display: flex;
          flex-direction: column;
          animation: dropIn 0.15s ease-out;
        }

        @keyframes dropIn {
          from { opacity: 0; transform: translateY(-6px); }
          to { opacity: 1; transform: translateY(0); }
        }

        .notif-dropdown-header {
          padding: 12px 14px;
          background-color: var(--bg-secondary);
          border-bottom: 1px solid var(--border-color);
        }

        .notif-title-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .notif-dropdown-title {
          font-family: var(--font-heading);
          font-size: 0.78rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          color: var(--text-bright);
        }

        .notif-header-actions {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .btn-icon-subtle {
          background: transparent;
          border: none;
          color: var(--text-muted);
          cursor: pointer;
          padding: 4px;
          border-radius: 3px;
          display: flex;
          transition: all var(--transition-fast);
        }

        .btn-icon-subtle:hover {
          color: var(--text-bright);
          background-color: rgba(255, 255, 255, 0.06);
        }

        .notif-dropdown-subtitle {
          font-size: 0.68rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .notif-list {
          max-height: 380px;
          overflow-y: auto;
          display: flex;
          flex-direction: column;
        }

        .notif-empty-state {
          padding: 30px 16px;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
          color: var(--text-muted);
          font-size: 0.78rem;
        }

        .empty-bell {
          opacity: 0.3;
        }

        .notif-item {
          padding: 10px 14px;
          border-bottom: 1px solid rgba(42, 42, 45, 0.4);
          cursor: pointer;
          transition: background var(--transition-fast);
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .notif-item:hover {
          background-color: rgba(193, 18, 31, 0.06);
        }

        .notif-item:last-child {
          border-bottom: none;
        }

        .notif-item-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 6px;
        }

        .notif-type-tag {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          color: var(--text-primary);
        }

        .notif-badges-row {
          display: flex;
          align-items: center;
          gap: 6px;
        }

        .channel-pill {
          display: flex;
          align-items: center;
          gap: 3px;
          font-size: 0.6rem;
          font-weight: 600;
          padding: 2px 6px;
          border-radius: 3px;
          text-transform: uppercase;
        }

        .pill-email {
          background-color: rgba(30, 136, 229, 0.15);
          color: #64b5f6;
          border: 1px solid rgba(30, 136, 229, 0.3);
        }

        .pill-whatsapp {
          background-color: rgba(46, 125, 50, 0.15);
          color: #81c784;
          border: 1px solid rgba(46, 125, 50, 0.3);
        }

        .status-pill {
          font-size: 0.58rem;
          font-weight: 700;
          padding: 1px 5px;
          border-radius: 3px;
          text-transform: uppercase;
        }

        .pill-sent {
          background-color: rgba(46, 125, 50, 0.2);
          color: #a5d6a7;
        }

        .pill-pending, .pill-sending, .pill-retrying {
          background-color: rgba(255, 179, 0, 0.2);
          color: #ffe082;
        }

        .pill-failed {
          background-color: rgba(193, 18, 31, 0.2);
          color: #ff8a80;
        }

        .btn-inline-retry {
          background-color: rgba(193, 18, 31, 0.18);
          border: 1px solid rgba(193, 18, 31, 0.4);
          color: #ff8a80;
          font-size: 0.6rem;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 4px;
          transition: all var(--transition-fast);
        }

        .btn-inline-retry:hover {
          background-color: var(--bright-crime-red);
          color: white;
          border-color: var(--bright-crime-red);
        }

        .notif-case-link {
          display: flex;
          align-items: center;
          gap: 4px;
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          font-weight: 700;
          color: var(--bright-crime-red);
        }

        .notif-message-text {
          font-size: 0.72rem;
          color: var(--text-secondary);
          line-height: 1.3;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .notif-time-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-size: 0.65rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .notif-recipient-hint {
          max-width: 160px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .notif-dropdown-footer {
          padding: 8px 14px;
          background-color: var(--bg-secondary);
          border-top: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .btn-open-settings {
          background: transparent;
          border: none;
          color: var(--text-muted);
          cursor: pointer;
          font-size: 0.72rem;
          display: flex;
          align-items: center;
          gap: 6px;
          transition: color var(--transition-fast);
        }

        .btn-open-settings:hover {
          color: var(--bright-crime-red);
        }
      `}</style>
    </header>
  );
}
