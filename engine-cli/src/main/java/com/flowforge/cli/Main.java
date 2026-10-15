package com.flowforge.cli;

import com.flowforge.core.EngineInfo;
import com.flowforge.core.engine.WorkflowRunner;
import com.flowforge.core.model.DelayStep;
import com.flowforge.core.model.HttpStep;
import com.flowforge.core.model.ShellStep;
import com.flowforge.core.model.Workflow;
import com.flowforge.core.run.StepRun;
import com.flowforge.core.run.WorkflowRun;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("Starting " + EngineInfo.version());
        WorkflowRunner runner = new WorkflowRunner();

        List<Workflow> workflows = List.of(
                ciPipeline(),
                diamondPipeline(),
                flakyPipeline(),
                backwardsPipeline(),
                cyclicPipeline());

        for (Workflow workflow : workflows) {
            printSummary(runner.run(workflow));
        }
    }

    /** Everything succeeds: real shell commands, a 1-second wait, a simulated HTTP check. */
    private static Workflow ciPipeline() {
        return new Workflow("ci-pipeline", List.of(
                new ShellStep("checkout", List.of(), "echo Cloning repository"),
                new ShellStep("build", List.of("checkout"), "echo Building project"),
                new DelayStep("wait-for-db", List.of("build"), 1000),
                new HttpStep("health-check", List.of("wait-for-db"), "http://localhost:8080/health")));
    }

    /** Branches and joins: build and lint both depend on checkout; deploy waits for both. */
    private static Workflow diamondPipeline() {
        return new Workflow("diamond-pipeline", List.of(
                new ShellStep("checkout", List.of(), "echo Cloning"),
                new ShellStep("build", List.of("checkout"), "echo Building"),
                new ShellStep("lint", List.of("checkout"), "echo Linting"),
                new ShellStep("test", List.of("build"), "echo Testing"),
                new ShellStep("deploy", List.of("build", "lint"), "echo Deploying")));
    }

    /** A step that always fails: 3 attempts with backoff, then the run fails and deploy is skipped. */
    private static Workflow flakyPipeline() {
        return new Workflow("flaky-pipeline", List.of(
                new ShellStep("compile", List.of(), "echo Compiling"),
                new ShellStep("integration-test", List.of("compile"), "exit 1", 3),
                new ShellStep("deploy", List.of("integration-test"), "echo Deploying")));
    }

    /** Listed in the wrong order: the topological sort runs checkout first anyway. */
    private static Workflow backwardsPipeline() {
        return new Workflow("backwards-pipeline", List.of(
                new ShellStep("test", List.of("build"), "echo Testing"),
                new ShellStep("build", List.of("checkout"), "echo Building"),
                new ShellStep("checkout", List.of(), "echo Cloning")));
    }

    /** build -> test -> deploy -> build: rejected before any step runs. */
    private static Workflow cyclicPipeline() {
        return new Workflow("cyclic-pipeline", List.of(
                new ShellStep("checkout", List.of(), "echo Cloning"),
                new ShellStep("build", List.of("test"), "echo Building"),
                new ShellStep("test", List.of("deploy"), "echo Testing"),
                new ShellStep("deploy", List.of("build"), "echo Deploying")));
    }

    private static void printSummary(WorkflowRun run) {
        System.out.println();
        System.out.println(run.getWorkflow().getName() + ": " + run.getStatus());
        run.getFailureReason().ifPresent(reason -> System.out.println("  reason: " + reason));
        for (StepRun stepRun : run.getStepRuns()) {
            System.out.println(String.format("  %-17s %-10s attempts=%d",
                    stepRun.getStepName(), stepRun.getStatus(), stepRun.getAttempts()));
        }
        System.out.println();
    }
}