<script setup>
import { computed, ref, watch, nextTick } from 'vue'

const props = defineProps({
  date: { type: String, default: '' },
  events: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'event-click'])

const open = computed(() => !!props.date)

function fmtDate(s) {
  if (!s) return ''
  const d = new Date(s)
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일 (${['일','월','화','수','목','금','토'][d.getDay()]})`
}
function fmtRange(s, e) {
  if (!s) return ''
  const f = (d) => {
    const dt = new Date(d)
    return `${dt.getMonth() + 1}.${String(dt.getDate()).padStart(2, '0')}`
  }
  return s === e || !e ? f(s) : `${f(s)} ~ ${f(e)}`
}
function defaultColor(type) {
  return {
    deadline: '#F59E0B',
    milestone: '#3B82F6',
    task: '#10B981',
  }[type] ?? '#8B5CF6'
}

/* ─── 모달 드래그 이동 ─── */
const panelRef = ref(null)
const pos = ref({ x: 0, y: 0 })  // translate offset
const startMouse = ref({ x: 0, y: 0 })
const startPos = ref({ x: 0, y: 0 })
const isDragging = ref(false)

watch(() => props.date, async (d) => {
  if (d) {
    pos.value = { x: 0, y: 0 }
    await nextTick()
  }
})

function startDrag(e) {
  // 내부 버튼 / 리스트 항목 클릭은 드래그로 안 잡히게
  if (e.target.closest('.day-modal__item') || e.target.closest('button')) return
  isDragging.value = true
  startMouse.value = { x: e.clientX, y: e.clientY }
  startPos.value = { ...pos.value }
  window.addEventListener('pointermove', onDrag)
  window.addEventListener('pointerup', endDrag, { once: true })
  e.preventDefault()
}
function onDrag(e) {
  if (!isDragging.value) return
  pos.value = {
    x: startPos.value.x + (e.clientX - startMouse.value.x),
    y: startPos.value.y + (e.clientY - startMouse.value.y),
  }
}
function endDrag() {
  isDragging.value = false
  window.removeEventListener('pointermove', onDrag)
}
</script>

<template>
  <transition name="modal-fade">
    <div v-if="open" class="day-modal-host">
      <!-- 백드롭은 클릭 시 닫힘 (드래그 가능한 모달이므로 백드롭 dim 약하게) -->
      <div class="day-modal__backdrop" @click="emit('close')"></div>
      <div
        ref="panelRef"
        class="day-modal__panel"
        :class="{ 'day-modal__panel--dragging': isDragging }"
        :style="{ transform: `translate(calc(-50% + ${pos.x}px), calc(-50% + ${pos.y}px))` }"
        role="dialog"
        aria-modal="true"
        @pointerdown="startDrag"
      >
        <header class="day-modal__head">
          <div class="day-modal__head-text">
            <h3>{{ fmtDate(date) }}</h3>
            <div class="day-modal__count">{{ events.length }}개 일정</div>
          </div>
          <button class="day-modal__close" @click="emit('close')" aria-label="닫기">
            <span class="material-symbols-outlined">close</span>
          </button>
        </header>
        <div class="day-modal__body">
          <ul class="day-modal__list">
            <li
              v-for="ev in events"
              :key="ev.id"
              class="day-modal__item"
              :style="{
                background: `color-mix(in srgb, ${ev.customColor || defaultColor(ev.type)} 10%, transparent)`,
                borderLeft: `3px solid ${ev.customColor || defaultColor(ev.type)}`,
              }"
              @click="emit('event-click', ev); emit('close')"
            >
              <div class="day-modal__item-title">
                <span v-if="ev.icon" class="day-modal__item-icon">{{ ev.icon }}</span>
                {{ ev.title }}
              </div>
              <div class="day-modal__item-meta">
                <span v-if="ev.projectManager" class="day-modal__item-mgr">{{ ev.projectManager }}</span>
                <span class="day-modal__item-range">{{ fmtRange(ev.start, ev.end) }}</span>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.day-modal-host {
  position: fixed;
  inset: 0;
  z-index: 250;
  pointer-events: none;
}
.day-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.18);
  pointer-events: auto;
}
.day-modal__panel {
  position: fixed;
  top: 50%;
  left: 50%;
  width: 100%;
  max-width: 520px;
  max-height: 78vh;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: grab;
  user-select: none;
  pointer-events: auto;
  transition: box-shadow 0.15s;
}
.day-modal__panel--dragging {
  cursor: grabbing;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.42);
}
.day-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 14px;
  border-bottom: 1px solid var(--border-color);
}
.day-modal__head-text { display: flex; flex-direction: column; gap: 2px; }
.day-modal__head h3 {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.01em;
}
.day-modal__count {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.day-modal__close {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.day-modal__close:hover { background: var(--panel-muted); color: var(--text-primary); }
.day-modal__close .material-symbols-outlined { font-size: 18px; }

.day-modal__body { padding: 14px 18px 18px; overflow-y: auto; cursor: default; }
.day-modal__list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
.day-modal__item {
  padding: 14px 16px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: transform 0.1s, box-shadow 0.15s;
}
.day-modal__item:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
.day-modal__item-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.day-modal__item-icon { font-size: 16px; }
.day-modal__item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  color: var(--muted-text);
}
.day-modal__item-mgr {
  font-weight: 700;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.day-modal__item-range {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  flex-shrink: 0;
}

.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.15s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-active .day-modal__panel, .modal-fade-leave-active .day-modal__panel {
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.modal-fade-enter-from .day-modal__panel { transform: translate(-50%, -50%) scale(0.94); }
</style>
