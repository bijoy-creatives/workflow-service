package com.cintara.workflow.repository;

import com.cintara.workflow.model.StepDefinition;
import com.cintara.workflow.model.Workflow;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final WorkflowRepository workflowRepository;

    public DatabaseSeeder(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (workflowRepository.findById("contract-review").isEmpty()) {
            StepDefinition validateStep = new StepDefinition(
                    "validate", 
                    Map.of("contractId", "123")
            );
            StepDefinition approveStep = new StepDefinition("approve");
            StepDefinition executeStep = new StepDefinition("execute");
            
            Workflow contractReview = new Workflow(
                    "contract-review", 
                    List.of(validateStep, approveStep, executeStep)
            );
            workflowRepository.save(contractReview);
        }
    }
}
