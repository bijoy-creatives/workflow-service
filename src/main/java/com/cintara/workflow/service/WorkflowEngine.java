package com.cintara.workflow.service;

import com.cintara.workflow.model.StepDefinition;
import com.cintara.workflow.model.StepExecution;
import com.cintara.workflow.model.Workflow;
import com.cintara.workflow.model.WorkflowExecution;
import com.cintara.workflow.repository.WorkflowExecutionRepository;
import com.cintara.workflow.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkflowEngine {
    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final Map<String, StepExecutor> stepExecutors;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public WorkflowEngine(
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository executionRepository,
            Map<String, StepExecutor> stepExecutors) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.stepExecutors = stepExecutors;
        log.info("Registered step executors: {}", stepExecutors.keySet());
    }

    public WorkflowExecution startExecution(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        String executionId = UUID.randomUUID().toString();
        WorkflowExecution execution = new WorkflowExecution(executionId, workflowId, "RUNNING");
        execution.setCurrentStep(1);

        // Pre-initialize step executions
        for (StepDefinition stepDef : workflow.getSteps()) {
            StepExecution stepExec = new StepExecution(stepDef.getName(), "PENDING");
            // Clone step input if configured
            if (stepDef.getInput() != null) {
                stepExec.setInput(new HashMap<>(stepDef.getInput()));
            } else {
                stepExec.setInput(new HashMap<>());
            }
            execution.getSteps().add(stepExec);
        }

        executionRepository.save(execution);

        // Submit the execution task to run asynchronously
        executorService.submit(() -> runWorkflow(executionId));

        return execution;
    }

    private void runWorkflow(String executionId) {
        log.info("Starting execution of workflow task: {}", executionId);
        WorkflowExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            log.error("Execution record {} not found inside workflow runner", executionId);
            return;
        }

        Workflow workflow = workflowRepository.findById(execution.getWorkflowId()).orElse(null);
        if (workflow == null) {
            log.error("Workflow definition {} not found for execution {}", execution.getWorkflowId(), executionId);
            updateStatus(execution, "FAILED", null);
            return;
        }

        for (int i = 0; i < workflow.getSteps().size(); i++) {
            StepDefinition stepDef = workflow.getSteps().get(i);
            StepExecution stepExec = execution.getSteps().get(i);

            // Update current step index and status
            execution.setCurrentStep(i + 1);
            stepExec.setStatus("RUNNING");
            stepExec.setStartedAt(Instant.now());
            executionRepository.save(execution);

            // Optional simulated delay to make tracking progress observable on the dashboard
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stepExec.setStatus("FAILED");
                stepExec.setErrorMessage("Execution interrupted");
                stepExec.setCompletedAt(Instant.now());
                updateStatus(execution, "FAILED", null);
                return;
            }

            StepExecutor executor = stepExecutors.get(stepDef.getName());
            if (executor == null) {
                String error = "No step executor bean registered for step name: " + stepDef.getName();
                log.warn(error);
                stepExec.setStatus("FAILED");
                stepExec.setErrorMessage(error);
                stepExec.setCompletedAt(Instant.now());
                updateStatus(execution, "FAILED", null);
                return;
            }

            try {
                // Execute the step handler
                // Input is step configuration inputs. Context is cumulative outputs.
                StepResult result = executor.execute(stepExec.getInput(), execution.getContext());

                stepExec.setStatus(result.status());
                stepExec.setCompletedAt(Instant.now());

                if ("COMPLETED".equals(result.status())) {
                    if (result.output() != null) {
                        stepExec.setOutput(result.output());
                        // Merge output into shared context under the step name
                        execution.getContext().put(stepDef.getName(), result.output());
                    }
                    executionRepository.save(execution);
                } else if ("FAILED".equals(result.status())) {
                    stepExec.setErrorMessage(result.errorMessage());
                    executionRepository.save(execution);
                    updateStatus(execution, "FAILED", null);
                    return;
                } else if ("SUSPENDED".equals(result.status())) {
                    // Paused workflow execution
                    updateStatus(execution, "SUSPENDED", null);
                    return;
                }
            } catch (Exception e) {
                log.error("Exception during step execution of {}", stepDef.getName(), e);
                stepExec.setStatus("FAILED");
                stepExec.setErrorMessage(e.getMessage());
                stepExec.setCompletedAt(Instant.now());
                updateStatus(execution, "FAILED", null);
                return;
            }
        }

        updateStatus(execution, "COMPLETED", Instant.now());
    }

    private void updateStatus(WorkflowExecution execution, String status, Instant completedAt) {
        execution.setStatus(status);
        if (completedAt != null) {
            execution.setCompletedAt(completedAt);
        }
        executionRepository.save(execution);
        log.info("Workflow execution {} status updated to {}", execution.getExecutionId(), status);
    }
}
