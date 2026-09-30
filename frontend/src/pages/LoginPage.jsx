import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Shield, Lock, UserCheck, AlertTriangle, ArrowRight, CheckCircle2, KeyRound, RefreshCw, ArrowLeft, Mail } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { resendLoginOtp } from '../services/api';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { initiateLogin, completeOtpVerification, isAuthenticated } = useAuth();

  // Phase: 'CREDENTIALS' | 'OTP'
  const [phase, setPhase] = useState('CREDENTIALS');

  // Phase 1 States
  const [policeId, setPoliceId] = useState('TN-POL-1001');
  const [password, setPassword] = useState('Demo@123');

  // Phase 2 States
  const [challengeId, setChallengeId] = useState(null);
  const [maskedEmail, setMaskedEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [timeLeft, setTimeLeft] = useState(300); // 5 minutes in seconds
  const [resending, setResending] = useState(false);
  const [resendSuccess, setResendSuccess] = useState('');

  // UI States
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
    const params = new URLSearchParams(location.search);
    if (params.get('expired') === 'true') {
      setErrorMessage('Your session has expired. Please log in again.');
    }
  }, [isAuthenticated, navigate, location]);

  // Countdown timer for OTP
  useEffect(() => {
    if (phase !== 'OTP' || timeLeft <= 0) return;

    const timer = setInterval(() => {
      setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => clearInterval(timer);
  }, [phase, timeLeft]);

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // Phase 1: Submit Credentials
  const handleCredentialsSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setResendSuccess('');
    setLoading(true);

    try {
      const response = await initiateLogin(policeId.trim(), password);
      if (response && response.requiresOtp) {
        setChallengeId(response.challengeId);
        setMaskedEmail(response.maskedEmail || 'registered email');
        setPhase('OTP');
        setTimeLeft(300);
        setOtp('');
      } else {
        setErrorMessage('Unexpected login response. Please try again.');
      }
    } catch (err) {
      setErrorMessage(err.message || 'Invalid Police ID or password');
    } finally {
      setLoading(false);
    }
  };

  // Phase 2: Verify OTP
  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    if (!otp || otp.trim().length !== 6) {
      setErrorMessage('Please enter the 6-digit verification code.');
      return;
    }

    setErrorMessage('');
    setResendSuccess('');
    setLoading(true);

    try {
      await completeOtpVerification(challengeId, otp.trim());
      setIsSuccess(true);
      setTimeout(() => {
        const from = location.state?.from?.pathname || '/dashboard';
        navigate(from, { replace: true });
      }, 700);
    } catch (err) {
      setErrorMessage(err.message || 'Invalid verification code.');
      setLoading(false);
    }
  };

  // Phase 2: Resend OTP
  const handleResendOtp = async () => {
    if (!challengeId || resending) return;
    setResending(true);
    setErrorMessage('');
    setResendSuccess('');

    try {
      const response = await resendLoginOtp(challengeId);
      if (response && response.challengeId) {
        setChallengeId(response.challengeId);
        setMaskedEmail(response.maskedEmail || maskedEmail);
        setTimeLeft(300);
        setOtp('');
        setResendSuccess('New verification code dispatched to ' + (response.maskedEmail || maskedEmail));
      }
    } catch (err) {
      setErrorMessage(err.message || 'Failed to resend verification code.');
    } finally {
      setResending(false);
    }
  };

  const handleBackToCredentials = () => {
    setPhase('CREDENTIALS');
    setOtp('');
    setErrorMessage('');
    setResendSuccess('');
  };

  return (
    <div className="cs-login-page forensic-grid scanline-overlay">
      <div className="login-container">
        {/* Top Header Badge */}
        <div className="login-header-badge">
          <Shield size={28} className="login-shield-icon" />
          <div className="login-brand-titles">
            <h1 className="login-main-title">CRIMESPHERE <span className="title-red">AI</span></h1>
            <div className="login-sub-title">CRIME INTELLIGENCE SYSTEM</div>
          </div>
        </div>

        {/* Login Panel Card */}
        <div className="login-card cs-panel cs-panel-accent">
          <div className="login-card-header">
            {phase === 'CREDENTIALS' ? (
              <>
                <UserCheck size={18} className="header-icon" />
                <span>AUTHORIZED POLICE ACCESS ONLY</span>
              </>
            ) : (
              <>
                <KeyRound size={18} className="header-icon" />
                <span>MULTI-FACTOR IDENTITY VERIFICATION</span>
              </>
            )}
          </div>

          {phase === 'CREDENTIALS' ? (
            /* ============================================================
               PHASE 1: CREDENTIALS (POLICE ID + PASSWORD)
               ============================================================ */
            <form onSubmit={handleCredentialsSubmit} className="login-form">
              {errorMessage && (
                <div className="auth-alert error">
                  <AlertTriangle size={16} className="alert-icon" />
                  <span>{errorMessage}</span>
                </div>
              )}

              <div className="form-group">
                <label className="form-label label-system">POLICE ID</label>
                <div className="input-wrapper">
                  <input 
                    type="text" 
                    placeholder="Enter Police ID (e.g. TN-POL-1001)"
                    value={policeId}
                    onChange={(e) => setPoliceId(e.target.value)}
                    className="login-input"
                    disabled={loading}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label label-system">PASSWORD</label>
                <div className="input-wrapper">
                  <input 
                    type="password" 
                    placeholder="Enter Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="login-input"
                    disabled={loading}
                    required
                  />
                </div>
              </div>

              <button 
                type="submit" 
                className={`login-submit-btn ${loading ? 'loading' : ''}`}
                disabled={loading}
              >
                <span>{loading ? 'VALIDATING CREDENTIALS...' : 'PROCEED TO MFA VERIFICATION'}</span>
                <ArrowRight size={16} />
              </button>

              <div className="login-register-prompt">
                <span>New Personnel without credentials?</span>
                <button 
                  type="button" 
                  onClick={() => navigate('/register')}
                  className="login-register-link"
                >
                  Register Police ID & Apply for Clearance
                </button>
              </div>
            </form>
          ) : (
            /* ============================================================
               PHASE 2: EMAIL OTP VERIFICATION
               ============================================================ */
            <form onSubmit={handleOtpSubmit} className="login-form">
              <div className="otp-info-banner">
                <Mail size={16} className="mail-icon" />
                <div className="otp-info-text">
                  <span>A 6-digit verification code has been dispatched to:</span>
                  <strong className="masked-email">{maskedEmail}</strong>
                </div>
              </div>

              {errorMessage && (
                <div className="auth-alert error">
                  <AlertTriangle size={16} className="alert-icon" />
                  <span>{errorMessage}</span>
                </div>
              )}

              {resendSuccess && (
                <div className="auth-alert success">
                  <CheckCircle2 size={16} className="alert-icon" />
                  <span>{resendSuccess}</span>
                </div>
              )}

              {isSuccess && (
                <div className="auth-alert success">
                  <CheckCircle2 size={16} className="alert-icon" />
                  <span>MFA VERIFIED — ACCESS GRANTED</span>
                </div>
              )}

              <div className="form-group">
                <div className="otp-label-row">
                  <label className="form-label label-system">ENTER 6-DIGIT OTP</label>
                  <span className={`otp-timer ${timeLeft <= 60 ? 'timer-urgent' : ''}`}>
                    Expires in {formatTimer(timeLeft)}
                  </span>
                </div>

                <div className="input-wrapper">
                  <input 
                    type="text" 
                    maxLength={6}
                    placeholder="• • • • • •"
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, ''))}
                    className="login-input otp-code-input"
                    disabled={loading || isSuccess || timeLeft <= 0}
                    autoFocus
                    required
                  />
                </div>
              </div>

              <button 
                type="submit" 
                className={`login-submit-btn ${loading ? 'loading' : ''} ${isSuccess ? 'success' : ''}`}
                disabled={loading || isSuccess || otp.length !== 6 || timeLeft <= 0}
              >
                <span>{loading ? 'VERIFYING SECURITY CODE...' : isSuccess ? 'ACCESS VERIFIED' : 'VERIFY OTP & LOGIN'}</span>
                <ArrowRight size={16} />
              </button>

              {/* Resend & Back actions */}
              <div className="otp-actions-row">
                <button
                  type="button"
                  onClick={handleResendOtp}
                  className="otp-resend-btn"
                  disabled={resending || loading || isSuccess}
                >
                  <RefreshCw size={13} className={resending ? 'spinning' : ''} />
                  <span>{resending ? 'Sending...' : 'Resend OTP'}</span>
                </button>

                <button
                  type="button"
                  onClick={handleBackToCredentials}
                  className="otp-back-btn"
                  disabled={loading || isSuccess}
                >
                  <ArrowLeft size={13} />
                  <span>Back to Login</span>
                </button>
              </div>
            </form>
          )}

          {/* Footer Security Notice */}
          <div className="login-card-footer">
            <div className="sec-notice-row">
              <Lock size={13} className="sec-icon" />
              <span>AUTHORIZED PERSONNEL ONLY — RESEND MFA PROTECTED</span>
            </div>
            <div className="phase-disclaimer">
              [ SPRING SECURITY 6 • BCRYPT OTP HASH • STATION ISOLATION ACTIVE ]
            </div>
          </div>
        </div>

        {/* System Watermark */}
        <div className="login-system-footer">
          LAW ENFORCEMENT DECISION-SUPPORT SYSTEM • DEPT OF POLICE INTELLIGENCE
        </div>
      </div>

      <style>{`
        .cs-login-page {
          min-height: 100vh;
          width: 100vw;
          display: flex;
          align-items: center;
          justify-content: center;
          background: 
            radial-gradient(circle at 50% 40%, rgba(193, 18, 31, 0.15) 0%, transparent 55%),
            radial-gradient(circle at 80% 80%, rgba(139, 0, 0, 0.08) 0%, transparent 40%),
            var(--bg-primary);
          padding: 20px;
          position: relative;
        }

        .login-container {
          width: 100%;
          max-width: 440px;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 24px;
          z-index: 10;
        }

        .login-header-badge {
          display: flex;
          align-items: center;
          gap: 14px;
        }

        .login-shield-icon {
          color: var(--bright-crime-red);
          filter: drop-shadow(0 0 10px rgba(193, 18, 31, 0.5));
        }

        .login-brand-titles {
          text-align: left;
        }

        .login-main-title {
          font-family: var(--font-heading);
          font-size: 1.8rem;
          font-weight: 700;
          letter-spacing: 0.1em;
          color: var(--text-bright);
          margin: 0;
          line-height: 1;
        }

        .title-red {
          color: var(--bright-crime-red);
        }

        .login-sub-title {
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          letter-spacing: 0.22em;
          color: var(--text-secondary);
          margin-top: 4px;
        }

        .login-card {
          width: 100%;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          box-shadow: 0 12px 40px rgba(0, 0, 0, 0.8), var(--red-glow-subtle);
        }

        .login-card-header {
          padding: 14px 20px;
          background-color: var(--bg-panel-elevated);
          border-bottom: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          gap: 10px;
          font-family: var(--font-subheading);
          font-size: 0.78rem;
          font-weight: 700;
          letter-spacing: 0.12em;
          color: var(--text-primary);
        }

        .header-icon {
          color: var(--bright-crime-red);
        }

        .login-form {
          padding: 24px 20px 20px 20px;
          display: flex;
          flex-direction: column;
          gap: 18px;
        }

        .otp-info-banner {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          background: rgba(193, 18, 31, 0.08);
          border: 1px solid rgba(193, 18, 31, 0.3);
          border-radius: 4px;
          padding: 12px 14px;
        }

        .mail-icon {
          color: var(--bright-crime-red);
          flex-shrink: 0;
          margin-top: 2px;
        }

        .otp-info-text {
          font-family: var(--font-subheading);
          font-size: 0.76rem;
          color: var(--text-secondary);
          display: flex;
          flex-direction: column;
          gap: 3px;
        }

        .masked-email {
          color: var(--text-bright);
          font-family: var(--font-mono);
          font-size: 0.82rem;
          letter-spacing: 0.05em;
        }

        .auth-alert {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 10px 14px;
          border-radius: 4px;
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 600;
          letter-spacing: 0.05em;
        }

        .auth-alert.error {
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          color: #ff858d;
        }

        .auth-alert.success {
          background-color: rgba(40, 167, 69, 0.15);
          border: 1px solid #28a745;
          color: #85e89d;
        }

        .alert-icon {
          flex-shrink: 0;
        }

        .form-group {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .otp-label-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .otp-timer {
          font-family: var(--font-mono);
          font-size: 0.75rem;
          color: var(--text-muted);
          font-weight: 600;
        }

        .timer-urgent {
          color: #ff4d5a;
          animation: pulse 1s infinite;
        }

        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }

        .form-label {
          color: var(--text-secondary);
        }

        .input-wrapper {
          position: relative;
        }

        .login-input {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 12px 14px;
          color: var(--text-bright);
          font-family: var(--font-body);
          font-size: 0.9rem;
          outline: none;
          transition: all var(--transition-fast);
        }

        .login-input:focus {
          border-color: var(--bright-crime-red);
          box-shadow: 0 0 10px rgba(193, 18, 31, 0.3);
        }

        .otp-code-input {
          font-family: var(--font-mono);
          font-size: 1.4rem;
          letter-spacing: 0.5em;
          text-align: center;
          font-weight: 800;
          color: #ff4d5a;
        }

        .login-submit-btn {
          margin-top: 6px;
          width: 100%;
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 12px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.95rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 10px;
          transition: all var(--transition-fast);
          box-shadow: var(--red-glow-subtle);
        }

        .login-submit-btn:hover:not(:disabled) {
          box-shadow: var(--red-glow-medium);
          transform: translateY(-1px);
        }

        .login-submit-btn:disabled {
          opacity: 0.75;
          cursor: not-allowed;
        }

        .otp-actions-row {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding-top: 6px;
        }

        .otp-resend-btn,
        .otp-back-btn {
          background: transparent;
          border: none;
          color: var(--text-secondary);
          font-family: var(--font-subheading);
          font-size: 0.76rem;
          font-weight: 600;
          display: flex;
          align-items: center;
          gap: 6px;
          cursor: pointer;
          transition: color var(--transition-fast);
          padding: 4px 6px;
        }

        .otp-resend-btn:hover:not(:disabled),
        .otp-back-btn:hover:not(:disabled) {
          color: var(--text-bright);
        }

        .otp-resend-btn:disabled,
        .otp-back-btn:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }

        .spinning {
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }

        .login-register-prompt {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 6px;
          padding-top: 8px;
          border-top: 1px dashed rgba(255, 255, 255, 0.1);
          font-family: var(--font-subheading);
          font-size: 0.76rem;
          color: var(--text-muted);
        }

        .login-register-link {
          background: transparent;
          border: none;
          color: var(--bright-crime-red);
          font-family: var(--font-subheading);
          font-size: 0.78rem;
          font-weight: 700;
          letter-spacing: 0.05em;
          text-decoration: underline;
          cursor: pointer;
          transition: color var(--transition-fast);
          padding: 2px 4px;
        }

        .login-register-link:hover {
          color: #ff4d5a;
          text-shadow: 0 0 8px rgba(193, 18, 31, 0.4);
        }

        .login-card-footer {
          padding: 14px 20px;
          background-color: var(--bg-primary);
          border-top: 1px solid var(--border-color);
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 6px;
          text-align: center;
        }

        .sec-notice-row {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 600;
          color: var(--text-secondary);
          letter-spacing: 0.08em;
        }

        .sec-icon {
          color: var(--bright-crime-red);
        }

        .phase-disclaimer {
          font-family: var(--font-mono);
          font-size: 0.65rem;
          color: var(--text-muted);
        }

        .login-system-footer {
          font-family: var(--font-subheading);
          font-size: 0.65rem;
          letter-spacing: 0.15em;
          color: var(--text-muted);
          text-align: center;
        }
      `}</style>
    </div>
  );
}
