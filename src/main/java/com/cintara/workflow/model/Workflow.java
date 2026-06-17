package com.cintara.workflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflows")
public class Workflow {
    @Id
    private String workflowId;

    @Convert(converter = JpaConverters.StepDefinitionsConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<StepDefinition> steps = new ArrayList<>();

    public Workflow() {
    }

    public Workflow(String workflowId, List<StepDefinition> steps) {
        this.workflowId = workflowId;
        this.steps = steps;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public List<StepDefinition> getSteps() {
        return steps;
    }

    public void setSteps(List<StepDefinition> steps) {
        this.steps = steps;
    }
}
