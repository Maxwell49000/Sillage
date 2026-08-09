package com.springai.chatspringai.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springai.chatspringai.config.VectorStoreConfig.SimplePostgresVectorStore;
import com.springai.chatspringai.entity.DocumentChunk;
import com.springai.chatspringai.entity.DocumentUpload;
import com.springai.chatspringai.repository.DocumentChunkRepository;
import com.springai.chatspringai.repository.DocumentUploadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service pour ingérer les documents : parsing → chunking → embedding → stockage.
 * C'est le pipeline ETL du RAG.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIngestionService {
    
    private final DocumentProcessingService documentProcessingService;
    private final DocumentUploadRepository documentUploadRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final SimplePostgresVectorStore vectorStore;
    
    @Value("${app.rag.chunk-size:700}")
    private int chunkSize;
    
    @Value("${app.rag.min-chunk-size-chars:300}")
    private int minChunkSizeChars;
    
    /**
     * Pipeline entier : parse le fichier → découpe en chunks → vectorise → persiste.
     * 
     * @param conversationId ID de la conversation
     * @param fileName Nom du fichier
     * @param fileType Type MIME
     * @param fileContent Contenu binaire
     * @return Le DocumentUpload créé avec les métadonnées
     */
    @Transactional
    public DocumentUpload ingestDocument(String conversationId, String fileName, String fileType, byte[] fileContent) {
        log.info("🚀 Ingestion du document : {} pour la conversation {}", fileName, conversationId);
        
        try {
            // 1. PARSE le fichier
            String extractedText = documentProcessingService.processPdfFile(fileContent, fileName, fileType);
            log.debug("📝 Texte extrait : {} caractères", extractedText.length());
            
            // 1.5 NETTOIE le texte extrait pour éviter les artefacts
            extractedText = cleanExtractedText(extractedText);
            log.debug("📝 Texte nettoyé : {} caractères", extractedText.length());
            
            // 2. Créez l'entité DocumentUpload (numberOfChunks sera mis à jour après chunking)
            String documentId = UUID.randomUUID().toString();
            DocumentUpload docUpload = DocumentUpload.builder()
                .id(documentId)
                .conversationId(conversationId)
                .fileName(fileName)
                .fileSize((long) fileContent.length)
                .fileType(fileType)
                .content(extractedText)
                .numberOfChunks(0)
                .build();
            
            // 3. CHUNK le texte avec TokenTextSplitter
            TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,           // defaultChunkSize
                minChunkSizeChars,   // minChunkSizeChars
                5,                    // minChunkLengthToEmbed
                10000,                // maxNumChunks
                true                  // keepSeparator
            );
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", documentId);
            if (conversationId != null) {
                metadata.put("conversation_id", conversationId);
            }
            metadata.put("file_name", fileName);
            
            Document doc = new Document(extractedText, metadata);
            List<Document> chunks = splitter.apply(List.of(doc));
            log.info("✂️  Document découpé en {} chunks", chunks.size());
            
            // 4. Crée les entités DocumentChunk et les persiste
            List<DocumentChunk> persistedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                
                // Créez l'entité DocumentChunk
                DocumentChunk docChunk = DocumentChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .documentId(documentId)
                    .chunkIndex(i)
                    .content(chunk.getText())
                    .chunkSize(chunk.getText().length())
                    .build();
                
                persistedChunks.add(docChunk);
                
                // Ajouter les métadonnées pour le VectorStore
                chunk.getMetadata().put("chunk_index", String.valueOf(i));
            }
            
            // 5. Mettez à jour le nombre de chunks et persistez le DocumentUpload EN PREMIER
            docUpload.setNumberOfChunks(chunks.size());
            DocumentUpload savedDoc = documentUploadRepository.save(docUpload);
            log.info("💾 DocumentUpload {} persisté en BD", documentId);
            
            // 6. Puis persistez les chunks (le parent existe maintenant)
            documentChunkRepository.saveAll(persistedChunks);
            log.info("💾 {} chunks persistés en BD", persistedChunks.size());
            
            // 7. Ajoutez les chunks au VectorStore (pgvector)
            vectorStore.add(chunks);
            log.info("🔍 Chunks ajoutés au VectorStore pour recherche sémantique");
            
            log.info("✅ Ingestion complète : document {} avec {} chunks vectorisés", documentId, chunks.size());
            
            return savedDoc;
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'ingestion du document {}", fileName, e);
            throw new RuntimeException("Erreur lors de l'ingestion : " + e.getMessage(), e);
        }
    }
    
    /**
     * Supprime un document et tous ses chunks du VectorStore et de la BD.
     */
    @Transactional
    public void deleteDocument(String documentId) {
        log.info("🗑️  Suppression du document {}", documentId);
        
        try {
            // Supprime d'abord l'index sémantique, puis les données relationnelles.
            vectorStore.deleteByDocumentId(documentId);
            documentChunkRepository.deleteByDocumentId(documentId);
            DocumentUpload doc = documentUploadRepository.findById(documentId).orElse(null);
            
            if (doc != null) {
                documentUploadRepository.delete(doc);
                log.info("✅ Document {} supprimé", documentId);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression du document {}", documentId, e);
            throw new RuntimeException("Erreur lors de la suppression : " + e.getMessage(), e);
        }
    }
    
    /**
     * Nettoie le texte extrait pour éviter les artefacts et labels de rôle.
     * Supprime les patterns bizarres que les extracteurs PDF peuvent ajouter.
     */
    private String cleanExtractedText(String text) {
        if (text == null) return "";
        
        String cleaned = text;
        
        // Supprime les labels de rôle (User:, Assistant:, AI:, etc.)
        cleaned = cleaned.replaceAll("(?i)^\\s*[UA]\\s*(?:\\bASSISTANT\\b)?\\s*[:\\-]\\s*", "");
        cleaned = cleaned.replaceAll("(?i)\\n\\s*[UA]\\s*(?:\\bASSISTANT\\b)?\\s*[:\\-]\\s*", "\n");
        cleaned = cleaned.replaceAll("(?i)\\b(USER|ASSISTANT|AI|SYSTEM|BOT)\\b\\s*[:\\-]\\s*", "");
        
        // Supprime les délimiteurs répétés
        cleaned = cleaned.replaceAll("[=\\-\\*]{3,}", "");
        
        // Supprime les espaces excessifs
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        cleaned = cleaned.replaceAll(" {2,}", " ");
        
        return cleaned.trim();
    }
}
