import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Avatar,
  Box,
  Button,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  type SelectChangeEvent,
} from "@mui/material";
import { useI18n } from "../i18n/i18nContext";
import { ExpandMore, PhotoCamera } from "@mui/icons-material";
import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { ENV } from "../config/env";
import UserDTO from "../models/dto/UserDTO";
import LoginResponse from "../models/dto/LoginResponse";
import DeleteForever from "@mui/icons-material/DeleteForever";
import DeletionReason from "../models/dto/DeletionReason";

const ProfilePage = () => {
  const { translation } = useI18n();
  const {
    profilePictureUrl,
    token,
    isLocalPasswordSet,
    userId,
    userState,
    login,
    logout,
  } = useAuth();
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [language, setLanguage] = useState<string>("en");
  const [loading, setLoading] = useState(false);
  const [passwordExpanded, setPasswordExpanded] = useState(false);

  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);

  const [nameError, setNameError] = useState(false);
  const [nameTouched, setNameTouched] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [emailTouched, setEmailTouched] = useState(false);
  const [passwordError, setPasswordError] = useState(false);
  const [oldPasswordError, setOldPasswordError] = useState(false);
  const [passwordTouched, setPasswordTouched] = useState(false);
  const [oldPasswordTouched, setOldPasswordTouched] = useState(false);
  const [repeatPasswordError, setRepeatPasswordError] = useState(false);
  const [repeatPasswordTouched, setRepeatPasswordTouched] = useState(false);

  const handleDeleteAccount = async () => {
    setLoading(true);
    setError(null);
    try {
      const apiEndpoint = `${ENV.API_BASE_URL}/users/${userId}`;
      const deletionRequest = new DeletionReason(null);
      const response = await fetch(apiEndpoint, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(deletionRequest),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Failed to delete account");
      }

      // Successful deletion - Clear local storage and state via AuthContext
      logout();
    } catch (err: any) {
      setError(err.message || "An error occurred while deleting your account.");
      setOpenDeleteDialog(false);
    } finally {
      setLoading(false);
    }
  };

  const handleLoad = async () => {
    const apiEndpoint = `${ENV.API_BASE_URL}/users/`;
    const response = await fetch(apiEndpoint, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(text || translation.register.registration_failed);
    }

    const data = await response.json();
    const userDTO = new UserDTO(
      data.email,
      data.name,
      data.profilePicture,
      data.language,
      null,
      null,
      null,
      userId!,
      data.oauthUser,
    );
    if (userDTO.name) {
      setForm((prev) => ({
        ...prev,
        ["name"]: userDTO.name,
      }));
    }
    if (userDTO.email) {
      setForm((prev) => ({
        ...prev,
        ["email"]: userDTO.email,
      }));
    }
    if (userDTO.language) {
      setForm((prev) => ({
        ...prev,
        ["language"]: userDTO.language!,
      }));
    } else {
      const english = translation.languages.filter(
        (lng) => lng.code === "en",
      )[0];
      setForm((prev) => ({
        ...prev,
        ["language"]: english.code,
      }));
    }
    setForm((prev) => ({
      ...prev,
      ["oldPassword"]: "",
      ["password"]: "",
      ["repeatPassword"]: "",
    }));
    setPasswordExpanded(false);
  };

  const loadData = async () => {
    await handleLoad();
  };

  useEffect(() => {
    loadData();
  }, []);

  interface ChangeProfileFormState {
    name: string;
    email: string;
    language: string;
    oldPassword: string;
    password: string;
    repeatPassword: string;
    profilePicture?: File;
  }
  const [form, setForm] = useState<ChangeProfileFormState>({
    name: "",
    email: "",
    language: "",
    oldPassword: "",
    password: "",
    repeatPassword: "",
  });

  const handleProfilePictureChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (file.size > 5242880) {
      setError(translation.register.file_too_big);
      return;
    }
    if (error === translation.register.file_too_big) {
      setError(null);
    }
    setForm((prev) => ({
      ...prev,
      profilePicture: file,
    }));

    setAvatarPreview(URL.createObjectURL(file));
  };

  const handleChangeSelect = (event: SelectChangeEvent<string>) => {
    setLanguage(event.target.value as string);
    setForm((prev) => ({
      ...prev,
      ["language"]: event.target.value as string,
    }));
  };

  const handleChange =
    (field: keyof ChangeProfileFormState) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      switch (field) {
        case "email":
          setEmailError(!isValidEmail(event.target.value));
          break;
        case "name":
          setNameError(!isValidName(event.target.value));
          break;
        case "password":
          setPasswordError(!isValidPassword(event.target.value));
          break;
        case "repeatPassword":
          setRepeatPasswordError(!isRepeatPasswordEquals(event.target.value));
          break;
      }
      setForm((prev) => ({
        ...prev,
        [field]: event.target.value,
      }));
    };

  const handlePreview = () => {
    if (avatarPreview) {
      return avatarPreview;
    } else if (profilePictureUrl) {
      return `${ENV.BARE_URL_BASE}${profilePictureUrl}`;
    } else {
      return undefined;
    }
  };

  const isValidName = (name: string) => name.trim() !== "";

  const isValidEmail = (email: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const isValidPassword = (pass: string) =>
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/.test(pass);

  const isRepeatPasswordEquals = (rep: string) => form.password === rep;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const nameHasError = form.name.trim() === "";
    const emailHasError = form.email.trim() === "";
    const passwordHasError = form.password.trim() === "";
    const repeatPasswordHasError = form.repeatPassword.trim() === "";
    const oldPassworHasError = form.oldPassword.trim() === "";

    setNameTouched(true);
    setEmailTouched(true);
    setOldPasswordTouched(true);
    setPasswordTouched(true);
    setRepeatPasswordTouched(true);

    setNameError(nameHasError);
    setEmailError(emailHasError);
    setOldPasswordError(oldPassworHasError);
    setPasswordError(passwordHasError);
    setRepeatPasswordError(repeatPasswordHasError);

    const isOldPassNotSetAndAnyPasswordSet =
      oldPassworHasError && (!passwordHasError || !repeatPasswordHasError);
    const isOldPassSetAndAnyOtherPassNotSet =
      !oldPassworHasError && (passwordHasError || repeatPasswordError);

    const localUserNotOk =
      !isLocalPasswordSet &&
      (isOldPassNotSetAndAnyPasswordSet || isOldPassSetAndAnyOtherPassNotSet);
    const oauthUserNotOk =
      isLocalPasswordSet && (passwordHasError || repeatPasswordError);

    if (nameHasError || emailHasError || localUserNotOk || oauthUserNotOk) {
      setError(translation.profile.something_went_wrong);
      return;
    }

    if (form.password !== form.repeatPassword) {
      setError(translation.profile.pws_don_t_match);
      return;
    }

    setLoading(true);
    try {
      const oldPass =
        oldPassworHasError && passwordHasError && repeatPasswordHasError
          ? null
          : form.oldPassword;
      const newPass =
        oldPassworHasError && passwordHasError && repeatPasswordHasError
          ? null
          : form.password;
      const dto = new UserDTO(
        form.email,
        form.name,
        null,
        form.language,
        oldPass,
        newPass,
        null,
        userId!,
        false,
      );

      const formData = new FormData();

      formData.append(
        "userDTO",
        new Blob([JSON.stringify(dto)], { type: "application/json" }),
      );

      if (form.profilePicture) {
        formData.append("profilePicture", form.profilePicture);
      }

      const apiEndpoint = `${ENV.API_BASE_URL}/users/update`;

      const response = await fetch(apiEndpoint, {
        method: "PUT",
        body: formData,
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || translation.profile.something_went_wrong);
      }
      setSuccess(true);
      loadData();
      const data = await response.json();

      if (data.token) {
        const input = new LoginResponse(data.token);
        login(input);
      }
      setTimeout(() => {
        setSuccess(false);
      }, 10000);
    } catch (err: any) {
      setError(err.message ?? translation.profile.something_went_wrong);
    } finally {
      setLoading(false);
      setOldPasswordTouched(false);
      setPasswordTouched(false);
      setRepeatPasswordTouched(false);
    }
  };

  const [resendSuccess, setResendSuccess] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);

  const handleResendActivation = async () => {
    setResendLoading(true);
    setError(null);
    setResendSuccess(false);

    try {
      const params = new URLSearchParams({ email: form.email });
      const apiEndpoint = `${ENV.API_BASE_URL}/users/resend-activation?${params.toString()}`;

      const response = await fetch(apiEndpoint, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || "Failed to resend activation email.");
      }

      setResendSuccess(true);
      // Hide the success message after 5 seconds
      setTimeout(() => setResendSuccess(false), 5000);
    } catch (err: any) {
      setError(err.message || "An error occurred.");
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <Box
      component="form"
      noValidate
      onSubmit={handleSubmit}
      maxWidth={600}
      width="100%"
      mx="auto"
      mt={8}
      px={4}
      py={4}
      display="flex"
      flexDirection="column"
      gap={2}
    >
      <Typography
        variant="h4"
        textAlign="center"
        fontWeight={600}
        sx={{ mb: 3 }}
      >
        {translation.profile.title}
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {success && (
        <Alert severity="success">
          {translation.profile.changed_succesfully}
        </Alert>
      )}

      {userState === "INACTIVE" && (
        <Alert
          severity="warning"
          variant="outlined"
          action={
            <Button
              color="inherit"
              size="small"
              onClick={handleResendActivation}
              disabled={resendLoading}
            >
              {resendLoading ? translation.profile.sending : translation.profile.resend_now}
            </Button>
          }
        >
          {translation.profile.account_inactive}
        </Alert>
      )}

      {resendSuccess && (
        <Alert severity="info">
          {translation.profile.activation_sent}
        </Alert>
      )}

      <Box display="flex" justifyContent="center">
        <Stack>
          <Avatar src={handlePreview()} sx={{ width: 90, height: 90 }} />
          <IconButton color="primary" component="label">
            <input
              hidden
              accept="image/*"
              type="file"
              onChange={handleProfilePictureChange}
            />
            <PhotoCamera />
          </IconButton>
        </Stack>
      </Box>

      <TextField
        label={translation.profile.name}
        onBlur={() => {
          setNameTouched(true);
          setNameError(!isValidName(form.name));
        }}
        error={nameTouched && nameError}
        required
        value={form.name}
        fullWidth
        helperText={
          nameTouched && nameError ? translation.profile.name_required : ""
        }
        onChange={handleChange("name")}
      />

      <TextField
        label={translation.profile.email}
        onBlur={() => {
          setEmailTouched(true);
          setEmailError(!isValidEmail(form.email));
        }}
        error={emailTouched && emailError}
        helperText={
          emailTouched && emailError ? translation.profile.email_required : ""
        }
        type="email"
        required
        value={form.email}
        fullWidth
        onChange={handleChange("email")}
      />

      <FormControl variant="outlined" fullWidth>
        <InputLabel id="language-select-label">Language</InputLabel>
        <Select
          labelId="language-select-label"
          value={language}
          onChange={handleChangeSelect}
          label="Language"
        >
          {translation.languages.map((lng) => (
            <MenuItem value={lng.code}>{lng.name}</MenuItem>
          ))}
        </Select>
      </FormControl>
      <Divider sx={{ my: 3 }} />

      <Accordion
        expanded={passwordExpanded}
        onChange={(_, expanded) => setPasswordExpanded(expanded)}
        elevation={0}
        sx={{
          border: "1px solid",
          borderColor: "divider",
          borderRadius: 2,
          "&:before": { display: "none" },
        }}
      >
        <AccordionSummary
          expandIcon={<ExpandMore />}
          sx={{
            fontWeight: 600,
          }}
        >
          <Typography fontWeight={600}>
            {isLocalPasswordSet
              ? translation.profile.set_password
              : translation.profile.change_password}
          </Typography>
        </AccordionSummary>

        <AccordionDetails>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {isLocalPasswordSet
              ? translation.profile.set_password_title
              : translation.profile.change_password_title}
          </Typography>

          <Stack gap={2}>
            {!isLocalPasswordSet && (
              <TextField
                label={translation.profile.old_password}
                type="password"
                required={passwordExpanded}
                value={form.oldPassword}
                onBlur={() => {
                  setPasswordTouched(true);
                  setPasswordError(!isValidPassword(form.oldPassword));
                }}
                error={
                  passwordExpanded && oldPasswordTouched && oldPasswordError
                }
                helperText={
                  passwordExpanded && oldPasswordTouched && oldPasswordError
                    ? translation.profile.pass_form
                    : ""
                }
                fullWidth
                onChange={handleChange("oldPassword")}
              />
            )}

            <TextField
              label={translation.profile.new_password}
              type="password"
              required={passwordExpanded}
              value={form.password}
              onBlur={() => {
                setPasswordTouched(true);
                setPasswordError(!isValidPassword(form.password));
              }}
              error={passwordExpanded && passwordTouched && passwordError}
              helperText={
                passwordExpanded && passwordTouched && passwordError
                  ? translation.profile.pass_form
                  : ""
              }
              fullWidth
              onChange={handleChange("password")}
            />

            <TextField
              label={translation.profile.repeat_password}
              type="password"
              required={passwordExpanded}
              value={form.repeatPassword}
              onBlur={() => {
                setRepeatPasswordTouched(true);
                setRepeatPasswordError(form.repeatPassword !== form.password);
              }}
              error={
                passwordExpanded && repeatPasswordTouched && repeatPasswordError
              }
              helperText={
                passwordExpanded && repeatPasswordTouched && repeatPasswordError
                  ? translation.profile.repeat_pass_helper
                  : ""
              }
              fullWidth
              onChange={handleChange("repeatPassword")}
            />
          </Stack>
        </AccordionDetails>
      </Accordion>

      <Stack direction="column" gap={2} mt={2}>
        <Button
          fullWidth
          type="submit"
          variant="contained"
          size="large"
          sx={{
            textTransform: "none",
            fontWeight: 600,
            fontSize: 20,
          }}
          disabled={loading}
        >
          {translation.profile.save}
        </Button>

        {/* ADDED: Delete Account Section */}
        <Divider sx={{ my: 2 }}>
          <Typography variant="body2" color="text.secondary">
            {translation.profile.danger_zone}
          </Typography>
        </Divider>

        <Button
          fullWidth
          variant="outlined"
          color="error"
          startIcon={<DeleteForever />}
          onClick={() => setOpenDeleteDialog(true)}
          sx={{ textTransform: "none" }}
        >
          {translation.profile.delete_account}
        </Button>
      </Stack>

      {/* Confirmation Dialog */}
      <Dialog
        open={openDeleteDialog}
        onClose={() => !loading && setOpenDeleteDialog(false)}
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-description"
      >
        <DialogTitle
          id="delete-dialog-title"
          sx={{ fontWeight: 600, color: "error.main" }}
        >
          {translation.profile.delete_question}
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="delete-dialog-description">
            {translation.profile.delete_dialog_description}
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            onClick={() => setOpenDeleteDialog(false)}
            disabled={loading}
            variant="text"
            sx={{ color: "text.secondary" }}
          >
            {translation.profile.cancel}
          </Button>
          <Button
            onClick={handleDeleteAccount}
            disabled={loading}
            variant="contained"
            color="error"
            autoFocus
          >
            {loading
              ? translation.profile.deleting
              : translation.profile.delete_confirmation}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default ProfilePage;
