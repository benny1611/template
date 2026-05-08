import {
  Alert,
  Box,
  Button,
  Container,
  Link,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useAuth } from "../auth/AuthContext";
import { ENV } from "../config/env";
import { useI18n } from "../i18n/i18nContext";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import LoginRequest from "../models/dto/LoginRequest";
import { Link as RouterLink } from "react-router-dom";
import LoginResponse from "../models/dto/LoginResponse";
import GoogleIcon from "@mui/icons-material/Google";

export default function LoginPage() {
  const { login } = useAuth();
  const { translation } = useI18n();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState(false);
  const isValidEmail = (email: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    setError(null);
    setLoading(true);

    try {
      if (emailError) {
        const msg = {
          message: "EMAIL_WRONG",
        };
        throw new Error(JSON.stringify(msg));
      }

      const apiEndpoint = `${ENV.API_BASE_URL}/auth/login`;
      const response = await fetch(apiEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(new LoginRequest(email, password)),
      });

      if (!response.ok) {
        const message =
          (await response.text()) || translation.login.invalid_credentials;
        const err = new Error(message);
        err.cause = response.status;
        throw err;
      }

      const data = await response.json();
      const input = new LoginResponse(data.token);
      login(input);

      navigate("/", { replace: true });
    } catch (err: any) {
      if (!navigator.onLine) {
        setError(translation.login.no_internet);
      } else {
        try {
          const errorJSON = JSON.parse(err.message);
          switch (errorJSON.message) {
            case "BAD_CREDENTIALS":
              setError(translation.login.invalid_credentials);
              break;
            case "EMAIL_WRONG":
              setError(translation.login.email_wrong);
              break;
            default:
              setError(translation.login.something_went_wrong);
          }
        } catch (e: any) {
          try {
            const errorData = JSON.parse(err.message);
            if (errorData.message === "ACCOUNT_SOFT_DELETED") {
              navigate(`/recover?email=${encodeURIComponent(email)}`);
              return;
            }
          } catch (err: any) {
            // ignore
          }
          if (err.cause === 401) {
            setError(translation.login.invalid_credentials);
          } else {
            setError(translation.login.server_unreachable);
          }
        }
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        flex: 1,
        display: "flex",
        mt: 15,
      }}
    >
      <Container
        maxWidth="sm"
        sx={{
          alignContent: "center",
          mt: 5,
        }}
      >
        <Stack spacing={3}>
          <Typography variant="h4" textAlign="center">
            {translation.login.login}
          </Typography>

          {error && <Alert severity="error">{error}</Alert>}

          <TextField
            label={translation.login.email}
            value={email}
            onChange={(e) => {
              const value = e.target.value;
              setEmail(value);
              setEmailError(value !== "" && !isValidEmail(value));
            }}
            error={emailError}
            autoComplete="email"
          />

          <TextField
            label={translation.login.password}
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                handleLogin();
              }
            }}
            autoComplete="current-password"
          />

          <Button
            variant="contained"
            size="large"
            onClick={handleLogin}
            disabled={loading}
            fullWidth
            sx={{
              textTransform: "none",
              fontWeight: 600,
              fontSize: 20,
            }}
          >
            {translation.login.login}
          </Button>
          <Button
            variant="outlined"
            fullWidth
            onClick={() => {
              window.location.href = `${ENV.BARE_URL_BASE}/oauth2/authorization/google`;
            }}
          >
            {translation.login.continue_with}
            <GoogleIcon sx={{ ml: 2 }}></GoogleIcon>
          </Button>
          <Typography variant="subtitle2" textAlign="left">
            {translation.login.no_account}{" "}
            <Link component={RouterLink} to="/register">
              {translation.login.sign_in}
            </Link>
          </Typography>
          <Typography
            variant="subtitle2"
            textAlign="left"
            sx={{ ":not(style)": { marginTop: "0 !important" } }}
          >
            {translation.login.forgot}{" "}
            <Link component={RouterLink} to="/forgot">
              {translation.login.reset}
            </Link>
          </Typography>
        </Stack>
      </Container>
    </Box>
  );
}
