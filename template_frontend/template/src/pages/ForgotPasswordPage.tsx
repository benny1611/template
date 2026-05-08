import { useState } from "react";
import PasswordResetRequest from "../models/dto/PasswordResetRequest";
import { ENV } from "../config/env";
import { useI18n } from "../i18n/i18nContext";
import { Alert, Box, Button, TextField, Typography } from "@mui/material";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const { translation } = useI18n();

  async function handleSubmit() {
    setLoading(true);
    let pwRstRequest = new PasswordResetRequest(email);
    const apiEndpoint = `${ENV.API_BASE_URL}/auth/password-reset/request`;
    try {
      const response = await fetch(apiEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(pwRstRequest),
      });

      if (!response.ok) {
        const message = (await response.text()) || translation.forgot.error;
        throw new Error(message);
      }

      setSent(true);
      setError(null);
    } catch (err: any) {
      setSent(false);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box maxWidth={400} mx="auto" mt={30}>
      <Typography variant="h5" mb={2} textAlign="center">
        {translation.forgot.forgot_pw}
      </Typography>

      {sent && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {translation.forgot.success}
        </Alert>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <TextField
        label={translation.forgot.email}
        fullWidth
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        disabled={sent}
        margin="normal"
        sx={{
          mb: 5,
        }}
      />

      <Button
        fullWidth
        variant="contained"
        onClick={handleSubmit}
        disabled={!email || loading || sent}
      >
        {translation.forgot.send}
      </Button>
    </Box>
  );
}
