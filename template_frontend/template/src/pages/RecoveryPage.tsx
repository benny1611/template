import {
  Button,
  Container,
  Stack,
  Typography,
  Alert,
} from "@mui/material";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useState } from "react";
import { ENV } from "../config/env";
import { useI18n } from "../i18n/i18nContext";

export default function RecoveryPage() {
  const [searchParams] = useSearchParams();
  const email = searchParams.get("email");
  const { translation } = useI18n();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleRecover = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${ENV.API_BASE_URL}/users/recover?email=${email}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });

      if (response.ok) {
        // Recovery successful! Send them back to login
        navigate("/login", { state: { recovered: true } });
      } else {
        setError(translation.recover_dialog.recover_fail);
      }
    } catch (err) {
      setError(translation.recover_dialog.server_unreachable);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="sm" sx={{ mt: 15 }}>
      <Stack spacing={3} alignItems="center">
        <Typography variant="h4">{translation.recover_dialog.title}</Typography>
        <Typography textAlign="center">
          {translation.recover_dialog.question_part_1} <strong>{email}</strong> {translation.recover_dialog.question_part_2}
        </Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <Button
          variant="contained"
          size="large"
          onClick={handleRecover}
          disabled={loading || !email}
        >
          {loading ? translation.recover_dialog.restoring : translation.recover_dialog.restore_confirmation}
        </Button>
        <Button variant="text" onClick={() => navigate("/login")}>
          {translation.recover_dialog.cancel}
        </Button>
      </Stack>
    </Container>
  );
}
