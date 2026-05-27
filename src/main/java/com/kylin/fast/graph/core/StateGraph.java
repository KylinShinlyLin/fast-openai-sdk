package com.kylin.fast.graph.core;

import lombok.Getter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class StateGraph<S> {
    public static final String START = "__start__";
    public static final String END = "__end__";

    @Getter
    private final Supplier<S> stateSupplier;
    @Getter
    private final Map<String, Node<S>> nodes = new HashMap<>();
    @Getter
    private final Map<String, String> edges = new HashMap<>();
    @Getter
    private final Map<String, ConditionalEdge<S>> conditionalEdges = new HashMap<>();
    private final Map<String, BiFunction<Object, Object, Object>> reducers = new HashMap<>();

    public StateGraph(Supplier<S> stateSupplier) {
        this.stateSupplier = stateSupplier;
    }

    public void addNode(String name, Node<S> node) {
        nodes.put(name, node);
    }

    public void addEdge(String from, String to) {
        edges.put(from, to);
    }

    public void addConditionalEdges(String from, Function<S, String> routingFunction, Map<String, String> pathMap) {
        conditionalEdges.put(from, new ConditionalEdge<>(routingFunction, pathMap));
    }

    public void registerReducer(String fieldName, BiFunction<Object, Object, Object> reducer) {
        reducers.put(fieldName, reducer);
    }

    @SuppressWarnings("unchecked")
    public S mergeState(S currentState, Map<String, Object> update) {
        if (update == null || update.isEmpty()) {
            return currentState;
        }
        try {
            Class<?> clazz = currentState.getClass();
            for (Map.Entry<String, Object> entry : update.entrySet()) {
                String key = entry.getKey();
                Object newVal = entry.getValue();
                
                Field field = getDeclaredField(clazz, key);
                if (field != null) {
                    field.setAccessible(true);
                    Object oldVal = field.get(currentState);
                    Object finalVal;
                    
                    if (reducers.containsKey(key)) {
                        finalVal = reducers.get(key).apply(oldVal, newVal);
                    } else {
                        finalVal = newVal; // 覆盖模式
                    }
                    field.set(currentState, finalVal);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to merge state: " + e.getMessage(), e);
        }
        return currentState;
    }

    private Field getDeclaredField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public CompiledGraph<S> compile(StateSaver saver) {
        return new CompiledGraph<>(this, saver);
    }

    public static class ConditionalEdge<S> {
        private final Function<S, String> routingFunction;
        private final Map<String, String> pathMap;

        public ConditionalEdge(Function<S, String> routingFunction, Map<String, String> pathMap) {
            this.routingFunction = routingFunction;
            this.pathMap = pathMap;
        }

        public Function<S, String> getRoutingFunction() { return routingFunction; }
        public Map<String, String> getPathMap() { return pathMap; }
    }
}
