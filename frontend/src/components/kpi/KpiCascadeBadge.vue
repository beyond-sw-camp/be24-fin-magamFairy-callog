<script setup>
import { computed } from 'vue'

const props = defineProps({
  parentName: { type: String, default: '' },
  contribution: { type: [Number, String], default: null },
  unit: { type: String, default: '' },
})

const hasParent = computed(() => Boolean(props.parentName))
const contributionLabel = computed(() => {
  if (props.contribution === null || props.contribution === undefined || props.contribution === '') {
    return ''
  }
  return `${props.contribution}${props.unit ? ` ${props.unit}` : ''}`
})
</script>

<template>
  <span v-if="hasParent" class="cascade-badge" :title="`${parentName}에 cascade`">
    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M3 6h13a4 4 0 0 1 4 4v8" />
      <path d="m16 14 4 4 4-4" />
    </svg>
    <span class="cascade-badge__txt">
      <strong>{{ parentName }}</strong>
      <em v-if="contributionLabel">+{{ contributionLabel }}</em>
    </span>
  </span>
</template>

<style scoped>
.cascade-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-primary-500) 10%, transparent);
  color: var(--color-primary-700);
  font-size: 11px;
  font-weight: 600;
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 22%, transparent);
  max-width: 100%;
}
.cascade-badge svg { flex-shrink: 0; }
.cascade-badge__txt {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  min-width: 0;
}
.cascade-badge__txt strong {
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 14ch;
}
.cascade-badge__txt em {
  font-style: normal;
  font-weight: 700;
  color: var(--color-primary-600);
  font-variant-numeric: tabular-nums;
}
</style>
