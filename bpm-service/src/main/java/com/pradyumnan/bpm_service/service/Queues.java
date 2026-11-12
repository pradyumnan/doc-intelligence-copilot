package com.pradyumnan.bpm_service.service;

import java.util.List;
import java.util.Optional;

public final class Queues {

    /** The system queue that holds cases waiting for a person. Documents are never routed to it by name. */
    public static final String HUMAN_REVIEW = "Human Review";
    public static final String GENERAL_REVIEW = "General Review";

    // The queues a document can be routed to. Keep in sync with QUEUES in ai-service/app/queues.py.
    public static final List<String> WORK_QUEUES = List.of(
            "Finance",
            "Approval Workflow",
            "Compliance Review",
            "Fraud Review",
            "Document Collection",
            "Underwriting",
            "Senior Review",
            "Risk Assessment",
            "KYC Cleared",
            "KYC Pending",
            "Manual KYC Review",
            "Department Head Approval",
            "Legal Review",
            "Legal Escalation",
            "Vendor Onboarding",
            GENERAL_REVIEW);

    private Queues() {}

    /** The canonical queue name for a name written any way a model might write it, or empty if it is not a real queue. */
    public static Optional<String> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String wanted = key(name);
        return WORK_QUEUES.stream().filter(q -> key(q).equals(wanted)).findFirst();
    }

    private static String key(String s) {
        return s.toLowerCase()
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("\\s+queue$", "");
    }
}
