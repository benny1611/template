import {
  Box,
  Container,
  Grid,
  Link,
  Stack,
  SvgIcon,
  Typography,
} from "@mui/material";
import { useI18n } from "../i18n/i18nContext";
import LogoIcon from "../assets/react.svg?react";

export default function Footer() {
  const { translation } = useI18n();

  return (
    <Box
      component="footer"
      sx={{
        mt: 8,
        py: 6,
        borderTop: "1px solid",
        borderColor: "divider",
        bgcolor: "background.paper",
      }}
    >
      <Container maxWidth="lg">
        <Grid container spacing={4}>
          {/* Brand */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Stack spacing={2}>
              <Stack direction="row" spacing={1} alignItems="center">
                <SvgIcon
                  component={LogoIcon}
                  inheritViewBox
                  sx={{ fontSize: 32 }}
                />
                <Typography variant="h6" fontWeight={700}>
                  {translation.appName}
                </Typography>
              </Stack>

              <Typography color="text.secondary" maxWidth={300}>
                {translation.footer.tagline}
              </Typography>
            </Stack>
          </Grid>

          {/* Links */}
          <Grid size={{ xs: 6, md: 4 }}>
            <Stack spacing={1}>
              <Typography fontWeight={600}>
                {translation.footer.product}
              </Typography>

              <Link href="#" underline="hover" color="text.secondary">
                {translation.footer.privacy}
              </Link>
              <Link href="#" underline="hover" color="text.secondary">
                {translation.footer.terms}
              </Link>
            </Stack>
          </Grid>
        </Grid>

        {/* Bottom bar */}
        <Box
          sx={{
            mt: 6,
            pt: 3,
            borderTop: "1px solid",
            borderColor: "divider",
            textAlign: "center",
          }}
        >
          <Typography variant="body2" color="text.secondary">
            © {new Date().getFullYear()} {translation.footer.rights}
          </Typography>
        </Box>
      </Container>
    </Box>
  );
}
