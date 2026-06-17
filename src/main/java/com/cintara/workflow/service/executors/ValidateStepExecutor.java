package com.cintara.workflow.service.executors;

import com.cintara.workflow.service.StepExecutor;
import com.cintara.workflow.service.StepResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("validate")
public class ValidateStepExecutor implements StepExecutor {
    @Override
    public StepResult execute(Map<String, Object> input, Map<String, Object> context) {
        if (input == null || !input.containsKey("contractId")) {
            return StepResult.success(Map.of("valid", false, "reason", "Missing contractId"));
        }
        return StepResult.success(Map.of("valid", true));
    }
}
