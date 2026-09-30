import React from 'react';

export default function StatusBadge({ type = 'neutral', label }) {
  const getBadgeStyle = () => {
    switch (type.toLowerCase()) {
      case 'high':
      case 'critical':
      case 'missing':
      case 'danger':
        return {
          bg: 'rgba(193, 18, 31, 0.15)',
          border: 'var(--bright-crime-red)',
          color: '#FF4D4D',
          dot: 'var(--bright-crime-red)'
        };
      case 'medium':
      case 'pending':
      case 'warning':
        return {
          bg: 'rgba(217, 119, 6, 0.15)',
          border: 'var(--color-warning)',
          color: '#FBBF24',
          dot: 'var(--color-warning)'
        };
      case 'low':
      case 'verified':
      case 'available':
      case 'success':
        return {
          bg: 'rgba(16, 185, 129, 0.15)',
          border: 'var(--color-success)',
          color: '#34D399',
          dot: 'var(--color-success)'
        };
      default:
        return {
          bg: 'rgba(167, 167, 170, 0.1)',
          border: 'var(--border-color)',
          color: 'var(--text-secondary)',
          dot: 'var(--text-muted)'
        };
    }
  };

  const style = getBadgeStyle();

  return (
    <span className="cs-status-badge">
      <span className="badge-dot" style={{ backgroundColor: style.dot }}></span>
      {label || type.toUpperCase()}

      <style>{`
        .cs-status-badge {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 3px 8px;
          border-radius: 3px;
          font-family: var(--font-subheading);
          font-weight: 700;
          font-size: 0.7rem;
          letter-spacing: 0.08em;
          text-transform: uppercase;
          background-color: ${style.bg};
          border: 1px solid ${style.border};
          color: ${style.color};
          line-height: 1;
        }

        .badge-dot {
          width: 5px;
          height: 5px;
          border-radius: 50%;
        }
      `}</style>
    </span>
  );
}
