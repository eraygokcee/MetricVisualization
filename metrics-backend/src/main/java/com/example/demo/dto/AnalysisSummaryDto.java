package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisSummaryDto {
	private UUID id;
	private String type; // Class veya Project
	private String targetPath; // Dizin yolu
	private String projectName ; //CLass ya da proje adı
	private LocalDateTime createdAt;
}
