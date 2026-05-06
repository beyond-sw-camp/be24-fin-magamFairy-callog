<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h3 class="modal__title">실적값 입력 — {{ kpi?.name }}</h3>
      <div class="form">
        <label>
          실적값 ({{ kpi?.unit }})
          <input v-model.number="form.actualValue" type="number" />
        </label>
        <label>
          메모
          <textarea v-model="form.memo" rows="3" placeholder="측정 방법, 출처 등" />
        </label>
        <label>
          다음 액션
          <textarea v-model="form.nextAction" rows="2" placeholder="개선 방향, 다음 조치 등" />
        </label>
      </div>
      <div class="modal__footer">
        <button class="btn btn--outline" @click="$emit('close')">취소</button>
        <button class="btn btn--primary" @click="submit">저장</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

const props = defineProps({ kpi: Object })
const emit = defineEmits(['close', 'submit'])

const form = reactive({
  actualValue: props.kpi?.actualValue ?? null,
  memo: props.kpi?.memo ?? '',
  nextAction: props.kpi?.nextAction ?? ''
})

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
.form textarea {
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--panel-color);
  resize: vertical;
  transition: border-color var(--transition-fast);
}

.form input:focus,
.form textarea:focus {
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
