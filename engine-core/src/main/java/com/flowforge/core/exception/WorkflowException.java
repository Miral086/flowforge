package com.flowforge.core.exception;

public class WorkflowException extends Exception{
    public WorkflowException(String message){
        super(message);
    }

    public WorkflowException(String message, Throwable cause){
        super(message,cause);
    }
}
