package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisualizeResponseDTO {
	private UUID id;
    private String type;
    private String targetPath;
    private String projectName;
    private LocalDateTime createdAt;
    private List<VisualizeResultDTO> results;
}
