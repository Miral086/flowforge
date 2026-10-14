package com.flowforge.core.model;

import java.util.List;

public class Workflow {

    private final String name;
    private final List<Step> steps;

    public Workflow(String name, List<Step> steps) {
        this.name = name;
        this.steps = steps;
    }

    public String getName() {
        return name;
    }

    public List<Step> getSteps() {
        return steps;
    }

    // Returns the Step with this name, or null if none matches
    public Step findStep(String stepName) {
        for (Step step : steps) {
            if (step.getName().equals(stepName)) {
                return step;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Workflow{name=" + name + ", steps=" + steps + "}";
    }
}
