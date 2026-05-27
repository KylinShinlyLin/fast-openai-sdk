package com.kylin.fast.graph.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class GraphContextTest {
    @Test
    public void testContextAndEventEmit() {
        List<GraphEvent> emitted = new ArrayList<>();
        StreamEmitter emitter = emitted::add;
        
        GraphContext.setEmitter(emitter);
        
        try {
            GraphContext.getEmitter().emit(new GraphEvent("token", "agent", "word"));
            assertEquals(1, emitted.size());
            assertEquals("token", emitted.get(0).getType());
            assertEquals("word", emitted.get(0).getPayload());
        } finally {
            GraphContext.clear();
        }
        
        assertNull(GraphContext.getEmitter());
    }
}
