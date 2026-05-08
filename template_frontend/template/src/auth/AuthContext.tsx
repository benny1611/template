import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { JwtPayload } from "./JwtPayload";
import type LoginResponse from "../models/dto/LoginResponse";

type AuthContextType = {
  token: string | null;
  roles: string[];
  profilePictureUrl: string | null;
  username: string;
  userId: number | null;
  userState: string | null;
  isAuthenticated: boolean;
  isLocalPasswordSet: boolean;
  hasRole: (role: string) => boolean;
  login: (input: LoginResponse) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const STORAGE_KEY = "auth_token";

function decodeJwt(token: string): JwtPayload {
  const payload = token.split(".")[1];
  return JSON.parse(atob(payload));
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(STORAGE_KEY),
  );
  const [payload, setPayload] = useState<JwtPayload | null>(() => {
    const initialToken = localStorage.getItem(STORAGE_KEY);
    if (initialToken) {
      try {
        return decodeJwt(initialToken);
      } catch {
        return null;
      }
    }
    return null;
  });

  useEffect(() => {
    if (!token) {
      setPayload(null);
      return;
    }
    try {
      const decoded = decodeJwt(token);
      setPayload(decoded);
    } catch {
      logout();
    }
  }, [token]);

  useEffect(() => {
    if (!payload) {
      return;
    }

    const expiresAtMs = payload.exp * 1000;
    const timeout = expiresAtMs - Date.now();

    if (timeout <= 0) {
      logout();
      return;
    }

    const timer = setTimeout(logout, timeout);
    return () => clearTimeout(timer);
  }, [payload]);

  const login = (input: LoginResponse) => {
    localStorage.setItem(STORAGE_KEY, input.token);
    setToken(input.token);
  };

  const logout = () => {
    localStorage.removeItem(STORAGE_KEY);
    setToken(null);
    setPayload(null);
  };

  const value = useMemo<AuthContextType>(
    () => ({
      token,
      isAuthenticated: !!token,
      userId: payload?.sub ?? null,
      userState: payload?.state!,
      roles: payload?.roles ?? [],
      profilePictureUrl: payload?.profilePictureUrl ?? null,
      username: payload?.username ?? "",
      isLocalPasswordSet: payload?.isLocalPasswordSet!,
      login,
      logout,
      hasRole: (role: string) => payload?.roles.includes(role) ?? false,
    }),
    [token, payload],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return ctx;
}
