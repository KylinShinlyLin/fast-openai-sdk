package com.kylin.fast.graph.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class StateGraphTest {
    public static class TestState {
        private List<String> listVal = new ArrayList<>();
        private String strVal;
        
        public List<String> getListVal() { return listVal; }
        public void setListVal(List<String> listVal) { this.listVal = listVal; }
        public String getStrVal() { return strVal; }
        public void setStrVal(String strVal) { this.strVal = strVal; }
    }

    @Test
    public void testStateReducerMerging() {
        StateGraph<TestState> graph = new StateGraph<>(TestState::new);
        
        // 注册 List 类型的 Reducer 追加合并器
        graph.registerReducer("listVal", (oldVal, newVal) -> {
            List<String> merged = new ArrayList<>();
            if (oldVal != null) merged.addAll((Collection<String>) oldVal);
            if (newVal != null) merged.addAll((Collection<String>) newVal);
            return merged;
        });

        TestState state = new TestState();
        state.getListVal().add("msg1");
        state.setStrVal("original");

        Map<String, Object> update = new HashMap<>();
        update.put("listVal", Collections.singletonList("msg2"));
        update.put("strVal", "updated");

        TestState mergedState = graph.mergeState(state, update);
        
        assertEquals(2, mergedState.getListVal().size());
        assertTrue(mergedState.getListVal().contains("msg1"));
        assertTrue(mergedState.getListVal().contains("msg2"));
        assertEquals("updated", mergedState.getStrVal());
    }
}
