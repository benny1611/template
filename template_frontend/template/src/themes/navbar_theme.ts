import { createTheme } from "@mui/material";

export const theme = createTheme({
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: ({ theme: muiTheme }) => ({
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          backgroundColor:
            muiTheme.palette.mode === 'dark'
              ? 'rgba(0,0,0,0.5)'
              : 'rgba(255,255,255,0.12)',
          borderBottom: '1px solid rgba(255,255,255,0.2)',
          color: muiTheme.palette.text.primary,
        }),
      },
    },
  },
})