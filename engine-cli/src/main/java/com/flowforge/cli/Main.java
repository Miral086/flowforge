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

        // 1. Everything succeeds. Shell steps now run real commands and capture their output.
        Workflow ciPipeline = new Workflow("ci-pipeline", List.of(
                new ShellStep("checkout", List.of(), "echo Cloning repository"),
                new ShellStep("build", List.of("checkout"), "echo Building project"),
                new DelayStep("wait-for-db", List.of("build"), 1000),
                new HttpStep("health-check", List.of("wait-for-db"), "http://localhost:8080/health")
        ));

        // 2. A step that always fails: retried 3 times with growing waits, then the run fails.
        Workflow flakyPipeline = new Workflow("flaky-pipeline", List.of(
                new ShellStep("compile", List.of(), "echo Compiling"),
                new ShellStep("integration-test", List.of("compile"), "exit 1", 3),
                new ShellStep("deploy", List.of("integration-test"), "echo Deploying")
        ));

        // 3. Steps in the wrong order: fails on the dependency check (fixed properly in A2).
        Workflow badPipeline = new Workflow("bad-pipeline", List.of(
                new ShellStep("build", List.of("checkout"), "echo Building"),
                new ShellStep("checkout", List.of(), "echo Cloning")
        ));

        printSummary(runner.run(ciPipeline));
        printSummary(runner.run(flakyPipeline));
        printSummary(runner.run(badPipeline));

        // Experiment: uncomment, run, read the exception, then comment it out again.
        // new ShellStep("", List.of(), "echo hi");
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