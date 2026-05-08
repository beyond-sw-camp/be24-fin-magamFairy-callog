<script setup>
import { computed, ref } from 'vue'
import EvaluationModal from './EvaluationModal.vue'
import { ListBenefits } from '@/api/matchingBenefits/index.js'
import { onMounted } from 'vue';

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['navigate', 'request-evaluation'])

const isEvaluationModalOpen = ref(false)

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

// 실제 상태 코드에 맞게 필터 ID 변경
const statusFilters = [
  { id: 'all', label: '전체' },
  { id: 'PENDING', label: '새 제안' },
  { id: 'INCOMPLETE', label: '보완 필요' },
  { id: 'APPROVED', label: '승인됨' },
  { id: 'HOLD', label: '보류' },
]

function getStatusLabel(status) {
  const filter = statusFilters.find(f => f.id === status)
  return filter ? filter.label : status
}

// 1️⃣ 기존 더미 데이터를 지우고 빈 배열로 초기화합니다.
const benefits = ref([])

onMounted(async () => {
  try {
    const response = await ListBenefits()
    
    benefits.value = response.benefitList || response 

    if (benefits.value.length > 0) {
      selectedId.value = benefits.value[0].id
    }
  } catch (error) {
    console.error('혜택 목록을 불러오는데 실패했습니다:', error)
  }
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
  incomplete: benefits.value.filter((benefit) => benefit.status === 'INCOMPLETE').length,
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
  if (status === 'INCOMPLETE') return 'warning'
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
</script>

<template>
  <section class="benefit-inbox" :class="{ 'benefit-inbox--dark': isDark }">
    <header class="benefit-inbox__head">
      <div>
        <span>Benefit Proposals</span>
        <h3>혜택 평가</h3>
        <p>파트너가 등록한 혜택을 검토하고 평가를 진행합니다.</p>
      </div>
      <button type="button" class="benefit-inbox__primary" @click="openEvaluationModal">
        평가 진행하기
        <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true">
          <path d="M5 12h14m-6-6 6 6-6 6" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </header>

    <div class="benefit-layout">
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
          <article class="warning">
            <span>보완 필요</span>
            <strong>{{ summary.incomplete }}<small>건</small></strong>
          </article>
          <article class="success">
            <span>승인됨</span>
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

      <aside v-if="selectedBenefit" class="benefit-detail">
        <div class="benefit-detail__scroll">
          <header>
            <div>
              <span>{{ selectedBenefit.receivedAt }}</span>
              <h4>{{ selectedBenefit.managerName }} · {{ selectedBenefit.name }}</h4>
              <p>{{ selectedBenefit.description }}</p>
            </div>
            <strong :class="{ muted: !selectedBenefit.matchScore }">
              {{ selectedBenefit.matchScore ? `${selectedBenefit.matchScore}점` : '보완 필요' }}
            </strong>
          </header>

          <dl class="benefit-detail__grid">
            <div>
              <dt>혜택 유형</dt>
              <dd>{{ selectedBenefit.type }}</dd>
            </div>
            <div>
              <dt>대상 고객</dt>
              <dd>{{ selectedBenefit.targetAudience }}</dd>
            </div>
            <div>
              <dt>규모/가치</dt>
              <!-- totalValue가 있으면 포맷팅, 없으면 미입력 처리 -->
              <dd>{{ formatQuantity(selectedBenefit) }} · {{ selectedBenefit.totalValue ? `총 ${selectedBenefit.totalValue.toLocaleString()}원` : '미산정' }}</dd>
            </div>
            <div>
              <dt>비용 부담</dt>
              <dd>{{ selectedBenefit.costDetails }}</dd>
            </div>
            <div>
              <dt>유효 기간</dt>
              <dd>{{ formatPeriod(selectedBenefit) }}</dd>
            </div>
            <div>
              <dt>담당 연락처</dt>
              <dd>{{ selectedBenefit.managerEmail }}<br/><small>{{ selectedBenefit.managerPhone }}</small></dd>
            </div>
          </dl>

          <section class="benefit-notes">
            <div>
              <h5>제안 조건 및 강점</h5>
              <ul>
                <!-- 문자열 기반 데이터를 배열 형태로 화면에 뿌려줌 -->
                <li v-if="selectedBenefit.conditions">{{ selectedBenefit.conditions }}</li>
                <li v-else>기재된 내용이 없습니다.</li>
              </ul>
            </div>
            <div>
              <h5>요구 사항 (확인 필요)</h5>
              <ul>
                <li v-if="selectedBenefit.requiredCollaborations">{{ selectedBenefit.requiredCollaborations }}</li>
                <li v-else>기재된 내용이 없습니다.</li>
              </ul>
            </div>
          </section>
        </div>

        <footer class="benefit-actions">
          <button type="button">보완 요청</button>
          <button type="button">보류</button>
          <button type="button" class="primary" @click="openEvaluationModal">평가 진행하기</button>
        </footer>
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
  </section>
</template>

<style scoped>
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

.benefit-inbox__primary,
.benefit-actions .primary {
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.benefit-summary .warning strong {
  color: var(--benefit-amber);
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
  grid-template-columns: minmax(0, 1.7fr) minmax(27rem, 0.8fr);
  gap: 0.85rem;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.benefit-list-column {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 0.85rem;
  min-width: 0;
  min-height: 0;
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
}

.benefit-detail__scroll {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 0.85rem;
  overflow: auto;
  padding: 1rem;
  scrollbar-gutter: stable;
}

.benefit-detail header {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  border-bottom: 1px solid var(--benefit-line);
  padding-bottom: 0.85rem;
}

.benefit-detail header span {
  color: var(--benefit-text-3);
  font-size: 0.7rem;
  font-weight: 800;
}

.benefit-detail h4 {
  margin: 0.2rem 0 0;
  color: var(--benefit-text);
  font-size: 1rem;
  font-weight: 900;
}

.benefit-detail header p {
  margin: 0.35rem 0 0;
  color: var(--benefit-text-2);
  font-size: 0.76rem;
  font-weight: 650;
  line-height: 1.45;
}

.benefit-detail header > strong {
  flex-shrink: 0;
  color: var(--benefit-brand);
  font-size: 1.35rem;
  font-weight: 900;
}

.benefit-detail header > strong.muted {
  color: var(--benefit-amber);
  font-size: 0.9rem;
}

.benefit-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.45rem;
  margin: 0;
}

.benefit-detail__grid div {
  min-width: 0;
  border: 1px solid var(--benefit-line);
  border-radius: 9px;
  background: var(--benefit-muted);
  padding: 0.62rem 0.7rem;
}

.benefit-detail__grid dt,
.benefit-detail__grid dd {
  margin: 0;
}

.benefit-detail__grid dt {
  color: var(--benefit-text-3);
  font-size: 0.66rem;
  font-weight: 900;
}

.benefit-detail__grid dd {
  overflow-wrap: anywhere;
  margin-top: 0.18rem;
  color: var(--benefit-text);
  font-size: 0.76rem;
  font-weight: 800;
  line-height: 1.35;
}

.benefit-notes {
  display: grid;
  gap: 0.55rem;
}

.benefit-notes div {
  border: 1px solid var(--benefit-line);
  border-radius: 9px;
  padding: 0.7rem;
}

.benefit-notes h5 {
  margin: 0 0 0.45rem;
  color: var(--benefit-text);
  font-size: 0.78rem;
  font-weight: 900;
}

.benefit-notes ul {
  display: grid;
  gap: 0.28rem;
  margin: 0;
  padding-left: 1rem;
}

.benefit-notes li {
  color: var(--benefit-text-2);
  font-size: 0.72rem;
  font-weight: 650;
  line-height: 1.45;
}

.benefit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  border-top: 1px solid var(--benefit-line);
  background: var(--benefit-surface);
  padding: 0.8rem 1rem 1rem;
}

.benefit-actions button {
  min-height: 2.1rem;
  border: 1px solid var(--benefit-line);
  border-radius: 8px;
  background: var(--benefit-surface);
  color: var(--benefit-text-2);
  padding: 0 0.8rem;
  font-size: 0.74rem;
  font-weight: 900;
  cursor: pointer;
}

@media (max-width: 1120px) {
  .benefit-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .benefit-summary,
  .benefit-detail__grid {
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
  .benefit-detail header {
    flex-direction: column;
  }

  .benefit-summary,
  .benefit-detail__grid {
    grid-template-columns: 1fr;
  }

  .benefit-inbox__primary,
  .benefit-actions button {
    width: 100%;
  }
}
</style>