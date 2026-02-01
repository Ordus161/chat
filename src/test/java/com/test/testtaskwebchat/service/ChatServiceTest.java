package com.test.testtaskwebchat.service;

import com.test.testtaskwebchat.dto.MessageDto;
import com.test.testtaskwebchat.model.ChatMessage;
import com.test.testtaskwebchat.model.ChatUser;
import com.test.testtaskwebchat.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void getLastMessages_ShouldReturnLimitedMessages() {
        MessageDto message1 = new MessageDto(1L, "Message 1", "user1", LocalDateTime.now().minusHours(1));
        MessageDto message2 = new MessageDto(2L, "Message 2", "user2", LocalDateTime.now().minusMinutes(30));
        MessageDto message3 = new MessageDto(3L, "Message 3", "user3", LocalDateTime.now());

        List<MessageDto> allMessages = Arrays.asList(message1, message2, message3);
        when(messageRepository.findAllMessagesOrderedByDateDesc()).thenReturn(allMessages);

        List<MessageDto> result = chatService.getLastMessages();

        assertEquals(3, result.size());
        assertEquals("Message 1", result.get(0).getContent());
        assertEquals("Message 2", result.get(1).getContent());
        assertEquals("Message 3", result.get(2).getContent());
        verify(messageRepository).findAllMessagesOrderedByDateDesc();
    }

    @Test
    void getLastMessages_ShouldLimitTo50Messages() {
        List<MessageDto> manyMessages = createMessages(60);
        when(messageRepository.findAllMessagesOrderedByDateDesc()).thenReturn(manyMessages);

        List<MessageDto> result = chatService.getLastMessages();

        assertEquals(50, result.size());
        verify(messageRepository).findAllMessagesOrderedByDateDesc();
    }

    @Test
    void getLastMessages_WithEmptyRepository_ShouldReturnEmptyList() {
        when(messageRepository.findAllMessagesOrderedByDateDesc()).thenReturn(Arrays.asList());

        List<MessageDto> result = chatService.getLastMessages();

        assertTrue(result.isEmpty());
        verify(messageRepository).findAllMessagesOrderedByDateDesc();
    }

    @Test
    void getLastMessages_ShouldReturnMessagesInCorrectOrder() {
        LocalDateTime now = LocalDateTime.now();
        MessageDto oldMessage = new MessageDto(1L, "Old", "user1", now.minusHours(2));
        MessageDto newMessage = new MessageDto(2L, "New", "user2", now);

        when(messageRepository.findAllMessagesOrderedByDateDesc()).thenReturn(Arrays.asList(newMessage, oldMessage));

        List<MessageDto> result = chatService.getLastMessages();

        assertEquals(2, result.size());
        assertEquals("New", result.get(0).getContent());
        assertEquals("Old", result.get(1).getContent());
    }

    @Test
    void saveMessage_WithValidUser_ShouldSaveAndReturnMessageDto() {
        String username = "testUser";
        String content = "Hello World";

        ChatUser user = new ChatUser();
        user.setUsername(username);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(1L)
                .content(content)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageDto result = chatService.saveMessage(content, username);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(content, result.getContent());
        assertEquals(username, result.getUsername());
        assertNotNull(result.getCreatedAt());

        verify(userService).findByUsername(username);
        verify(messageRepository).save(argThat(message ->
                content.equals(message.getContent()) &&
                        user.equals(message.getUser())
        ));
    }

    @Test
    void saveMessage_WithNonExistentUser_ShouldThrowException() {
        String username = "nonExistentUser";
        String content = "Test message";

        when(userService.findByUsername(username)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> chatService.saveMessage(content, username));

        assertEquals("User not found", exception.getMessage());
        verify(userService).findByUsername(username);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void saveMessage_WithEmptyContent_ShouldSaveMessage() {
        String username = "testUser";
        String content = "";

        ChatUser user = new ChatUser();
        user.setUsername(username);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(1L)
                .content(content)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageDto result = chatService.saveMessage(content, username);

        assertNotNull(result);
        assertEquals("", result.getContent());
        verify(messageRepository).save(argThat(message ->
                message.getContent().isEmpty()
        ));
    }

    @Test
    void saveMessage_WithNullContent_ShouldSaveMessage() {
        String username = "testUser";
        String content = null;

        ChatUser user = new ChatUser();
        user.setUsername(username);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(1L)
                .content(content)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageDto result = chatService.saveMessage(content, username);

        assertNotNull(result);
        assertNull(result.getContent());
    }

    @Test
    void saveMessage_WithMessageDto_ShouldSaveMessage() {
        String username = "testUser";
        MessageDto messageDto = new MessageDto(null, "DTO Content", null, null);

        ChatUser user = new ChatUser();
        user.setUsername(username);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(1L)
                .content("DTO Content")
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageDto result = chatService.saveMessage(messageDto, username);

        assertNotNull(result);
        assertEquals("DTO Content", result.getContent());
        assertEquals(username, result.getUsername());

        verify(userService).findByUsername(username);
        verify(messageRepository).save(argThat(message ->
                "DTO Content".equals(message.getContent())
        ));
    }

    @Test
    void saveMessage_ShouldPreserveAllMessageProperties() {
        String username = "testUser";
        String content = "Message with\ntwo lines";

        ChatUser user = ChatUser.builder()
                .username(username)
                .firstName("John")
                .lastName("Doe")
                .build();

        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        ChatMessage savedMessage = ChatMessage.builder()
                .id(100L)
                .content(content)
                .user(user)
                .createdAt(createdAt)
                .build();

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageDto result = chatService.saveMessage(content, username);

        assertEquals(100L, result.getId());
        assertEquals(content, result.getContent());
        assertEquals(username, result.getUsername());
        assertEquals(createdAt, result.getCreatedAt());
    }

    @Test
    void saveMessage_WithRepositoryException_ShouldPropagateException() {
        String username = "testUser";
        String content = "Test";

        ChatUser user = new ChatUser();
        user.setUsername(username);

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> chatService.saveMessage(content, username));
    }

    @Test
    void saveMessage_ShouldHandleSpecialCharacters() {
        String username = "testUser";
        String content = "Message with спецсимволы! @#$%^&*()";

        ChatUser user = new ChatUser();
        user.setUsername(username);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(1L)
                .content(content)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageDto result = chatService.saveMessage(content, username);

        assertEquals(content, result.getContent());
    }

    @Test
    void saveMessage_WithLongContent_ShouldSaveMessage() {
        String username = "testUser";
        String content = "A".repeat(1000);

        ChatUser user = new ChatUser();
        user.setUsername(username);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(1L)
                .content(content)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        MessageDto result = chatService.saveMessage(content, username);

        assertEquals(content.length(), result.getContent().length());
    }

    private List<MessageDto> createMessages(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new MessageDto(
                        (long) i,
                        "Message " + i,
                        "user" + (i % 5),
                        LocalDateTime.now().minusMinutes(i)
                ))
                .collect(java.util.stream.Collectors.toList());
    }
}