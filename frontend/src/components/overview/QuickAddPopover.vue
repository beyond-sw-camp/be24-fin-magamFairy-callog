<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ListMilestones } from '@/api/teamboard'

const props = defineProps({
  date: { type: String, default: '' },
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
  campaigns: { type: Array, default: () => [] },  // 마일스톤/업무파트용 캠페인 옵션
})
const emit = defineEmits(['close', 'create-task', 'create-milestone', 'create-taskpart'])

// 개인 업무: 캠페인 없음 / 마일스톤·업무파트: 캠페인 필요
const step = ref('pick')   // 'pick' | 'task' | 'milestone' | 'taskpart'
const title = ref('')
const time = ref('10:00')
const priority = ref('MEDIUM')
const selectedCampaignId = ref(null)
const selectedMilestoneId = ref(null)
const milestones = ref([])
const loadingMs = ref(false)
const inputRef = ref(null)
const popoverRef = ref(null)
const posStyle = ref({ left: '0px', top: '0px' })

const PRIORITY_OPTIONS = [
  { value: 'LOW', label: '낮음' },
  { value: 'MEDIUM', label: '보통' },
  { value: 'HIGH', label: '높음' },
  { value: 'CRITICAL', label: '긴급' },
]

const open = computed(() => !!props.date)

async function reposition() {
  await nextTick()
  const el = popoverRef.value
  if (!el) return
  const margin = 12
  const w = el.offsetWidth || 300
  const h = el.offsetHeight || 200
  let x = props.position?.x ?? 0
  let y = props.position?.y ?? 0
  x = Math.max(margin, Math.min(x, window.innerWidth - w - margin))
  if (y + h > window.innerHeight - margin) {
    const above = y - h - 8
    y = above >= margin ? above : Math.max(margin, window.innerHeight - h - margin)
  }
  y = Math.max(margin, y)
  posStyle.value = { left: x + 'px', top: y + 'px' }
}

function resetForm() {
  title.value = ''
  time.value = '10:00'
  priority.value = 'MEDIUM'
  selectedCampaignId.value = props.campaigns[0]?.id ?? null
  selectedMilestoneId.value = null
}

watch(open, async (v) => {
  if (!v) return
  step.value = 'pick'
  resetForm()
  await reposition()
})

watch(step, async (s) => {
  await reposition()
  if (s === 'task' || s === 'milestone' || s === 'taskpart') {
    if (s === 'taskpart') await loadMilestones()
    await nextTick()
    inputRef.value?.focus()
  }
})

watch(selectedCampaignId, () => {
  selectedMilestoneId.value = null
  if (step.value === 'taskpart') loadMilestones()
})

async function loadMilestones() {
  if (!selectedCampaignId.value) { milestones.value = []; return }
  loadingMs.value = true
  try {
    const ms = await ListMilestones(selectedCampaignId.value).catch(() => [])
    milestones.value = Array.isArray(ms) ? ms : []
  } finally {
    loadingMs.value = false
  }
}

function submit() {
  const t = title.value.trim()
  if (!t) { emit('close'); return }

  if (step.value === 'task') {
    // 개인 업무 — 캠페인 없음
    emit('create-task', { title: t, date: props.date, time: time.value || '23:59', priority: priority.value })
  } else if (step.value === 'milestone') {
    if (!selectedCampaignId.value) return
    emit('create-milestone', { title: t, date: props.date, campaignId: selectedCampaignId.value })
  } else if (step.value === 'taskpart') {
    if (!selectedCampaignId.value || !selectedMilestoneId.value) return
    emit('create-taskpart', {
      title: t,
      campaignId: selectedCampaignId.value,
      milestoneId: selectedMilestoneId.value,
      priority: priority.value,
    })
  }
  emit('close')
}

const canSubmit = computed(() => {
  if (!title.value.trim()) return false
  if (step.value === 'milestone') return !!selectedCampaignId.value
  if (step.value === 'taskpart') return !!selectedCampaignId.value && !!selectedMilestoneId.value
  return true   // 개인 업무
})
</script>

<template>
  <transition name="qa-fade">
    <div v-if="open" class="qa-backdrop" @click="emit('close')">
      <div ref="popoverRef" class="qa-popover" :style="posStyle" @click.stop>
        <div class="qa-popover__date">
          <span class="material-symbols-outlined">add_circle</span>
          {{ date }}
        </div>

        <!-- Step 1: 종류 선택 -->
        <div v-if="step === 'pick'" class="qa-pick">
          <button class="qa-pick__btn" @click="step = 'task'">
            <span class="qa-pick__emoji">✅</span>
            <span class="qa-pick__label">개인 업무</span>
            <span class="qa-pick__hint">캠페인 없이 나만의 to-do</span>
          </button>
          <button class="qa-pick__btn" @click="step = 'milestone'">
            <span class="qa-pick__emoji">🚩</span>
            <span class="qa-pick__label">마일스톤</span>
            <span class="qa-pick__hint">캠페인 단계</span>
          </button>
          <button class="qa-pick__btn" @click="step = 'taskpart'">
            <span class="qa-pick__emoji">🧩</span>
            <span class="qa-pick__label">업무파트</span>
            <span class="qa-pick__hint">마일스톤 하위 분류</span>
          </button>
        </div>

        <!-- Step 2: 입력 -->
        <div v-else class="qa-form">
          <button class="qa-form__back" @click="step = 'pick'">
            <span class="material-symbols-outlined">arrow_back</span>
            뒤로
          </button>

          <!-- 마일스톤/업무파트: 캠페인 선택 -->
          <label v-if="step !== 'task'" class="qa-form__field">
            <span class="qa-form__lbl">캠페인 <em>*</em></span>
            <select v-if="campaigns.length" v-model="selectedCampaignId" class="qa-form__select">
              <option v-for="c in campaigns" :key="c.id" :value="c.id">{{ c.title || c.name }}</option>
            </select>
            <div v-else class="qa-form__empty">먼저 캠페인을 만드세요</div>
          </label>

          <!-- 업무파트: 상위 마일스톤 -->
          <label v-if="step === 'taskpart'" class="qa-form__field">
            <span class="qa-form__lbl">상위 마일스톤 <em>*</em></span>
            <select v-model="selectedMilestoneId" class="qa-form__select">
              <option :value="null" disabled>마일스톤 선택…</option>
              <option v-for="m in milestones" :key="m.idx" :value="m.idx">{{ m.name }}</option>
            </select>
            <small v-if="!milestones.length && !loadingMs" class="qa-form__hint">이 캠페인엔 마일스톤이 없습니다. 먼저 마일스톤을 만드세요.</small>
          </label>

          <!-- 개인 업무: 시간 -->
          <label v-if="step === 'task'" class="qa-form__field">
            <span class="qa-form__lbl">시간</span>
            <input v-model="time" type="time" class="qa-form__input" />
          </label>

          <!-- 개인 업무 / 업무파트: 우선순위 -->
          <label v-if="step === 'task' || step === 'taskpart'" class="qa-form__field">
            <span class="qa-form__lbl">우선순위</span>
            <select v-model="priority" class="qa-form__select">
              <option v-for="p in PRIORITY_OPTIONS" :key="p.value" :value="p.value">{{ p.label }}</option>
            </select>
          </label>

          <!-- 제목 -->
          <label class="qa-form__field">
            <span class="qa-form__lbl">제목</span>
            <input
              ref="inputRef"
              v-model="title"
              type="text"
              :placeholder="step === 'task' ? '할 일 제목…' : step === 'milestone' ? '마일스톤 제목…' : '업무파트 제목…'"
              class="qa-form__input"
              @keydown.enter.prevent="submit"
              @keydown.esc.prevent="emit('close')"
            />
          </label>

          <div class="qa-form__actions">
            <button class="qa-btn qa-btn--ghost" @click="emit('close')">취소</button>
            <button class="qa-btn qa-btn--primary" :disabled="!canSubmit" @click="submit">추가</button>
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

.qa-form { display: flex; flex-direction: column; gap: 8px; max-height: 70vh; overflow-y: auto; }
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

.qa-form__field { display: flex; flex-direction: column; gap: 3px; }
.qa-form__lbl { font-size: 11px; font-weight: 700; color: var(--muted-text); }
.qa-form__lbl em { color: #df5f75; font-style: normal; }
.qa-form__hint { font-size: 10.5px; color: #df5f75; }

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
  box-sizing: border-box;
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
</style>
