package com.kylin.fast.graph.core;


import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CompiledGraphStreamTest {
    public static class ChatState {
        private List<String> log = new ArrayList<>();

        public List<String> getLog() {
            return log;
        }

        public void setLog(List<String> log) {
            this.log = log;
        }
    }

    @Test
    public void testStreamEventsAndInterrupt() {
        StateGraph<ChatState> graph = getChatStateStateGraph();

        StateSaver saver = new InMemorySaver();
        CompiledGraph<ChatState> compiled = graph.compile(saver);

        // 设定在 human_approval 节点执行前进行 Interrupt 中断
        GraphConfig config = GraphConfig.builder()
                .threadId("th_stream_01")
                .interruptBefore(Collections.singletonList("human_approval"))
                .build();

        Iterator<GraphEvent> stream = compiled.stream(new ChatState(), config);

        List<String> eventTypes = new ArrayList<>();
        List<String> tokens = new ArrayList<>();

        while (stream.hasNext()) {
            GraphEvent e = stream.next();
            eventTypes.add(e.getType());
            if ("token".equals(e.getType())) {
                tokens.add((String) e.getPayload());
            }
        }

        // 验证前半段事件流
        assertTrue(eventTypes.contains("node_start"));
        assertTrue(eventTypes.contains("token"));
        assertTrue(eventTypes.contains("node_end"));
        assertEquals(2, tokens.size());
        assertEquals("word-1", tokens.get(0));

        // 验证已被中断，此时 human_approval 没有被执行
        Checkpoint latest = saver.getLatest("th_stream_01");
        assertNotNull(latest);
        assertEquals("human_approval", latest.getNextNode());
        System.out.println("--- SNAPSHOT LOG: " + latest.getStateSnapshot().get("log"));

        // 二次调用：从检查点恢复，不带中断参数
        GraphConfig resumeConfig = GraphConfig.builder()
                .threadId("th_stream_01")
                .build();

        ChatState resumedState = compiled.invoke(new ChatState(), resumeConfig);
        assertTrue(resumedState.getLog().contains("agent_done"));
        assertTrue(resumedState.getLog().contains("approved"));
    }

    private static StateGraph<ChatState> getChatStateStateGraph() {
        StateGraph<ChatState> graph = new StateGraph<>(ChatState::new);
        graph.registerReducer("log", (o, n) -> {
            List<String> m = new ArrayList<>((Collection<String>) o);
            m.addAll((Collection<String>) n);
            return m;
        });

        graph.addNode("agent", state -> {
            StreamEmitter emitter = GraphContext.getEmitter();
            if (emitter != null) {
                emitter.emit(new GraphEvent("token", "agent", "word-1"));
                emitter.emit(new GraphEvent("token", "agent", "word-2"));
            }
            Map<String, Object> up = new HashMap<>();
            up.put("log", Collections.singletonList("agent_done"));
            return up;
        });

        graph.addNode("human_approval", state -> {
            Map<String, Object> up = new HashMap<>();
            up.put("log", Collections.singletonList("approved"));
            return up;
        });

        graph.addEdge(StateGraph.START, "agent");
        graph.addEdge("agent", "human_approval");
        graph.addEdge("human_approval", StateGraph.END);
        return graph;
    }
}
