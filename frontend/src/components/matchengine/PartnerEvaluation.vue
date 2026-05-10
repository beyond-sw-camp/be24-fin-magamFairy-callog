<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { getEvaluationResult } from '@/api/evaluation'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
  // 부모 컴포넌트에서 API 호출 후 배열을 넘겨줄 경우를 대비한 Prop
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

function getScoreFromBreakdown(candidate, label, fallback) {
  return candidate.scoreBreakdown?.find((item) => item.label === label)?.score ?? fallback
}

const goalLabels = {
  NEW_CUSTOMER: '신규 고객 유입',
  CUSTOMER_REVISIT: '기존 고객 재방문',
  MEMBER_SIGNUP: '회원 가입 유도',
  PURCHASE_BOOKING: '구매/예약 유도',
  BRAND_AWARENESS: '브랜드 인지도 확대',
  REVENUE: '매출 증대',
  UPSELL: '객단가/업셀 향상',
  DIRECT_BOOKING: '직접예약 비중 확대',
  REVIEW_REPUTATION: '리뷰/평판 개선',
  OTHER: '기타',
}

function getDetailMeta(candidate, label, fallback = '미입력') {
  return candidate.detailCards?.find((card) => card.label === label)?.meta ?? fallback
}

// 1. UI용 데이터 포맷으로 정규화하는 함수
function mapCandidateToProposal(candidate) {
  const score = Number(candidate.score ?? 0)
  const fallback = score || 75
  const title = candidate.title ?? '선택한 조합'
  const partnerName = candidate.partner ?? '파트너 미정'
  const scheduleMeta = getDetailMeta(candidate, '일정', candidate.schedule ?? '일정 미입력')

  return {
    id: candidate.id,
    isSample: candidate.isSample === true,
    campaignName: title,
    partnerName,
    benefitSummary: candidate.offer ?? candidate.title ?? '혜택 정보 미입력',
    goalLabel: goalLabels[candidate.goal] ?? candidate.goalLabel ?? '목표 미지정',
    period: scheduleMeta,
    status: candidate.statusLabel ?? '신규 추천',
    reviewStatus: candidate.reviewStatus ?? 'new',
    scores: {
      customerFit: getScoreFromBreakdown(candidate, '고객 적합도', fallback),
      revenue: getScoreFromBreakdown(candidate, '수익 기여도', fallback),
      cost: getScoreFromBreakdown(candidate, '비용 효율성', fallback),
      operation: getScoreFromBreakdown(candidate, '운영 용이성', fallback),
      brand: getScoreFromBreakdown(candidate, '브랜드 적합도', fallback),
    },
    comparison: candidate.comparison ?? null,
    warnings: candidate.risk ? [candidate.risk] : [],
    reason: candidate.reasons?.[0] ?? title + '의 평가 후보입니다.',
    targetKpis: candidate.targetKpis?.length ? candidate.targetKpis : [],
    detailCards: candidate.detailCards?.length
      ? candidate.detailCards
      : [
          { label: '보유 자산', value: candidate.asset ?? '미입력', meta: candidate.target ?? '대상 미입력' },
          { label: '파트너 혜택', value: candidate.offer ?? '미입력', meta: candidate.partner ?? '파트너 미입력' },
          { label: '채널', value: candidate.channels ?? '미입력', meta: '채널 정보 미입력' },
          { label: '산출물', value: candidate.outputs ?? '미입력', meta: '산출물 정보 미입력' },
          { label: '일정', value: candidate.schedule ?? '미입력', meta: '일정 정보 미입력' },
          { label: '리스크', value: candidate.risk ?? '미입력', meta: '리스크 정보 미입력' },
        ],
    evidence: [
      candidate.reasons?.[0] ?? '추천 사유 미입력',
      candidate.reasons?.[1] ?? '수익 기여 근거 미입력',
      getDetailMeta(candidate, '파트너 혜택', '비용 근거 미입력'),
      getDetailMeta(candidate, '일정', '운영 근거 미입력'),
      candidate.reasons?.[0] ?? '브랜드 적합 근거 미입력',
    ],
    riskMatrix: candidate.riskMatrix ?? null,
    nextActions: candidate.nextActions ?? [],
    comments: candidate.comments ?? [],
    detailedMetrics: candidate.detailedMetrics || null,
    manualScore: score || null,
  }
}

// 2. 서버에서 받은 JSON 데이터를 모델 규격으로 변환하는 어댑터 함수
function adaptServerDataToCandidate(serverData, index = 0) {
  const getScore = (evalObj) => evalObj?.overallScore ?? 0;
  
  const scores = {
    customerFit: getScore(serverData.customerEval),
    revenue: getScore(serverData.revenueEval),
    cost: getScore(serverData.costEval),
    operation: getScore(serverData.operationEval),
    brand: getScore(serverData.brandEval),
  };

  const finalScore = Math.round(
    scores.customerFit * 0.25 +
    scores.revenue * 0.25 +
    scores.cost * 0.2 +
    scores.operation * 0.15 +
    scores.brand * 0.15
  );

  return {
    id: `server-candidate-${index}-${Date.now()}`, // 배열 내 고유 ID 부여
    isSample: false,
    goal: serverData.goal || 'MEMBER_SIGNUP',
    title: serverData.title || '제목 미입력',
    partner: serverData.partner || '미상 (공동 프로모션)',
    offer: serverData.offer || '혜택 미입력',
    asset: serverData.assetDescription || '자산 정보 미상',
    target: serverData.target || '타겟 미정',
    schedule: `${serverData.startDate || '미정'} ~ ${serverData.endDate || '미정'}`,
    risk: serverData.operationEval?.improvementDirections?.[0] || '리스크 정보 없음',
    score: finalScore,
    reasons: [
      serverData.customerEval?.improvementDirections?.[0],
      serverData.revenueEval?.improvementDirections?.[0],
      serverData.costEval?.improvementDirections?.[0]
    ].filter(Boolean),
    scoreBreakdown: [
      { label: '고객 적합도', score: scores.customerFit },
      { label: '수익 기여도', score: scores.revenue },
      { label: '비용 효율성', score: scores.cost },
      { label: '운영 용이성', score: scores.operation },
      { label: '브랜드 적합도', score: scores.brand },
    ],
    targetKpis: ['신규 회원 가입 유도', '객단가(AOV) 상승 유도', '프리미엄 혜택 전환'],
    detailedMetrics: {
      customerFit: {
        text: serverData.customerEval?.customerAgeGroup,
        reasons: serverData.customerEval?.improvementDirections || []
      },
      revenue: {
        text: serverData.revenueEval?.purchaseConversionProbability,
        reasons: serverData.revenueEval?.improvementDirections || []
      },
      cost: {
        text: serverData.costEval?.partnerDiscountCostBurden,
        reasons: serverData.costEval?.improvementDirections || []
      },
      operation: {
        text: serverData.operationEval?.approvalStepsCount,
        reasons: serverData.operationEval?.improvementDirections || []
      },
      brand: {
        text: serverData.brandEval?.brandTone,
        reasons: serverData.brandEval?.improvementDirections || []
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
      
      // 데이터 세팅 후 첫 번째 항목 자동 선택
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

// // 4. 배열 데이터를 순회하며 UI 큐(Queue) 초기화
// proposalQueue.value = serverDataList
//   .map((data, index) => adaptServerDataToCandidate(data, index))
//   .map(mapCandidateToProposal)

/* 
 * [참고사항] 
 * 만약 하드코딩 배열(serverDataList) 대신 부모 컴포넌트로부터 
 * Props(serverCandidatesList)를 통해 배열을 동적으로 넘겨받는다면, 
 * 아래 Watch를 활성화하여 사용하시면 됩니다.
 * 
 * watch(() => props.serverCandidatesList, (newVal) => {
 *   if (newVal && newVal.length > 0) {
 *     proposalQueue.value = newVal
 *       .map((data, index) => adaptServerDataToCandidate(data, index))
 *       .map(mapCandidateToProposal)
 *     
 *     setActiveStatus('new')
 *     selectedId.value = proposalQueue.value[0]?.id ?? null
 *   }
 * }, { immediate: true })
 */

const metrics = [
  { key: 'customerFit', label: '고객 적합도', weight: 25 },
  { key: 'revenue', label: '수익 기여도', weight: 25 },
  { key: 'cost', label: '비용 효율성', weight: 20 },
  { key: 'operation', label: '운영 용이성', weight: 15 },
  { key: 'brand', label: '브랜드 적합도', weight: 15 },
]

const metricDetails = {
  customerFit: {
    benchmark: 78,
    reasons: [
      '타겟 고객군과 혜택 이용층이 겹칩니다.',
      '채널 접점이 명확해 캠페인 노출 손실이 적습니다.',
      '대상 고객이 구체적일수록 점수가 높게 산정됩니다.',
    ],
  },
  revenue: {
    benchmark: 74,
    reasons: [
      '가입, 구매, 예약 같은 전환 행동과 연결됩니다.',
      '성과 측정 KPI가 비교적 명확합니다.',
      '단순 노출형 캠페인보다 수익 기여를 크게 봅니다.',
    ],
  },
  cost: {
    benchmark: 76,
    reasons: [
      '파트너 부담 비용과 자체 채널 활용 가능성을 함께 봅니다.',
      '추가 제작비가 낮을수록 점수가 높습니다.',
      '혜택 규모 대비 운영 비용이 적정합니다.',
    ],
  },
  operation: {
    benchmark: 72,
    reasons: [
      '승인 단계와 제작 산출물 수를 기준으로 봅니다.',
      '법무/브랜드 검수가 많으면 점수가 낮아집니다.',
      '일정이 짧을수록 실행 리스크가 커집니다.',
    ],
  },
  brand: {
    benchmark: 80,
    reasons: [
      '브랜드 톤과 고객 경험의 연결성이 높습니다.',
      '평판 리스크가 낮을수록 점수가 높습니다.',
      '제휴사가 가진 이미지가 캠페인 메시지를 보강합니다.',
    ],
  },
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
  return metricDetails[activeMetric.value.key] ?? metricDetails.customerFit
})

const activeMetricEvidence = computed(() => {
  if (!selectedProposal.value) return ''
  if (selectedProposal.value.detailedMetrics?.[activeMetric.value.key]?.text) {
    return selectedProposal.value.detailedMetrics[activeMetric.value.key].text
  }
  return selectedProposal.value.evidence[activeMetricIndex.value] ?? selectedProposal.value.reason
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
            <p>{{ activeMetricEvidence }}</p>
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
        <ol>
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
  gap: 0.5rem;
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

.eval-workspace {
  display: grid;
  grid-template-columns: minmax(260px, 0.55fr) minmax(0, 1.45fr);
  gap: 0.7rem;
  height: 100%;
  min-height: 0;
}

.eval-list,
.eval-detail {
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  padding: 0.8rem;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
  min-height: 0;
}

.eval-list {
  display: grid;
  grid-auto-rows: max-content;
  align-content: start;
  gap: 0.5rem;
  overflow: auto;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-muted) 86%, var(--accent-soft)),
      var(--panel-muted)
    );
  border-color: color-mix(in srgb, var(--border-strong) 72%, var(--accent-color));
  box-shadow: inset -1px 0 0 color-mix(in srgb, var(--border-color) 72%, transparent);
}

.eval-detail {
  display: grid;
  align-content: start;
  gap: 0.75rem;
  overflow: auto;
  background: var(--panel-color);
  border-color: var(--border-strong);
}

.eval-head,
.eval-hero__main,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.eval-head h3,
.eval-hero h3,
.section-head h4,
.now-panel h5 {
  color: var(--text-primary);
  font-size: 0.95rem;
}

.eval-head span,
.eval-hero p,
.eval-item small,
.section-head span,
.metric-evidence p,
.kpi-list small,
.condition-list li,
.action-list li {
  color: var(--muted-text);
  font-size: 0.76rem;
}

.eval-status-tabs {
  display: grid;
  gap: 0.38rem;
}

.eval-status-tab {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  min-height: 2.2rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0 0.58rem;
  cursor: pointer;
  text-align: left;
}

.eval-status-tab span {
  overflow: hidden;
  font-size: 0.74rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.eval-status-tab b {
  display: inline-flex;
  min-width: 1.28rem;
  min-height: 1.28rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 900;
}

.eval-status-tab:hover,
.eval-status-tab.active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 9%, var(--panel-color));
  color: var(--text-primary);
}

.eval-status-tab.active b {
  background: var(--accent-color);
  color: #fff;
}

.eval-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 2.6rem;
  align-items: center;
  gap: 0.6rem;
  min-height: 4rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: color-mix(in srgb, var(--panel-color) 66%, var(--panel-muted));
  padding: 0.6rem;
  text-align: left;
}

.eval-item.active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 11%, var(--panel-color));
  box-shadow:
    0 5px 14px rgba(19, 35, 68, 0.06),
    inset 3px 0 0 var(--accent-color);
}

.eval-item span {
  display: grid;
  min-width: 0;
  gap: 0.15rem;
}

.eval-item__titleline,
.eval-hero__titleline {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
}

.eval-hero__titleline h3 {
  margin: 0;
}

.sample-badge {
  display: inline-flex;
  align-items: center;
  min-height: 1.25rem;
  border: 1px solid color-mix(in srgb, var(--accent-color) 28%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 9%, var(--panel-color));
  color: var(--accent-color);
  padding: 0 0.45rem;
  font-size: 0.62rem;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.eval-item strong {
  color: var(--text-primary);
  font-size: 0.86rem;
}

.eval-item small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.eval-item__status {
  display: inline-flex !important;
  width: fit-content;
  min-height: 1.25rem;
  align-items: center;
  border: 1px solid color-mix(in srgb, var(--accent-color) 22%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 7%, var(--panel-color));
  color: var(--accent-color) !important;
  padding: 0 0.46rem;
  font-size: 0.64rem !important;
  font-weight: 900 !important;
}

.eval-item b,
.eval-score strong {
  color: var(--accent-color);
}

.eval-hero,
.eval-what,
.eval-composition,
.eval-now,
.eval-collab {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-color) 80%, var(--panel-muted)),
      var(--panel-color)
  );
  padding: 0.85rem;
}

.eval-hero {
  border-color: color-mix(in srgb, var(--accent-color) 36%, var(--border-color));
  background:
    linear-gradient(
      135deg,
      color-mix(in srgb, var(--accent-color) 10%, var(--panel-color)),
      var(--panel-color) 62%
    );
}

.eval-score {
  min-width: 7rem;
  text-align: right;
}

.eval-score strong {
  display: block;
  font-size: 1.75rem;
  line-height: 1;
}

.eval-score span {
  color: var(--text-primary);
  font-size: 0.76rem;
  font-weight: 900;
}

.eval-assessment {
  display: grid;
  gap: 0.45rem;
  margin-top: 0.8rem;
  border: 1px solid color-mix(in srgb, var(--accent-color) 18%, var(--border-color));
  border-radius: 8px;
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-color));
  padding: 0.72rem 0.78rem;
}

.eval-assessment__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
}

.eval-assessment__head span {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.eval-assessment__head b {
  display: inline-flex;
  align-items: center;
  min-height: 1.45rem;
  border-radius: 999px;
  background: var(--accent-color);
  color: #fff;
  padding: 0 0.62rem;
  font-size: 0.7rem;
  font-weight: 900;
  white-space: nowrap;
}

.eval-assessment p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.8rem;
  font-weight: 750;
  line-height: 1.58;
}

.formula-toggle {
  margin-top: 0.65rem;
  border: 0;
  background: transparent;
  color: var(--accent-color);
  padding: 0;
  font-size: 0.72rem;
  font-weight: 900;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.conditions-toggle {
  margin-top: 0.65rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  padding: 0.38rem 0.55rem;
  font-size: 0.72rem;
  font-weight: 800;
}

.score-formula {
  margin-top: 0.45rem;
  border-top: 1px solid var(--border-color);
  padding-top: 0.45rem;
  color: var(--muted-text);
  font-size: 0.72rem;
  line-height: 1.45;
}

.metric-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.55fr);
  gap: 0.7rem;
  margin-top: 0.65rem;
}

.metric-bars {
  display: grid;
  gap: 0.5rem;
}

.eval-bar {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  align-items: center;
  gap: 0.55rem;
  width: 100%;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  padding: 0.5rem;
  text-align: left;
}

.eval-bar.active {
  border-color: color-mix(in srgb, var(--accent-color) 42%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-color));
}

.eval-bar span {
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 800;
}

.eval-bar small {
  color: var(--muted-text);
  font-size: 0.64rem;
}

.eval-bar__meter {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 2rem;
  align-items: center;
  gap: 0.32rem;
}

.eval-bar__track {
  height: 0.58rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-muted);
}

.eval-bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-color);
}

.eval-bar__meter strong {
  color: var(--text-primary);
  font-size: 0.78rem;
  text-align: left;
}

.metric-evidence {
  display: grid;
  align-content: start;
  gap: 0.55rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.7rem;
}

.metric-evidence__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.6rem;
}

.metric-evidence span,
.kpi-list span {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.metric-evidence strong {
  color: var(--accent-color);
  font-size: 1rem;
}

.metric-evidence ul {
  display: grid;
  gap: 0.28rem;
  margin: 0;
  padding-left: 1rem;
}

.metric-evidence li {
  color: var(--text-secondary);
  font-size: 0.72rem;
  line-height: 1.42;
}

.metric-evidence li::marker {
  color: var(--accent-color);
}

.metric-compare {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.35rem 0.55rem;
  border-top: 1px solid var(--border-color);
  padding-top: 0.55rem;
}

.metric-compare div:not(.metric-compare__bar) {
  display: grid;
  gap: 0.08rem;
}

.metric-compare b {
  color: var(--text-primary);
  font-size: 0.82rem;
}

.metric-compare__bar {
  position: relative;
  grid-column: 1 / -1;
  height: 0.46rem;
  border-radius: 999px;
  background: var(--panel-color);
}

.metric-compare__bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-color);
}

.metric-compare__bar em {
  position: absolute;
  top: -0.18rem;
  width: 2px;
  height: 0.82rem;
  border-radius: 999px;
  background: var(--color-warning-dark, #b45309);
}

.composition-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
  margin: 0.65rem 0 0;
}

.composition-grid div {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.65rem;
}

.composition-grid dt {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.composition-grid dd {
  display: grid;
  gap: 0.18rem;
  margin: 0.2rem 0 0;
}

.composition-grid strong {
  color: var(--text-primary);
  font-size: 0.8rem;
  line-height: 1.35;
}

.composition-grid small {
  color: var(--muted-text);
  font-size: 0.71rem;
  line-height: 1.42;
}

.now-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0.65rem;
  margin-top: 0.65rem;
}

.now-panel {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.7rem;
}

.kpi-list {
  display: grid;
  gap: 0.42rem;
  margin-top: 0.5rem;
}

.target-kpi-list {
  display: grid;
  gap: 0.38rem;
  margin: 0.55rem 0 0;
  padding: 0;
  list-style: none;
}

.target-kpi-list li {
  position: relative;
  padding-left: 0.85rem;
  color: var(--text-primary);
  font-size: 0.8rem;
  font-weight: 800;
  line-height: 1.45;
}

.target-kpi-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.58rem;
  width: 0.32rem;
  height: 0.32rem;
  border-radius: 999px;
  background: var(--accent-color);
}

.target-kpi-empty {
  margin: 0.55rem 0 0;
  color: var(--muted-text);
  font-size: 0.75rem;
  font-weight: 750;
  font-style: italic;
}

.kpi-list div {
  display: grid;
  grid-template-columns: minmax(72px, 0.6fr) minmax(64px, 0.4fr);
  gap: 0.1rem 0.45rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  padding: 0.48rem 0.55rem;
}

.kpi-list strong {
  color: var(--accent-color);
  font-size: 0.86rem;
  text-align: right;
}

.kpi-list small {
  grid-column: 1 / -1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.condition-list,
.action-list {
  display: grid;
  gap: 0.38rem;
  margin: 0.55rem 0 0;
  padding-left: 1rem;
}

.condition-list li,
.action-list li {
  line-height: 1.45;
}

.condition-list li::marker {
  color: var(--accent-color);
}

.action-list {
  margin-top: 0.5rem;
  padding-left: 0;
  list-style: none;
}

.action-list li {
  display: flex;
  align-items: flex-start;
  gap: 0.45rem;
  color: var(--text-secondary);
  font-weight: 700;
}

.action-list span {
  width: 0.82rem;
  height: 0.82rem;
  flex: 0 0 auto;
  margin-top: 0.1rem;
  border: 1px solid color-mix(in srgb, var(--accent-color) 48%, var(--border-color));
  border-radius: 3px;
  background: var(--panel-color);
}

.decision-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.8rem;
}

.decision-actions button {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0.48rem 0.7rem;
  font-size: 0.74rem;
  font-weight: 900;
}

.decision-actions .primary-action,
.decision-actions button.active {
  border-color: var(--accent-color);
  background: var(--accent-color);
  color: white;
}

.eval-hero__eyebrow {
  display: inline-flex;
  margin-bottom: 0.25rem;
  color: var(--accent-color);
  font-size: 0.68rem;
  font-weight: 900;
}

.eval-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.45rem;
}

.eval-hero__meta span,
.eval-hero__meta b {
  display: inline-flex;
  align-items: center;
  min-height: 1.35rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0 0.55rem;
  font-size: 0.68rem;
  font-weight: 850;
}

.eval-hero__meta b {
  border-color: color-mix(in srgb, var(--accent-color) 35%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: var(--accent-color);
}

.metric-compare small {
  grid-column: 1 / -1;
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 750;
}


.action-list--rich li {
  align-items: flex-start;
}

.action-list--rich div {
  display: grid;
  gap: 0.12rem;
  min-width: 0;
}

.action-list--rich strong {
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 900;
}

.action-list--rich small {
  color: var(--muted-text);
  font-size: 0.69rem;
  font-weight: 750;
  line-height: 1.35;
}

.eval-collab {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.65rem;
}

.collab-panel {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.7rem;
}

.comment-list {
  display: grid;
  gap: 0.45rem;
  margin: 0.55rem 0 0;
  padding: 0;
  list-style: none;
}

.comment-list li {
  display: grid;
  gap: 0.12rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  padding: 0.55rem 0.6rem;
}

.comment-list strong {
  color: var(--text-primary);
  font-size: 0.76rem;
  font-weight: 900;
}

.comment-list small {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 750;
}

.comment-list p {
  margin: 0.22rem 0 0;
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 750;
  line-height: 1.45;
}

.subtle-add {
  margin-top: 0.55rem;
  width: 100%;
  border: 1px dashed var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  color: var(--accent-color);
  padding: 0.48rem 0.6rem;
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
}

.subtle-add:hover {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 7%, var(--panel-color));
}

.decision-help {
  margin: 0.45rem 0 0;
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 750;
  line-height: 1.45;
}

.decision-actions--modal {
  display: flex;
  gap: 0.45rem;
  margin-top: 0.8rem;
}

.decision-btn {
  flex: 1;
  height: 2.35rem;
  border-radius: 7px;
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
}

.decision-btn--primary {
  flex: 1.5;
  border: 0;
  background: var(--accent-color);
  color: #fff;
}

.decision-btn--ghost,
.decision-btn--danger-ghost {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.decision-btn--ghost:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.decision-btn--danger-ghost:hover {
  border-color: color-mix(in srgb, #ef4444 35%, var(--border-color));
  background: color-mix(in srgb, #ef4444 8%, var(--panel-color));
  color: #b91c1c;
}

.decision-modal {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, 0.52);
  padding: 1rem;
}

.decision-modal__panel {
  width: min(430px, 100%);
  overflow: hidden;
  border-top: 4px solid var(--accent-color);
  border-radius: 11px;
  background: var(--panel-color);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.25);
}

.decision-modal__panel--neutral {
  border-top-color: #94a3b8;
}

.decision-modal__panel--danger {
  border-top-color: #ef4444;
}

.decision-modal__head {
  padding: 1.05rem 1.1rem 0.45rem;
}

.decision-modal__head h3 {
  margin: 0 0 0.3rem;
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 900;
}

.decision-modal__head p {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.75rem;
  font-weight: 750;
  line-height: 1.45;
}

.decision-modal__target {
  display: grid;
  gap: 0.15rem;
  margin: 0.6rem 1.1rem 0;
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.58rem 0.72rem;
}

.decision-modal__target span:first-child {
  color: var(--text-primary);
  font-size: 0.8rem;
  font-weight: 900;
}

.decision-modal__target span:last-child {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 750;
}

.decision-modal__field {
  display: grid;
  gap: 0.35rem;
  margin: 0.75rem 1.1rem 0;
}

.decision-modal__field label {
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 900;
}

.decision-modal__field label span {
  color: #ef4444;
}

.decision-modal__field textarea {
  width: 100%;
  min-height: 4.5rem;
  resize: vertical;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0.58rem 0.66rem;
  font: inherit;
  font-size: 0.78rem;
}

.decision-modal__field textarea:focus {
  outline: 0;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--accent-color) 24%, transparent);
}

.decision-modal__auto {
  margin: 0.85rem 1.1rem 0;
  border: 1px solid color-mix(in srgb, var(--accent-color) 24%, var(--border-color));
  border-radius: 8px;
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-color));
  padding: 0.66rem 0.75rem;
}

.decision-modal__auto > span {
  display: block;
  margin-bottom: 0.4rem;
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.decision-modal__auto ul {
  display: grid;
  gap: 0.32rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.decision-modal__auto li {
  display: flex;
  align-items: center;
  gap: 0.42rem;
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 780;
}

.decision-modal__auto i {
  color: var(--accent-color);
  font-style: normal;
  font-weight: 900;
}

.decision-modal__foot {
  display: flex;
  gap: 0.45rem;
  padding: 1rem 1.1rem 1.1rem;
}

.decision-modal__cancel,
.decision-modal__confirm {
  flex: 1;
  height: 2.3rem;
  border-radius: 7px;
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
}

.decision-modal__cancel {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.decision-modal__confirm {
  flex: 1.25;
  border: 0;
  color: #fff;
}

.decision-modal__confirm--primary {
  background: var(--accent-color);
}

.decision-modal__confirm--neutral {
  background: #475569;
}

.decision-modal__confirm--danger {
  background: #dc2626;
}

.decision-modal__confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.18s ease;
}

.modal-enter-active .decision-modal__panel,
.modal-leave-active .decision-modal__panel {
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .decision-modal__panel,
.modal-leave-to .decision-modal__panel {
  transform: scale(0.96);
  opacity: 0;
}

@media (max-width: 1180px) {
  .eval-workspace,
  .metric-layout,
  .composition-grid,
  .now-grid,
  .eval-collab {
    grid-template-columns: 1fr;
  }
}

.eval-empty-list {
  margin: 0;
  border: 1px dashed var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--muted-text);
  padding: 0.7rem;
  font-size: 0.74rem;
  font-weight: 800;
  line-height: 1.45;
}

.eval-detail--empty {
  min-height: 18rem;
  align-content: center;
  justify-items: center;
  text-align: center;
}

.eval-detail--empty strong {
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 900;
}

.eval-detail--empty p {
  margin: 0.35rem 0 0;
  color: var(--muted-text);
  font-size: 0.76rem;
  font-weight: 750;
}
</style>