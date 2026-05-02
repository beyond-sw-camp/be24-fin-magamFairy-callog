<template>
  <div class="analysis-box">
    <div class="analysis-box__header">
      <span class="analysis-box__title">성과 분석 메모</span>
      <button
        v-if="editable"
        class="btn btn--sm"
        :disabled="saving"
        @click="save"
      >
        {{ saving ? '저장 중…' : '저장' }}
      </button>
    </div>
    <textarea
      v-model="draft"
      :disabled="!editable"
      class="analysis-box__textarea"
      placeholder="캠페인 성과에 대한 분석 내용을 입력하세요."
      rows="5"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ modelValue: String, editable: Boolean })
const emit = defineEmits(['save'])

const draft = ref(props.modelValue ?? '')
const saving = ref(false)

watch(() => props.modelValue, (v) => {
  draft.value = v ?? ''
})

async function save() {
  saving.value = true
  try {
    await emit('save', draft.value)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.analysis-box {
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  margin-top: 16px;
}

.analysis-box__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.analysis-box__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.analysis-box__textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 10px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--control-color);
  font-family: inherit;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.analysis-box__textarea:focus {
  outline: none;
  border-color: var(--color-primary-500);
  background: var(--panel-color);
}

.analysis-box__textarea:disabled {
  background: var(--panel-muted);
  color: var(--muted-text);
  cursor: not-allowed;
}

.analysis-box__textarea::placeholder {
  color: var(--subtle-text);
}

.btn--sm {
  padding: 5px 14px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  background: var(--color-primary-500);
  color: #fff;
  border: none;
  transition: background var(--transition-fast);
}

.btn--sm:hover:not(:disabled) {
  background: var(--color-primary-600);
}

.btn--sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
