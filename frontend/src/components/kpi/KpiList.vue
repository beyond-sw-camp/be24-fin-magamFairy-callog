<script setup>
import { computed } from 'vue'
import KpiCard from './KpiCard.vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  editable: { type: Boolean, default: false },
  emptyText: { type: String, default: '등록된 KPI가 없습니다.' },
})

const emit = defineEmits(['edit', 'archive', 'activate'])

const sortedItems = computed(() => {
  const order = { ACTIVE: 0, DRAFT: 1, ARCHIVED: 2 }
  return [...props.items].sort((a, b) => {
    const sa = order[a.status] ?? 3
    const sb = order[b.status] ?? 3
    if (sa !== sb) return sa - sb
    return (a.name || '').localeCompare(b.name || '')
  })
})
</script>

<template>
  <div class="kpi-list">
    <div v-if="sortedItems.length === 0" class="kpi-list__empty">
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
           stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 8v4M12 16h.01" />
      </svg>
      <p>{{ emptyText }}</p>
    </div>
    <div v-else class="kpi-list__grid">
      <KpiCard
        v-for="kpi in sortedItems"
        :key="kpi.idx"
        :kpi="kpi"
        :editable="editable"
        @edit="emit('edit', $event)"
        @archive="emit('archive', $event)"
        @activate="emit('activate', $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.kpi-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.kpi-list__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.kpi-list__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 32px 20px;
  background: var(--panel-color);
  border: 1px dashed var(--border-color);
  border-radius: 16px;
  color: var(--muted-text);
}
.kpi-list__empty p { margin: 0; font-size: 13px; }
</style>
