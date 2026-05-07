<script setup>
import { computed, onMounted } from 'vue'
import { useOrganizationKpiStore } from '@/stores/organizationKpi'

const props = defineProps({
  modelValue: { type: [Number, String, null], default: null },
})
const emit = defineEmits(['update:modelValue', 'apply'])

const store = useOrganizationKpiStore()

onMounted(() => {
  if (store.templates.length === 0) {
    void store.fetchTemplates()
  }
})

const selected = computed({
  get: () => props.modelValue,
  set: (next) => emit('update:modelValue', next),
})

function handleApply(template) {
  selected.value = template.idx
  emit('apply', template)
}

function categoryLabel(category) {
  const map = {
    IMPRESSION: '노출',
    ENGAGEMENT: '참여',
    CONVERSION: '전환',
    REVENUE: '매출',
    BRAND: '브랜드',
    OTHER: '기타',
  }
  return map[category] ?? category
}
</script>

<template>
  <div class="tpl-picker">
    <p class="tpl-picker__hint">자주 쓰는 KPI 템플릿을 골라서 빠르게 시작하세요.</p>
    <ul v-if="store.templates.length" class="tpl-picker__list">
      <li
        v-for="tpl in store.templates"
        :key="tpl.idx"
        class="tpl-item"
        :class="{ 'tpl-item--selected': selected === tpl.idx }"
        @click="handleApply(tpl)"
      >
        <div class="tpl-item__head">
          <span class="tpl-item__name">{{ tpl.name }}</span>
          <span class="tpl-item__usage">사용 {{ tpl.usageCount ?? 0 }}회</span>
        </div>
        <div class="tpl-item__meta">
          <span class="tpl-item__pill">{{ categoryLabel(tpl.defaultCategory) }}</span>
          <span class="tpl-item__pill tpl-item__pill--soft">{{ tpl.defaultUnit }}</span>
          <span class="tpl-item__scope">{{ tpl.scope === 'GLOBAL' ? '공용' : '조직 전용' }}</span>
        </div>
      </li>
    </ul>
    <p v-else class="tpl-picker__empty">사용할 수 있는 템플릿이 없습니다.</p>
  </div>
</template>

<style scoped>
.tpl-picker { display: flex; flex-direction: column; gap: 10px; }
.tpl-picker__hint {
  margin: 0;
  font-size: 12px;
  color: var(--muted-text);
}
.tpl-picker__list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px;
  max-height: 220px;
  overflow-y: auto;
}
.tpl-item {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.tpl-item:hover {
  border-color: color-mix(in srgb, var(--color-primary-500) 30%, var(--border-color));
  background: var(--color-primary-50);
}
.tpl-item--selected {
  border-color: var(--color-primary-500);
  background: var(--color-primary-50);
}
.tpl-item__head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}
.tpl-item__name { font-size: 13px; font-weight: 700; color: var(--text-primary); }
.tpl-item__usage { font-size: 10px; color: var(--muted-text); font-variant-numeric: tabular-nums; }
.tpl-item__meta { display: inline-flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.tpl-item__pill {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}
.tpl-item__pill--soft {
  background: var(--panel-muted);
  color: var(--text-secondary);
}
.tpl-item__scope { font-size: 10px; color: var(--muted-text); }
.tpl-picker__empty {
  margin: 0;
  font-size: 12px;
  color: var(--muted-text);
  text-align: center;
  padding: 20px;
}
</style>
