<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  events: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'jump-today', 'change-view', 'select-event', 'jump-date'])

const query = ref('')
const inputRef = ref(null)
const activeIdx = ref(0)

const STATIC_COMMANDS = [
  { id: 'today',     label: '오늘로 점프',     hint: 'T',     emoji: '📍', action: () => emit('jump-today') },
  { id: 'view-week', label: '주간 뷰',         hint: 'W',     emoji: '🗓️', action: () => emit('change-view', 'week') },
  { id: 'view-month',label: '월간 뷰',         hint: 'M',     emoji: '📅', action: () => emit('change-view', 'calendar') },
  { id: 'view-tl',   label: '타임라인 뷰',     hint: '',      emoji: '⏱️', action: () => emit('change-view', 'timeline') },
  { id: 'view-tbl',  label: '테이블 뷰',       hint: '',      emoji: '📋', action: () => emit('change-view', 'table') },
  { id: 'view-ag',   label: 'Agenda 뷰',       hint: '',      emoji: '📜', action: () => emit('change-view', 'agenda') },
]

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return STATIC_COMMANDS.map(c => ({ ...c, type: 'cmd' }))
  const cmds = STATIC_COMMANDS
    .filter(c => c.label.toLowerCase().includes(q))
    .map(c => ({ ...c, type: 'cmd' }))
  const events = (props.events ?? [])
    .filter(e => (e.title ?? '').toLowerCase().includes(q))
    .slice(0, 8)
    .map(e => ({
      id: 'evt-' + e.id,
      label: e.title,
      hint: e.start ?? '',
      emoji: e.isPartnership ? '🤝' : '📣',
      type: 'evt',
      action: () => emit('select-event', e),
    }))
  return [...cmds, ...events]
})

watch(() => props.open, async (v) => {
  if (v) {
    query.value = ''
    activeIdx.value = 0
    await nextTick()
    inputRef.value?.focus()
  }
})
watch(filtered, () => { activeIdx.value = 0 })

function run(cmd) {
  cmd.action?.()
  emit('close')
}
function moveActive(delta) {
  if (!filtered.value.length) return
  activeIdx.value = (activeIdx.value + delta + filtered.value.length) % filtered.value.length
}
function onEnter() {
  const cmd = filtered.value[activeIdx.value]
  if (cmd) run(cmd)
}
function onBackdrop(e) {
  if (e.target === e.currentTarget) emit('close')
}
</script>

<template>
  <transition name="cmdk-fade">
    <div v-if="open" class="cmdk" @click="onBackdrop" @keydown.esc.prevent="emit('close')">
      <div class="cmdk__panel" role="dialog" aria-modal="true">
        <div class="cmdk__input-wrap">
          <span class="material-symbols-outlined">search</span>
          <input
            ref="inputRef"
            v-model="query"
            class="cmdk__input"
            placeholder="명령어 또는 일정 검색..."
            @keydown.down.prevent="moveActive(1)"
            @keydown.up.prevent="moveActive(-1)"
            @keydown.enter.prevent="onEnter"
            @keydown.esc.prevent="emit('close')"
          />
          <kbd class="cmdk__kbd">Esc</kbd>
        </div>
        <ul class="cmdk__list">
          <li
            v-for="(cmd, i) in filtered"
            :key="cmd.id"
            class="cmdk__item"
            :class="{ 'cmdk__item--active': i === activeIdx }"
            @click="run(cmd)"
            @mouseenter="activeIdx = i"
          >
            <span class="cmdk__emoji">{{ cmd.emoji }}</span>
            <span class="cmdk__label">{{ cmd.label }}</span>
            <span v-if="cmd.hint" class="cmdk__hint">{{ cmd.hint }}</span>
          </li>
          <li v-if="!filtered.length" class="cmdk__empty">결과 없음</li>
        </ul>
        <footer class="cmdk__foot">
          <span><kbd>↑</kbd> <kbd>↓</kbd> 이동</span>
          <span><kbd>↵</kbd> 실행</span>
          <span><kbd>Esc</kbd> 닫기</span>
        </footer>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.cmdk {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 300;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 14vh;
  padding-left: 16px;
  padding-right: 16px;
}
.cmdk__panel {
  width: 100%;
  max-width: 540px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 70vh;
}
.cmdk__input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}
.cmdk__input-wrap .material-symbols-outlined { color: var(--muted-text); font-size: 18px; }
.cmdk__input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
}
.cmdk__kbd {
  font-size: 10px;
  padding: 2px 6px;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--muted-text);
  font-family: inherit;
}

.cmdk__list { list-style: none; padding: 6px; margin: 0; overflow-y: auto; flex: 1; }
.cmdk__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-primary);
}
.cmdk__item--active { background: var(--panel-muted); }
.cmdk__emoji { font-size: 16px; flex-shrink: 0; }
.cmdk__label { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cmdk__hint { font-size: 11px; color: var(--muted-text); font-variant-numeric: tabular-nums; }
.cmdk__empty { padding: 20px; text-align: center; color: var(--muted-text); font-size: 12px; }

.cmdk__foot {
  display: flex;
  gap: 14px;
  padding: 8px 14px;
  border-top: 1px solid var(--border-color);
  font-size: 11px;
  color: var(--muted-text);
}
.cmdk__foot kbd {
  font-size: 10px;
  padding: 1px 5px;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 3px;
  font-family: inherit;
  margin-right: 2px;
}

.cmdk-fade-enter-active, .cmdk-fade-leave-active { transition: opacity 0.15s; }
.cmdk-fade-enter-from, .cmdk-fade-leave-to { opacity: 0; }
.cmdk-fade-enter-active .cmdk__panel, .cmdk-fade-leave-active .cmdk__panel {
  transition: transform 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}
.cmdk-fade-enter-from .cmdk__panel { transform: translateY(-12px) scale(0.98); }
</style>
