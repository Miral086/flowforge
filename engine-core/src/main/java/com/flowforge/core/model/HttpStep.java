package com.flowforge.core.model;

import com.flowforge.core.exception.StepExecutionException;

import java.util.List;

public class HttpStep extends Step {

    private final String url;

    public HttpStep(String name, List<String> dependsOn, String url) {
        super(name, dependsOn);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String execute() throws StepExecutionException {
        return "GET " + url;
    }

    @Override
    public String toString() {
        return "HttpStep{" + super.toString() + ", url=" + url + "}";
    }
}
