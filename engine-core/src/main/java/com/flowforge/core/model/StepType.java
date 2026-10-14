package com.flowforge.core.model;

/** The kinds of step the engine knows how to execute. Each type has exactly one executor. */
public enum StepType {
    SHELL,
    HTTP,
    DELAY
}
