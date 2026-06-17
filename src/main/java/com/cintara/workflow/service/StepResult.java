package com.cintara.workflow.service;

import java.util.Map;

public record StepResult(
    String status, // COMPLETED, FAILED, SUSPENDED
    Map<String, Object> output,
    String errorMessage
) {
    public static StepResult success(Map<String, Object> output) {
        return new StepResult("COMPLETED", output, null);
    }

    public static StepResult fail(String errorMessage) {
        return new StepResult("FAILED", null, errorMessage);
    }

    public static StepResult suspend(Map<String, Object> output) {
        return new StepResult("SUSPENDED", output, null);
    }
}
