import { createRouter, createWebHashHistory } from 'vue-router';
import ConversationPage from 'pages/ConversationPage.vue';

/**
 * Définition des routes de l'application
 * - / : Accueil (aucune conversation sélectionnée)
 * - /chat/:conversationId : Page de chat avec une conversation spécifique
 */
const routes = [
    {
        // Route racine - aucune conversation active
        path: '/',
        component: ConversationPage,
        name: 'home'
    },
    {
        // Route avec conversation spécifique
        path: '/chat/:conversationId',
        component: ConversationPage,
        name: 'chat'
    },

];

/**
 * Création du routeur Vue
 * Utilise l'historique en hash (#) pour compatibilité sans serveur
 */
const router = createRouter({
    history: createWebHashHistory(import.meta.env.BASE_URL || '/'),
    routes
});

export default router;
