<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref } from 'vue'
import { ListBenefits } from '@/api/matchingBenefits'
import { CreateAsset } from '@/api/matchingAssets'
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
  '쿠폰/할인',
  '체험권/사은품',
  '멤버십 혜택',
  '공동 프로모션',
  '콘텐츠 협업',
  '채널/앱 노출',
]

const campaignMethodValues = [
  'COUPON_DISCOUNT',
  'TRIAL_GIFT',
  'MEMBERSHIP_LOYALTY',
  'JOINT_PROMOTION',
  'CONTENT_COLLABORATION',
  'CHANNEL_APP_PROMOTION',
]

const partnerCategories = [
  '패션/뷰티',
  'F&B',
  '카드/금융',
  '여행/항공',
  '엔터테인먼트',
  '리빙/홈',
  '디지털/IT',
  '자동차',
]

const goals = ref([])

const selectedGoalId = ref(null)
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
const assetPanelKey = ref(0)
let assetPanelFrame = null

const selectedGoal = computed(
  () => goals.value.find((goal) => goal.id === selectedGoalId.value) ?? goals.value[0],
)

const canRequestMatching = computed(() => Boolean(selectedGoal.value))
const canCreateRecommendation = computed(() => Boolean(recommendationForm.value.goalType))

const canAddGoal = computed(() => {
  return (
    form.value.assetName.trim() &&
    form.value.primaryType &&
    form.value.periodStart &&
    form.value.periodEnd &&
    form.value.periodEnd >= form.value.periodStart &&
    form.value.ownerName.trim() &&
    form.value.ownerEmail.trim()
  )
})

const workspaceStyle = computed(() => ({
  '--goal-panel-width': `${leftPanelPercent.value}%`,
}))

function createGoalForm() {
  return {
    assetName: '',
    assetDescription: '',
    name: '',
    primaryType: '신규 고객 유입',
    campaignMethods: [],
    periodStart: '',
    periodEnd: '',
    maxCost: '',
    minRevenue: '',
    partnerFit: [],
    partnerOther: '',
    ownerName: '',
    ownerEmail: '',
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
  const selectedMethods = form.value.campaignMethods.map(toBackendCampaignMethod).filter(Boolean)
  const financeNotes = [
    form.value.maxCost ? `최대 부담 비용 ${form.value.maxCost}원` : '',
    form.value.minRevenue ? `최소 기대 매출 ${form.value.minRevenue}원` : '',
  ].filter(Boolean)

  return {
    name: form.value.name || `${form.value.assetName} 매칭 캠페인`,
    primaryType: toBackendGoalType(form.value.primaryType),
    secondaryType: null,
    campaignMethod: selectedMethods[0] ?? null,
    kpiPrimary: financeNotes.join(' · '),
    kpiSecondary: '',
    budgetLimit: form.value.maxCost,
    effortLimit: '',
    periodStart: form.value.periodStart,
    periodEnd: form.value.periodEnd,
    weightRevenue: 40,
    weightEffort: 30,
    weightBrand: 30,
    ownerLabel: `${form.value.ownerName} · ${form.value.ownerEmail}`,
    status: 'ACTIVE',
  }
}

function createAssetPayload() {
  const partnerFit = [...form.value.partnerFit]
  const partnerOther = form.value.partnerOther.trim()
  if (partnerOther) partnerFit.push(partnerOther)

  return {
    type: form.value.assetName,
    affiliate: form.value.ownerName,
    registeredAt: new Date().toISOString().slice(0, 10),
    category: 'customer',
    target: form.value.assetDescription,
    scale: form.value.assetDescription,
    exposureValue: '',
    performance: '',
    conditions: form.value.campaignMethods.map(fromBackendCampaignMethod).filter(Boolean).join(', '),
    partnerFit,
    blockedPartners: [],
    supplyLimit: [form.value.periodStart, form.value.periodEnd].filter(Boolean).join(' ~ '),
    publicStatus: 'PUBLIC',
    matchingStatus: 'ACTIVE',
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

  await CreateAsset(createAssetPayload())
  await CreateGoal(createGoalPayload())
  await loadGoals()

  form.value = { ...createGoalForm(), primaryType: 'NEW_CUSTOMER' }
  isAddingGoal.value = false
  assetPanelKey.value += 1
  emit('goal-count-change', goals.value.length)
}

function saveGoalDraft() {
  isAddingGoal.value = false
}

function toggleMultiValue(field, value) {
  const values = form.value[field]
  const index = values.indexOf(value)
  if (index > -1) values.splice(index, 1)
  else values.push(value)
}

function isMultiSelected(field, value) {
  return form.value[field].includes(value)
}

function forwardAssetCount(count) {
  emit('asset-count-change', count)
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

  assetPanelFrame = window.requestAnimationFrame(() => {
    showAssetBenefitPanel.value = true
  })
})

onBeforeUnmount(() => {
  stopResize()
  if (assetPanelFrame !== null) window.cancelAnimationFrame(assetPanelFrame)
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
        {{ isAddingGoal ? '매칭 폼 닫기' : '+ 캠페인 매칭 시작하기' }}
      </button>

      <form v-if="isAddingGoal" class="settings-goal-form" @submit.prevent="addGoal">
        <header class="match-start-head">
          <strong>캠페인 매칭 시작하기</strong>
        </header>

        <section class="form-section">
          <div class="form-section__head">
            <span class="form-section__num">1</span>
            <strong class="form-section__title">무엇을 활용할 건가요? <em>자산</em></strong>
          </div>
          <label>
            <span>자산명 <em>*</em></span>
            <input v-model="form.assetName" placeholder="예: 갤러리아 VIP 고객층" />
          </label>
          <label>
            <span>자산 설명</span>
            <textarea v-model="form.assetDescription" rows="3" placeholder="예: VIP App 활성 고객 5만 명, 앱 배너"></textarea>
          </label>
        </section>

        <section class="form-section">
          <div class="form-section__head">
            <span class="form-section__num">2</span>
            <strong class="form-section__title">무엇을 달성하고 싶나요? <em>목표</em></strong>
          </div>
          <div class="settings-goal-form__group">
            <span>주 목표 <em>*</em></span>
            <div class="choice-grid">
              <button
                v-for="(type, index) in goalTypes"
                :key="goalTypeValues[index]"
                type="button"
                class="choice-card"
                :class="{ selected: form.primaryType === goalTypeValues[index] }"
                @click="form.primaryType = goalTypeValues[index]"
              >
                {{ type }}
              </button>
            </div>
          </div>
          <label>
            <span>캠페인 목표명</span>
            <input v-model="form.name" placeholder="예: 2026 Q3 객실 예약 증대" />
          </label>
        </section>

        <section class="form-section">
          <div class="form-section__head">
            <span class="form-section__num">3</span>
            <strong class="form-section__title">어떻게 진행할 건가요? <em>조건</em></strong>
          </div>
          <div class="settings-goal-form__group">
            <span>캠페인 방식 <em>다중</em></span>
            <div class="choice-grid">
              <button
                v-for="(method, index) in campaignMethods"
                :key="campaignMethodValues[index]"
                type="button"
                class="choice-card"
                :class="{ selected: isMultiSelected('campaignMethods', campaignMethodValues[index]) }"
                @click="toggleMultiValue('campaignMethods', campaignMethodValues[index])"
              >
                {{ method }}
              </button>
            </div>
          </div>
          <label>
            <span>기간 <em>*</em></span>
            <div class="settings-date-range">
              <input v-model="form.periodStart" type="date" />
              <input v-model="form.periodEnd" type="date" :min="form.periodStart || undefined" />
            </div>
          </label>
        </section>

        <section class="form-section">
          <div class="form-section__head">
            <span class="form-section__num">4</span>
            <strong class="form-section__title">재무 기준 <em>선택</em></strong>
          </div>
          <div class="finance-grid">
            <label>
              <span>최대 부담 비용</span>
              <div class="money-input">
                <input v-model="form.maxCost" inputmode="numeric" placeholder="예: 50000000" />
                <b>원</b>
              </div>
            </label>
            <label>
              <span>최소 기대 매출</span>
              <div class="money-input">
                <input v-model="form.minRevenue" inputmode="numeric" placeholder="예: 120000000" />
                <b>원</b>
              </div>
            </label>
          </div>
        </section>

        <section class="form-section">
          <div class="form-section__head">
            <span class="form-section__num">5</span>
            <strong class="form-section__title">어떤 파트너와? <em>선택</em></strong>
          </div>
          <div class="settings-goal-form__group">
            <span>선호 파트너 업종 <em>다중</em></span>
            <div class="choice-grid">
              <button
                v-for="category in partnerCategories"
                :key="category"
                type="button"
                class="choice-card"
                :class="{ selected: isMultiSelected('partnerFit', category) }"
                @click="toggleMultiValue('partnerFit', category)"
              >
                {{ category }}
              </button>
            </div>
          </div>
          <label>
            <span>기타 업종</span>
            <input v-model="form.partnerOther" placeholder="자유 입력" />
          </label>
        </section>

        <section class="form-section form-section--contact">
          <label>
            <span>담당자 이름 <em>*</em></span>
            <input v-model="form.ownerName" placeholder="예: 김OO" />
          </label>
          <label>
            <span>담당자 이메일 <em>*</em></span>
            <input v-model="form.ownerEmail" type="email" placeholder="name@example.com" />
          </label>
        </section>

        <footer class="match-start-actions">
          <button type="button" class="settings-draft" @click="saveGoalDraft">임시 저장</button>
          <button type="submit" :disabled="!canAddGoal">매칭 시작 →</button>
        </footer>
      </form>

      <section v-if="selectedGoal" class="settings-selected">
        <h4>선택 목표</h4>
        <dl>
          <div>
            <dt>목표명</dt>
            <dd>{{ selectedGoal.name }}</dd>
          </div>
          <div>
            <dt>주 목표 유형</dt>
            <dd>{{ selectedGoal.primaryType }}</dd>
          </div>
          <div>
            <dt>보조 목표</dt>
            <dd>{{ selectedGoal.secondaryType || '선택 안 함' }}</dd>
          </div>
          <div>
            <dt>캠페인 방식</dt>
            <dd>{{ selectedGoal.campaignMethod || '선택 안 함' }}</dd>
          </div>
          <div>
            <dt>핵심 지표</dt>
            <dd>{{ selectedGoal.kpi || '미입력' }}</dd>
          </div>
          <div>
            <dt>예산/공수 한도</dt>
            <dd>{{ selectedGoal.limit || '미입력' }}</dd>
          </div>
          <div>
            <dt>기간</dt>
            <dd>{{ selectedGoal.period || '미입력' }}</dd>
          </div>
          <div>
            <dt>담당자/부서</dt>
            <dd>{{ selectedGoal.owner || '미입력' }}</dd>
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
        :key="assetPanelKey"
        :isDark="isDark"
        @asset-count-change="forwardAssetCount"
        @request-matching="requestMatching"
      />
      <div v-else class="settings-assets__placeholder">
        <strong>자산/혜택을 불러오는 중</strong>
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
.match-start-actions button {
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
  gap: 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.85rem;
  margin-top: -0.15rem;
}

.match-start-head {
  padding: 0 0 0.75rem;
}

.match-start-head strong {
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 900;
}

.form-section {
  display: grid;
  gap: 0.7rem;
  border-top: 1px solid var(--border-color);
  padding: 1rem 0;
}

.form-section__head {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.form-section__num {
  display: grid;
  width: 1.8rem;
  height: 1.8rem;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 6px;
  background: var(--accent-color);
  color: #fff;
  font-size: 0.82rem;
  font-weight: 900;
}

.form-section__title {
  color: var(--text-primary);
  font-size: 0.92rem;
  font-weight: 900;
}

.form-section__title em {
  color: var(--muted-text);
  font-style: normal;
}

.form-section--contact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.settings-goal-form label,
.settings-goal-form__group {
  display: grid;
  gap: 0.28rem;
}

.settings-goal-form span,
.settings-goal-form__group > span {
  color: var(--text-primary);
  font-size: 0.7rem;
  font-weight: 900;
}

.settings-goal-form em {
  margin-left: 0.22rem;
  color: var(--muted-text);
  font-size: 0.64rem;
  font-style: normal;
  font-weight: 800;
}

.settings-goal-form .form-section__num {
  color: #fff;
  font-size: 0.82rem;
  font-weight: 900;
}

.settings-goal-form .form-section__title {
  font-size: 0.92rem;
}

.settings-goal-form input,
.settings-goal-form select,
.settings-goal-form textarea {
  height: 2.25rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0 0.65rem;
  font-size: 0.76rem;
  font-weight: 750;
}

.settings-goal-form textarea {
  height: auto;
  min-height: 5.2rem;
  resize: vertical;
  padding: 0.65rem;
  line-height: 1.45;
}

.settings-goal-form input:focus,
.settings-goal-form select:focus,
.settings-goal-form textarea:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 16%, transparent);
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem;
}

.choice-card {
  display: flex;
  min-height: 4.5rem;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0.75rem;
  cursor: pointer;
  font-size: 0.78rem;
  font-weight: 900;
  line-height: 1.25;
  text-align: center;
  transition:
    border-color 0.14s ease,
    background 0.14s ease,
    color 0.14s ease;
}

.choice-card:hover {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-color));
}

.choice-card.selected {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 12%, var(--panel-color));
  color: var(--accent-color);
}

.finance-grid,
.match-start-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.55rem;
}

.money-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
}

.money-input:focus-within {
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 16%, transparent);
}

.money-input input {
  border: 0;
  background: transparent;
  box-shadow: none !important;
}

.money-input b {
  padding-right: 0.7rem;
  color: var(--muted-text);
  font-size: 0.75rem;
  font-weight: 900;
}

.match-start-actions {
  border-top: 1px solid var(--border-color);
  padding-top: 0.85rem;
}

.settings-date-range {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.4rem;
}

.settings-request {
  border: 1px solid var(--accent-color);
  background: var(--accent-color);
  color: #fff;
}

.match-start-actions button[type='submit'] {
  border: 1px solid var(--accent-color);
  background: var(--accent-color);
  color: #fff;
}

.settings-draft {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.match-start-actions button[type='submit']:disabled,
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
