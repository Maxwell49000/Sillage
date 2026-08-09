package com.springai.chatspringai.service;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.springai.chatspringai.dto.MessageDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Service de chat scopé par requête HTTP. Permet de gérer une conversation
 * à la fois, mais permet de changer facilement de conversation.
 * L'historique est persisté via ChatMemoryService.
 */
@Slf4j
@Component
@RequestScope
public class ChatService {
    private final ChatClient chatClient;
    private String conversationId;
    private final List<String> history = new ArrayList<>();
    private final ChatMemoryService chatMemoryService;
    private final RagChatService ragChatService;
    
    /** Instructions système pour guider le format et le ton des réponses du modèle */
    private static final String SYSTEM_INSTRUCTION = "RÈGLES ABSOLUES - À SUIVRE STRICTEMENT:\n" +
        "1. Si des documents sont fournis (section 'Documents pertinents'), lis-les avec attention.\n" +
        "2. ⚠️ CRITIQUE: Tous les détails supplémentaires QUE TU N'AS PAS LUS DANS LES DOCUMENTS sont INTERDITS.\n" +
        "3. Répondre UNIQUEMENT ce que tu lis explicitement dans les documents fournis.\n" +
        "4. Ne JAMAIS ajouter d'informations de tes connaissances générales, même si tu en as.\n" +
        "5. Si la réponse n'est pas dans le document, dire: 'Je ne trouve pas cette information dans le document fourni.'\n" +
        "6. Réponse la plus courte possible. Une phrase suffit si c'est tout ce qu'il y a.\n" +
        "7. Absolument zéro hallucination, zéro invention, zéro détail bonus.";

    public ChatService(ChatClient.Builder chatClientBuilder, ChatMemoryService chatMemoryService, RagChatService ragChatService) {
        this.chatClient = chatClientBuilder.build();
        this.conversationId = UUID.randomUUID().toString();
        this.chatMemoryService = chatMemoryService;
        this.ragChatService = ragChatService;
    }

    /** ExecutorService utilisé pour exécuter les appels au modèle hors des threads servlet et appliquer les timeouts */
    private static final ExecutorService modelExecutor = Executors.newCachedThreadPool();
    
    /** ExecutorService pour les opérations RAG (recherche vectorielle) */
    private static final ExecutorService ragExecutor = Executors.newCachedThreadPool();

    /**
     * Définit la conversation courante (utile pour charger une conversation existante).
     * Recharge l'historique depuis la BDD pour que le modèle IA le connaisse.
     */
    public void setConversationId(String newConversationId) {
        this.conversationId = newConversationId;
        this.history.clear();
        
        // Recharge l'historique depuis la BDD
        if (this.chatMemoryService != null) {
            try {
                List<MessageDTO> messages = this.chatMemoryService.getConversationMessagesDetailed(newConversationId);
                for (MessageDTO msg : messages) {
                    String role = msg.getRole() != null ? msg.getRole() : "USER";
                    String content = msg.getContent() != null ? msg.getContent() : "";
                    // Nettoie tout prefix de rôle qui aurait pu être ajouté par erreur
                    content = sanitizeContent(content);
                    history.add(role + ": " + content);
                }
                log.info("Loaded {} messages for conversation {}", messages.size(), newConversationId);
            } catch (Exception ex) {
                log.warn("Failed to load conversation history for {}: {}", newConversationId, ex.getMessage(), ex);
            }
        }
    }

    public synchronized String chat(String prompt) {
        // Ajoute le tour utilisateur à l'historique
        history.add("User: " + sanitizeContent(prompt));
        try {
            if (this.chatMemoryService != null) {
                this.chatMemoryService.saveUserMessage(conversationId, prompt);
            }
        } catch (Exception ex) {
            // Enregistre l'erreur sans interrompre le flux de conversation
            log.warn("Failed to persist user message for conversation {}: {}", conversationId, ex.getMessage(), ex);
        }

        // 🔍 RECHERCHE RAG : Cherche les documents pertinents pour augmenter le contexte
        String ragContext = "";
        try {
            if (ragChatService != null) {
                // Exécute la recherche RAG avec timeout de 10 secondes max
                Callable<String> ragTask = () -> {
                    List<Document> relevantDocs = ragChatService.searchByConversation(conversationId, prompt);
                    if (!relevantDocs.isEmpty()) {
                        log.info("📚 RAG: {} documents pertinents trouvés pour la question", relevantDocs.size());
                        return ragChatService.formatDocumentsAsContext(relevantDocs);
                    }
                    return "";
                };
                
                Future<String> ragFuture = ragExecutor.submit(ragTask);
                final long RAG_TIMEOUT_SEC = 30; // 30 secondes max pour la recherche RAG (1er appel charge le modèle)
                try {
                    ragContext = ragFuture.get(RAG_TIMEOUT_SEC, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    ragFuture.cancel(true);
                    log.warn("RAG search timed out after {} seconds, continuing without context", RAG_TIMEOUT_SEC);
                    ragContext = "";
                }
            }
        } catch (Exception e) {
            log.warn("RAG search failed, continuing without context: {}", e.getMessage());
        }
        
        // Augmente l'instruction système avec le contexte RAG si disponible
        final String systemPrompt;
        if (!ragContext.isEmpty()) {
            systemPrompt = SYSTEM_INSTRUCTION + "\n\n" + ragContext;
        } else {
            systemPrompt = SYSTEM_INSTRUCTION;
        }

        // Construit un prompt combiné à partir d'une fenêtre limitée de l'historique
        // Pour les PC faibles, limiter à N derniers tours réduit la taille du prompt envoyé
        final int MAX_TURNS = 10; // Réduire la fenêtre pour diminuer la taille du prompt
        int start = Math.max(0, history.size() - MAX_TURNS);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < history.size(); i++) {
            // Garde les labels complets pour clarté du modèle (plus stable que les abréviations)
            String h = history.get(i);
            sb.append(h).append("\n");
        }
        sb.append("Assistant:");
        String combined = sb.toString();

        log.info("Calling chat model: conversationId={}, prompt-length={}, has-rag-context={}", 
                 conversationId, combined.length(), !ragContext.isEmpty());
        String reply = null;
        try {
            // Exécute l'appel au modèle (bloquant) dans un thread séparé avec timeout
            Callable<String> task = () -> this.chatClient.prompt(systemPrompt)
                    .user(combined)
                    .call()
                    .content();

            Future<String> future = modelExecutor.submit(task);
            final long MODEL_TIMEOUT_SEC = 300; // 5 minutes maximum
            try {
                reply = future.get(MODEL_TIMEOUT_SEC, TimeUnit.SECONDS);
                log.info("Model returned reply-length={}", reply == null ? 0 : reply.length());
            } catch (TimeoutException te) {
                future.cancel(true);
                log.error("Model call timed out after {} seconds for conversation {}", MODEL_TIMEOUT_SEC, conversationId);
                reply = "Erreur: délai d'attente du modèle dépassé.";
            } catch (ExecutionException ee) {
                log.error("ExecutionException during model call for conversation {}", conversationId, ee.getCause());
                throw new RuntimeException(ee.getCause());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Model call interrupted for conversation {}", conversationId, ie);
            throw new RuntimeException(ie);
        }
        
        // Nettoie et enregistre la réponse de l'assistant pour éviter l'injection de labels de rôle
        String cleanReply = sanitizeContent(reply == null ? "" : reply);
        history.add("Assistant: " + cleanReply);
        try {
            if (this.chatMemoryService != null) {
                // Persiste la réponse nettoyée pour éviter de stocker des labels de rôle ou des artefacts
                this.chatMemoryService.saveAssistantMessage(conversationId, cleanReply);
            }
        } catch (Exception ex) {
            log.warn("Failed to persist assistant message for conversation {}: {}", conversationId, ex.getMessage(), ex);
        }
        return reply;
    }

    public synchronized String systemUser(String systemInstruction, String userMessage) {
        // Ajoute l'instruction système et le message utilisateur à l'historique
        history.add("System: " + sanitizeContent(systemInstruction));
        history.add("User: " + sanitizeContent(userMessage));
        try {
            if (this.chatMemoryService != null) {
                this.chatMemoryService.saveUserMessage(conversationId, userMessage);
            }
        } catch (Exception ex) {
            log.warn("Failed to persist system/user message for conversation {}: {}", conversationId, ex.getMessage(), ex);
        }

        // Combine les instructions système par défaut avec l'instruction personnalisée
        String combinedSystem = SYSTEM_INSTRUCTION + "\n" + systemInstruction;
        String reply = null;
        try {
            // Exécute l'appel au modèle (bloquant) dans un thread séparé avec timeout
            Callable<String> task = () -> this.chatClient.prompt(combinedSystem)
                    .user(userMessage)
                    .call()
                    .content();

            Future<String> future = modelExecutor.submit(task);
            final long MODEL_TIMEOUT_SEC = 300; // 5 minutes maximum
            try {
                reply = future.get(MODEL_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                future.cancel(true);
                log.error("Model call timed out after {} seconds for conversation {} (systemUser)", MODEL_TIMEOUT_SEC, conversationId);
                reply = "Erreur: délai d'attente du modèle dépassé.";
            } catch (ExecutionException ee) {
                log.error("ExecutionException during model call for conversation {} (systemUser)", conversationId, ee.getCause());
                throw new RuntimeException(ee.getCause());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Model call interrupted for conversation {} (systemUser)", conversationId, ie);
            throw new RuntimeException(ie);
        }

        // Nettoie et enregistre la réponse de l'assistant
        String cleanReply = sanitizeContent(reply == null ? "" : reply);
        history.add("Assistant: " + cleanReply);
        try {
            if (this.chatMemoryService != null) {
                this.chatMemoryService.saveAssistantMessage(conversationId, cleanReply);
            }
        } catch (Exception ex) {
            log.warn("Failed to persist assistant message for conversation {}: {}", conversationId, ex.getMessage(), ex);
        }
        return reply;
    }

    public String getConversationId() {
        return conversationId;
    }

    public synchronized List<String> getHistory() {
        return List.copyOf(history);
    }

    /**
     * Variante utilisée par les tâches de fond : ne persiste PAS le message utilisateur
     * (pour éviter les duplications lorsque le contrôleur a déjà enregistré un placeholder),
     * mais inclut le message utilisateur dans le prompt envoyé au modèle et persiste
     * la réponse assistant une fois reçue.
     */
    public synchronized String chatBackground(String prompt) {
        // Construit le prompt à partir d'une fenêtre limitée de l'historique et ajoute le tour utilisateur
        final int MAX_TURNS = 10;
        int start = Math.max(0, history.size() - MAX_TURNS);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < history.size(); i++) {
            // Garde les labels complets pour clarté du modèle
            String h = history.get(i);
            sb.append(h).append("\n");
        }
        // Ajoute le message utilisateur directement (sans le persister)
        sb.append("User: ").append(sanitizeContent(prompt)).append("\n");
        sb.append("Assistant:");

        String combined = sb.toString();
        log.info("Background calling chat model: conversationId={}, prompt-length={}", conversationId, combined.length());

        String reply = null;
        try {
            // Exécute l'appel au modèle (bloquant) dans un thread séparé avec timeout
            Callable<String> task = () -> this.chatClient.prompt(SYSTEM_INSTRUCTION)
                    .user(combined)
                    .call()
                    .content();

            Future<String> future = modelExecutor.submit(task);
            final long MODEL_TIMEOUT_SEC = 300; // 5 minutes maximum
            try {
                reply = future.get(MODEL_TIMEOUT_SEC, TimeUnit.SECONDS);
                log.info("Background model returned reply-length={}", reply == null ? 0 : reply.length());
            } catch (TimeoutException te) {
                future.cancel(true);
                log.error("Background model call timed out after {} seconds for conversation {}", MODEL_TIMEOUT_SEC, conversationId);
                reply = "Erreur: délai d'attente du modèle dépassé.";
            } catch (ExecutionException ee) {
                log.error("ExecutionException during background model call for conversation {}", conversationId, ee.getCause());
                throw new RuntimeException(ee.getCause());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Background model call interrupted for conversation {}", conversationId, ie);
            throw new RuntimeException(ie);
        }

        // Ajoute la réponse nettoyée à l'historique et persiste
        String cleanReply = sanitizeContent(reply == null ? "" : reply);
        history.add("Assistant: " + cleanReply);
        try {
            if (this.chatMemoryService != null) {
                this.chatMemoryService.saveAssistantMessage(conversationId, cleanReply);
            }
        } catch (Exception ex) {
            log.warn("Failed to persist assistant message for conversation {} (background): {}", conversationId, ex.getMessage(), ex);
        }

        return reply;
    }

    /**
     * Nettoie une chaîne renvoyée par le modèle pour retirer les labels de rôle
     * que le modèle pourrait insérer (USER:, ASSISTANT:, AI:, SYSTEM:, etc.).
     * 
     * Cela évite que le modèle ajoute des préfixes indésirables qui pourraient
     * être interprétés par le contrôleur comme des labels de rôle.
     *
     * @param s La chaîne à nettoyer
     * @return La chaîne nettoyée et trimée
     */
    private String sanitizeContent(String s) {
        if (s == null) return null;
        
        String t = s;
        
        // Supprime les labels de rôle au DÉBUT
        // Patterns: "User:", "U:", "A:", "Assistant:", "AI:", "SYSTEM:", etc.
        t = t.replaceAll("(?i)^\\s*[UA]\\s*(?:\\bASSISTANT\\b)?\\s*[:\\-]\\s*", "");
        t = t.replaceAll("(?i)^\\s*\\b(USER|ASSISTANT|AI|SYSTEM|BOT)\\b\\s*[:\\-]\\s*", "");
        
        // Supprime les labels de rôle au MILIEU de la réponse (artefacts du modèle)
        // Exemple: "... blah blah ==--- Now, let's assume: User: blah blah" → garder seulement avant "User:"
        t = t.replaceAll("(?i)\\s+===\\s*(Now|Let's assume|---).+?(?=\\b(USER|ASSISTANT|AI|SYSTEM)\\b\\s*[:\\-])", "");
        // Nettoie les patterns comme "User:", "Assistant:", "A:", "U:" quand ils apparaissent milieu/fin
        t = t.replaceAll("(?i)\\n\\s*[UA]\\s*(?:\\bASSISTANT\\b)?\\s*[:\\-]\\s*.*", "");
        t = t.replaceAll("(?i)\\n\\s*\\b(USER|ASSISTANT|AI|SYSTEM|BOT|Me|You)\\b\\s*[:\\-]\\s*.*", "");
        
        // Supprime les sections xyptonites (chaines de caractères bizarres)
        t = t.replaceAll("[=\\-\\*]{3,}", "");
        
        // Réduit les espaces multiples et whitespace excessif
        t = t.replaceAll("\\s+", " ");
        
        // Trime les extrémités et éventuels caractères de bruit
        t = t.trim();
        
        return t;
    }

}
