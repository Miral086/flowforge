package com.flowforge.core.run;

import com.flowforge.core.model.Step;
import com.flowforge.core.model.Workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One execution of a Workflow. Owns a StepRun per step and is the only way to change them,
 * so every rule about valid state changes is enforced in one place (an "aggregate root").
 * Later phases save this object to PostgreSQL and expose it through the REST API.
 */
public class WorkflowRun {

    private final UUID id;
    private final Workflow workflow;
    private final Instant createdAt;
    private final Map<String, StepRun> stepRuns = new LinkedHashMap<>();   // keeps workflow step order

    private RunStatus status = RunStatus.PENDING;
    private Instant startedAt;
    private Instant finishedAt;
    private String failureReason;

    public WorkflowRun(Workflow workflow) {
        this.id = UUID.randomUUID();
        this.workflow = Objects.requireNonNull(workflow, "workflow");
        this.createdAt = Instant.now();
        for (Step step : workflow.getSteps()) {
            stepRuns.put(step.getName(), new StepRun(step.getName()));
        }
    }

    // ---- run lifecycle ----

    public void start() {
        requireStatus(RunStatus.PENDING, "start");
        status = RunStatus.RUNNING;
        startedAt = Instant.now();
    }

    /** Only allowed once every step has succeeded. */
    public void succeed() {
        requireStatus(RunStatus.RUNNING, "succeed");
        for (StepRun stepRun : stepRuns.values()) {
            if (stepRun.getStatus() != StepStatus.SUCCEEDED) {
                throw new IllegalStateException("Cannot succeed run " + id + ": step '"
                        + stepRun.getStepName() + "' is " + stepRun.getStatus());
            }
        }
        status = RunStatus.SUCCEEDED;
        finishedAt = Instant.now();
    }

    /** Fails the run and marks every step that never started as SKIPPED. */
    public void fail(String reason) {
        requireStatus(RunStatus.RUNNING, "fail");
        skipPendingSteps("Skipped because the run failed");
        status = RunStatus.FAILED;
        failureReason = reason;
        finishedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel run " + id + " because it is already " + status);
        }
        skipPendingSteps("Skipped because the run was cancelled");
        status = RunStatus.CANCELLED;
        failureReason = reason;
        finishedAt = Instant.now();
    }

    // ---- step lifecycle (delegates to StepRun, after checking the run itself is running) ----

    public void startStepAttempt(String stepName) {
        requireStatus(RunStatus.RUNNING, "start a step in");
        stepRun(stepName).startAttempt();
    }

    public void recordStepAttemptFailure(String stepName, String error) {
        requireStatus(RunStatus.RUNNING, "record a step failure in");
        stepRun(stepName).recordFailedAttempt(error);
    }

    public void completeStep(String stepName, String output) {
        requireStatus(RunStatus.RUNNING, "complete a step in");
        stepRun(stepName).succeed(output);
    }

    public void failStep(String stepName, String error) {
        requireStatus(RunStatus.RUNNING, "fail a step in");
        stepRun(stepName).fail(error);
    }

    private void skipPendingSteps(String reason) {
        for (StepRun stepRun : stepRuns.values()) {
            if (stepRun.getStatus() == StepStatus.PENDING) {
                stepRun.skip(reason);
            }
        }
    }

    private StepRun stepRun(String stepName) {
        StepRun stepRun = stepRuns.get(stepName);
        if (stepRun == null) {
            throw new IllegalArgumentException("Workflow '" + workflow.getName() + "' has no step '" + stepName + "'");
        }
        return stepRun;
    }

    private void requireStatus(RunStatus expected, String action) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " run " + id + " because it is " + status + ", expected " + expected);
        }
    }

    // ---- read-only view ----

    public boolean isStepSucceeded(String stepName) {
        return stepRun(stepName).getStatus() == StepStatus.SUCCEEDED;
    }

    public StepRun getStepRun(String stepName) {
        return stepRun(stepName);
    }

    /** All step runs in workflow order. The list is a read-only copy. */
    public List<StepRun> getStepRuns() {
        return List.copyOf(stepRuns.values());
    }

    public List<String> getCompletedStepNames() {
        List<String> names = new ArrayList<>();
        for (StepRun stepRun : stepRuns.values()) {
            if (stepRun.getStatus() == StepStatus.SUCCEEDED) {
                names.add(stepRun.getStepName());
            }
        }
        return List.copyOf(names);
    }

    public UUID getId() {
        return id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public RunStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> getFinishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    public Optional<Duration> getDuration() {
        if (startedAt == null || finishedAt == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(startedAt, finishedAt));
    }

    @Override
    public String toString() {
        return "WorkflowRun{id=" + id + ", workflow=" + workflow.getName() + ", status=" + status
                + (failureReason != null ? ", failureReason=" + failureReason : "") + "}";
    }
}
