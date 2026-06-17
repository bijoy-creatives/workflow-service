package com.cintara.workflow.controller;

import com.cintara.workflow.model.Workflow;
import com.cintara.workflow.model.WorkflowExecution;
import com.cintara.workflow.repository.WorkflowExecutionRepository;
import com.cintara.workflow.repository.WorkflowRepository;
import com.cintara.workflow.service.WorkflowEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api")
public class WorkflowController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowEngine workflowEngine;

    public WorkflowController(
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository executionRepository,
            WorkflowEngine workflowEngine) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.workflowEngine = workflowEngine;
    }

    // Create a new workflow definition
    @PostMapping("/workflows")
    public ResponseEntity<Workflow> createWorkflow(@RequestBody Workflow workflow) {
        if (workflow.getWorkflowId() == null || workflow.getWorkflowId().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Workflow savedWorkflow = workflowRepository.save(workflow);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWorkflow);
    }

    // Get a workflow definition
    @GetMapping("/workflows/{workflowId}")
    public ResponseEntity<Workflow> getWorkflow(@PathVariable String workflowId) {
        return workflowRepository.findById(workflowId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // List all workflow definitions
    @GetMapping("/workflows")
    public ResponseEntity<Collection<Workflow>> listWorkflows() {
        return ResponseEntity.ok(workflowRepository.findAll());
    }

    // Execute a workflow by workflowId
    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<WorkflowExecution> executeWorkflow(@PathVariable String workflowId) {
        try {
            WorkflowExecution execution = workflowEngine.startExecution(workflowId);
            return ResponseEntity.status(HttpStatus.CREATED).body(execution);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Get execution status and history by executionId
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<WorkflowExecution> getExecution(@PathVariable String executionId) {
        return executionRepository.findById(executionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // List all execution runs
    @GetMapping("/executions")
    public ResponseEntity<Collection<WorkflowExecution>> listExecutions() {
        return ResponseEntity.ok(executionRepository.findAll());
    }
}
