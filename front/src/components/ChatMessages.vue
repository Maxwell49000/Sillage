<template>
  <section
    ref="messagesContainer"
    class="messages-area"
    aria-label="Messages de la conversation"
    aria-live="polite"
  >
    <div
      v-if="messages.length === 0"
      class="empty-chat"
    >
      <span
        class="empty-icon"
        aria-hidden="true"
      >
        <svg viewBox="0 0 32 32"><path d="M7 9h18M7 16h14M7 23h9" /><circle
          cx="24"
          cy="23"
          r="2.5"
        /></svg>
      </span>
      <h2>Posez votre première question</h2>
      <p>Les documents associés à cette conversation seront utilisés pour contextualiser la réponse.</p>
    </div>

    <div
      v-else
      class="messages-list"
    >
      <article
        v-for="(message, index) in messages"
        :key="message.id || `${message.timestamp}-${index}`"
        :class="['message', roleClass(message.role)]"
      >
        <div class="message-meta">
          <span
            class="avatar"
            aria-hidden="true"
          >
            <svg
              v-if="roleClass(message.role) === 'assistant'"
              viewBox="0 0 32 32"
            ><path d="M7 9h18M7 16h14M7 23h9" /><circle
              cx="24"
              cy="23"
              r="2.5"
            /></svg>
            <svg
              v-else
              viewBox="0 0 24 24"
            ><circle
              cx="12"
              cy="8"
              r="4"
            /><path d="M5 21c.5-4 3-6 7-6s6.5 2 7 6" /></svg>
          </span>
          <span class="author">{{ roleClass(message.role) === 'assistant' ? 'Sillage' : 'Vous' }}</span>
          <time :datetime="message.timestamp">{{ formatTime(message.timestamp) }}</time>
        </div>

        <div class="message-content">
          <span
            v-if="isPending(message)"
            class="thinking"
            aria-label="Réponse en cours"
          >
            <i></i><i></i><i></i>
          </span>
          <p v-else>
            {{ message.content }}
          </p>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue';

const props = defineProps({ messages: { type: Array, required: true } });
const messagesContainer = ref(null);

const roleClass = (role) => String(role || 'assistant').toLowerCase() === 'user' ? 'user' : 'assistant';
const isPending = (message) => message.pending === true;

const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
};

const scrollToBottom = async () => {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

watch(() => props.messages, scrollToBottom, { deep: true });
defineExpose({ scrollToBottom });
</script>

<style scoped>
.messages-area { flex: 1; min-height: 0; overflow-y: auto; scroll-behavior: smooth; }
.messages-list { width: min(820px, calc(100% - 40px)); margin: 0 auto; padding: 36px 0 20px; }
.message { padding: 25px 0 28px; border-bottom: 1px solid var(--line); animation: reveal 220ms ease both; }
.message:last-child { border-bottom: 0; }
.message-meta { display: flex; align-items: center; gap: 10px; }
.avatar { width: 31px; height: 31px; display: grid; place-items: center; color: var(--ink-950); background: var(--mint); border-radius: 9px; }
.user .avatar { color: var(--paper); background: var(--ink-950); }
.avatar svg { width: 20px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.avatar circle:last-child { fill: var(--accent); stroke: none; }
.author { font-size: 11px; font-weight: 800; letter-spacing: 0.02em; }
.message-meta time { color: var(--ink-400); font-size: 9px; }
.message-content { padding: 5px 0 0 41px; }
.message-content p { margin: 0; color: var(--ink-800); font-size: 14px; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; }
.user .message-content p { color: var(--ink-950); }
.thinking { height: 24px; display: inline-flex; align-items: center; gap: 5px; }
.thinking i { width: 5px; height: 5px; background: var(--accent); border-radius: 50%; animation: pulse 1s ease-in-out infinite; }
.thinking i:nth-child(2) { animation-delay: 140ms; }
.thinking i:nth-child(3) { animation-delay: 280ms; }
.empty-chat { height: 100%; padding: 30px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.empty-icon { width: 54px; height: 54px; display: grid; place-items: center; color: var(--paper); background: var(--ink-950); border-radius: 17px; box-shadow: var(--shadow-sm); }
.empty-icon svg { width: 31px; }
.empty-icon path { fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; }
.empty-icon circle { fill: var(--accent); }
.empty-chat h2 { margin: 20px 0 8px; font-family: Georgia, serif; font-size: 24px; font-weight: 500; }
.empty-chat p { max-width: 420px; margin: 0; color: var(--ink-600); font-size: 12px; line-height: 1.7; }
@keyframes pulse { 0%, 100% { opacity: .25; transform: translateY(0); } 50% { opacity: 1; transform: translateY(-3px); } }
@keyframes reveal { from { opacity: 0; transform: translateY(7px); } }
@media (max-width: 600px) {
  .messages-list { width: calc(100% - 30px); padding-top: 18px; }
  .message { padding: 20px 0; }
  .message-content { padding-left: 0; padding-top: 12px; }
}
</style>

