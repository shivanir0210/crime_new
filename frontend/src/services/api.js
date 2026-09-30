/**
 * CrimeSphere AI - Centralized API Service Module
 * Handles JWT authentication headers, case management, FIRs, victims, suspects, dashboard statistics, and health checks.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const AI_SERVICE_BASE_URL = import.meta.env.VITE_AI_SERVICE_BASE_URL || 'http://localhost:8000/api';

/**
 * Helper to get active session token
 */
function getAuthToken() {
  return sessionStorage.getItem('crimesphere_token');
}

/**
 * Authenticated fetch helper that injects JWT Bearer token and handles 401 unauthorized errors
 */
async function authFetch(endpoint, options = {}) {
  const token = getAuthToken();
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    sessionStorage.removeItem('crimesphere_token');
    sessionStorage.removeItem('crimesphere_officer');
    if (!window.location.pathname.includes('/login')) {
      window.location.href = '/login?expired=true';
    }
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Session expired or unauthorized');
  }

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Request failed with status ${response.status}`);
  }

  return response.json();
}

/* ==========================================================================
   AUTHENTICATION ENDPOINTS
   ========================================================================== */

export async function loginPoliceOfficer(policeId, password) {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ policeId, password }),
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      throw new Error(data.message || 'Invalid Police ID or password');
    }

    return data;
  } catch (err) {
    if (err.name === 'TypeError' && err.message.includes('fetch')) {
      throw new Error('Unable to connect to authentication server. Please verify the backend is running.');
    }
    throw err;
  }
}

export async function verifyLoginOtp(challengeId, otp) {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/verify-otp`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ challengeId, otp }),
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      if (response.status === 401 && (!data.message || data.message === 'Authentication required')) {
        throw new Error('Authentication session error. Please log in again.');
      }
      throw new Error(data.message || 'Invalid verification code.');
    }

    return data;
  } catch (err) {
    if (err.name === 'TypeError' && err.message.includes('fetch')) {
      throw new Error('Unable to connect to authentication server. Please verify the backend is running.');
    }
    throw err;
  }
}

export async function resendLoginOtp(challengeId) {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/resend-otp`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ challengeId }),
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      throw new Error(data.message || 'Failed to resend verification code');
    }

    return data;
  } catch (err) {
    if (err.name === 'TypeError' && err.message.includes('fetch')) {
      throw new Error('Unable to connect to authentication server. Please verify the backend is running.');
    }
    throw err;
  }
}

export async function getCurrentOfficer() {
  return authFetch('/auth/me');
}

/* ==========================================================================
   DASHBOARD STATS ENDPOINTS
   ========================================================================== */

export async function getDashboardStats() {
  return authFetch('/dashboard/stats');
}

/* ==========================================================================
   CASE MANAGEMENT ENDPOINTS
   ========================================================================== */

export async function getCases({
  search = '',
  status = '',
  priority = '',
  caseType = '',
  page = 0,
  size = 10,
  sort = 'updatedAt,desc',
} = {}) {
  const params = new URLSearchParams();
  if (search) params.append('search', search);
  if (status && status !== 'ALL') params.append('status', status);
  if (priority && priority !== 'ALL') params.append('priority', priority);
  if (caseType && caseType !== 'ALL') params.append('caseType', caseType);
  params.append('page', page);
  params.append('size', size);
  params.append('sort', sort);

  return authFetch(`/cases?${params.toString()}`);
}

export async function getCaseById(id) {
  return authFetch(`/cases/${id}`);
}

export async function createCase(payload) {
  return authFetch('/cases', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateCase(id, payload) {
  return authFetch(`/cases/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function updateCaseStatus(id, status) {
  return authFetch(`/cases/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function updateCasePriority(id, priority) {
  return authFetch(`/cases/${id}/priority`, {
    method: 'PATCH',
    body: JSON.stringify({ priority }),
  });
}

/* ==========================================================================
   PHASE 5: FIR MANAGEMENT ENDPOINTS
   ========================================================================== */

export async function getCaseFIR(caseId) {
  return authFetch(`/cases/${caseId}/fir`);
}

export async function createCaseFIR(caseId, payload) {
  return authFetch(`/cases/${caseId}/fir`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateCaseFIR(caseId, payload) {
  return authFetch(`/cases/${caseId}/fir`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/* ==========================================================================
   PHASE 5: VICTIM MANAGEMENT ENDPOINTS
   ========================================================================== */

export async function getCaseVictims(caseId) {
  return authFetch(`/cases/${caseId}/victims`);
}

export async function getCaseVictimById(caseId, victimId) {
  return authFetch(`/cases/${caseId}/victims/${victimId}`);
}

export async function createCaseVictim(caseId, payload) {
  return authFetch(`/cases/${caseId}/victims`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateCaseVictim(caseId, victimId, payload) {
  return authFetch(`/cases/${caseId}/victims/${victimId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/* ==========================================================================
   PHASE 5: SUSPECT MANAGEMENT ENDPOINTS
   ========================================================================== */

export async function getCaseSuspects(caseId) {
  return authFetch(`/cases/${caseId}/suspects`);
}

export async function getCaseSuspectById(caseId, suspectId) {
  return authFetch(`/cases/${caseId}/suspects/${suspectId}`);
}

export async function createCaseSuspect(caseId, payload) {
  return authFetch(`/cases/${caseId}/suspects`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateCaseSuspect(caseId, suspectId, payload) {
  return authFetch(`/cases/${caseId}/suspects/${suspectId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/* ==========================================================================
   WITNESS MANAGEMENT ENDPOINTS
   ========================================================================== */

export async function getCaseWitnesses(caseId) {
  return authFetch(`/cases/${caseId}/witnesses`);
}

export async function getCaseWitnessById(caseId, witnessId) {
  return authFetch(`/cases/${caseId}/witnesses/${witnessId}`);
}

export async function createCaseWitness(caseId, payload) {
  return authFetch(`/cases/${caseId}/witnesses`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateCaseWitness(caseId, witnessId, payload) {
  return authFetch(`/cases/${caseId}/witnesses/${witnessId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteCaseWitness(caseId, witnessId) {
  return authFetch(`/cases/${caseId}/witnesses/${witnessId}`, {
    method: 'DELETE',
  });
}

/* ==========================================================================
   CASE TIMELINE ENDPOINTS
   ========================================================================== */

export async function getCaseTimeline(caseId) {
  return authFetch(`/cases/${caseId}/timeline`);
}

/* ==========================================================================
   PHASE 6: EVIDENCE MANAGEMENT & SECURE UPLOAD ENDPOINTS
   ========================================================================== */

export async function getCaseEvidence(caseId) {
  return authFetch(`/cases/${caseId}/evidence`);
}

export async function getEvidenceDetail(caseId, evidenceId) {
  return authFetch(`/cases/${caseId}/evidence/${evidenceId}`);
}

export async function uploadEvidence(caseId, formData, onProgress) {
  const token = getAuthToken();
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${API_BASE_URL}/cases/${caseId}/evidence`);
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }

    if (xhr.upload && onProgress) {
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          const percentComplete = Math.round((event.loaded / event.total) * 100);
          onProgress(percentComplete);
        }
      };
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const data = JSON.parse(xhr.responseText);
          resolve(data);
        } catch (e) {
          resolve({ status: 'SUCCESS' });
        }
      } else {
        try {
          const err = JSON.parse(xhr.responseText);
          const errorObj = new Error(err.message || `Upload failed with status ${xhr.status}`);
          errorObj.isCrossCaseDuplicate = !!err.isCrossCaseDuplicate;
          errorObj.caseNumber = err.caseNumber;
          errorObj.status = xhr.status;
          reject(errorObj);
        } catch (e) {
          reject(new Error(`Upload failed with status ${xhr.status}`));
        }
      }
    };

    xhr.onerror = () => {
      reject(new Error('Network error during evidence upload'));
    };

    xhr.send(formData);
  });
}

export async function updateEvidence(caseId, evidenceId, payload) {
  return authFetch(`/cases/${caseId}/evidence/${evidenceId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function updateEvidenceStatus(caseId, evidenceId, status, notes = '') {
  return authFetch(`/cases/${caseId}/evidence/${evidenceId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status, notes }),
  });
}

export async function downloadEvidenceFile(caseId, evidenceId, filename = 'evidence_file') {
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}/cases/${caseId}/evidence/${evidenceId}/download`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Failed to download evidence file');
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
  return true;
}

export async function getEvidenceAuditLogs(caseId, evidenceId) {
  return authFetch(`/cases/${caseId}/evidence/${evidenceId}/audit`);
}

/* ==========================================================================
   PHASE 7: ADAPTIVE CASE STATE & EVIDENCE GAP ANALYSIS ENDPOINTS
   ========================================================================== */

export async function getCaseState(caseId) {
  return authFetch(`/cases/${caseId}/state`);
}

export async function getEvidenceGaps(caseId) {
  return authFetch(`/cases/${caseId}/evidence-gaps`);
}

export async function analyzeEvidenceGaps(caseId) {
  return authFetch(`/cases/${caseId}/evidence-gaps/analyze`, {
    method: 'POST',
  });
}

export async function updateEvidenceGapStatus(caseId, gapId, status, reason = '') {
  return authFetch(`/cases/${caseId}/evidence-gaps/${gapId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status, reason }),
  });
}

/* ==========================================================================
   SYSTEM & HEALTH ENDPOINTS
   ========================================================================== */

export async function getBackendHealth() {
  try {
    const response = await fetch(`${API_BASE_URL}/health`);
    if (!response.ok) throw new Error(`Status: ${response.status}`);
    return await response.json();
  } catch (error) {
    return { status: 'DOWN', service: 'CrimeSphere Backend', error: error.message };
  }
}

export async function getDatabaseHealth() {
  try {
    const response = await fetch(`${API_BASE_URL}/db/health`);
    if (!response.ok) throw new Error(`Status: ${response.status}`);
    return await response.json();
  } catch (error) {
    return { status: 'DOWN', database: 'disconnected', error: error.message };
  }
}

export async function getPoliceStations() {
  const response = await fetch(`${API_BASE_URL}/police-stations`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to fetch police stations (HTTP ${response.status})`);
  }

  const data = await response.json();
  if (!Array.isArray(data)) {
    throw new Error('Invalid data format received from police stations API');
  }

  return data;
}

export async function getAIServiceHealth() {
  try {
    const aiHealthUrl = AI_SERVICE_BASE_URL.endsWith('/api')
      ? `${AI_SERVICE_BASE_URL}/health`
      : `${AI_SERVICE_BASE_URL}/api/health`;

    const response = await fetch(aiHealthUrl, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    }).catch(() => null);

    if (response && response.ok) {
      return await response.json();
    }

    // Fallback to Spring Boot's internal AI health check if direct connection fails
    const backendAi = await getBackendAiHealth().catch(() => null);
    if (backendAi && backendAi.status) {
      return backendAi;
    }

    return {
      status: 'DOWN',
      service: 'CrimeSphere AI Service',
      error: 'AI Microservice offline'
    };
  } catch (error) {
    return {
      status: 'DOWN',
      service: 'CrimeSphere AI Service',
      error: error?.message || 'AI Microservice offline'
    };
  }
}

export async function getBackendAiHealth() {
  return authFetch('/ai/health');
}

export async function sendCaseToAIService(caseId) {
  return authFetch(`/cases/${caseId}/ai/analyze`, {
    method: 'POST',
  });
}

export async function getCaseAiHistory(caseId) {
  return authFetch(`/cases/${caseId}/ai/history`);
}

export async function getCaseAiAnalysisDetail(caseId, analysisId) {
  return authFetch(`/cases/${caseId}/ai/analysis/${analysisId}`);
}

/* ==========================================================================
   PHASE 10: RETRIEVAL / RAG / SIMILAR CASE INTELLIGENCE ENDPOINTS
   ========================================================================== */

export async function retrieveSimilarKnowledge(caseId, query = null, topK = 5, minSimilarity = 0.70) {
  return authFetch(`/cases/${caseId}/ai/retrieve`, {
    method: 'POST',
    body: JSON.stringify({
      query: query || null,
      top_k: topK,
      min_similarity: minSimilarity,
    }),
  });
}

export async function indexCaseKnowledge(caseId) {
  return authFetch(`/cases/${caseId}/ai/index`, {
    method: 'POST',
  });
}

export async function updateCaseRetrievalStatus(caseId, retrievalEnabled) {
  return authFetch(`/cases/${caseId}/retrieval-status`, {
    method: 'PATCH',
    body: JSON.stringify({
      retrieval_enabled: retrievalEnabled,
    }),
  });
}

/* ==========================================================================
   PHASE 11: EVIDENCE VERIFICATION LAYER ENDPOINTS
   ========================================================================== */

export async function verifyRecommendation(caseId, recommendationId) {
  return authFetch(`/cases/${caseId}/ai/recommendations/${recommendationId}/verify`, {
    method: 'POST',
  });
}

export async function getVerificationHistory(caseId) {
  return authFetch(`/cases/${caseId}/ai/verifications`);
}

export async function getVerificationDetails(caseId, verificationId) {
  return authFetch(`/cases/${caseId}/ai/verifications/${verificationId}`);
}

export async function verifyCaseAiAnalysis(caseId, analysisId) {
  return verifyRecommendation(caseId, analysisId);
}

export async function getCaseAiVerification(caseId, analysisId) {
  return getVerificationDetails(caseId, analysisId);
}

/* ==========================================================================
   PHASE 12: INVESTIGATOR REVIEW LAYER ENDPOINTS
   ========================================================================== */

export async function approveRecommendationItem(caseId, analysisId, itemId, payload = {}) {
  return authFetch(`/cases/${caseId}/ai/reviews/${analysisId}/items/${itemId}/approve`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function approveRecommendation(caseId, recommendationId, comment = '', confirmUnsupported = false) {
  return authFetch(`/cases/${caseId}/ai/recommendations/${recommendationId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ comment, confirmUnsupported }),
  });
}

export async function editRecommendationItem(caseId, analysisId, itemId, payload) {
  return authFetch(`/cases/${caseId}/ai/reviews/${analysisId}/items/${itemId}/edit`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function editRecommendation(caseId, recommendationId, editedSummary, comment = '', evidenceReferences = []) {
  return authFetch(`/cases/${caseId}/ai/recommendations/${recommendationId}/edit`, {
    method: 'POST',
    body: JSON.stringify({ editedSummary, comment, evidenceReferences }),
  });
}

export async function rejectRecommendationItem(caseId, analysisId, itemId, payload) {
  return authFetch(`/cases/${caseId}/ai/reviews/${analysisId}/items/${itemId}/reject`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function rejectRecommendation(caseId, recommendationId, comment) {
  return authFetch(`/cases/${caseId}/ai/recommendations/${recommendationId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ comment }),
  });
}

export async function getAnalysisReviews(caseId, analysisId) {
  return authFetch(`/cases/${caseId}/ai/reviews/${analysisId}`);
}

export async function getRecommendationReviews(caseId, recommendationId) {
  return authFetch(`/cases/${caseId}/ai/recommendations/${recommendationId}/reviews`);
}

export async function getRecommendationDetail(caseId, recommendationId) {
  return authFetch(`/cases/${caseId}/ai/recommendations/${recommendationId}`);
}

export async function getItemReview(caseId, analysisId, itemId) {
  return authFetch(`/cases/${caseId}/ai/reviews/${analysisId}/items/${itemId}`);
}

export async function getAnalysisReviewSummary(caseId, analysisId) {
  return authFetch(`/cases/${caseId}/ai/reviews/${analysisId}/summary`);
}

export async function getPendingReviews(caseId) {
  return authFetch(`/cases/${caseId}/ai/reviews/pending`);
}

/* ==========================================================================
   PHASE 13: NOTIFICATIONS & ALERTS & CASE OFFICERS ENDPOINTS
   ========================================================================== */

export async function getCaseOfficers(caseId) {
  return authFetch(`/cases/${caseId}/officers`);
}

export async function getAvailableOfficers(caseId) {
  return authFetch(`/cases/${caseId}/available-officers`);
}

export async function assignCaseOfficer(caseId, officerId, role = 'INVESTIGATOR') {
  return authFetch(`/cases/${caseId}/officers/${officerId}?role=${encodeURIComponent(role)}`, {
    method: 'POST',
  });
}

export async function removeCaseOfficer(caseId, officerId) {
  return authFetch(`/cases/${caseId}/officers/${officerId}`, {
    method: 'DELETE',
  });
}

export async function getUnreadNotificationCount() {
  return authFetch('/notifications/unread-count');
}

export async function markNotificationRead(notificationId) {
  return authFetch(`/notifications/${notificationId}/read`, {
    method: 'PATCH',
  });
}

export async function markAllNotificationsRead() {
  return authFetch('/notifications/read-all', {
    method: 'PATCH',
  });
}

export async function getCaseNotifications(caseId, page = 0, size = 20) {
  return authFetch(`/cases/${caseId}/notifications?page=${page}&size=${size}`);
}

export async function getNotifications(params = {}) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, value);
    }
  });
  const qs = query.toString();
  return authFetch(`/notifications${qs ? `?${qs}` : ''}`);
}

export async function getNotificationById(id) {
  return authFetch(`/notifications/${id}`);
}

export async function retryNotification(id) {
  return authFetch(`/notifications/${id}/retry`, {
    method: 'POST',
  });
}

export async function getNotificationPreferences() {
  return authFetch('/notification-preferences');
}

export async function updateNotificationPreferences(payload) {
  return authFetch('/notification-preferences', {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function sendTestCaseNotification(caseId, payload = { channel: 'EMAIL' }) {
  return authFetch(`/cases/${caseId}/notifications/test`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/* ==========================================================================
   PHASE 14: PUBLIC NEWS & CRIME SEARCH INTELLIGENCE ENDPOINTS
   ========================================================================== */

export async function searchPublicInformation(payload) {
  return authFetch('/public-search', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function searchCasePublicInformation(caseId, payload) {
  return authFetch(`/cases/${caseId}/public-search`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getCaseSuggestedQueries(caseId) {
  return authFetch(`/cases/${caseId}/public-search/suggested-queries`);
}

export async function savePublicInformation(caseId, payload) {
  return authFetch(`/cases/${caseId}/public-information`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getCasePublicInformation(caseId) {
  return authFetch(`/cases/${caseId}/public-information`);
}

export async function getPublicInformationItem(caseId, id) {
  return authFetch(`/cases/${caseId}/public-information/${id}`);
}

export async function updatePublicInformationItem(caseId, id, payload) {
  return authFetch(`/cases/${caseId}/public-information/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deletePublicInformationItem(caseId, id) {
  return authFetch(`/cases/${caseId}/public-information/${id}`, {
    method: 'DELETE',
  });
}

/* ==========================================================================
   PHASE 15: PDF CASE REPORT GENERATION ENDPOINTS
   ========================================================================== */

export async function downloadCaseReportPdf(caseId) {
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}/cases/${caseId}/report/pdf`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!response.ok) {
    let errorText = 'Failed to generate PDF case report';
    try {
      const errJson = await response.json();
      errorText = errJson.message || errorText;
    } catch (e) {
      // ignored
    }
    throw new Error(errorText);
  }

  const blob = await response.blob();
  return blob;
}

export async function getCaseReportSummary(caseId) {
  return authFetch(`/cases/${caseId}/report/summary`);
}

export async function getCaseReportHistory(caseId) {
  return authFetch(`/cases/${caseId}/report/history`);
}

/* ==========================================================================
   PHASE 17: AI SCENE ILLUSTRATIONS & PHOTO MANAGEMENT ENDPOINTS
   ========================================================================== */

export async function generateCaseIllustration(caseId, payload) {
  return authFetch(`/cases/${caseId}/illustrations/generate`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getCaseIllustrations(caseId) {
  return authFetch(`/cases/${caseId}/illustrations`);
}

export async function deleteCaseIllustration(caseId, illustrationId) {
  return authFetch(`/cases/${caseId}/illustrations/${illustrationId}`, {
    method: 'DELETE',
  });
}

export async function uploadSuspectPhoto(caseId, suspectId, formData) {
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}/cases/${caseId}/suspects/${suspectId}/photo`, {
    method: 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: formData,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Failed to upload suspect photograph');
  }

  return response.json();
}

export function getSuspectPhotoUrl(caseId, suspectId) {
  return `${API_BASE_URL}/cases/${caseId}/suspects/${suspectId}/photo`;
}

export function getSuspectDefaultImageUrl(caseId, suspectId) {
  return `${API_BASE_URL}/cases/${caseId}/suspects/${suspectId}/default-image`;
}

export async function uploadVictimPhoto(caseId, victimId, formData) {
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}/cases/${caseId}/victims/${victimId}/photo`, {
    method: 'POST',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: formData,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Failed to upload victim photograph');
  }

  return response.json();
}

export function getVictimPhotoUrl(caseId, victimId) {
  return `${API_BASE_URL}/cases/${caseId}/victims/${victimId}/photo`;
}

export function getVictimDefaultImageUrl(caseId, victimId) {
  return `${API_BASE_URL}/cases/${caseId}/victims/${victimId}/default-image`;
}

export function getEvidenceViewUrl(caseId, evidenceId) {
  return `${API_BASE_URL}/cases/${caseId}/evidence/${evidenceId}/view`;
}

export function resolveAssetUrl(urlOrPath) {
  if (!urlOrPath) return '';
  if (urlOrPath.startsWith('http://') || urlOrPath.startsWith('https://') || urlOrPath.startsWith('data:') || urlOrPath.startsWith('blob:')) {
    return urlOrPath;
  }
  const baseUrl = API_BASE_URL.replace(/\/api\/?$/, '');
  const cleanPath = urlOrPath.startsWith('/') ? urlOrPath : `/${urlOrPath}`;
  return `${baseUrl}${cleanPath.startsWith('/api') ? cleanPath : `/api${cleanPath}`}`;
}

export function getIllustrationViewUrl(caseId, illustrationId) {
  return `${API_BASE_URL}/cases/${caseId}/illustrations/${illustrationId}/view`;
}

export async function generateEvidenceIllustration(caseId, evidenceId, prompt) {
  const token = getAuthToken();
  const response = await fetch(`${API_BASE_URL}/cases/${caseId}/evidence/${evidenceId}/illustration`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ prompt })
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Failed to generate AI illustration for evidence');
  }

  return response.json();
}

export async function replaceEvidenceFile(caseId, evidenceId, formData, onProgress) {
  const token = getAuthToken();
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${API_BASE_URL}/cases/${caseId}/evidence/${evidenceId}/replace-image`);
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }

    if (xhr.upload && onProgress) {
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          const percentComplete = Math.round((event.loaded / event.total) * 100);
          onProgress(percentComplete);
        }
      };
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const data = JSON.parse(xhr.responseText);
          resolve(data);
        } catch (e) {
          resolve({ status: 'SUCCESS' });
        }
      } else {
        try {
          const err = JSON.parse(xhr.responseText);
          reject(new Error(err.message || `Replacement failed with status ${xhr.status}`));
        } catch (e) {
          reject(new Error(`Replacement failed with status ${xhr.status}`));
        }
      }
    };

    xhr.onerror = () => {
      reject(new Error('Network error during evidence replacement'));
    };

    xhr.send(formData);
  });
}

/* ==========================================================================
   INTER-STATION COORDINATION ENDPOINTS
   ========================================================================== */

export async function createInterStationRequest(data) {
  return authFetch('/inter-station-requests', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function getSentInterStationRequests() {
  return authFetch('/inter-station-requests/sent');
}

export async function getReceivedInterStationRequests(status = '') {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return authFetch(`/inter-station-requests/received${query}`);
}

export async function getInterStationRequestById(id) {
  return authFetch(`/inter-station-requests/${id}`);
}

export async function acceptInterStationRequest(id) {
  return authFetch(`/inter-station-requests/${id}/accept`, {
    method: 'PATCH',
  });
}

export async function rejectInterStationRequest(id, responseNotes) {
  return authFetch(`/inter-station-requests/${id}/reject`, {
    method: 'PATCH',
    body: JSON.stringify({ responseNotes }),
  });
}

export async function respondToInterStationRequest(id, responseNotes) {
  return authFetch(`/inter-station-requests/${id}/respond`, {
    method: 'POST',
    body: JSON.stringify({ responseNotes }),
  });
}

/* ==========================================================================
   OFFICER REGISTRATION & VERIFICATION ENDPOINTS
   ========================================================================== */

export async function registerOfficer(formData) {
  const response = await fetch(`${API_BASE_URL}/auth/register`, {
    method: 'POST',
    body: formData,
  });

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(data.message || `Registration failed with status ${response.status}`);
  }

  return data;
}

export async function getPendingOfficerRegistrations() {
  return authFetch('/auth/station-registrations/pending');
}

export async function getOfficerRegistrationDetail(id) {
  return authFetch(`/auth/station-registrations/${id}`);
}

export async function approveOfficerRegistration(id) {
  return authFetch(`/auth/station-registrations/${id}/approve`, {
    method: 'PATCH',
  });
}

export async function rejectOfficerRegistration(id, rejectionReason) {
  return authFetch(`/auth/station-registrations/${id}/reject`, {
    method: 'PATCH',
    body: JSON.stringify({ rejectionReason }),
  });
}

export function getOfficerIdDocumentUrl(id) {
  return `${API_BASE_URL}/auth/station-registrations/${id}/document`;
}

/* ==========================================================================
   LOCATION INTELLIGENCE ENDPOINTS
   ========================================================================== */

export async function getCaseLocationIntelligence(caseId) {
  return authFetch(`/cases/${caseId}/locations`);
}

export async function createCaseLocation(caseId, payload) {
  return authFetch(`/cases/${caseId}/locations`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function deleteCaseLocation(caseId, locationId) {
  return authFetch(`/cases/${caseId}/locations/${locationId}`, {
    method: 'DELETE',
  });
}

export { API_BASE_URL, AI_SERVICE_BASE_URL };


