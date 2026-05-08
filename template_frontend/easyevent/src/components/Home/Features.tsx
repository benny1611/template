import { useI18n } from "../../i18n/i18nContext";
import LoginIcon from "@mui/icons-material/Login";
import MailOutlineIcon from "@mui/icons-material/MailOutline";
import DashboardCustomizeIcon from "@mui/icons-material/DashboardCustomize";
import FlashOnIcon from "@mui/icons-material/FlashOn";
import { Box, Container, Grid, Stack, Typography } from "@mui/material";

export default function Features() {
  const { translation } = useI18n();

  const features = [
    {
      icon: <LoginIcon />,
      title: translation.features.no_account_title,
      description: translation.features.no_account_description,
    },
    {
      icon: <MailOutlineIcon />,
      title: translation.features.email_title,
      description: translation.features.email_description,
    },
    {
      icon: <DashboardCustomizeIcon />,
      title: translation.features.manage_title,
      description: translation.features.manage_description,
    },
    {
      icon: <FlashOnIcon />,
      title: translation.features.fast_title,
      description: translation.features.fast_description,
    },
  ];

  return (
    <Box
      component="section"
      sx={{ py: { xs: 8 }, pb: { md: 12 }, pt: { md: 0 } }}
    >
      <Container maxWidth="lg">
        <Stack spacing={6}>
          {/* Header */}
          <Stack spacing={2} textAlign="center" alignItems="center">
            <Typography variant="h3" fontWeight={700}>
              {translation.features.title}
            </Typography>
            <Typography variant="h6" color="text.secondary" maxWidth={640}>
              {translation.features.subtitle}
            </Typography>
          </Stack>

          {/* Feature Grid */}
          <Grid container spacing={4} alignItems="stretch">
            {features.map((feature, index) => (
              <Grid key={index} size={{ xs: 12, sm: 6, md: 3 }} display="flex">
                <Stack
                  spacing={2}
                  sx={{
                    height: "100%",
                    p: 3,
                    borderRadius: 3,
                    border: "1px solid",
                    borderColor: "divider",
                    transition: "all 0.3s ease",
                    alignItems: "center",
                    textAlign: "center",
                    "&:hover": {
                      boxShadow: 3,
                      transform: "translateY(-4px)",
                    },
                    cursor: "pointer",
                  }}
                >
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: 2,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      bgcolor: "primary.main",
                      color: "primary.contrastText",
                      mb: 1,
                    }}
                  >
                    {feature.icon}
                  </Box>

                  <Typography variant="h6" fontWeight={600}>
                    {feature.title}
                  </Typography>

                  <Typography color="text.secondary">
                    {feature.description}
                  </Typography>
                </Stack>
              </Grid>
            ))}
          </Grid>
        </Stack>
      </Container>
    </Box>
  );
}
