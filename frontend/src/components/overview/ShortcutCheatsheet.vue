<script setup>
defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const SHORTCUTS = [
  { keys: ['T'],         label: '오늘로 점프' },
  { keys: ['J'],         label: '다음 (다음 달/주)' },
  { keys: ['K'],         label: '이전 (지난 달/주)' },
  { keys: ['M'],         label: '월간 뷰' },
  { keys: ['W'],         label: '주간 뷰' },
  { keys: ['A'],         label: 'Agenda 뷰' },
  { keys: ['/'],         label: '검색 포커스' },
  { keys: ['Esc'],       label: '패널/모달 닫기' },
  { keys: ['⌘', 'K'],    label: '명령 팔레트' },
  { keys: ['?'],         label: '이 단축키 모달' },
]

function onBackdrop(e) {
  if (e.target === e.currentTarget) emit('close')
}
</script>

<template>
  <transition name="cheat-fade">
    <div v-if="open" class="cheat" @click="onBackdrop" @keydown.esc.prevent="emit('close')">
      <div class="cheat__panel" role="dialog" aria-modal="true">
        <header class="cheat__head">
          <h3>⌨️ 키보드 단축키</h3>
          <button class="cheat__close" @click="emit('close')" aria-label="닫기">
            <span class="material-symbols-outlined">close</span>
          </button>
        </header>
        <ul class="cheat__list">
          <li v-for="(s, i) in SHORTCUTS" :key="i" class="cheat__item">
            <span class="cheat__label">{{ s.label }}</span>
            <span class="cheat__keys">
              <kbd v-for="k in s.keys" :key="k">{{ k }}</kbd>
            </span>
          </li>
        </ul>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.cheat {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 280;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.cheat__panel {
  width: 100%;
  max-width: 380px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}
.cheat__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-color);
}
.cheat__head h3 { font-size: 14px; font-weight: 800; color: var(--text-primary); margin: 0; }
.cheat__close {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
}
.cheat__close:hover { background: var(--panel-muted); color: var(--text-primary); }
.cheat__close .material-symbols-outlined { font-size: 16px; }

.cheat__list { list-style: none; padding: 8px 16px 14px; margin: 0; }
.cheat__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border-color);
}
.cheat__item:last-child { border-bottom: none; }
.cheat__label { font-size: 12.5px; color: var(--text-primary); }
.cheat__keys { display: flex; gap: 3px; }
kbd {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  min-width: 22px;
  text-align: center;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-family: inherit;
  color: var(--text-primary);
}

.cheat-fade-enter-active, .cheat-fade-leave-active { transition: opacity 0.15s; }
.cheat-fade-enter-from, .cheat-fade-leave-to { opacity: 0; }
.cheat-fade-enter-active .cheat__panel, .cheat-fade-leave-active .cheat__panel {
  transition: transform 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}
.cheat-fade-enter-from .cheat__panel { transform: scale(0.94); }
</style>
