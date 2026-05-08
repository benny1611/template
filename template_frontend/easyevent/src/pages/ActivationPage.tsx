import { useEffect, useState } from "react";
import { ActivationMailRequest } from "../models/dto/ActivationMailRequest";
import { useNavigate } from "react-router-dom";
import { ENV } from "../config/env";
import { useI18n } from "../i18n/i18nContext";
import { 
  Box, 
  CircularProgress, 
  Typography, 
  Container, 
  Paper, 
  Avatar, 
  Zoom,
  Fade,
  Button
} from "@mui/material";
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import { useAuth } from "../auth/AuthContext";

export default function ActivationPage() {
  const params = new URLSearchParams(window.location.search);
  const activationToken = params.get("token");
  const navigate = useNavigate();
  const { translation } = useI18n();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const { token, logout } = useAuth();

  useEffect(() => {
    if (token) logout();

    if (!activationToken) {
      setError(translation.activation.fail);
      return;
    }

    const activationRequest = new ActivationMailRequest(activationToken);
    setLoading(true);

    fetch(`${ENV.API_BASE_URL}/users/activate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(activationRequest),
    })
      .then((res) => {
        if (!res.ok) {
          const err = new Error(translation.activation.fail);
          err.cause = res.status;
          throw err;
        }
        setSuccess(true);
      })
      .catch((err) => {
        if (err.cause === 404) {
          setError(translation.activation.token_not_found);
        } else {
          setError(err.message);
        }
      })
      .finally(() => {
        setLoading(false);
      });
  }, [activationToken]);

  // Handle Redirect after success
  useEffect(() => {
    if (success) {
      const timer = setTimeout(() => {
        navigate("/login", { replace: true });
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [success, navigate]);

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          marginTop: 15,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
        }}
      >
        <Paper
          elevation={3}
          sx={{
            padding: 6,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            width: "100%",
            borderRadius: 4,
            textAlign: "center",
            minHeight: '300px',
            justifyContent: 'center'
          }}
        >
          {loading && (
            <Fade in={loading}>
              <Box>
                <CircularProgress size={60} sx={{ mb: 2 }} />
                <Typography variant="h6" color="textSecondary">
                  {translation.activation.loading}
                </Typography>
              </Box>
            </Fade>
          )}

          {success && (
            <Zoom in={success}>
              <Box>
                <Avatar sx={{ m: "auto", bgcolor: "success.main", width: 80, height: 80, mb: 2 }}>
                  <CheckCircleOutlineIcon sx={{ fontSize: 50 }} />
                </Avatar>
                <Typography component="h1" variant="h4" gutterBottom fontWeight="bold">
                  {translation.activation.success_title}
                </Typography>
                <Typography variant="body1" color="textSecondary">
                  {translation.activation.success}
                </Typography>
                <Typography variant="body2" color="textSecondary" sx={{ mt: 3 }}>
                   {translation.activation.redirecting}
                </Typography>
              </Box>
            </Zoom>
          )}

          {error && (
            <Zoom in={!!error}>
              <Box>
                <Avatar sx={{ m: "auto", bgcolor: "error.main", width: 80, height: 80, mb: 2 }}>
                  <ErrorOutlineIcon sx={{ fontSize: 50 }} />
                </Avatar>
                <Typography component="h1" variant="h4" gutterBottom fontWeight="bold">
                  {translation.activation.error_title}
                </Typography>
                <Typography variant="body1" color="textSecondary">
                  {error}
                </Typography>
                <Button 
                  variant="contained" 
                  sx={{ mt: 4 }} 
                  onClick={() => navigate("/login")}
                >
                  {translation.activation.back}
                </Button>
              </Box>
            </Zoom>
          )}
        </Paper>
      </Box>
    </Container>
  );
}