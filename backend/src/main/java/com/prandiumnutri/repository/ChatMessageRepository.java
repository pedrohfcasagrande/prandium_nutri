package com.prandiumnutri.repository;

import com.prandiumnutri.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByLinkIdOrderByCreatedAtAsc(Long linkId);

    java.util.Optional<ChatMessage> findFirstByLinkIdOrderByCreatedAtDesc(Long linkId);

    @Query("select count(m) from ChatMessage m where m.link.id = :linkId " +
           "and m.sender.id <> :readerId and m.readAt is null")
    long countUnreadForReader(@Param("linkId") Long linkId, @Param("readerId") Long readerId);

    @Query("select count(m) from ChatMessage m where m.sender.id <> :readerId " +
           "and m.readAt is null " +
           "and (m.link.nutritionist.id = :readerId or m.link.client.id = :readerId)")
    long countUnreadTotalForUser(@Param("readerId") Long readerId);

    @Modifying
    @Query("update ChatMessage m set m.readAt = :now " +
           "where m.link.id = :linkId and m.sender.id <> :readerId and m.readAt is null")
    int markReadForReader(@Param("linkId") Long linkId,
                          @Param("readerId") Long readerId,
                          @Param("now") LocalDateTime now);
}
