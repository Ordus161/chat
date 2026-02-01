package com.test.testtaskwebchat.security;

import com.test.testtaskwebchat.model.ChatUser;
import com.test.testtaskwebchat.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

//мок обработка для успешной авторизации через соц сети
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final ChatUserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = extractEmail(registrationId, attributes);

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<ChatUser> userOptional = userRepository.findByEmail(email);

        ChatUser user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            updateUserDetails(user, registrationId, attributes);
        } else {
            user = createNewUser(email, registrationId, attributes);
        }

        userRepository.save(user);

        return oauth2User;
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        switch (registrationId) {
            case "vkontakte":
                return (String) attributes.get("email");
            case "yandex":
                return (String) attributes.get("default_email");
            default:
                return null;
        }
    }

    private ChatUser createNewUser(String email, String provider, Map<String, Object> attributes) {
        ChatUser user = new ChatUser();
        user.setEmail(email);
        user.setProviderId(provider);
        user.setEnabled(true);

        if ("vkontakte".equals(provider)) {
            user.setFirstName((String) attributes.get("first_name"));
            user.setLastName((String) attributes.get("last_name"));
            user.setAvatarUrl((String) attributes.get("photo_200"));
        }

        if ("yandex".equals(provider)) {
            user.setFirstName((String) attributes.get("first_name"));
            user.setLastName((String) attributes.get("last_name"));
        }

        return user;
    }

    private void updateUserDetails(ChatUser user, String provider, Map<String, Object> attributes) {
        if ("vkontakte".equals(provider)) {
            user.setFirstName((String) attributes.get("first_name"));
            user.setLastName((String) attributes.get("last_name"));
            user.setAvatarUrl((String) attributes.get("photo_200"));
        }
    }
}