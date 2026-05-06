<template>
  <div class="summary-cards">
    <div class="card">
      <span class="card__label">전체 달성률</span>
      <span class="card__value">{{ summary?.overallAchievementPercent ?? 0 }}%</span>
    </div>
    <div class="card">
      <span class="card__label">총 노출 (IMPRESSION)</span>
      <span class="card__value">{{ fmt(summary?.totalImpression) }}</span>
    </div>
    <div class="card">
      <span class="card__label">총 클릭 (ENGAGEMENT)</span>
      <span class="card__value">{{ fmt(summary?.totalClicks) }}</span>
    </div>
    <div class="card">
      <span class="card__label">KPI 현황</span>
      <span class="card__value">
        {{ summary?.achievedCount ?? 0 }}달성 /
        {{ summary?.totalKpiCount ?? 0 }}개
      </span>
    </div>
  </div>
</template>

<script setup>
defineProps({ summary: Object })

function fmt(val) {
  if (val == null) return '-'
  const n = Number(val)
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K'
  return n.toLocaleString()
}
</script>

<style scoped>
.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.card {
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.card:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-sm);
}

.card__label {
  font-size: 11px;
  color: var(--muted-text);
  font-weight: 500;
}

.card__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

@media (max-width: 800px) {
  .summary-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }
}
</style>
