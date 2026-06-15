package com.kylin.fast.graph.core;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ⏱ 全链路耗时监控组件 — 可选的、外部传入的、线程安全的。
 *
 * <p>支持节点级 (Phase) 和子任务级 (SubSpan) 两级计时，外部通过
 * {@link #prettyPrint()} 一键输出美化耗时报告。
 *
 * <p>使用方式：
 * <pre>
 * GraphStopwatch sw = GraphStopwatch.create("MyGraph");
 *
 * graph.addNode("agent", state ->
 *     OpenAiAgentNode.create(service, "gpt-4")
 *         .stopwatch(sw)
 *         .apply(state));
 *
 * graph.addNode("action", state ->
 *     OpenAiActionNode.create()
 *         .stopwatch(sw)
 *         .apply(state));
 *
 * sw.prettyPrint();
 * </pre>
 *
 * <p>不传入 stopwatch 时，节点行为与原有完全一致，零性能开销。
 *
 * @author AI Agent
 */
public class GraphStopwatch {

    private final String name;
    private final long createTime;
    private final List<Phase> phases = new ArrayList<>();

    // 线程安全：ConcurrentLinkedQueue 收集来自 parallelStream 的并发 lap
    private final ConcurrentLinkedQueue<PendingLap> pendingLaps = new ConcurrentLinkedQueue<>();

    // ────────── 构造 ──────────

    public GraphStopwatch(String name) {
        this.name = name;
        this.createTime = System.currentTimeMillis();
    }

    public static GraphStopwatch create(String name) {
        return new GraphStopwatch(name);
    }

    // ────────── 记录 API ──────────

    /**
     * 开始一个阶段（如 "agent"、"action"、"tts"）。
     * 如果上一个阶段未结束，自动关闭它。
     */
    public void start(String phaseName) {
        // 自动关闭上一个未结束的 phase
        autoCloseLastPhase();
        Phase phase = new Phase(phaseName);
        phase.startTime = System.currentTimeMillis();
        synchronized (phases) {
            phases.add(phase);
        }
    }

    private void autoCloseLastPhase() {
        synchronized (phases) {
            if (!phases.isEmpty()) {
                Phase last = phases.get(phases.size() - 1);
                if (last.endTime == 0) {
                    last.endTime = System.currentTimeMillis();
                    // flush pending laps
                    PendingLap lap;
                    while ((lap = pendingLaps.poll()) != null) {
                        last.spans.add(new SubSpan(lap.spanName, lap.elapsedMs));
                    }
                }
            }
        }
    }

    /**
     * 记录一个子环节耗时（线程安全，支持 parallelStream 并发调用）
     */
    public void lap(String spanName, long elapsedMs) {
        if (elapsedMs < 0) elapsedMs = 0;
        pendingLaps.add(new PendingLap(spanName, elapsedMs));
    }

    /**
     * 结束当前阶段，将并发收集的 lap 批量 flush 到该 Phase。
     * 如果 phase 已经结束过（endTime > 0），则跳过不重复结束。
     */
    public void end(String phaseName) {
        Phase phase;
        synchronized (phases) {
            phase = phases.isEmpty() ? null : phases.get(phases.size() - 1);
        }
        if (phase == null) return;
        if (phase.endTime > 0) return; // 已经结束过了，跳过

        phase.endTime = System.currentTimeMillis();

        // flush 所有 pending laps 到当前 phase
        PendingLap lap;
        while ((lap = pendingLaps.poll()) != null) {
            phase.spans.add(new SubSpan(lap.spanName, lap.elapsedMs));
        }
    }

    // ────────── 查询 API ──────────

    /**
     * 从第一个 phase 开始到最后一个 phase 结束的总耗时（毫秒）
     */
    public long getTotalMs() {
        synchronized (phases) {
            if (phases.isEmpty()) return 0;
            long first = phases.get(0).startTime;
            long last = phases.get(phases.size() - 1).endTime;
            if (last == 0) last = System.currentTimeMillis();
            return Math.max(0, last - first);
        }
    }

    public List<Phase> getPhases() {
        synchronized (phases) {
            return new ArrayList<>(phases);
        }
    }

    public String getName() {
        return name;
    }

    // ────────── 美化输出 ──────────

    /**
     * 美化打印到 System.out
     */
    public void prettyPrint() {
        System.out.println(prettyString());
    }

    /**
     * 返回美化字符串（可用于 log.info 等）
     */
    public String prettyString() {
        List<Phase> snapshot = getPhases();
        long totalMs = getTotalMs();
        if (snapshot.isEmpty()) {
            return "[GraphStopwatch] " + name + ": no phases recorded.";
        }

        // 计算各列宽度
        int maxPhaseLen = 8; // "Phase" 最小
        int maxTimeLen = 6;  // "耗时" 最小
        for (Phase p : snapshot) {
            maxPhaseLen = Math.max(maxPhaseLen, p.name.length());
            maxTimeLen = Math.max(maxTimeLen, formatTime(p.elapsed()).length());
            for (SubSpan s : p.spans) {
                maxPhaseLen = Math.max(maxPhaseLen, ("  " + s.name).length());
                maxTimeLen = Math.max(maxTimeLen, formatTime(s.elapsedMs).length());
            }
        }
        // 总行
        maxPhaseLen = Math.max(maxPhaseLen, "TOTAL".length());
        maxTimeLen = Math.max(maxTimeLen, formatTime(totalMs).length());

        // 总宽度：边框3 + Phase列 + 3分隔 + 耗时列 + 3分隔 + 占比列(6) + 3分隔 + 详情列(12) + 边框1
        int detailW = Math.max(12, snapshot.size() + " phases".length() + 2);
        int totalW = 2 + maxPhaseLen + 3 + maxTimeLen + 3 + 7 + 3 + detailW + 1;
        totalW = Math.max(totalW, 60);

        StringBuilder sb = new StringBuilder();

        // ── 顶框 + 标题 ──
        String title = "⏱  GraphStopwatch: " + name;
        sb.append(boxTop(totalW)).append("\n");
        sb.append(boxRow(totalW, centerText(title, totalW - 2))).append("\n");
        sb.append(boxSep(totalW)).append("\n");
        // 表头
        sb.append(boxRow(totalW,
                padRight("Phase", maxPhaseLen) + " │ " +
                padRight("耗时", maxTimeLen) + " │ " +
                padRight("占比", 6) + " │ " +
                padRight("详情", detailW)
        )).append("\n");
        sb.append(boxSep(totalW)).append("\n");

        // ── 各 Phase ──
        for (Phase p : snapshot) {
            String icon;
            if (p.name.startsWith("agent")) {
                icon = "🧠 ";
            } else if (p.name.startsWith("action")) {
                icon = "🔧 ";
            } else if (p.name.startsWith("tts")) {
                icon = "🔊 ";
            } else {
                icon = "⏺ ";
            }
            String phaseDisplay = icon + p.name;
            String timeStr = formatTime(p.elapsed());
            String pct = totalMs > 0 ? String.format("%.1f%%", 100.0 * p.elapsed() / totalMs) : "-";

            List<SubSpan> spans = p.spans;
            String detail;
            if (spans.isEmpty()) {
                detail = "";
            } else {
                long toolCount = spans.stream().filter(s -> s.name.startsWith("tool:")).count();
                if (toolCount > 0) {
                    detail = toolCount + " tools";
                } else {
                    detail = spans.size() + " spans";
                }
            }

            sb.append(boxRow(totalW,
                    padRight(phaseDisplay, maxPhaseLen) + " │ " +
                    padRight(timeStr, maxTimeLen) + " │ " +
                    padRight(pct, 6) + " │ " +
                    padRight(detail, detailW)
            )).append("\n");

            // 子 span
            for (int i = 0; i < spans.size(); i++) {
                SubSpan s = spans.get(i);
                String prefix = (i == spans.size() - 1) ? "  └─ " : "  ├─ ";
                sb.append(boxRow(totalW,
                        padRight(prefix + s.name, maxPhaseLen) + " │ " +
                        padRight(formatTime(s.elapsedMs), maxTimeLen) + " │ " +
                        padRight("", 6) + " │ " +
                        padRight("", detailW)
                )).append("\n");
            }
        }

        // ── 底部合计 ──
        sb.append(boxSep(totalW)).append("\n");
        sb.append(boxRow(totalW,
                padRight("📊 TOTAL", maxPhaseLen) + " │ " +
                padRight(formatTime(totalMs), maxTimeLen) + " │ " +
                padRight("100.0%", 6) + " │ " +
                padRight(snapshot.size() + " phases", detailW)
        )).append("\n");
        sb.append(boxBtm(totalW));

        return sb.toString();
    }

    // ────────── 内部类 ──────────

    public static class Phase {
        public final String name;
        public long startTime;
        public long endTime;
        public final List<SubSpan> spans = new ArrayList<>();

        Phase(String name) { this.name = name; }

        public long elapsed() {
            if (endTime == 0 || startTime == 0) return 0;
            return Math.max(0, endTime - startTime);
        }
    }

    public static class SubSpan {
        public final String name;
        public final long elapsedMs;

        SubSpan(String name, long elapsedMs) {
            this.name = name;
            this.elapsedMs = elapsedMs;
        }
    }

    private static class PendingLap {
        final String spanName;
        final long elapsedMs;

        PendingLap(String spanName, long elapsedMs) {
            this.spanName = spanName;
            this.elapsedMs = elapsedMs;
        }
    }

    // ────────── 格式化工具 ──────────

    static String formatTime(long ms) {
        if (ms < 1) return "<1 ms";
        if (ms < 1000) return String.format("%,d ms", ms);
        if (ms < 60_000) return String.format("%.2f s", ms / 1000.0);
        long min = ms / 60_000;
        long sec = (ms % 60_000) / 1000;
        return String.format("%dm %02ds", min, sec);
    }

    // ────────── Box Drawing Helpers ──────────

    private static String boxTop(int w) {
        return "╔" + repeat("═", w - 2) + "╗";
    }

    private static String boxSep(int w) {
        return "╠" + repeat("═", w - 2) + "╣";
    }

    private static String boxBtm(int w) {
        return "╚" + repeat("═", w - 2) + "╝";
    }

    private static String boxRow(int w, String content) {
        return "║ " + padRight(content, w - 4) + " ║";
    }

    private static String centerText(String text, int w) {
        if (text.length() >= w) return text;
        int pad = (w - text.length()) / 2;
        return repeat(" ", pad) + text + repeat(" ", w - text.length() - pad);
    }

    private static String padRight(String s, int len) {
        if (s == null) s = "";
        if (s.length() >= len) return s;
        return s + repeat(" ", len - s.length());
    }

    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
