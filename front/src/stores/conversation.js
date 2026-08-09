import { defineStore } from 'pinia';
import { ref } from 'vue';

/**
 * Store Pinia pour gérer l'état global des conversations
 * Responsabilités:
 * - Maintenir la conversation courante active
 * - Gérer la liste des messages de la conversation
 * - Fournir des mutations pour mettre à jour l'état
 */
export const useConversationStore = defineStore('conversation', () => {
    // =========== ÉTAT RÉACTIF ===========

    /**
     * ID de la conversation courante
     * Utilisé pour identifier quelle conversation est active
     * @type {Ref<string|null>}
     */
    const currentConversation = ref(null);

    /**
     * Array des messages de la conversation courante
     * Chaque message contient: {id, role, content, timestamp}
     * @type {Ref<Array<Object>>}
     */
    const currentMessages = ref([]);

    // =========== MUTATIONS ===========

    /**
     * Définit la conversation courante et réinitialise les messages
     * Appelé quand l'utilisateur switch entre conversations
     * @param {string} conversationId - ID de la nouvelle conversation active
     */
    const setCurrentConversation = (conversationId) => {
        currentConversation.value = conversationId;
        currentMessages.value = [];
    };

    /**
     * Met à jour la liste complète des messages pour la conversation courante
     * Appelé après récupération de l'historique du backend
     * @param {Array<Object>} messages - Tous les messages de la conversation
     */
    const setCurrentMessages = (messages) => {
        currentMessages.value = messages;
    };

    /**
     * Ajoute un nouveau message à la fin de la conversation
     * Appelé quand un message utilisateur ou réponse IA arrive
     * @param {Object} message - Objet message à ajouter {id, role, content, timestamp}
     */
    const addMessage = (message) => {
        currentMessages.value.push(message);
    };

    /**
     * Met à jour le contenu d'un message existant
     * Utile pour mettre à jour le contenu d'une réponse IA en streaming
     * @param {string} id - ID du message à mettre à jour
     * @param {string} content - Nouveau contenu du message
     */
    const updateMessageContent = (id, content) => {
        const idx = currentMessages.value.findIndex(m => m.id === id);
        if (idx !== -1) {
            currentMessages.value[idx].content = content;
        }
    };

    const updateMessage = (id, changes) => {
        const message = currentMessages.value.find(item => item.id === id);
        if (message) Object.assign(message, changes);
    };

    /**
     * Vide tous les messages de la conversation courante
     * Appelé lors du changement de conversation ou de suppression
     */
    const clearMessages = () => {
        currentMessages.value = [];
    };

    // =========== EXPORTS ===========
    return {
        currentConversation,
        currentMessages,
        setCurrentConversation,
        setCurrentMessages,
        addMessage,
        updateMessageContent,
        updateMessage,
        clearMessages
    };
});
