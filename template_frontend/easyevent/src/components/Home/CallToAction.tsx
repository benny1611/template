import { Box, Button, Container, Stack, Typography } from "@mui/material";
import { useI18n } from "../../i18n/i18nContext";

export default function CallToAction() {
  const { translation } = useI18n();

  return (
    <Box
      component="section"
      minHeight="100vh"
      alignContent="center"
      sx={{
        py: { xs: 8, md: 12 },
        background: (theme) =>
          `linear-gradient(135deg, ${theme.palette.primary.main}, ${theme.palette.primary.dark})`,
        color: "primary.contrastText",
      }}
    >
      <Container maxWidth="md">
        <Stack spacing={4} textAlign="center" alignItems="center">
          <Typography variant="h3" fontWeight={700}>
            {translation.cta.title}
          </Typography>

          <Typography variant="h6" sx={{ opacity: 0.9, maxWidth: 600 }}>
            {translation.cta.subtitle}
          </Typography>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
            <Button
              size="large"
              variant="contained"
              color="secondary"
              sx={{
                textTransform: "none",
                fontWeight: 600,
                fontSize: 20,
              }}
            >
              {translation.cta.create}
            </Button>
            <Button
              size="large"
              variant="outlined"
              sx={{
                borderColor: "primary.contrastText",
                color: "primary.contrastText",
                "&:hover": {
                  borderColor: "primary.contrastText",
                  bgcolor: "rgba(255,255,255,0.1)",
                },
                textTransform: "none",
                fontWeight: 600,
                fontSize: 20,
                borderWidth: "3px",
              }}
            >
              {translation.cta.join}
            </Button>
          </Stack>
        </Stack>
      </Container>
    </Box>
  );
}
