import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

interface User {
  username: string;
  roles: string[];
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string, username: string, roles: string[]) => void;
  logout: () => void;
  isAuthenticated: boolean;
}

// Synchronously configure axios defaults on initial module evaluation
const initialSavedToken = localStorage.getItem('smart_inventory_jwt');
if (initialSavedToken) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${initialSavedToken}`;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(initialSavedToken);
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('smart_inventory_user');
    return saved ? JSON.parse(saved) : null;
  });

  const login = (newToken: string, username: string, roles: string[]) => {
    axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
    setToken(newToken);
    const userInfo = { username, roles };
    setUser(userInfo);
    localStorage.setItem('smart_inventory_jwt', newToken);
    localStorage.setItem('smart_inventory_user', JSON.stringify(userInfo));
  };

  const logout = () => {
    delete axios.defaults.headers.common['Authorization'];
    setToken(null);
    setUser(null);
    localStorage.removeItem('smart_inventory_jwt');
    localStorage.removeItem('smart_inventory_user');
  };

  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response && error.response.status === 401) {
          logout();
        }
        return Promise.reject(error);
      }
    );

    return () => {
      axios.interceptors.response.eject(interceptor);
    };
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
