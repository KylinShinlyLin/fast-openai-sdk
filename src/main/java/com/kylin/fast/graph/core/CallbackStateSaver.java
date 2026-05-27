package com.kylin.fast.graph.core;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 🌟 函数式委托状态保存器 (CallbackStateSaver)
 * 利用 Java 8 Lambda 表达式，让用户连类都无需编写，直接一句话传入两个/三个 Callback，
 * 即可零负担对接外部 Redis, JDBC 数据库, 飞书, 或者是你的自定义微服务存储！
 *
 * @author AI Agent
 */
public class CallbackStateSaver implements StateSaver {

    private final BiConsumer<String, Checkpoint> writeCallback;
    private final Function<String, Checkpoint> readLatestCallback;
    private final BiFunction<String, String, Checkpoint> readCallback;

    /**
     * 极简构造：如果只关心写入和读取最新快照
     */
    public CallbackStateSaver(BiConsumer<String, Checkpoint> writeCallback, Function<String, Checkpoint> readLatestCallback) {
        this(writeCallback, readLatestCallback, (tid, cpid) -> null);
    }

    /**
     * 完备构造：包含写、读最新、根据 ID 精准读取历史
     */
    public CallbackStateSaver(BiConsumer<String, Checkpoint> writeCallback, 
                              Function<String, Checkpoint> readLatestCallback, 
                              BiFunction<String, String, Checkpoint> readCallback) {
        this.writeCallback = writeCallback;
        this.readLatestCallback = readLatestCallback;
        this.readCallback = readCallback;
    }

    @Override
    public void put(String threadId, String checkpointId, Checkpoint checkpoint) {
        if (writeCallback != null) {
            writeCallback.accept(threadId, checkpoint);
        }
    }

    @Override
    public Checkpoint get(String threadId, String checkpointId) {
        return readCallback != null ? readCallback.apply(threadId, checkpointId) : null;
    }

    @Override
    public Checkpoint getLatest(String threadId) {
        return readLatestCallback != null ? readLatestCallback.apply(threadId) : null;
    }
}
