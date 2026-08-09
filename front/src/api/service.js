import axios from 'axios';

// URL de base de l'API backend
// En développement, une URL vide utilise le proxy Vite (/api -> backend).
// En production, VITE_API_URL permet de cibler un backend distinct si nécessaire.
const API_URL = import.meta.env.VITE_API_URL || '';

// Création d'une instance Axios avec configuration
// IMPORTANT: Le timeout doit correspondre au timeout backend (300s) pour éviter les timeouts côté client
// Cette instance est utilisée pour tous les appels API
const apiClient = axios.create({
    baseURL: API_URL,
    timeout: 320000,
    headers: {
        'Content-Type': 'application/json'
    }
});

// Ajoute des logs de requête/réponse pour le debug en mode développement
if (import.meta.env.DEV) {
    apiClient.interceptors.request.use(
        config => {
            console.log(`[API] ${config.method.toUpperCase()} ${config.url}`);
            return config;
        },
        error => Promise.reject(error)
    );

    apiClient.interceptors.response.use(
        response => {
            console.log(`[API] Réponse: ${response.status} - ${response.config.url}`);
            return response;
        },
        error => {
            if (error.code === 'ECONNABORTED') {
                console.error(`[API] Timeout: ${error.config.url}`);
            } else {
                console.error(`[API] Erreur: ${error.message}`);
            }
            return Promise.reject(error);
        }
    );
}

/**
 * Service API pour toutes les opérations du chatbot
 * Gère les conversations, messages et documents
 * @returns {Object} Objet contenant toutes les méthodes API disponibles
 */
export function useApiService() {
    return {
        // =========== GESTION DES CONVERSATIONS ===========
        /**
         * Récupère la liste de toutes les conversations
         * @returns {Promise} Résultat contenant la liste des conversations
         */
        async listConversations() {
            const response = await apiClient.get('/api/chat/conversations');
            return response.data;
        },

        /**
         * Crée une nouvelle conversation
         * @returns {Promise} Résultat contenant l'ID de la nouvelle conversation
         */
        async createNewConversation() {
            const response = await apiClient.post('/api/chat/conversations/new');
            return response.data;
        },

        /**
         * Supprime une conversation existante
         * @param {string} conversationId - ID de la conversation à supprimer
         * @returns {Promise} Résultat de la suppression
         */
        async deleteConversation(conversationId) {
            const response = await apiClient.delete(`/api/chat/conversations/${conversationId}`);
            return response.data;
        },

        // =========== GESTION DES MESSAGES ===========
        /**
         * Récupère l'historique complet d'une conversation
         * @param {string} conversationId - ID de la conversation
         * @returns {Promise} Array contenant tous les messages de la conversation
         */
        async getConversationHistory(conversationId) {
            const response = await apiClient.get(`/api/chat/history/${conversationId}`);
            return response.data;
        },

        /**
         * Envoie un message et reçoit la réponse RAG
         * @param {string} conversationId - ID de la conversation
         * @param {string} message - Message utilisateur
         * @param {string|null} systemInstruction - Instruction système optionnelle
         * @returns {Promise} Résultat contenant la réponse du modèle
         */
        async sendMessage(conversationId, message, systemInstruction = null) {
            const payload = {
                conversationId,
                message
            };
            if (systemInstruction) {
                payload.systemInstruction = systemInstruction;
            }
            const response = await apiClient.post('/api/chat/send', payload);
            return response.data;
        },

        // =========== GESTION DES DOCUMENTS ===========
        /**
         * Upload un document pour ingestion RAG
         * Le document est parsé, chunké et vectorisé
         * @param {FormData} formData - Données du formulaire (fichier + conversationId optionnel)
         * @returns {Promise} Résultat contenant les détails du document uploadé
         */
        async uploadDocument(formData) {
            const config = {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            };
            const response = await apiClient.post('/api/documents/upload', formData, config);
            return response.data;
        },

        /**
         * Supprime un document et ses vecteurs associés
         * @param {string} documentId - ID du document à supprimer
         * @returns {Promise} Résultat de la suppression
         */
        async deleteDocument(documentId) {
            const response = await apiClient.delete(`/api/documents/${documentId}`);
            return response.data;
        }
    };
}
