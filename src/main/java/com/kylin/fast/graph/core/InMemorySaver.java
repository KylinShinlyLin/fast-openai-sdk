package com.kylin.fast.graph.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class InMemorySaver implements StateSaver {
    private final Map<String, Map<String, Checkpoint>> storage = new ConcurrentHashMap<>();
    private final Map<String, Checkpoint> latestHolder = new ConcurrentHashMap<>();

    @Override
    public void put(String threadId, String checkpointId, Checkpoint checkpoint) {
        storage.computeIfAbsent(threadId, k -> new ConcurrentHashMap<>()).put(checkpointId, checkpoint);
        
        Checkpoint last = latestHolder.get(threadId);
        if (last == null || checkpoint.getCreatedAt() >= last.getCreatedAt()) {
            latestHolder.put(threadId, checkpoint);
        }
    }

    @Override
    public Checkpoint get(String threadId, String checkpointId) {
        Map<String, Checkpoint> threadMap = storage.get(threadId);
        return threadMap == null ? null : threadMap.get(checkpointId);
    }

    @Override
    public Checkpoint getLatest(String threadId) {
        return latestHolder.get(threadId);
    }
}
