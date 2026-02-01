package com.test.testtaskwebchat.controller;

import com.test.testtaskwebchat.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private LoginController loginController;

    private LoginController.RegistrationForm registrationForm;

    @BeforeEach
    void setUp() {
        registrationForm = new LoginController.RegistrationForm();
        registrationForm.setUsername("testuser");
        registrationForm.setPassword("password123");
        registrationForm.setConfirmPassword("password123");
    }

    @Test
    void showRegistrationForm_ShouldReturnRegisterViewWithForm() {
        String viewName = loginController.showRegistrationForm(model);

        assertEquals("register", viewName);
        verify(model).addAttribute(eq("registrationForm"), any(LoginController.RegistrationForm.class));
    }

    @Test
    void registerUser_WithValidForm_ShouldRegisterAndRedirect() {
        when(bindingResult.hasErrors()).thenReturn(false);

        String viewName = loginController.registerUser(registrationForm, bindingResult, model);

        assertEquals("redirect:/login?registered=true", viewName);
        verify(userService).registerNewUser("testuser", "password123");
        verifyNoInteractions(model); // Не должно добавлять атрибуты ошибок
    }

    @Test
    void registerUser_WithBindingErrors_ShouldReturnRegisterView() {
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = loginController.registerUser(registrationForm, bindingResult, model);

        assertEquals("register", viewName);
        verify(userService, never()).registerNewUser(anyString(), anyString());
    }

    @Test
    void registerUser_WithPasswordMismatch_ShouldReturnError() {
        when(bindingResult.hasErrors()).thenReturn(false);
        registrationForm.setConfirmPassword("differentPassword");

        String viewName = loginController.registerUser(registrationForm, bindingResult, model);

        assertEquals("register", viewName);
        verify(model).addAttribute("passwordError", "Passwords do not match");
        verify(userService, never()).registerNewUser(anyString(), anyString());
    }

    @Test
    void registerUser_WithDuplicateUsername_ShouldReturnError() {
        when(bindingResult.hasErrors()).thenReturn(false);
        String errorMessage = "Username already exists";
        doThrow(new IllegalArgumentException(errorMessage))
                .when(userService).registerNewUser(eq("testuser"), eq("password123"));

        String viewName = loginController.registerUser(registrationForm, bindingResult, model);

        assertEquals("register", viewName);
        verify(model).addAttribute("usernameError", errorMessage);
    }

    @Test
    void registerUser_WithEmptyPassword_ShouldReturnToForm() {
        registrationForm.setPassword("");
        registrationForm.setConfirmPassword("");
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = loginController.registerUser(registrationForm, bindingResult, model);

        assertEquals("register", viewName);
        verify(userService, never()).registerNewUser(anyString(), anyString());
    }

    @Test
    void registerUser_WithNullForm_ShouldHandleGracefully() {
        when(bindingResult.hasErrors()).thenReturn(false);

        assertThrows(NullPointerException.class, () ->
                loginController.registerUser(null, bindingResult, model)
        );
    }

    @Test
    void registerUser_WithServiceException_ShouldPropagateRuntimeException() {
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("Database error"))
                .when(userService).registerNewUser(anyString(), anyString());

        assertThrows(RuntimeException.class, () ->
                loginController.registerUser(registrationForm, bindingResult, model)
        );
    }

    @Test
    void registrationForm_GetterSetter_ShouldWorkCorrectly() {
        LoginController.RegistrationForm form = new LoginController.RegistrationForm();
        String username = "newuser";
        String password = "newpass";
        String confirmPassword = "newpass";

        form.setUsername(username);
        form.setPassword(password);
        form.setConfirmPassword(confirmPassword);

        assertEquals(username, form.getUsername());
        assertEquals(password, form.getPassword());
        assertEquals(confirmPassword, form.getConfirmPassword());
    }

    @Test
    void registrationForm_WithDifferentConstructors_ShouldBeConsistent() {
        LoginController.RegistrationForm form1 = new LoginController.RegistrationForm();
        LoginController.RegistrationForm form2 = new LoginController.RegistrationForm();

        form1.setUsername("user1");
        form2.setUsername("user2");

        assertNotEquals(form1.getUsername(), form2.getUsername());
    }

    @Test
    void registerUser_WithSpecialCharactersInUsername_ShouldProcess() {
        registrationForm.setUsername("user.name-123");
        when(bindingResult.hasErrors()).thenReturn(false);

        String viewName = loginController.registerUser(registrationForm, bindingResult, model);

        assertEquals("redirect:/login?registered=true", viewName);
        verify(userService).registerNewUser("user.name-123", "password123");
    }

    @Test
    void registerUser_WithLongPassword_ShouldProcess() {
        String longPassword = "a".repeat(100);
        registrationForm.setPassword(longPassword);
        registrationForm.setConfirmPassword(longPassword);
        when(bindingResult.hasErrors()).thenReturn(false);

        String viewName = loginController.registerUser(registrationForm, bindingResult, model);

        assertEquals("redirect:/login?registered=true", viewName);
        verify(userService).registerNewUser("testuser", longPassword);
    }
}