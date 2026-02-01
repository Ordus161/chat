package com.test.testtaskwebchat.repository;

import com.test.testtaskwebchat.model.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {
    Optional<ChatUser> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("SELECT u.username FROM ChatUser u")
    List<String> findAllUsernames();
    Optional<ChatUser> findByEmail(String email);
}
