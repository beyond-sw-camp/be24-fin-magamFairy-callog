<script setup>
import { useConfirmStore } from '@/stores/confirmDialog'

const cd = useConfirmStore()

function onBackdrop(e) {
  if (e.target === e.currentTarget) cd.close(false)
}
function onKey(e) {
  if (e.key === 'Escape') cd.close(false)
  else if (e.key === 'Enter') cd.close(true)
}
</script>

<template>
  <transition name="cd-fade">
    <div
      v-if="cd.open"
      class="cd"
      role="alertdialog"
      aria-modal="true"
      tabindex="-1"
      @click="onBackdrop"
      @keydown="onKey"
    >
      <div class="cd__panel" :class="`cd__panel--${cd.tone}`">
        <header class="cd__head">
          <span class="cd__icon material-symbols-outlined">
            {{ cd.tone === 'danger' ? 'warning' : cd.tone === 'warn' ? 'help' : 'check_circle' }}
          </span>
          <h3>{{ cd.title }}</h3>
        </header>
        <p class="cd__msg">{{ cd.message }}</p>
        <footer class="cd__foot">
          <button class="cd__btn cd__btn--ghost" @click="cd.close(false)" autofocus>
            {{ cd.cancelText }}
          </button>
          <button
            class="cd__btn cd__btn--primary"
            :class="{ 'cd__btn--danger': cd.tone === 'danger' }"
            @click="cd.close(true)"
          >
            {{ cd.confirmText }}
          </button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.cd {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 350;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.cd__panel {
  width: 100%;
  max-width: 380px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.cd__head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.cd__head h3 {
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.01em;
}
.cd__icon { font-size: 22px; color: var(--accent-color, #8B5CF6); }
.cd__panel--danger .cd__icon { color: #EF4444; }
.cd__panel--warn .cd__icon { color: #F59E0B; }
.cd__msg {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
  word-break: keep-all;
  white-space: pre-line;
}
.cd__foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
}
.cd__btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: background 0.15s;
}
.cd__btn--ghost {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}
.cd__btn--ghost:hover { background: var(--panel-muted); color: var(--text-primary); }
.cd__btn--primary {
  background: var(--accent-color, #8B5CF6);
  color: #fff;
}
.cd__btn--primary:hover { background: #7C3AED; }
.cd__btn--danger {
  background: #EF4444;
}
.cd__btn--danger:hover { background: #DC2626; }

.cd-fade-enter-active, .cd-fade-leave-active { transition: opacity 0.15s; }
.cd-fade-enter-from, .cd-fade-leave-to { opacity: 0; }
.cd-fade-enter-active .cd__panel, .cd-fade-leave-active .cd__panel {
  transition: transform 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}
.cd-fade-enter-from .cd__panel { transform: scale(0.94); }
</style>
