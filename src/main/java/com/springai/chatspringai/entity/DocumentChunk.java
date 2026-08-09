package com.springai.chatspringai.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un chunk (morceau) d'un document uploadé.
 * Les documents sont découpés en chunks pour être vectorisés séparément.
 * Chaque chunk est stocké avec son embedding dans le vector store.
 */
@Entity
@Table(
    name = "document_chunks",
    uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "chunk_index"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {
    
    @Id
    private String id;
    
    @Column(name = "document_id", nullable = false)
    private String documentId;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    /**
     * Appelé avant la création de l'entité en BD
     */
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
