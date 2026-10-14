package com.flowforge.core.model;

import java.util.List;
import com.flowforge.core.exception.StepExecutionException;
import com.flowforge.core.exception.InvalidWorkflowException;

public abstract class Step {

    private final String name;
    private final List<String> dependsOn;
    private final int maxAttempts;

    protected Step (String name, List<String> dependsOn){
        this(name, dependsOn, 1);
    }
    protected Step(String name, List<String> dependsOn, int maxAttempts) {
        if (name == null || name.isBlank()) {
            throw new InvalidWorkflowException("Step name must not be blank");
        }
        this.name = name;
        this.dependsOn = dependsOn;
        if (maxAttempts == 0){
            maxAttempts = 1;
        }
        this.maxAttempts = maxAttempts;
    }

    public String getName() {
        return name;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
    public List<String> getDependsOn() {
        return dependsOn;
    }

    public abstract String execute() throws StepExecutionException;

    @Override
    public String toString() {
        return "name=" + name + ", dependsOn=" + dependsOn;
    }
}
