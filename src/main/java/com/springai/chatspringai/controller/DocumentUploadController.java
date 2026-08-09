package com.springai.chatspringai.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.springai.chatspringai.dto.DocumentUploadResponse;
import com.springai.chatspringai.entity.DocumentUpload;
import com.springai.chatspringai.service.RagIngestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST pour gérer l'upload et la gestion des documents pour le RAG.
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentUploadController {
    
    private final RagIngestionService ragIngestionService;

    @Value("${app.file.max-size:52428800}")
    private long maxFileSize;

    @Value("${app.file.allowed-types:pdf,docx,doc,txt}")
    private String allowedTypes;
    
    /**
     * Upload un document pour une conversation (optionnelle).
     * Le fichier est parsé, chunké, vectorisé et indexé automatiquement.
     * Si pas de conversationId, le document est ajouté au contexte global.
     * 
     * @param conversationId ID de la conversation (optionnel)
     * @param file Le fichier à uploader (PDF, DOCX, TXT, etc.)
     * @return Réponse avec les métadonnées du document uploadé
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam("file") MultipartFile file) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("📤 Upload reçu : {} pour la conversation {}", file.getOriginalFilename(), conversationId);
            
            String fileName = validateAndNormalizeFile(file);
            String fileType = file.getContentType();
            byte[] fileContent = file.getBytes();
            
            log.info("📋 Fichier : {}, Type : {}, Taille : {} bytes", fileName, fileType, fileContent.length);
            
            // Ingestion complète : parse → chunk → embed → persist
            DocumentUpload docUpload = ragIngestionService.ingestDocument(
                conversationId,
                fileName,
                fileType,
                fileContent
            );
            
            // Comptez le nombre de chunks créés
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("✅ Document {} uploadé en {} ms", docUpload.getId(), elapsedTime);
            
            return ResponseEntity.ok(
                DocumentUploadResponse.builder()
                    .conversationId(conversationId)
                    .documentId(docUpload.getId())
                    .fileName(docUpload.getFileName())
                    .fileSize(docUpload.getFileSize())
                    .fileType(docUpload.getFileType())
                    .numberOfChunks(docUpload.getNumberOfChunks())
                    .uploadedAt(docUpload.getUploadedAt())
                    .success(true)
                    .message("Document uploadé et vectorisé avec succès")
                    .build()
            );
            
        } catch (IllegalArgumentException e) {
            log.warn("Upload refusé : {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(DocumentUploadResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (IOException e) {
            log.error("❌ Erreur lors de la lecture du fichier", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DocumentUploadResponse.builder()
                    .success(false)
                    .message("Erreur lors de la lecture du fichier : " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'ingestion du document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DocumentUploadResponse.builder()
                    .success(false)
                    .message("Erreur lors de l'ingestion : " + e.getMessage())
                    .build());
        }
    }

    private String validateAndNormalizeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale autorisée.");
        }

        String fileName = StringUtils.cleanPath(
            file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()
        );
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("Le nom du fichier est invalide.");
        }

        String extension = StringUtils.getFilenameExtension(fileName);
        Set<String> allowedExtensions = Arrays.stream(allowedTypes.split(","))
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Ce format de fichier n’est pas accepté.");
        }
        return fileName;
    }
    
    /**
     * Supprime un document et tous ses chunks du système RAG.
     * 
     * @param documentId ID du document à supprimer
     * @return Message de confirmation
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        try {
            log.info("🗑️  Suppression du document {}", documentId);
            ragIngestionService.deleteDocument(documentId);
            
            return ResponseEntity.ok()
                .body(java.util.Map.of(
                    "success", true,
                    "message", "Document supprimé avec succès"
                ));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression du document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(java.util.Map.of(
                    "success", false,
                    "message", "Erreur lors de la suppression : " + e.getMessage()
                ));
        }
    }
}
