package cn.chollter.agent.demo.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 任务请求DTO
 */
@Setter
@Getter
public class TaskRequest {

    private String task;

    public TaskRequest() {}

    public TaskRequest(String task) {
        this.task = task;
    }

}
