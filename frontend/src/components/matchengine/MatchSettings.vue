<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AssetBenefitManagement from '@/components/matchengine/AssetBenefitManagement.vue'
import { CreateGoal, ListGoals } from '@/api/matchingGoals'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['asset-count-change', 'goal-count-change', 'request-matching'])

const goalTypes = [
  '신규 고객 유입',
  'VIP 혜택 강화',
  '객실 예약 증가',
  '앱 가입 증가',
  '멤버십 재방문',
  '브랜드 노출',
  '매출 증대',
]

const goalTypeValues = [
  'NEW_CUSTOMER',
  'VIP_BENEFIT',
  'ROOM_BOOKING',
  'APP_JOIN',
  'MEMBER_REVISIT',
  'BRAND_EXPOSURE',
  'REVENUE',
]

const goals = ref([
  {
    id: 1,
    name: '2026 Q2 VIP 리프레시',
    primaryType: 'VIP 혜택 강화',
    secondaryType: '멤버십 재방문',
    kpi: 'VIP 재방문율 +10%, 객단가 +5%',
    limit: '5,000만 원 · 100시간',
    period: '2026.05.01 ~ 2026.06.30',
    owner: '갤러리아 마케팅팀 김OO',
    weights: '수익성 30 · 공수 20 · 브랜드 50',
  },
  {
    id: 2,
    name: '앱 신규 가입 전환',
    primaryType: '앱 가입 증가',
    secondaryType: '신규 고객 유입',
    kpi: '신규 다운로드 30,000건, D7 잔존율 18%',
    limit: '3,000만 원 · 80시간',
    period: '2026.06.01 ~ 2026.07.15',
    owner: '디지털채널팀 박OO',
    weights: '수익성 35 · 공수 35 · 브랜드 30',
  },
])

const selectedGoalId = ref(goals.value[0].id)
const isAddingGoal = ref(false)
const form = ref({ ...createGoalForm(), primaryType: 'VIP_BENEFIT' })
const workspaceRef = ref(null)
const leftPanelPercent = ref(33)
const isResizing = ref(false)

const selectedGoal = computed(
  () => goals.value.find((goal) => goal.id === selectedGoalId.value) ?? goals.value[0],
)

const canRequestMatching = computed(() => Boolean(selectedGoal.value))

const canAddGoal = computed(() => {
  return (
    form.value.name.trim() &&
    form.value.primaryType &&
    form.value.kpi.trim() &&
    form.value.periodStart &&
    form.value.periodEnd &&
    form.value.periodEnd >= form.value.periodStart
  )
})

const workspaceStyle = computed(() => ({
  '--goal-panel-width': `${leftPanelPercent.value}%`,
}))

function createGoalForm() {
  return {
    name: '',
    primaryType: 'VIP 혜택 강화',
    secondaryType: '',
    kpi: '',
    limit: '',
    periodStart: '',
    periodEnd: '',
    owner: '',
    weights: '수익성 40 · 공수 30 · 브랜드 30',
  }
}

function toBackendGoalType(value) {
  if (goalTypeValues.includes(value)) return value

  const index = goalTypes.indexOf(value)
  return index >= 0 ? goalTypeValues[index] : value || null
}

function fromBackendGoalType(value) {
  const index = goalTypeValues.indexOf(value)
  return index >= 0 ? goalTypes[index] : value || ''
}

function createGoalPayload() {
  return {
    name: form.value.name,
    primaryType: toBackendGoalType(form.value.primaryType),
    secondaryType: form.value.secondaryType ? toBackendGoalType(form.value.secondaryType) : null,
    kpiPrimary: form.value.kpi,
    kpiSecondary: '',
    budgetLimit: form.value.limit,
    effortLimit: form.value.limit,
    periodStart: form.value.periodStart,
    periodEnd: form.value.periodEnd,
    weightRevenue: 40,
    weightEffort: 30,
    weightBrand: 30,
    ownerLabel: form.value.owner,
    status: 'ACTIVE',
  }
}

function mapGoal(goal) {
  return {
    id: goal.id ?? goal.idx,
    name: goal.name ?? '',
    primaryType: fromBackendGoalType(goal.primaryType),
    secondaryType: fromBackendGoalType(goal.secondaryType),
    kpi: [goal.kpiPrimary, goal.kpiSecondary].filter(Boolean).join(', '),
    limit: [goal.budgetLimit, goal.effortLimit].filter(Boolean).join(' · '),
    period: [goal.periodStart, goal.periodEnd].filter(Boolean).join(' ~ '),
    owner: goal.owner ?? goal.ownerLabel ?? '',
    weights: `${goal.weightRevenue ?? 0} / ${goal.weightEffort ?? 0} / ${goal.weightBrand ?? 0}`,
  }
}

async function loadGoals() {
  const data = await ListGoals()
  goals.value = (data.goalList ?? data ?? []).map(mapGoal)
  selectedGoalId.value = goals.value[0]?.id ?? null
  emit('goal-count-change', goals.value.length)
}

async function addGoal() {
  if (!canAddGoal.value) return

  await CreateGoal(createGoalPayload())
  await loadGoals()

  form.value = { ...createGoalForm(), primaryType: 'VIP_BENEFIT' }
  isAddingGoal.value = false
  emit('goal-count-change', goals.value.length)
}

function forwardAssetCount(count) {
  emit('asset-count-change', count)
}

function requestMatching() {
  if (!canRequestMatching.value) return
  emit('request-matching')
}

function startResize(event) {
  isResizing.value = true
  window.addEventListener('pointermove', resizePanels)
  window.addEventListener('pointerup', stopResize)
  resizePanels(event)
}

function resizePanels(event) {
  const rect = workspaceRef.value?.getBoundingClientRect()
  if (!rect) return

  const rawPercent = ((event.clientX - rect.left) / rect.width) * 100
  leftPanelPercent.value = Math.min(46, Math.max(22, Math.round(rawPercent)))
}

function stopResize() {
  isResizing.value = false
  window.removeEventListener('pointermove', resizePanels)
  window.removeEventListener('pointerup', stopResize)
}

onMounted(() => {
  loadGoals().catch(() => {
    emit('goal-count-change', goals.value.length)
  })
})

onBeforeUnmount(stopResize)
</script>

<template>
  <section
    ref="workspaceRef"
    class="settings-workspace"
    :class="{ resizing: isResizing }"
    :style="workspaceStyle"
  >
    <aside class="settings-goals">
      <header class="settings-goals__head">
        <div>
          <h3>목표</h3>
          <p>선택한 목표가 추천 점수의 기준이 됩니다.</p>
        </div>
        <b>{{ goals.length }}</b>
      </header>

      <div class="settings-goal-list">
        <button
          v-for="goal in goals"
          :key="goal.id"
          type="button"
          class="settings-goal-card"
          :class="{ active: selectedGoalId === goal.id }"
          @click="selectedGoalId = goal.id"
        >
          <strong>{{ goal.name }}</strong>
          <span>{{ goal.primaryType }}<template v-if="goal.secondaryType"> + {{ goal.secondaryType }}</template></span>
          <small>{{ goal.kpi }}</small>
        </button>
      </div>

      <button type="button" class="settings-add-goal" @click="isAddingGoal = !isAddingGoal">
        {{ isAddingGoal ? '목표 입력 닫기' : '+ 목표 추가' }}
      </button>

      <form v-if="isAddingGoal" class="settings-goal-form" @submit.prevent="addGoal">
        <label>
          <span>목표명</span>
          <input v-model="form.name" placeholder="예: 2026 Q3 객실 예약 증대" />
        </label>
        <label>
          <span>주 목표 유형</span>
          <select v-model="form.primaryType">
            <option v-for="(type, index) in goalTypes" :key="goalTypeValues[index]" :value="goalTypeValues[index]">
              {{ type }}
            </option>
          </select>
        </label>
        <label>
          <span>보조 목표</span>
          <select v-model="form.secondaryType">
            <option value="">선택 안 함</option>
            <option
              v-for="(type, index) in goalTypes"
              :key="goalTypeValues[index]"
              :value="goalTypeValues[index]"
              :disabled="goalTypeValues[index] === toBackendGoalType(form.primaryType)"
            >
              {{ type }}
            </option>
          </select>
        </label>
        <label>
          <span>핵심 KPI</span>
          <input v-model="form.kpi" placeholder="예: 추가 예약 300건, ADR 18만 원 유지" />
        </label>
        <label>
          <span>예산/공수 한도</span>
          <input v-model="form.limit" placeholder="예: 5,000만 원 · 100시간" />
        </label>
        <label>
          <span>기간</span>
          <div class="settings-date-range">
            <input v-model="form.periodStart" type="date" />
            <input v-model="form.periodEnd" type="date" :min="form.periodStart || undefined" />
          </div>
        </label>
        <label>
          <span>등록자</span>
          <input v-model="form.owner" placeholder="예: 갤러리아 마케팅팀 김OO" />
        </label>
        <button type="submit" :disabled="!canAddGoal">추가</button>
      </form>

      <section v-if="selectedGoal" class="settings-selected">
        <h4>선택 목표</h4>
        <dl>
          <div>
            <dt>KPI</dt>
            <dd>{{ selectedGoal.kpi }}</dd>
          </div>
          <div>
            <dt>한도</dt>
            <dd>{{ selectedGoal.limit }}</dd>
          </div>
          <div>
            <dt>기간</dt>
            <dd>{{ selectedGoal.period }}</dd>
          </div>
          <div>
            <dt>가중치</dt>
            <dd>{{ selectedGoal.weights }}</dd>
          </div>
        </dl>
      </section>

      <button
        type="button"
        class="settings-request"
        :disabled="!canRequestMatching"
        @click="requestMatching"
      >
        매칭 추천 받기
      </button>
    </aside>

    <button
      type="button"
      class="settings-resizer"
      aria-label="목표와 자산 영역 크기 조절"
      title="좌우 영역 크기 조절"
      @pointerdown.prevent="startResize"
    >
      <span />
    </button>

    <section class="settings-assets">
      <AssetBenefitManagement :isDark="isDark" @asset-count-change="forwardAssetCount" />
    </section>
  </section>
</template>

<style scoped>
.settings-workspace {
  display: grid;
  grid-template-columns: minmax(15rem, var(--goal-panel-width, 33%)) 0.7rem minmax(0, 1fr);
  gap: 0.45rem;
  height: 100%;
  min-height: 0;
}

.settings-workspace.resizing {
  cursor: col-resize;
  user-select: none;
}

.settings-goals,
.settings-assets {
  min-height: 0;
}

.settings-goals {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  overflow-y: auto;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.85rem;
}

.settings-resizer {
  display: flex;
  width: 0.7rem;
  min-width: 0.7rem;
  height: 100%;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  background: transparent;
  cursor: col-resize;
  padding: 0;
}

.settings-resizer span {
  display: block;
  width: 3px;
  height: 3.4rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--border-strong) 78%, transparent);
  transition:
    width 0.15s ease,
    background 0.15s ease;
}

.settings-resizer:hover span,
.settings-workspace.resizing .settings-resizer span {
  width: 4px;
  background: var(--accent-color);
}

.settings-goals__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.8rem;
}

.settings-goals__head h3,
.settings-selected h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 900;
}

.settings-goals__head p {
  margin: 0.16rem 0 0;
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 700;
}

.settings-goals__head b {
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: var(--accent-color);
  padding: 0.24rem 0.58rem;
  font-size: 0.7rem;
  font-weight: 900;
}

.settings-goal-list {
  display: grid;
  gap: 0.45rem;
}

.settings-goal-card {
  display: grid;
  gap: 0.22rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.7rem;
  cursor: pointer;
  text-align: left;
}

.settings-goal-card.active {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-color));
  box-shadow: inset 3px 0 0 var(--accent-color);
}

.settings-goal-card strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-goal-card span,
.settings-goal-card small {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.68rem;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-add-goal,
.settings-request,
.settings-goal-form button {
  min-height: 2.35rem;
  border-radius: 7px;
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
}

.settings-add-goal {
  align-self: flex-start;
  min-height: 2rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  padding: 0 0.75rem;
}

.settings-add-goal:focus,
.settings-add-goal:focus-visible {
  outline: none;
  border-color: color-mix(in srgb, var(--accent-color) 42%, var(--border-color));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 12%, transparent);
}

.settings-goal-form {
  display: grid;
  gap: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.7rem;
  margin-top: -0.15rem;
}

.settings-goal-form label {
  display: grid;
  gap: 0.28rem;
}

.settings-goal-form span {
  color: var(--text-primary);
  font-size: 0.7rem;
  font-weight: 900;
}

.settings-goal-form input,
.settings-goal-form select {
  height: 2.25rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0 0.65rem;
  font-size: 0.76rem;
  font-weight: 750;
}

.settings-goal-form input:focus,
.settings-goal-form select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 16%, transparent);
}

.settings-date-range {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.4rem;
}

.settings-goal-form button,
.settings-request {
  border: 1px solid var(--accent-color);
  background: var(--accent-color);
  color: #fff;
}

.settings-goal-form button:disabled,
.settings-request:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.settings-selected {
  display: grid;
  gap: 0.55rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.7rem;
}

.settings-selected dl {
  display: grid;
  gap: 0.42rem;
  margin: 0;
}

.settings-selected dl > div {
  display: grid;
  grid-template-columns: 3.6rem minmax(0, 1fr);
  gap: 0.45rem;
}

.settings-selected dt,
.settings-selected dd {
  margin: 0;
  font-size: 0.7rem;
}

.settings-selected dt {
  color: var(--muted-text);
  font-weight: 900;
}

.settings-selected dd {
  overflow: hidden;
  color: var(--text-secondary);
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-assets {
  min-width: 0;
}

.settings-assets :deep(.asset-panel) {
  height: 100%;
}

.settings-assets :deep(.asset-panel__title p) {
  display: none;
}

@media (max-width: 1180px) {
  .settings-workspace {
    grid-template-columns: minmax(0, 1fr);
  }

  .settings-resizer {
    display: none;
  }
}
</style>
