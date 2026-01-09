package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.entity.Conversation;
import cn.chollter.agent.demo.entity.Execution;
import cn.chollter.agent.demo.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ExecutionService 测试类
 *
 * @author Chollter
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

    @Mock
    private ExecutionRepository executionRepository;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ExecutionService executionService;

    private Conversation testConversation;
    private Execution testExecution;

    @BeforeEach
    void setUp() {
        testConversation = new Conversation();
        testConversation.setId(1L);
        testConversation.setConversationId("conv-123");
        testConversation.setTitle("Test Conversation");

        testExecution = new Execution();
        testExecution.setId(1L);
        testExecution.setExecutionId("exec-123");
        testExecution.setConversation(testConversation);
        testExecution.setTask("Test task");
        testExecution.setStatus(Execution.ExecutionStatus.IN_PROGRESS);
    }

    @Test
    void testCreateExecution() {
        when(conversationService.getConversationByConversationId("conv-123"))
                .thenReturn(Optional.of(testConversation));
        when(executionRepository.save(any(Execution.class))).thenReturn(testExecution);

        Execution result = executionService.createExecution("conv-123", "Test task");

        assertNotNull(result);
        assertEquals("exec-123", result.getExecutionId());
        assertEquals("Test task", result.getTask());
        assertEquals(Execution.ExecutionStatus.IN_PROGRESS, result.getStatus());

        verify(executionRepository, times(1)).save(any(Execution.class));
    }

    @Test
    void testCompleteExecution() {
        when(executionRepository.findByExecutionId("exec-123"))
                .thenReturn(Optional.of(testExecution));
        when(executionRepository.save(any(Execution.class))).thenReturn(testExecution);

        executionService.completeExecution(
                "exec-123",
                "Final answer",
                List.of(),
                5,
                1000L
        );

        assertEquals("Final answer", testExecution.getFinalAnswer());
        assertEquals(5, testExecution.getSteps());
        assertEquals(1000L, testExecution.getDurationMs());
        assertEquals(Execution.ExecutionStatus.COMPLETED, testExecution.getStatus());
        assertTrue(testExecution.getSuccess());

        verify(executionRepository, times(1)).save(testExecution);
    }

    @Test
    void testFailExecution() {
        when(executionRepository.findByExecutionId("exec-123"))
                .thenReturn(Optional.of(testExecution));
        when(executionRepository.save(any(Execution.class))).thenReturn(testExecution);

        executionService.failExecution("exec-123", "Error occurred", 500L);

        assertEquals("Error occurred", testExecution.getErrorMessage());
        assertEquals(500L, testExecution.getDurationMs());
        assertEquals(Execution.ExecutionStatus.FAILED, testExecution.getStatus());
        assertFalse(testExecution.getSuccess());

        verify(executionRepository, times(1)).save(testExecution);
    }

    @Test
    void testGetExecutionByExecutionId() {
        when(executionRepository.findByExecutionId("exec-123"))
                .thenReturn(Optional.of(testExecution));

        Optional<Execution> result = executionService.getExecutionByExecutionId("exec-123");

        assertTrue(result.isPresent());
        assertEquals("exec-123", result.get().getExecutionId());
        verify(executionRepository, times(1)).findByExecutionId("exec-123");
    }

    @Test
    void testGetSuccessfulExecutionCount() {
        when(executionRepository.countSuccessfulExecutions()).thenReturn(10L);

        long count = executionService.getSuccessfulExecutionCount();

        assertEquals(10L, count);
        verify(executionRepository, times(1)).countSuccessfulExecutions();
    }

    @Test
    void testGetFailedExecutionCount() {
        when(executionRepository.countFailedExecutions()).thenReturn(2L);

        long count = executionService.getFailedExecutionCount();

        assertEquals(2L, count);
        verify(executionRepository, times(1)).countFailedExecutions();
    }
}
