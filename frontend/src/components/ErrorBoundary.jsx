import React from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Unhandled React Error Boundary caught:', error);
    console.error(error?.stack);
    console.error('Component Stack:', errorInfo?.componentStack);
  }

  handleReload = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    window.location.href = '/dashboard';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '100vh',
          backgroundColor: '#080808',
          color: '#F5F5F5',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '24px',
          fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
        }}>
          <div style={{
            maxWidth: '520px',
            width: '100%',
            backgroundColor: '#121214',
            border: '1px solid #2A2A2D',
            borderRadius: '8px',
            padding: '32px',
            textAlign: 'center',
            boxShadow: '0 10px 25px rgba(0, 0, 0, 0.5)'
          }}>
            <div style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: 'rgba(193, 18, 31, 0.15)',
              border: '1px solid rgba(193, 18, 31, 0.4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px',
              color: '#E63946'
            }}>
              <AlertTriangle size={24} />
            </div>

            <h2 style={{
              fontSize: '1.25rem',
              fontWeight: 700,
              letterSpacing: '0.05em',
              marginBottom: '8px',
              color: '#FFFFFF'
            }}>
              APPLICATION INTERFACE ERROR
            </h2>

            <p style={{
              fontSize: '0.85rem',
              color: '#A7A7AA',
              lineHeight: 1.5,
              marginBottom: '20px'
            }}>
              An unexpected component rendering error occurred. The system safely intercepted this exception to protect your session context.
            </p>

            {this.state.error?.message && (
              <div style={{
                backgroundColor: '#0A0A0C',
                border: '1px solid #2A2A2D',
                borderRadius: '4px',
                padding: '10px 14px',
                fontSize: '0.75rem',
                fontFamily: 'monospace',
                color: '#E63946',
                marginBottom: '24px',
                textAlign: 'left',
                wordBreak: 'break-word'
              }}>
                {this.state.error.message}
              </div>
            )}

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={this.handleReload}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  backgroundColor: '#C1121F',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '4px',
                  padding: '10px 18px',
                  fontWeight: 600,
                  fontSize: '0.82rem',
                  cursor: 'pointer'
                }}
              >
                <RefreshCw size={14} />
                <span>Reload Page</span>
              </button>

              <button
                onClick={this.handleGoHome}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px',
                  backgroundColor: '#1E1E22',
                  color: '#F5F5F5',
                  border: '1px solid #2A2A2D',
                  borderRadius: '4px',
                  padding: '10px 18px',
                  fontWeight: 600,
                  fontSize: '0.82rem',
                  cursor: 'pointer'
                }}
              >
                <Home size={14} />
                <span>Dashboard</span>
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
