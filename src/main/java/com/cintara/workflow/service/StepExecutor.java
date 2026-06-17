package com.cintara.workflow.service;

import java.util.Map;

public interface StepExecutor {
    StepResult execute(Map<String, Object> input, Map<String, Object> context);
}
