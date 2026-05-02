<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h3 class="modal__title">{{ editTarget ? 'KPI 수정' : 'KPI 추가' }}</h3>
      <div class="form">
        <label>
          이름
          <input v-model="form.name" type="text" placeholder="예: 총 노출 수" />
        </label>
        <label>
          분류
          <select v-model="form.category">
            <option v-for="(label, key) in CATEGORIES" :key="key" :value="key">
              {{ label }}
            </option>
          </select>
        </label>
        <label>
          목표값
          <input v-model.number="form.targetValue" type="number" />
        </label>
        <label>
          단위
          <input v-model="form.unit" type="text" placeholder="Views, %, 원 …" />
        </label>
        <label>
          담당자 표시명
          <input v-model="form.ownerLabel" type="text" />
        </label>
      </div>
      <div class="modal__footer">
        <button class="btn btn--outline" @click="$emit('close')">취소</button>
        <button class="btn btn--primary" :disabled="!canSubmit" @click="submit">저장</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed } from 'vue'

const props = defineProps({ editTarget: Object })
const emit = defineEmits(['close', 'submit'])

const CATEGORIES = {
  IMPRESSION: '노출도',
  ENGAGEMENT: '참여도',
  CONVERSION: '전환',
  REVENUE: '매출/ROI',
  BRAND: '브랜드',
  ESG: 'ESG',
  OTHER: '기타'
}

const form = reactive({
  name: props.editTarget?.name ?? '',
  category: props.editTarget?.category ?? 'IMPRESSION',
  targetValue: props.editTarget?.targetValue ?? null,
  unit: props.editTarget?.unit ?? '',
  ownerLabel: props.editTarget?.ownerLabel ?? '',
  ownerUserIdx: props.editTarget?.ownerUserIdx ?? null
})

const canSubmit = computed(() => form.name && form.targetValue && form.unit)
function submit() {
  emit('submit', { ...form })
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: var(--panel-color);
  border-radius: 10px;
  padding: 24px;
  width: 420px;
  max-width: 90vw;
  box-shadow: var(--shadow-sm);
}

.modal__title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--muted-text);
  font-weight: 500;
}

.form input,
.form select {
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--panel-color);
  transition: border-color var(--transition-fast);
}

.form input:focus,
.form select:focus {
  outline: none;
  border-color: var(--border-strong);
}

.modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  font-weight: 600;
  transition: background var(--transition-fast), border-color var(--transition-fast);
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
