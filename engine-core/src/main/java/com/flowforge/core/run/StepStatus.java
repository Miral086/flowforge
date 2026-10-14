package com.flowforge.core.run;

/** Lifecycle of one step within a run: PENDING -> RUNNING -> SUCCEEDED | FAILED, or PENDING -> SKIPPED. */
public enum StepStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED;
}
