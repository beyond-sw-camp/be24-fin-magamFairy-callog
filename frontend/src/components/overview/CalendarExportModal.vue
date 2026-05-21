<script setup>
/**
 * 캘린더 내보내기 모달 — 날짜 범위 + 유형 선택 → .ics(구글 캘린더 형식) 다운로드.
 */
import { ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  defaultFrom: { type: String, default: '' },
  defaultTo: { type: String, default: '' },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'export']) // export({ from, to, types: [] })

const from = ref('')
const to = ref('')
const types = ref({ task: true, milestone: true, deadline: true })

watch(() => props.open, (v) => {
  if (!v) return
  from.value = props.defaultFrom || ''
  to.value = props.defaultTo || ''
  types.value = { task: true, milestone: true, deadline: true }
})

function submit() {
  if (!from.value || !to.value) return
  if (to.value < from.value) return
  const selected = Object.entries(types.value).filter(([, on]) => on).map(([k]) => k)
  if (!selected.length) return
  emit('export', { from: from.value, to: to.value, types: selected })
}
</script>

<template>
  <Transition name="cie-fade">
    <div v-if="open" class="cie-backdrop" @click="!busy && emit('close')">
      <div class="cee-modal" role="dialog" aria-modal="true" @click.stop>
        <div class="cie-h">
          <h3>캘린더 내보내기</h3>
          <button class="cie-x" aria-label="닫기" :disabled="busy" @click="emit('close')">✕</button>
        </div>
        <p class="cie-desc">선택한 기간·유형의 내 일정을 구글 캘린더 형식(<b>.ics</b>)으로 내려받습니다.</p>

        <div class="cee-row">
          <label class="cee-field">
            <span class="cee-lbl">시작일</span>
            <input v-model="from" type="date" class="cee-input" :max="to || undefined" />
          </label>
          <label class="cee-field">
            <span class="cee-lbl">종료일</span>
            <input v-model="to" type="date" class="cee-input" :min="from || undefined" />
          </label>
        </div>

        <div class="cee-types">
          <span class="cee-lbl">포함할 유형</span>
          <div class="cee-checks">
            <label class="cee-check"><input v-model="types.task" type="checkbox" /> 업무</label>
            <label class="cee-check"><input v-model="types.milestone" type="checkbox" /> 마일스톤</label>
            <label class="cee-check"><input v-model="types.deadline" type="checkbox" /> 모집 마감</label>
          </div>
        </div>

        <div class="cee-actions">
          <button class="cee-btn cee-btn--ghost" :disabled="busy" @click="emit('close')">취소</button>
          <button class="cee-btn cee-btn--primary" :disabled="busy || !from || !to" @click="submit">
            {{ busy ? '내보내는 중…' : '내보내기' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.cie-backdrop {
  position: fixed; inset: 0; z-index: 220;
  background: rgba(42,36,64,.46); backdrop-filter: blur(3px);
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.cee-modal {
  background: var(--lp-surface); border-radius: var(--r-lg, 18px);
  width: min(440px, 94vw); padding: 22px 24px 24px;
  box-shadow: 0 24px 60px rgba(63,52,99,.32);
}
.cie-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.cie-h h3 { margin: 0; font-size: 17px; font-weight: 800; color: var(--lp-text); }
.cie-x { width: 30px; height: 30px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-text-muted); cursor: pointer; font-size: 13px; }
.cie-x:hover { background: var(--lp-surface-soft); }
.cie-desc { margin: 0 0 18px; font-size: 12.5px; color: var(--lp-text-muted); line-height: 1.5; }
.cie-desc b { color: var(--lp-text); }

.cee-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.cee-field { display: flex; flex-direction: column; gap: 5px; }
.cee-lbl { font-size: 11px; font-weight: 700; color: var(--lp-text-muted); }
.cee-input {
  width: 100%; padding: 9px 11px; border: 1px solid var(--lp-border); border-radius: 9px;
  background: var(--lp-surface); color: var(--lp-text); font: inherit; font-size: 13px; outline: none; box-sizing: border-box;
}
.cee-input:focus { border-color: var(--lp-primary-strong); }

.cee-types { margin-bottom: 22px; display: flex; flex-direction: column; gap: 8px; }
.cee-checks { display: flex; flex-wrap: wrap; gap: 14px; }
.cee-check { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--lp-text); cursor: pointer; }
.cee-check input { width: 16px; height: 16px; accent-color: var(--lp-primary-strong); cursor: pointer; }

.cee-actions { display: flex; justify-content: flex-end; gap: 8px; }
.cee-btn { padding: 9px 18px; border-radius: 9px; font-size: 13px; font-weight: 700; cursor: pointer; border: 0; transition: background .15s, opacity .15s; }
.cee-btn:disabled { opacity: .5; cursor: not-allowed; }
.cee-btn--ghost { background: var(--lp-surface-soft); color: var(--lp-text-muted); }
.cee-btn--ghost:hover:not(:disabled) { color: var(--lp-text); }
.cee-btn--primary { background: var(--lp-button-bg); color: #fff; }
.cee-btn--primary:hover:not(:disabled) { background: var(--lp-button-bg-hover); }

.cie-fade-enter-active, .cie-fade-leave-active { transition: opacity .18s ease; }
.cie-fade-enter-from, .cie-fade-leave-to { opacity: 0; }
</style>
