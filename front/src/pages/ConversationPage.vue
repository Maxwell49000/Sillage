<template>
  <div class="app-shell">
    <div :class="['sidebar-panel', { open: sidebarOpen }]">
      <ConversationSidebar
        :conversations="conversations"
        :conversation-id="conversationId"
        @new-chat="startNewChat"
        @open-chat="openChat"
        @delete-chat="deleteConversation"
        @close="sidebarOpen = false"
        @go-home="goHome"
      />
    </div>
    <button
      v-if="sidebarOpen"
      class="sidebar-scrim"
      type="button"
      aria-label="Fermer le menu"
      @click="sidebarOpen = false"
    ></button>

    <main class="main-panel">
      <header class="topbar">
        <div class="topbar-title">
          <button
            class="menu-button"
            type="button"
            aria-label="Ouvrir le menu"
            @click="sidebarOpen = true"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            ><path d="M4 7h16M4 12h16M4 17h16" /></svg>
          </button>
          <div>
            <span>{{ conversationId ? 'Conversation active' : 'Espace documentaire' }}</span>
            <strong>{{ activeTitle }}</strong>
          </div>
        </div>

        <button
          class="sources-button"
          type="button"
          aria-label="Ajouter des sources"
          @click="uploadOpen = true"
        >
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
          ><path d="M4 5h16v14H4zM8 9h8m-8 4h5" /></svg>
          <span>Ajouter des sources</span>
        </button>
      </header>

      <WelcomeScreen
        v-if="!conversationId"
        @start-chat="startNewChat"
      />

      <section
        v-else
        class="chat-panel"
        aria-label="Conversation"
      >
        <ChatMessages
          ref="messagesComponent"
          :messages="messages"
        />
        <ChatInput
          v-model="messageInput"
          :is-loading="loading"
          @send="sendMessage"
        />
      </section>

      <Transition name="toast">
        <div
          v-if="notice.message"
          :class="['notice', notice.type]"
          role="status"
        >
          <span></span>{{ notice.message }}
          <button
            type="button"
            aria-label="Fermer"
            @click="clearNotice"
          >
            ×
          </button>
        </div>
      </Transition>
    </main>

    <DocumentUploadModal
      v-model="uploadOpen"
      :current-conversation-id="conversationId"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useApiService } from '@/api/service';
import { useConversationStore } from '@/stores/conversation';
import ChatInput from '@/components/ChatInput.vue';
import ChatMessages from '@/components/ChatMessages.vue';
import ConversationSidebar from '@/components/ConversationSidebar.vue';
import DocumentUploadModal from '@/components/DocumentUploadModal.vue';
import WelcomeScreen from '@/components/WelcomeScreen.vue';

const router = useRouter();
const route = useRoute();
const store = useConversationStore();
const api = useApiService();
const messageInput = ref('');
const loading = ref(false);
const conversations = ref([]);
const messagesComponent = ref(null);
const sidebarOpen = ref(false);
const uploadOpen = ref(false);
const notice = ref({ message: '', type: 'info' });
let noticeTimer;

const conversationId = computed(() => route.params.conversationId || null);
const messages = computed(() => store.currentMessages);
const activeTitle = computed(() => {
  if (!conversationId.value) return 'Bienvenue dans Sillage';
  const activeConversation = conversations.value.find(({ conversationId: id }) => id === conversationId.value);
  return activeConversation?.lastMessage?.slice(0, 56) || 'Nouvelle conversation';
});

const showNotice = (message, type = 'info') => {
  window.clearTimeout(noticeTimer);
  notice.value = { message, type };
  noticeTimer = window.setTimeout(clearNotice, 5000);
};
const clearNotice = () => { notice.value = { message: '', type: 'info' }; };

const loadConversations = async () => {
  try {
    conversations.value = await api.listConversations();
  } catch {
    showNotice('Le serveur est indisponible. Vérifiez que le backend est démarré.', 'error');
  }
};

const startNewChat = async () => {
  try {
    const response = await api.createNewConversation();
    store.setCurrentConversation(response.conversationId);
    await router.push(`/chat/${response.conversationId}`);
    await loadConversations();
  } catch {
    showNotice('Impossible de créer une conversation.', 'error');
  }
};

const openChat = (id) => router.push(`/chat/${id}`);
const goHome = () => {
  sidebarOpen.value = false;
  router.push('/');
};

const deleteConversation = async (id) => {
  if (!window.confirm('Supprimer définitivement cette conversation ?')) return;
  try {
    await api.deleteConversation(id);
    if (conversationId.value === id) await router.push('/');
    await loadConversations();
    showNotice('Conversation supprimée.', 'success');
  } catch {
    showNotice('La conversation n’a pas pu être supprimée.', 'error');
  }
};

const sendMessage = async () => {
  const content = messageInput.value.trim();
  if (!content || loading.value || !conversationId.value) return;
  messageInput.value = '';
  loading.value = true;
  const userId = `user-${Date.now()}`;
  const assistantId = `assistant-${Date.now()}`;
  const now = new Date().toISOString();

  store.addMessage({ id: userId, conversationId: conversationId.value, role: 'USER', content, timestamp: now });
  store.addMessage({ id: assistantId, conversationId: conversationId.value, role: 'ASSISTANT', content: '', timestamp: now, pending: true });

  try {
    const response = await api.sendMessage(conversationId.value, content);
    store.updateMessage(assistantId, { content: response.reply || 'Aucune réponse reçue.', pending: false });
    await loadConversations();
  } catch (error) {
    const timedOut = error.code === 'ECONNABORTED' || error.message?.toLowerCase().includes('timeout');
    store.updateMessage(assistantId, {
      content: timedOut ? 'Le modèle a mis trop de temps à répondre. Vous pouvez réessayer.' : 'Une erreur est survenue pendant la génération.',
      pending: false
    });
    showNotice(timedOut ? 'Le délai de réponse du modèle a été dépassé.' : 'La réponse n’a pas pu être générée.', 'error');
  } finally {
    loading.value = false;
    await nextTick();
    messagesComponent.value?.scrollToBottom();
  }
};

const loadMessages = async (id) => {
  try {
    const history = await api.getConversationHistory(id);
    if (conversationId.value === id) store.setCurrentMessages(history);
  } catch {
    showNotice('L’historique de la conversation est indisponible.', 'error');
  }
};

onMounted(async () => {
  await loadConversations();
  if (conversationId.value) {
    store.setCurrentConversation(conversationId.value);
    await loadMessages(conversationId.value);
  }
});

watch(conversationId, async (id) => {
  if (!id) {
    store.setCurrentConversation(null);
    return;
  }
  store.setCurrentConversation(id);
  await loadMessages(id);
});
</script>

<style scoped>
.app-shell { height: 100dvh; display: flex; overflow: hidden; background: var(--surface); }
.sidebar-panel { position: relative; z-index: 30; flex: 0 0 auto; height: 100%; }
.main-panel { position: relative; min-width: 0; flex: 1; display: flex; flex-direction: column; overflow: hidden; background: var(--surface); }
.topbar { min-height: 72px; padding: 13px clamp(18px, 3vw, 36px); display: flex; align-items: center; justify-content: space-between; gap: 20px; background: rgba(255, 253, 248, .88); border-bottom: 1px solid var(--line); backdrop-filter: blur(14px); }
.topbar-title { min-width: 0; display: flex; align-items: center; gap: 12px; }
.topbar-title > div { min-width: 0; }
.topbar-title span { display: block; margin-bottom: 3px; color: var(--ink-400); font-size: 8px; font-weight: 850; text-transform: uppercase; letter-spacing: .13em; }
.topbar-title strong { display: block; max-width: 50vw; overflow: hidden; font-family: Georgia, serif; font-size: 15px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.menu-button { display: none; }
.sources-button, .menu-button { align-items: center; justify-content: center; color: var(--ink-800); background: transparent; border: 1px solid var(--line); border-radius: 11px; cursor: pointer; }
.sources-button { padding: 10px 13px; display: flex; gap: 8px; font-size: 10px; font-weight: 750; transition: background 150ms ease, border-color 150ms ease; }
.sources-button:hover { background: var(--surface-muted); border-color: #c7c3b8; }
.sources-button svg, .menu-button svg { width: 17px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.chat-panel { min-height: 0; flex: 1; display: flex; flex-direction: column; background: radial-gradient(circle at 85% 10%, rgba(185, 216, 204, .13), transparent 27rem), var(--surface); }
.sidebar-scrim { display: none; }
.notice { position: absolute; z-index: 60; right: 24px; bottom: 24px; max-width: min(380px, calc(100% - 48px)); padding: 12px 13px; display: flex; align-items: center; gap: 9px; color: var(--ink-800); font-size: 10px; font-weight: 650; background: white; border: 1px solid var(--line); border-radius: 12px; box-shadow: var(--shadow-lg); }
.notice > span { width: 7px; height: 7px; flex: 0 0 auto; background: var(--ink-400); border-radius: 50%; }
.notice.error > span { background: var(--danger); }
.notice.success > span { background: #5a9b81; }
.notice button { margin-left: auto; color: var(--ink-400); background: none; border: 0; cursor: pointer; }
.toast-enter-active, .toast-leave-active { transition: opacity 180ms ease, transform 180ms ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(8px); }
@media (max-width: 820px) {
  .sidebar-panel { position: fixed; inset: 0 auto 0 0; z-index: 50; transform: translateX(-105%); transition: transform 220ms ease; }
  .sidebar-panel.open { transform: translateX(0); }
  .sidebar-scrim { position: fixed; inset: 0; z-index: 40; display: block; background: rgba(10, 29, 33, .55); border: 0; }
  .menu-button { width: 38px; height: 38px; display: flex; flex: 0 0 auto; }
}
@media (max-width: 560px) {
  .topbar { min-height: 64px; padding: 10px 12px; }
  .sources-button { width: 39px; height: 39px; padding: 0; }
  .sources-button span { display: none; }
  .topbar-title strong { max-width: 50vw; font-size: 13px; }
  .notice { right: 12px; bottom: 12px; max-width: calc(100% - 24px); }
}
</style>
