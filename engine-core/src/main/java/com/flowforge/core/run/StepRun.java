package com.flowforge.core.run;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * The state of one step inside one workflow run: status, attempts, timings, output and last error.
 *
 * Mutating methods are package-private on purpose: only WorkflowRun (same package) may change a StepRun.
 * Code in other packages can read a StepRun but never modify it directly.
 */
public class StepRun {

    private final String stepName;
    private StepStatus status = StepStatus.PENDING;
    private int attempts;
    private Instant startedAt;
    private Instant finishedAt;
    private String output;
    private String error;

    StepRun(String stepName) {
        this.stepName = stepName;
    }

    // ---- state changes (package-private, called only by WorkflowRun) ----

    void startAttempt() {
        if (status == StepStatus.PENDING) {             // first attempt
            status = StepStatus.RUNNING;
            startedAt = Instant.now();
        } else if (status != StepStatus.RUNNING) {      // finished steps can never start again
            throw new IllegalStateException(
                    "Cannot start step '" + stepName + "' because it is " + status);
        }
        attempts++;
    }

    void recordFailedAttempt(String error) {
        requireStatus(StepStatus.RUNNING, "record a failed attempt for");
        this.error = error;
    }

    void succeed(String output) {
        requireStatus(StepStatus.RUNNING, "succeed");
        this.status = StepStatus.SUCCEEDED;
        this.output = output;
        this.error = null;
        this.finishedAt = Instant.now();
    }

    void fail(String error) {
        requireStatus(StepStatus.RUNNING, "fail");
        this.status = StepStatus.FAILED;
        this.error = error;
        this.finishedAt = Instant.now();
    }

    void skip(String reason) {
        requireStatus(StepStatus.PENDING, "skip");
        this.status = StepStatus.SKIPPED;
        this.error = reason;
        this.finishedAt = Instant.now();
    }

    private void requireStatus(StepStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " step '" + stepName + "' because it is " + status + ", expected " + expected);
        }
    }

    // ---- read-only view ----

    public String getStepName() {
        return stepName;
    }

    public StepStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> getFinishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<String> getOutput() {
        return Optional.ofNullable(output);
    }

    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }

    /** How long the step took, once it has both started and finished. */
    public Optional<Duration> getDuration() {
        if (startedAt == null || finishedAt == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(startedAt, finishedAt));
    }

    @Override
    public String toString() {
        return "StepRun{step=" + stepName + ", status=" + status + ", attempts=" + attempts
                + (error != null ? ", error=" + error : "") + "}";
    }
}
