package com.springai.chatspringai.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un document uploadé associé à une conversation.
 * Stocke les métadonnées du fichier et son contenu textuel.
 */
@Entity
@Table(name = "document_uploads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUpload {
    
    @Id
    private String id;
    
    @Column(name = "conversation_id", nullable = true)
    private String conversationId;
    
    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "number_of_chunks", nullable = false)
    private int numberOfChunks;
    
    @Column(name = "uploaded_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime uploadedAt;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
    
    /**
     * Appelé avant la création de l'entité en BD
     */
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        uploadedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Appelé avant la mise à jour de l'entité en BD
     */
    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
