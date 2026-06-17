package com.cintara.workflow.service.executors;

import com.cintara.workflow.service.StepExecutor;
import com.cintara.workflow.service.StepResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("approve")
public class ApproveStepExecutor implements StepExecutor {
    @SuppressWarnings("unchecked")
    @Override
    public StepResult execute(Map<String, Object> input, Map<String, Object> context) {
        // Retrieve the output from the "validate" step if it exists
        Map<String, Object> validateOutput = (Map<String, Object>) context.get("validate");
        
        if (validateOutput != null && Boolean.FALSE.equals(validateOutput.get("valid"))) {
            return StepResult.fail("Validation check failed. Cannot approve.");
        }
        
        return StepResult.success(Map.of("approvedBy", "system"));
    }
}
