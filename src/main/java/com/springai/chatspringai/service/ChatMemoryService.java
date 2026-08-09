package com.springai.chatspringai.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.springai.chatspringai.dto.ConversationDTO;
import com.springai.chatspringai.dto.MessageDTO;
import com.springai.chatspringai.repository.ChatMemoryIDRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service de plus haut niveau pour gérer la persistance des messages de
 * conversation. Utilise `ChatMemoryIDRepository` pour les opérations JDBC.
 */
@Service
@Slf4j
public class ChatMemoryService {

    private final ChatMemoryIDRepository repository;

    public ChatMemoryService(ChatMemoryIDRepository repository) {
        this.repository = repository;
    }

    public void saveUserMessage(String conversationId, String message) {
        try {
                // stocke le message en séparant le role (type) et le contenu
                // Contrainte CHECK en base accepte : USER, ASSISTANT, SYSTEM, TOOL (majuscules)
                repository.saveMessage(conversationId, message, "USER");
            log.debug("Saved user message for conversation {}", conversationId);
        } catch (Exception ex) {
            log.warn("Failed to save user message for {}: {}", conversationId, ex.getMessage(), ex);
        }
    }

    public void saveAssistantMessage(String conversationId, String message) {
        try {
                repository.saveMessage(conversationId, message, "ASSISTANT");
            log.debug("Saved assistant message for conversation {}", conversationId);
        } catch (Exception ex) {
            log.warn("Failed to save assistant message for {}: {}", conversationId, ex.getMessage(), ex);
        }
    }

    public List<String> getConversationMessages(String conversationId) {
        try {
            return repository.getMessages(conversationId);
        } catch (Exception ex) {
            log.warn("Failed to read messages for {}: {}", conversationId, ex.getMessage(), ex);
            return List.of();
        }
    }

    /**
     * Récupère les messages détaillés d'une conversation avec structure complète.
     * Nettoie les messages pour retirer tout préfixe de rôle indésirable.
     */
    public List<MessageDTO> getConversationMessagesDetailed(String conversationId) {
        try {
            List<MessageDTO> messages = repository.getMessagesDetailed(conversationId);
            // Nettoie les messages pour retirer les préfixes de rôle (U:, A:, etc.)
            for (MessageDTO msg : messages) {
                msg.setContent(sanitizeMessageContent(msg.getContent()));
            }
            return messages;
        } catch (Exception ex) {
            log.warn("Failed to read detailed messages for {}: {}", conversationId, ex.getMessage(), ex);
            return List.of();
        }
    }

    /**
     * Nettoie le contenu d'un message pour retirer les préfixes de rôle que le modèle
     * aurait pu ajouter (U:, A:, USER:, ASSISTANT:, etc.)
     */
    private String sanitizeMessageContent(String content) {
        if (content == null) return null;
        // Retire les patterns comme "U: ", "A: ", "USER: ", "ASSISTANT: ", etc.
        String cleaned = content.replaceAll("^\\s*[UA]\\s*:\\s*", "")  // U: ou A: au début
                                .replaceAll("^\\s*(USER|ASSISTANT|AI|SYSTEM)\\s*[:\\-]\\s*", ""); // Versions longues
        return cleaned.trim();
    }

    /**
     * Récupère la liste de toutes les conversations.
     */
    public List<String> getAllConversations() {
        try {
            return repository.getAllConversationIds();
        } catch (Exception ex) {
            log.warn("Failed to read conversations list: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    /**
     * Récupère les conversations avec métadonnées détaillées.
     */
    public List<ConversationDTO> getAllConversationsDetailed() {
        try {
            return repository.getAllConversationsDetailed();
        } catch (Exception ex) {
            log.warn("Failed to read detailed conversations: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    /**
     * Supprime une conversation et tous ses messages.
     */
    public void deleteConversation(String conversationId) {
        try {
            repository.deleteConversation(conversationId);
            log.info("Deleted conversation {}", conversationId);
        } catch (Exception ex) {
            log.warn("Failed to delete conversation {}: {}", conversationId, ex.getMessage(), ex);
        }
    }
}
