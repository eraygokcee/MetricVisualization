// src/Header.js

import React from 'react';
import { AppBar, Toolbar, Box } from '@mui/material';

export default function Header() {
  return (
    // AppBar, header oluşturmak için kullanılan şık bir MUI bileşenidir.
    <AppBar position="static" sx={{ backgroundColor: '#fff', boxShadow: 2 }}>
      <Toolbar sx={{ justifyContent: 'center' }}>
        <Box 
          component="img"
          src="/banner.png"
          alt="MetricVis Banner"
          sx={{
            height: 100, // Banner'ınızın yüksekliğini buradan ayarlayabilirsiniz
            width: 'auto',
            py: 1, // Üstten ve alttan küçük bir boşluk (padding)
          }}
        />
      </Toolbar>
    </AppBar>
  );
}