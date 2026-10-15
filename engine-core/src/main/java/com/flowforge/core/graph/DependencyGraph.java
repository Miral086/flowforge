package com.flowforge.core.graph;

import com.flowforge.core.exception.InvalidWorkflowException;
import com.flowforge.core.model.Step;
import com.flowforge.core.model.Workflow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Queue;

public class DependencyGraph {
    private final Map<String, List<String>> dependents = new LinkedHashMap<>();
    private final Map<String, Integer> inDegree = new LinkedHashMap<>();

    public DependencyGraph(Workflow workflow){
        for(Step steps: workflow.getSteps()){
            dependents.put(steps.getName(), new ArrayList<>());
            inDegree.put(steps.getName(),0);
        }
        for(Step step: workflow.getSteps()){
            for(String dependency: step.getDependsOn()){
                dependents.get(dependency).add(step.getName());
                inDegree.compute(step.getName(), (name, count) -> count + 1);
            }
        }

    }

    private void requireKnownStep(String stepName) {
        if (!dependents.containsKey(stepName)) {
            throw new IllegalArgumentException("Unknown step: " + stepName);
        }
    }

    public List<String> getDependents(String stepName) {
        requireKnownStep(stepName);
        return List.copyOf(dependents.get(stepName));
    }
    public int getInDegree(String stepName) {
        requireKnownStep(stepName);
        return inDegree.get(stepName);                 // Integer → int automatically (auto-unboxing)
    }

    public List<String> getStepNames() {
        return List.copyOf(dependents.keySet());
    }

    @Override
    public String toString() {
        return "DependencyGraph{dependents=" + dependents + ", inDegree=" + inDegree + "}";
    }

    public List<String> topologicalOrder() {
        // 1. Working copy of the in-degrees: we decrement these, never the graph's own map
        Map<String, Integer> remaining = new LinkedHashMap<>(inDegree);

        // 2. Steps that wait for nothing can go first
        Queue<String> ready = new ArrayDeque<>();
        for (String stepName : remaining.keySet()) {
            if (remaining.get(stepName) == 0) {
                ready.offer(stepName);
            }
        }

        List<String> order = new ArrayList<>();

        // 3. Keep taking a ready step until none are left
        while (!ready.isEmpty()) {
            String current = ready.poll();          // a. take the next ready step...
            order.add(current);                     //    ...and place it in the order

            for (String dependent : dependents.get(current)) {     // b. everyone waiting on it
                int count = remaining.get(dependent) - 1;          //    now waits for one fewer step
                remaining.put(dependent, count);
                if (count == 0) {                                  //    nothing left to wait for:
                    ready.offer(dependent);                        //    it's ready too
                }
            }
        }
        if (order.size() < remaining.size()) {
            List<String> stuck = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : remaining.entrySet()) {
                if (entry.getValue() > 0) {            // still waiting on something that never finished
                    stuck.add(entry.getKey());
                }
            }
            throw new InvalidWorkflowException("Cycle detected among steps: " + stuck);
        }

        // 4. The finished order (Step 4 adds a check here for cycles)
        return List.copyOf(order);
    }
}
