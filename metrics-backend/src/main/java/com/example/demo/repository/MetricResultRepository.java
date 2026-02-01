package com.example.demo.repository;

import com.example.demo.model.MetricResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MetricResultRepository extends JpaRepository<MetricResultEntity, UUID> {
}
