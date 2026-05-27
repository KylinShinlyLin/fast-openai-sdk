package com.kylin.fast.graph.core;

public class GraphContext {
    private static final ThreadLocal<StreamEmitter> EMITTER_HOLDER = new ThreadLocal<>();

    public static void setEmitter(StreamEmitter emitter) {
        EMITTER_HOLDER.set(emitter);
    }

    public static StreamEmitter getEmitter() {
        return EMITTER_HOLDER.get();
    }

    public static void clear() {
        EMITTER_HOLDER.remove();
    }
}
