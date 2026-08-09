package com.springai.chatspringai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse du chat.
 *
 * Contient l'identifiant de la conversation et la réponse du modèle IA.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponseDTO {
    
    /** Identifiant unique de la conversation */
    private String conversationId;
    
    /** Réponse générée par le modèle IA */
    private String reply;
}
