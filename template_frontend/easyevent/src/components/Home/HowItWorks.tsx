import { Box, Container, Grid, Stack, Typography } from "@mui/material";
import EventAvailableIcon from "@mui/icons-material/EventAvailable";
import ShareIcon from "@mui/icons-material/Share";
import MailOutlineIcon from "@mui/icons-material/MailOutline";
import { useI18n } from "../../i18n/i18nContext";

export default function HowItWorks() {
  const { translation } = useI18n();

  const steps = [
    {
      icon: <EventAvailableIcon fontSize="large" />,
      title: translation.how_it_works.create,
      description: translation.how_it_works.create_description,
    },
    {
      icon: <ShareIcon fontSize="large" />,
      title: translation.how_it_works.share,
      description: translation.how_it_works.share_description,
    },
    {
      icon: <MailOutlineIcon fontSize="large" />,
      title: translation.how_it_works.get_notified,
      description: translation.how_it_works.get_notified_description,
    },
  ];

  return (
    <Box component="section" sx={{ minHeight: "100vh" }}>
      <Container maxWidth="lg">
        <Stack spacing={6} alignItems="center">
          {/* Header */}
          <Stack spacing={2} textAlign="center">
            <Typography variant="h3" fontWeight={700}>
              {translation.how_it_works.how_it_works}
            </Typography>
            <Typography variant="h6" color="text.secondary">
              {translation.how_it_works.how_it_works_description}
            </Typography>
          </Stack>

          {/* Steps */}
          <Grid container spacing={4}>
            {steps.map((step, index) => (
              <Grid
                key={index}
                sx={{ display: { xs: 12, md: 4, width: "100%" } }}
              >
                <Stack spacing={2} alignItems="center" textAlign="center">
                  <Box
                    sx={{
                      width: 64,
                      height: 64,
                      borderRadius: "50%",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      bgcolor: "primary.main",
                      color: "primary.contrastText",
                    }}
                  >
                    {step.icon}
                  </Box>

                  <Typography variant="h6" fontWeight={600}>
                    {step.title}
                  </Typography>

                  <Typography color="text.secondary">
                    {step.description}
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
