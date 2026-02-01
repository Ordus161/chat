package com.test.testtaskwebchat.security;

import com.test.testtaskwebchat.model.ChatUser;
import com.test.testtaskwebchat.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final ChatUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Для мока просто возвращаем заглушку
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String socialId;
        String username;

        if ("vk".equals(registrationId)) {
            socialId = "vk_mock_" + UUID.randomUUID().toString().substring(0, 8);
            username = "vk_user_" + System.currentTimeMillis() % 10000;
        } else if ("yandex".equals(registrationId)) {
            socialId = "yandex_mock_" + UUID.randomUUID().toString().substring(0, 8);
            username = "ya_user_" + System.currentTimeMillis() % 10000;
        } else {
            socialId = "social_mock_" + UUID.randomUUID().toString().substring(0, 8);
            username = "social_user_" + System.currentTimeMillis() % 10000;
        }

        ChatUser user = userRepository.findBySocialProviderAndSocialId(registrationId, socialId)
                .orElseGet(() -> {
                    ChatUser newUser = ChatUser.builder()
                            .username(username)
                            .password(passwordEncoder.encode("social_password"))
                            .socialProvider(registrationId)
                            .socialId(socialId)
                            .build();
                    return userRepository.save(newUser);
                });

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "name", user.getUsername(),
                        "sub", socialId,
                        "email", username + "@mock-social.com"
                ),
                "name"
        );
    }
}