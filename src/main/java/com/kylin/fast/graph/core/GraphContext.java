package com.kylin.fast.graph.core;

/**
 * 图执行上下文 —— ThreadLocal 隔离，线程安全
 * <p>
 * 在节点执行期间，通过 ThreadLocal 向节点透明注入：
 * <ul>
 *   <li>{@link StreamEmitter} — 流式 token 发射器</li>
 *   <li>{@link CompiledGraph} — 已编译的图实例（工具注册中心）</li>
 * </ul>
 */
public class GraphContext {
    private static final ThreadLocal<StreamEmitter> EMITTER_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<CompiledGraph<?>> COMPILED_HOLDER = new ThreadLocal<>();

    // ---------- Emitter ----------

    public static void setEmitter(StreamEmitter emitter) {
        EMITTER_HOLDER.set(emitter);
    }

    public static StreamEmitter getEmitter() {
        return EMITTER_HOLDER.get();
    }

    // ---------- CompiledGraph ----------

    public static void setCompiledGraph(CompiledGraph<?> compiled) {
        COMPILED_HOLDER.set(compiled);
    }

    @SuppressWarnings("unchecked")
    public static <S> CompiledGraph<S> getCompiledGraph() {
        return (CompiledGraph<S>) COMPILED_HOLDER.get();
    }

    // ---------- Clear ----------

    public static void clear() {
        EMITTER_HOLDER.remove();
        COMPILED_HOLDER.remove();
    }
}
