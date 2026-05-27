package com.kylin.fast.graph.core;

@FunctionalInterface
public interface StreamEmitter {
    void emit(GraphEvent event);
}
