<script setup>
defineProps({
  modelValue: { type: String, required: true },  // "mine" | "org"
  mineCount: { type: Number, default: 0 },
  orgCount: { type: Number, default: 0 },
})
const emit = defineEmits(['update:modelValue'])
function pick(value) {
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="hero-toggle" role="tablist">
    <button
      type="button"
      class="hero-toggle__btn"
      :class="{ 'hero-toggle__btn--active': modelValue === 'mine' }"
      :aria-selected="modelValue === 'mine'"
      role="tab"
      @click="pick('mine')"
    >
      <span>내 캠페인</span>
      <strong>{{ mineCount }}</strong>
    </button>
    <button
      type="button"
      class="hero-toggle__btn"
      :class="{ 'hero-toggle__btn--active': modelValue === 'org' }"
      :aria-selected="modelValue === 'org'"
      role="tab"
      @click="pick('org')"
    >
      <span>조직 전체</span>
      <strong>{{ orgCount }}</strong>
    </button>
  </div>
</template>

<style scoped>
.hero-toggle {
  display: inline-flex;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-color);
  padding: 4px;
  gap: 2px;
}
.hero-toggle__btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: background 0.15s, color 0.15s;
}
.hero-toggle__btn:hover {
  color: var(--text-primary);
}
.hero-toggle__btn--active {
  background: var(--color-primary-500);
  color: #fff;
}
.hero-toggle__btn strong {
  font-weight: 800;
  font-size: 12px;
  padding: 0 6px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.18);
}
.hero-toggle__btn:not(.hero-toggle__btn--active) strong {
  background: var(--panel-muted);
  color: var(--text-primary);
}
</style>
