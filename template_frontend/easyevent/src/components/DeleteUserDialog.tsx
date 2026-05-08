import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, TextField } from "@mui/material";
import { useEffect, useState } from "react";
import { useI18n } from "../i18n/i18nContext";

interface DeleteUserDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => void;
  userName?: string;
  loading: boolean;
}

const DeleteUserDialog = ({ open, onClose, onConfirm, userName, loading }: DeleteUserDialogProps) => {
  const [reason, setReason] = useState("");
  const {translation} = useI18n();

  // Reset reason when dialog opens/closes
  useEffect(() => {
    if (!open) setReason("");
  }, [open]);

  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>{translation.delete_dialog.delete_user}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          {translation.delete_dialog.delete_question} <strong>{userName}</strong>
          {translation.delete_dialog.delete_question_continuation}
        </DialogContentText>
        <TextField
          autoFocus
          margin="dense"
          label={translation.delete_dialog.reason}
          fullWidth
          variant="outlined"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          error={reason.length > 0 && !reason.trim()}
          helperText={translation.delete_dialog.reason_helper}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>{translation.delete_dialog.cancel}</Button>
        <Button
          onClick={() => onConfirm(reason)}
          color="error"
          variant="contained"
          disabled={!reason.trim() || loading}
        >
          {translation.delete_dialog.delete}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default DeleteUserDialog;