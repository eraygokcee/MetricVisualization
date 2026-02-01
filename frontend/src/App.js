// src/App.js
import React, { useState } from 'react';
import { Button, Box, Typography, Container } from '@mui/material';
import ProjectAnalysis from './ProjectAnalysis';
import ClassAnalysis   from './ClassAnalysis';
import HistoryList     from './HistoryList';
import Header from './Header'; // YENİ: Header bileşenini import ediyoruz


const BASE_URL = 'http://localhost:8080/api/metrics';

function App() {
  const [step, setStep]     = useState('home');
  const [result, setResult] = useState(null);

  const handleNewAnalysis = () => {
    setResult(null);
    setStep('new');
  };

  const handleHistory = () => {
    setResult(null);
    setStep('history');
  };

  const handleSelectProject = async () => {
    const folderPath = await window.electronAPI.openProject();
    if (!folderPath) return;
    try {
      const encoded = encodeURIComponent(folderPath);
      const resp = await fetch(`${BASE_URL}/project?targetPath=${encoded}`, { method: 'POST' });
      if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
      const data = await resp.json();
      setResult(data);
      setStep('home');
    } catch (err) {
      setResult({ error: err.message });
      setStep('home');
    }
  };

  const handleSelectJar = async () => {
    const folderPath = await window.electronAPI.openJar();
    if (!folderPath) return;
    try {
      const encoded = encodeURIComponent(folderPath);
      const resp = await fetch(`${BASE_URL}/project?targetPath=${encoded}`, { method: 'POST' });
      if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
      const data = await resp.json();
      setResult(data);
      setStep('home');
    } catch (err) {
      setResult({ error: err.message });
      setStep('home');
    }
  };
  
  const handleSelectClass = async () => {
    const filePath = await window.electronAPI.openClass();
    if (!filePath) return;
    try {
      const encoded = encodeURIComponent(filePath);
      const resp = await fetch(`${BASE_URL}/class?path=${encoded}`, { method: 'POST' });
      if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
      const data = await resp.json();
      setResult(data);
      setStep('home');
    } catch (err) {
      setResult({ error: err.message });
      setStep('home');
    }
  };

  const handleShowDetails = async (record) => {
    try {
      const resp = await fetch(`${BASE_URL}/${record.id}`);
      if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
      const data = await resp.json();
      setResult(data);
      setStep('home');
    } catch (err) {
      setResult({ error: err.message });
      setStep('home');
    }
  };

  const goHome = () => {
    setResult(null);
    setStep('home');
  }

  return (
    <Box sx={{ backgroundColor: '#f4f6f8', minHeight: '100vh' }}>
      {/* YENİ: Header bileşeni her zaman en üstte gösteriliyor */}
      <Header />
        <Container maxWidth="lg">
          <Box 
            display="flex"
            flexDirection="column"
            justifyContent="center"
            alignItems="center"
            minHeight="100vh"
             // Yukarıdan ve aşağıdan boşluk
          >
            {/* Ana Menü: Sadece home ekranındaysa ve sonuç yoksa göster */}
            {step === 'home' && !result && (
              <Box display="flex" gap={2} flexDirection="column" alignItems="center">
                <Button variant="contained" onClick={handleNewAnalysis} sx={{ width: 250 }}>
                    Start New Analysis
                </Button>
                <Button variant="outlined" onClick={handleHistory} sx={{ width: 250 }}>
                    List Past Records
                </Button>
              </Box>
            )}

            {step === 'new' && (
              <Box display="flex" flexDirection="column" gap={2} alignItems="center" width="100%">
                <Typography variant="h6" mb={2}>Select Analysis Type</Typography>
                <Button variant="contained" onClick={handleSelectJar} sx={{ width: 250 }}>
                  Jar
                </Button>
                <Button variant="contained" onClick={handleSelectProject} sx={{ width: 250 }}>
                  Project
                </Button>
                <Button variant="contained" onClick={handleSelectClass} sx={{ width: 250 }}>
                  Class
                </Button>
                <Button variant="text" onClick={goHome} sx={{ mt: 2 }}>
                  Back
                </Button>
              </Box>
            )}
            
            {step === 'history' && (
              <HistoryList onShowDetails={handleShowDetails} onBack={goHome} />
            )}

            {result && (result.type === 'PROJECT' || result.type === 'JAR') && (
              <ProjectAnalysis data={result} onBack={goHome} />
            )}
            {result && result.type === 'CLASS' && (
              <ClassAnalysis data={result} onBack={goHome} />
            )}
          </Box>
        </Container>
      </Box>
  );
}

export default App;