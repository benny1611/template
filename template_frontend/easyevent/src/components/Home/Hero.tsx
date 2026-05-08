import { Box, Button, Container, Stack, Typography } from "@mui/material";
import SvgIcon from "@mui/material/SvgIcon";
import LogoIcon from "../../assets/react.svg?react";
import { useI18n } from "../../i18n/i18nContext";
import backgroundImage from "../../assets/artist-3480274.jpg";

export default function Hero() {
  const { translation } = useI18n();

  return (
    <Box
      component="section"
      sx={{
        minHeight: (theme) =>
          `calc(100vh - ${theme.mixins.toolbar.minHeight}px)`,
        display: "flex",
        flexDirection: "column",
      }}
    >
      {/* Image Section */}
      <Box
        sx={{
          position: "relative",
          height: "50%",
          minHeight: {
            xs: 250,
            sm: 300,
            md: 300,
            lg: 400,
            xl: 500,
          },
          backgroundImage: `url(${backgroundImage})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      >
        {/* Overlay */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            textAlign: "center",
            bgcolor: "rgba(0,0,0,0.35)",
          }}
        >
          <Stack spacing={2} alignItems="center">
            <SvgIcon
              component={LogoIcon}
              inheritViewBox
              sx={{ fontSize: 96, color: "white" }}
            />

            <Typography
              variant="h2"
              fontWeight={700}
              letterSpacing="-0.02em"
              sx={{
                color: "white",
                textShadow: "0 4px 12px rgba(0,0,0,0.6)",
              }}
            >
              {translation.appName}
            </Typography>
          </Stack>
        </Box>
      </Box>

      {/* Content Section */}
      <Container
        maxWidth="sm"
        sx={{
          py: { xs: 4, md: 6 },
          textAlign: "center",
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <Stack spacing={3} alignItems="center">
          <Typography variant="h6" color="text.secondary">
            {translation.hero.mantra}
          </Typography>

          <Stack spacing={2} width="100%">
            <Button
              size="large"
              variant="contained"
              fullWidth
              sx={{
                textTransform: "none",
                fontWeight: 600,
                fontSize: 20,
              }}
            >
              {translation.hero.create}
            </Button>

            <Button
              size="large"
              variant="outlined"
              fullWidth
              sx={{
                textTransform: "none",
                fontWeight: 600,
                fontSize: 20,
                borderWidth: "3px",
              }}
            >
              {translation.hero.join}
            </Button>
          </Stack>
        </Stack>
      </Container>
    </Box>
  );
}
