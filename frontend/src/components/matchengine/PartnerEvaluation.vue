<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { getEvaluationResult } from '@/api/evaluation'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
  serverCandidatesList: {
    type: Array,
    default: () => [],
  },
  evaluationCandidate: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['decided'])

const proposalQueue = ref([])

const statusTabs = [
  { key: 'new', label: '신규 추천' },
  { key: 'proceed', label: '진행 결정' },
  { key: 'hold', label: '보류' },
  { key: 'exclude', label: '제외' },
]
const activeStatus = ref('new')
const proposals = computed(() =>
  proposalQueue.value.filter((proposal) => proposal.reviewStatus === activeStatus.value),
)
const totalProposalCount = computed(() => proposalQueue.value.length)
const statusCounts = computed(() =>
  statusTabs.reduce((counts, tab) => {
    counts[tab.key] = proposalQueue.value.filter((proposal) => proposal.reviewStatus === tab.key).length
    return counts
  }, {}),
)

// 1. UI용 데이터 포맷으로 정규화하는 함수
function mapCandidateToProposal(candidate) {
  const score = Number(candidate.score ?? 0)
  
  return {
    id: candidate.id,
    isSample: candidate.isSample === true,
    campaignName: candidate.title,
    partnerName: candidate.partner,
    benefitSummary: candidate.offer || '혜택 정보 미입력',
    goalLabel: candidate.goal,
    period: candidate.schedule,
    status: '신규 추천',
    reviewStatus: 'new',
    scores: {
      customerFit: candidate.scoreBreakdown[0].score,
      revenue: candidate.scoreBreakdown[1].score,
      cost: candidate.scoreBreakdown[2].score,
      operation: candidate.scoreBreakdown[3].score,
      brand: candidate.scoreBreakdown[4].score,
    },
    comparison: null,
    warnings: [],
    reason: '',
    targetKpis: candidate.targetKpis, 
    detailCards: candidate.detailCards,
    evidence: [],
    riskMatrix: null,
    nextActions: [],
    comments: [], // JSON 내 코멘트 부재로 빈 배열 처리
    detailedMetrics: candidate.detailedMetrics,
    manualScore: score,
  }
}

// 2. 서버에서 받은 JSON 데이터를 모델 규격으로 변환하는 어댑터 함수
function adaptServerDataToCandidate(serverData, index = 0) {
  const evals = serverData.evaluations || {};
  
  const scores = {
    customerFit: evals.customer?.overallScore ?? 0,
    revenue: evals.revenue?.overallScore ?? 0,
    cost: evals.cost?.overallScore ?? 0,
    operation: evals.operation?.overallScore ?? 0,
    brand: evals.brand?.overallScore ?? 0,
  };

  const finalScore = Math.round(
    scores.customerFit * 0.25 +
    scores.revenue * 0.25 +
    scores.cost * 0.2 +
    scores.operation * 0.15 +
    scores.brand * 0.15
  );

  // 각 평가 항목 내부의 상세 텍스트 필드들을 결합하여 근거 본문 생성
  const getEvaluationText = (evalObj) => {
    if (!evalObj) return '';
    return Object.entries(evalObj)
      .filter(([key]) => key !== 'overallScore' && key !== 'improvementDirections')
      .map(([_, value]) => value)
      .filter(Boolean)
      .join('\n\n');
  };

  return {
    id: serverData.sessionId || `server-candidate-${index}-${Date.now()}`,
    isSample: false,
    goal: serverData.goal || '목표 미지정',
    title: serverData.title || '제목 미입력',
    partner: serverData.partner || '파트너 미정',
    offer: serverData.offer || '혜택 미입력',
    asset: serverData.assetDescription || '자산 정보 미입력',
    target: serverData.target || '타겟 미정',
    schedule: (serverData.startedAt || serverData.endedAt)
      ? `${serverData.startedAt || '미정'} ~ ${serverData.endedAt || '미정'}`
      : '일정 정보 미입력',
    score: finalScore,
    scoreBreakdown: [
      { label: '고객 적합도', score: scores.customerFit },
      { label: '수익 기여도', score: scores.revenue },
      { label: '비용 효율성', score: scores.cost },
      { label: '운영 용이성', score: scores.operation },
      { label: '브랜드 적합도', score: scores.brand },
    ],
    targetKpis: [], // JSON 구조 내 가짜 KPI 데이터 제거
    detailCards: [
      { label: '보유 자산', value: serverData.assetDescription || '미입력', meta: '' },
      { label: '파트너 혜택', value: serverData.offer || '미입력', meta: '' },
      { label: '타겟 대상', value: serverData.target || '미입력', meta: '' }
    ],
    detailedMetrics: {
      customerFit: {
        text: getEvaluationText(evals.customer),
        reasons: evals.customer?.improvementDirections || []
      },
      revenue: {
        text: getEvaluationText(evals.revenue),
        reasons: evals.revenue?.improvementDirections || []
      },
      cost: {
        text: getEvaluationText(evals.cost),
        reasons: evals.cost?.improvementDirections || []
      },
      operation: {
        text: getEvaluationText(evals.operation),
        reasons: evals.operation?.improvementDirections || []
      },
      brand: {
        text: getEvaluationText(evals.brand),
        reasons: evals.brand?.improvementDirections || []
      }
    }
  };
}

const currentCampaign = localStorage.getItem("callog-active-campaign-id")

const fetchEvaluationData = async () => {
  try {
    const serverDataList = await getEvaluationResult(currentCampaign)
    
    if (serverDataList && serverDataList.length > 0) {
      proposalQueue.value = serverDataList
        .map((data, index) => adaptServerDataToCandidate(data, index))
        .map(mapCandidateToProposal)
      
      setActiveStatus('new')
      selectedId.value = proposalQueue.value[0]?.id ?? null
    }
  } catch (error) {
    console.error('평가 데이터를 불러오는데 실패했습니다:', error)
  }
}

onMounted(() => {
  fetchEvaluationData()
})

const metrics = [
  { key: 'customerFit', label: '고객 적합도', weight: 25 },
  { key: 'revenue', label: '수익 기여도', weight: 25 },
  { key: 'cost', label: '비용 효율성', weight: 20 },
  { key: 'operation', label: '운영 용이성', weight: 15 },
  { key: 'brand', label: '브랜드 적합도', weight: 15 },
]

// 하드코딩 빈 값 처리 (서버 데이터를 우선 하도록 변경)
const metricDetails = {
  customerFit: { benchmark: 0, reasons: [] },
  revenue: { benchmark: 0, reasons: [] },
  cost: { benchmark: 0, reasons: [] },
  operation: { benchmark: 0, reasons: [] },
  brand: { benchmark: 0, reasons: [] },
}

const selectedId = ref(proposalQueue.value[0]?.id || null)
const activeMetricKey = ref(metrics[0].key)
const pendingDecision = ref(null)
const decisionReason = ref('')
const isFormulaOpen = ref(false)
const isConditionsOpen = ref(false)
const showCompactActions = ref(false)

const selectedProposal = computed(
  () => proposals.value.find((proposal) => proposal.id === selectedId.value) ?? proposals.value[0] ?? null,
)
const selectedScore = computed(() => (selectedProposal.value ? calculateScore(selectedProposal.value) : 0))
const activeMetric = computed(() => metrics.find((metric) => metric.key === activeMetricKey.value) ?? metrics[0])
const activeMetricIndex = computed(() => metrics.findIndex((metric) => metric.key === activeMetric.value.key))

const activeMetricDetails = computed(() => {
  if (selectedProposal.value?.detailedMetrics?.[activeMetric.value.key]) {
    return selectedProposal.value.detailedMetrics[activeMetric.value.key]
  }
  return metricDetails[activeMetric.value.key]
})

const activeMetricEvidence = computed(() => {
  if (!selectedProposal.value) return ''
  return selectedProposal.value.detailedMetrics?.[activeMetric.value.key]?.text || ''
})

const topMetric = computed(() => {
  if (!selectedProposal.value) return metrics[0]
  return metrics.reduce((top, metric) => {
    return selectedProposal.value.scores[metric.key] > selectedProposal.value.scores[top.key] ? metric : top
  }, metrics[0])
})
const weakestMetric = computed(() => {
  if (!selectedProposal.value) return metrics[0]
  return metrics.reduce((weakest, metric) => {
    return selectedProposal.value.scores[metric.key] < selectedProposal.value.scores[weakest.key] ? metric : weakest
  }, metrics[0])
})

const radarLevels = [20, 40, 60, 80, 100]
const radarCenter = 96
const radarRadius = 68
const radarAxes = computed(() => {
  const total = metrics.length
  return metrics.map((metric, index) => ({
    ...metric,
    point: radarPoint(100, index, total),
    labelPoint: radarPoint(117, index, total),
  }))
})
const radarPolygonPoints = computed(() => {
  if (!selectedProposal.value) return ''
  return metrics
    .map((metric, index) => radarPoint(selectedProposal.value.scores[metric.key], index, metrics.length))
    .join(' ')
})
const radarGridPolygons = computed(() => {
  return radarLevels.map((level) => ({
    level,
    points: metrics.map((_, index) => radarPoint(level, index, metrics.length)).join(' '),
  }))
})
const decisionConfig = {
  proceed: {
    label: '진행하기',
    title: '운영 보드로 전환할까요?',
    description: '진행 결정 시 자동으로 다음 작업이 수행됩니다.',
    autoActions: ['운영 보드에 카드 생성', '담당자에게 알림 발송'],
    confirmLabel: '진행 확정',
    requireReason: false,
    tone: 'primary',
  },
  hold: {
    label: '보류',
    title: '보류 사유를 알려주세요',
    description: '재검토 시 참고할 수 있도록 사유를 기록합니다.',
    autoActions: ['보류 상태로 변경', '재검토 알림 예약 (7일 후)'],
    confirmLabel: '보류 확정',
    requireReason: true,
    tone: 'neutral',
  },
  exclude: {
    label: '제외',
    title: '제외하시겠어요?',
    description: '제외된 후보는 추천 목록에서 사라집니다.',
    autoActions: ['제외 상태로 변경', '파트너에게 통보 메일 (선택)'],
    confirmLabel: '제외 확정',
    requireReason: true,
    tone: 'danger',
  },
}

const currentDecisionConfig = computed(() => decisionConfig[pendingDecision.value] ?? null)

function getStatusLabel(statusKey) {
  return statusTabs.find((tab) => tab.key === statusKey)?.label ?? '신규 추천'
}

function getDecisionStatus(decision) {
  if (decision === 'proceed') return 'proceed'
  if (decision === 'hold') return 'hold'
  if (decision === 'exclude') return 'exclude'
  return 'new'
}

function setActiveStatus(statusKey) {
  activeStatus.value = statusKey
  const firstVisible = proposals.value[0]
  if (!proposals.value.some((proposal) => proposal.id === selectedId.value)) {
    selectedId.value = firstVisible?.id ?? null
  }
}

function openDecisionConfirm(decision) {
  pendingDecision.value = decision
  decisionReason.value = ''
}

function handleDetailScroll(event) {
  showCompactActions.value = event.currentTarget.scrollTop > 118
}

function radarPoint(percent, index, total) {
  const angle = -Math.PI / 2 + index * ((Math.PI * 2) / total)
  const radius = (percent / 100) * radarRadius
  const x = radarCenter + Math.cos(angle) * radius
  const y = radarCenter + Math.sin(angle) * radius
  return `${x.toFixed(1)},${y.toFixed(1)}`
}

function closeDecisionConfirm() {
  pendingDecision.value = null
  decisionReason.value = ''
}

function confirmDecision() {
  const config = currentDecisionConfig.value
  if (!config) return
  if (config.requireReason && !decisionReason.value.trim()) return
  const nextStatus = getDecisionStatus(pendingDecision.value)
  const index = proposalQueue.value.findIndex((proposal) => proposal.id === selectedProposal.value?.id)
  if (index >= 0) {
    proposalQueue.value[index] = {
      ...proposalQueue.value[index],
      reviewStatus: nextStatus,
      status: getStatusLabel(nextStatus),
      decisionReason: decisionReason.value.trim() || proposalQueue.value[index].decisionReason,
    }
    activeStatus.value = nextStatus
    selectedId.value = proposalQueue.value[index].id
  }
  emit('decided', {
    decision: pendingDecision.value,
    reason: decisionReason.value.trim() || null,
    proposal: selectedProposal.value,
  })
  closeDecisionConfirm()
}

watch(selectedId, () => {
  activeMetricKey.value = metrics[0].key
  closeDecisionConfirm()
  isFormulaOpen.value = false
  isConditionsOpen.value = false
  showCompactActions.value = false
})

watch(
  () => props.evaluationCandidate,
  (candidate) => {
    if (!candidate?.id) return
    const mapped = mapCandidateToProposal(candidate)
    const index = proposalQueue.value.findIndex((proposal) => proposal.id === mapped.id)
    const newProposal = { ...mapped, reviewStatus: 'new', status: '신규 추천' }
    if (index >= 0) proposalQueue.value.splice(index, 1, { ...proposalQueue.value[index], ...newProposal })
    else proposalQueue.value.unshift(newProposal)
    activeStatus.value = 'new'
    selectedId.value = mapped.id
  },
  { immediate: true },
)

function calculateScore(proposal) {
  if (!proposal) return 0
  if (proposal.manualScore !== null) return proposal.manualScore
  return Number(
    (
      proposal.scores.customerFit * 0.25 +
      proposal.scores.revenue * 0.25 +
      proposal.scores.cost * 0.2 +
      proposal.scores.operation * 0.15 +
      proposal.scores.brand * 0.15
    ).toFixed(1),
  )
}

function grade(score) {
  if (score >= 90) return '최우선 추천'
  if (score >= 80) return '우선 검토'
  if (score >= 70) return '조건부 검토'
  return '검토 제외'
}
</script>

<template>
  <section class="partner-eval">
    <aside class="pe-sidebar">
      <header class="pe-sidebar__head">
        <strong>파트너 평가</strong>
        <span>전체 {{ totalProposalCount }}</span>
      </header>

      <label class="pe-search">
        <svg viewBox="0 0 24 24" width="13" height="13" aria-hidden="true">
          <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2" />
          <path d="m16 16 4 4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        </svg>
        <input type="text" placeholder="파트너명 검색" />
      </label>

      <div class="pe-candidate-list">
        <section v-for="tab in statusTabs" :key="tab.key" class="pe-status-section">
          <button
            type="button"
            class="pe-filter"
            :class="{ active: activeStatus === tab.key }"
            @click="setActiveStatus(tab.key)"
          >
            <span>{{ tab.label }}</span>
            <b>{{ statusCounts[tab.key] }}</b>
          </button>

          <div v-if="activeStatus === tab.key" class="pe-status-panel">
            <button
              v-for="proposal in proposals"
              :key="proposal.id"
              type="button"
              class="pe-candidate"
              :class="{ active: selectedId === proposal.id }"
              @click="selectedId = proposal.id"
            >
              <span class="pe-candidate__top">
                <strong>{{ proposal.partnerName }}</strong>
                <em v-if="proposal.isSample">샘플</em>
                <small>{{ tab.label }}</small>
                <b>{{ calculateScore(proposal) }}</b>
              </span>
              <small>{{ proposal.benefitSummary }}</small>
              <i><span :style="{ width: `${calculateScore(proposal)}%` }" /></i>
            </button>

            <p v-if="!proposals.length" class="pe-empty">이 상태의 후보가 없습니다.</p>
          </div>
        </section>
      </div>
    </aside>

    <article v-if="selectedProposal" class="pe-detail" @scroll="handleDetailScroll">
      <div v-show="showCompactActions" class="pe-mini-actions">
        <strong>{{ selectedProposal.partnerName }}</strong>
        <span>{{ selectedScore }}점</span>
        <button type="button" class="primary" @click="openDecisionConfirm('proceed')">진행</button>
        <button type="button" @click="openDecisionConfirm('hold')">보류</button>
        <button type="button" class="danger" @click="openDecisionConfirm('exclude')">제외</button>
      </div>

      <section class="pe-hero pe-card">
        <div class="pe-hero__icon">{{ selectedProposal.partnerName.slice(0, 1) }}</div>
        <div class="pe-hero__copy">
          <span>매칭 후보</span>
          <h3>
            {{ selectedProposal.campaignName }}
            <em v-if="selectedProposal.isSample">샘플</em>
          </h3>
          <p>{{ selectedProposal.partnerName }} · {{ selectedProposal.benefitSummary }}</p>
          <div>
            <small>목표 <b>{{ selectedProposal.goalLabel }}</b></small>
            <small>기간 <b>{{ selectedProposal.period }}</b></small>
            <small class="pe-status">{{ selectedProposal.status }}</small>
          </div>
        </div>
        <div class="pe-hero__side">
          <div class="pe-score">
            <strong>{{ selectedScore }}</strong>
            <span>{{ grade(selectedScore) }}</span>
          </div>
          <div class="pe-hero__actions">
            <button type="button" class="primary" @click="openDecisionConfirm('proceed')">진행하기</button>
            <button type="button" @click="openDecisionConfirm('hold')">보류</button>
            <button type="button" class="danger" @click="openDecisionConfirm('exclude')">제외</button>
          </div>
        </div>
      </section>

      <section class="pe-card">
        <header class="pe-section-head">
          <h4>평가 결과 상세</h4>
        </header>

        <div class="pe-eval-grid">
          <div class="pe-radar-panel">
            <svg class="pe-radar" viewBox="0 0 192 192" role="img" aria-label="세부 평가 레이더 차트">
              <polygon
                v-for="grid in radarGridPolygons"
                :key="grid.level"
                :points="grid.points"
                class="pe-radar__grid"
              />
              <line
                v-for="axis in radarAxes"
                :key="axis.key"
                :x1="radarCenter"
                :y1="radarCenter"
                :x2="axis.point.split(',')[0]"
                :y2="axis.point.split(',')[1]"
                class="pe-radar__axis"
              />
              <polygon :points="radarPolygonPoints" class="pe-radar__shape" />
              <circle
                v-for="axis in radarAxes"
                :key="axis.key + '-point'"
                :cx="radarPoint(selectedProposal.scores[axis.key], metrics.findIndex((metric) => metric.key === axis.key), metrics.length).split(',')[0]"
                :cy="radarPoint(selectedProposal.scores[axis.key], metrics.findIndex((metric) => metric.key === axis.key), metrics.length).split(',')[1]"
                r="3"
                class="pe-radar__point"
              />
              <text
                v-for="axis in radarAxes"
                :key="axis.key + '-label'"
                :x="axis.labelPoint.split(',')[0]"
                :y="axis.labelPoint.split(',')[1]"
                text-anchor="middle"
                dominant-baseline="middle"
              >
                {{ axis.label }} {{ selectedProposal.scores[axis.key] }}
              </text>
            </svg>
          </div>

          <aside class="pe-evidence">
            <div>
              <h5>{{ activeMetric.label }} 근거</h5>
              <strong>{{ selectedProposal.scores[activeMetric.key] }} / 100</strong>
            </div>
            <div class="pe-stat-grid">
              <button
                v-for="metric in metrics"
                :key="metric.key"
                type="button"
                :class="{ active: activeMetricKey === metric.key }"
                @click="activeMetricKey = metric.key"
              >
                {{ metric.label }}
                <b>{{ selectedProposal.scores[metric.key] }}점 · {{ metric.weight }}%</b>
              </button>
            </div>
            <p style="white-space: pre-wrap;">{{ activeMetricEvidence }}</p>
            <ul>
              <li v-for="reason in activeMetricDetails.reasons" :key="reason">{{ reason }}</li>
            </ul>
            <div class="pe-dist">
              <i :style="{ left: `${selectedProposal.scores[activeMetric.key]}%` }" />
            </div>
          </aside>
        </div>
      </section>

      <section class="pe-card">
        <header class="pe-section-head">
          <h4>조합 구성과 운영 정보</h4>
        </header>
        <dl class="pe-info-grid">
          <div v-for="card in selectedProposal.detailCards" :key="card.label">
            <dt>{{ card.label }}</dt>
            <dd>
              <strong>{{ card.value }}</strong>
              <small>{{ card.meta }}</small>
            </dd>
          </div>
        </dl>
      </section>

      <section class="pe-card">
        <header class="pe-section-head">
          <h4>목표와 실행</h4>
        </header>
        <div class="pe-action-grid pe-action-grid--single">
          <section>
            <h5>목표 KPI</h5>
            <ul v-if="selectedProposal.targetKpis.length">
              <li v-for="kpi in selectedProposal.targetKpis" :key="kpi">{{ kpi }}</li>
            </ul>
            <p v-else>KPI가 설정되지 않았습니다.</p>
          </section>
        </div>
      </section>

      <section class="pe-card pe-comments">
        <header class="pe-section-head">
          <h4>코멘트</h4>
          <span>{{ selectedProposal.comments.length }}건</span>
        </header>
        <ol v-if="selectedProposal.comments.length">
          <li v-for="comment in selectedProposal.comments" :key="comment.author + comment.time">
            <b>{{ comment.author }}</b>
            <small>{{ comment.time }}</small>
            <p>{{ comment.text }}</p>
          </li>
        </ol>
        <div class="pe-comment-input">
          <span>AD</span>
          <input type="text" placeholder="코멘트를 작성하세요..." />
          <button type="button">등록</button>
        </div>
      </section>
    </article>

    <article v-else class="pe-detail pe-empty-detail">
      <strong>평가할 후보가 없습니다.</strong>
      <p>추천 후보를 평가로 보내면 이곳에 표시됩니다.</p>
    </article>

    <Transition name="modal">
      <div v-if="currentDecisionConfig" class="decision-modal" @click.self="closeDecisionConfirm">
        <div class="decision-modal__panel" :class="'decision-modal__panel--' + currentDecisionConfig.tone">
          <header class="decision-modal__head">
            <h3>{{ currentDecisionConfig.title }}</h3>
            <p>{{ currentDecisionConfig.description }}</p>
          </header>

          <div class="decision-modal__target">
            <span>{{ selectedProposal?.campaignName }}</span>
            <span>{{ selectedProposal?.partnerName }}</span>
          </div>

          <div v-if="currentDecisionConfig.requireReason" class="decision-modal__field">
            <label>사유 <span>*</span></label>
            <textarea
              v-model="decisionReason"
              :placeholder="pendingDecision === 'hold' ? '예: 운영 일정 재조율 필요' : '예: 타겟 적합도 부족'"
              rows="3"
            />
          </div>

          <div class="decision-modal__auto">
            <span>자동 처리</span>
            <ul>
              <li v-for="action in currentDecisionConfig.autoActions" :key="action">
                <i aria-hidden="true">✓</i>
                {{ action }}
              </li>
            </ul>
          </div>

          <footer class="decision-modal__foot">
            <button type="button" class="decision-modal__cancel" @click="closeDecisionConfirm">취소</button>
            <button
              type="button"
              class="decision-modal__confirm"
              :class="'decision-modal__confirm--' + currentDecisionConfig.tone"
              :disabled="currentDecisionConfig.requireReason && !decisionReason.trim()"
              @click="confirmDecision"
            >
              {{ currentDecisionConfig.confirmLabel }}
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.partner-eval {
  --pe-bg: #f7f7f9;
  --pe-surface: #ffffff;
  --pe-muted: #fafafb;
  --pe-line: #ecedf0;
  --pe-line-strong: #dee0e5;
  --pe-text: #0f1115;
  --pe-text-2: #4a4f5a;
  --pe-text-3: #8a8f99;
  --pe-brand: #5b5bf5;
  --pe-brand-strong: #4848e0;
  --pe-brand-soft: #eef0ff;
  --pe-violet: #7c5cfa;
  --pe-green: #16a368;
  --pe-green-soft: #e5f6ee;
  --pe-amber: #c97a0e;
  --pe-amber-soft: #fbefd7;
  --pe-rose: #d0395f;
  display: grid;
  grid-template-columns: 18rem minmax(0, 1fr);
  gap: 0.85rem;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: var(--pe-text);
}

.pe-sidebar,
.pe-detail {
  min-height: 0;
  border: 1px solid var(--pe-line);
  border-radius: 12px;
  background: var(--pe-surface);
}

.pe-sidebar {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pe-sidebar__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.85rem 0.9rem 0.5rem;
}

.pe-sidebar__head strong {
  font-size: 0.86rem;
  font-weight: 900;
}

.pe-sidebar__head span,
.pe-section-head span {
  color: var(--pe-text-3);
  font-size: 0.7rem;
  font-weight: 800;
}

.pe-search {
  display: flex;
  height: 2rem;
  align-items: center;
  gap: 0.4rem;
  border: 1px solid var(--pe-line);
  border-radius: 8px;
  background: var(--pe-muted);
  margin: 0 0.85rem 0.75rem;
  padding: 0 0.6rem;
  color: var(--pe-text-3);
}

.pe-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--pe-text);
  font-size: 0.72rem;
  font-weight: 700;
}

.pe-filter {
  display: grid;
  width: 100%;
  min-height: 2.1rem;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  border: 0;
  border-top: 1px solid var(--pe-line);
  border-bottom: 1px solid var(--pe-line);
  background: transparent;
  color: var(--pe-text-2);
  padding: 0 0.8rem;
  text-align: left;
  cursor: pointer;
}

.pe-filter span {
  font-size: 0.72rem;
  font-weight: 850;
}

.pe-filter b {
  display: inline-flex;
  min-width: 1.18rem;
  height: 1.18rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--pe-line);
  color: var(--pe-text-2);
  font-size: 0.64rem;
  font-weight: 900;
}

.pe-filter.active {
  background: var(--pe-surface);
  color: var(--pe-text);
}

.pe-filter.active b {
  background: var(--pe-text);
  color: #fff;
}

.pe-candidate-list {
  min-height: 0;
  flex: 1;
  overflow: auto;
  background: var(--pe-muted);
  padding: 0;
}

.pe-status-section {
  background: var(--pe-muted);
}

.pe-status-section + .pe-status-section .pe-filter {
  border-top: 0;
}

.pe-status-panel {
  position: relative;
  background: var(--pe-surface);
  margin: 0.42rem 0.55rem 0.55rem;
  border: 1px solid color-mix(in srgb, var(--pe-brand) 20%, var(--pe-line));
  border-radius: 10px;
  box-shadow: inset 3px 0 0 var(--pe-brand);
  overflow: hidden;
}

.pe-status-panel::before {
  position: absolute;
  top: -0.42rem;
  left: 1rem;
  width: 0.75rem;
  height: 0.75rem;
  border-top: 1px solid color-mix(in srgb, var(--pe-brand) 20%, var(--pe-line));
  border-left: 1px solid color-mix(in srgb, var(--pe-brand) 20%, var(--pe-line));
  background: var(--pe-surface);
  content: '';
  transform: rotate(45deg);
}

.pe-status-panel::after {
  position: absolute;
  top: 0.85rem;
  bottom: 0.85rem;
  left: 0.78rem;
  width: 1px;
  background: color-mix(in srgb, var(--pe-brand) 22%, transparent);
  content: '';
}

.pe-candidate {
  position: relative;
  display: grid;
  width: 100%;
  gap: 0.4rem;
  border: 0;
  border-bottom: 1px solid var(--pe-line);
  background: var(--pe-surface);
  padding: 0.9rem 0.95rem 0.9rem 1.45rem;
  text-align: left;
  cursor: pointer;
  z-index: 1;
}

.pe-candidate::before {
  position: absolute;
  top: 1.35rem;
  left: 0.78rem;
  width: 0.35rem;
  height: 1px;
  background: color-mix(in srgb, var(--pe-brand) 36%, transparent);
  content: '';
}

.pe-candidate:hover,
.pe-candidate.active {
  background: var(--pe-brand-soft);
}

.pe-candidate.active {
  box-shadow: inset 3px 0 0 color-mix(in srgb, var(--pe-brand) 70%, #fff);
}

.pe-candidate__top {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  min-width: 0;
}

.pe-candidate__top strong {
  overflow: hidden;
  color: var(--pe-text);
  font-size: 0.82rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pe-candidate__top em,
.pe-hero h3 em,
.pe-status,
.pe-candidate__top small {
  border-radius: 999px;
  background: color-mix(in srgb, var(--pe-brand) 12%, #fff);
  color: var(--pe-brand);
  padding: 0.16rem 0.42rem;
  font-size: 0.62rem;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.pe-candidate__top small {
  background: transparent;
  color: var(--pe-brand);
  padding: 0;
  font-size: 0.68rem;
}

.pe-candidate__top b {
  margin-left: auto;
  color: var(--pe-text);
  font-size: 0.82rem;
  font-weight: 900;
}

.pe-candidate small {
  overflow: hidden;
  color: var(--pe-text-3);
  font-size: 0.68rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pe-candidate i {
  height: 0.24rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--pe-line);
}

.pe-candidate i span,
.pe-bar-row i b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--pe-violet), var(--pe-brand));
}

.pe-empty {
  color: var(--pe-text-3);
  font-size: 0.78rem;
  font-weight: 800;
  padding: 1rem;
}

.pe-detail {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow: auto;
  padding: 0.85rem;
  background: var(--pe-bg);
}

.pe-mini-actions {
  position: sticky;
  top: 0;
  z-index: 8;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto auto;
  align-items: center;
  gap: 0.45rem;
  border: 0;
  border-bottom: 1px solid var(--pe-line);
  border-radius: 12px 12px 0 0;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 18px rgba(15, 17, 21, 0.08);
  margin: -0.85rem -0.85rem 0;
  padding: 0.42rem 0.85rem;
  backdrop-filter: blur(10px);
}

.pe-mini-actions strong {
  overflow: hidden;
  color: var(--pe-text);
  font-size: 0.8rem;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pe-mini-actions span {
  color: var(--pe-brand);
  font-size: 0.78rem;
  font-weight: 950;
}

.pe-mini-actions button,
.pe-hero__actions button {
  min-height: 1.9rem;
  border: 1px solid var(--pe-line);
  border-radius: 8px;
  background: #fff;
  color: var(--pe-text-2);
  padding: 0 0.72rem;
  font-size: 0.7rem;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.pe-mini-actions .primary,
.pe-hero__actions .primary {
  border-color: var(--pe-brand);
  background: var(--pe-brand);
  color: #fff;
}

.pe-mini-actions .danger,
.pe-hero__actions .danger {
  color: var(--pe-rose);
}

.pe-card,
.pe-callout {
  border: 1px solid var(--pe-line);
  border-radius: 12px;
  background: var(--pe-surface);
  padding: 1rem;
}

.pe-hero {
  display: grid;
  grid-template-columns: 3rem minmax(0, 1fr) 19rem;
  gap: 1rem;
  align-items: start;
}

.pe-hero__icon {
  display: grid;
  width: 3rem;
  height: 3rem;
  place-items: center;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--pe-violet), var(--pe-brand));
  color: #fff;
  font-size: 1rem;
  font-weight: 950;
}

.pe-hero__copy span,
.pe-callout span {
  color: var(--pe-text-3);
  font-size: 0.66rem;
  font-weight: 900;
  letter-spacing: 0.06em;
}

.pe-hero h3 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0.22rem 0 0;
  color: var(--pe-text);
  font-size: 1.12rem;
  font-weight: 950;
}

.pe-hero p,
.pe-callout p,
.pe-action-grid p,
.pe-comments p {
  margin: 0.35rem 0 0;
  color: var(--pe-text-2);
  font-size: 0.76rem;
  font-weight: 700;
  line-height: 1.55;
}

.pe-hero__copy div {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-top: 0.65rem;
}

.pe-hero__copy small {
  color: var(--pe-text-3);
  font-size: 0.68rem;
  font-weight: 800;
}

.pe-hero__copy small b {
  color: var(--pe-text);
}

.pe-hero__side {
  align-self: stretch;
  border-left: 1px solid var(--pe-line);
  display: grid;
  align-content: space-between;
  gap: 0.8rem;
  margin: -1rem -1rem -1rem 0;
  border-radius: 0 12px 12px 0;
  background: linear-gradient(180deg, #ffffff, var(--pe-muted));
  padding: 1rem;
}

.pe-score {
  text-align: right;
}

.pe-score strong {
  display: block;
  color: var(--pe-text);
  font-size: 2rem;
  font-weight: 950;
  line-height: 1;
}

.pe-score span {
  color: var(--pe-text-3);
  font-size: 0.7rem;
  font-weight: 800;
}

.pe-hero__actions {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.45rem;
}

.pe-callout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.45rem 0.85rem;
  border-color: color-mix(in srgb, var(--pe-brand) 24%, var(--pe-line));
  background: linear-gradient(135deg, #f7f5ff, #f0f2ff);
}

.pe-callout span,
.pe-callout p,
.pe-callout small {
  grid-column: 1;
}

.pe-callout button,
.pe-section-head button {
  align-self: start;
  border: 1px solid color-mix(in srgb, var(--pe-brand) 22%, var(--pe-line));
  border-radius: 8px;
  background: #fff;
  color: var(--pe-brand);
  padding: 0.35rem 0.65rem;
  font-size: 0.68rem;
  font-weight: 900;
  cursor: pointer;
}

.pe-callout small {
  color: var(--pe-brand);
  font-size: 0.68rem;
  font-weight: 800;
}

.pe-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.85rem;
}

.pe-section-head h4,
.pe-action-grid h5 {
  margin: 0;
  color: var(--pe-text);
  font-size: 0.86rem;
  font-weight: 950;
}

.pe-eval-grid {
  display: grid;
  grid-template-columns: minmax(15rem, 0.78fr) minmax(20rem, 1.22fr);
  gap: 1rem;
  align-items: stretch;
}

.pe-radar-panel {
  display: grid;
  min-height: 18rem;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--pe-brand) 14%, var(--pe-line));
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff, color-mix(in srgb, var(--pe-brand) 4%, #fff));
  padding: 0.75rem;
}

.pe-radar {
  width: min(100%, 19rem);
  height: auto;
  overflow: visible;
}

.pe-radar__grid {
  fill: color-mix(in srgb, var(--pe-brand) 5%, transparent);
  stroke: color-mix(in srgb, var(--pe-brand) 20%, var(--pe-line));
  stroke-width: 1;
}

.pe-radar__axis {
  stroke: color-mix(in srgb, var(--pe-brand) 18%, var(--pe-line));
  stroke-width: 1;
}

.pe-radar__shape {
  fill: color-mix(in srgb, var(--pe-brand) 24%, transparent);
  stroke: var(--pe-brand);
  stroke-width: 2.4;
  stroke-linejoin: round;
}

.pe-radar__point {
  fill: #fff;
  stroke: var(--pe-brand);
  stroke-width: 2;
}

.pe-radar text {
  fill: var(--pe-text-2);
  font-size: 0.48rem;
  font-weight: 850;
}

.pe-evidence {
  border: 1px solid var(--pe-line);
  border-radius: 10px;
  background: var(--pe-muted);
  padding: 0.85rem;
}

.pe-evidence > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.pe-evidence h5 {
  margin: 0;
  color: var(--pe-text);
  font-size: 0.78rem;
  font-weight: 950;
}

.pe-evidence > div:first-child strong {
  color: var(--pe-brand);
  font-size: 0.8rem;
  font-weight: 950;
}

.pe-evidence ul,
.pe-action-grid ul,
.pe-comments ol {
  display: grid;
  gap: 0.32rem;
  margin: 0.5rem 0 0;
  padding-left: 1rem;
}

.pe-evidence li,
.pe-action-grid li {
  color: var(--pe-text-2);
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 1.45;
}

.pe-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.55rem;
  margin-top: 0.75rem;
}

.pe-stat-grid span,
.pe-stat-grid button {
  border: 1px solid var(--pe-line);
  border-radius: 8px;
  background: #fff;
  color: var(--pe-text-3);
  padding: 0.55rem;
  font-size: 0.66rem;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
}

.pe-stat-grid button.active {
  border-color: color-mix(in srgb, var(--pe-brand) 38%, var(--pe-line));
  background: var(--pe-brand-soft);
  color: var(--pe-brand);
}

.pe-stat-grid b {
  display: block;
  margin-top: 0.18rem;
  color: var(--pe-text);
  font-size: 0.9rem;
  font-weight: 950;
}

.pe-dist {
  position: relative;
  height: 0.44rem;
  border-radius: 999px;
  background: linear-gradient(90deg, #f2eeff, #eceeff 42%, #dedfff 72%, var(--pe-brand));
  margin-top: 0.75rem;
}

.pe-dist i {
  position: absolute;
  top: -0.22rem;
  width: 0.12rem;
  height: 0.9rem;
  border-radius: 999px;
  background: var(--pe-text);
}

.pe-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
  margin: 0;
}

.pe-info-grid div {
  border: 1px solid var(--pe-line);
  border-radius: 9px;
  background: var(--pe-muted);
  padding: 0.72rem;
}

.pe-info-grid dt {
  color: var(--pe-text-3);
  font-size: 0.66rem;
  font-weight: 900;
}

.pe-info-grid dd {
  display: grid;
  gap: 0.18rem;
  margin: 0.3rem 0 0;
}

.pe-info-grid strong {
  color: var(--pe-text);
  font-size: 0.76rem;
  font-weight: 900;
}

.pe-info-grid small {
  color: var(--pe-text-3);
  font-size: 0.66rem;
  font-weight: 700;
}

.pe-action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.pe-action-grid--single {
  grid-template-columns: 1fr;
}

.pe-action-grid section {
  border: 1px solid var(--pe-line);
  border-radius: 10px;
  background: var(--pe-muted);
  padding: 0.85rem;
}

.pe-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0.45rem;
  margin-top: 0.75rem;
}

.pe-actions button {
  min-height: 2rem;
  border: 1px solid var(--pe-line);
  border-radius: 8px;
  background: #fff;
  color: var(--pe-text-2);
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
}

.pe-actions .primary {
  border-color: var(--pe-brand);
  background: var(--pe-brand);
  color: #fff;
}

.pe-actions .danger {
  color: var(--pe-rose);
}

.pe-comments ol {
  list-style: none;
  padding: 0;
}

.pe-comments li {
  position: relative;
  padding-left: 1.8rem;
}

.pe-comments li::before,
.pe-comment-input span {
  display: grid;
  width: 1.35rem;
  height: 1.35rem;
  place-items: center;
  border-radius: 999px;
  background: var(--pe-brand-soft);
  color: var(--pe-text-2);
  font-size: 0.62rem;
  font-weight: 900;
}

.pe-comments li::before {
  position: absolute;
  left: 0;
  top: 0;
  content: 'AD';
}

.pe-comments li b {
  color: var(--pe-text);
  font-size: 0.74rem;
}

.pe-comments li small {
  margin-left: 0.35rem;
  color: var(--pe-text-3);
  font-size: 0.66rem;
}

.pe-comment-input {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.5rem;
  align-items: center;
  border: 1px solid var(--pe-line);
  border-radius: 10px;
  background: var(--pe-muted);
  margin-top: 0.85rem;
  padding: 0.45rem;
}

.pe-comment-input input {
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--pe-text);
  font-size: 0.74rem;
  font-weight: 700;
}

.pe-comment-input button {
  border: 0;
  border-radius: 7px;
  background: var(--pe-brand);
  color: #fff;
  padding: 0.45rem 0.7rem;
  font-size: 0.7rem;
  font-weight: 900;
}

.pe-empty-detail {
  display: grid;
  place-items: center;
  align-content: center;
  text-align: center;
}

@media (max-width: 1100px) {
  .partner-eval,
  .pe-eval-grid,
  .pe-action-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .pe-hero,
  .pe-info-grid {
    grid-template-columns: 1fr;
  }

  .pe-hero__side {
    border-left: 0;
    padding-left: 0;
  }

  .pe-score {
    text-align: left;
  }

  .pe-mini-actions {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .pe-mini-actions button {
    width: 100%;
  }
}
</style>