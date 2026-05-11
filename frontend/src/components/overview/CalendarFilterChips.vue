<script setup>
defineProps({
  filter: {
    type: Object,
    default: () => ({ mineOnly: false }),
  },
})
const emit = defineEmits(['update:filter'])

function setMine(value, current) {
  emit('update:filter', { ...current, mineOnly: value })
}
</script>

<template>
  <div class="filter-chips">
    <div class="filter-chips__group" role="radiogroup" aria-label="필터">
      <button
        type="button"
        role="radio"
        :aria-checked="!filter.mineOnly"
        class="filter-chip"
        :class="{ 'filter-chip--on': !filter.mineOnly }"
        @click="setMine(false, filter)"
      >
        <span class="filter-chip__emoji">✨</span>
        전체
      </button>
      <button
        type="button"
        role="radio"
        :aria-checked="filter.mineOnly"
        class="filter-chip"
        :class="{ 'filter-chip--on': filter.mineOnly }"
        @click="setMine(true, filter)"
      >
        <span class="filter-chip__emoji">👤</span>
        내 캠페인
      </button>
    </div>
  </div>
</template>

<style scoped>
.filter-chips {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-chips__group {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--panel-muted);
  padding: 3px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
}
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
  color: var(--muted-text);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.filter-chip:hover { color: var(--text-primary); }
.filter-chip--on {
  background: var(--panel-color);
  color: var(--accent-color, #8B5CF6);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.filter-chip--toggle {
  background: var(--panel-muted);
  border-color: var(--border-color);
  padding: 5px 10px;
}
.filter-chip--toggle.filter-chip--on {
  background: rgba(139, 92, 246, 0.1);
  border-color: var(--accent-color, #8B5CF6);
  color: var(--accent-color, #8B5CF6);
}
.filter-chip__emoji { font-size: 13px; }
.filter-chip .material-symbols-outlined { font-size: 14px; }
</style>
