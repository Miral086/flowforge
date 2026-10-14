package com.flowforge.core.exception;

public class StepExecutionException extends WorkflowException {

    private final String stepName;

    public StepExecutionException(String stepName, String message) {
        super(message);
        this.stepName = stepName;
    }

    public StepExecutionException(String stepName, String message, Throwable cause) {
        super(message, cause);
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }
}
