<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref } from 'vue'
import { ListBenefits } from '@/api/matchingBenefits'
import { CreateGoal, ListGoals } from '@/api/matchingGoals'

const AssetBenefitManagement = defineAsyncComponent(() =>
  import('@/components/matchengine/AssetBenefitManagement.vue'),
)

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['asset-count-change', 'goal-count-change', 'request-matching'])

const goalTypes = [
  '신규 고객 유입',
  '기존 고객 재방문',
  '회원 가입 유도',
  '구매/예약 유도',
  '브랜드 인지도 확대',
  '매출 증대',
  '객단가/업셀 향상',
  '직접예약 비중 확대',
  '리뷰/평판 개선',
  '기타',
]

const goalTypeValues = [
  'NEW_CUSTOMER',
  'CUSTOMER_REVISIT',
  'MEMBER_SIGNUP',
  'PURCHASE_BOOKING',
  'BRAND_AWARENESS',
  'REVENUE',
  'UPSELL',
  'DIRECT_BOOKING',
  'REVIEW_REPUTATION',
  'OTHER',
]

const campaignMethods = [
  '쿠폰/할인 혜택',
  '체험권/사은품 제공',
  '멤버십·로열티 강화',
  '공동 프로모션',
  '콘텐츠 협업',
  '채널/앱 프로모션',
  '기타',
]

const campaignMethodValues = [
  'COUPON_DISCOUNT',
  'TRIAL_GIFT',
  'MEMBERSHIP_LOYALTY',
  'JOINT_PROMOTION',
  'CONTENT_COLLABORATION',
  'CHANNEL_APP_PROMOTION',
  'OTHER',
]

const goals = ref([
  {
    id: 1,
    name: '2026 Q2 업셀 프로모션',
    primaryType: '객단가/업셀 향상',
    secondaryType: '기존 고객 재방문',
    campaignMethod: '멤버십·로열티 강화',
    kpi: '객단가 +5%, 재방문율 +10%',
    limit: '5,000만 원 · 100시간',
    period: '2026.05.01 ~ 2026.06.30',
    owner: '제휴마케팅팀 김OO',
    weights: '수익성 30 · 공수 20 · 브랜드 50',
  },
  {
    id: 2,
    name: '신규 회원 가입 캠페인',
    primaryType: '회원 가입 유도',
    secondaryType: '신규 고객 유입',
    campaignMethod: '채널/앱 프로모션',
    kpi: '신규 가입 30,000건, D7 잔존율 18%',
    limit: '3,000만 원 · 80시간',
    period: '2026.06.01 ~ 2026.07.15',
    owner: '디지털채널팀 박OO',
    weights: '수익성 35 · 공수 35 · 브랜드 30',
  },
])

const selectedGoalId = ref(goals.value[0].id)
const isAddingGoal = ref(false)
const form = ref({ ...createGoalForm(), primaryType: 'NEW_CUSTOMER' })
const isRecommendationModalOpen = ref(false)
const isPartnerFilterOpen = ref(false)
const benefitOptions = ref([])
const recommendationForm = ref(createRecommendationForm())
const workspaceRef = ref(null)
const leftPanelPercent = ref(33)
const isResizing = ref(false)
const showAssetBenefitPanel = ref(false)
const isGoalLoading = ref(false)
const goalError = ref('')

const selectedGoal = computed(
  () => goals.value.find((goal) => goal.id === selectedGoalId.value) ?? goals.value[0],
)

const canRequestMatching = computed(() => Boolean(selectedGoal.value))
const canCreateRecommendation = computed(() => Boolean(recommendationForm.value.goalType))

const canAddGoal = computed(() => {
  return (
    form.value.name.trim() &&
    form.value.primaryType &&
    form.value.campaignMethod &&
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
    primaryType: '신규 고객 유입',
    secondaryType: '',
    campaignMethod: 'COUPON_DISCOUNT',
    kpi: '',
    limit: '',
    periodStart: '',
    periodEnd: '',
    owner: '',
    weights: '수익성 40 · 공수 30 · 브랜드 30',
  }
}

function createRecommendationForm() {
  return {
    goalIdx: null,
    goalType: 'NEW_CUSTOMER',
    campaignMethods: [],
    benefitIds: [],
    sortType: 'HIGH_SCORE',
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

function toBackendCampaignMethod(value) {
  if (campaignMethodValues.includes(value)) return value

  const index = campaignMethods.indexOf(value)
  return index >= 0 ? campaignMethodValues[index] : value || null
}

function fromBackendCampaignMethod(value) {
  const index = campaignMethodValues.indexOf(value)
  return index >= 0 ? campaignMethods[index] : value || ''
}

function createGoalPayload() {
  return {
    name: form.value.name,
    primaryType: toBackendGoalType(form.value.primaryType),
    secondaryType: form.value.secondaryType ? toBackendGoalType(form.value.secondaryType) : null,
    campaignMethod: toBackendCampaignMethod(form.value.campaignMethod),
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
    campaignMethod: fromBackendCampaignMethod(goal.campaignMethod),
    kpi: [goal.kpiPrimary, goal.kpiSecondary].filter(Boolean).join(', '),
    limit: [goal.budgetLimit, goal.effortLimit].filter(Boolean).join(' · '),
    period: [goal.periodStart, goal.periodEnd].filter(Boolean).join(' ~ '),
    owner: goal.owner ?? goal.ownerLabel ?? '',
    weights: `${goal.weightRevenue ?? 0} / ${goal.weightEffort ?? 0} / ${goal.weightBrand ?? 0}`,
  }
}

function mapBenefit(benefit) {
  return {
    id: benefit.id ?? benefit.idx,
    name: benefit.name ?? '-',
    partner: benefit.partnerName ?? benefit.affiliate ?? '-',
    type: benefit.type ?? '-',
    scale: benefit.scale ?? '',
  }
}

function useFallbackBenefits() {
  benefitOptions.value = [
    { id: 1, name: '쿠폰/할인 혜택', partner: '파트너사 A', type: '할인권', scale: '10,000건' },
    { id: 2, name: '체험권 제공', partner: '파트너사 B', type: '체험권', scale: '500건' },
    { id: 3, name: '공동 콘텐츠 제작', partner: '파트너사 C', type: '공동 콘텐츠', scale: '콘텐츠 3종' },
  ]
}

async function loadGoals() {
  isGoalLoading.value = true
  goalError.value = ''

  try {
    const data = await ListGoals()
    const loadedGoals = (data.goalList ?? data ?? []).map(mapGoal)
    if (loadedGoals.length) {
      goals.value = loadedGoals
      selectedGoalId.value = loadedGoals[0]?.id ?? null
    }
    emit('goal-count-change', goals.value.length)
  } catch (error) {
    goalError.value = error.message ?? '목표 목록을 불러오지 못했습니다.'
    emit('goal-count-change', goals.value.length)
  } finally {
    isGoalLoading.value = false
  }
}

async function loadBenefits() {
  if (benefitOptions.value.length) return

  try {
    const data = await ListBenefits()
    const list = data.benefitList ?? data ?? []
    benefitOptions.value = list.map(mapBenefit).filter((benefit) => benefit.id)
    if (!benefitOptions.value.length) useFallbackBenefits()
  } catch {
    useFallbackBenefits()
  }
}

async function addGoal() {
  if (!canAddGoal.value) return

  await CreateGoal(createGoalPayload())
  await loadGoals()

  form.value = { ...createGoalForm(), primaryType: 'NEW_CUSTOMER' }
  isAddingGoal.value = false
  emit('goal-count-change', goals.value.length)
}

function forwardAssetCount(count) {
  emit('asset-count-change', count)
}

function openAssetBenefitPanel() {
  showAssetBenefitPanel.value = true
}

function requestMatching() {
  if (!canRequestMatching.value) return
  recommendationForm.value = {
    goalIdx: selectedGoal.value?.id ?? null,
    goalType: toBackendGoalType(selectedGoal.value?.primaryType) ?? 'NEW_CUSTOMER',
    campaignMethods: selectedGoal.value?.campaignMethod
      ? [toBackendCampaignMethod(selectedGoal.value.campaignMethod)].filter(Boolean)
      : [],
    benefitIds: [],
    sortType: 'HIGH_SCORE',
  }
  isPartnerFilterOpen.value = false
  isRecommendationModalOpen.value = true
  loadBenefits()
}

function closeRecommendationModal() {
  isRecommendationModalOpen.value = false
}

function createRecommendation() {
  if (!canCreateRecommendation.value) return

  emit('request-matching', {
    ...recommendationForm.value,
  })
  closeRecommendationModal()
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
  emit('goal-count-change', goals.value.length)
})

onBeforeUnmount(() => {
  stopResize()
})
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
        <div class="settings-goals__tools">
          <b>{{ goals.length }}</b>
          <button type="button" :disabled="isGoalLoading" @click="loadGoals">
            {{ isGoalLoading ? '불러오는 중' : '목록 새로고침' }}
          </button>
        </div>
      </header>

      <p v-if="goalError" class="settings-message">{{ goalError }}</p>

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
          <span v-if="goal.campaignMethod">{{ goal.campaignMethod }}</span>
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
          <span>캠페인 방식</span>
          <select v-model="form.campaignMethod">
            <option
              v-for="(method, index) in campaignMethods"
              :key="campaignMethodValues[index]"
              :value="campaignMethodValues[index]"
            >
              {{ method }}
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
            <dt>방식</dt>
            <dd>{{ selectedGoal.campaignMethod || '-' }}</dd>
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
      <AssetBenefitManagement
        v-if="showAssetBenefitPanel"
        :isDark="isDark"
        @asset-count-change="forwardAssetCount"
      />
      <div v-else class="settings-assets__placeholder">
        <strong>자산/혜택 영역</strong>
        <span>필요할 때 불러오면 설정 탭 진입이 가벼워집니다.</span>
        <button type="button" @click="openAssetBenefitPanel">자산/혜택 불러오기</button>
      </div>
    </section>
  </section>

  <div v-if="isRecommendationModalOpen" class="recommend-modal" role="dialog" aria-modal="true">
    <div class="recommend-modal__panel">
      <header class="recommend-modal__head">
        <div>
          <h3>추천 조건 선택</h3>
          <p>목표와 파트너 혜택을 기준으로 추천 조합을 생성합니다.</p>
        </div>
        <button type="button" aria-label="닫기" @click="closeRecommendationModal">×</button>
      </header>

      <label class="recommend-field">
        <span>목표 유형</span>
        <select v-model="recommendationForm.goalType">
          <option v-for="(type, index) in goalTypes" :key="goalTypeValues[index]" :value="goalTypeValues[index]">
            {{ type }}
          </option>
        </select>
      </label>

      <label class="recommend-field">
        <span>캠페인 방식</span>
        <div class="recommend-methods">
          <label v-for="(method, index) in campaignMethods" :key="campaignMethodValues[index]">
            <input v-model="recommendationForm.campaignMethods" type="checkbox" :value="campaignMethodValues[index]" />
            <span>{{ method }}</span>
          </label>
        </div>
      </label>

      <section class="recommend-field recommend-filter">
        <button type="button" class="recommend-filter__toggle" @click="isPartnerFilterOpen = !isPartnerFilterOpen">
          <span>파트너 필터</span>
          <strong>{{ isPartnerFilterOpen ? '접기' : '선택 사항' }}</strong>
        </button>
        <p>선택하지 않으면 전체 파트너와 혜택을 기준으로 추천합니다.</p>
        <div v-if="isPartnerFilterOpen" class="recommend-benefits">
          <label v-for="benefit in benefitOptions" :key="benefit.id" class="recommend-benefit">
            <input v-model="recommendationForm.benefitIds" type="checkbox" :value="benefit.id" />
            <strong>{{ benefit.name }}</strong>
            <small>{{ benefit.partner }} · {{ benefit.type }}<template v-if="benefit.scale"> · {{ benefit.scale }}</template></small>
          </label>
        </div>
      </section>

      <label class="recommend-field">
        <span>추천 방식</span>
        <select v-model="recommendationForm.sortType">
          <option value="HIGH_SCORE">점수 높은 순</option>
          <option value="LOW_EFFORT">운영 쉬운 순</option>
          <option value="BRAND_FIT">브랜드 적합도 높은 순</option>
        </select>
      </label>

      <footer class="recommend-modal__actions">
        <button type="button" class="recommend-secondary" @click="closeRecommendationModal">취소</button>
        <button type="button" class="recommend-primary" :disabled="!canCreateRecommendation" @click="createRecommendation">
          추천 조합 생성
        </button>
      </footer>
    </div>
  </div>
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

.settings-goals__tools {
  display: grid;
  justify-items: end;
  gap: 0.35rem;
}

.settings-goals__tools button {
  min-height: 1.8rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  padding: 0 0.6rem;
  font-size: 0.68rem;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.settings-goals__tools button:disabled {
  cursor: wait;
  opacity: 0.58;
}

.settings-message {
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--color-warning-dark, #b45309);
  padding: 0.55rem 0.65rem;
  font-size: 0.72rem;
  font-weight: 800;
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

.settings-assets__placeholder {
  display: grid;
  height: 100%;
  min-height: 14rem;
  align-content: center;
  gap: 0.55rem;
  justify-items: center;
  place-items: center;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 1rem;
  color: var(--muted-text);
  text-align: center;
}

.settings-assets__placeholder strong {
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
}

.settings-assets__placeholder span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 750;
}

.settings-assets__placeholder button {
  min-height: 2.25rem;
  border: 1px solid var(--accent-color);
  border-radius: 7px;
  background: var(--accent-color);
  color: #fff;
  padding: 0 0.85rem;
  font-size: 0.76rem;
  font-weight: 900;
  cursor: pointer;
}

.settings-assets :deep(.asset-panel) {
  height: 100%;
}

.settings-assets :deep(.asset-panel__title p) {
  display: none;
}

.recommend-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  background: rgb(15 23 42 / 46%);
  padding: 1rem;
}

.recommend-modal__panel {
  width: min(34rem, 100%);
  max-height: min(42rem, calc(100vh - 2rem));
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  box-shadow: 0 1.4rem 3rem rgb(15 23 42 / 22%);
  padding: 1rem;
}

.recommend-modal__head,
.recommend-modal__actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.recommend-modal__head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 900;
}

.recommend-modal__head p {
  margin: 0.2rem 0 0;
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 700;
}

.recommend-modal__head button {
  width: 2rem;
  height: 2rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
}

.recommend-field {
  display: grid;
  gap: 0.42rem;
  margin-top: 0.85rem;
}

.recommend-field > span {
  color: var(--text-primary);
  font-size: 0.72rem;
  font-weight: 900;
}

.recommend-field select {
  height: 2.35rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0 0.75rem;
  font-size: 0.78rem;
  font-weight: 750;
}

.recommend-methods {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem;
}

.recommend-methods label {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.42rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.55rem 0.6rem;
  cursor: pointer;
}

.recommend-methods span {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-filter {
  border-top: 1px solid var(--border-color);
  padding-top: 0.85rem;
}

.recommend-filter > p {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 700;
}

.recommend-filter__toggle {
  display: flex;
  width: 100%;
  min-height: 2.35rem;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--text-primary);
  cursor: pointer;
  padding: 0 0.75rem;
}

.recommend-filter__toggle span {
  font-size: 0.78rem;
  font-weight: 900;
}

.recommend-filter__toggle strong {
  color: var(--accent-color);
  font-size: 0.68rem;
  font-weight: 900;
}

.recommend-benefits {
  display: grid;
  gap: 0.45rem;
}

.recommend-benefit {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 0.2rem 0.55rem;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.65rem;
  cursor: pointer;
}

.recommend-benefit input {
  grid-row: span 2;
}

.recommend-benefit strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-benefit small {
  overflow: hidden;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-modal__actions {
  align-items: center;
  margin-top: 1rem;
}

.recommend-secondary,
.recommend-primary {
  min-height: 2.35rem;
  border-radius: 7px;
  padding: 0 0.9rem;
  cursor: pointer;
  font-size: 0.78rem;
  font-weight: 900;
}

.recommend-secondary {
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  color: var(--text-secondary);
}

.recommend-primary {
  border: 1px solid var(--accent-color);
  background: var(--accent-color);
  color: #fff;
}

.recommend-primary:disabled {
  cursor: not-allowed;
  opacity: 0.48;
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
