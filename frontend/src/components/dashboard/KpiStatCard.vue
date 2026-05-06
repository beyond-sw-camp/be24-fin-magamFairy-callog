<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  caption: { type: String, default: '' },
  positive: { type: Boolean, default: false },
})
</script>

<template>
  <article class="kpi-card">
    <p class="kpi-card__label">{{ label }}</p>
    <strong class="kpi-card__value">{{ value }}</strong>
    <span class="kpi-card__caption" :class="{ 'kpi-card__caption--positive': positive }">
      {{ caption }}
    </span>
  </article>
</template>

<style scoped>
.kpi-card {
  min-height: 134px;
  padding: 22px 24px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal),
    color var(--transition-normal);
}
.kpi-card__label {
  color: var(--muted-text);
  font-size: 13px;
  font-weight: 600;
}
.kpi-card__value {
  display: block;
  margin: 8px 0 4px;
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 700;
  line-height: 1.1;
}
.kpi-card__caption {
  color: var(--subtle-text);
  font-size: 13px;
}
.kpi-card__caption--positive {
  color: var(--color-success);
  font-weight: 600;
}
</style>
