package com.test.testtaskwebchat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MockOAuth2Config {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        // Мок для VK
        ClientRegistration vkRegistration = ClientRegistration
                .withRegistrationId("vk")
                .clientId("mock_vk_client_id")
                .clientSecret("mock_vk_client_secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("email")
                .authorizationUri("https://oauth.vk.com/authorize")
                .tokenUri("https://oauth.vk.com/access_token")
                .userInfoUri("https://api.vk.com/method/users.get?fields=photo_200,screen_name,email")
                .userNameAttributeName("response")
                .clientName("VK")
                .build();

        // Мок для Yandex
        ClientRegistration yandexRegistration = ClientRegistration
                .withRegistrationId("yandex")
                .clientId("mock_yandex_client_id")
                .clientSecret("mock_yandex_client_secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("login:info", "login:email")
                .authorizationUri("https://oauth.yandex.ru/authorize")
                .tokenUri("https://oauth.yandex.ru/token")
                .userInfoUri("https://login.yandex.ru/info")
                .userNameAttributeName("id")
                .clientName("Yandex")
                .build();

        registrations.add(vkRegistration);
        registrations.add(yandexRegistration);

        return new InMemoryClientRegistrationRepository(registrations);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }
}
