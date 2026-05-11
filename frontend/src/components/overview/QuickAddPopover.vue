<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  date: { type: String, default: '' },
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
  campaigns: { type: Array, default: () => [] },  // 캠페인 picker 옵션
})
const emit = defineEmits(['close', 'create-task', 'create-milestone', 'open-campaign-modal'])

const step = ref('pick')   // 'pick' | 'task' | 'milestone'
const title = ref('')
const selectedCampaignId = ref(null)
const inputRef = ref(null)

const open = computed(() => !!props.date)

watch(open, async (v) => {
  if (v) {
    step.value = 'pick'
    title.value = ''
    selectedCampaignId.value = props.campaigns[0]?.id ?? null
  } else {
    return
  }
})

watch(step, async (s) => {
  if (s === 'task' || s === 'milestone') {
    await nextTick()
    inputRef.value?.focus()
  }
})

function pick(kind) {
  if (kind === 'campaign') {
    emit('open-campaign-modal', { date: props.date })
    emit('close')
    return
  }
  step.value = kind
}

function submit() {
  const t = title.value.trim()
  if (!t) { emit('close'); return }
  if (!selectedCampaignId.value) {
    emit('close')
    return
  }
  if (step.value === 'task') {
    emit('create-task', { title: t, date: props.date, campaignId: selectedCampaignId.value })
  } else if (step.value === 'milestone') {
    emit('create-milestone', { title: t, date: props.date, campaignId: selectedCampaignId.value })
  }
  emit('close')
}
</script>

<template>
  <transition name="qa-fade">
    <div v-if="open" class="qa-backdrop" @click="emit('close')">
      <div
        class="qa-popover"
        :style="{ left: position.x + 'px', top: position.y + 'px' }"
        @click.stop
      >
        <div class="qa-popover__date">
          <span class="material-symbols-outlined">add_circle</span>
          {{ date }}
        </div>

        <!-- Step 1: 종류 선택 -->
        <div v-if="step === 'pick'" class="qa-pick">
          <button class="qa-pick__btn" @click="pick('task')">
            <span class="qa-pick__emoji">✅</span>
            <span class="qa-pick__label">업무</span>
            <span class="qa-pick__hint">간단한 to-do</span>
          </button>
          <button class="qa-pick__btn" @click="pick('milestone')">
            <span class="qa-pick__emoji">🚩</span>
            <span class="qa-pick__label">마일스톤</span>
            <span class="qa-pick__hint">캠페인 단계</span>
          </button>
          <button class="qa-pick__btn" @click="pick('campaign')">
            <span class="qa-pick__emoji">📣</span>
            <span class="qa-pick__label">캠페인</span>
            <span class="qa-pick__hint">상세 작성 모달</span>
          </button>
        </div>

        <!-- Step 2: 제목/캠페인 입력 -->
        <div v-else class="qa-form">
          <button class="qa-form__back" @click="step = 'pick'">
            <span class="material-symbols-outlined">arrow_back</span>
            뒤로
          </button>
          <select
            v-if="campaigns.length"
            v-model="selectedCampaignId"
            class="qa-form__select"
          >
            <option v-for="c in campaigns" :key="c.id" :value="c.id">{{ c.title || c.name }}</option>
          </select>
          <div v-else class="qa-form__empty">먼저 캠페인을 만드세요</div>
          <input
            ref="inputRef"
            v-model="title"
            type="text"
            :placeholder="step === 'task' ? '업무 제목 입력...' : '마일스톤 제목 입력...'"
            class="qa-form__input"
            @keydown.enter.prevent="submit"
            @keydown.esc.prevent="emit('close')"
          />
          <div class="qa-form__actions">
            <button class="qa-btn qa-btn--ghost" @click="emit('close')">취소</button>
            <button class="qa-btn qa-btn--primary" :disabled="!campaigns.length || !title.trim()" @click="submit">
              추가
            </button>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.qa-backdrop {
  position: fixed;
  inset: 0;
  z-index: 180;
  background: transparent;
}
.qa-popover {
  position: absolute;
  width: 300px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
  padding: 12px;
  transform: translateY(8px);
}
.qa-popover__date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--accent-color, #8B5CF6);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 10px;
}
.qa-popover__date .material-symbols-outlined { font-size: 14px; }

.qa-pick { display: flex; flex-direction: column; gap: 4px; }
.qa-pick__btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  border-radius: 8px;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: background 0.15s, border-color 0.15s;
}
.qa-pick__btn:hover { background: var(--panel-muted); border-color: var(--accent-color, #8B5CF6); }
.qa-pick__emoji { font-size: 20px; flex-shrink: 0; }
.qa-pick__label { flex: 1; font-size: 13px; font-weight: 700; color: var(--text-primary); }
.qa-pick__hint { font-size: 11px; color: var(--muted-text); }

.qa-form { display: flex; flex-direction: column; gap: 8px; }
.qa-form__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  align-self: flex-start;
  font-size: 11px;
  font-weight: 600;
  color: var(--muted-text);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
}
.qa-form__back .material-symbols-outlined { font-size: 14px; }
.qa-form__back:hover { color: var(--accent-color, #8B5CF6); }
.qa-form__select, .qa-form__input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 13px;
  background: var(--control-color);
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.15s;
}
.qa-form__select:focus, .qa-form__input:focus { border-color: var(--accent-color, #8B5CF6); }
.qa-form__empty {
  padding: 8px;
  font-size: 11px;
  color: var(--muted-text);
  text-align: center;
  background: var(--panel-muted);
  border-radius: 6px;
}
.qa-form__actions { display: flex; justify-content: flex-end; gap: 6px; margin-top: 4px; }
.qa-btn {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: background 0.15s;
}
.qa-btn--ghost { background: transparent; color: var(--muted-text); }
.qa-btn--ghost:hover { color: var(--text-primary); }
.qa-btn--primary { background: var(--accent-color, #8B5CF6); color: #fff; }
.qa-btn--primary:hover:not(:disabled) { background: #7C3AED; }
.qa-btn--primary:disabled { opacity: 0.5; cursor: not-allowed; }

.qa-fade-enter-active, .qa-fade-leave-active { transition: opacity 0.12s; }
.qa-fade-enter-from, .qa-fade-leave-to { opacity: 0; }
.qa-fade-enter-from .qa-popover, .qa-fade-leave-to .qa-popover { transform: translateY(0); }
</style>
