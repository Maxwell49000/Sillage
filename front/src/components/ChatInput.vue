<template>
  <footer class="composer-area">
    <div class="composer">
      <label
        class="sr-only"
        for="chat-message"
      >Votre message</label>
      <textarea
        id="chat-message"
        ref="textarea"
        v-model="localValue"
        rows="1"
        maxlength="4000"
        placeholder="Posez une question sur vos documents…"
        :disabled="isLoading"
        @input="resize"
        @keydown.enter.exact.prevent="send"
      ></textarea>
      <button
        type="button"
        :disabled="!localValue.trim() || isLoading"
        :aria-label="isLoading ? 'Réponse en cours' : 'Envoyer le message'"
        @click="send"
      >
        <svg
          viewBox="0 0 24 24"
          aria-hidden="true"
        ><path d="m5 12 14-7-4 14-3-6-7-1Zm7 1 7-8" /></svg>
      </button>
    </div>
    <p>Entrée pour envoyer · Maj + Entrée pour une nouvelle ligne</p>
  </footer>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue';

const props = defineProps({
  modelValue: { type: String, default: '' },
  isLoading: { type: Boolean, default: false }
});
const emit = defineEmits(['update:modelValue', 'send']);
const localValue = ref(props.modelValue);
const textarea = ref(null);

watch(() => props.modelValue, (value) => { localValue.value = value; });
watch(localValue, (value) => emit('update:modelValue', value));

const resize = () => {
  if (!textarea.value) return;
  textarea.value.style.height = 'auto';
  textarea.value.style.height = `${Math.min(textarea.value.scrollHeight, 150)}px`;
};

const send = async () => {
  if (!localValue.value.trim() || props.isLoading) return;
  emit('send');
  localValue.value = '';
  await nextTick();
  resize();
};
</script>

<style scoped>
.composer-area { padding: 12px 24px 18px; background: linear-gradient(to top, var(--surface) 75%, transparent); }
.composer { width: min(820px, 100%); margin: 0 auto; padding: 7px 7px 7px 17px; display: flex; align-items: flex-end; gap: 10px; background: white; border: 1px solid var(--line); border-radius: 17px; box-shadow: 0 13px 38px rgba(16, 42, 46, 0.09); transition: border-color 160ms ease, box-shadow 160ms ease; }
.composer:focus-within { border-color: rgba(231, 101, 79, 0.7); box-shadow: 0 13px 38px rgba(16, 42, 46, 0.09), 0 0 0 3px rgba(231, 101, 79, 0.09); }
textarea { width: 100%; max-height: 150px; padding: 9px 0; color: var(--ink-950); font-size: 13px; line-height: 1.5; resize: none; background: transparent; border: 0; outline: 0; }
textarea::placeholder { color: var(--ink-400); }
textarea:disabled { cursor: not-allowed; }
button { width: 40px; height: 40px; flex: 0 0 auto; display: grid; place-items: center; color: white; background: var(--ink-950); border: 0; border-radius: 12px; cursor: pointer; transition: background 150ms ease, transform 150ms ease, opacity 150ms ease; }
button:hover:not(:disabled) { background: var(--accent); transform: translateY(-1px); }
button:disabled { opacity: .28; cursor: not-allowed; }
button svg { width: 19px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.composer-area > p { margin: 8px auto 0; color: var(--ink-400); font-size: 9px; text-align: center; }
@media (max-width: 600px) { .composer-area { padding: 10px 12px 14px; } .composer-area > p { display: none; } }
</style>
