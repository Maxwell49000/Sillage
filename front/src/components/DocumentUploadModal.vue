<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="overlay"
      @click.self="close"
    >
      <section
        class="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="upload-title"
      >
        <header>
          <div>
            <p class="eyebrow">
              Base documentaire
            </p>
            <h2 id="upload-title">
              Ajouter des sources
            </h2>
          </div>
          <button
            class="icon-button"
            type="button"
            aria-label="Fermer"
            @click="close"
          >
            <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
            ><path d="m6 6 12 12M18 6 6 18" /></svg>
          </button>
        </header>

        <p class="scope-note">
          <span class="scope-dot"></span>
          {{ currentConversationId ? 'Ces sources seront liées à la conversation active.' : 'Ces sources seront disponibles dans toutes les conversations.' }}
        </p>

        <label
          :class="['drop-zone', { dragging: isDragging, disabled: uploading }]"
          @dragover.prevent="isDragging = true"
          @dragleave.prevent="isDragging = false"
          @drop.prevent="handleDrop"
        >
          <input
            type="file"
            multiple
            accept=".pdf,.doc,.docx,.txt"
            :disabled="uploading"
            @change="handleFileSelect"
          >
          <span
            class="upload-mark"
            aria-hidden="true"
          >
            <svg viewBox="0 0 24 24"><path d="M12 16V4m-4 4 4-4 4 4M5 14v5h14v-5" /></svg>
          </span>
          <strong>Déposez vos documents ici</strong>
          <span>ou cliquez pour parcourir vos fichiers</span>
          <small>PDF, DOC, DOCX ou TXT · 50 Mo maximum</small>
        </label>

        <div
          v-if="uploading"
          class="progress"
          aria-live="polite"
        >
          <div><span :style="{ width: `${uploadProgress}%` }"></span></div>
          <p>Indexation en cours… <strong>{{ uploadProgress }}%</strong></p>
        </div>

        <p
          v-if="uploadError"
          class="feedback error"
          role="alert"
        >
          {{ uploadError }}
        </p>
        <p
          v-if="successMessage"
          class="feedback success"
          role="status"
        >
          {{ successMessage }}
        </p>

        <div
          v-if="recentUploads.length"
          class="recent"
        >
          <h3>Ajoutés récemment</h3>
          <article
            v-for="document in recentUploads"
            :key="document.documentId"
          >
            <span class="file-badge">{{ fileExtension(document.fileName) }}</span>
            <span class="file-copy">
              <strong>{{ document.fileName }}</strong>
              <small>{{ formatFileSize(document.fileSize) }} · {{ document.numberOfChunks }} fragments</small>
            </span>
            <button
              type="button"
              :aria-label="`Supprimer ${document.fileName}`"
              @click="deleteDocument(document)"
            >
              <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
              ><path d="M4 7h16M9 7V4h6v3m3 0-1 13H7L6 7m4 4v5m4-5v5" /></svg>
            </button>
          </article>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useApiService } from '@/api/service';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  currentConversationId: { type: String, default: null }
});
const emit = defineEmits(['update:modelValue']);
const apiService = useApiService();
const uploading = ref(false);
const uploadProgress = ref(0);
const uploadError = ref('');
const successMessage = ref('');
const isDragging = ref(false);
const recentUploads = ref([]);

const close = () => {
  if (!uploading.value) emit('update:modelValue', false);
};

const handleFileSelect = (event) => {
  uploadFiles(Array.from(event.target.files || []));
  event.target.value = '';
};

const handleDrop = (event) => {
  isDragging.value = false;
  if (!uploading.value) uploadFiles(Array.from(event.dataTransfer.files || []));
};

const isValidFile = (file) => {
  const allowedExtensions = ['pdf', 'doc', 'docx', 'txt'];
  const extension = file.name.split('.').pop()?.toLowerCase();
  if (!allowedExtensions.includes(extension)) return `Le format de « ${file.name} » n’est pas accepté.`;
  if (file.size > 50 * 1024 * 1024) return `« ${file.name} » dépasse la limite de 50 Mo.`;
  if (file.size === 0) return `« ${file.name} » est vide.`;
  return '';
};

const uploadFiles = async (files) => {
  if (!files.length) return;
  uploadError.value = '';
  successMessage.value = '';

  for (const file of files) {
    const validationError = isValidFile(file);
    if (validationError) {
      uploadError.value = validationError;
      return;
    }
  }

  uploading.value = true;
  uploadProgress.value = 4;
  let successCount = 0;

  for (const [index, file] of files.entries()) {
    try {
      const formData = new FormData();
      if (props.currentConversationId) formData.append('conversationId', props.currentConversationId);
      formData.append('file', file);
      const response = await apiService.uploadDocument(formData);
      recentUploads.value.unshift(response);
      recentUploads.value = recentUploads.value.slice(0, 5);
      successCount += 1;
      uploadProgress.value = Math.round(((index + 1) / files.length) * 100);
    } catch (error) {
      uploadError.value = error.response?.data?.message || `L’indexation de « ${file.name} » a échoué.`;
    }
  }

  uploading.value = false;
  if (successCount) successMessage.value = `${successCount} source${successCount > 1 ? 's ont' : ' a'} été indexée${successCount > 1 ? 's' : ''}.`;
};

const deleteDocument = async (document) => {
  if (!window.confirm(`Supprimer « ${document.fileName} » de la base documentaire ?`)) return;
  try {
    await apiService.deleteDocument(document.documentId);
    recentUploads.value = recentUploads.value.filter(({ documentId }) => documentId !== document.documentId);
    successMessage.value = 'La source a été supprimée.';
  } catch (error) {
    uploadError.value = error.response?.data?.message || 'La suppression a échoué.';
  }
};

const formatFileSize = (bytes = 0) => {
  if (bytes < 1024) return `${bytes} o`;
  const units = ['Ko', 'Mo', 'Go'];
  const unitIndex = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)) - 1, units.length - 1);
  return `${(bytes / (1024 ** (unitIndex + 1))).toLocaleString('fr-FR', { maximumFractionDigits: 1 })} ${units[unitIndex]}`;
};

const fileExtension = (name = '') => name.split('.').pop()?.toUpperCase() || 'DOC';
const handleEscape = (event) => { if (event.key === 'Escape' && props.modelValue) close(); };
onMounted(() => window.addEventListener('keydown', handleEscape));
onBeforeUnmount(() => window.removeEventListener('keydown', handleEscape));
</script>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 100; padding: 20px; display: grid; place-items: center; background: rgba(10, 29, 33, 0.7); backdrop-filter: blur(7px); animation: fade 160ms ease both; }
.dialog { width: min(610px, 100%); max-height: min(760px, calc(100vh - 40px)); padding: 28px; overflow-y: auto; background: var(--surface); border: 1px solid rgba(255, 255, 255, 0.6); border-radius: 23px; box-shadow: var(--shadow-lg); animation: rise 190ms ease both; }
header { display: flex; align-items: flex-start; justify-content: space-between; }
.eyebrow { margin: 0 0 4px; color: var(--accent); font-size: 9px; font-weight: 850; text-transform: uppercase; letter-spacing: .14em; }
h2 { margin: 0; font-family: Georgia, serif; font-size: 29px; font-weight: 500; letter-spacing: -.02em; }
.icon-button { width: 37px; height: 37px; display: grid; place-items: center; color: var(--ink-600); background: var(--surface-muted); border: 0; border-radius: 11px; cursor: pointer; }
.icon-button svg { width: 18px; fill: none; stroke: currentColor; stroke-width: 1.8; }
.scope-note { margin: 19px 0; padding: 11px 13px; display: flex; align-items: center; gap: 9px; color: var(--ink-600); font-size: 10px; background: rgba(185, 216, 204, .22); border-radius: 10px; }
.scope-dot { width: 7px; height: 7px; background: #5a9b81; border-radius: 50%; }
.drop-zone { min-height: 220px; padding: 26px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--ink-600); text-align: center; background: rgba(239, 237, 230, .42); border: 1.5px dashed #c7c6be; border-radius: 17px; cursor: pointer; transition: border-color 150ms ease, background 150ms ease, transform 150ms ease; }
.drop-zone:hover, .drop-zone.dragging { background: rgba(185, 216, 204, .18); border-color: var(--ink-800); transform: translateY(-1px); }
.drop-zone.disabled { opacity: .55; cursor: wait; }
.drop-zone input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.upload-mark { width: 48px; height: 48px; margin-bottom: 14px; display: grid; place-items: center; color: white; background: var(--ink-950); border-radius: 14px; }
.upload-mark svg { width: 23px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.drop-zone strong { color: var(--ink-950); font-family: Georgia, serif; font-size: 17px; font-weight: 500; }
.drop-zone > span:not(.upload-mark) { margin-top: 5px; font-size: 11px; }
.drop-zone small { margin-top: 17px; color: var(--ink-400); font-size: 9px; }
.progress { margin-top: 16px; }
.progress > div { height: 5px; overflow: hidden; background: var(--surface-muted); border-radius: 99px; }
.progress > div span { height: 100%; display: block; background: var(--accent); border-radius: inherit; transition: width 250ms ease; }
.progress p { margin: 7px 0 0; display: flex; justify-content: space-between; color: var(--ink-600); font-size: 10px; }
.feedback { margin: 14px 0 0; padding: 11px 13px; font-size: 10px; border-radius: 10px; }
.feedback.error { color: #8c2d2d; background: #f7dfdc; }
.feedback.success { color: #285f4b; background: #dfeee7; }
.recent { margin-top: 23px; }
.recent h3 { margin: 0 0 9px; color: var(--ink-600); font-size: 9px; text-transform: uppercase; letter-spacing: .12em; }
.recent article { padding: 10px 5px; display: flex; align-items: center; gap: 11px; border-top: 1px solid var(--line); }
.file-badge { width: 38px; height: 38px; display: grid; place-items: center; color: var(--accent-dark); font-size: 8px; font-weight: 850; background: rgba(231, 101, 79, .1); border-radius: 10px; }
.file-copy { min-width: 0; flex: 1; }
.file-copy strong, .file-copy small { display: block; }
.file-copy strong { overflow: hidden; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.file-copy small { margin-top: 3px; color: var(--ink-400); font-size: 9px; }
.recent article button { width: 32px; height: 32px; display: grid; place-items: center; color: var(--ink-400); background: transparent; border: 0; border-radius: 8px; cursor: pointer; }
.recent article button:hover { color: var(--danger); background: #f7dfdc; }
.recent article button svg { width: 16px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
@keyframes fade { from { opacity: 0; } }
@keyframes rise { from { opacity: 0; transform: translateY(12px) scale(.985); } }
@media (max-width: 560px) { .overlay { padding: 0; align-items: end; } .dialog { max-height: 92vh; padding: 22px 18px; border-radius: 22px 22px 0 0; } }
</style>
