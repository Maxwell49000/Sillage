package com.springai.chatspringai.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.springai.chatspringai.dto.ConversationDTO;
import com.springai.chatspringai.dto.MessageDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Repository minimal utilisant JdbcTemplate pour persister les messages de
 * mémoire de chat. Table attendue: spring_ai_chat_memory(conversation_id, message, created_at).
 */
@Repository
@Slf4j
public class ChatMemoryIDRepository {
    private final JdbcTemplate jdbcTemplate;

    public ChatMemoryIDRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Adapté à votre schéma : colonnes 'content', 'timestamp', 'type'
    // Utilise SQL `now()` pour remplir la colonne NOT NULL `timestamp` côté base.
    public void saveMessage(String conversationId, String content, String type) {
        String sql = "INSERT INTO spring_ai_chat_memory (conversation_id, content, type, timestamp) VALUES (?, ?, ?, now())";
        int rows = jdbcTemplate.update(sql, conversationId, content, type);
        log.debug("Inserted {} row(s) into spring_ai_chat_memory for conversation {}", rows, conversationId);
    }

    public List<String> getMessages(String conversationId) {
        String sql = "SELECT type, content FROM spring_ai_chat_memory WHERE conversation_id = ? ORDER BY timestamp ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String t = rs.getString("type");
            String c = rs.getString("content");
            return (t == null ? "": t + ": ") + (c == null ? "" : c);
        }, conversationId);
    }

    /**
     * Récupère les messages détaillés d'une conversation avec structure complète.
     */
    public List<MessageDTO> getMessagesDetailed(String conversationId) {
        String sql = "SELECT conversation_id, type, content, timestamp FROM spring_ai_chat_memory WHERE conversation_id = ? ORDER BY timestamp ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> 
            MessageDTO.builder()
                .id(rowNum)  
                .conversationId(rs.getString("conversation_id"))
                .role(rs.getString("type"))
                .content(rs.getString("content"))
                .timestamp(rs.getString("timestamp"))
                .build()
        , conversationId);
    }

    /**
     * Récupère la liste des conversations uniques (identifiants), triées par date la plus récente.
     */
    public List<String> getAllConversationIds() {
        String sql = "SELECT DISTINCT conversation_id FROM spring_ai_chat_memory GROUP BY conversation_id ORDER BY MAX(timestamp) DESC";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Supprime tous les messages d'une conversation.
     */
    public void deleteConversation(String conversationId) {
        String sql = "DELETE FROM spring_ai_chat_memory WHERE conversation_id = ?";
        int rows = jdbcTemplate.update(sql, conversationId);
        log.info("Deleted {} messages for conversation {}", rows, conversationId);
    }

    /**
     * Récupère les conversations avec métadonnées détaillées.
     */
    public List<ConversationDTO> getAllConversationsDetailed() {
        String sql = """
            SELECT 
                m.conversation_id,
                MIN(m.timestamp) as createdAt,
                MAX(m.timestamp) as lastMessageAt,
                COUNT(*) as messageCount,
                SUBSTRING((SELECT content FROM spring_ai_chat_memory m2 
                    WHERE m2.conversation_id = m.conversation_id 
                    ORDER BY m2.timestamp DESC LIMIT 1), 1, 100) as lastMessage
            FROM spring_ai_chat_memory m
            GROUP BY m.conversation_id 
            ORDER BY MAX(m.timestamp) DESC
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> 
            ConversationDTO.builder()
                .conversationId(rs.getString("conversation_id"))
                .createdAt(rs.getString("createdAt"))
                .lastMessageAt(rs.getString("lastMessageAt"))
                .messageCount(rs.getInt("messageCount"))
                .lastMessage(rs.getString("lastMessage"))
                .build()
        );
    }
    
}
