package com.springai.chatspringai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour représenter un message de conversation avec tous les détails.
 *
 * Encapsule l'historique complet d'une conversation avec les métadonnées
 * de chaque message (id, rôle, contenu, timestamp).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    
    /** Identifiant unique du message */
    private Integer id;
    
    /** Identifiant de la conversation à laquelle appartient le message */
    private String conversationId;
    
    /** Rôle du message: USER, ASSISTANT, SYSTEM, ou TOOL */
    private String role;
    
    /** Contenu textuel du message */
    private String content;
    
    /** Timestamp du message (date/heure de création) */
    private String timestamp;
}
