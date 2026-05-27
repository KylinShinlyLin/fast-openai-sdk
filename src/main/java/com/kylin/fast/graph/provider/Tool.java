package com.kylin.fast.graph.provider;

public interface Tool {
    String getName();
    String getDescription();
    String getParametersSchema();
    String execute(String argumentsJSON) throws Exception;
}
