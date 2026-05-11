<script setup>
import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()

const ICONS = {
  success: 'check_circle',
  error:   'error',
  warn:    'warning',
  info:    'info',
}
</script>

<template>
  <div class="toast-stack" aria-live="polite" aria-atomic="true">
    <transition-group name="toast">
      <div
        v-for="t in toastStore.toasts"
        :key="t.id"
        class="toast"
        :class="`toast--${t.type}`"
        role="status"
      >
        <span class="toast__icon material-symbols-outlined">{{ ICONS[t.type] || 'info' }}</span>
        <div class="toast__body">
          <div v-if="t.title" class="toast__title">{{ t.title }}</div>
          <div class="toast__msg">{{ t.message }}</div>
        </div>
        <button class="toast__close" @click="toastStore.dismiss(t.id)" aria-label="닫기">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 400;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: calc(100vw - 32px);
}
.toast {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 280px;
  max-width: 380px;
  padding: 12px 14px;
  background: var(--panel-color, #fff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-left-width: 4px;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  font-size: 13px;
}
.toast--success { border-left-color: #10B981; }
.toast--success .toast__icon { color: #10B981; }
.toast--error   { border-left-color: #EF4444; }
.toast--error   .toast__icon { color: #EF4444; }
.toast--warn    { border-left-color: #F59E0B; }
.toast--warn    .toast__icon { color: #F59E0B; }
.toast--info    { border-left-color: #3B82F6; }
.toast--info    .toast__icon { color: #3B82F6; }

.toast__icon { font-size: 20px; flex-shrink: 0; margin-top: 1px; }
.toast__body { flex: 1; min-width: 0; }
.toast__title { font-weight: 700; color: var(--text-primary, #111827); margin-bottom: 2px; }
.toast__msg { color: var(--text-secondary, #374151); line-height: 1.45; word-break: keep-all; }

.toast__close {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  color: var(--muted-text, #9CA3AF);
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.toast__close:hover { background: var(--panel-muted, #f3f4f6); color: var(--text-primary, #111827); }
.toast__close .material-symbols-outlined { font-size: 16px; }

.toast-enter-active, .toast-leave-active { transition: all 0.22s cubic-bezier(0.16, 1, 0.3, 1); }
.toast-enter-from { opacity: 0; transform: translateX(40px); }
.toast-leave-to   { opacity: 0; transform: translateX(40px); }
.toast-move { transition: transform 0.22s; }
</style>
