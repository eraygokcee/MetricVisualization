package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetricResultDto {
	private UUID id;
    private String className;
    private double tcc; //diğer metrikler devam edecek
    private int wmc;
    private double lcom;
    private int dit;
    private int cbo;
    private int maxCyclo;
    private double avgCyclo;
}

