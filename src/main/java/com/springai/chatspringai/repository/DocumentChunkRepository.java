package com.springai.chatspringai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.springai.chatspringai.entity.DocumentChunk;

/**
 * Repository pour accéder aux chunks de documents en BD.
 * Permet la recherche et la gestion des chunks.
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {
    
    /**
     * Trouve tous les chunks d'un document spécifique.
     */
    List<DocumentChunk> findByDocumentId(String documentId);
    
    /**
     * Supprime tous les chunks d'un document.
     */
    void deleteByDocumentId(String documentId);
    
    /**
     * Compte le nombre de chunks pour un document.
     */
    long countByDocumentId(String documentId);
}
