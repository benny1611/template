import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { CircularProgress } from "@mui/material";

const AdminRoute = () => {
  const { roles, isAuthenticated, token } = useAuth();
  const location = useLocation();

  if (token && roles.length === 0) return <CircularProgress />;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles.includes("ROLE_ADMIN") || roles.includes("ROLE_SUPER_ADMIN")) {
    return <Outlet />;
  }

  return <Navigate to="/" replace />;
};

export default AdminRoute;
