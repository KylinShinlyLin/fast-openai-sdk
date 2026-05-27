package com.kylin.fast.graph.core;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class InMemorySaverTest {
    @Test
    public void testSaveAndRetrieve() {
        StateSaver saver = new InMemorySaver();
        
        Map<String, Object> stateMap = new HashMap<>();
        stateMap.put("someKey", "someVal");
        
        Checkpoint cp = new Checkpoint("thread_01", "cp_01", stateMap, "nodeA", System.currentTimeMillis());
        saver.put("thread_01", "cp_01", cp);
        
        Checkpoint retrieved = saver.getLatest("thread_01");
        assertNotNull(retrieved);
        assertEquals("nodeA", retrieved.getNextNode());
        assertEquals("someVal", retrieved.getStateSnapshot().get("someKey"));
    }
}
