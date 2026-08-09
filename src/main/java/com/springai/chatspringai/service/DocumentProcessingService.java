package com.springai.chatspringai.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service pour traiter et parser les documents uploadés.
 * Supporte PDF, DOCX, DOC, TXT et autres formats avec Apache Tika.
 */
@Slf4j
@Service
public class DocumentProcessingService {
    
    private final Tika tika = new Tika();
    
    /**
     * Parse le contenu d'un fichier et retourne le texte brut.
     * Détecte automatiquement le format basé sur le file_type.
     * 
     * @param fileContent Le contenu binaire du fichier
     * @param fileName Le nom du fichier
     * @param fileType Le type MIME du fichier
     * @return Le texte brut extrait du fichier
     */
    public String processPdfFile(byte[] fileContent, String fileName, String fileType) throws IOException, TikaException {
        try {
            log.info("📄 Traitement du fichier : {} (type: {})", fileName, fileType);
            
            String text = extractTextWithTika(fileContent);
            log.info("✅ Fichier parsé avec succès");
            return text;
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement du fichier : {}", fileName, e);
            throw new RuntimeException("Erreur traitement fichier : " + e.getMessage(), e);
        }
    }
    
    /**
     * Extrait le texte avec Tika (support multiple formats: PDF, DOCX, DOC, TXT, etc).
     */
    private String extractTextWithTika(byte[] fileContent) throws IOException, TikaException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent)) {
            String text = tika.parseToString(inputStream);
            if (text == null || text.isEmpty()) {
                log.warn("⚠️ Attention: Le fichier est vide ou non parsable");
                return "";
            }
            return text;
        }
    }
}