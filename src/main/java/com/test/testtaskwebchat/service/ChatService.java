package com.test.testtaskwebchat.service;

import com.test.testtaskwebchat.dto.MessageDto;
import com.test.testtaskwebchat.model.ChatMessage;
import com.test.testtaskwebchat.model.ChatUser;
import com.test.testtaskwebchat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private static final int LAST_MESSAGES_LIMIT = 50;

    private final ChatMessageRepository messageRepository;
    private final UserService userService;

    public List<MessageDto> getLastMessages() {

        List<MessageDto> allMessages = messageRepository.findAllMessagesOrderedByDateDesc();

        return allMessages.stream()
                .limit(LAST_MESSAGES_LIMIT)
                .collect(Collectors.toList());
    }

    public MessageDto saveMessage(String content, String username) {
        ChatUser user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ChatMessage message = ChatMessage.builder()
                .content(content)
                .user(user)
                .build();

        message = messageRepository.save(message);

        return new MessageDto(
                message.getId(),
                message.getContent(),
                message.getUser().getUsername(),
                message.getCreatedAt()
        );
    }

    public MessageDto saveMessage(MessageDto messageDto, String username) {
        return saveMessage(messageDto.getContent(), username);
    }

}