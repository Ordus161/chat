package com.test.testtaskwebchat.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.*;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // Очищаем стандартные конвертеры
        messageConverters.clear();

        // 1. StringMessageConverter для текстовых сообщений (должен быть ПЕРВЫМ!)
        StringMessageConverter stringConverter = new StringMessageConverter(StandardCharsets.UTF_8);
        messageConverters.add(stringConverter);

        // 2. JSON конвертер для объектов (MessageDto, List<UserDto> и т.д.)
        MappingJackson2MessageConverter jsonConverter = new MappingJackson2MessageConverter();

        // Настраиваем ObjectMapper для JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.registerModule(new JavaTimeModule());

        jsonConverter.setObjectMapper(objectMapper);
        messageConverters.add(jsonConverter);

        messageConverters.add(new ByteArrayMessageConverter());

        return false;
    }
}