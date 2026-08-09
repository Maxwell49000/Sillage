package com.springai.chatspringai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour représenter une conversation avec métadonnées enrichies.
 *
 * Utilisé pour lister les conversations et afficher des informations
 * comme la date de création, le dernier message, le nombre de messages, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDTO {
    
    /** Identifiant unique de la conversation */
    private String conversationId;
    
    /** Date/heure de création de la conversation */
    private String createdAt;
    
    /** Date/heure du dernier message de la conversation */
    private String lastMessageAt;
    
    /** Nombre total de messages dans la conversation */
    private Integer messageCount;
    
    /** Aperçu du dernier message (truncné pour l'affichage) */
    private String lastMessage;
}
