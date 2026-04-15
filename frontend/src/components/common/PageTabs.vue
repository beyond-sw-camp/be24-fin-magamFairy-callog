<script setup>
const props = defineProps({
  active: {
    type: String,
    required: true,
  },
  tabs: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['select'])

function handleSelect(value) {
  emit('select', value)
}
</script>

<template>
  <div class="page-tabs">
    <button
      v-for="tab in props.tabs"
      :key="tab.value"
      class="page-tabs__item"
      :class="{ 'page-tabs__item--active': tab.value === props.active }"
      type="button"
      @click="handleSelect(tab.value)"
    >
      <svg v-if="tab.icon" viewBox="0 0 24 24" aria-hidden="true">
        <path :d="tab.icon" />
      </svg>

      <span>{{ tab.label }}</span>
      <small v-if="tab.caption">{{ tab.caption }}</small>
    </button>
  </div>
</template>

<style scoped>
.page-tabs {
  display: flex;
  align-items: center;
  gap: 0.1rem;
  border-bottom: 1px solid var(--border-color);
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}

.page-tabs::-webkit-scrollbar {
  display: none;
}

.page-tabs__item {
  position: relative;
  min-height: 3rem;
  flex-shrink: 0;
  padding: 0.8rem 0.9rem 0.85rem;
  border-bottom: 2px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--muted-text);
  cursor: pointer;
  transition:
    color 0.18s ease,
    border-color 0.18s ease;
}

.page-tabs__item svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.page-tabs__item span {
  font-weight: 600;
}

.page-tabs__item small {
  color: inherit;
  opacity: 0.74;
  font-size: 0.74rem;
}

.page-tabs__item:hover,
.page-tabs__item--active {
  color: var(--text-primary);
}

.page-tabs__item--active {
  border-bottom-color: var(--accent-color);
}
</style>
