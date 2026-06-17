package com.cintara.workflow.service.executors;

import com.cintara.workflow.service.StepExecutor;
import com.cintara.workflow.service.StepResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("execute")
public class ExecuteStepExecutor implements StepExecutor {
    @SuppressWarnings("unchecked")
    @Override
    public StepResult execute(Map<String, Object> input, Map<String, Object> context) {
        // Retrieve the output from the "approve" step if it exists
        Map<String, Object> approveOutput = (Map<String, Object>) context.get("approve");
        
        if (approveOutput == null) {
            return StepResult.fail("Approval step output missing. Cannot execute.");
        }
        
        return StepResult.success(Map.of("result", "success"));
    }
}
