package com.test.testtaskwebchat.service;

import com.test.testtaskwebchat.model.ChatUser;
import com.test.testtaskwebchat.repository.ChatUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private ChatUserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_WithExistingUser_ShouldReturnUserDetails() {
        String username = "testuser";
        String password = "encodedPassword";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void loadUserByUsername_WithNonExistentUser_ShouldThrowException() {
        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(username));

        assertEquals("User not found: " + username, exception.getMessage());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void loadUserByUsername_WithNullUsername_ShouldThrowException() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null));

        assertEquals("User not found: null", exception.getMessage());
        verify(userRepository).findByUsername(null);
    }

    @Test
    void loadUserByUsername_WithEmptyUsername_ShouldThrowException() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(""));

        assertEquals("User not found: ", exception.getMessage());
        verify(userRepository).findByUsername("");
    }

    @Test
    void loadUserByUsername_WithUserHavingEmptyPassword_ShouldReturnUserDetails() {
        String username = "userWithEmptyPassword";
        String password = "";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void loadUserByUsername_ShouldAlwaysAssignUserRole() {
        String username = "testuser";
        String password = "password123";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertEquals(1, userDetails.getAuthorities().size());
        assertEquals("ROLE_USER", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_WithSpecialCharactersInUsername_ShouldWork() {
        String username = "user.name-123_456";
        String password = "password";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
    }

    @Test
    void loadUserByUsername_WithUserDisabled_ShouldReturnEnabledUserDetails() {
        String username = "disabledUser";
        String password = "password";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .enabled(false)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertNotNull(userDetails);
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_WithRepositoryException_ShouldPropagateException() {
        String username = "testuser";

        when(userRepository.findByUsername(username)).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userDetailsService.loadUserByUsername(username));

        assertEquals("Database error", exception.getMessage());
    }

    @Test
    void loadUserByUsername_ShouldMapUserPropertiesCorrectly() {
        String username = "john.doe";
        String password = "$2a$10$encodedHash";
        String email = "john@example.com";

        ChatUser chatUser = ChatUser.builder()
                .id(1L)
                .username(username)
                .password(password)
                .email(email)
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertEquals(username, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    void loadUserByUsername_WithCaseSensitiveUsername_ShouldRespectCase() {
        String username = "TestUser";
        String password = "password";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails1 = userDetailsService.loadUserByUsername(username);
        assertNotNull(userDetails1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("testuser"));

        assertEquals("User not found: testuser", exception.getMessage());
    }

    @Test
    void loadUserByUsername_WithOAuthUserHavingNullPassword_ShouldHandleGracefully() {
        String username = "oauthuser";
        String password = "";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .providerId("vkontakte")
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
    }

    @Test
    void loadUserByUsername_ShouldCreateUserWithDefaultSecurityProperties() {
        String username = "defaultuser";
        String password = "pass";

        ChatUser chatUser = ChatUser.builder()
                .username(username)
                .password(password)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(chatUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertEquals(1, userDetails.getAuthorities().size());
    }
}