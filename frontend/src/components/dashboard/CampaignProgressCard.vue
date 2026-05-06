<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  campaign: { type: Object, required: true },
  // 현재 활성 모드: "progress" | "time" | "task"
  mode: { type: String, default: 'progress' },
  // 태스크 통계 (외부에서 계산해서 주입) — { done: number, total: number }
  taskStats: { type: Object, default: () => ({ done: 0, total: 0 }) },
})

const router = useRouter()

function dDay(endDate) {
  if (!endDate) return null
  const target = new Date(endDate)
  if (Number.isNaN(target.getTime())) return null
  const ms = target - new Date()
  return Math.ceil(ms / 86400000)
}

const dDayValue = computed(() => dDay(props.campaign.endDate))
const dDayLabel = computed(() => {
  const d = dDayValue.value
  if (d == null) return '미정'
  if (d === 0) return '오늘 마감'
  return d > 0 ? `D-${d}` : `D+${-d}`
})

function timeProgress(start, end) {
  if (!start || !end) return 0
  const s = new Date(start).getTime()
  const e = new Date(end).getTime()
  if (Number.isNaN(s) || Number.isNaN(e) || e <= s) return 0
  const elapsed = Date.now() - s
  return Math.max(0, Math.min(100, Math.round((elapsed / (e - s)) * 100)))
}

const taskProgress = computed(() => {
  const { done = 0, total = 0 } = props.taskStats || {}
  if (total <= 0) return 0
  return Math.round((done / total) * 100)
})

// D 옵션: 태스크 데이터 있으면 우선, 없으면 시간 기반
const hybridProgress = computed(() => {
  const total = props.taskStats?.total ?? 0
  if (total > 0) return taskProgress.value
  return timeProgress(props.campaign.startDate, props.campaign.endDate)
})

const progressBasis = computed(() => {
  const total = props.taskStats?.total ?? 0
  return total > 0 ? '태스크 기준' : '시간 기준'
})

// 모드별 표시
const display = computed(() => {
  if (props.mode === 'time') {
    return { primary: dDayLabel.value, bar: timeProgress(props.campaign.startDate, props.campaign.endDate), sub: formatRange(props.campaign.startDate, props.campaign.endDate) }
  }
  if (props.mode === 'task') {
    const { done = 0, total = 0 } = props.taskStats || {}
    return { primary: total > 0 ? `${done}/${total}` : '0/0', bar: taskProgress.value, sub: total > 0 ? `${total - done}건 진행 중` : '태스크 없음' }
  }
  // progress (default)
  return { primary: `${hybridProgress.value}%`, bar: hybridProgress.value, sub: progressBasis.value }
})

function formatRange(s, e) {
  return `${formatDate(s)} ~ ${formatDate(e)}`
}
function formatDate(d) {
  if (!d) return '미정'
  const dt = new Date(d)
  if (Number.isNaN(dt.getTime())) return '미정'
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const day = String(dt.getDate()).padStart(2, '0')
  return `${dt.getFullYear()}.${m}.${day}`
}

const partnerCount = computed(() => (props.campaign.partners ?? []).length)

function goToDetail() {
  router.push({ name: 'campaign-detail', params: { campaignId: props.campaign.id ?? props.campaign.idx } })
}
</script>

<template>
  <article class="cp-card" tabindex="0" role="link" @click="goToDetail" @keydown.enter="goToDetail">
    <header class="cp-card__head">
      <h4 class="cp-card__title">{{ campaign.name }}</h4>
      <strong class="cp-card__primary">{{ display.primary }}</strong>
    </header>
    <div class="cp-card__track">
      <i class="cp-card__fill" :style="{ width: `${Math.max(2, display.bar)}%` }" />
    </div>
    <footer class="cp-card__meta">
      <span>{{ display.sub }}</span>
      <span class="cp-card__sep">·</span>
      <span>참여 {{ partnerCount }}개사</span>
    </footer>
  </article>
</template>

<style scoped>
.cp-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  background: var(--panel-color);
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cp-card:hover {
  border-color: var(--color-primary-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}
.cp-card:focus-visible {
  outline: 2px solid var(--color-primary-500);
  outline-offset: 2px;
}
.cp-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.cp-card__title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cp-card__primary {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 800;
  white-space: nowrap;
}
.cp-card__track {
  height: 8px;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  overflow: hidden;
}
.cp-card__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary-500);
  transition: width 0.3s cubic-bezier(0.32, 0.72, 0, 1);
}
.cp-card__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--muted-text);
  font-size: 12px;
}
.cp-card__sep {
  color: var(--subtle-text);
}
</style>
