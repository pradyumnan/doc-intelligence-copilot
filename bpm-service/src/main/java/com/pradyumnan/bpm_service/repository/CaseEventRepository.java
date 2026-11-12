package com.pradyumnan.bpm_service.repository;

import com.pradyumnan.bpm_service.model.CaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    List<CaseEvent> findByCaseIdOrderByOccurredAtAscIdAsc(Long caseId);

    long countByAction(String action);

    long countByActionAndFromQueue(String action, String fromQueue);

    long countByActionAndFromQueueNot(String action, String fromQueue);
}
