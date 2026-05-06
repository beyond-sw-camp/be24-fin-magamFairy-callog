<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  // task: 태스크 객체 — { title, dueDate, campaignId, campaignName, ... }
  task: { type: Object, required: true },
})

const router = useRouter()

const dDay = computed(() => {
  if (!props.task.dueDate) return null
  const target = new Date(props.task.dueDate)
  if (Number.isNaN(target.getTime())) return null
  // 일자 단위로 비교: 오늘 자정 대비
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const t = new Date(target)
  t.setHours(0, 0, 0, 0)
  return Math.round((t - today) / 86400000)
})

const dDayLabel = computed(() => {
  if (dDay.value == null) return '미정'
  if (dDay.value === 0) return '오늘'
  return dDay.value > 0 ? `D-${dDay.value}` : `D+${-dDay.value}`
})

const tone = computed(() => {
  if (dDay.value == null) return 'muted'
  if (dDay.value <= 0) return 'urgent'
  if (dDay.value <= 2) return 'warning'
  return 'normal'
})

function goToCampaign() {
  if (props.task.campaignId) {
    router.push({ name: 'campaign-detail', params: { campaignId: props.task.campaignId } })
  }
}
</script>

<template>
  <div
    class="upc-item"
    role="link"
    tabindex="0"
    @click="goToCampaign"
    @keydown.enter="goToCampaign"
  >
    <span class="upc-item__day" :class="`upc-item__day--${tone}`">{{ dDayLabel }}</span>
    <div class="upc-item__body">
      <strong class="upc-item__title">{{ task.title || '제목 없음' }}</strong>
      <span class="upc-item__campaign">{{ task.campaignName || '캠페인' }}</span>
    </div>
  </div>
</template>

<style scoped>
.upc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.15s;
}
.upc-item:hover {
  background: var(--panel-muted);
}
.upc-item:focus-visible {
  outline: 2px solid var(--color-primary-500);
  outline-offset: 2px;
}
.upc-item__day {
  flex: 0 0 auto;
  min-width: 48px;
  height: 28px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 800;
  background: var(--panel-muted);
  color: var(--muted-text);
}
.upc-item__day--urgent {
  background: var(--color-danger);
  color: #fff;
}
.upc-item__day--warning {
  background: var(--color-warning);
  color: var(--color-warning-dark);
}
.upc-item__day--normal {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}
.upc-item__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.upc-item__title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.upc-item__campaign {
  color: var(--muted-text);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
