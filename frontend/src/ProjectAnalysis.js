import React, { useState } from 'react';
import { Box, Typography, Button, FormGroup, FormControlLabel, Checkbox, IconButton, Paper } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { DataGrid, GridToolbar } from '@mui/x-data-grid';

const BASE_URL = 'http://localhost:8080/api/metrics';
const METRICS   = ['tcc','wmc','lcom','dit','cbo','maxCyclo','avgCyclo'];

export default function ClassAnalysis({ data, onBack }) {
  const { id, targetPath, createdAt, results } = data;
  const fileName = targetPath.split(/[/\\]/).pop();
  const rows = results.map((r, index) => ({ ...r, id: r.id || index }));
  const columns = [
    { field: 'className', headerName: 'Class Name', flex: 1, minWidth: 200 },
    { field: 'tcc', headerName: 'TCC', type: 'number', flex: 0.5 },
    { field: 'wmc', headerName: 'WMC', type: 'number', flex: 0.5 },
    { field: 'lcom', headerName: 'LCOM', type: 'number', flex: 0.5 },
    { field: 'dit', headerName: 'DIT', type: 'number', flex: 0.5 },
    { field: 'cbo', headerName: 'CBO', type: 'number', flex: 0.5 },
     { field: 'maxCyclo',        headerName: 'MAXCYCLO',        type: 'number', flex: 0.5 },
    { field: 'avgCyclo',        headerName: 'AVGCYCLO',        type: 'number', flex: 0.5 },
  ];

  const [selectedMetrics, setSelectedMetrics] = useState([]);

  const toggleMetric = (m) => {
    if (selectedMetrics.includes(m)) {
      setSelectedMetrics(selectedMetrics.filter(x => x !== m));
    } else if (selectedMetrics.length < 3) {
      setSelectedMetrics([...selectedMetrics, m]);
    }
  };

  const handleVisualize = async () => {
    if (selectedMetrics.length !== 3) return;
    try {
      const resp = await fetch(`${BASE_URL}/visualize`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id, metrics: selectedMetrics })
      });
      if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
      console.log('🔔 Görselleştirme için gönderildi:', await resp.json());
    } catch (err) {
      console.error('Görselleştirme hatası:', err);
    }
  };

  return (
    <Paper elevation={3} sx={{ p: 4, width: '100%', maxWidth: '1200px', display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Box display="flex" alignItems="center">
        <IconButton onClick={onBack} sx={{ mr: 2 }}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5" component="div" sx={{ flexGrow: 1, textAlign: 'center' }}>
          Project Analysis: {fileName}
        </Typography>
      </Box>
      <Typography variant="subtitle2" color="text.secondary" textAlign="center" mb={2}>
        Creation: {new Date(createdAt).toLocaleString()}
      </Typography>

      <Box sx={{ height: 250, width: '100%' }}>
        <DataGrid rows={rows} columns={columns} hideFooter disableSelectionOnClick components={{ Toolbar: GridToolbar }} />
      </Box>

      <Box mt={2} display="flex" flexDirection="column" alignItems="center" gap={1}>
        <Typography>Select 3 metrics to visualize:</Typography>
        <FormGroup row>
          {METRICS.map(m => (
            <FormControlLabel key={m} control={<Checkbox checked={selectedMetrics.includes(m)} onChange={() => toggleMetric(m)} disabled={!selectedMetrics.includes(m) && selectedMetrics.length >= 3} />} label={m.toUpperCase()} />
          ))}
        </FormGroup>
      </Box>

      <Box mt={2} display="flex" justifyContent="center">
        <Button variant="contained" disabled={selectedMetrics.length !== 3} onClick={handleVisualize}>
          Visualize Results
        </Button>
      </Box>
    </Paper>
  );
}