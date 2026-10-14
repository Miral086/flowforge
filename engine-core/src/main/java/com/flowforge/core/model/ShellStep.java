package com.flowforge.core.model;

import com.flowforge.core.exception.InvalidWorkflowException;
import com.flowforge.core.exception.StepExecutionException;

import java.io.IOException;
import java.util.List;

public class ShellStep extends Step {

    private final String command;

    public ShellStep(String name, List<String> dependsOn, String command) {
        this(name, dependsOn, command, 1);
    }

    public ShellStep(String name, List<String> dependsOn, String command, int maxAttempts){
        super(name, dependsOn, maxAttempts);
        if(command == null || command.isBlank()){
            throw new InvalidWorkflowException("Shell step " + name + " needs a command.");
        }
        this.command = command;

    }

    public String getCommand() {
        return command;
    }

    @Override
    public String execute() throws StepExecutionException {
        List<String> cmd = List.of("cmd.exe", "/c", command);
        try{
            // 1. Start the command. redirectErrorStream merges error text into normal output.
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            // 2. Read everything it prints (blocks until the command closes its output).
            String output = new String(process.getInputStream().readAllBytes()).strip();

            // 3. Wait for it to finish and get the exit code.
            int exitCode = process.waitFor();

            // 4. Non-zero exit code = the command failed. Raise our own exception.
            if (exitCode != 0) {
                throw new StepExecutionException(getName(),
                        "Command exited with code " + exitCode + (output.isEmpty() ? "" : ": " + output));
            }
            return output;                               // success: hand the output back

        } catch (IOException e) {
            // Couldn't start the command or read its output: translate, keep the cause
            throw new StepExecutionException(getName(), "Could not run command: " + e.getMessage(), e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();          // restore the flag, then translate
            throw new StepExecutionException(getName(), "Interrupted while running command", e);
        }
    }

    @Override
    public String toString() {
        return "ShellStep{" + super.toString() + ", command=" + command + "}";
    }
}
