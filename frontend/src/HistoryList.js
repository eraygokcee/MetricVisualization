// src/HistoryList.js
import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper
} from '@mui/material';

const BASE_URL = 'http://localhost:8080/api/metrics';

export default function HistoryList({ onShowDetails, onBack }) {
  const [records, setRecords] = useState([]);
  const [selectedId, setSelectedId] = useState(null);

  useEffect(() => {
    fetch(BASE_URL)
      .then(res => res.json())
      .then(data => setRecords(data))
      .catch(err => console.error(err));
  }, []);

  return (
    <Box width="100%" maxWidth="1000px" display="flex" flexDirection="column" alignItems="center" gap={2}>
      <Typography variant="h6" textAlign="center">📜 Past Analysis Records</Typography>

      <TableContainer component={Paper} sx={{ maxHeight: 450, width: '100%' }}>
        <Table stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>Type</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Class / Project Name</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Creation Date</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {records.map(rec => (
              <TableRow
                key={rec.id}
                hover
                selected={rec.id === selectedId}
                onClick={() => setSelectedId(rec.id)}
                sx={{ cursor: 'pointer' }}
              >
                <TableCell>{rec.type}</TableCell>
                <TableCell sx={{ fontFamily: 'monospace', fontSize: 14 }}>
                  {rec.projectName || rec.targetPath.split("bin\\")?.[1] || rec.targetPath}
                </TableCell>

                <TableCell>
                  {new Date(rec.createdAt).toLocaleString()}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Box mt={2} width="100%" display="flex" justifyContent="center" gap={2}>
         <Button
            variant="text"
            onClick={onBack}
          >
            Geri
          </Button>
        <Button
          variant="contained"
          disabled={!selectedId}
          onClick={() => {
            const chosen = records.find(r => r.id === selectedId);
            if (chosen) onShowDetails(chosen);
          }}
        >
          Show Selected Analysis
        </Button>
      </Box>
    </Box>
  );
}