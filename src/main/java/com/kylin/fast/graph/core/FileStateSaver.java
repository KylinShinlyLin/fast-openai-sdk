package com.kylin.fast.graph.core;

import com.alibaba.fastjson.JSON;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 🌟 本地文件磁盘持久化保存器 (FileStateSaver)
 * 开箱即用的物理磁盘持久化器！将 Checkpoint 会话自动转换为格式化的 JSON 字符串落盘。
 * 支持会话断电保存、故障恢复和单机历史追溯。
 *
 * @author AI Agent
 */
public class FileStateSaver implements StateSaver {

    private final File baseDir;

    public FileStateSaver(String baseDirPath) {
        this.baseDir = new File(baseDirPath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    public FileStateSaver(File baseDir) {
        this.baseDir = baseDir;
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void put(String threadId, String checkpointId, Checkpoint checkpoint) {
        try {
            File threadDir = new File(baseDir, threadId);
            if (!threadDir.exists()) {
                threadDir.mkdirs();
            }

            // 1. 将 checkpoint 序列化为 JSON 字符串并写入具体文件
            File cpFile = new File(threadDir, checkpointId + ".json");
            writeStringToFile(cpFile, JSON.toJSONString(checkpoint, true));

            // 2. 保持最新的 latest.json 指向，供 getLatest 读取
            File latestFile = new File(threadDir, "latest.json");
            writeStringToFile(latestFile, JSON.toJSONString(checkpoint, true));

        } catch (Exception e) {
            throw new RuntimeException("FileStateSaver failed to save checkpoint: " + e.getMessage(), e);
        }
    }

    @Override
    public Checkpoint get(String threadId, String checkpointId) {
        File cpFile = new File(new File(baseDir, threadId), checkpointId + ".json");
        if (!cpFile.exists()) {
            return null;
        }
        try {
            String json = readStringFromFile(cpFile);
            return JSON.parseObject(json, Checkpoint.class);
        } catch (Exception e) {
            throw new RuntimeException("FileStateSaver failed to load checkpoint: " + e.getMessage(), e);
        }
    }

    @Override
    public Checkpoint getLatest(String threadId) {
        File latestFile = new File(new File(baseDir, threadId), "latest.json");
        if (!latestFile.exists()) {
            return null;
        }
        try {
            String json = readStringFromFile(latestFile);
            return JSON.parseObject(json, Checkpoint.class);
        } catch (Exception e) {
            throw new RuntimeException("FileStateSaver failed to load latest checkpoint: " + e.getMessage(), e);
        }
    }

    private void writeStringToFile(File file, String data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readStringFromFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
