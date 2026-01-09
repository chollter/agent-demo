package cn.chollter.agent.demo.service;

import cn.chollter.agent.demo.entity.Conversation;
import cn.chollter.agent.demo.repository.ConversationRepository;
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
 * ConversationService 测试类
 *
 * @author Chollter
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private ConversationService conversationService;

    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        testConversation = new Conversation();
        testConversation.setId(1L);
        testConversation.setConversationId("conv-123");
        testConversation.setTitle("Test Conversation");
        testConversation.setModelProvider("ollama");
        testConversation.setModelName("qwen2.5:7b");
        testConversation.setStatus(Conversation.ConversationStatus.ACTIVE);
    }

    @Test
    void testCreateConversation() {
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        Conversation result = conversationService.createConversation(
                "Test Conversation Title",
                "ollama",
                "qwen2.5:7b"
        );

        assertNotNull(result);
        assertEquals("conv-123", result.getConversationId());
        assertEquals("Test Conversation", result.getTitle());
        assertEquals(Conversation.ConversationStatus.ACTIVE, result.getStatus());

        verify(conversationRepository, times(1)).save(any(Conversation.class));
    }

    @Test
    void testGetConversationById() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(testConversation));

        Optional<Conversation> result = conversationService.getConversationById(1L);

        assertTrue(result.isPresent());
        assertEquals("conv-123", result.get().getConversationId());
        verify(conversationRepository, times(1)).findById(1L);
    }

    @Test
    void testGetConversationByConversationId() {
        when(conversationRepository.findByConversationId("conv-123")).thenReturn(Optional.of(testConversation));

        Optional<Conversation> result = conversationService.getConversationByConversationId("conv-123");

        assertTrue(result.isPresent());
        assertEquals("conv-123", result.get().getConversationId());
        verify(conversationRepository, times(1)).findByConversationId("conv-123");
    }

    @Test
    void testArchiveConversation() {
        when(conversationRepository.findByConversationId("conv-123")).thenReturn(Optional.of(testConversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        conversationService.archiveConversation("conv-123");

        assertEquals(Conversation.ConversationStatus.ARCHIVED, testConversation.getStatus());
        verify(conversationRepository, times(1)).save(testConversation);
    }

    @Test
    void testDeleteConversation() {
        when(conversationRepository.findByConversationId("conv-123")).thenReturn(Optional.of(testConversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        conversationService.deleteConversation("conv-123");

        assertEquals(Conversation.ConversationStatus.DELETED, testConversation.getStatus());
        verify(conversationRepository, times(1)).save(testConversation);
    }
}
