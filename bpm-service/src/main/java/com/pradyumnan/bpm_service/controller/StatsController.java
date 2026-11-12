package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.repository.CaseEventRepository;
import com.pradyumnan.bpm_service.repository.CaseRepository;
import com.pradyumnan.bpm_service.service.Queues;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * How often people agree with the AI. Every review action is an implicit label, so this is a live
 * measure of accuracy on real traffic, not just on the hand-written test sets.
 * Admin only (see SecurityConfig: /admin/**).
 */
@RestController
@RequestMapping("/admin/stats")
public class StatsController {

    public record Stats(
            long totalCases,
            long autoRouted,              // the AI routed it without a person
            long held,                    // the AI held it for review
            long heldApproved,            // a reviewer agreed with the AI's suggested queue
            long heldReassigned,          // a reviewer sent it somewhere else
            long heldRejected,            // a reviewer rejected it
            long autoRoutedCorrected,     // a person had to fix an automatic routing
            Double heldApprovalRate,      // heldApproved / reviewed held cases (null until there is data)
            Double autoRouteCorrectionRate) {} // autoRoutedCorrected / autoRouted (a lower bound on the error rate)

    private final CaseRepository caseRepository;
    private final CaseEventRepository eventRepository;

    public StatsController(CaseRepository caseRepository, CaseEventRepository eventRepository) {
        this.caseRepository = caseRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public Stats stats() {
        long autoRouted = eventRepository.countByAction("AI_ROUTED");
        long held = eventRepository.countByAction("AI_HELD");
        long approved = eventRepository.countByAction("APPROVED");
        long reassigned = eventRepository.countByActionAndFromQueue("REASSIGNED", Queues.HUMAN_REVIEW);
        long rejected = eventRepository.countByActionAndFromQueue("REJECTED", Queues.HUMAN_REVIEW);
        long corrected = eventRepository.countByActionAndFromQueueNot("REASSIGNED", Queues.HUMAN_REVIEW)
                + eventRepository.countByActionAndFromQueueNot("REJECTED", Queues.HUMAN_REVIEW);

        long reviewedHeld = approved + reassigned + rejected;
        Double approvalRate = reviewedHeld == 0 ? null : (double) approved / reviewedHeld;
        Double correctionRate = autoRouted == 0 ? null : (double) corrected / autoRouted;

        return new Stats(caseRepository.count(), autoRouted, held, approved, reassigned, rejected,
                corrected, approvalRate, correctionRate);
    }
}
