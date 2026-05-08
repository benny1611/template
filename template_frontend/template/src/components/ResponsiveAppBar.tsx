import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import IconButton from "@mui/material/IconButton";
import Typography from "@mui/material/Typography";
import Menu from "@mui/material/Menu";
import MenuIcon from "@mui/icons-material/Menu";
import Container from "@mui/material/Container";
import Avatar from "@mui/material/Avatar";
import Button from "@mui/material/Button";
import Tooltip from "@mui/material/Tooltip";
import MenuItem from "@mui/material/MenuItem";
import { useI18n } from "../i18n/i18nContext";
import SvgIcon from "@mui/icons-material/Menu";
import LogoIcon from "../assets/react.svg?react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { Link } from "@mui/material";
import { useAuth } from "../auth/AuthContext";
import { ENV } from "../config/env";

function ResponsiveAppBar() {
  const { translation } = useI18n();
  const { isAuthenticated, roles, logout, profilePictureUrl, username } =
    useAuth();
  const navigate = useNavigate();
  const [anchorElNav, setAnchorElNav] = React.useState<null | HTMLElement>(
    null,
  );
  const [anchorElUser, setAnchorElUser] = React.useState<null | HTMLElement>(
    null,
  );

  const handleOpenNavMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorElNav(event.currentTarget);
  };
  const handleOpenUserMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorElUser(event.currentTarget);
  };

  const handleCloseNavMenu = () => {
    setAnchorElNav(null);
  };

  const handleCloseUserMenu = () => {
    setAnchorElUser(null);
  };

  const logoutAndCloseMenu = () => {
    logout();
    setAnchorElUser(null);
  };

  const goToPathAndCloseMenu = (path: string) => {
    navigate("/" + path);
    setAnchorElUser(null);
  };

  const handleProfile = () => {
    goToPathAndCloseMenu("profile");
  };

  const handleAdmin = () => {
    goToPathAndCloseMenu("admin");
  };

  return (
    <AppBar position="fixed" color="default">
      <Container maxWidth="xl">
        <Toolbar disableGutters>
          <Link component={RouterLink} to="/">
            <SvgIcon
              component={LogoIcon}
              inheritViewBox
              sx={{ display: { xs: "none", md: "flex" }, mr: 1 }}
            />
          </Link>
          <Typography
            variant="h6"
            noWrap
            component={RouterLink}
            to="/"
            sx={{
              mr: 2,
              display: { xs: "none", md: "flex" },
              fontFamily: "monospace",
              fontWeight: 700,
              letterSpacing: ".3rem",
              color: "inherit",
              textDecoration: "none",
            }}
          >
            {translation.appName}
          </Typography>

          <Box sx={{ flexGrow: 1, display: { xs: "flex", md: "none" } }}>
            <IconButton
              size="large"
              aria-label="account of current user"
              aria-controls="menu-appbar"
              aria-haspopup="true"
              onClick={handleOpenNavMenu}
              color="inherit"
            >
              <MenuIcon />
            </IconButton>
            <Menu
              id="menu-appbar"
              anchorEl={anchorElNav}
              anchorOrigin={{
                vertical: "bottom",
                horizontal: "left",
              }}
              keepMounted
              transformOrigin={{
                vertical: "top",
                horizontal: "left",
              }}
              open={Boolean(anchorElNav)}
              onClose={handleCloseNavMenu}
              sx={{ display: { xs: "block", md: "none" } }}
            >
              <MenuItem key={translation.nav.home} onClick={handleCloseNavMenu}>
                <Typography
                  component={RouterLink}
                  to="/"
                  sx={{ textAlign: "center" }}
                >
                  {translation.nav.home}
                </Typography>
              </MenuItem>
              <MenuItem
                key={translation.nav.create}
                onClick={handleCloseNavMenu}
              >
                <Typography
                  component={RouterLink}
                  to="/create"
                  sx={{ textAlign: "center" }}
                >
                  {translation.nav.create}
                </Typography>
              </MenuItem>
              <MenuItem key={translation.nav.join} onClick={handleCloseNavMenu}>
                <Typography
                  component={RouterLink}
                  to="/join"
                  sx={{ textAlign: "center" }}
                >
                  {translation.nav.join}
                </Typography>
              </MenuItem>
              {isAuthenticated ? null : (
                <MenuItem
                  key={translation.nav.login}
                  onClick={handleCloseNavMenu}
                >
                  <Typography
                    component={RouterLink}
                    to="/login"
                    sx={{ textAlign: "center" }}
                  >
                    {translation.nav.login}
                  </Typography>
                </MenuItem>
              )}
            </Menu>
          </Box>
          <SvgIcon
            component={LogoIcon}
            inheritViewBox
            sx={{ display: { xs: "flex", md: "none" }, mr: 1 }}
          />
          <Typography
            variant="h5"
            noWrap
            component="a"
            href="#app-bar-with-responsive-menu"
            sx={{
              mr: 2,
              display: { xs: "flex", md: "none" },
              flexGrow: 1,
              fontFamily: "monospace",
              fontWeight: 700,
              letterSpacing: ".2rem",
              color: "inherit",
              textDecoration: "none",
            }}
          >
            {translation.appName}
          </Typography>
          <Box sx={{ flexGrow: 1, display: { xs: "none", md: "flex" } }}>
            <Button
              key={translation.nav.home}
              component={RouterLink}
              to="/"
              onClick={handleCloseNavMenu}
              sx={{ my: 2, color: "inherit", display: "block" }}
            >
              {translation.nav.home}
            </Button>
            <Button
              key={translation.nav.create}
              component={RouterLink}
              to="/create"
              onClick={handleCloseNavMenu}
              sx={{ my: 2, color: "inherit", display: "block" }}
            >
              {translation.nav.create}
            </Button>
            <Button
              key={translation.nav.join}
              component={RouterLink}
              to="/join"
              onClick={handleCloseNavMenu}
              sx={{
                my: 2,
                color: "inherit",
                display: "block",
                justifyContent: "center",
              }}
            >
              {translation.nav.join}
            </Button>
            {isAuthenticated ? null : (
              <Button
                key={translation.nav.login}
                component={RouterLink}
                to="/login"
                onClick={handleCloseNavMenu}
                sx={{ my: 2, color: "inherit", display: "block" }}
              >
                {translation.nav.login}
              </Button>
            )}
          </Box>
          {isAuthenticated ? (
            <Box sx={{ flexGrow: 0 }}>
              <Tooltip title="Open settings">
                <IconButton onClick={handleOpenUserMenu} sx={{ p: 0 }}>
                  <Avatar
                    alt={username}
                    src={
                      profilePictureUrl
                        ? `${ENV.BARE_URL_BASE}${profilePictureUrl}`
                        : undefined
                    }
                  >
                    {" "}
                    {username?.charAt(0).toUpperCase()}{" "}
                  </Avatar>
                </IconButton>
              </Tooltip>
              <Menu
                sx={{ mt: "45px" }}
                id="menu-appbar"
                anchorEl={anchorElUser}
                anchorOrigin={{
                  vertical: "top",
                  horizontal: "right",
                }}
                keepMounted
                transformOrigin={{
                  vertical: "top",
                  horizontal: "right",
                }}
                open={Boolean(anchorElUser)}
                onClose={handleCloseUserMenu}
              >
                <MenuItem key={translation.nav.profile} onClick={handleProfile}>
                  <Typography sx={{ textAlign: "center" }}>
                    {translation.nav.profile}
                  </Typography>
                </MenuItem>
                {
                  (roles.includes("ROLE_ADMIN") || roles.includes("ROLE_SUPER_ADMIN")) &&
                  <MenuItem key={translation.nav.admin} onClick={handleAdmin}>
                    <Typography sx={{ textAlign: "center" }}>
                      {translation.nav.admin}
                    </Typography>
                  </MenuItem>
                }
                <MenuItem
                  key={translation.nav.account}
                  onClick={handleCloseUserMenu}
                >
                  <Typography sx={{ textAlign: "center" }}>
                    {translation.nav.account}
                  </Typography>
                </MenuItem>
                <MenuItem
                  key={translation.nav.dashboard}
                  onClick={handleCloseUserMenu}
                >
                  <Typography sx={{ textAlign: "center" }}>
                    {translation.nav.dashboard}
                  </Typography>
                </MenuItem>
                <MenuItem
                  key={translation.nav.logout}
                  onClick={logoutAndCloseMenu}
                >
                  <Typography sx={{ textAlign: "center" }}>
                    {translation.nav.logout}
                  </Typography>
                </MenuItem>
              </Menu>
            </Box>
          ) : null}
        </Toolbar>
      </Container>
    </AppBar>
  );
}
export default ResponsiveAppBar;
