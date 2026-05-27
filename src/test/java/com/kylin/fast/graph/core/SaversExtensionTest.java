package com.kylin.fast.graph.core;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class SaversExtensionTest {

    @Test
    public void testCallbackStateSaver() {
        // 模拟用户在外部持有的一个自定义的 KV 存储（如 Redis 或者是 ConcurrentHashMap 或者是本地数据库）
        Map<String, String> externalRedisMock = new HashMap<>();

        // 🌟 仅仅一句话：利用 Java 8 函数式表达式，极速对接并注入外部存储！
        StateSaver callbackSaver = new CallbackStateSaver(
                (threadId, cp) -> {
                    // 写入回调：将 cp 序列化为 JSON 写入你的外部存储
                    externalRedisMock.put(threadId, JSON.toJSONString(cp));
                },
                (threadId) -> {
                    // 读取回调：从你的外部存储读取 JSON 并还原
                    String json = externalRedisMock.get(threadId);
                    return json == null ? null : JSON.parseObject(json, Checkpoint.class);
                }
        );

        Map<String, Object> stateSnapshot = new HashMap<>();
        stateSnapshot.put("user_name", "shilin");

        Checkpoint cp = new Checkpoint("session_001", "cp_999", stateSnapshot, "agent", System.currentTimeMillis());
        callbackSaver.put("session_001", "cp_999", cp);

        // 验证外部 Mock 存储中是否真的存在了序列化好的 JSON 串！
        assertTrue(externalRedisMock.containsKey("session_001"));
        assertTrue(externalRedisMock.get("session_001").contains("shilin"));

        // 验证通过 CallbackSaver 从外部读取出的状态
        Checkpoint retrieved = callbackSaver.getLatest("session_001");
        assertNotNull(retrieved);
        assertEquals("agent", retrieved.getNextNode());
        assertEquals("shilin", retrieved.getStateSnapshot().get("user_name"));
    }

    @Test
    public void testFileStateSaver(@TempDir File tempDir) {
        // 🌟 使用 FileStateSaver 指定一个本地物理文件夹 (此处使用 JUnit5 提供的临时测试夹具临时文件夹)
        StateSaver fileSaver = new FileStateSaver(tempDir);

        Map<String, Object> stateSnapshot = new HashMap<>();
        stateSnapshot.put("score", 100);

        Checkpoint cp = new Checkpoint("user_777", "cp_file_01", stateSnapshot, "human_input", System.currentTimeMillis());
        fileSaver.put("user_777", "cp_file_01", cp);

        // 验证磁盘上是否真的生成了物理文件！
        File userDir = new File(tempDir, "user_777");
        assertTrue(userDir.exists());
        
        File cpFile = new File(userDir, "cp_file_01.json");
        assertTrue(cpFile.exists());

        File latestFile = new File(userDir, "latest.json");
        assertTrue(latestFile.exists());

        // 验证从物理磁盘读取状态还原
        Checkpoint retrievedLatest = fileSaver.getLatest("user_777");
        assertNotNull(retrievedLatest);
        assertEquals("human_input", retrievedLatest.getNextNode());
        assertEquals(100, retrievedLatest.getStateSnapshot().get("score"));

        Checkpoint retrievedById = fileSaver.get("user_777", "cp_file_01");
        assertNotNull(retrievedById);
        assertEquals(100, retrievedById.getStateSnapshot().get("score"));
    }
}
