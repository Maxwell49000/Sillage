<template>
  <section
    class="welcome"
    aria-labelledby="welcome-title"
  >
    <div
      class="grain"
      aria-hidden="true"
    ></div>
    <div class="welcome-copy">
      <p class="eyebrow">
        <span></span> IA locale · RAG documentaire
      </p>
      <h1 id="welcome-title">
        Vos documents ont<br><em>des choses à dire.</em>
      </h1>
      <p class="lead">
        Importez vos sources, posez vos questions et obtenez des réponses contextualisées — sans envoyer vos données dans le cloud.
      </p>
      <div class="actions">
        <button
          class="primary-action"
          type="button"
          @click="emit('start-chat')"
        >
          Commencer
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
          ><path d="M5 12h14m-5-5 5 5-5 5" /></svg>
        </button>
        <span class="privacy-note">
          <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
          ><path d="M7 10V8a5 5 0 0 1 10 0v2m-11 0h12v10H6V10Z" /></svg>
          Propulsé localement par Ollama
        </span>
      </div>
    </div>

    <div
      class="showcase"
      aria-label="Aperçu du fonctionnement"
    >
      <div class="document-card card-back">
        <span class="file-icon">PDF</span>
        <span><strong>rapport_cda.pdf</strong><small>12 pages · indexé</small></span>
        <span class="check">✓</span>
      </div>
      <div class="document-card card-front">
        <div class="question">
          <span>Vous</span>
          <p>Quels étaient les objectifs du projet&nbsp;?</p>
        </div>
        <div class="answer">
          <span
            class="answer-mark"
            aria-hidden="true"
          >
            <svg viewBox="0 0 32 32"><path d="M7 9h18M7 16h14M7 23h9" /><circle
              cx="24"
              cy="23"
              r="2.5"
            /></svg>
          </span>
          <div>
            <span>Sillage</span>
            <p>Le projet visait à expérimenter une architecture RAG locale avec Spring AI et pgvector.</p>
            <small><i></i> Source · rapport_cda.pdf, p. 3</small>
          </div>
        </div>
      </div>
      <span
        class="decor decor-one"
        aria-hidden="true"
      ></span>
      <span
        class="decor decor-two"
        aria-hidden="true"
      ></span>
    </div>

    <div
      class="capabilities"
      aria-label="Fonctionnalités principales"
    >
      <article>
        <span>01</span>
        <div><strong>Importez</strong><p>PDF, Word ou texte</p></div>
      </article>
      <article>
        <span>02</span>
        <div><strong>Questionnez</strong><p>En langage naturel</p></div>
      </article>
      <article>
        <span>03</span>
        <div><strong>Comprenez</strong><p>Grâce au contexte retrouvé</p></div>
      </article>
    </div>
  </section>
</template>

<script setup>
const emit = defineEmits(['start-chat']);
</script>

<style scoped>
.welcome {
  position: relative;
  height: 100%;
  padding: clamp(38px, 6vw, 88px);
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(340px, 0.85fr);
  grid-template-rows: 1fr auto;
  align-items: center;
  gap: 40px 8vw;
  overflow: auto;
  isolation: isolate;
}

.grain { position: absolute; inset: 0; z-index: -2; opacity: 0.32; background-image: radial-gradient(rgba(16, 42, 46, 0.14) 0.5px, transparent 0.5px); background-size: 6px 6px; mask-image: linear-gradient(to right, black, transparent 70%); }
.welcome::after { content: ''; position: absolute; z-index: -1; top: 8%; right: -8%; width: 48vw; aspect-ratio: 1; background: rgba(185, 216, 204, 0.25); border-radius: 50%; filter: blur(2px); }

.eyebrow { margin: 0 0 22px; display: flex; align-items: center; gap: 10px; color: var(--ink-600); font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.14em; }
.eyebrow span { width: 32px; height: 2px; background: var(--accent); }
h1 { margin: 0; color: var(--ink-950); font-family: Georgia, 'Times New Roman', serif; font-size: clamp(48px, 5.6vw, 82px); font-weight: 500; line-height: 0.98; letter-spacing: -0.055em; }
h1 em { color: var(--accent); font-weight: 400; }
.lead { max-width: 600px; margin: 30px 0; color: var(--ink-600); font-size: clamp(15px, 1.4vw, 18px); line-height: 1.75; }
.actions { display: flex; align-items: center; flex-wrap: wrap; gap: 20px; }
.primary-action { padding: 14px 19px 14px 23px; display: flex; align-items: center; gap: 30px; color: white; font-size: 13px; font-weight: 750; background: var(--ink-950); border: 0; border-radius: 13px; cursor: pointer; box-shadow: var(--shadow-sm); transition: transform 160ms ease, background 160ms ease; }
.primary-action:hover { background: var(--ink-800); transform: translateY(-2px); }
.primary-action svg { width: 18px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.privacy-note { display: flex; align-items: center; gap: 7px; color: var(--ink-600); font-size: 10px; font-weight: 650; }
.privacy-note svg { width: 15px; fill: none; stroke: currentColor; stroke-width: 1.6; }

.showcase { position: relative; min-height: 430px; display: grid; place-items: center; }
.document-card { width: min(420px, 100%); background: rgba(255, 253, 248, 0.92); border: 1px solid rgba(16, 42, 46, 0.1); border-radius: 22px; box-shadow: var(--shadow-lg); backdrop-filter: blur(12px); }
.card-back { position: absolute; top: 12%; right: 1%; padding: 17px 19px; display: flex; align-items: center; gap: 12px; transform: rotate(4deg); }
.file-icon { width: 42px; height: 42px; display: grid; place-items: center; color: var(--accent-dark); font-size: 9px; font-weight: 900; background: rgba(231, 101, 79, 0.1); border-radius: 11px; }
.card-back strong, .card-back small { display: block; }
.card-back strong { font-size: 12px; }
.card-back small { margin-top: 3px; color: var(--ink-400); font-size: 9px; }
.check { margin-left: auto; width: 24px; height: 24px; display: grid; place-items: center; color: var(--ink-950); font-size: 11px; background: var(--mint); border-radius: 50%; }
.card-front { position: relative; z-index: 2; margin-top: 65px; overflow: hidden; transform: rotate(-2deg); }
.question { padding: 24px 25px 20px; background: var(--surface-muted); }
.question span, .answer > div > span { color: var(--ink-400); font-size: 9px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.12em; }
.question p { margin: 8px 0 0; font-family: Georgia, serif; font-size: 19px; line-height: 1.35; }
.answer { padding: 25px; display: flex; gap: 14px; }
.answer-mark { width: 35px; height: 35px; flex: 0 0 auto; display: grid; place-items: center; color: white; background: var(--ink-950); border-radius: 10px; }
.answer-mark svg { width: 23px; }
.answer-mark path { fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; }
.answer-mark circle { fill: var(--accent); }
.answer p { margin: 9px 0 17px; color: var(--ink-800); font-size: 12px; line-height: 1.7; }
.answer small { color: var(--ink-600); font-size: 9px; font-weight: 650; }
.answer i { width: 6px; height: 6px; display: inline-block; margin-right: 5px; background: #63a489; border-radius: 50%; }
.decor { position: absolute; border: 1px solid rgba(16, 42, 46, 0.14); border-radius: 50%; }
.decor-one { width: 76px; height: 76px; left: 0; bottom: 12%; }
.decor-two { width: 18px; height: 18px; right: 5%; bottom: 4%; background: var(--accent); border: 0; }

.capabilities { grid-column: 1 / -1; display: grid; grid-template-columns: repeat(3, 1fr); border-top: 1px solid var(--line); }
.capabilities article { padding: 24px 7%; display: flex; gap: 16px; border-right: 1px solid var(--line); }
.capabilities article:last-child { border-right: 0; }
.capabilities article > span { color: var(--accent); font-family: Georgia, serif; font-size: 12px; font-style: italic; }
.capabilities strong { display: block; font-family: Georgia, serif; font-size: 15px; }
.capabilities p { margin: 4px 0 0; color: var(--ink-400); font-size: 10px; }

@media (max-width: 1050px) {
  .welcome { grid-template-columns: 1fr; grid-template-rows: auto auto auto; }
  .welcome-copy { padding-top: 20px; }
  .showcase { min-height: 360px; }
  .document-card { width: min(470px, 88%); }
}

@media (max-width: 620px) {
  .welcome { padding: 34px 22px; gap: 32px; }
  h1 { font-size: clamp(43px, 14vw, 62px); }
  .showcase { min-height: 330px; }
  .card-back { top: 5%; }
  .capabilities { grid-template-columns: 1fr; }
  .capabilities article { padding: 16px 4px; border-right: 0; border-bottom: 1px solid var(--line); }
}
</style>
