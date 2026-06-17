package com.cintara.workflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {
    @Id
    private String executionId;
    private String workflowId;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, SUSPENDED
    private int currentStep; // 1-based index of the currently executing step

    @Convert(converter = JpaConverters.StepExecutionsConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<StepExecution> steps = new ArrayList<>();

    @Convert(converter = JpaConverters.MapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> context = new HashMap<>();
    private Instant startedAt;
    private Instant completedAt;

    public WorkflowExecution() {
    }

    public WorkflowExecution(String executionId, String workflowId, String status) {
        this.executionId = executionId;
        this.workflowId = workflowId;
        this.status = status;
        this.startedAt = Instant.now();
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public List<StepExecution> getSteps() {
        return steps;
    }

    public void setSteps(List<StepExecution> steps) {
        this.steps = steps;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
