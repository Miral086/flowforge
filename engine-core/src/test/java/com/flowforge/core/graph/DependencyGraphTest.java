package com.flowforge.core.graph;

import com.flowforge.core.exception.InvalidWorkflowException;
import com.flowforge.core.model.ShellStep;
import com.flowforge.core.model.Workflow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyGraphTest {

    private static ShellStep step(String name, String... dependsOn) {   // small helper to keep tests short
        return new ShellStep(name, List.of(dependsOn), "echo " + name);
    }

    @Test
    void ordersDiamondWorkflow() {
        Workflow diamond = new Workflow("diamond", List.of(
                step("checkout"),
                step("build", "checkout"),
                step("lint", "checkout"),
                step("test", "build"),
                step("deploy", "build", "lint")));

        List<String> order = new DependencyGraph(diamond).topologicalOrder();

        assertEquals(List.of("checkout", "build", "lint", "test", "deploy"), order);
    }

    @Test
    void putsDependenciesFirstEvenWhenListedLast() {
        Workflow backwards = new Workflow("backwards", List.of(
                step("test", "build"),
                step("build", "checkout"),
                step("checkout")));

        List<String> order = new DependencyGraph(backwards).topologicalOrder();

        assertTrue(order.indexOf("checkout") < order.indexOf("build"));
        assertTrue(order.indexOf("build") < order.indexOf("test"));
    }

    @Test
    void rejectsCycle() {
        Workflow cyclic = new Workflow("cyclic", List.of(
                step("checkout"),
                step("build", "test"),
                step("test", "deploy"),
                step("deploy", "build")));

        InvalidWorkflowException e = assertThrows(InvalidWorkflowException.class,
                () -> new DependencyGraph(cyclic).topologicalOrder());

        assertTrue(e.getMessage().contains("build"));
    }

    @Test
    void sortingTwiceGivesSameResult() {
        DependencyGraph graph = new DependencyGraph(new Workflow("simple", List.of(
                step("a"), step("b", "a"))));

        assertEquals(graph.topologicalOrder(), graph.topologicalOrder());
    }

    @Test
    void joinWaitsForAllDependencies() {
        // Arrange
        Workflow workflow = new Workflow("join", List.of(
                new ShellStep("build", List.of(), "echo build"),
                new ShellStep("lint", List.of(), "echo lint"),
                new ShellStep("deploy", List.of("build", "lint"), "echo deploy")));

        // Act
        List<String> order = new DependencyGraph(workflow).topologicalOrder();

        // Assert
        assertTrue(order.indexOf("deploy") > order.indexOf("build"));
        assertTrue(order.indexOf("deploy") > order.indexOf("lint"));
    }
}