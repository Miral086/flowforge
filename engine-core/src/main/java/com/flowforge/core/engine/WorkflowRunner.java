package com.flowforge.core.engine;

import com.flowforge.core.exception.InvalidWorkflowException;
import com.flowforge.core.exception.StepExecutionException;
import com.flowforge.core.graph.DependencyGraph;
import com.flowforge.core.model.Step;
import com.flowforge.core.model.Workflow;
import com.flowforge.core.run.WorkflowRun;

import java.util.List;
import java.util.Optional;

public class WorkflowRunner {

    private static final long INITIAL_BACKOFF_MILLIS = 500;

    public WorkflowRun run(Workflow workflow) {
        WorkflowRun run = new WorkflowRun(workflow);

        List<String> order;
        try {
            order = new DependencyGraph(workflow).topologicalOrder();
        } catch (InvalidWorkflowException e) {
            run.start();
            run.fail(e.getMessage());          // marks every step SKIPPED
            System.out.println("Rejected " + workflow.getName() + ": " + e.getMessage());
            return run;
        }

        run.start();
        System.out.println("Running workflow: " + workflow.getName());

        for (String stepName : order) {
            Step step = workflow.findStep(stepName);
            Optional<String> pending = findPendingDependency(run, step);

            if (pending.isPresent()) {
                String reason = "Cannot run " + step.getName()
                        + ": dependency " + pending.get() + " not completed";   // fix 1: get() the value
                System.out.println(reason);
                run.fail(reason);
                return run;
            }

            boolean succeeded = executeWithRetries(run, step);
            if (!succeeded) {
                String error = run.getStepRun(step.getName()).getError().orElse("unknown error");
                run.fail("Step " + step.getName() + " failed: " + error);
                return run;
            }
        }

        run.succeed();
        System.out.println("Workflow " + workflow.getName() + " completed");
        return run;
    }

    private boolean executeWithRetries(WorkflowRun run, Step step) {
        int maxAttempts = step.getMaxAttempts();
        long backoffMillis = INITIAL_BACKOFF_MILLIS;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            run.startStepAttempt(step.getName());
            System.out.println("Running " + step.getName() + " (attempt " + attempt + "/" + maxAttempts + ")");

            try {
                String output = step.execute();                    // may throw StepExecutionException
                run.completeStep(step.getName(), output);           // only reached if execute() succeeded
                System.out.println("  output: " + output);
                return true;

            } catch (StepExecutionException e) {
                if (attempt == maxAttempts) {                       // no attempts left: give up
                    run.failStep(step.getName(), e.getMessage());
                    System.out.println("  failed: " + e.getMessage());
                    return false;
                }
                run.recordStepAttemptFailure(step.getName(), e.getMessage());   // attempts left: retry
                System.out.println("  attempt " + attempt + " failed: " + e.getMessage()
                        + "; retrying in " + backoffMillis + " ms");
                if (!sleep(backoffMillis)) {                        // interrupted while waiting: stop retrying
                    run.failStep(step.getName(), "Interrupted while waiting to retry");
                    return false;
                }
                backoffMillis = backoffMillis * 2;                  // 500 → 1000 → 2000 ...
            }
        }
        return false;   // only reached if maxAttempts is 0, which Step's constructor should prevent
    }

    /** Waits; returns false if the thread was interrupted while waiting. */
    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    // Returns the first dependency that has not succeeded yet, or an empty Optional if all have   // fix 2
    private Optional<String> findPendingDependency(WorkflowRun run, Step step) {
        for (String dependency : step.getDependsOn()) {
            if (!run.isStepSucceeded(dependency)) {
                return Optional.of(dependency);
            }
        }
        return Optional.empty();
    }
}