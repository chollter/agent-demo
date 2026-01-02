package cn.chollter.agent.demo.dto;

/**
 * 任务请求DTO
 */
public class TaskRequest {

    private String task;

    public TaskRequest() {}

    public TaskRequest(String task) {
        this.task = task;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }
}
