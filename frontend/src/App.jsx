import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorBoundary from './components/ErrorBoundary';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import CasesPage from './pages/CasesPage';
import CreateCasePage from './pages/CreateCasePage';
import CaseDetailPage from './pages/CaseDetailPage';
import EvidenceGapPage from './pages/EvidenceGapPage';
import PublicNewsPage from './pages/PublicNewsPage';
import InterStationRequestsPage from './pages/InterStationRequestsPage';
import RegisterPage from './pages/RegisterPage';
import OfficerRegistrationsPage from './pages/OfficerRegistrationsPage';
import OfficerRegistrationDetailPage from './pages/OfficerRegistrationDetailPage';
import LocationIntelligencePage from './pages/LocationIntelligencePage';

export default function App() {
  return (
    <AuthProvider>
      <ErrorBoundary>
        <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/location-intelligence" 
            element={
              <ProtectedRoute>
                <LocationIntelligencePage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/location-intelligence/:caseId" 
            element={
              <ProtectedRoute>
                <LocationIntelligencePage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/cases" 
            element={
              <ProtectedRoute>
                <CasesPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/cases/new" 
            element={
              <ProtectedRoute>
                <CreateCasePage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/cases/:id" 
            element={
              <ProtectedRoute>
                <CaseDetailPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/evidence-gaps" 
            element={
              <ProtectedRoute>
                <EvidenceGapPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/public-news" 
            element={
              <ProtectedRoute>
                <PublicNewsPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/inter-station-requests" 
            element={
              <ProtectedRoute>
                <InterStationRequestsPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/officer-approvals" 
            element={
              <ProtectedRoute>
                <OfficerRegistrationsPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/officer-approvals/:id" 
            element={
              <ProtectedRoute>
                <OfficerRegistrationDetailPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/station-head/registrations" 
            element={
              <ProtectedRoute>
                <OfficerRegistrationsPage />
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/station-head/registrations/:id" 
            element={
              <ProtectedRoute>
                <OfficerRegistrationDetailPage />
              </ProtectedRoute>
            } 
          />

          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Router>
    </ErrorBoundary>
  </AuthProvider>
  );
}
