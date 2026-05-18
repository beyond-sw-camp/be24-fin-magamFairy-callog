<script setup>
import { computed, ref, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const props = defineProps({
  event: { type: Object, default: null },
})
const emit = defineEmits(['close'])

const router = useRouter()
const store = usePlannerStore()
const isDark = computed(() => store.theme === 'dark')

const open = computed(() => !!props.event)

const TYPE_META = {
  campaign: { label: '캠페인',     emoji: '📣', cls: 'campaign' },
  deadline: { label: '모집 마감',  emoji: '⏰', cls: 'deadline' },
  milestone:{ label: '마일스톤',   emoji: '🚩', cls: 'milestone' },
  task:     { label: '내 업무',    emoji: '✅', cls: 'task' },
}
const typeMeta = computed(() => TYPE_META[props.event?.type] ?? { label: '일정', emoji: '📅', cls: 'campaign' })

function fmtDate(s) {
  if (!s) return '-'
  const d = new Date(s)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}
function durationDays(s, e) {
  if (!s || !e) return 0
  return Math.round((new Date(e) - new Date(s)) / 86400000) + 1
}
function dDay(end) {
  if (!end) return null
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const e = new Date(end); e.setHours(0, 0, 0, 0)
  const diff = Math.round((e - today) / 86400000)
  if (diff > 0) return `D-${diff}`
  if (diff === 0) return 'D-DAY'
  return `D+${-diff}`
}

const ctaConfig = computed(() => {
  if (!props.event) return null
  const ev = props.event
  switch (ev.type) {
    case 'campaign':
      return { label: '캠페인 상세 페이지', icon: 'arrow_outward', action: () => goCampaignDetail(ev.campaignId) }
    case 'deadline':
      return { label: '캠페인 소개 / 제안서', icon: 'description', action: () => goCampaignIntro(ev.campaignId) }
    case 'milestone':
      return { label: '마일스톤 보드 열기', icon: 'view_kanban', action: () => goTeamBoard(ev.campaignId) }
    case 'task':
      return { label: '팀 보드에서 업무 보기', icon: 'task_alt', action: () => goTeamBoard(ev.campaignId) }
    default:
      return null
  }
})

function goCampaignDetail(campaignId) {
  if (!campaignId) return
  router.push({ name: 'campaign-detail', params: { campaignId } })
  emit('close')
}
function goCampaignIntro(campaignId) {
  if (!campaignId) return
  router.push({ name: 'campaign-intro', params: { campaignId } })
  emit('close')
}
function goTeamBoard(campaignId) {
  router.push({ name: 'team-board', query: campaignId ? { campaign: campaignId } : {} })
  emit('close')
}

/* ─── 외부 클릭 시 패널 닫기 ─── */
const panelRef = ref(null)

function onDocPointerDown(e) {
  if (!props.event) return
  // 패널 내부 클릭은 무시
  if (panelRef.value && panelRef.value.contains(e.target)) return
  // 캘린더 이벤트 막대 클릭은 새 이벤트로 교체 — 닫지 않음 (자체 stopPropagation 처리됨)
  // 그 외 외부 영역 → 닫기
  emit('close')
}

watch(open, (isOpen) => {
  if (isOpen) {
    // 같은 클릭 사이클에서 즉시 닫히는 것 방지 — 다음 tick에 등록
    setTimeout(() => document.addEventListener('pointerdown', onDocPointerDown), 0)
  } else {
    document.removeEventListener('pointerdown', onDocPointerDown)
  }
})

onUnmounted(() => document.removeEventListener('pointerdown', onDocPointerDown))
</script>

<template>
  <transition name="panel-slide">
    <aside v-if="open" ref="panelRef" class="event-panel" :class="{ 'event-panel--dark': isDark }">
      <header class="event-panel__head">
        <span class="event-panel__type" :class="`event-panel__type--${typeMeta.cls}`">
          {{ typeMeta.emoji }} {{ typeMeta.label }}
        </span>
        <button class="event-panel__close" @click="emit('close')" aria-label="닫기">
          <span class="material-symbols-outlined">close</span>
        </button>
      </header>

      <div class="event-panel__body">
        <h3 class="event-panel__title">{{ event.title }}</h3>

        <div class="event-panel__dday">
          <span class="event-panel__dday-chip" :class="{'event-panel__dday-chip--soon': dDay(event.end) && dDay(event.end).startsWith('D-') && parseInt(dDay(event.end).slice(2)) <= 7}">
            {{ dDay(event.end) ?? '-' }}
          </span>
          <span class="event-panel__duration">{{ durationDays(event.start, event.end) }}일간</span>
        </div>

        <dl class="event-panel__meta">
          <dt><span class="material-symbols-outlined">event</span>시작</dt>
          <dd>{{ fmtDate(event.start) }}</dd>
          <dt><span class="material-symbols-outlined">event_busy</span>마감</dt>
          <dd>{{ fmtDate(event.end) }}</dd>
          <dt v-if="event.projectManager"><span class="material-symbols-outlined">person</span>담당</dt>
          <dd v-if="event.projectManager">{{ event.projectManager }}</dd>
        </dl>

        <button v-if="ctaConfig" class="event-panel__cta" @click="ctaConfig.action">
          <span class="material-symbols-outlined">{{ ctaConfig.icon }}</span>
          {{ ctaConfig.label }}
        </button>
      </div>
    </aside>
  </transition>
</template>

<style scoped>
.event-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 360px;
  max-width: 92vw;
  background: var(--panel-color);
  border-left: 1px solid var(--border-color);
  box-shadow: -8px 0 24px rgba(0, 0, 0, 0.08);
  z-index: 200;
  display: flex;
  flex-direction: column;
}
.event-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-color);
}
.event-panel__type {
  font-size: 11px;
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}
.event-panel__type--campaign {
  background: rgba(139, 92, 246, 0.12);
  color: #7C3AED;
}
.event-panel__type--deadline {
  background: rgba(245, 158, 11, 0.12);
  color: #B45309;
}
.event-panel__type--milestone {
  background: rgba(59, 130, 246, 0.12);
  color: #1E40AF;
}
.event-panel__type--task {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}
.event-panel__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.event-panel__close:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}
.event-panel__close .material-symbols-outlined { font-size: 18px; }

.event-panel__body {
  flex: 1;
  padding: 20px 22px;
  overflow-y: auto;
}
.event-panel__title {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: 1.35;
  margin: 0 0 14px;
}
.event-panel__dday {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 22px;
}
.event-panel__dday-chip {
  font-size: 14px;
  font-weight: 800;
  padding: 4px 12px;
  border-radius: 6px;
  background: var(--panel-muted);
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.event-panel__dday-chip--soon {
  background: rgba(245, 158, 11, 0.15);
  color: #B45309;
}
.event-panel__duration {
  font-size: 12px;
  color: var(--muted-text);
}

.event-panel__meta {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px 14px;
  margin: 0 0 24px;
  padding: 14px 16px;
  background: var(--panel-muted);
  border-radius: 10px;
  border: 1px solid var(--border-color);
}
.event-panel__meta dt {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--muted-text);
}
.event-panel__meta dt .material-symbols-outlined {
  font-size: 15px;
  color: var(--subtle-text);
}
.event-panel__meta dd {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.event-panel__cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 11px 16px;
  background: var(--accent-color, #8B5CF6);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}
.event-panel__cta:hover { background: #7C3AED; transform: translateY(-1px); }
.event-panel__cta .material-symbols-outlined { font-size: 16px; }

/* Slide-in animation */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>
