package com.test.testtaskwebchat.service;

import com.test.testtaskwebchat.dto.UserDto;
import com.test.testtaskwebchat.model.ChatUser;
import com.test.testtaskwebchat.repository.ChatUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private ChatUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private ChatUser testUser;

    @BeforeEach
    void setUp() {
        testUser = ChatUser.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerNewUser_WithNewUsername_ShouldRegisterSuccessfully() {
        String username = "newuser";
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(ChatUser.class))).thenAnswer(invocation -> {
            ChatUser user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        ChatUser result = userService.registerNewUser(username, rawPassword);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(encodedPassword, result.getPassword());
        assertNotNull(result.getCreatedAt());

        verify(userRepository).existsByUsername(username);
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(user ->
                username.equals(user.getUsername()) &&
                        encodedPassword.equals(user.getPassword())
        ));
    }

    @Test
    void registerNewUser_WithExistingUsername_ShouldThrowException() {
        String username = "existinguser";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.registerNewUser(username, password));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByUsername(username);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_WithExistingUser_ShouldReturnUser() {
        String username = "testuser";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        Optional<ChatUser> result = userService.findByUsername(username);

        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void userConnected_ShouldAddToOnlineUsersAndUpdateLastSeen() {
        String username = "testuser";
        LocalDateTime now = LocalDateTime.now();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(ChatUser.class))).thenReturn(testUser);

        userService.userConnected(username);

        verify(userRepository).findByUsername(username);
        verify(userRepository).save(argThat(user ->
                user.getLastSeen() != null
        ));
    }

    @Test
    void userDisconnected_ShouldRemoveFromOnlineUsersAndUpdateLastSeen() {
        String username = "testuser";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        userService.userConnected(username);
        userService.userDisconnected(username);

        verify(userRepository, times(2)).save(argThat(user ->
                user.getLastSeen() != null
        ));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsersWithOnlineStatus() {
        String user1 = "user1";
        String user2 = "user2";
        String user3 = "user3";

        List<String> usernames = Arrays.asList(user1, user2, user3);
        when(userRepository.findAllUsernames()).thenReturn(usernames);

        when(userRepository.findByUsername(user1)).thenReturn(Optional.of(
                ChatUser.builder().username(user1).lastSeen(LocalDateTime.now()).build()));
        when(userRepository.findByUsername(user2)).thenReturn(Optional.of(
                ChatUser.builder().username(user2).lastSeen(null).build()));
        when(userRepository.findByUsername(user3)).thenReturn(Optional.empty());

        userService.userConnected(user1);

        List<UserDto> result = userService.getAllUsers();

        assertEquals(3, result.size());

        UserDto user1Dto = result.stream().filter(u -> u.getUsername().equals(user1)).findFirst().get();
        assertTrue(user1Dto.isOnline());
        assertEquals("", user1Dto.getLastSeen());

        UserDto user2Dto = result.stream().filter(u -> u.getUsername().equals(user2)).findFirst().get();
        assertFalse(user2Dto.isOnline());
        assertEquals("Never", user2Dto.getLastSeen());

        UserDto user3Dto = result.stream().filter(u -> u.getUsername().equals(user3)).findFirst().get();
        assertFalse(user3Dto.isOnline());
        assertEquals("Never", user3Dto.getLastSeen());
    }

    @Test
    void getAllUsers_ShouldFormatLastSeenTime() {
        String username = "testuser";
        LocalDateTime lastSeen = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
        String expectedTime = "14:30:45";

        List<String> usernames = Arrays.asList(username);
        when(userRepository.findAllUsernames()).thenReturn(usernames);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(
                ChatUser.builder().username(username).lastSeen(lastSeen).build()));

        List<UserDto> result = userService.getAllUsers();

        assertEquals(1, result.size());
        UserDto userDto = result.get(0);
        assertEquals(expectedTime, userDto.getLastSeen());
        assertFalse(userDto.isOnline());
    }

    @Test
    void getAllUsers_WithEmptyRepository_ShouldReturnEmptyList() {
        when(userRepository.findAllUsernames()).thenReturn(Arrays.asList());

        List<UserDto> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
        verify(userRepository).findAllUsernames();
    }

    @Test
    void getAllUsers_WithOnlineUser_ShouldNotShowLastSeen() {
        String username = "onlineuser";
        List<String> usernames = Arrays.asList(username);

        when(userRepository.findAllUsernames()).thenReturn(usernames);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(
                ChatUser.builder().username(username).lastSeen(LocalDateTime.now()).build()));

        userService.userConnected(username);

        List<UserDto> result = userService.getAllUsers();

        UserDto userDto = result.get(0);
        assertTrue(userDto.isOnline());
        assertEquals("", userDto.getLastSeen());
    }

    @Test
    void userConnected_ShouldBeIdempotent() {
        String username = "testuser";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        userService.userConnected(username);
        userService.userConnected(username);

        verify(userRepository, times(2)).save(any());
    }

    @Test
    void registerNewUser_WithSpecialCharactersInUsername_ShouldWork() {
        String username = "user.name-123_456";
        String password = "pass";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encoded");
        when(userRepository.save(any(ChatUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatUser result = userService.registerNewUser(username, password);

        assertEquals(username, result.getUsername());
    }

    @Test
    void userConnected_WithNonExistentUser_ShouldNotCrash() {
        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.userConnected(username));
        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
    }

    @Test
    void userDisconnected_WithOnlineUser_ShouldWorkCorrectly() {
        String username = "testuser";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        userService.userConnected(username);
        verify(userRepository).save(any());

        userService.userDisconnected(username);
        verify(userRepository, times(2)).save(any());
    }

    @Test
    void getAllUsers_ShouldHandleMultipleOnlineUsers() {
        String user1 = "user1";
        String user2 = "user2";

        List<String> usernames = Arrays.asList(user1, user2);
        when(userRepository.findAllUsernames()).thenReturn(usernames);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(
                ChatUser.builder().username("test").lastSeen(LocalDateTime.now()).build()));

        userService.userConnected(user1);
        userService.userConnected(user2);

        List<UserDto> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(UserDto::isOnline));
        assertTrue(result.stream().allMatch(u -> u.getLastSeen().isEmpty()));
    }

    @Test
    void getAllUsers_ShouldReturnUsersInCorrectOrder() {
        List<String> usernames = Arrays.asList("user3", "user1", "user2");
        when(userRepository.findAllUsernames()).thenReturn(usernames);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(
                ChatUser.builder().username("test").lastSeen(LocalDateTime.now()).build()));

        List<UserDto> result = userService.getAllUsers();

        assertEquals(3, result.size());
        assertEquals("user3", result.get(0).getUsername());
        assertEquals("user1", result.get(1).getUsername());
        assertEquals("user2", result.get(2).getUsername());
    }
}