package com.test.testtaskwebchat.controller;

import com.test.testtaskwebchat.dto.MessageDto;
import com.test.testtaskwebchat.dto.UserDto;
import com.test.testtaskwebchat.model.ChatMessage;
import com.test.testtaskwebchat.service.ChatService;
import com.test.testtaskwebchat.service.UserService;
import com.test.testtaskwebchat.websocket.WebSocketEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener webSocketEventListener;

    @GetMapping("/chat")
    public String chatPage(Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        userService.userConnected(username);

        List<MessageDto> lastMessages = chatService.getLastMessages();
        List<UserDto> allUsers = userService.getAllUsers();

        model.addAttribute("username", username);
        model.addAttribute("messages", lastMessages);
        model.addAttribute("users", allUsers);
        model.addAttribute("newMessage", new ChatMessage());

        return "chat";
    }

    @PostMapping("/chat/send")
    public String sendMessage(@ModelAttribute("newMessage") ChatMessage message,
                              @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        MessageDto savedMessage = chatService.saveMessage(message.getContent(), username);

        messagingTemplate.convertAndSend("/topic/messages", savedMessage);

        List<UserDto> users = userService.getAllUsers();
        messagingTemplate.convertAndSend("/topic/users", users);

        return "redirect:/chat";
    }

    @MessageMapping("/chat.send")
    public void handleChatMessage(String content, SimpMessageHeaderAccessor headerAccessor) {

        // Получаем пользователя из заголовков
        java.security.Principal principal = headerAccessor.getUser();
        if (principal == null) {
            log.warn("Пользователь не аутентифицирован");
            return;
        }

        String username = principal.getName();
        log.info("Получено сообщение через WebSocket от {}: {}", username, content);

        // Сохраняем сообщение
        MessageDto savedMessage = chatService.saveMessage(content, username);

        // Отправляем сообщение всем - будет использован JSON конвертер
        messagingTemplate.convertAndSend("/topic/messages", savedMessage);

        // Обновляем список пользователей - тоже будет использован JSON конвертер
        List<UserDto> users = userService.getAllUsers();
        messagingTemplate.convertAndSend("/topic/users", users);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(String username,
                        org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("Пользователь {} подключился через WebSocket, сессия: {}", username, sessionId);

        webSocketEventListener.registerUserSession(sessionId, username);
    }

    @SubscribeMapping("/topic/users")
    public List<UserDto> subscribeToUsers() {
        return userService.getAllUsers();
    }

    @SubscribeMapping("/topic/messages")
    public List<MessageDto> subscribeToMessages() {
        return chatService.getLastMessages();
    }

    @GetMapping("/chat/users/update")
    @ResponseBody
    public List<UserDto> updateUsers() {
        List<UserDto> users = userService.getAllUsers();
        messagingTemplate.convertAndSend("/topic/users", users);
        return users;
    }
}

