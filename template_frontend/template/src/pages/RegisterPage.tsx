import {
  Alert,
  Avatar,
  Box,
  Button,
  IconButton,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useState } from "react";
import CreateUserRequest from "../models/dto/CreateUserRequest";
import { ENV } from "../config/env";
import { PhotoCamera } from "@mui/icons-material";
import { useI18n } from "../i18n/i18nContext";
import { useNavigate } from "react-router-dom";

interface RegistrationFormState {
  name: string;
  email: string;
  password: string;
  repeatPassword: string;
  profilePicture?: File;
}

const RegisterPage: React.FC = () => {
  const { translation } = useI18n();
  const navigate = useNavigate();

  const [form, setForm] = useState<RegistrationFormState>({
    name: "",
    email: "",
    password: "",
    repeatPassword: "",
  });

  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const [nameError, setNameError] = useState(false);
  const [nameTouched, setNameTouched] = useState(false);
  const [emailError, setEmailError] = useState(false);
  const [emailTouched, setEmailTouched] = useState(false);
  const [passwordError, setPasswordError] = useState(false);
  const [passwordTouched, setPasswordTouched] = useState(false);
  const [repeatPasswordError, setRepeatPasswordError] = useState(false);
  const [repeatPasswordTouched, setRepeatPasswordTouched] = useState(false);

  const handleChange =
    (field: keyof RegistrationFormState) =>
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const nameHasError = form.name.trim() === "";
    const emailHasError = form.email.trim() === "";
    const passwordHasError = form.password.trim() === "";
    const repeatPasswordHasError = form.repeatPassword.trim() === "";

    setNameTouched(true);
    setEmailTouched(true);
    setPasswordTouched(true);
    setRepeatPasswordTouched(true);

    setNameError(nameHasError);
    setEmailError(emailHasError);
    setPasswordError(passwordHasError);
    setRepeatPasswordError(repeatPasswordHasError);

    if (
      nameHasError ||
      emailHasError ||
      passwordHasError ||
      repeatPasswordHasError
    ) {
      setError(translation.register.something_went_wrong);
      return;
    }

    if (form.password !== form.repeatPassword) {
      setError(translation.register.pws_don_t_match);
      return;
    }

    setLoading(true);

    try {
      const dto = new CreateUserRequest(form.name, form.email, form.password, [
        "ROLE_USER",
      ]);

      const formData = new FormData();
      formData.append("name", dto.name);
      formData.append("email", dto.email);
      formData.append("password", dto.password);

      dto.roles.forEach((role) => {
        formData.append("roles", role);
      });

      if (form.profilePicture) {
        formData.append("profilePicture", form.profilePicture);
      }

      const apiEndpoint = `${ENV.API_BASE_URL}/users/create`;

      const response = await fetch(apiEndpoint, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || translation.register.registration_failed);
      }

      setSuccess(true);
      setForm(() => ({
        name: "",
        email: "",
        password: "",
        repeatPassword: "",
        profilePicture: undefined,
      }));
      setAvatarPreview(null);
      setTimeout(() => {
        navigate("/");
        setSuccess(false);
      }, 5000);
    } catch (err: any) {
      try {
        const errorData = JSON.parse(err.message);
        if (errorData.message === "ACCOUNT_SOFT_DELETED") {
          navigate(`/recover?email=${encodeURIComponent(form.email)}`);
          return;
        }
      } catch (err: any) {
        // ignore
      }
      setError(err.message ?? "Something went wrong");
      setSuccess(false);
    } finally {
      setLoading(false);
    }
  };

  const isValidName = (name: string) => name.trim() !== "";

  const isValidEmail = (email: string) =>
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

  const isValidPassword = (pass: string) =>
    /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,}$/.test(pass);

  const isRepeatPasswordEquals = (rep: string) => form.password === rep;

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
        variant="h5"
        textAlign="center"
        fontWeight={600}
        sx={{ mb: 3 }}
      >
        {translation.register.create_account}
      </Typography>

      {error && <Alert severity="error">{error}</Alert>}
      {success && (
        <Alert severity="success">
          {translation.register.registration_successful}
        </Alert>
      )}

      <Box display="flex" justifyContent="center">
        <Stack>
          <Avatar
            src={avatarPreview ?? undefined}
            sx={{ width: 90, height: 90 }}
          />
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
        label={translation.register.name}
        onBlur={() => {
          setNameTouched(true);
          setNameError(!isValidName(form.name));
        }}
        error={nameTouched && nameError}
        required
        value={form.name}
        fullWidth
        helperText={
          nameTouched && nameError ? translation.register.name_required : ""
        }
        onChange={handleChange("name")}
      />

      <TextField
        label={translation.register.email}
        onBlur={() => {
          setEmailTouched(true);
          setEmailError(!isValidEmail(form.email));
        }}
        error={emailTouched && emailError}
        helperText={
          emailTouched && emailError ? translation.register.email_required : ""
        }
        type="email"
        required
        value={form.email}
        fullWidth
        onChange={handleChange("email")}
      />

      <TextField
        label={translation.register.password}
        onBlur={() => {
          setPasswordTouched(true);
          setPasswordError(!isValidPassword(form.password));
        }}
        error={passwordTouched && passwordError}
        helperText={
          passwordTouched && passwordError ? translation.register.pass_form : ""
        }
        type="password"
        required
        value={form.password}
        fullWidth
        onChange={handleChange("password")}
      />

      <TextField
        label={translation.register.repeat_password}
        onFocus={() => {
          setRepeatPasswordTouched(true);
          setRepeatPasswordError(!isRepeatPasswordEquals(form.repeatPassword));
        }}
        error={repeatPasswordTouched && repeatPasswordError}
        helperText={
          repeatPasswordTouched && repeatPasswordError
            ? translation.register.repeat_pass_helper
            : ""
        }
        type="password"
        required
        value={form.repeatPassword}
        fullWidth
        onChange={handleChange("repeatPassword")}
      />

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
        {loading
          ? translation.register.creating_account
          : translation.register.register}
      </Button>
    </Box>
  );
};

export default RegisterPage;
