package com.cintara.workflow;

import com.cintara.workflow.model.StepDefinition;
import com.cintara.workflow.model.Workflow;
import com.cintara.workflow.model.WorkflowExecution;
import com.cintara.workflow.repository.WorkflowExecutionRepository;
import com.cintara.workflow.repository.WorkflowRepository;
import com.cintara.workflow.service.WorkflowEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class WorkflowApplicationTests {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @Autowired
    private WorkflowEngine workflowEngine;

    @Test
    void contextLoads() {
    }

    @Test
    void testSeededWorkflowExists() {
        Optional<Workflow> workflow = workflowRepository.findById("contract-review");
        assertThat(workflow).isPresent();
        assertThat(workflow.get().getSteps()).hasSize(3);
        assertThat(workflow.get().getSteps().get(0).getName()).isEqualTo("validate");
        assertThat(workflow.get().getSteps().get(1).getName()).isEqualTo("approve");
        assertThat(workflow.get().getSteps().get(2).getName()).isEqualTo("execute");
    }

    @Test
    void testCreateNewWorkflowAndExecute() {
        // 1. Create a custom workflow
        String workflowId = "custom-test-workflow";
        StepDefinition step1 = new StepDefinition("validate", Map.of("contractId", "999"));
        StepDefinition step2 = new StepDefinition("approve");
        
        Workflow workflow = new Workflow(workflowId, List.of(step1, step2));
        workflowRepository.save(workflow);

        // 2. Start Execution
        WorkflowExecution execution = workflowEngine.startExecution(workflowId);
        assertThat(execution).isNotNull();
        assertThat(execution.getWorkflowId()).isEqualTo(workflowId);
        assertThat(execution.getStatus()).isEqualTo("RUNNING");

        // 3. Wait for execution to complete (polling because engine runs asynchronously)
        // Since each step has a 1.5s simulated sleep, 2 steps will take ~3 seconds.
        await().atMost(6, TimeUnit.SECONDS).untilAsserted(() -> {
            WorkflowExecution currentExec = executionRepository.findById(execution.getExecutionId()).orElse(null);
            assertThat(currentExec).isNotNull();
            assertThat(currentExec.getStatus()).isEqualTo("COMPLETED");
        });

        // 4. Assert execution context and outputs
        WorkflowExecution finalExec = executionRepository.findById(execution.getExecutionId()).orElseThrow();
        assertThat(finalExec.getSteps().get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(finalExec.getSteps().get(0).getOutput()).containsEntry("valid", true);
        
        assertThat(finalExec.getSteps().get(1).getStatus()).isEqualTo("COMPLETED");
        assertThat(finalExec.getSteps().get(1).getOutput()).containsEntry("approvedBy", "system");

        assertThat(finalExec.getContext()).containsKey("validate");
        assertThat(finalExec.getContext()).containsKey("approve");
    }

    @Test
    void testWorkflowFailureWhenValidationMissing() {
        // 1. Create a workflow with missing contractId
        String workflowId = "invalid-test-workflow";
        StepDefinition step1 = new StepDefinition("validate", Map.of()); // Empty input
        StepDefinition step2 = new StepDefinition("approve");
        
        Workflow workflow = new Workflow(workflowId, List.of(step1, step2));
        workflowRepository.save(workflow);

        // 2. Execute
        WorkflowExecution execution = workflowEngine.startExecution(workflowId);
        
        // 3. Wait for it to fail/stop
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            WorkflowExecution currentExec = executionRepository.findById(execution.getExecutionId()).orElse(null);
            assertThat(currentExec).isNotNull();
            assertThat(currentExec.getStatus()).isEqualTo("FAILED");
        });

        // 4. Verify step level error
        WorkflowExecution finalExec = executionRepository.findById(execution.getExecutionId()).orElseThrow();
        assertThat(finalExec.getSteps().get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(finalExec.getSteps().get(0).getOutput()).containsEntry("valid", false); // Output marked as invalid: false
        
        assertThat(finalExec.getSteps().get(1).getStatus()).isEqualTo("FAILED"); // Fails because validation was invalid
        assertThat(finalExec.getSteps().get(1).getErrorMessage()).contains("Validation check failed");
    }
}
