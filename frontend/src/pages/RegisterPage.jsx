import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { 
  Shield, 
  Lock, 
  UserCheck, 
  Upload, 
  FileText, 
  CheckCircle2, 
  AlertTriangle, 
  ArrowRight, 
  ArrowLeft, 
  Building2, 
  Eye, 
  EyeOff, 
  ShieldCheck, 
  Sparkles,
  Info,
  Clock
} from 'lucide-react';
import { getPoliceStations, registerOfficer } from '../services/api';

export default function RegisterPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  // Form Steps: 1 = Account Details, 2 = Upload Police ID, 3 = Verification / Review, 4 = Success
  const [currentStep, setCurrentStep] = useState(1);

  // Form Fields
  const [fullName, setFullName] = useState('');
  const [policeId, setPoliceId] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [stationId, setStationId] = useState('');
  const [role, setRole] = useState('OFFICER');
  const [idDocument, setIdDocument] = useState(null);
  const [idDocPreview, setIdDocPreview] = useState(null);

  // Stations List
  const [stations, setStations] = useState([]);
  const [stationsLoading, setStationsLoading] = useState(true);
  const [stationsError, setStationsError] = useState('');

  // State
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [registrationResult, setRegistrationResult] = useState(null);

  const loadPoliceStations = async () => {
    setStationsLoading(true);
    setStationsError('');
    try {
      const data = await getPoliceStations();
      if (Array.isArray(data) && data.length > 0) {
        setStations(data);
      } else {
        setStations([]);
        setStationsError('No police stations found in the database.');
      }
    } catch (err) {
      console.error('Failed to load police stations:', err);
      setStationsError(err.message || 'Failed to connect to police stations API.');
    } finally {
      setStationsLoading(false);
    }
  };

  useEffect(() => {
    loadPoliceStations();
  }, []);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      processSelectedFile(file);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      processSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const processSelectedFile = (file) => {
    setErrorMessage('');
    const validExtensions = ['pdf', 'jpg', 'jpeg', 'png', 'webp'];
    const ext = file.name.split('.').pop().toLowerCase();

    if (!validExtensions.includes(ext)) {
      setErrorMessage('Invalid file format. Please upload a PDF, JPG, PNG, or WEBP ID document.');
      return;
    }

    if (file.size > 20 * 1024 * 1024) {
      setErrorMessage('File size exceeds 20 MB limit.');
      return;
    }

    setIdDocument(file);

    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = () => {
        setIdDocPreview(reader.result);
      };
      reader.readAsDataURL(file);
    } else {
      setIdDocPreview(null);
    }
  };

  const validateStep1 = () => {
    setErrorMessage('');
    if (!fullName.trim()) {
      setErrorMessage('Full name is required.');
      return false;
    }
    if (!policeId.trim()) {
      setErrorMessage('Police ID is required.');
      return false;
    }
    if (!email.trim() || !email.includes('@')) {
      setErrorMessage('Valid official email address is required.');
      return false;
    }
    if (!password || password.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return false;
    }
    if (password !== confirmPassword) {
      setErrorMessage('Passwords do not match.');
      return false;
    }
    if (!stationId) {
      setErrorMessage('Please select your assigned police station.');
      return false;
    }
    return true;
  };

  const validateStep2 = () => {
    setErrorMessage('');
    if (!idDocument) {
      setErrorMessage('Official Police ID document upload is mandatory for identity verification.');
      return false;
    }
    return true;
  };

  const handleNextStep = () => {
    if (currentStep === 1) {
      if (validateStep1()) {
        setCurrentStep(2);
      }
    } else if (currentStep === 2) {
      if (validateStep2()) {
        setCurrentStep(3);
      }
    }
  };

  const handlePrevStep = () => {
    setErrorMessage('');
    setCurrentStep((prev) => Math.max(1, prev - 1));
  };

  const handleRegistrationSubmit = async (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');
    setSubmitting(true);

    try {
      const formData = new FormData();
      const payload = {
        fullName: fullName.trim(),
        policeId: policeId.trim().toUpperCase(),
        email: email.trim().toLowerCase(),
        phone: phone.trim(),
        password: password,
        confirmPassword: confirmPassword,
        stationId: stationId,
        role: role,
      };

      formData.append('data', JSON.stringify(payload));
      formData.append('idDocument', idDocument);

      const result = await registerOfficer(formData);
      setRegistrationResult(result);
      setCurrentStep(4);
    } catch (err) {
      setErrorMessage(err.message || 'Registration failed. Please check your credentials and ID document.');
    } finally {
      setSubmitting(false);
    }
  };

  const selectedStationObj = stations.find((s) => s.id === stationId);

  return (
    <div className="cs-register-page forensic-grid scanline-overlay">
      <div className="register-container">
        {/* Brand Header */}
        <div className="register-header-badge">
          <Shield size={32} className="register-shield-icon" />
          <div className="register-brand-titles">
            <h1 className="register-main-title">
              CRIMESPHERE <span className="title-red">AI</span>
            </h1>
            <div className="register-sub-title">TRUSTED OFFICER ONBOARDING PORTAL</div>
          </div>
        </div>

        {/* Multi-Step Wizard Progress Bar */}
        <div className="wizard-stepper cs-panel">
          {[
            { num: 1, label: 'Account Details' },
            { num: 2, label: 'Upload Police ID' },
            { num: 3, label: 'Verification' },
            { num: 4, label: 'Approval Status' },
          ].map((s) => (
            <div
              key={s.num}
              className={`stepper-step ${currentStep === s.num ? 'active' : ''} ${
                currentStep > s.num ? 'completed' : ''
              }`}
            >
              <div className="step-circle">
                {currentStep > s.num ? <CheckCircle2 size={16} /> : s.num}
              </div>
              <div className="step-label">
                <span className="step-tag">STEP {s.num}</span>
                <span className="step-name">{s.label}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Main Panel Card */}
        <div className="register-card cs-panel cs-panel-accent">
          {errorMessage && (
            <div className="auth-alert error">
              <AlertTriangle size={18} className="alert-icon" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* ============================================================ */}
          {/* STEP 1: ACCOUNT DETAILS                                      */}
          {/* ============================================================ */}
          {currentStep === 1 && (
            <div className="step-content">
              <div className="step-header">
                <UserCheck size={20} className="step-icon" />
                <div>
                  <h2 className="step-title">STEP 1: OFFICER CREDENTIALS</h2>
                  <p className="step-desc">Enter your official police identity and station assignment.</p>
                </div>
              </div>

              <div className="form-grid-2">
                <div className="form-group">
                  <label className="form-label label-system">FULL NAME *</label>
                  <input
                    type="text"
                    placeholder="e.g. R. Kumar"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    className="cs-input"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label label-system">POLICE ID / BADGE NUMBER *</label>
                  <input
                    type="text"
                    placeholder="e.g. TN-POL-5001"
                    value={policeId}
                    onChange={(e) => setPoliceId(e.target.value)}
                    className="cs-input uppercase"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label label-system">OFFICIAL EMAIL ADDRESS *</label>
                  <input
                    type="email"
                    placeholder="officer@department.internal"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="cs-input"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label label-system">PHONE NUMBER</label>
                  <input
                    type="tel"
                    placeholder="e.g. 9876543210"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="cs-input"
                  />
                </div>

                <div className="form-group">
                  <label className="form-label label-system">PASSWORD *</label>
                  <div className="password-input-wrapper">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      placeholder="Min 6 characters"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="cs-input password-field"
                      required
                    />
                    <button
                      type="button"
                      className="pw-toggle-btn"
                      onClick={() => setShowPassword(!showPassword)}
                    >
                      {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label label-system">CONFIRM PASSWORD *</label>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Re-enter password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="cs-input"
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label label-system">POLICE STATION *</label>
                  <select
                    value={stationId}
                    onChange={(e) => {
                      setStationId(e.target.value);
                      if (errorMessage) setErrorMessage('');
                    }}
                    className="cs-input cs-select"
                    disabled={stationsLoading}
                    required
                  >
                    {stationsLoading ? (
                      <option value="">Loading police stations...</option>
                    ) : stationsError ? (
                      <option value="">Error: {stationsError}</option>
                    ) : stations.length === 0 ? (
                      <option value="">No police stations found</option>
                    ) : (
                      <>
                        <option value="">[ Select Police Station ▼ ]</option>
                        {stations.map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.stationName} ({s.stationCode})
                          </option>
                        ))}
                      </>
                    )}
                  </select>
                  {stationsError && (
                    <div style={{ marginTop: '6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '0.78rem', color: '#f87171' }}>
                      <span>{stationsError}</span>
                      <button
                        type="button"
                        onClick={loadPoliceStations}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: '#60a5fa',
                          cursor: 'pointer',
                          textDecoration: 'underline',
                          fontSize: '0.78rem',
                          fontWeight: 600,
                          padding: 0,
                          marginLeft: '8px'
                        }}
                      >
                        Retry
                      </button>
                    </div>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label label-system">ASSIGNED ROLE *</label>
                  <select
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    className="cs-input cs-select"
                    required
                  >
                    <option value="OFFICER">General Police Officer</option>
                    <option value="INVESTIGATOR">Investigating Officer (IO)</option>
                    <option value="INSPECTOR">Inspector of Police</option>
                    <option value="FORENSIC_EXPERT">Forensic Specialist</option>
                    <option value="STATION_HEAD">Station Head</option>
                  </select>
                </div>
              </div>

              <div className="form-actions">
                <Link to="/login" className="cs-btn-secondary">
                  <ArrowLeft size={16} />
                  <span>BACK TO LOGIN</span>
                </Link>
                <button type="button" onClick={handleNextStep} className="cs-btn-primary">
                  <span>PROCEED TO STEP 2</span>
                  <ArrowRight size={16} />
                </button>
              </div>
            </div>
          )}

          {/* ============================================================ */}
          {/* STEP 2: UPLOAD POLICE ID                                     */}
          {/* ============================================================ */}
          {currentStep === 2 && (
            <div className="step-content">
              <div className="step-header">
                <Upload size={20} className="step-icon" />
                <div>
                  <h2 className="step-title">STEP 2: UPLOAD POLICE ID DOCUMENT</h2>
                  <p className="step-desc">
                    Attach official Police Department ID Card or Service Credentials for automated OCR extraction.
                  </p>
                </div>
              </div>

              <div
                className={`dropzone-box ${idDocument ? 'has-file' : ''}`}
                onDragOver={(e) => e.preventDefault()}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current && fileInputRef.current.click()}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept=".pdf,.jpg,.jpeg,.png,.webp"
                  style={{ display: 'none' }}
                />

                {idDocument ? (
                  <div className="file-preview-card">
                    {idDocPreview ? (
                      <div className="id-image-preview-wrapper">
                        <img src={idDocPreview} alt="ID Document Preview" className="id-doc-thumbnail" />
                      </div>
                    ) : (
                      <FileText size={48} className="doc-icon-large" />
                    )}
                    <div className="file-meta">
                      <div className="file-name">{idDocument.name}</div>
                      <div className="file-details">
                        {(idDocument.size / 1024).toFixed(1)} KB • {idDocument.type || 'Official Document'}
                      </div>
                      <div className="file-status-tag">
                        <CheckCircle2 size={13} />
                        <span>DOCUMENT READY FOR SECURE ENCRYPTED STORAGE</span>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="dropzone-empty">
                    <Upload size={42} className="upload-cloud-icon" />
                    <div className="drop-title">DRAG & DROP OFFICIAL ID CARD HERE</div>
                    <div className="drop-subtitle">or click to browse local files</div>
                    <div className="drop-formats">Supported Formats: PDF, JPG, PNG, WEBP (Max 20 MB)</div>
                  </div>
                )}
              </div>

              <div className="security-notice-card">
                <ShieldCheck size={20} className="notice-icon" />
                <div className="notice-text">
                  <strong>SECURE DOCUMENT VAULT GUARANTEE:</strong> Uploaded ID credentials are encrypted and stored in a private restricted partition. Documents are accessible strictly by authorized Station Heads for verification.
                </div>
              </div>

              <div className="form-actions">
                <button type="button" onClick={handlePrevStep} className="cs-btn-secondary">
                  <ArrowLeft size={16} />
                  <span>PREVIOUS STEP</span>
                </button>
                <button type="button" onClick={handleNextStep} className="cs-btn-primary">
                  <span>PROCEED TO VERIFICATION</span>
                  <ArrowRight size={16} />
                </button>
              </div>
            </div>
          )}

          {/* ============================================================ */}
          {/* STEP 3: VERIFICATION & REVIEW                                */}
          {/* ============================================================ */}
          {currentStep === 3 && (
            <div className="step-content">
              <div className="step-header">
                <Sparkles size={20} className="step-icon" />
                <div>
                  <h2 className="step-title">STEP 3: REGISTRATION VERIFICATION</h2>
                  <p className="step-desc">
                    Review your application summary before submission to the Station Head.
                  </p>
                </div>
              </div>

              <div className="review-summary-grid">
                <div className="summary-item">
                  <span className="summary-label">OFFICER NAME:</span>
                  <span className="summary-value">{fullName}</span>
                </div>

                <div className="summary-item">
                  <span className="summary-label">POLICE ID:</span>
                  <span className="summary-value badge-pid">{policeId.toUpperCase()}</span>
                </div>

                <div className="summary-item">
                  <span className="summary-label">EMAIL ADDRESS:</span>
                  <span className="summary-value">{email}</span>
                </div>

                <div className="summary-item">
                  <span className="summary-label">PHONE:</span>
                  <span className="summary-value">{phone || 'N/A'}</span>
                </div>

                <div className="summary-item">
                  <span className="summary-label">ASSIGNED STATION:</span>
                  <span className="summary-value">
                    {selectedStationObj ? `${selectedStationObj.stationName} (${selectedStationObj.stationCode})` : 'Selected Station'}
                  </span>
                </div>

                <div className="summary-item">
                  <span className="summary-label">REQUESTED ROLE:</span>
                  <span className="summary-value">{role}</span>
                </div>

                <div className="summary-item full-width">
                  <span className="summary-label">ID DOCUMENT ATTACHED:</span>
                  <span className="summary-value text-green">
                    <CheckCircle2 size={15} style={{ display: 'inline', marginRight: 6, verticalAlign: 'middle' }} />
                    {idDocument ? idDocument.name : 'Attached'}
                  </span>
                </div>
              </div>

              <div className="verification-lifecycle-notice">
                <Info size={20} className="info-icon" />
                <div className="info-body">
                  <strong>AUTOMATED OCR EXTRACTION & STATION HEAD REVIEW</strong>
                  <p>
                    Upon submission, our backend OCR engine will inspect your ID document for identity verification. Your account will enter <span className="highlight">PENDING_STATION_APPROVAL</span> status and an automated alert will be dispatched to your Station Head for authorization.
                  </p>
                </div>
              </div>

              <div className="form-actions">
                <button type="button" onClick={handlePrevStep} className="cs-btn-secondary" disabled={submitting}>
                  <ArrowLeft size={16} />
                  <span>PREVIOUS STEP</span>
                </button>
                <button
                  type="button"
                  onClick={handleRegistrationSubmit}
                  className={`cs-btn-primary ${submitting ? 'loading' : ''}`}
                  disabled={submitting}
                >
                  <span>{submitting ? 'PROCESSING OCR & SUBMITTING...' : 'SUBMIT REGISTRATION'}</span>
                  <ArrowRight size={16} />
                </button>
              </div>
            </div>
          )}

          {/* ============================================================ */}
          {/* STEP 4: STATION HEAD APPROVAL CONFIRMATION                   */}
          {/* ============================================================ */}
          {currentStep === 4 && (
            <div className="step-content success-step">
              <div className="success-icon-wrapper">
                <Clock size={48} className="clock-pending-icon" />
              </div>

              <h2 className="success-main-title">REGISTRATION SUBMITTED SUCCESSFULLY</h2>
              <div className="success-status-pill">STATUS: PENDING STATION HEAD APPROVAL</div>

              <p className="success-description">
                Your registration application and ID credentials have been securely registered and processed by our OCR verification engine.
              </p>

              <div className="success-result-card cs-panel">
                <div className="res-row">
                  <span className="res-k">OFFICER:</span>
                  <span className="res-v">{registrationResult?.fullName || fullName}</span>
                </div>
                <div className="res-row">
                  <span className="res-k">POLICE ID:</span>
                  <span className="res-v">{registrationResult?.policeId || policeId}</span>
                </div>
                <div className="res-row">
                  <span className="res-k">STATION:</span>
                  <span className="res-v">{registrationResult?.stationName || selectedStationObj?.stationName}</span>
                </div>
                <div className="res-row">
                  <span className="res-k">OCR VERIFICATION:</span>
                  <span className={`res-v ${registrationResult?.ocrStatus === 'PASSED' ? 'ocr-pass' : 'ocr-review'}`}>
                    {registrationResult?.ocrStatus === 'PASSED' ? 'PASSED (ALL FIELDS MATCHED)' : 'PENDING STATION HEAD REVIEW'}
                  </span>
                </div>
                <div className="res-row">
                  <span className="res-k">ACCOUNT ACTIVATION:</span>
                  <span className="res-v text-amber">REQUIRED — Awaiting Station Head verification</span>
                </div>
              </div>

              <div className="next-steps-card">
                <h4 className="next-steps-title">WHAT HAPPENS NEXT?</h4>
                <ol className="next-steps-list">
                  <li>Your Station Head has received an email and system notification to review your credentials.</li>
                  <li>Once reviewed and approved, your account will be activated.</li>
                  <li>You will receive an email confirmation and can then log into the CrimeSphere AI command center.</li>
                </ol>
              </div>

              <div className="form-actions center-actions">
                <button
                  type="button"
                  onClick={() => navigate('/login')}
                  className="cs-btn-primary full-width"
                >
                  <ArrowLeft size={16} />
                  <span>RETURN TO LOGIN PORTAL</span>
                </button>
              </div>
            </div>
          )}

          {/* Footer Security Notice */}
          <div className="register-card-footer">
            <div className="sec-notice-row">
              <Lock size={13} className="sec-icon" />
              <span>STATION HEAD AUTHORIZATION MANDATORY • LAW ENFORCEMENT INTEGRITY PROTOCOL</span>
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .cs-register-page {
          min-height: 100vh;
          width: 100vw;
          display: flex;
          align-items: center;
          justify-content: center;
          background: 
            radial-gradient(circle at 50% 30%, rgba(193, 18, 31, 0.12) 0%, transparent 60%),
            radial-gradient(circle at 80% 80%, rgba(139, 0, 0, 0.08) 0%, transparent 40%),
            var(--bg-primary);
          padding: 30px 20px;
          position: relative;
        }

        .register-container {
          width: 100%;
          max-width: 680px;
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 20px;
          z-index: 10;
        }

        .register-header-badge {
          display: flex;
          align-items: center;
          gap: 14px;
        }

        .register-shield-icon {
          color: var(--bright-crime-red);
          filter: drop-shadow(0 0 10px rgba(193, 18, 31, 0.5));
        }

        .register-brand-titles {
          text-align: left;
        }

        .register-main-title {
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

        .register-sub-title {
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          letter-spacing: 0.22em;
          color: var(--text-secondary);
          margin-top: 4px;
        }

        .wizard-stepper {
          width: 100%;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 12px 16px;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          border-radius: 6px;
        }

        .stepper-step {
          display: flex;
          align-items: center;
          gap: 10px;
          opacity: 0.5;
          transition: all var(--transition-fast);
        }

        .stepper-step.active {
          opacity: 1;
        }

        .stepper-step.completed {
          opacity: 0.9;
        }

        .step-circle {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          display: flex;
          align-items: center;
          justify-content: center;
          font-family: var(--font-subheading);
          font-size: 0.8rem;
          font-weight: 700;
          color: var(--text-muted);
        }

        .stepper-step.active .step-circle {
          background-color: var(--bright-crime-red);
          border-color: var(--bright-crime-red);
          color: white;
          box-shadow: 0 0 10px rgba(193, 18, 31, 0.5);
        }

        .stepper-step.completed .step-circle {
          background-color: rgba(40, 167, 69, 0.2);
          border-color: #28a745;
          color: #28a745;
        }

        .step-label {
          display: flex;
          flex-direction: column;
          text-align: left;
        }

        .step-tag {
          font-family: var(--font-mono);
          font-size: 0.6rem;
          color: var(--text-muted);
        }

        .step-name {
          font-family: var(--font-subheading);
          font-size: 0.75rem;
          font-weight: 600;
          color: var(--text-primary);
        }

        .register-card {
          width: 100%;
          background-color: var(--bg-panel);
          border: 1px solid var(--border-color);
          box-shadow: 0 12px 40px rgba(0, 0, 0, 0.8), var(--red-glow-subtle);
          border-radius: 6px;
          overflow: hidden;
        }

        .step-content {
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 20px;
        }

        .step-header {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding-bottom: 14px;
          border-bottom: 1px solid var(--border-color);
        }

        .step-icon {
          color: var(--bright-crime-red);
          margin-top: 2px;
        }

        .step-title {
          font-family: var(--font-heading);
          font-size: 1.1rem;
          color: var(--text-bright);
          margin: 0;
          letter-spacing: 0.05em;
        }

        .step-desc {
          font-family: var(--font-body);
          font-size: 0.8rem;
          color: var(--text-secondary);
          margin: 4px 0 0 0;
        }

        .form-grid-2 {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 16px;
        }

        @media (max-width: 600px) {
          .form-grid-2 {
            grid-template-columns: 1fr;
          }
          .wizard-stepper {
            display: none;
          }
        }

        .form-group {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }

        .form-label {
          font-family: var(--font-subheading);
          font-size: 0.72rem;
          font-weight: 700;
          color: var(--text-secondary);
          letter-spacing: 0.08em;
        }

        .cs-input {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 10px 12px;
          color: var(--text-bright);
          font-family: var(--font-body);
          font-size: 0.85rem;
          outline: none;
          transition: all var(--transition-fast);
        }

        .cs-input.uppercase {
          text-transform: uppercase;
        }

        .cs-input:focus {
          border-color: var(--bright-crime-red);
          box-shadow: 0 0 8px rgba(193, 18, 31, 0.3);
        }

        .cs-select {
          cursor: pointer;
        }

        .password-input-wrapper {
          position: relative;
          display: flex;
          align-items: center;
        }

        .password-field {
          padding-right: 36px;
        }

        .pw-toggle-btn {
          position: absolute;
          right: 8px;
          background: transparent;
          border: none;
          color: var(--text-muted);
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .pw-toggle-btn:hover {
          color: var(--text-primary);
        }

        .dropzone-box {
          border: 2px dashed var(--border-color);
          border-radius: 6px;
          padding: 28px;
          text-align: center;
          background-color: rgba(18, 18, 20, 0.6);
          cursor: pointer;
          transition: all var(--transition-fast);
        }

        .dropzone-box:hover {
          border-color: var(--bright-crime-red);
          background-color: rgba(193, 18, 31, 0.04);
        }

        .dropzone-box.has-file {
          border-style: solid;
          border-color: rgba(40, 167, 69, 0.5);
          background-color: rgba(40, 167, 69, 0.05);
        }

        .dropzone-empty {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 8px;
        }

        .upload-cloud-icon {
          color: var(--bright-crime-red);
        }

        .drop-title {
          font-family: var(--font-heading);
          font-size: 0.95rem;
          color: var(--text-bright);
        }

        .drop-subtitle {
          font-size: 0.8rem;
          color: var(--text-muted);
        }

        .drop-formats {
          font-family: var(--font-mono);
          font-size: 0.7rem;
          color: var(--text-secondary);
          margin-top: 6px;
        }

        .file-preview-card {
          display: flex;
          align-items: center;
          gap: 16px;
          text-align: left;
        }

        .id-image-preview-wrapper {
          width: 80px;
          height: 60px;
          border-radius: 4px;
          overflow: hidden;
          border: 1px solid var(--border-color);
        }

        .id-doc-thumbnail {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .doc-icon-large {
          color: #28a745;
        }

        .file-meta {
          flex: 1;
        }

        .file-name {
          font-weight: 600;
          color: var(--text-bright);
          font-size: 0.9rem;
        }

        .file-details {
          font-size: 0.75rem;
          color: var(--text-muted);
          margin-top: 2px;
        }

        .file-status-tag {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.7rem;
          color: #28a745;
          margin-top: 6px;
        }

        .security-notice-card {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          background-color: rgba(30, 41, 59, 0.5);
          border: 1px solid rgba(59, 130, 246, 0.3);
          padding: 12px 14px;
          border-radius: 4px;
          font-size: 0.78rem;
          color: #93c5fd;
        }

        .notice-icon {
          color: #3b82f6;
          flex-shrink: 0;
          margin-top: 2px;
        }

        .review-summary-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
          background-color: var(--bg-primary);
          padding: 16px;
          border-radius: 4px;
          border: 1px solid var(--border-color);
        }

        .summary-item {
          display: flex;
          flex-direction: column;
          gap: 3px;
        }

        .summary-item.full-width {
          grid-column: span 2;
          border-top: 1px solid var(--border-color);
          padding-top: 8px;
        }

        .summary-label {
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          font-weight: 700;
          color: var(--text-muted);
          letter-spacing: 0.05em;
        }

        .summary-value {
          font-size: 0.88rem;
          font-weight: 600;
          color: var(--text-bright);
        }

        .badge-pid {
          color: var(--bright-crime-red);
          font-family: var(--font-mono);
        }

        .text-green {
          color: #28a745;
        }

        .text-amber {
          color: #f59e0b;
        }

        .verification-lifecycle-notice {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          background-color: rgba(245, 158, 11, 0.1);
          border: 1px solid rgba(245, 158, 11, 0.3);
          padding: 14px;
          border-radius: 4px;
          font-size: 0.78rem;
          color: #fde68a;
        }

        .info-icon {
          color: #f59e0b;
          flex-shrink: 0;
          margin-top: 2px;
        }

        .info-body p {
          margin: 4px 0 0 0;
          line-height: 1.4;
          color: #fef08a;
        }

        .info-body .highlight {
          color: var(--bright-crime-red);
          font-weight: 700;
        }

        .success-step {
          text-align: center;
          align-items: center;
        }

        .success-icon-wrapper {
          width: 80px;
          height: 80px;
          border-radius: 50%;
          background-color: rgba(245, 158, 11, 0.15);
          border: 2px solid #f59e0b;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-top: 10px;
        }

        .clock-pending-icon {
          color: #f59e0b;
        }

        .success-main-title {
          font-family: var(--font-heading);
          font-size: 1.3rem;
          color: var(--text-bright);
          margin: 0;
        }

        .success-status-pill {
          display: inline-block;
          background-color: rgba(245, 158, 11, 0.2);
          border: 1px solid #f59e0b;
          color: #fde68a;
          padding: 4px 12px;
          border-radius: 12px;
          font-family: var(--font-mono);
          font-size: 0.75rem;
          font-weight: 700;
        }

        .success-description {
          font-size: 0.85rem;
          color: var(--text-secondary);
          max-width: 500px;
          margin: 0;
          line-height: 1.5;
        }

        .success-result-card {
          width: 100%;
          background-color: var(--bg-primary);
          border: 1px solid var(--border-color);
          padding: 16px;
          border-radius: 4px;
          display: flex;
          flex-direction: column;
          gap: 8px;
          text-align: left;
        }

        .res-row {
          display: flex;
          justify-content: space-between;
          font-size: 0.82rem;
          border-bottom: 1px solid rgba(42, 42, 45, 0.5);
          padding-bottom: 6px;
        }

        .res-k {
          font-family: var(--font-subheading);
          color: var(--text-muted);
          font-weight: 700;
        }

        .res-v {
          color: var(--text-bright);
          font-weight: 600;
        }

        .ocr-pass {
          color: #28a745;
        }

        .ocr-review {
          color: #f59e0b;
        }

        .next-steps-card {
          width: 100%;
          background-color: rgba(15, 23, 42, 0.5);
          border: 1px solid var(--border-color);
          padding: 16px;
          border-radius: 4px;
          text-align: left;
        }

        .next-steps-title {
          font-family: var(--font-subheading);
          font-size: 0.78rem;
          color: var(--text-primary);
          margin: 0 0 8px 0;
          letter-spacing: 0.08em;
        }

        .next-steps-list {
          margin: 0;
          padding-left: 18px;
          font-size: 0.8rem;
          color: var(--text-secondary);
          line-height: 1.6;
        }

        .form-actions {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12px;
          margin-top: 10px;
        }

        .form-actions.center-actions {
          justify-content: center;
          width: 100%;
        }

        .cs-btn-primary {
          background: linear-gradient(180deg, var(--bright-crime-red) 0%, var(--primary-crimson) 100%);
          border: 1px solid var(--bright-crime-red);
          border-radius: 4px;
          padding: 10px 18px;
          color: white;
          font-family: var(--font-heading);
          font-size: 0.9rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 8px;
          transition: all var(--transition-fast);
          box-shadow: var(--red-glow-subtle);
        }

        .cs-btn-primary:hover:not(:disabled) {
          box-shadow: var(--red-glow-medium);
          transform: translateY(-1px);
        }

        .cs-btn-primary:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }

        .cs-btn-secondary {
          background-color: transparent;
          border: 1px solid var(--border-color);
          border-radius: 4px;
          padding: 10px 16px;
          color: var(--text-secondary);
          font-family: var(--font-heading);
          font-size: 0.85rem;
          font-weight: 700;
          letter-spacing: 0.08em;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 8px;
          text-decoration: none;
          transition: all var(--transition-fast);
        }

        .cs-btn-secondary:hover:not(:disabled) {
          background-color: var(--bg-primary);
          color: var(--text-bright);
          border-color: rgba(193, 18, 31, 0.4);
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
          margin: 16px 24px 0 24px;
        }

        .auth-alert.error {
          background-color: rgba(193, 18, 31, 0.15);
          border: 1px solid var(--bright-crime-red);
          color: #ff858d;
        }

        .register-card-footer {
          padding: 12px 20px;
          background-color: var(--bg-primary);
          border-top: 1px solid var(--border-color);
          display: flex;
          justify-content: center;
        }

        .sec-notice-row {
          display: flex;
          align-items: center;
          gap: 6px;
          font-family: var(--font-subheading);
          font-size: 0.68rem;
          color: var(--text-muted);
          letter-spacing: 0.08em;
        }

        .sec-icon {
          color: var(--bright-crime-red);
        }
      `}</style>
    </div>
  );
}
