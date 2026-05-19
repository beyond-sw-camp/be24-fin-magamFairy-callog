<script setup>
import { computed, ref, onBeforeUnmount, onMounted } from 'vue'
import EvaluationModal from './EvaluationModal.vue'
import { getBenefitsFromCampaignIdx } from '@/api/matchingBenefits/index.js'
import CampaignProposalSubmitView from '@/views/CampaignProposalSubmitView.vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})


const isCreateModalOpen = ref(false)

function openCreateBenefitModal() {
  isCreateModalOpen.value = true
}
function closeCreateBenefitModal() {
  isCreateModalOpen.value = false
}
const emit = defineEmits(['navigate', 'request-evaluation'])

const isEvaluationModalOpen = ref(false)
const layoutRef = ref(null)
const listPanelPercent = ref(68)
const isResizing = ref(false)

const layoutStyle = computed(() => ({
  '--benefit-list-width': `${listPanelPercent.value}%`,
}))

const campaignInfo = ref({
  title: '2026 상반기 VIP 스프링 프로모션',
  asset: 'VIP 전용 앱 푸시 및 라운지 배너',
  target: '기존 VIP 및 신규 프리미엄 등급 진입 고객',
})

// 버튼 클릭 시 호출
function openEvaluationModal() {
  isEvaluationModalOpen.value = true
}

// 자식 모달에서 최종 선택 완료 후 submit 이벤트 발생 시 실행됨
function handleEvaluationSubmit(payload) {
  emit('request-evaluation', payload)
  alert('선택한 혜택의 평가가 요청되었습니다.')
}

function startResize(event) {
  isResizing.value = true
  window.addEventListener('pointermove', resizePanels)
  window.addEventListener('pointerup', stopResize)
  resizePanels(event)
}

function resizePanels(event) {
  const rect = layoutRef.value?.getBoundingClientRect()
  if (!rect) return

  const rawPercent = ((event.clientX - rect.left) / rect.width) * 100
  listPanelPercent.value = Math.min(76, Math.max(42, Math.round(rawPercent)))
}

function stopResize() {
  isResizing.value = false
  window.removeEventListener('pointermove', resizePanels)
  window.removeEventListener('pointerup', stopResize)
}

// 실제 상태 코드에 맞게 필터 ID 변경
const statusFilters = [
  { id: 'all', label: '전체' },
  { id: 'PENDING', label: '새 제안' },
  { id: 'HOLD', label: '보류' },
  { id: 'APPROVED', label: '승인' },
]

function getStatusLabel(status) {
  const filter = statusFilters.find(f => f.id === status)
  return filter ? filter.label : status
}

// 1️⃣ 기존 더미 데이터를 지우고 빈 배열로 초기화합니다.
const benefits = ref([])
const campaignIdx = localStorage.getItem('callog-active-campaign-id')

async function loadBenefits() {
    try {
    const response = await getBenefitsFromCampaignIdx(campaignIdx)
    
    benefits.value = response.benefitList || response 

    if (benefits.value.length > 0) {
      selectedId.value = benefits.value[0].id
    }
  } catch (error) {
    console.error('혜택 목록을 불러오는데 실패했습니다:', error)
  }
}

onMounted(async () => {
  loadBenefits()
})

onBeforeUnmount(() => {
  stopResize()
})


const activeFilter = ref('all')
const selectedId = ref(benefits.value[0]?.id ?? null)

const filteredBenefits = computed(() => {
  if (activeFilter.value === 'all') return benefits.value
  return benefits.value.filter((benefit) => benefit.status === activeFilter.value)
})

const selectedBenefit = computed(() => {
  return benefits.value.find((benefit) => benefit.id === selectedId.value) ?? filteredBenefits.value[0] ?? null
})

const summary = computed(() => ({
  total: benefits.value.length,
  new: benefits.value.filter((benefit) => benefit.status === 'PENDING').length,
  approved: benefits.value.filter((benefit) => benefit.status === 'APPROVED').length,
}))

function selectFilter(filterId) {
  activeFilter.value = filterId
  const first = filteredBenefits.value[0]
  if (first && !filteredBenefits.value.some((benefit) => benefit.id === selectedId.value)) {
    selectedId.value = first.id
  }
}

function statusTone(status) {
  if (status === 'PENDING') return 'primary'
  if (status === 'APPROVED') return 'success'
  return 'muted'
}

// UI 출력을 위한 유틸리티 함수
function formatQuantity(benefit) {
  if (benefit.alwaysNegotiable && benefit.quantity === 9999) return '제한 없음'
  if (benefit.quantity === 0) return '미입력'
  return `${benefit.quantity.toLocaleString()}${benefit.quantityUnit}`
}

function formatPeriod(benefit) {
  if (benefit.alwaysNegotiable) return '상시 협의'
  if (!benefit.periodStart || benefit.periodStart === '미입력') return '미입력'
  return `${benefit.periodStart} - ${benefit.periodEnd}`
}

function isBlank(value) {
  return value === null || value === undefined || value === '' || value === '미입력'
}

function displayValue(value) {
  return isBlank(value) ? '미입력' : value
}

function formatNumber(value) {
  if (isBlank(value)) return '미입력'
  const number = Number(value)
  return Number.isFinite(number) ? number.toLocaleString() : value
}

function formatMoney(value) {
  if (isBlank(value)) return '미입력'
  const number = Number(value)
  return Number.isFinite(number) ? `${number.toLocaleString()}원` : value
}

function formatValuePerPerson(benefit) {
  return formatMoney(benefit.valuePerPerson)
}

function formatTotalValue(benefit) {
  return formatMoney(benefit.totalValue)
}

function formatPrepDays(benefit) {
  if (isBlank(benefit.prepDays)) return '미입력'
  return `${formatNumber(benefit.prepDays)}일`
}

function formatExpectedReach(benefit) {
  if (isBlank(benefit.expectedReach)) return '미입력'
  return `${formatNumber(benefit.expectedReach)}명`
}

function formatCostBearer(benefit) {
  if (benefit.costBearer === 'PARTNER') return '파트너 전액 부담'
  if (benefit.costBearer === 'OURS') return '우리 측 전액 부담'
  if (benefit.costBearer === 'JOINT') {
    const partner = displayValue(benefit.costPartnerPercent)
    const ours = displayValue(benefit.costOursPercent)
    return `공동 부담 (${partner}% : ${ours}%)`
  }
  return displayValue(benefit.costBearer)
}

function formatAutoRecommend(benefit) {
  return benefit.autoRecommend ? '추천 받기' : '직접 입력'
}
</script>

<template>
  <section class="benefit-inbox" :class="{ 'benefit-inbox--dark': isDark }">
    <header class="benefit-inbox__head">
      <div>
        <span>Benefit Proposals</span>
        <h3>혜택 평가</h3>
        <p>파트너가 등록한 혜택을 검토하고 평가를 진행합니다.</p>
      </div>
      <button 
        type="button" 
        class="benefit-create-btn"
        @click="openCreateBenefitModal"
      >
        새로운 혜택 제안하기
      </button>
    </header>

    <div
      ref="layoutRef"
      class="benefit-layout"
      :class="{ resizing: isResizing }"
      :style="layoutStyle"
    >
      <div class="benefit-list-column">
        <section class="benefit-summary" aria-label="혜택 제안 요약">
          <article>
            <span>받은 제안</span>
            <strong>{{ summary.total }}<small>건</small></strong>
          </article>
          <article class="accent">
            <span>새 제안</span>
            <strong>{{ summary.new }}<small>건</small></strong>
          </article>
          <article class="success">
            <span>승인</span>
            <strong>{{ summary.approved }}<small>건</small></strong>
          </article>
        </section>

        <nav class="benefit-filters" aria-label="혜택 제안 상태 필터">
          <button
            v-for="filter in statusFilters"
            :key="filter.id"
            type="button"
            :class="{ active: activeFilter === filter.id }"
            @click="selectFilter(filter.id)"
          >
            {{ filter.label }}
          </button>
        </nav>

        <section class="benefit-table" aria-label="들어온 혜택 제안 목록">
          <div class="benefit-table__head">
            <span>파트너/담당자</span>
            <span>혜택</span>
            <span>규모/기간</span>
            <span>매칭 자산</span>
            <span>상태</span>
          </div>
          <button
            v-for="benefit in filteredBenefits"
            :key="benefit.id"
            type="button"
            class="benefit-row"
            :class="{ selected: selectedBenefit?.id === benefit.id }"
            @click="selectedId = benefit.id"
          >
            <strong>{{ benefit.managerName }}</strong>
            <span>
              <b>{{ benefit.name }}</b>
              <small>{{ benefit.type }} · {{ benefit.targetAudience }}</small>
            </span>
            <span>
              <b>{{ formatQuantity(benefit) }}</b>
              <small>{{ formatPeriod(benefit) }}</small>
            </span>
            <span :class="{ muted: !benefit.matchScore }">
              <b>{{ benefit.desiredAssets }}</b>
              <small>{{ benefit.matchScore ? `적합도 ${benefit.matchScore}%` : '매칭 점수 없음' }}</small>
            </span>
            <em :class="`tone-${statusTone(benefit.status)}`">{{ getStatusLabel(benefit.status) }}</em>
          </button>

          <p v-if="!filteredBenefits.length" class="benefit-empty">해당 상태의 제안이 없습니다.</p>
        </section>
      </div>

      <button
        v-if="selectedBenefit"
        type="button"
        class="benefit-resizer"
        aria-label="혜택 목록과 상세 영역 크기 조절"
        title="좌우 영역 크기 조절"
        @pointerdown.prevent="startResize"
      >
        <span />
      </button>

      <aside v-if="selectedBenefit" class="benefit-detail">
        <div class="benefit-detail__scroll">
          <header class="benefit-detail__head">
            <div class="benefit-detail__title">
              <span>혜택 제안 상세</span>
              <h4>{{ selectedBenefit.name }}</h4>
              <p>{{ displayValue(selectedBenefit.managerName) }} · 접수 {{ displayValue(selectedBenefit.receivedAt) }}</p>
            </div>
            <div class="benefit-detail__actions">
              <button type="button">승인</button>
              <button type="button">보류</button>
              <button type="button" class="primary" @click="openEvaluationModal">평가하기</button>
            </div>
          </header>

          <section class="benefit-detail__section">
            <h5>기본 정보</h5>
            <dl class="benefit-detail__table">
              <div>
                <dt>혜택 유형</dt>
                <dd>{{ displayValue(selectedBenefit.type) }}</dd>
              </div>
              <div>
                <dt>대상 고객</dt>
                <dd>{{ displayValue(selectedBenefit.targetAudience) }}</dd>
              </div>
              <div>
                <dt>예상 도달 규모</dt>
                <dd>{{ formatExpectedReach(selectedBenefit) }}</dd>
              </div>
              <div>
                <dt>추천 받기 여부</dt>
                <dd>{{ formatAutoRecommend(selectedBenefit) }}</dd>
              </div>
            </dl>
          </section>

          <section class="benefit-detail__section">
            <h5>규모·기간</h5>
            <dl class="benefit-detail__table">
              <div>
                <dt>제공 수량</dt>
                <dd>{{ formatQuantity(selectedBenefit) }}</dd>
              </div>
              <div>
                <dt>1인당 가치</dt>
                <dd>{{ formatValuePerPerson(selectedBenefit) }}</dd>
              </div>
              <div>
                <dt>총 환산 가치</dt>
                <dd>{{ formatTotalValue(selectedBenefit) }}</dd>
              </div>
              <div>
                <dt>유효 기간</dt>
                <dd>{{ formatPeriod(selectedBenefit) }}</dd>
              </div>
              <div>
                <dt>상시 협의 여부</dt>
                <dd>{{ selectedBenefit.alwaysNegotiable ? '상시 협의' : '기간 지정' }}</dd>
              </div>
              <div>
                <dt>준비 필요 기간</dt>
                <dd>{{ formatPrepDays(selectedBenefit) }}</dd>
              </div>
            </dl>
          </section>

          <section class="benefit-detail__section">
            <h5>비용·운영</h5>
            <dl class="benefit-detail__table benefit-detail__table--wide">
              <div>
                <dt>비용 부담</dt>
                <dd>{{ formatCostBearer(selectedBenefit) }}</dd>
              </div>
              <div>
                <dt>비용 부담 상세</dt>
                <dd>{{ displayValue(selectedBenefit.costDetails) }}</dd>
              </div>
              <div>
                <dt>노출 채널</dt>
                <dd>{{ displayValue(selectedBenefit.exposureChannels) }}</dd>
              </div>
              <div>
                <dt>필요 협업 산출물</dt>
                <dd>{{ displayValue(selectedBenefit.requiredCollaborations) }}</dd>
              </div>
              <div>
                <dt>사용 조건/제약</dt>
                <dd>{{ displayValue(selectedBenefit.conditions) }}</dd>
              </div>
              <div>
                <dt>연결 희망 자산</dt>
                <dd>{{ displayValue(selectedBenefit.desiredAssets) }}</dd>
              </div>
            </dl>
          </section>

          <section class="benefit-detail__section">
            <h5>담당자</h5>
            <dl class="benefit-detail__table benefit-detail__table--stacked">
              <div>
                <dt>담당자 이름</dt>
                <dd>{{ displayValue(selectedBenefit.managerName) }}</dd>
              </div>
              <div>
                <dt>이메일</dt>
                <dd>{{ displayValue(selectedBenefit.managerEmail) }}</dd>
              </div>
              <div>
                <dt>연락처</dt>
                <dd>{{ displayValue(selectedBenefit.managerPhone) }}</dd>
              </div>
            </dl>
          </section>

          <section class="benefit-detail__section">
            <h5>혜택 설명</h5>
            <div class="benefit-detail__memo">{{ displayValue(selectedBenefit.description) }}</div>
          </section>
        </div>

      </aside>
    </div>

    <!-- 모달: proposals Prop에는 바뀐 변수명인 benefits를 전달 -->
    <EvaluationModal
      v-model:isOpen="isEvaluationModalOpen"
      :campaign-info="campaignInfo"
      :proposals="benefits" 
      :initial-selected-id="selectedBenefit?.id"
      @submit="handleEvaluationSubmit"
    />

    <div 
      v-if="isCreateModalOpen" 
      class="custom-modal-overlay" 
      @click.self="closeCreateBenefitModal"
    >
      <div class="custom-modal-content">
        <header class="custom-modal-header">
          <button type="button" class="close-btn" @click="closeCreateBenefitModal">✕</button>
        </header>
        
        <div class="custom-modal-body">
          <CampaignProposalSubmitView 
            @close="closeCreateBenefitModal" 
            @saved="loadBenefits" 
          />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>

/* 👇 제안하기 모달 스타일 👇 */
.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 17, 21, 0.4); /* 어두운 반투명 배경 */
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999; /* 다른 요소들보다 무조건 위에 오도록 */
  backdrop-filter: blur(2px);
}

.custom-modal-content {
  background: var(--benefit-surface);
  border-radius: 16px;
  width: 90%;
  max-width: 900px; /* 제안 폼 크기에 맞게 조절하세요 */
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: modal-fade-in 0.2s ease-out;
}

.custom-modal-header {
  display: flex;
  justify-content: flex-end;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--benefit-line);
  background: var(--benefit-surface);
}

.custom-modal-header .close-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  font-weight: bold;
  color: var(--benefit-text-3);
  cursor: pointer;
  padding: 0.2rem 0.5rem;
  transition: color 0.15s;
}

.custom-modal-header .close-btn:hover {
  color: var(--benefit-text);
}

.custom-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  /* 스크롤바 스타일 부드럽게 */
  scrollbar-gutter: stable;
}

@keyframes modal-fade-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 기존의 CSS와 100% 동일하므로 UI가 깨지지 않습니다. */
.benefit-inbox {
  --benefit-surface: #ffffff;
  --benefit-muted: #fafafb;
  --benefit-line: #ecedf0;
  --benefit-strong: #dee0e5;
  --benefit-text: #0f1115;
  --benefit-text-2: #4a4f5a;
  --benefit-text-3: #8a8f99;
  --benefit-brand: #5b5bf5;
  --benefit-brand-soft: #eef0ff;
  --benefit-green: #16a368;
  --benefit-green-soft: #e5f6ee;
  --benefit-amber: #c97a0e;
  --benefit-amber-soft: #fbefd7;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  gap: 0.85rem;
  overflow: hidden;
  color: var(--benefit-text);
}

.benefit-inbox__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border: 1px solid var(--benefit-line);
  border-radius: 16px;
  background:
    radial-gradient(110% 80% at 100% 0%, #eef0ff 0%, transparent 58%),
    var(--benefit-surface);
  padding: 1.15rem 1.25rem;
}

.benefit-inbox__head span {
  color: var(--benefit-brand);
  font-size: 0.68rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.benefit-inbox__head h3 {
  margin: 0.25rem 0 0;
  color: var(--benefit-text);
  font-size: 1.35rem;
  font-weight: 900;
}

.benefit-inbox__head p {
  margin: 0.35rem 0 0;
  color: var(--benefit-text-2);
  font-size: 0.82rem;
  font-weight: 650;
}

.benefit-detail__actions .primary {
  display: inline-flex;
  height: 2.35rem;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  border: 1px solid var(--benefit-brand);
  border-radius: 8px;
  background: var(--benefit-brand);
  color: #fff;
  padding: 0 0.95rem;
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
}

.benefit-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.benefit-summary article {
  border: 1px solid var(--benefit-line);
  border-radius: 14px;
  background: var(--benefit-surface);
  padding: 0.78rem 1rem;
}

.benefit-summary span {
  color: var(--benefit-text-3);
  font-size: 0.72rem;
  font-weight: 800;
}

.benefit-summary strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--benefit-text);
  font-size: 1.55rem;
  font-weight: 900;
  line-height: 1;
}

.benefit-summary small {
  margin-left: 0.16rem;
  color: var(--benefit-text-3);
  font-size: 0.78rem;
}

.benefit-summary .accent strong {
  color: var(--benefit-brand);
}

.benefit-summary .success strong {
  color: var(--benefit-green);
}

.benefit-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.benefit-filters button {
  height: 1.95rem;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--benefit-text-2);
  padding: 0 0.75rem;
  font-size: 0.76rem;
  font-weight: 800;
  cursor: pointer;
}

.benefit-filters button:hover,
.benefit-filters button.active {
  border-color: var(--benefit-line);
  background: var(--benefit-surface);
  color: var(--benefit-text);
}

.benefit-layout {
  display: grid;
  grid-template-columns: minmax(24rem, var(--benefit-list-width, 68%)) 0.7rem minmax(25rem, 1fr);
  gap: 0.45rem;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.benefit-layout.resizing {
  cursor: col-resize;
  user-select: none;
}

.benefit-list-column {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 0.85rem;
  min-width: 0;
  min-height: 0;
}

.benefit-resizer {
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

.benefit-resizer span {
  display: block;
  width: 3px;
  height: 3.2rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--benefit-strong) 80%, transparent);
  transition: width 0.15s, background 0.15s;
}

.benefit-resizer:hover span,
.benefit-layout.resizing .benefit-resizer span {
  width: 4px;
  background: var(--benefit-brand);
}

.benefit-table,
.benefit-detail {
  height: 100%;
  min-height: 0;
  border: 1px solid var(--benefit-line);
  border-radius: 14px;
  background: var(--benefit-surface);
}

.benefit-table {
  overflow: auto;
  scrollbar-gutter: stable;
}

.benefit-table__head,
.benefit-row {
  display: grid;
  grid-template-columns:
    minmax(5rem, 0.75fr)
    minmax(12rem, 1.35fr)
    minmax(8rem, 0.95fr)
    minmax(9rem, 0.95fr)
    minmax(5.6rem, 0.72fr);
  gap: 0.75rem;
  align-items: center;
}

.benefit-table__head {
  position: sticky;
  top: 0;
  z-index: 2;
  border-bottom: 1px solid var(--benefit-line);
  background: var(--benefit-muted);
  padding: 0.72rem 0.9rem;
  color: var(--benefit-text-3);
  font-size: 0.7rem;
  font-weight: 900;
}

.benefit-row {
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--benefit-line);
  background: var(--benefit-surface);
  padding: 0.78rem 0.9rem;
  text-align: left;
  cursor: pointer;
}

.benefit-row:last-child {
  border-bottom: 0;
}

.benefit-row:hover,
.benefit-row.selected {
  background: color-mix(in srgb, var(--benefit-brand) 4%, var(--benefit-surface));
}

.benefit-row.selected {
  box-shadow: inset 3px 0 0 var(--benefit-brand);
}

.benefit-row strong,
.benefit-row b {
  overflow: hidden;
  color: var(--benefit-text);
  font-size: 0.78rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.benefit-row span {
  display: grid;
  min-width: 0;
  gap: 0.16rem;
}

.benefit-row small {
  overflow: hidden;
  color: var(--benefit-text-3);
  font-size: 0.68rem;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.benefit-row .muted b {
  color: var(--benefit-text-3);
}

.benefit-row em {
  justify-self: start;
  border-radius: 999px;
  padding: 0.22rem 0.5rem;
  font-size: 0.66rem;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.tone-primary {
  background: var(--benefit-brand-soft);
  color: var(--benefit-brand);
}

.tone-warning {
  background: var(--benefit-amber-soft);
  color: var(--benefit-amber);
}

.tone-success {
  background: var(--benefit-green-soft);
  color: var(--benefit-green);
}

.tone-muted {
  background: var(--benefit-muted);
  color: var(--benefit-text-2);
}

.benefit-empty {
  margin: 0;
  padding: 2rem;
  color: var(--benefit-text-3);
  font-size: 0.8rem;
  font-weight: 800;
  text-align: center;
}

.benefit-detail {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--benefit-surface);
}

.benefit-detail__scroll {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 0.95rem;
  overflow: auto;
  padding: 1rem 1.05rem 1.15rem;
  scrollbar-gutter: stable;
}

.benefit-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 2px solid var(--benefit-strong);
  background: #fff;
  padding: 0.2rem 0 0.9rem;
}

.benefit-detail__title {
  min-width: 0;
}

.benefit-detail__title span {
  display: block;
  color: var(--benefit-text-3);
  font-size: 0.68rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.benefit-detail__title h4 {
  margin: 0.35rem 0 0;
  color: var(--benefit-text);
  font-size: 1.05rem;
  font-weight: 900;
  line-height: 1.28;
}

.benefit-detail__title p {
  margin: 0.3rem 0 0;
  color: var(--benefit-text-2);
  font-size: 0.76rem;
  font-weight: 700;
  line-height: 1.45;
}

.benefit-detail__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.35rem;
}

.benefit-detail__actions button {
  min-height: 2rem;
  border: 1px solid var(--benefit-strong);
  border-radius: 4px;
  background: #fff;
  color: var(--benefit-text-2);
  padding: 0 0.75rem;
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.benefit-detail__actions button:hover {
  border-color: var(--benefit-brand);
  color: var(--benefit-brand);
}

.benefit-detail__actions .primary {
  border-color: var(--benefit-brand);
  background: var(--benefit-brand);
  color: #fff;
}

.benefit-detail__actions .primary:hover {
  background: color-mix(in srgb, var(--benefit-brand) 88%, black);
  color: #fff;
}

.benefit-detail__section {
  display: grid;
  gap: 0.45rem;
}

.benefit-detail__section h5 {
  margin: 0;
  color: var(--benefit-text);
  font-size: 0.78rem;
  font-weight: 900;
}

.benefit-detail__table {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  border-top: 1px solid var(--benefit-strong);
  border-left: 1px solid var(--benefit-strong);
  background: #fff;
}

.benefit-detail__table div {
  display: grid;
  grid-template-columns: 7.2rem minmax(0, 1fr);
  min-width: 0;
  border-right: 1px solid var(--benefit-strong);
  border-bottom: 1px solid var(--benefit-strong);
}

.benefit-detail__table--wide div {
  grid-column: span 2;
  grid-template-columns: 8.5rem minmax(0, 1fr);
}

.benefit-detail__table--stacked {
  grid-template-columns: 1fr;
}

.benefit-detail__table--stacked div {
  grid-column: span 1;
  grid-template-columns: 8.5rem minmax(0, 1fr);
}

.benefit-detail__table dt,
.benefit-detail__table dd {
  min-width: 0;
  margin: 0;
  line-height: 1.45;
}

.benefit-detail__table dt {
  display: flex;
  align-items: center;
  background: #f6f7f9;
  color: var(--benefit-text-2);
  padding: 0.55rem 0.65rem;
  font-size: 0.7rem;
  font-weight: 900;
}

.benefit-detail__table dd {
  overflow-wrap: anywhere;
  background: #fff;
  color: var(--benefit-text);
  padding: 0.55rem 0.7rem;
  font-size: 0.76rem;
  font-weight: 750;
}

.benefit-detail__memo {
  min-height: 5.2rem;
  border: 1px solid var(--benefit-strong);
  background: #fff;
  color: var(--benefit-text);
  padding: 0.75rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.65;
  white-space: pre-wrap;
}

@media (max-width: 1120px) {
  .benefit-layout {
    grid-template-columns: 1fr;
    gap: 0.85rem;
  }

  .benefit-resizer {
    display: none;
  }
}

@media (max-width: 860px) {
  .benefit-summary,
  .benefit-detail__table {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .benefit-table {
    overflow-x: auto;
  }

  .benefit-table__head,
  .benefit-row {
    min-width: 48rem;
  }
}

@media (max-width: 560px) {
  .benefit-inbox__head,
  .benefit-detail__head {
    flex-direction: column;
  }

  .benefit-summary,
  .benefit-detail__table {
    grid-template-columns: 1fr;
  }

  .benefit-detail__table--wide div,
  .benefit-detail__table div {
    grid-column: span 1;
    grid-template-columns: 6.8rem minmax(0, 1fr);
  }

  .benefit-detail__actions {
    width: 100%;
    justify-content: stretch;
  }

  .benefit-detail__actions button {
    flex: 1;
  }
}

/* 👇 새로운 혜택 제안하기 버튼 스타일 👇 */
.benefit-create-btn {
  display: inline-flex;
  height: 2.6rem;
  align-items: center;
  justify-content: center;
  align-self: center; /* 헤더 텍스트와 수직 중앙을 맞추기 위함 */
  border: none;
  border-radius: 8px;
  background: var(--benefit-brand);
  color: #ffffff;
  padding: 0 1.25rem;
  font-size: 0.85rem;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.benefit-create-btn:hover {
  background: color-mix(in srgb, var(--benefit-brand) 88%, black);
}

.benefit-create-btn:active {
  transform: scale(0.98);
}

/* 모바일 대응 (선택 사항): 화면이 좁아졌을 때 텍스트와 버튼 간격 조정 */
@media (max-width: 560px) {
  .benefit-create-btn {
    width: 100%;
    margin-top: 1rem;
  }
}
</style>
