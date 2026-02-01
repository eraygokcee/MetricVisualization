package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "metric_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricResultEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "class_name", nullable = false)
    private String className;

    private double tcc;
    private int wmc;
    private double lcom;
    private int dit;
    private int cbo;
    private int maxCyclo; // YENİ EKLENDİ
    private double avgCyclo; // YENİ EKLENDİ
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisEntity analysis;
}
