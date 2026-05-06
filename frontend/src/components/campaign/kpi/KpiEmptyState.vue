<template>
  <div class="kpi-empty">
    <p class="kpi-empty__title">아직 등록된 KPI가 없습니다</p>
    <p class="kpi-empty__sub">지표를 추가하거나 프레임워크를 불러와 시작하세요.</p>
    <div v-if="editable" class="kpi-empty__actions">
      <button class="btn btn--primary" @click="$emit('add')">지표 추가</button>
      <button class="btn btn--outline" @click="$emit('framework')">프레임워크 불러오기</button>
    </div>
  </div>
</template>

<script setup>
defineProps({ editable: Boolean })
defineEmits(['add', 'framework'])
</script>

<style scoped>
.kpi-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
  gap: 12px;
}

.kpi-empty__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.kpi-empty__sub {
  font-size: 13px;
  color: var(--muted-text);
}

.kpi-empty__actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  font-weight: 600;
  transition: background var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn--primary {
  background: var(--color-primary-500);
  color: #fff;
}

.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-600);
}

.btn--outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn--outline:hover:not(:disabled) {
  background: var(--panel-muted);
  border-color: var(--border-strong);
}
</style>
