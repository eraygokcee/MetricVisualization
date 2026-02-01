package com.example.demo.service;

import com.example.demo.dto.AnalysisResponseDto;
import com.example.demo.dto.AnalysisSummaryDto;
import com.example.demo.dto.VisualizeRequest;
import com.example.demo.dto.VisualizeResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AnalysisService {

    /**
     * Tek bir .class dosyası için CLASS-bazlı analiz yapar ve tüm metrik sonuçlarını döner.
     */
    AnalysisResponseDto analyzeClass(String path);

    /**
     * Bir proje klasörü için PROJECT-bazlı analiz yapar.
     */
    AnalysisResponseDto analyzeProject(String targetPath);
    
    AnalysisResponseDto analyzeJar(String targetPath);

    /**
     * Tüm analiz özetlerini getirir.
     */
    List<AnalysisSummaryDto> getAllAnalyses();

    /**
     * Belirli bir analiz detayı ve metrik sonuçlarını getirir.
     */
    AnalysisResponseDto getAnalysisById(UUID id);
    
    VisualizeResponseDTO visualize(VisualizeRequest req);
}
