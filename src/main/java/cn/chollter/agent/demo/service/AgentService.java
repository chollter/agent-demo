package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.agent.AgentResponse;
import cn.chollter.agent.demo.agent.Message;
import cn.chollter.agent.demo.core.ReActAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent服务
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ReActAgent agent;

    public AgentService(@Qualifier("reActAgent") ReActAgent agent) {
        this.agent = agent;
    }

    /**
     * 执行任务
     */
    public AgentResponse executeTask(String task) {
        log.info("执行任务: {}", task);
        AgentResponse response = agent.execute(task);

        if (response.isSuccess()) {
            log.info("任务执行成功，答案: {}", response.getFinalAnswer());
        } else {
            log.error("任务执行失败: {}", response.getErrorMessage());
        }

        return response;
    }

    /**
     * 执行任务（带对话历史）
     */
    public AgentResponse executeTaskWithHistory(String task, List<Message> history) {
        log.info("执行任务(带历史): {}", task);
        return agent.execute(task, history);
    }

    /**
     * 获取Agent信息
     */
    public String getAgentInfo() {
        return String.format("Agent名称: %s\n描述: %s\n可用工具: %d个",
            agent.getName(),
            agent.getDescription(),
            agent.getTools().size());
    }
}
