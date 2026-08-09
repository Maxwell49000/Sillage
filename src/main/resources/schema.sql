-- ========== EXISTING CHAT MEMORY TABLE ==========
CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
  conversation_id VARCHAR(36) NOT NULL,
  content TEXT NOT NULL,
  type VARCHAR(10) DEFAULT 'USER',
  timestamp TIMESTAMP DEFAULT now(),
  CONSTRAINT chat_type_check CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
);

CREATE INDEX IF NOT EXISTS idx_conversation_id ON spring_ai_chat_memory(conversation_id);

-- ========== RAG: DOCUMENT UPLOADS TABLE ==========
CREATE TABLE IF NOT EXISTS document_uploads (
  id VARCHAR(36) PRIMARY KEY,
  conversation_id VARCHAR(36) NULL,
  file_name VARCHAR(512) NOT NULL,
  file_size BIGINT NOT NULL,
  file_type VARCHAR(50) NOT NULL,
  content TEXT NOT NULL,
  number_of_chunks INT DEFAULT 0,
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ensure conversation_id is nullable (for global uploads)
ALTER TABLE document_uploads 
  ALTER COLUMN conversation_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_document_uploads_conversation_id ON document_uploads(conversation_id);
CREATE INDEX IF NOT EXISTS idx_document_uploads_created_at ON document_uploads(created_at);

-- ========== RAG: DOCUMENT CHUNKS (for embeddings) ==========
CREATE TABLE IF NOT EXISTS document_chunks (
  id VARCHAR(36) PRIMARY KEY,
  document_id VARCHAR(36) NOT NULL,
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  chunk_size INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (document_id) REFERENCES document_uploads(id) ON DELETE CASCADE,
  UNIQUE(document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);

-- ========== pgvector EXTENSION ==========
CREATE EXTENSION IF NOT EXISTS vector;

-- ========== RAG: VECTOR STORE ==========
CREATE TABLE IF NOT EXISTS vector_store (
  id SERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  metadata JSONB,
  embedding vector(768),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
  ON vector_store USING ivfflat (embedding vector_cosine_ops)
  WITH (lists = 100);
