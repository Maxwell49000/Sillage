package com.springai.chatspringai.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.springai.chatspringai.config.VectorStoreConfig.SimplePostgresVectorStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour l'augmentation RAG des requêtes chat.
 * Récupère les documents pertinents du VectorStore et les ajoute au contexte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {
    
    private final SimplePostgresVectorStore vectorStore;
    
    @Value("${app.rag.top-k:5}")
    private int topK;
    
    @Value("${app.rag.similarity-threshold:0.75}")
    private double similarityThreshold;
    
    /**
     * Recherche les documents les plus pertinents pour une requête donnée.
     * Utile pour augmenter les prompts du chat avec du contexte pertinent.
     * 
     * @param query La requête utilisateur
     * @return Liste des documents pertinents
     */
    public List<Document> searchRelevantDocuments(String query) {
        log.info("🔍 Recherche de documents pour : {}", query);
        
        try {
            return vectorStore.similaritySearch(query, topK, null, similarityThreshold);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche vectorielle", e);
            return List.of();
        }
    }
    
    /**
     * Filtre les documents d'une conversation spécifique.
     * Utile pour limiter le RAG à une conversation donnée.
     * 
     * @param conversationId ID de la conversation
     * @param query La requête
     * @return Documents pertinents pour cette conversation
     */
    public List<Document> searchByConversation(String conversationId, String query) {
        log.info("🔍 Recherche de documents pour la conversation {} : {}", conversationId, query);
        
        try {
            return vectorStore.similaritySearch(query, topK, conversationId, similarityThreshold);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la recherche vectorielle par conversation", e);
            return List.of();
        }
    }
    
    /**
     * Formate les documents trouvés en contexte pour le prompt.
     * Utile pour augmenter le prompt avec les documents pertinents.
     * 
     * @param documents Les documents trouvés
     * @return Une chaîne formatée avec le contexte
     */
    public String formatDocumentsAsContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("\n📚 Documents pertinents pour ta réponse:\n");
        
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String text = doc.getText();
            
            // Nettoie le texte du chunk
            if (text != null) {
                text = text.trim()
                    .replaceAll("(?i)\\buser\\b\\s*[:\\-]\\s*", "")
                    .replaceAll("(?i)\\bassistant\\b\\s*[:\\-]\\s*", "")
                    .replaceAll("(?i)\\bai\\b\\s*[:\\-]\\s*", "");
            }
            
            context.append("\n• ").append(text);
        }
        
        context.append("\n\nUtilise ces informations pour construire ta réponse.");
        return context.toString();
    }
}
