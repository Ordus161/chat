package com.test.testtaskwebchat.repository;

import com.test.testtaskwebchat.dto.MessageDto;
import com.test.testtaskwebchat.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT new com.test.testtaskwebchat.dto.MessageDto(m.id, m.content, m.user.username, m.createdAt) " +
            "FROM ChatMessage m ORDER BY m.createdAt DESC")
    List<MessageDto> findAllMessagesOrderedByDateDesc();

}
