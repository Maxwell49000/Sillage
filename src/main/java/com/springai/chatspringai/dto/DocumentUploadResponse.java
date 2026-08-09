package com.springai.chatspringai.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO représentant la réponse après upload d'un document.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {
    
    private String conversationId;
    private String documentId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private int numberOfChunks;
    private LocalDateTime uploadedAt;
    private String message;
    private boolean success;
}
