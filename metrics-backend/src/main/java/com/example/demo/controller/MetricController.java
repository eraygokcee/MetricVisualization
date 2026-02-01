package com.example.demo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.AnalysisResponseDto;
import com.example.demo.dto.AnalysisSummaryDto;
import com.example.demo.dto.VisualizeRequest;
import com.example.demo.dto.VisualizeResponseDTO;
import com.example.demo.service.AnalysisService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {
    private final AnalysisService analysisService;
    private final RestTemplate restTemplate;
	
    @PostMapping(path="/class", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisResponseDto> analyzeClass(
            @RequestParam("path") String path) {
        AnalysisResponseDto dto = analysisService.analyzeClass(path);
        return ResponseEntity
        		.ok(dto);
    }
	
    /** Bir klasör/proje için PROJECT-bazlı analiz */
    @PostMapping("/project")
    public ResponseEntity<AnalysisResponseDto> analyzeProject(
            @RequestParam("targetPath") String targetPath) {
    	
    	AnalysisResponseDto dto;
    	if(targetPath.endsWith(".jar")) {
    		dto = analysisService.analyzeJar(targetPath);
    	}else {
    		dto = analysisService.analyzeProject(targetPath);
    	}
        return ResponseEntity.ok(dto);
    }
	
    /** Tüm analiz özetlerini getir (analysis tablosu) */
    @GetMapping
    public ResponseEntity<List<AnalysisSummaryDto>> getAllAnalyses() {
        List<AnalysisSummaryDto> list = analysisService.getAllAnalyses();
        return ResponseEntity.ok(list);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponseDto> getAnalysisById(
            @PathVariable("id") UUID id) {
        AnalysisResponseDto dto = analysisService.getAnalysisById(id);
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping("/visualize")
    public ResponseEntity<VisualizeResponseDTO> visualize(
    		@RequestBody VisualizeRequest req){
    	VisualizeResponseDTO resp = analysisService.visualize(req);
    	
    	try {
    		String unityUrl = "http://localhost:5000/visualize";
    		restTemplate.postForObject(unityUrl,resp,String.class);
    		
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	return ResponseEntity.ok(resp);
    }
    
}
