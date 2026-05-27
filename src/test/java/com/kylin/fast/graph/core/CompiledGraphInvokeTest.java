package com.kylin.fast.graph.core;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class CompiledGraphInvokeTest {
    public static class FlowState {
        private String data = "";
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    @Test
    public void testInvokeSuccess() {
        StateGraph<FlowState> graph = new StateGraph<>(FlowState::new);
        
        graph.addNode("nodeA", state -> {
            Map<String, Object> update = new HashMap<>();
            update.put("data", "A");
            return update;
        });

        graph.addNode("nodeB", state -> {
            Map<String, Object> update = new HashMap<>();
            update.put("data", state.getData() + "B");
            return update;
        });

        graph.addEdge(StateGraph.START, "nodeA");
        graph.addEdge("nodeA", "nodeB");
        graph.addEdge("nodeB", StateGraph.END);

        StateSaver saver = new InMemorySaver();
        CompiledGraph<FlowState> compiled = graph.compile(saver);

        GraphConfig config = GraphConfig.builder().threadId("thread_invoke_01").build();
        FlowState result = compiled.invoke(new FlowState(), config);

        assertEquals("AB", result.getData());
        
        // 验证最后一个 Checkpoint 保存的 nextNode 为 __end__
        Checkpoint cp = saver.getLatest("thread_invoke_01");
        assertNotNull(cp);
        assertEquals(StateGraph.END, cp.getNextNode());
        assertEquals("AB", cp.getStateSnapshot().get("data"));
    }
}
