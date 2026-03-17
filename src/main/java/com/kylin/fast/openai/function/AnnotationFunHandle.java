package com.kylin.fast.openai.function;

import com.alibaba.fastjson.JSONObject;
import com.kylin.fast.openai.constant.MessageRole;
import com.kylin.fast.openai.function.annotation.AiFunction;
import com.kylin.fast.openai.function.annotation.AiFunctionParam;
import com.kylin.fast.openai.request.ChatRequest;
import com.kylin.fast.openai.request.dto.Message;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class AnnotationFunHandle extends AbstractFunHandle {

    private final Object handler;
    private final Method method;
    private final String name;
    private final String description;

    public AnnotationFunHandle(Object handler, Method method) {
        this.handler = handler;
        this.method = method;
        AiFunction annotation = method.getAnnotation(AiFunction.class);
        this.name = annotation.name().isEmpty() ? method.getName() : annotation.name();
        this.description = annotation.description();
    }

    @Override
    public String functionName() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public JSONObject parametersType() {
        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");

        JSONObject properties = new JSONObject();
        List<String> required = new ArrayList<>();

        for (Parameter param : method.getParameters()) {
            AiFunctionParam paramAnnotation = param.getAnnotation(AiFunctionParam.class);
            String paramName = paramAnnotation != null ? paramAnnotation.name() : param.getName();

            JSONObject paramSchema = new JSONObject();
            paramSchema.put("description", paramAnnotation != null ? paramAnnotation.description() : "");

            Class<?> type = param.getType();
            if (type == String.class) {
                paramSchema.put("type", "string");
            } else if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
                paramSchema.put("type", "integer");
            } else if (type == boolean.class || type == Boolean.class) {
                paramSchema.put("type", "boolean");
            } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
                paramSchema.put("type", "number");
            } else if (type.isEnum()) {
                paramSchema.put("type", "string");
                List<String> enumValues = new ArrayList<>();
                for (Object enumConstant : type.getEnumConstants()) {
                    enumValues.add(enumConstant.toString());
                }
                paramSchema.put("enum", enumValues);
            } else if (Collection.class.isAssignableFrom(type)) {
                paramSchema.put("type", "array");
                JSONObject items = new JSONObject();
                Type parameterizedType = param.getParameterizedType();
                if (parameterizedType instanceof ParameterizedType) {
                    Type actualTypeArgument = ((ParameterizedType) parameterizedType).getActualTypeArguments()[0];
                    if (actualTypeArgument == String.class) {
                        items.put("type", "string");
                    } else if (actualTypeArgument == Integer.class || actualTypeArgument == int.class || actualTypeArgument == Long.class || actualTypeArgument == long.class) {
                        items.put("type", "integer");
                    } else if (actualTypeArgument == Double.class || actualTypeArgument == double.class || actualTypeArgument == Float.class || actualTypeArgument == float.class) {
                        items.put("type", "number");
                    } else if (actualTypeArgument == Boolean.class || actualTypeArgument == boolean.class) {
                        items.put("type", "boolean");
                    } else {
                        items.put("type", "string");
                    }
                } else {
                    items.put("type", "string");
                }
                paramSchema.put("items", items);
            } else {
                paramSchema.put("type", "string");
            }

            properties.put(paramName, paramSchema);

            boolean isRequired = paramAnnotation == null || paramAnnotation.required();
            if (paramAnnotation != null && !"\u0000".equals(paramAnnotation.defaultValue())) {
                isRequired = false;
            }
            if (isRequired) {
                required.add(paramName);
            }
        }

        parameters.put("properties", properties);
        if (!required.isEmpty()) {
            parameters.put("required", required);
        }
        return parameters;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void handle(JSONObject param, ChatRequest request, String id) {
        try {
            Object[] args = new Object[method.getParameterCount()];
            Parameter[] parameters = method.getParameters();

            for (int i = 0; i < parameters.length; i++) {
                Parameter p = parameters[i];
                AiFunctionParam paramAnnotation = p.getAnnotation(AiFunctionParam.class);
                String name = paramAnnotation != null ? paramAnnotation.name() : p.getName();

                if (param.containsKey(name) && param.get(name) != null) {
                    args[i] = param.getObject(name, p.getType());
                } else {
                    if (paramAnnotation != null && !"\u0000".equals(paramAnnotation.defaultValue())) {
                        String defStr = paramAnnotation.defaultValue();
                        Class<?> type = p.getType();
                        if (type == String.class) {
                            args[i] = defStr;
                        } else if (type == int.class || type == Integer.class) {
                            args[i] = Integer.parseInt(defStr);
                        } else if (type == long.class || type == Long.class) {
                            args[i] = Long.parseLong(defStr);
                        } else if (type == double.class || type == Double.class) {
                            args[i] = Double.parseDouble(defStr);
                        } else if (type == float.class || type == Float.class) {
                            args[i] = Float.parseFloat(defStr);
                        } else if (type == boolean.class || type == Boolean.class) {
                            args[i] = Boolean.parseBoolean(defStr);
                        } else if (type.isEnum()) {
                            args[i] = Enum.valueOf((Class<Enum>) type, defStr);
                        } else {
                            args[i] = com.alibaba.fastjson.JSON.parseObject(defStr, type);
                        }
                    } else {
                        Class<?> type = p.getType();
                        if (type.isPrimitive()) {
                            if (type == boolean.class) args[i] = false;
                            else if (type == char.class) args[i] = '\u0000';
                            else if (type == byte.class) args[i] = (byte) 0;
                            else if (type == short.class) args[i] = (short) 0;
                            else if (type == int.class) args[i] = 0;
                            else if (type == long.class) args[i] = 0L;
                            else if (type == float.class) args[i] = 0.0f;
                            else if (type == double.class) args[i] = 0.0d;
                        } else {
                            args[i] = null;
                        }
                    }
                }
            }

            Object result = method.invoke(handler, args);
            String resultStr = result != null ? result.toString() : "success";

            Message toolMessage = new Message();
            toolMessage.setRole(MessageRole.tool.role);
            toolMessage.setContent(resultStr);
            toolMessage.setToolCallId(id);
            request.addMessage(toolMessage);

        } catch (Exception e) {
            log.error("Error executing function {}", name, e);
            Message errorMessage = new Message();
            errorMessage.setRole(MessageRole.tool.role);
            errorMessage.setContent("Error executing function: " + e.getMessage());
            errorMessage.setToolCallId(id);
            request.addMessage(errorMessage);
        }
    }
}
