package com.test.testtaskwebchat.websocket;

import com.test.testtaskwebchat.dto.UserDto;
import com.test.testtaskwebchat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    private final ConcurrentHashMap<String, String> sessionUsernameMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Новое WebSocket подключение");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String username = sessionUsernameMap.get(sessionId);
        if (username != null) {
            log.info("Пользователь отключился: {}", username);
            sessionUsernameMap.remove(sessionId);
            userService.userDisconnected(username);

            List<UserDto> users = userService.getAllUsers();
            messagingTemplate.convertAndSend("/topic/users", users);
        }
    }

    public void registerUserSession(String sessionId, String username) {
        sessionUsernameMap.put(sessionId, username);
        userService.userConnected(username);

        List<UserDto> users = userService.getAllUsers();
        messagingTemplate.convertAndSend("/topic/users", users);
    }

    public void removeUserSession(String sessionId) {
        sessionUsernameMap.remove(sessionId);
    }
}
