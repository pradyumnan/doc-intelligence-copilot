package com.pradyumnan.bpm_service.repository;

import com.pradyumnan.bpm_service.dto.CaseSummary;
import com.pradyumnan.bpm_service.model.Case;
import com.pradyumnan.bpm_service.model.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Long> {

    // A projection, so the list never loads the large OCR text of every case
    @Query("select new com.pradyumnan.bpm_service.dto.CaseSummary("
            + "c.id, c.filename, c.category, c.route, c.queue, c.status, c.confidence, c.finalStatus, "
            + "c.justification, c.uploadedBy, c.routedBy, c.createdAt) "
            + "from CaseRecord c order by c.id desc")
    List<CaseSummary> findAllSummaries();

    @Query("select c.queue, count(c) from CaseRecord c where c.status = :status group by c.queue")
    List<Object[]> countByQueue(@Param("status") CaseStatus status);

    long countByStatus(CaseStatus status);
}
