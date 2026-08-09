package com.springai.chatspringai.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.springai.chatspringai.entity.DocumentUpload;

/**
 * Repository pour accéder aux documents uploadés en BD.
 * Permet la recherche et la gestion des documents associés aux conversations.
 */
@Repository
public interface DocumentUploadRepository extends JpaRepository<DocumentUpload, String> {
    
    /**
     * Trouve tous les documents d'une conversation.
     */
    List<DocumentUpload> findByConversationId(String conversationId);
    
    /**
     * Compte les documents d'une conversation.
     */
    long countByConversationId(String conversationId);
    
    /**
     * Supprime tous les documents d'une conversation.
     */
    void deleteByConversationId(String conversationId);
}
