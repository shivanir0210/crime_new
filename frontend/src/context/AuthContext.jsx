import React, { createContext, useContext, useState, useEffect } from 'react';
import { getCurrentOfficer, loginPoliceOfficer, verifyLoginOtp } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => sessionStorage.getItem('crimesphere_token') || null);
  const [officer, setOfficer] = useState(() => {
    const saved = sessionStorage.getItem('crimesphere_officer');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Validate existing session on application mount
    if (token) {
      getCurrentOfficer()
        .then((data) => {
          if (data && data.policeId) {
            setOfficer(data);
            sessionStorage.setItem('crimesphere_officer', JSON.stringify(data));
          } else {
            logout();
          }
        })
        .catch(() => {
          logout();
        })
        .finally(() => {
          setLoading(false);
        });
    } else {
      setLoading(false);
    }
  }, [token]);

  const initiateLogin = async (policeId, password) => {
    return await loginPoliceOfficer(policeId, password);
  };

  const completeOtpVerification = async (challengeId, otp) => {
    const response = await verifyLoginOtp(challengeId, otp);
    if (response && response.token) {
      setToken(response.token);
      sessionStorage.setItem('crimesphere_token', response.token);

      const officerData = {
        ...response.officer,
        station: response.station
      };
      setOfficer(officerData);
      sessionStorage.setItem('crimesphere_officer', JSON.stringify(officerData));
      return { success: true, officer: officerData };
    }
    throw new Error(response?.message || 'Verification failed');
  };

  const login = async (policeId, password) => {
    // If called directly, initiates login
    return await initiateLogin(policeId, password);
  };

  const logout = () => {
    setToken(null);
    setOfficer(null);
    sessionStorage.removeItem('crimesphere_token');
    sessionStorage.removeItem('crimesphere_officer');
  };

  const isAuthenticated = !!token && !!officer;

  return (
    <AuthContext.Provider value={{ 
      token, 
      officer, 
      isAuthenticated, 
      loading, 
      login, 
      initiateLogin, 
      completeOtpVerification, 
      logout 
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
