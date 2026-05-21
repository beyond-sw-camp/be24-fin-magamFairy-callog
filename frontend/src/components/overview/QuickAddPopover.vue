<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ListMilestones, ListTaskParts } from '@/api/teamboard'

const props = defineProps({
  date: { type: String, default: '' },
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
  campaigns: { type: Array, default: () => [] },  // 마일스톤/업무파트용 캠페인 옵션
})
const emit = defineEmits([
  'close',
  'create-task',
  'create-campaign-task',
  'create-milestone',
  'create-taskpart',
])

// 개인 업무: 캠페인 없음 / 마일스톤·업무파트: 캠페인 필요
const step = ref('pick')   // 'pick' | 'task' | 'milestone' | 'taskpart'
const title = ref('')
const dateValue = ref('')   // 날짜 (수정 가능, 기본 = 클릭한 셀 날짜)
const startTime = ref('09:00')
const endTime = ref('10:00')
const priority = ref('MEDIUM')
const isPersonal = ref(true)            // 업무: 개인 / 캠페인 구분
const selectedCampaignId = ref(null)
const selectedMilestoneId = ref(null)
const selectedTaskPartId = ref(null)
const milestones = ref([])
const taskParts = ref([])
const loadingMs = ref(false)
const loadingParts = ref(false)
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
  dateValue.value = props.date || ''
  startTime.value = '09:00'
  endTime.value = '10:00'
  priority.value = 'MEDIUM'
  isPersonal.value = true
  selectedCampaignId.value = props.campaigns[0]?.id ?? null
  selectedMilestoneId.value = null
  selectedTaskPartId.value = null
  milestones.value = []
  taskParts.value = []
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
    if (s === 'task' && !isPersonal.value) await loadCampaignChildren()
    await nextTick()
    inputRef.value?.focus()
  }
})

watch(selectedCampaignId, () => {
  selectedMilestoneId.value = null
  selectedTaskPartId.value = null
  if (step.value === 'taskpart') loadMilestones()
  if (step.value === 'task' && !isPersonal.value) loadCampaignChildren()
})

// 개인 ↔ 캠페인 전환 시 캠페인 하위 옵션 로드
watch(isPersonal, async (personal) => {
  await reposition()
  if (!personal && step.value === 'task') await loadCampaignChildren()
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

// 캠페인 업무용 — 마일스톤 + 업무파트 동시 로드 (둘 다 선택 옵션)
async function loadCampaignChildren() {
  if (!selectedCampaignId.value) { milestones.value = []; taskParts.value = []; return }
  loadingMs.value = true
  loadingParts.value = true
  try {
    const [ms, parts] = await Promise.all([
      ListMilestones(selectedCampaignId.value).catch(() => []),
      ListTaskParts(selectedCampaignId.value).catch(() => []),
    ])
    milestones.value = Array.isArray(ms) ? ms : []
    taskParts.value = Array.isArray(parts) ? parts : []
  } finally {
    loadingMs.value = false
    loadingParts.value = false
  }
}

function submit() {
  const t = title.value.trim()
  if (!t) { emit('close'); return }

  const date = dateValue.value || props.date
  // 마감 시간이 시작보다 빠르면 시작과 동일하게 보정
  const start = startTime.value || '09:00'
  const end = (endTime.value && endTime.value >= start) ? endTime.value : start

  if (step.value === 'task') {
    if (isPersonal.value) {
      // 개인 업무 — 캠페인 없음
      emit('create-task', { title: t, date, startTime: start, endTime: end, priority: priority.value })
    } else {
      // 캠페인 업무 — 마일스톤/업무파트는 선택 사항
      if (!selectedCampaignId.value) return
      emit('create-campaign-task', {
        title: t,
        date,
        startTime: start,
        endTime: end,
        priority: priority.value,
        campaignId: selectedCampaignId.value,
        milestoneId: selectedMilestoneId.value ?? null,
        taskPartId: selectedTaskPartId.value ?? null,
      })
    }
  } else if (step.value === 'milestone') {
    if (!selectedCampaignId.value) return
    emit('create-milestone', { title: t, date, campaignId: selectedCampaignId.value })
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
  const hasDate = !!(dateValue.value || props.date)
  if (step.value === 'task') return hasDate && (isPersonal.value || !!selectedCampaignId.value)
  if (step.value === 'milestone') return hasDate && !!selectedCampaignId.value
  if (step.value === 'taskpart') return !!selectedCampaignId.value && !!selectedMilestoneId.value
  return true
})
</script>

<template>
  <transition name="qa-fade">
    <div v-if="open" class="qa-backdrop" @click="emit('close')">
      <div ref="popoverRef" class="qa-popover" :style="posStyle" @click.stop>
        <div class="qa-popover__date">
          <span class="material-symbols-outlined">add_circle</span>
          {{ dateValue || date }}
        </div>

        <!-- Step 1: 종류 선택 -->
        <div v-if="step === 'pick'" class="qa-pick">
          <button class="qa-pick__btn" @click="step = 'task'">
            <span class="qa-pick__emoji">✅</span>
            <span class="qa-pick__label">업무</span>
            <span class="qa-pick__hint">개인 to-do 또는 캠페인 업무</span>
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

          <!-- 업무: 개인 / 캠페인 구분 토글 -->
          <div v-if="step === 'task'" class="qa-form__field">
            <span class="qa-form__lbl">구분</span>
            <div class="qa-seg">
              <button
                type="button"
                class="qa-seg__btn"
                :class="{ 'qa-seg__btn--on': isPersonal }"
                @click="isPersonal = true"
              >개인 업무</button>
              <button
                type="button"
                class="qa-seg__btn"
                :class="{ 'qa-seg__btn--on': !isPersonal }"
                @click="isPersonal = false"
              >캠페인 업무</button>
            </div>
          </div>

          <!-- 캠페인 업무 / 마일스톤 / 업무파트: 캠페인 선택 (필수) -->
          <label v-if="step !== 'task' || !isPersonal" class="qa-form__field">
            <span class="qa-form__lbl">캠페인 <em>*</em></span>
            <select v-if="campaigns.length" v-model="selectedCampaignId" class="qa-form__select">
              <option v-for="c in campaigns" :key="c.id" :value="c.id">{{ c.title || c.name }}</option>
            </select>
            <div v-else class="qa-form__empty">먼저 캠페인을 만드세요</div>
          </label>

          <!-- 캠페인 업무: 마일스톤 (선택) -->
          <label v-if="step === 'task' && !isPersonal" class="qa-form__field">
            <span class="qa-form__lbl">마일스톤 <small class="qa-form__opt">(선택)</small></span>
            <select v-model="selectedMilestoneId" class="qa-form__select">
              <option :value="null">선택 안 함</option>
              <option v-for="m in milestones" :key="m.idx" :value="m.idx">{{ m.name }}</option>
            </select>
          </label>

          <!-- 캠페인 업무: 업무파트 (선택) -->
          <label v-if="step === 'task' && !isPersonal" class="qa-form__field">
            <span class="qa-form__lbl">업무파트 <small class="qa-form__opt">(선택)</small></span>
            <select v-model="selectedTaskPartId" class="qa-form__select">
              <option :value="null">선택 안 함</option>
              <option v-for="p in taskParts" :key="p.idx" :value="p.idx">{{ p.name }}</option>
            </select>
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

          <!-- 업무 / 마일스톤: 날짜 -->
          <label v-if="step === 'task' || step === 'milestone'" class="qa-form__field">
            <span class="qa-form__lbl">날짜 <em>*</em></span>
            <input v-model="dateValue" type="date" class="qa-form__input" />
          </label>

          <!-- 업무: 시작 / 마감 시간 -->
          <div v-if="step === 'task'" class="qa-form__field">
            <span class="qa-form__lbl">시간 <small class="qa-form__opt">(시작 ~ 마감)</small></span>
            <div class="qa-time-row">
              <input v-model="startTime" type="time" class="qa-form__input" aria-label="시작 시간" />
              <span class="qa-time-sep">~</span>
              <input v-model="endTime" type="time" class="qa-form__input" aria-label="마감 시간" />
            </div>
          </div>

          <!-- 업무 / 업무파트: 우선순위 -->
          <label v-if="step === 'task' || step === 'taskpart'" class="qa-form__field">
            <span class="qa-form__lbl">우선순위</span>
            <select v-model="priority" class="qa-form__select">
              <option v-for="p in PRIORITY_OPTIONS" :key="p.value" :value="p.value">{{ p.label }}</option>
            </select>
          </label>

          <!-- 제목 -->
          <label class="qa-form__field">
            <span class="qa-form__lbl">제목 <em>*</em></span>
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
.qa-form__lbl em { color: var(--lp-primary, #8B5CF6); font-style: normal; font-weight: 800; }
.qa-form__opt { font-size: 10px; font-weight: 600; color: var(--muted-text); opacity: 0.7; }
.qa-time-row { display: flex; align-items: center; gap: 8px; }
.qa-time-row .qa-form__input { flex: 1; min-width: 0; }
.qa-time-sep { color: var(--muted-text); font-weight: 700; flex-shrink: 0; }
.qa-form__hint { font-size: 10.5px; color: #df5f75; }

/* 개인/캠페인 구분 세그먼트 토글 */
.qa-seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 3px;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}
.qa-seg__btn {
  padding: 7px 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muted-text);
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, box-shadow 0.15s;
}
.qa-seg__btn--on {
  background: var(--accent-color, #8B5CF6);
  color: #fff;
  box-shadow: 0 1px 4px rgba(94, 106, 210, 0.25);
}

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
