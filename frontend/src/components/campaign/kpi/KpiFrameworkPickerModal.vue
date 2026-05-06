<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h3 class="modal__title">프레임워크 불러오기</h3>
      <p class="modal__sub">캠페인 유형에 맞는 KPI 세트를 선택하면 기존 KPI에 추가됩니다.</p>
      <div class="fw-grid">
        <div
          v-for="fw in frameworks"
          :key="fw.key"
          :class="['fw-card', { 'fw-card--selected': selected === fw.key }]"
          @click="selected = fw.key"
        >
          <p class="fw-card__name">{{ fw.name }}</p>
          <p class="fw-card__desc">{{ fw.description }}</p>
          <ul class="fw-card__items">
            <li v-for="item in fw.items" :key="item.name">
              {{ item.name }} ({{ item.defaultTarget }} {{ item.unit }})
            </li>
          </ul>
        </div>
      </div>
      <div class="modal__footer">
        <button class="btn btn--outline" @click="$emit('close')">취소</button>
        <button class="btn btn--primary" :disabled="!selected" @click="apply">이 프레임워크 적용</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({ frameworks: Array })
const emit = defineEmits(['close', 'apply'])

const selected = ref(null)
function apply() {
  if (selected.value) emit('apply', selected.value)
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 24px;
  width: 640px;
  max-width: 95vw;
  max-height: 85vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.modal__title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.modal__sub {
  font-size: 13px;
  color: var(--muted-text);
  margin: 0;
}

.fw-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.fw-card {
  border: 2px solid var(--border-color);
  border-radius: 8px;
  padding: 14px;
  cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.fw-card:hover {
  border-color: var(--color-primary-500);
}

.fw-card--selected {
  border-color: var(--color-primary-500);
  background: var(--color-primary-light);
}

.fw-card__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.fw-card__desc {
  font-size: 12px;
  color: var(--muted-text);
  margin: 0 0 10px;
}

.fw-card__items {
  list-style: disc;
  padding-left: 16px;
  margin: 0;
  font-size: 12px;
  color: var(--muted-text);
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
}

.btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.btn--outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn--outline:hover {
  border-color: var(--color-primary-500);
  color: var(--color-primary-500);
}

.btn--primary {
  background: var(--color-primary-500);
  border: 1px solid var(--color-primary-500);
  color: #fff;
}

.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-600);
  border-color: var(--color-primary-600);
}

.btn--primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
