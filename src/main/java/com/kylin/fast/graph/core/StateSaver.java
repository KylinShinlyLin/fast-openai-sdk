package com.kylin.fast.graph.core;

public interface StateSaver {
    void put(String threadId, String checkpointId, Checkpoint checkpoint);
    Checkpoint get(String threadId, String checkpointId);
    Checkpoint getLatest(String threadId);
}
