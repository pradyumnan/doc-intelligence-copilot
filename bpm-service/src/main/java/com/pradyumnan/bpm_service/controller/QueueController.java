package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.service.CaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QueueController {

    private final CaseService caseService;

    public QueueController(CaseService caseService) {
        this.caseService = caseService;
    }

    // Human Review first, then every work queue with how many cases are waiting in it
    @GetMapping("/queues")
    public List<CaseService.QueueView> queues() {
        return caseService.queueOverview();
    }
}
