<script setup>
import { computed, ref } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['navigate', 'requestEvaluation'])

const statusFilters = [
  { id: 'all', label: '전체' },
  { id: 'new', label: '새 제안' },
  { id: 'approved', label: '승인됨' },
  { id: 'hold', label: '보류' },
]

const proposals = ref([])

const activeFilter = ref('all')
const selectedId = ref(null)

// --- 평가 요청 모달 관련 상태 ---
const isEvaluationModalOpen = ref(false)
const selectedBenefitIds = ref([])

// TODO: 실제 환경에서는 부모에서 props로 받거나 스토어에서 가져와야 합니다.
const campaignInfo = ref({
  title: '2026 상반기 VIP 스프링 프로모션',
  asset: 'VIP 전용 앱 푸시 및 라운지 배너',
  target: '기존 VIP 및 신규 프리미엄 등급 진입 고객',
})

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
  if (status === 'approved') return 'success'
  return 'muted'
}

// --- 모달 제어 함수 ---
function openEvaluationModal() {
  // 모달을 열 때 현재 포커스된 제안을 기본으로 선택해둡니다.
  if (selectedProposal.value) {
    selectedBenefitIds.value = [selectedProposal.value.id]
  } else {
    selectedBenefitIds.value = []
  }
  isEvaluationModalOpen.value = true
}

function submitEvaluationRequest() {
  if (selectedBenefitIds.value.length === 0) {
    alert('평가할 혜택을 하나 이상 선택해주세요.')
    return
  }
  
  // 선택된 혜택 데이터 추출
  const selectedBenefits = proposals.value.filter(p => selectedBenefitIds.value.includes(p.id))
  
  // 상위 컴포넌트로 데이터 전송 혹은 API 호출
  emit('requestEvaluation', {
    campaign: campaignInfo.value,
    benefits: selectedBenefits
  })
  
  // 모달 닫기 및 알림
  isEvaluationModalOpen.value = false
  alert('선택한 혜택의 평가가 요청되었습니다.')
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
      <!-- 평가 요청 버튼 변경: openEvaluationModal 호출 -->
      <button type="button" class="benefit-inbox__primary" @click="openEvaluationModal">
        평가 요청하기
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
              {{ selectedProposal.matchScore ? `${selectedProposal.matchScore}점` : '평가 전' }}
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
          <button type="button">보류</button>
          <!-- 우측 패널 평가 요청 버튼도 동일하게 변경 -->
          <button type="button" class="primary" @click="openEvaluationModal">평가 요청하기</button>
        </footer>
      </aside>
    </div>

    <!-- 평가 요청 모달 -->
    <div v-if="isEvaluationModalOpen" class="modal-overlay" @click.self="isEvaluationModalOpen = false">
      <div class="modal-content">
        <header class="modal-header">
          <h4>평가 요청하기</h4>
          <button class="modal-close" @click="isEvaluationModalOpen = false">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </header>

        <div class="modal-body">
          <section class="modal-section">
            <h5>캠페인 정보</h5>
            <div class="campaign-info">
              <p><span>캠페인명</span> <strong>{{ campaignInfo.title }}</strong></p>
              <p><span>매칭 자산</span> {{ campaignInfo.asset }}</p>
              <p><span>타깃 고객</span> {{ campaignInfo.target }}</p>
            </div>
          </section>

          <section class="modal-section">
            <h5>제안된 혜택 선택</h5>
            <p class="modal-desc">이 캠페인에 적용할 혜택을 모두 선택해주세요.</p>
            <div class="benefit-check-list">
              <label v-for="proposal in proposals" :key="proposal.id" class="check-item">
                <input type="checkbox" :value="proposal.id" v-model="selectedBenefitIds">
                <div class="check-item__info">
                  <strong>{{ proposal.partner }}</strong>
                  <span>{{ proposal.name }}</span>
                  <small>{{ proposal.type }} · {{ proposal.target }}</small>
                </div>
              </label>
            </div>
          </section>
        </div>

        <footer class="modal-footer">
          <button type="button" @click="isEvaluationModalOpen = false">취소</button>
          <button type="button" class="primary" @click="submitEvaluationRequest">
            선택한 혜택 평가 요청
          </button>
        </footer>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* 기존 스타일은 유지하고 최하단에 모달 스타일만 추가했습니다. */
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
  position: relative; /* 모달 기준점을 위해 추가 */
}

/* ----------------------
   기존 스타일 생략 (원문과 동일)
---------------------- */
.benefit-inbox__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; border: 1px solid var(--benefit-line); border-radius: 16px; background: radial-gradient(110% 80% at 100% 0%, #eef0ff 0%, transparent 58%), var(--benefit-surface); padding: 1.15rem 1.25rem; }
.benefit-inbox__head span { color: var(--benefit-brand); font-size: 0.68rem; font-weight: 900; letter-spacing: 0.08em; text-transform: uppercase; }
.benefit-inbox__head h3 { margin: 0.25rem 0 0; color: var(--benefit-text); font-size: 1.35rem; font-weight: 900; }
.benefit-inbox__head p { margin: 0.35rem 0 0; color: var(--benefit-text-2); font-size: 0.82rem; font-weight: 650; }
.benefit-inbox__primary, .benefit-actions .primary { display: inline-flex; height: 2.35rem; align-items: center; justify-content: center; gap: 0.35rem; border: 1px solid var(--benefit-brand); border-radius: 8px; background: var(--benefit-brand); color: #fff; padding: 0 0.95rem; font-size: 0.78rem; font-weight: 900; cursor: pointer; }
.benefit-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0.75rem; }
.benefit-summary article { border: 1px solid var(--benefit-line); border-radius: 14px; background: var(--benefit-surface); padding: 0.78rem 1rem; }
.benefit-summary span { color: var(--benefit-text-3); font-size: 0.72rem; font-weight: 800; }
.benefit-summary strong { display: block; margin-top: 0.35rem; color: var(--benefit-text); font-size: 1.55rem; font-weight: 900; line-height: 1; }
.benefit-summary small { margin-left: 0.16rem; color: var(--benefit-text-3); font-size: 0.78rem; }
.benefit-summary .accent strong { color: var(--benefit-brand); }
.benefit-summary .warning strong { color: var(--benefit-amber); }
.benefit-summary .success strong { color: var(--benefit-green); }
.benefit-filters { display: flex; flex-wrap: wrap; gap: 0.4rem; }
.benefit-filters button { height: 1.95rem; border: 1px solid transparent; border-radius: 8px; background: transparent; color: var(--benefit-text-2); padding: 0 0.75rem; font-size: 0.76rem; font-weight: 800; cursor: pointer; }
.benefit-filters button:hover, .benefit-filters button.active { border-color: var(--benefit-line); background: var(--benefit-surface); color: var(--benefit-text); }
.benefit-layout { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(27rem, 0.8fr); gap: 0.85rem; height: 100%; min-height: 0; overflow: hidden; }
.benefit-list-column { display: grid; grid-template-rows: auto auto minmax(0, 1fr); gap: 0.85rem; min-width: 0; min-height: 0; }
.benefit-table, .benefit-detail { height: 100%; min-height: 0; border: 1px solid var(--benefit-line); border-radius: 14px; background: var(--benefit-surface); }
.benefit-table { overflow: auto; scrollbar-gutter: stable; }
.benefit-table__head, .benefit-row { display: grid; grid-template-columns: minmax(5rem, 0.75fr) minmax(12rem, 1.35fr) minmax(8rem, 0.95fr) minmax(9rem, 0.95fr) minmax(5.6rem, 0.72fr); gap: 0.75rem; align-items: center; }
.benefit-table__head { position: sticky; top: 0; z-index: 2; border-bottom: 1px solid var(--benefit-line); background: var(--benefit-muted); padding: 0.72rem 0.9rem; color: var(--benefit-text-3); font-size: 0.7rem; font-weight: 900; }
.benefit-row { width: 100%; border: 0; border-bottom: 1px solid var(--benefit-line); background: var(--benefit-surface); padding: 0.78rem 0.9rem; text-align: left; cursor: pointer; }
.benefit-row:last-child { border-bottom: 0; }
.benefit-row:hover, .benefit-row.selected { background: color-mix(in srgb, var(--benefit-brand) 4%, var(--benefit-surface)); }
.benefit-row.selected { box-shadow: inset 3px 0 0 var(--benefit-brand); }
.benefit-row strong, .benefit-row b { overflow: hidden; color: var(--benefit-text); font-size: 0.78rem; font-weight: 900; text-overflow: ellipsis; white-space: nowrap; }
.benefit-row span { display: grid; min-width: 0; gap: 0.16rem; }
.benefit-row small { overflow: hidden; color: var(--benefit-text-3); font-size: 0.68rem; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.benefit-row .muted b { color: var(--benefit-text-3); }
.benefit-row em { justify-self: start; border-radius: 999px; padding: 0.22rem 0.5rem; font-size: 0.66rem; font-style: normal; font-weight: 900; white-space: nowrap; }
.tone-primary { background: var(--benefit-brand-soft); color: var(--benefit-brand); }
.tone-warning { background: var(--benefit-amber-soft); color: var(--benefit-amber); }
.tone-success { background: var(--benefit-green-soft); color: var(--benefit-green); }
.tone-muted { background: var(--benefit-muted); color: var(--benefit-text-2); }
.benefit-empty { margin: 0; padding: 2rem; color: var(--benefit-text-3); font-size: 0.8rem; font-weight: 800; text-align: center; }
.benefit-detail { display: flex; flex-direction: column; overflow: hidden; }
.benefit-detail__scroll { display: flex; min-height: 0; flex: 1; flex-direction: column; gap: 0.85rem; overflow: auto; padding: 1rem; scrollbar-gutter: stable; }
.benefit-detail header { display: flex; justify-content: space-between; gap: 0.8rem; border-bottom: 1px solid var(--benefit-line); padding-bottom: 0.85rem; }
.benefit-detail header span { color: var(--benefit-text-3); font-size: 0.7rem; font-weight: 800; }
.benefit-detail h4 { margin: 0.2rem 0 0; color: var(--benefit-text); font-size: 1rem; font-weight: 900; }
.benefit-detail header p { margin: 0.35rem 0 0; color: var(--benefit-text-2); font-size: 0.76rem; font-weight: 650; line-height: 1.45; }
.benefit-detail header > strong { flex-shrink: 0; color: var(--benefit-brand); font-size: 1.35rem; font-weight: 900; }
.benefit-detail header > strong.muted { color: var(--benefit-amber); font-size: 0.9rem; }
.benefit-detail__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.45rem; margin: 0; }
.benefit-detail__grid div { min-width: 0; border: 1px solid var(--benefit-line); border-radius: 9px; background: var(--benefit-muted); padding: 0.62rem 0.7rem; }
.benefit-detail__grid dt, .benefit-detail__grid dd { margin: 0; }
.benefit-detail__grid dt { color: var(--benefit-text-3); font-size: 0.66rem; font-weight: 900; }
.benefit-detail__grid dd { overflow-wrap: anywhere; margin-top: 0.18rem; color: var(--benefit-text); font-size: 0.76rem; font-weight: 800; line-height: 1.35; }
.benefit-notes { display: grid; gap: 0.55rem; }
.benefit-notes div { border: 1px solid var(--benefit-line); border-radius: 9px; padding: 0.7rem; }
.benefit-notes h5 { margin: 0 0 0.45rem; color: var(--benefit-text); font-size: 0.78rem; font-weight: 900; }
.benefit-notes ul { display: grid; gap: 0.28rem; margin: 0; padding-left: 1rem; }
.benefit-notes li { color: var(--benefit-text-2); font-size: 0.72rem; font-weight: 650; line-height: 1.45; }
.benefit-actions { display: flex; flex-wrap: wrap; gap: 0.45rem; border-top: 1px solid var(--benefit-line); background: var(--benefit-surface); padding: 0.8rem 1rem 1rem; }
.benefit-actions button { min-height: 2.1rem; border: 1px solid var(--benefit-line); border-radius: 8px; background: var(--benefit-surface); color: var(--benefit-text-2); padding: 0 0.8rem; font-size: 0.74rem; font-weight: 900; cursor: pointer; }


/* --- 평가 요청 모달 스타일 --- */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}

.modal-content {
  background: var(--benefit-surface);
  width: 100%;
  max-width: 460px;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem;
  border-bottom: 1px solid var(--benefit-line);
}

.modal-header h4 {
  margin: 0;
  color: var(--benefit-text);
  font-size: 1.1rem;
  font-weight: 900;
}

.modal-close {
  background: transparent;
  border: none;
  color: var(--benefit-text-3);
  cursor: pointer;
  padding: 0.2rem;
  display: flex;
}

.modal-close:hover {
  color: var(--benefit-text);
}

.modal-body {
  padding: 1.25rem;
  overflow-y: auto;
  max-height: 60vh;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.modal-section h5 {
  margin: 0 0 0.5rem;
  color: var(--benefit-text);
  font-size: 0.85rem;
  font-weight: 900;
}

.campaign-info {
  background: var(--benefit-muted);
  border: 1px solid var(--benefit-line);
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.campaign-info p {
  margin: 0;
  font-size: 0.78rem;
  color: var(--benefit-text-2);
}

.campaign-info span {
  display: inline-block;
  width: 60px;
  color: var(--benefit-text-3);
  font-weight: 800;
}

.campaign-info strong {
  color: var(--benefit-text);
  font-weight: 900;
}

.modal-desc {
  margin: 0 0 0.75rem;
  font-size: 0.75rem;
  color: var(--benefit-text-3);
}

.benefit-check-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.check-item {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem;
  border: 1px solid var(--benefit-line);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.check-item:hover {
  background: var(--benefit-muted);
}

.check-item:has(input:checked) {
  border-color: var(--benefit-brand);
  background: var(--benefit-brand-soft);
}

.check-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
  accent-color: var(--benefit-brand);
  cursor: pointer;
}

.check-item__info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.check-item__info strong {
  font-size: 0.7rem;
  color: var(--benefit-brand);
}

.check-item__info span {
  font-size: 0.85rem;
  font-weight: 900;
  color: var(--benefit-text);
}

.check-item__info small {
  font-size: 0.7rem;
  color: var(--benefit-text-3);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--benefit-line);
  background: var(--benefit-surface);
}

.modal-footer button {
  min-height: 2.35rem;
  border: 1px solid var(--benefit-line);
  border-radius: 8px;
  background: var(--benefit-surface);
  color: var(--benefit-text-2);
  padding: 0 1rem;
  font-size: 0.8rem;
  font-weight: 900;
  cursor: pointer;
}

.modal-footer button.primary {
  border-color: var(--benefit-brand);
  background: var(--benefit-brand);
  color: #fff;
}


@media (max-width: 1120px) {
  .benefit-layout { grid-template-columns: 1fr; }
}
@media (max-width: 860px) {
  .benefit-summary, .benefit-detail__grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .benefit-table { overflow-x: auto; }
  .benefit-table__head, .benefit-row { min-width: 48rem; }
}
@media (max-width: 560px) {
  .benefit-inbox__head, .benefit-detail header { flex-direction: column; }
  .benefit-summary, .benefit-detail__grid { grid-template-columns: 1fr; }
  .benefit-inbox__primary, .benefit-actions button { width: 100%; }
}
</style>
