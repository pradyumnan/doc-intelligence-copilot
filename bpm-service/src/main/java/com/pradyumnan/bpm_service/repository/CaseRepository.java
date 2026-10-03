package com.pradyumnan.bpm_service.repository;

import com.pradyumnan.bpm_service.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseRepository extends JpaRepository<Case, Long> {
}