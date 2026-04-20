<script setup>
const props = defineProps({
  label: {
    type: String,
    required: true,
  },
  value: {
    type: String,
    required: true,
  },
  accent: {
    type: String,
    default: '#2f80ed',
  },
  helper: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <article class="metric-card" :style="{ '--metric-accent': props.accent }">
    <span class="metric-card__dot" />
    <p>{{ props.label }}</p>
    <strong>{{ props.value }}</strong>
    <small>{{ props.helper }}</small>
  </article>
</template>

<style scoped>
.metric-card {
  border: 1px solid var(--border-color);
  border-radius: 22px;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--metric-accent) 11%, var(--panel-color)),
      var(--panel-color)
    ),
    var(--panel-color);
  padding: 1rem 1.1rem;
  box-shadow: var(--shadow-soft);
}

.metric-card__dot {
  width: 0.58rem;
  height: 0.58rem;
  border-radius: 999px;
  display: inline-block;
  background: var(--metric-accent);
  margin-bottom: 0.65rem;
}

.metric-card p,
.metric-card small {
  color: var(--muted-text);
}

.metric-card strong {
  display: block;
  margin: 0.4rem 0 0.25rem;
  font-size: 1.8rem;
  line-height: 1;
}
</style>
