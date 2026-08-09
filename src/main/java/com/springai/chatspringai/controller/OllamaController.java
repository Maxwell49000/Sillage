package com.springai.chatspringai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springai.chatspringai.dto.ChatRequestDTO;
import com.springai.chatspringai.dto.ChatResponseDTO;
import com.springai.chatspringai.dto.ConversationDTO;
import com.springai.chatspringai.dto.MessageDTO;
import com.springai.chatspringai.service.ChatMemoryService;
import com.springai.chatspringai.service.ChatService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class OllamaController {
    
    private final ChatService chatService;
    private final ChatMemoryService chatMemoryService;
    
    public OllamaController(ChatService chatService, ChatMemoryService chatMemoryService) {
        this.chatService = chatService;
        this.chatMemoryService = chatMemoryService;
    }
    
    /**
     * Envoie un message dans une conversation (existante ou nouvelle).
     * Si conversationId est fourni, charge cette conversation.
     * Si conversationId est null/vide, utilise une conversation nouvelle.
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponseDTO> sendMessage(@Valid @RequestBody ChatRequestDTO request) {
        long start = System.currentTimeMillis();
        log.info("POST /api/chat/send received (conversationId={}, message-length={})", request.getConversationId(), request.getMessage() == null ? 0 : request.getMessage().length());
        try {
            // Si un conversationId est fourni, on charge cette conversation
            if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
                this.chatService.setConversationId(request.getConversationId());
            }

            String reply;
            if (request.getSystemInstruction() != null && !request.getSystemInstruction().isEmpty()) {
                reply = this.chatService.systemUser(request.getSystemInstruction(), request.getMessage());
            } else {
                reply = this.chatService.chat(request.getMessage());
            }

            long duration = System.currentTimeMillis() - start;
            log.info("POST /api/chat/send responded in {} ms (reply-length={})", duration, reply == null ? 0 : reply.length());

            ChatResponseDTO response = ChatResponseDTO.builder()
                .conversationId(this.chatService.getConversationId())
                .reply(reply)
                .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Erreur envoi message after {} ms", duration, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
   
    /**
     * Récupère l'historique d'une conversation donnée.
     */
    @GetMapping("/history/{conversationId}")
    public ResponseEntity<List<MessageDTO>> getConversationHistory(@PathVariable String conversationId) {
        try {
            List<MessageDTO> messages = this.chatMemoryService.getConversationMessagesDetailed(conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Erreur récupération history pour {}", conversationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crée une nouvelle conversation et retourne son ID.
     * Chaque appel à cet endpoint crée une nouvelle conversation.
     */
    @PostMapping("/conversations/new")
    public ResponseEntity<ChatResponseDTO> createNewConversation() {
        try {
            // Le ChatService est RequestScope, donc chaque requête crée une nouvelle instance
            // avec son propre conversationId
            String conversationId = this.chatService.getConversationId();
            ChatResponseDTO response = ChatResponseDTO.builder()
                .conversationId(conversationId)
                .reply("Nouvelle conversation créée")
                .build();
            log.info("Created new conversation with ID: {}", conversationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur création conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Liste toutes les conversations avec détails enrichis.
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> listConversations() {
        try {
            List<ConversationDTO> conversations = this.chatMemoryService.getAllConversationsDetailed();
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Erreur récupération liste des conversations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprime une conversation.
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, String>> deleteConversation(@PathVariable String conversationId) {
        try {
            this.chatMemoryService.deleteConversation(conversationId);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Conversation supprimée : " + conversationId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Erreur suppression conversation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}    




