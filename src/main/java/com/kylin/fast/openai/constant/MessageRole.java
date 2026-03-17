package com.kylin.fast.openai.constant;

/**
 * Created by ZengShilin on 2023/3/2 2:03 PM
 */
public enum MessageRole {

    /**
     *
     */
    system("system", "系统消息有助于设置助手的行为，设置情景"),
    assistant("assistant", "助手消息帮助存储先前的响应，也就是上下文"),
    user("user", "用户消息有助于指导助手，按照提问最终回复"),
    @Deprecated
    function("function", "函数调用（废弃）"),
    tool("tool", "函数调用");


    MessageRole(String role, String desc) {
        this.role = role;
        this.desc = desc;
    }

    public final String role;
    private String desc;


    public static MessageRole parse(String role) {
        for (MessageRole channelType : values()) {
            if (channelType.role.equals(role)) {
                return channelType;
            }
        }
        return null;
    }

}
