import { Alert, Box, Button, TextField, Typography } from "@mui/material";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useI18n } from "../i18n/i18nContext";
import { ENV } from "../config/env";
import PasswordResetConfirmRequest from "../models/dto/PasswordResetConfirmRequest";

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { translation } = useI18n();

  const id = params.get("id");
  const token = params.get("token");

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (!id || !token) {
    return (
      <Box maxWidth={400} mx="auto" mt={10}>
        <Alert severity="error">{translation.reset.invalid}</Alert>
      </Box>
    );
  }

  async function handleSubmit() {
    if (password !== confirm) {
      setError(translation.reset.not_matching);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const pwResetConfirmReset: PasswordResetConfirmRequest =
        new PasswordResetConfirmRequest(token!, id!, password);
      const apiEndpoint = `${ENV.API_BASE_URL}/auth/password-reset/confirm`;
      const response = await fetch(apiEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(pwResetConfirmReset),
      });

      if (!response.ok) {
        throw new Error();
      }

      navigate("/login");
    } catch {
      setError(translation.reset.error);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box maxWidth={400} mx="auto" mt={10}>
      <Typography variant="h5" mb={2}>
        {translation.reset.reset}
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TextField
        label={translation.reset.new_password}
        type="password"
        fullWidth
        margin="normal"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      <TextField
        label={translation.reset.confirm}
        type="password"
        fullWidth
        margin="normal"
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
      />

      <Button
        fullWidth
        variant="contained"
        onClick={handleSubmit}
        disabled={!password || loading}
      >
        {translation.reset.reset}
      </Button>
    </Box>
  );
}
