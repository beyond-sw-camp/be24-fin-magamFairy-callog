<script setup>
/**
 * 캘린더 가져오기 모달 — 파일 선택 후 가운데에 뜨는 덮어쓰기/추가하기 선택.
 *  왼쪽: 덮어쓰기  ·  오른쪽: 추가하기
 *  어느 쪽을 누르든 .ics(구글 캘린더 형식)를 우리 캘린더(개인 업무)로 넣는다.
 */
defineProps({
  open: { type: Boolean, default: false },
  fileName: { type: String, default: '' },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'confirm']) // confirm('overwrite' | 'append')
</script>

<template>
  <Transition name="cie-fade">
    <div v-if="open" class="cie-backdrop" @click="!busy && emit('close')">
      <div class="cie-modal" role="dialog" aria-modal="true" @click.stop>
        <div class="cie-h">
          <h3>캘린더 가져오기</h3>
          <button class="cie-x" aria-label="닫기" :disabled="busy" @click="emit('close')">✕</button>
        </div>

        <p class="cie-file">
          <span class="material-symbols-outlined">event</span>
          <span class="cie-file-name">{{ fileName || '선택된 파일' }}</span>
        </p>
        <p class="cie-desc">구글 캘린더 형식(.ics)을 우리 캘린더의 <b>개인 업무</b>로 가져옵니다. 방식을 선택하세요.</p>

        <div class="cie-choice">
          <button class="cie-btn cie-btn--overwrite" :disabled="busy" @click="emit('confirm', 'overwrite')">
            <span class="cie-btn-ic">🔁</span>
            <span class="cie-btn-t">덮어쓰기</span>
            <span class="cie-btn-s">가져온 파일의 날짜 범위에 있는 내 개인 일정을 지우고 교체</span>
          </button>
          <button class="cie-btn cie-btn--append" :disabled="busy" @click="emit('confirm', 'append')">
            <span class="cie-btn-ic">➕</span>
            <span class="cie-btn-t">추가하기</span>
            <span class="cie-btn-s">기존 일정은 그대로 두고 새로 추가</span>
          </button>
        </div>

        <div v-if="busy" class="cie-busy">가져오는 중…</div>
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
.cie-modal {
  background: var(--lp-surface); border-radius: var(--r-lg, 18px);
  width: min(560px, 94vw); padding: 22px 24px 24px;
  box-shadow: 0 24px 60px rgba(63,52,99,.32);
}
.cie-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.cie-h h3 { margin: 0; font-size: 17px; font-weight: 800; color: var(--lp-text); }
.cie-x { width: 30px; height: 30px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-text-muted); cursor: pointer; font-size: 13px; }
.cie-x:hover { background: var(--lp-surface-soft); }

.cie-file { display: flex; align-items: center; gap: 7px; margin: 0 0 8px; font-size: 12.5px; font-weight: 700; color: var(--lp-primary-deep); background: var(--lp-surface-soft); padding: 9px 12px; border-radius: 10px; }
.cie-file .material-symbols-outlined { font-size: 16px; }
.cie-file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cie-desc { margin: 0 0 18px; font-size: 12.5px; color: var(--lp-text-muted); line-height: 1.5; }
.cie-desc b { color: var(--lp-text); }

.cie-choice { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.cie-btn {
  display: flex; flex-direction: column; align-items: flex-start; gap: 4px;
  padding: 16px 16px; border-radius: 14px; border: 1.5px solid var(--lp-border);
  background: var(--lp-surface); cursor: pointer; text-align: left;
  transition: transform .12s ease, box-shadow .15s ease, border-color .15s ease, background .15s ease;
}
.cie-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(63,52,99,.14); }
.cie-btn:disabled { opacity: .55; cursor: not-allowed; }
.cie-btn-ic { font-size: 22px; }
.cie-btn-t { font-size: 15px; font-weight: 800; color: var(--lp-text); }
.cie-btn-s { font-size: 11px; font-weight: 500; color: var(--lp-text-muted); line-height: 1.45; }
.cie-btn--overwrite:hover:not(:disabled) { border-color: #E25B49; background: rgba(226,91,73,.06); }
.cie-btn--overwrite .cie-btn-t { color: #C04438; }
.cie-btn--append:hover:not(:disabled) { border-color: var(--lp-primary); background: var(--accent-soft); }
.cie-btn--append .cie-btn-t { color: var(--lp-primary-deep); }

.cie-busy { margin-top: 14px; text-align: center; font-size: 12px; font-weight: 700; color: var(--lp-primary-deep); }

.cie-fade-enter-active, .cie-fade-leave-active { transition: opacity .18s ease; }
.cie-fade-enter-from, .cie-fade-leave-to { opacity: 0; }

@media (max-width: 520px) {
  .cie-choice { grid-template-columns: 1fr; }
}
</style>
