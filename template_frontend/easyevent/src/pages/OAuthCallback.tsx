import { useEffect, useRef } from "react";
import { ENV } from "../config/env";
import LoginResponse from "../models/dto/LoginResponse";
import { useAuth } from "../auth/AuthContext";
import { useNavigate } from "react-router-dom";
import { Box, CircularProgress, Typography } from "@mui/material";
import { useI18n } from "../i18n/i18nContext";

const OAuthCallback = () => {
  const { translation } = useI18n();
  const { login } = useAuth();
  const navigate = useNavigate();

  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");

  const hasExchanged = useRef(false);

  useEffect(() => {
    if (!code || hasExchanged.current) {
      return;
    }

    hasExchanged.current = true;

    fetch(`${ENV.BARE_URL_BASE}/api/auth/oauth/exchange`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ code }),
    })
      .then(async (res) => {
        const data = await res.json();
        if (!res.ok) {
          if (data.message === "ACCOUNT_SOFT_DELETED") {
            navigate(`/recover?email=${encodeURIComponent(data.email)}`);
          } else {
            throw new Error("OAuth exchange failed");
          }
          return;
        }
        login(new LoginResponse(data.token));
        navigate("/", { replace: true });
      })
      .catch(() => {
        navigate("/login", { replace: true });
      });
  }, [code, login, navigate]);

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: 2,
      }}
    >
      <CircularProgress />
      <Typography color="text.secondary">
        {translation.login.signing_in}
      </Typography>
    </Box>
  );
};

export default OAuthCallback;
