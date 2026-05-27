package com.kylin.fast.graph.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kylin.fast.openai.function.AnnotationFunHandle;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.GptFunction;
import com.kylin.fast.openai.request.dto.GptTool;
import com.kylin.fast.openai.request.dto.Message;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class CompiledGraph<S> {
    private final StateGraph<S> graph;
    private final StateSaver saver;

    // 动态注册的注解工具链缓存
    private final List<GptTool> gptTools = new ArrayList<>();
    private final List<AnnotationFunHandle> funHandles = new ArrayList<>();

    public CompiledGraph(StateGraph<S> graph, StateSaver saver) {
        this.graph = graph;
        this.saver = saver;
    }

    /**
     * 极简注册：直接 new MyWeatherTools() 传入，自动动态反射解析为大模型声明与调用句柄
     */
    public CompiledGraph<S> registerTools(Object... handlers) {
        if (handlers == null) return this;
        for (Object handler : handlers) {
            if (handler == null) continue;
            for (Method method : handler.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(AiFunction.class)) {
                    AnnotationFunHandle handle = new AnnotationFunHandle(handler, method);
                    this.funHandles.add(handle);

                    // 自动生成 GptTool 元数据声明给大模型
                    GptFunction f = GptFunction.builder()
                            .name(handle.functionName())
                            .description(handle.description())
                            .parameters(handle.parametersType())
                            .build();
                    this.gptTools.add(GptTool.of(f));
                }
            }
        }
        return this;
    }

    public List<AnnotationFunHandle> getRegisterHandles() {
        return funHandles;
    }

    public List<Method> getRegisterMethods() {
        List<Method> methods = new ArrayList<>();
        for (AnnotationFunHandle handle : funHandles) {
            methods.add(handle.getMethod());
        }
        return methods;
    }

    public List<GptTool> getGptTools() {
        return gptTools;
    }

    /**
     * 一键根据 toolName 寻找并触发对应的注解方法反射，并获取格式化好的 Message
     */
    public Message executeTool(String toolName, String argumentsJson, String toolCallId) {
        for (AnnotationFunHandle handle : funHandles) {
            if (handle.functionName().equals(toolName)) {
                ChatRequest dummy = ChatRequest.builder().build();
                JSONObject args = JSON.parseObject(argumentsJson);
                handle.handle(args, dummy, toolCallId);
                if (dummy.getMessages() != null && !dummy.getMessages().isEmpty()) {
                    return (Message) dummy.getMessages().get(0);
                }
            }
        }
        return null;
    }

    public S invoke(S initialState, GraphConfig config) {
        Iterator<GraphEvent> streamIt = stream(initialState, config);
        S lastState = initialState;
        while (streamIt.hasNext()) {
            GraphEvent event = streamIt.next();
            if ("error".equals(event.getType())) {
                System.err.println(">>> [ENGINE ERROR] " + event.getPayload());
            }
            if ("node_end".equals(event.getType())) {
                @SuppressWarnings("unchecked")
                S currentRunState = (S) event.getPayload();
                if (currentRunState != null) {
                    lastState = currentRunState;
                }
            }
        }
        return lastState;
    }

    public Iterator<GraphEvent> stream(S initialState, GraphConfig config) {
        LinkedBlockingQueue<GraphEvent> queue = new LinkedBlockingQueue<>();

        new Thread(() -> {
            try {
                driveGraph(initialState, config, queue);
            } catch (Exception e) {
                queue.offer(new GraphEvent("error", null, e.getMessage()));
            } finally {
                queue.offer(new GraphEvent("end_of_stream", null, null));
            }
        }).start();

        return new Iterator<GraphEvent>() {
            private GraphEvent nextEvent = null;
            private boolean checked = false;

            private void fetchNext() {
                if (!checked) {
                    try {
                        nextEvent = queue.take();
                    } catch (InterruptedException e) {
                        nextEvent = null;
                    }
                    checked = true;
                }
            }

            @Override
            public boolean hasNext() {
                fetchNext();
                return nextEvent != null && !"end_of_stream".equals(nextEvent.getType());
            }

            @Override
            public GraphEvent next() {
                fetchNext();
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                GraphEvent current = nextEvent;
                checked = false;
                nextEvent = null;
                return current;
            }
        };
    }

    private void driveGraph(S state, GraphConfig config, LinkedBlockingQueue<GraphEvent> queue) throws Exception {
        String threadId = config.getThreadId();
        String currentNode = StateGraph.START;
        S runState = state;

        // 检查是否有现有 Checkpoint
        if (threadId != null && saver != null) {
            Checkpoint latest = saver.getLatest(threadId);
            if (latest != null && !StateGraph.END.equals(latest.getNextNode())) {
                currentNode = latest.getNextNode();
                runState = reconstructState(latest.getStateSnapshot());
            }
        }

        // 如果是全新执行，先获取 START 的下一个具体节点
        if (StateGraph.START.equals(currentNode)) {
            currentNode = getNextNodeTarget(StateGraph.START, runState);
        }

        // 运行生命周期驱动
        while (currentNode != null && !StateGraph.END.equals(currentNode)) {
            // 1. 检查 interruptBefore
            if (config.getInterruptBefore() != null && config.getInterruptBefore().contains(currentNode)) {
                saveCheckpoint(threadId, currentNode, runState);
                break; // 挂起并退出
            }

            queue.put(new GraphEvent("node_start", currentNode, null));

            // 2. 执行节点
            Node<S> nodeExecutor = graph.getNodes().get(currentNode);
            if (nodeExecutor == null) {
                throw new IllegalStateException("Node not found in graph: " + currentNode);
            }

            // 注入 Emitter 支持 Token 透传
            GraphContext.setEmitter(queue::offer);
            Map<String, Object> update;
            try {
                update = nodeExecutor.apply(runState);
            } finally {
                GraphContext.clear();
            }

            // 合并更新
            runState = graph.mergeState(runState, update);

            // 发射节点结束事件
            queue.put(new GraphEvent("node_end", currentNode, runState));

            // 3. 计算下一个节点
            String nextNode = getNextNodeTarget(currentNode, runState);

            // 4. 检查 interruptAfter
            if (config.getInterruptAfter() != null && config.getInterruptAfter().contains(currentNode)) {
                saveCheckpoint(threadId, nextNode, runState);
                break;
            }

            currentNode = nextNode;
        }

        // 保存最终结束 Checkpoint (当没有挂起断点且正常结束时)
        if (StateGraph.END.equals(currentNode) && threadId != null && saver != null) {
            saveCheckpoint(threadId, StateGraph.END, runState);
        }
    }

    private String getNextNodeTarget(String currentNode, S state) {
        if (StateGraph.START.equals(currentNode)) {
            return graph.getEdges().get(StateGraph.START);
        }

        // 优先检查条件边
        if (graph.getConditionalEdges().containsKey(currentNode)) {
            StateGraph.ConditionalEdge<S> cond = graph.getConditionalEdges().get(currentNode);
            String routeKey = cond.getRoutingFunction().apply(state);
            return cond.getPathMap().get(routeKey);
        }

        // 普通边
        return graph.getEdges().getOrDefault(currentNode, StateGraph.END);
    }

    private void saveCheckpoint(String threadId, String nextNode, S state) {
        if (threadId == null || saver == null) return;
        Map<String, Object> snapshot = captureStateSnapshot(state);
        String cpId = UUID.randomUUID().toString();
        Checkpoint cp = new Checkpoint(threadId, cpId, snapshot, nextNode, System.currentTimeMillis());
        saver.put(threadId, cpId, cp);
    }

    private Map<String, Object> captureStateSnapshot(S state) {
        Map<String, Object> snapshot = new HashMap<>();
        try {
            Class<?> clazz = state.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    snapshot.put(field.getName(), field.get(state));
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to capture snapshot: " + e.getMessage(), e);
        }
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private S reconstructState(Map<String, Object> snapshot) {
        S instance = graph.getStateSupplier().get();
        try {
            Class<?> clazz = instance.getClass();
            for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
                Field field = getDeclaredField(clazz, entry.getKey());
                if (field != null) {
                    field.setAccessible(true);
                    Object val = entry.getValue();

                    // 🌟 解决泛型擦除：如果字段是 List 且从 JSON 还原出的数据为 Collection / JSONArray，执行运行时高保真自动转换！
                    if (val != null && List.class.isAssignableFrom(field.getType()) && (val instanceof Collection)) {
                        java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) field.getGenericType();
                        if (pt.getActualTypeArguments() != null && pt.getActualTypeArguments().length > 0) {
                            Class<?> actualType = (Class<?>) pt.getActualTypeArguments()[0];
                            String listJson = JSON.toJSONString(val);
                            val = JSON.parseArray(listJson, actualType);
                        }
                    }

                    field.set(instance, val);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct state: " + e.getMessage(), e);
        }
        return instance;
    }

    private Field getDeclaredField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
