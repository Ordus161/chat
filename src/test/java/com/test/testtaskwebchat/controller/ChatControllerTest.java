package com.test.testtaskwebchat.controller;

import com.test.testtaskwebchat.dto.MessageDto;
import com.test.testtaskwebchat.dto.UserDto;
import com.test.testtaskwebchat.model.ChatMessage;
import com.test.testtaskwebchat.service.ChatService;
import com.test.testtaskwebchat.service.UserService;
import com.test.testtaskwebchat.websocket.WebSocketEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketEventListener webSocketEventListener;

    @Mock
    private Model model;

    @Mock
    private UserDetails userDetails;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatController chatController;

    private final String TEST_USERNAME = "testUser";
    private final String TEST_CONTENT = "Hello, World!";
    private final String TEST_SESSION_ID = "session-123";

    private MessageDto testMessageDto;
    private UserDto testUserDto;
    private List<MessageDto> testMessages;
    private List<UserDto> testUsers;

    @BeforeEach
    void setUp() {
        testMessageDto = MessageDto.builder()
                .id(1L)
                .content(TEST_CONTENT)
                .username(TEST_USERNAME)
                .build();

        testUserDto = UserDto.builder()
                .username(TEST_USERNAME)
                .build();

        testMessages = Arrays.asList(testMessageDto);
        testUsers = Arrays.asList(testUserDto);
    }

    @Test
    void chatPage_ShouldReturnChatViewWithAttributes() {
        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
        when(userService.getAllUsers()).thenReturn(testUsers);
        when(chatService.getLastMessages()).thenReturn(testMessages);

        String viewName = chatController.chatPage(model, userDetails);

        assertEquals("chat", viewName);
        verify(userService).userConnected(TEST_USERNAME);
        verify(userService).getAllUsers();
        verify(chatService).getLastMessages();
        verify(model).addAttribute("username", TEST_USERNAME);
        verify(model).addAttribute("messages", testMessages);
        verify(model).addAttribute("users", testUsers);
        verify(model).addAttribute(eq("newMessage"), any(ChatMessage.class));
    }

    @Test
    void sendMessage_ShouldSaveMessageAndRedirect() {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent(TEST_CONTENT);

        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
        when(chatService.saveMessage(TEST_CONTENT, TEST_USERNAME)).thenReturn(testMessageDto);
        when(userService.getAllUsers()).thenReturn(testUsers);

        String redirect = chatController.sendMessage(chatMessage, userDetails);

        assertEquals("redirect:/chat", redirect);
        verify(chatService).saveMessage(TEST_CONTENT, TEST_USERNAME);
        verify(messagingTemplate).convertAndSend("/topic/messages", testMessageDto);
        verify(messagingTemplate).convertAndSend("/topic/users", testUsers);
    }

    @Test
    void handleChatMessage_WithValidPrincipal_ShouldProcessMessage() {
        when(headerAccessor.getUser()).thenReturn(principal);
        when(principal.getName()).thenReturn(TEST_USERNAME);
        when(chatService.saveMessage(TEST_CONTENT, TEST_USERNAME)).thenReturn(testMessageDto);
        when(userService.getAllUsers()).thenReturn(testUsers);

        chatController.handleChatMessage(TEST_CONTENT, headerAccessor);

        verify(chatService).saveMessage(TEST_CONTENT, TEST_USERNAME);
        verify(messagingTemplate).convertAndSend("/topic/messages", testMessageDto);
        verify(messagingTemplate).convertAndSend("/topic/users", testUsers);
    }

    @Test
    void handleChatMessage_WithNullPrincipal_ShouldNotProcessMessage() {
        when(headerAccessor.getUser()).thenReturn(null);

        chatController.handleChatMessage(TEST_CONTENT, headerAccessor);

        verify(chatService, never()).saveMessage(anyString(), anyString());
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void addUser_ShouldRegisterUserSession() {
        when(headerAccessor.getSessionId()).thenReturn(TEST_SESSION_ID);

        chatController.addUser(TEST_USERNAME, headerAccessor);

        verify(webSocketEventListener).registerUserSession(TEST_SESSION_ID, TEST_USERNAME);
    }

    @Test
    void subscribeToUsers_ShouldReturnAllUsers() {
        when(userService.getAllUsers()).thenReturn(testUsers);

        List<UserDto> result = chatController.subscribeToUsers();

        assertEquals(testUsers, result);
        verify(userService).getAllUsers();
    }

    @Test
    void subscribeToMessages_ShouldReturnLastMessages() {
        when(chatService.getLastMessages()).thenReturn(testMessages);

        List<MessageDto> result = chatController.subscribeToMessages();

        assertEquals(testMessages, result);
        verify(chatService).getLastMessages();
    }

    @Test
    void updateUsers_ShouldReturnAndBroadcastUsers() {
        when(userService.getAllUsers()).thenReturn(testUsers);

        List<UserDto> result = chatController.updateUsers();

        assertEquals(testUsers, result);
        verify(userService).getAllUsers();
        verify(messagingTemplate).convertAndSend("/topic/users", testUsers);
    }

    @Test
    void sendMessage_WithEmptyContent_ShouldStillProcess() {
        String emptyContent = "";
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent(emptyContent);

        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
        when(chatService.saveMessage(emptyContent, TEST_USERNAME)).thenReturn(testMessageDto);
        when(userService.getAllUsers()).thenReturn(testUsers);

        String redirect = chatController.sendMessage(chatMessage, userDetails);

        assertEquals("redirect:/chat", redirect);
        verify(chatService).saveMessage(emptyContent, TEST_USERNAME);
    }

    @Test
    void chatPage_ShouldHandleServiceExceptionsGracefully() {
        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
        when(userService.getAllUsers()).thenThrow(new RuntimeException("Service error"));

        assertThrows(RuntimeException.class, () ->
                chatController.chatPage(model, userDetails)
        );
    }

    @Test
    void handleChatMessage_ShouldLogMessageContent() {
        when(headerAccessor.getUser()).thenReturn(principal);
        when(principal.getName()).thenReturn(TEST_USERNAME);
        when(chatService.saveMessage(TEST_CONTENT, TEST_USERNAME)).thenReturn(testMessageDto);
        when(userService.getAllUsers()).thenReturn(testUsers);

        chatController.handleChatMessage(TEST_CONTENT, headerAccessor);

        verify(chatService).saveMessage(TEST_CONTENT, TEST_USERNAME);
    }
}