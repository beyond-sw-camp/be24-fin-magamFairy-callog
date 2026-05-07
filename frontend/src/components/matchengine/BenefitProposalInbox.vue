<script setup>
import { computed, ref } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['navigate'])

const statusFilters = [
  { id: 'all', label: '전체' },
  { id: 'new', label: '새 제안' },
  { id: 'incomplete', label: '보완 필요' },
  { id: 'approved', label: '승인됨' },
  { id: 'hold', label: '보류' },
]

const proposals = ref([
  {
    id: 1,
    partner: '럭시드',
    name: '핸드크림 10ml 샘플',
    type: '체험/사은품',
    target: '2040 뷰티 고객, VIP/프리미엄',
    quantity: '10,000개',
    value: '총 5,000만원',
    period: '2026.05.01 - 2026.06.30',
    receivedAt: '오늘 09:42',
    owner: '제휴 검토 필요',
    status: 'new',
    statusLabel: '새 제안',
    matchAsset: '갤러리아 VIP 고객층',
    matchScore: 87,
    cost: '파트너 전액 부담',
    channels: '자사 앱, 알림톡, 제휴사 채널',
    summary: '럭시드 핸드크림 샘플을 VIP 고객에게 제공하는 체험형 혜택입니다.',
    strengths: ['파트너 전액 부담으로 비용 리스크가 낮음', 'VIP 고객층과 적합도 87%', '준비 기간 10일로 단기 실행 가능'],
    risks: ['샘플 재고 소진 시 대체 혜택 필요', '배송비 포함 범위 확인 필요'],
  },
  {
    id: 2,
    partner: '메리오',
    name: '전시 시설 30% 할인권',
    type: '할인/쿠폰',
    target: '패밀리, 4050 기존 고객',
    quantity: '제한 없음',
    value: '할인율 기반 정산',
    period: '상시 협의',
    receivedAt: '어제 16:20',
    owner: '제휴마케팅팀',
    status: 'approved',
    statusLabel: '승인됨',
    matchAsset: '호텔 객실 패키지',
    matchScore: 82,
    cost: '파트너 전액 부담',
    channels: '앱, SNS, 오프라인 매장',
    summary: '전시 시설 할인권을 활용해 기존 고객의 재방문을 유도하는 제안입니다.',
    strengths: ['기존 고객 재방문 목표와 연결이 명확함', '할인 비용을 파트너가 부담'],
    risks: ['운영비 부담 기준은 추가 협의 필요'],
  },
  {
    id: 3,
    partner: '어반스테이지',
    name: '오리지널 콘텐츠 공동 프로모션',
    type: '콘텐츠/이벤트',
    target: '미입력',
    quantity: '미입력',
    value: '미입력',
    period: '미입력',
    receivedAt: '2일 전',
    owner: '담당자 미지정',
    status: 'incomplete',
    statusLabel: '보완 필요',
    matchAsset: '매칭 불가',
    matchScore: null,
    cost: '비용 부담 구조 미입력',
    channels: '보도자료, 영상 콘텐츠 협의 필요',
    summary: '오리지널 콘텐츠를 활용한 공동 프로모션 제안입니다.',
    strengths: ['콘텐츠 협업 형태로 브랜드 노출 가능'],
    risks: ['대상 고객, 비용 부담, 유효 기간이 없어 검토 불가'],
  },
  {
    id: 4,
    partner: '하이테이블',
    name: '프리미엄 다이닝 코스 업그레이드',
    type: '멤버십 혜택',
    target: 'VIP, 기념일 고객',
    quantity: '300건',
    value: '1인 8만원 상당',
    period: '2026.06.01 - 2026.07.31',
    receivedAt: '3일 전',
    owner: 'CRM팀',
    status: 'hold',
    statusLabel: '보류',
    matchAsset: 'VIP 앱 고객층',
    matchScore: 74,
    cost: '공동 부담',
    channels: '앱 푸시, 카카오 알림톡',
    summary: '프리미엄 다이닝 업그레이드 혜택으로 고가 고객군의 반응을 확인하는 제안입니다.',
    strengths: ['VIP 고객에게 매력적인 고관여 혜택', '기념일 타깃 캠페인에 활용 가능'],
    risks: ['공동 부담 비율과 예약 취소 정책 확인 필요'],
  },
])

const activeFilter = ref('all')
const selectedId = ref(proposals.value[0]?.id ?? null)

const filteredProposals = computed(() => {
  if (activeFilter.value === 'all') return proposals.value
  return proposals.value.filter((proposal) => proposal.status === activeFilter.value)
})

const selectedProposal = computed(() => {
  return proposals.value.find((proposal) => proposal.id === selectedId.value) ?? filteredProposals.value[0] ?? null
})

const summary = computed(() => ({
  total: proposals.value.length,
  new: proposals.value.filter((proposal) => proposal.status === 'new').length,
  incomplete: proposals.value.filter((proposal) => proposal.status === 'incomplete').length,
  approved: proposals.value.filter((proposal) => proposal.status === 'approved').length,
}))

function selectFilter(filterId) {
  activeFilter.value = filterId
  const first = filteredProposals.value[0]
  if (first && !filteredProposals.value.some((proposal) => proposal.id === selectedId.value)) {
    selectedId.value = first.id
  }
}

function statusTone(status) {
  if (status === 'new') return 'primary'
  if (status === 'incomplete') return 'warning'
  if (status === 'approved') return 'success'
  return 'muted'
}

function moveToEvaluation() {
  emit('navigate', { tab: 'evaluation', filter: 'new', proposal: selectedProposal.value })
}
</script>

<template>
  <section class="benefit-inbox" :class="{ 'benefit-inbox--dark': isDark }">
    <header class="benefit-inbox__head">
      <div>
        <span>Benefit Proposals</span>
        <h3>혜택 제안</h3>
        <p>파트너가 보낸 혜택 제안을 검토하고 매칭 평가로 넘깁니다.</p>
      </div>
      <button type="button" class="benefit-inbox__primary" @click="moveToEvaluation">
        평가로 보내기
        <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true">
          <path
            d="M5 12h14m-6-6 6 6-6 6"
            fill="none"
            stroke="currentColor"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
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
            <span>파트너</span>
            <span>혜택</span>
            <span>규모/기간</span>
            <span>매칭</span>
            <span>상태</span>
          </div>
          <button
            v-for="proposal in filteredProposals"
            :key="proposal.id"
            type="button"
            class="benefit-row"
            :class="{ selected: selectedProposal?.id === proposal.id }"
            @click="selectedId = proposal.id"
          >
            <strong>{{ proposal.partner }}</strong>
            <span>
              <b>{{ proposal.name }}</b>
              <small>{{ proposal.type }} · {{ proposal.target }}</small>
            </span>
            <span>
              <b>{{ proposal.quantity }}</b>
              <small>{{ proposal.period }}</small>
            </span>
            <span :class="{ muted: !proposal.matchScore }">
              <b>{{ proposal.matchAsset }}</b>
              <small>{{ proposal.matchScore ? `적합도 ${proposal.matchScore}%` : '필수 정보 누락' }}</small>
            </span>
            <em :class="`tone-${statusTone(proposal.status)}`">{{ proposal.statusLabel }}</em>
          </button>

          <p v-if="!filteredProposals.length" class="benefit-empty">해당 상태의 제안이 없습니다.</p>
        </section>
      </div>

      <aside v-if="selectedProposal" class="benefit-detail">
        <div class="benefit-detail__scroll">
          <header>
            <div>
              <span>{{ selectedProposal.receivedAt }}</span>
              <h4>{{ selectedProposal.partner }} · {{ selectedProposal.name }}</h4>
              <p>{{ selectedProposal.summary }}</p>
            </div>
            <strong :class="{ muted: !selectedProposal.matchScore }">
              {{ selectedProposal.matchScore ? `${selectedProposal.matchScore}점` : '보완 필요' }}
            </strong>
          </header>

          <dl class="benefit-detail__grid">
            <div>
              <dt>혜택 유형</dt>
              <dd>{{ selectedProposal.type }}</dd>
            </div>
            <div>
              <dt>대상 고객</dt>
              <dd>{{ selectedProposal.target }}</dd>
            </div>
            <div>
              <dt>규모/가치</dt>
              <dd>{{ selectedProposal.quantity }} · {{ selectedProposal.value }}</dd>
            </div>
            <div>
              <dt>비용 부담</dt>
              <dd>{{ selectedProposal.cost }}</dd>
            </div>
            <div>
              <dt>유효 기간</dt>
              <dd>{{ selectedProposal.period }}</dd>
            </div>
            <div>
              <dt>담당</dt>
              <dd>{{ selectedProposal.owner }}</dd>
            </div>
          </dl>

          <section class="benefit-notes">
            <div>
              <h5>강점</h5>
              <ul>
                <li v-for="item in selectedProposal.strengths" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div>
              <h5>확인 필요</h5>
              <ul>
                <li v-for="item in selectedProposal.risks" :key="item">{{ item }}</li>
              </ul>
            </div>
          </section>
        </div>

        <footer class="benefit-actions">
          <button type="button">보완 요청</button>
          <button type="button">보류</button>
          <button type="button" class="primary" @click="moveToEvaluation">평가로 보내기</button>
        </footer>
      </aside>
    </div>
  </section>
</template>

<style scoped>
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
