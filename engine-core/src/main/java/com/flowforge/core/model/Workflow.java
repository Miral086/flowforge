package com.flowforge.core.model;

import com.flowforge.core.exception.InvalidWorkflowException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Workflow {

    private final String name;
    private final List<Step> steps;

    public Workflow(String name, List<Step> steps) {
        if (name == null || name.isBlank()){
            throw new InvalidWorkflowException("Workflow name must not be blank.");
        }
        if (steps == null || steps.isEmpty()){
            throw new InvalidWorkflowException("Workflow " + name + " must have atleast one step.");
        }
        this.name = name;
        this.steps = List.copyOf(steps);
        validateSteps();
    }

    public String getName() {
        return name;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void validateSteps(){
        Set<String> names = new HashSet<>();
        for (Step step: steps){
            if (!names.add(step.getName())){
                throw new InvalidWorkflowException("Workflow '" + this.name + "' has more than one step named '" + step.getName() + "'");
            }
        }
        for (Step step : steps) {
            for (String dependency : step.getDependsOn()) {
                if (dependency.equals(step.getName())) {
                    throw new InvalidWorkflowException("Step '" + step.getName() + "' cannot depend on itself");
                }
                if (!names.contains(dependency)) {
                    throw new InvalidWorkflowException(
                            "Step '" + step.getName() + "' depends on unknown step '" + dependency + "'");
                }
            }
        }
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
