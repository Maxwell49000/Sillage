package com.springai.chatspringai.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration pour le VectorStore pgvector.
 * Implémentation simplifiée qui utilise PostgreSQL + pgvector extension
 * pour stocker et rechercher les embeddings.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {
    
    private final EmbeddingModel embeddingModel;
    private final DataSource dataSource;
    
    /**
     * Crée le bean VectorStore qui utilise PostgreSQL + pgvector.
     * Cette implémentation simple stocke les embeddings dans une table.
     */
    @Bean
    public SimplePostgresVectorStore vectorStore() {
        log.info("🔍 Creating simple PgVector-based VectorStore for embeddings");
        return new SimplePostgresVectorStore(dataSource, embeddingModel);
    }
    
    /**
     * Implémentation simple d'un VectorStore qui utilise PostgreSQL et pgvector.
     * Stocke les embeddings dans la table vector_store et utilise la recherche de similarité.
     */
    @Slf4j
    public static class SimplePostgresVectorStore {
        
        private final JdbcTemplate jdbcTemplate;
        private final EmbeddingModel embeddingModel;
        private final ObjectMapper objectMapper = new ObjectMapper();
        
        public SimplePostgresVectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
            this.embeddingModel = embeddingModel;
            initializeTable();
        }
        
        /**
         * Initialise la table vector_store si elle n'existe pas déjà.
         * IMPORTANT: Ne pas DROP la table - persiste les vecteurs entre redémarrages!
         */
        private void initializeTable() {
            try {
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS vector_store (
                        id SERIAL PRIMARY KEY,
                        content TEXT NOT NULL,
                        metadata JSONB,
                        embedding vector(768),
                        created_at TIMESTAMP DEFAULT NOW()
                    )
                    """);
                jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
                    ON vector_store USING ivfflat (embedding vector_cosine_ops)
                    WITH (lists = 100)
                    """);
                
                log.info("✅ Vector store table initialized (persisted across restarts)");
            } catch (Exception e) {
                log.error("❌ Vector store table initialization failed", e);
                throw e;
            }
        }
        
        /**
         * Ajoute une liste de documents à l'index vectoriel.
         */
        public void add(List<Document> documents) {
            for (Document doc : documents) {
                try {
                    // Vectorise le contenu du document avec EmbeddingModel (nomic-embed-text)
                    float[] embedding = embeddingModel.embed(doc.getText());
                    
                    // Sauvegarde en PostgreSQL avec le vecteur
                    String sql = "INSERT INTO vector_store (content, metadata, embedding) VALUES (?, ?::jsonb, ?::vector)";
                    String metadata = doc.getMetadata() != null ? 
                        objectMapper.writeValueAsString(doc.getMetadata()) :
                        "{}";
                    
                    // Convertit le float[] en format vecteur PostgreSQL [0.234, -0.891, ...]
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < embedding.length; i++) {
                        if (i > 0) sb.append(",");
                        sb.append(embedding[i]);
                    }
                    sb.append("]");
                    String vectorStr = sb.toString();
                    
                    jdbcTemplate.update(sql, doc.getText(), metadata, vectorStr);
                    log.debug("✅ Document ajouté au VectorStore");
                } catch (Exception e) {
                    log.error("❌ Erreur lors de l'ajout du document au VectorStore: {}", e.getMessage(), e);
                }
            }
        }
        
        /**
         * Recherche des documents similaires basée sur une requête.
         * Utilise la similarité cosinus pour trouver les vecteurs les plus proches.
         * @param query Requête utilisateur (ex: "Qui est Salomé Bigot?")
         * @param topK Nombre de résultats à retourner (ex: 5)
         * @return Liste des documents pertinents ordonnés par similarité
         */
        public List<Document> similaritySearch(String query, int topK) {
            return similaritySearch(query, topK, null, 0.0);
        }

        /**
         * Recherche les fragments pertinents en respectant la portée d'une conversation.
         * Les documents globaux (sans conversation_id) restent accessibles partout.
         */
        public List<Document> similaritySearch(String query, int topK, String conversationId, double similarityThreshold) {
            try {
                // Vectorise la requête utilisateur avec EmbeddingModel
                float[] queryEmbedding = embeddingModel.embed(query);
                
                // Crée la chaîne vecteur au format PostgreSQL [0.234, -0.891, ...]
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < queryEmbedding.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(queryEmbedding[i]);
                }
                sb.append("]");
                String vectorStr = sb.toString();
                
                String sql;
                Object[] parameters;
                if (conversationId == null || conversationId.isBlank()) {
                    sql = """
                        SELECT content, metadata, 1 - (embedding <=> ?::vector) AS similarity
                        FROM vector_store
                        WHERE 1 - (embedding <=> ?::vector) >= ?
                        ORDER BY embedding <=> ?::vector
                        LIMIT ?
                        """;
                    parameters = new Object[] { vectorStr, vectorStr, similarityThreshold, vectorStr, topK };
                } else {
                    sql = """
                        SELECT content, metadata, 1 - (embedding <=> ?::vector) AS similarity
                        FROM vector_store
                        WHERE (metadata->>'conversation_id' = ? OR metadata->>'conversation_id' IS NULL)
                          AND 1 - (embedding <=> ?::vector) >= ?
                        ORDER BY embedding <=> ?::vector
                        LIMIT ?
                        """;
                    parameters = new Object[] { vectorStr, conversationId, vectorStr, similarityThreshold, vectorStr, topK };
                }
                
                List<Document> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    Map<String, Object> metadata = new HashMap<>();
                    try {
                        String metadataStr = rs.getString("metadata");
                        if (metadataStr != null && !metadataStr.equals("{}")) {
                            metadata = objectMapper.readValue(metadataStr, HashMap.class);
                        }
                    } catch (Exception e) {
                        log.debug("⚠️ Erreur lors du parsing des métadonnées: {}", e.getMessage());
                    }
                    return new Document(rs.getString("content"), metadata);
                }, parameters);
                
                log.info("🔍 Trouvé {} documents similaires", results.size());
                return results;
            } catch (Exception e) {
                log.error("❌ Erreur lors de la recherche dans le VectorStore: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        }

        /** Supprime tous les embeddings rattachés à un document. */
        public int deleteByDocumentId(String documentId) {
            int deleted = jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'document_id' = ?",
                documentId
            );
            log.info("{} vecteur(s) supprimé(s) pour le document {}", deleted, documentId);
            return deleted;
        }
    }
}
