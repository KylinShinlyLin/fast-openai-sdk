package com.kylin.fast.graph.core;

import java.util.Map;

@FunctionalInterface
public interface Node<S> {
    Map<String, Object> apply(S state);
}
