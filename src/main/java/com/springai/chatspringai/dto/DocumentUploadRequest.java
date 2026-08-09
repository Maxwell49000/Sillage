package com.springai.chatspringai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO représentant une requête d'upload de document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadRequest {
    
    /**
     * L'ID de la conversation pour laquelle le document est uploadé.
     */
    private String conversationId;
    
    /**
     * Le nom du fichier uploadé.
     */
    private String fileName;
    
    /**
     * Le type MIME du fichier (application/pdf, application/vnd.openxmlformats-officedocument.wordprocessingml.document, etc.)
     */
    private String fileType;
    
    /**
     * Le contenu du fichier en base64 ou raw (dépendra de l'implémentation frontend).
     * Pour simplifier, on utilisera multipart/form-data dans le controller.
     */
    private byte[] fileContent;
}
