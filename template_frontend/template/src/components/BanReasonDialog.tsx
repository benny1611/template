import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField, Typography } from "@mui/material";
import { useState } from "react";
import { useI18n } from "../i18n/i18nContext";

interface BanDialogProps {
  open: boolean;
  userName?: string;
  userId: number;
  onClose: () => void;
  onConfirm: (reason: string, userId: number) => void;
}

const BanReasonDialog = ({ open, userName, userId, onClose, onConfirm }: BanDialogProps) => {
  // Localizing the "noisy" state here prevents parent re-renders
  const [reason, setReason] = useState("");
  const { translation } = useI18n();

  const handleConfirm = () => {
    if (reason.trim()) {
      onConfirm(reason, userId);
      setReason(""); // Reset for next time
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{translation.admin.ban_user} {userName}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" sx={{ mb: 2, color: 'text.secondary', mt: 1 }}>
          {translation.admin.ban_text}
        </Typography>
        <TextField
          autoFocus
          margin="dense"
          label={translation.admin.reason_for_ban}
          fullWidth
          required
          error={!reason.trim()}
          value={reason}
          onChange={(e) => setReason(e.target.value)} // Only this component re-renders now!
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>{translation.admin.cancel}</Button>
        <Button 
          onClick={handleConfirm} 
          variant="contained" 
          color="error" 
          disabled={!reason.trim()}
        >
          {translation.admin.confirm_ban}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default BanReasonDialog;