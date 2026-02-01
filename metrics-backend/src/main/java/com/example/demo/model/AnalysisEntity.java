package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisEntity {
    @Id
    @GeneratedValue
    private UUID id;

    /** "CLASS" veya "PROJECT" */
    private String type;

    @Column(name = "target_path", nullable = false)
    private String targetPath;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "class_name")
    private String className;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetricResultEntity> results = new ArrayList<>();
}