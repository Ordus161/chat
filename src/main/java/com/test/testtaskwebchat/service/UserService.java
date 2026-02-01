package com.test.testtaskwebchat.service;

import com.test.testtaskwebchat.model.ChatUser;
import com.test.testtaskwebchat.dto.UserDto;
import com.test.testtaskwebchat.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final ChatUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, LocalDateTime> onlineUsers = new ConcurrentHashMap<>();

    public ChatUser registerNewUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        ChatUser user = ChatUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public Optional<ChatUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void userConnected(String username) {
        onlineUsers.put(username, LocalDateTime.now());
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void userDisconnected(String username) {
        onlineUsers.remove(username);
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public List<UserDto> getAllUsers() {
        List<String> allUsernames = userRepository.findAllUsernames();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        return allUsernames.stream()
                .map(username -> {
                    boolean online = onlineUsers.containsKey(username);
                    String lastSeen = "";

                    if (!online) {
                        lastSeen = userRepository.findByUsername(username)
                                .map(user -> user.getLastSeen() != null ?
                                        user.getLastSeen().format(formatter) : "Never")
                                .orElse("Never");
                    }

                    return new UserDto(username, online, lastSeen);
                })
                .toList();
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }
}
