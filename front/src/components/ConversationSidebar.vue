<template>
  <aside
    class="sidebar"
    aria-label="Navigation des conversations"
  >
    <div class="brand-row">
      <button
        class="brand"
        type="button"
        aria-label="Retour à l’accueil"
        @click="emit('go-home')"
      >
        <span
          class="brand-mark"
          aria-hidden="true"
        >
          <svg
            viewBox="0 0 32 32"
            role="img"
          >
            <path d="M7 9h18M7 16h14M7 23h9" />
            <circle
              cx="24"
              cy="23"
              r="2.5"
            />
          </svg>
        </span>
        <span>
          <strong>Sillage</strong>
          <small>Assistant documentaire</small>
        </span>
      </button>

      <button
        class="close-mobile"
        type="button"
        aria-label="Fermer le menu"
        @click="emit('close')"
      >
        <svg
          viewBox="0 0 24 24"
          aria-hidden="true"
        ><path d="m6 6 12 12M18 6 6 18" /></svg>
      </button>
    </div>

    <button
      class="new-chat"
      type="button"
      @click="handleNewChat"
    >
      <svg
        viewBox="0 0 24 24"
        aria-hidden="true"
      ><path d="M12 5v14M5 12h14" /></svg>
      Nouvelle conversation
    </button>

    <div class="section-heading">
      <span>Conversations</span>
      <span class="count">{{ conversations.length }}</span>
    </div>

    <nav
      class="conversation-list"
      aria-label="Historique"
    >
      <div
        v-if="conversations.length === 0"
        class="empty-state"
      >
        <span
          class="empty-mark"
          aria-hidden="true"
        >
          <svg viewBox="0 0 24 24"><path d="M5 5h14v11H9l-4 3V5Z" /></svg>
        </span>
        <strong>Votre historique est vide</strong>
        <p>Créez une conversation pour commencer.</p>
      </div>

      <button
        v-for="conversation in conversations"
        :key="conversation.conversationId"
        :class="['conversation-item', { active: conversationId === conversation.conversationId }]"
        type="button"
        :aria-current="conversationId === conversation.conversationId ? 'page' : undefined"
        @click="handleOpenChat(conversation.conversationId)"
      >
        <span class="conversation-copy">
          <strong>{{ conversationTitle(conversation) }}</strong>
          <small>{{ formatDate(conversation.lastMessageAt) }}</small>
        </span>
        <span
          class="delete-action"
          role="button"
          tabindex="0"
          :aria-label="`Supprimer ${conversationTitle(conversation)}`"
          @click.stop="handleDeleteChat(conversation.conversationId)"
          @keydown.enter.stop="handleDeleteChat(conversation.conversationId)"
        >
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
          ><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg>
        </span>
      </button>
    </nav>

    <footer class="sidebar-footer">
      <span
        class="status-dot"
        aria-hidden="true"
      ></span>
      <span><strong>IA locale</strong><small>Vos données restent chez vous</small></span>
    </footer>
  </aside>
</template>

<script setup>
defineProps({
  conversations: { type: Array, required: true },
  conversationId: { type: String, default: null }
});

const emit = defineEmits(['new-chat', 'open-chat', 'delete-chat', 'close', 'go-home']);

const handleNewChat = () => {
  emit('new-chat');
  emit('close');
};

const handleOpenChat = (conversationId) => {
  emit('open-chat', conversationId);
  emit('close');
};

const handleDeleteChat = (conversationId) => emit('delete-chat', conversationId);

const conversationTitle = (conversation) => {
  const message = conversation.lastMessage?.trim();
  return message ? message.slice(0, 42) : 'Nouvelle conversation';
};

const formatDate = (value) => {
  if (!value) return 'À l’instant';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';

  const today = new Date();
  if (date.toDateString() === today.toDateString()) {
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
};
</script>

<style scoped>
.sidebar {
  width: 292px;
  height: 100%;
  padding: 22px 16px 16px;
  display: flex;
  flex-direction: column;
  color: #f8f6ef;
  background: var(--ink-950);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-row,
.brand,
.sidebar-footer,
.section-heading {
  display: flex;
  align-items: center;
}

.brand-row { justify-content: space-between; }

.brand {
  gap: 11px;
  padding: 0;
  color: inherit;
  text-align: left;
  background: none;
  border: 0;
  cursor: pointer;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  color: var(--paper);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 13px;
}

.brand-mark svg { width: 27px; }
.brand-mark path, .brand-mark circle { fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; }
.brand-mark circle { fill: var(--accent); stroke: none; }
.brand strong { display: block; font-family: Georgia, serif; font-size: 21px; letter-spacing: -0.4px; }
.brand small { display: block; margin-top: 2px; color: #9fb2b5; font-size: 10px; letter-spacing: 0.04em; }

.close-mobile {
  display: none;
  width: 36px;
  height: 36px;
  place-items: center;
  color: inherit;
  background: transparent;
  border: 0;
  border-radius: 10px;
}
.close-mobile svg { width: 20px; fill: none; stroke: currentColor; stroke-width: 1.8; }

.new-chat {
  width: 100%;
  margin: 28px 0 27px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: white;
  font-size: 13px;
  font-weight: 700;
  background: var(--accent);
  border: 0;
  border-radius: 12px;
  cursor: pointer;
  transition: background 160ms ease, transform 160ms ease;
}
.new-chat:hover { background: #f0715b; transform: translateY(-1px); }
.new-chat svg { width: 18px; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; }

.section-heading {
  justify-content: space-between;
  padding: 0 6px 10px;
  color: #91a6aa;
  font-size: 10px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.13em;
}
.count { min-width: 22px; padding: 3px 6px; text-align: center; background: rgba(255, 255, 255, 0.07); border-radius: 999px; }

.conversation-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  overflow-y: auto;
}

.conversation-item {
  width: 100%;
  min-height: 58px;
  padding: 10px 10px 10px 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #d9e0df;
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: background 150ms ease, border-color 150ms ease;
}
.conversation-item:hover { background: rgba(255, 255, 255, 0.055); }
.conversation-item.active { background: rgba(185, 216, 204, 0.11); border-color: rgba(185, 216, 204, 0.18); }
.conversation-item.active::before { content: ''; width: 3px; height: 28px; margin-left: -9px; background: var(--accent); border-radius: 99px; }
.conversation-copy { min-width: 0; flex: 1; }
.conversation-copy strong { display: block; overflow: hidden; color: inherit; font-size: 12px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.conversation-copy small { display: block; margin-top: 5px; color: #82999d; font-size: 10px; }
.delete-action { width: 30px; height: 30px; display: grid; place-items: center; color: #8fa3a6; border-radius: 8px; opacity: 0; transition: opacity 150ms ease, color 150ms ease, background 150ms ease; }
.conversation-item:hover .delete-action, .delete-action:focus { opacity: 1; }
.delete-action:hover { color: #ffb3a7; background: rgba(231, 101, 79, 0.13); }
.delete-action svg { width: 15px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }

.empty-state { margin: auto 20px; color: #8ca1a5; text-align: center; }
.empty-state strong { display: block; color: #c8d3d2; font-size: 12px; }
.empty-state p { margin: 6px 0 0; font-size: 11px; line-height: 1.5; }
.empty-mark { width: 46px; height: 46px; margin: 0 auto 12px; display: grid; place-items: center; background: rgba(255, 255, 255, 0.05); border-radius: 14px; }
.empty-mark svg { width: 22px; fill: none; stroke: currentColor; stroke-width: 1.5; }

.sidebar-footer { gap: 10px; margin-top: 14px; padding: 13px 10px 4px; border-top: 1px solid rgba(255, 255, 255, 0.08); }
.sidebar-footer > span:last-child { display: flex; flex-direction: column; }
.sidebar-footer strong { color: #cad6d4; font-size: 10px; }
.sidebar-footer small { margin-top: 2px; color: #71898d; font-size: 9px; }
.status-dot { width: 8px; height: 8px; background: #71c7a5; border-radius: 50%; box-shadow: 0 0 0 4px rgba(113, 199, 165, 0.1); }

@media (max-width: 820px) {
  .sidebar { width: min(310px, 88vw); box-shadow: var(--shadow-lg); }
  .close-mobile { display: grid; cursor: pointer; }
}
</style>
