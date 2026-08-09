package com.springai.chatspringai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour envoyer un message de chat avec conversationId optionnel.
 *
 * Permet au client d'envoyer un message soit dans une conversation existante
 * (via conversationId) soit dans une nouvelle conversation (sans conversationId).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequestDTO {
    
    /** Identifiant optionnel de la conversation existante */
    private String conversationId;
    
    /** Contenu du message (obligatoire, non vide, 1-1000 caractères) */
    @NotBlank(message = "Message cannot be empty")
    @Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
    private String message;
    
    /** Instruction système optionnelle pour guider la réponse du modèle */
    private String systemInstruction;
}
