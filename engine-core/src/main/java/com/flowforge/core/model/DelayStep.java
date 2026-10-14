package com.flowforge.core.model;

import com.flowforge.core.exception.StepExecutionException;

import java.util.List;

public class DelayStep extends Step {

    private final long millis;

    public DelayStep(String name, List<String> dependsOn, long millis) {
        super(name, dependsOn);
        this.millis = millis;
    }

    public long getMillis() {
        return millis;
    }

    @Override
    public String execute() throws StepExecutionException {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Waiting " + millis + " ms";
    }

    @Override
    public String toString() {
        return "DelayStep{" + super.toString() + ", millis=" + millis + "}";
    }
}
