package cn.chollter.agent.demo.agent;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息实体
 */
@Setter
@Getter
public class Message {

    private Role role;
    private String content;

    public Message() {}

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
