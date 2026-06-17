package com.cintara.workflow.model;

import java.util.Map;

public class StepDefinition {
    private String name;
    private Map<String, Object> input;

    public StepDefinition() {
    }

    public StepDefinition(String name) {
        this.name = name;
    }

    public StepDefinition(String name, Map<String, Object> input) {
        this.name = name;
        this.input = input;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}
