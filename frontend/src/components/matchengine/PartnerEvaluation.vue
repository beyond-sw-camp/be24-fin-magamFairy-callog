<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
  evaluationCandidate: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['decided'])

const proposalQueue = ref([])
const proposals = computed(() => proposalQueue.value)

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
    status: '검토 중',
    scores: {
      customerFit: getScoreFromBreakdown(candidate, '고객 적합도', fallback),
      revenue: getScoreFromBreakdown(candidate, '수익 기여도', fallback),
      cost: getScoreFromBreakdown(candidate, '비용 효율성', fallback),
      operation: getScoreFromBreakdown(candidate, '운영 용이성', fallback),
      brand: getScoreFromBreakdown(candidate, '브랜드 적합도', fallback),
    },
    comparison: {
      goalAverage: Math.max(62, Math.round(fallback - 16)),
      goalSample: 12,
      partnerAverage: Math.max(60, Math.round(fallback - 6)),
      partnerCases: 3,
      percentile: fallback >= 90 ? '상위 12%' : fallback >= 80 ? '상위 28%' : '상위 45%',
    },
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
    riskMatrix: {
      impact: '중',
      probability: '중',
      checklist: ['일별 사용량 제한', '조기 소진 알림', '대체 혜택 문구 준비'],
    },
    nextActions: [
      { title: '파트너 조건 확인', owner: '김캘리', due: 'D-2', priority: '높음', dependency: '' },
      { title: '운영 일정 검토', owner: '박운영', due: 'D-5', priority: '보통', dependency: '조건 확인 후' },
      { title: '검토 의견 기록', owner: '김캘리', due: 'D-1', priority: '낮음', dependency: '' },
    ],
    comments: [
      { author: '김캘리', time: '05.06 14:23', text: '브랜드 적합도가 높아 우선 진행 의견입니다.' },
      { author: '박운영', time: '05.06 15:10', text: '운영 일정만 확인되면 바로 시작 가능합니다.' },
    ],
    manualScore: score || null,
  }
}

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

const selectedId = ref(null)
const activeMetricKey = ref(metrics[0].key)
const pendingDecision = ref(null)
const decisionReason = ref('')
const isFormulaOpen = ref(false)
const isConditionsOpen = ref(false)

const selectedProposal = computed(
  () => proposals.value.find((proposal) => proposal.id === selectedId.value) ?? proposals.value[0] ?? null,
)
const selectedScore = computed(() => (selectedProposal.value ? calculateScore(selectedProposal.value) : 0))
const activeMetric = computed(() => metrics.find((metric) => metric.key === activeMetricKey.value) ?? metrics[0])
const activeMetricIndex = computed(() => metrics.findIndex((metric) => metric.key === activeMetric.value.key))
const activeMetricEvidence = computed(() => {
  if (!selectedProposal.value) return ''
  return selectedProposal.value.evidence[activeMetricIndex.value] ?? selectedProposal.value.reason
})
const activeMetricDetails = computed(() => metricDetails[activeMetric.value.key] ?? metricDetails.customerFit)
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
const overallAssessment = computed(() => {
  const proposal = selectedProposal.value
  if (!proposal) return null

  const topLabel = topMetric.value.label
  const topScore = proposal.scores[topMetric.value.key]
  const weakLabel = weakestMetric.value.label
  const weakScore = proposal.scores[weakestMetric.value.key]
  const riskText = proposal.warnings.length
    ? `${proposal.warnings[0]} 확인이 선행되어야 합니다`
    : `${weakLabel} ${weakScore}점 항목만 보완하면 실행 가능성이 높습니다`

  const recommendation =
    selectedScore.value >= 90
      ? '진행 추천'
      : selectedScore.value >= 80
        ? '조건부 진행'
        : '보류 권고'

  const description =
    `${topLabel}가 ${topScore}점으로 가장 강한 후보입니다. ${proposal.reason} ` +
    `다만 ${riskText}. ${recommendation} 기준으로 검토하되, ${weakLabel} 보완 여부를 확인한 뒤 다음 단계로 넘기는 것을 권장합니다.`

  return { recommendation, description }
})
const scoreFormula = computed(() => {
  if (!selectedProposal.value) return ''
  return metrics
    .map((metric) => {
      const score = selectedProposal.value.scores[metric.key]
      return `${metric.label} ${score}×${(metric.weight / 100).toFixed(2)}`
    })
    .join(' + ')
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

function openDecisionConfirm(decision) {
  pendingDecision.value = decision
  decisionReason.value = ''
}

function closeDecisionConfirm() {
  pendingDecision.value = null
  decisionReason.value = ''
}

function confirmDecision() {
  const config = currentDecisionConfig.value
  if (!config) return
  if (config.requireReason && !decisionReason.value.trim()) return
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
})

watch(
  () => props.evaluationCandidate,
  (candidate) => {
    if (!candidate?.id) return
    const mapped = mapCandidateToProposal(candidate)
    const index = proposalQueue.value.findIndex((proposal) => proposal.id === mapped.id)
    if (index >= 0) proposalQueue.value.splice(index, 1, mapped)
    else proposalQueue.value.unshift(mapped)
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
  return '보완 필요'
}
</script>

<template>
  <section class="eval-workspace">
    <aside class="eval-list">
      <div class="eval-head">
        <h3>평가 목록</h3>
        <span>{{ proposals.length }}건</span>
      </div>

      <button
        v-for="proposal in proposals"
        :key="proposal.id"
        type="button"
        class="eval-item"
        :class="{ active: selectedId === proposal.id }"
        @click="selectedId = proposal.id"
      >
        <span>
          <span class="eval-item__titleline">
            <strong>{{ proposal.partnerName }}</strong>
            <em v-if="proposal.isSample" class="sample-badge">샘플</em>
          </span>
          <small>{{ proposal.benefitSummary }}</small>
        </span>
        <b>{{ calculateScore(proposal) }}</b>
      </button>

      <p v-if="!proposals.length" class="eval-empty-list">
        추천 조합에서 평가로 보낸 후보가 없습니다.
      </p>
    </aside>

    <article v-if="selectedProposal" class="eval-detail">
      <section class="eval-hero">
        <div class="eval-hero__main">
          <div>
            <span class="eval-hero__eyebrow">캠페인 평가</span>
            <div class="eval-hero__titleline">
              <h3>{{ selectedProposal.campaignName }}</h3>
              <em v-if="selectedProposal.isSample" class="sample-badge">샘플</em>
            </div>
            <p>{{ selectedProposal.partnerName }} · {{ selectedProposal.benefitSummary }}</p>
            <div class="eval-hero__meta">
              <span>목표 {{ selectedProposal.goalLabel }}</span>
              <span>기간 {{ selectedProposal.period }}</span>
              <b>{{ selectedProposal.status }}</b>
            </div>
          </div>
          <div class="eval-score">
            <strong>{{ selectedScore }}</strong>
            <span>{{ grade(selectedScore) }}</span>
          </div>
        </div>

        <section v-if="overallAssessment" class="eval-assessment">
          <div class="eval-assessment__head">
            <span>종합 평가</span>
            <b>{{ overallAssessment.recommendation }}</b>
          </div>
          <p>{{ overallAssessment.description }}</p>
        </section>

        <button type="button" class="formula-toggle" @click="isFormulaOpen = !isFormulaOpen">
          점수 산식 {{ isFormulaOpen ? '접기 ▴' : '보기 ▾' }}
        </button>
        <p v-if="isFormulaOpen" class="score-formula">{{ selectedScore }} = {{ scoreFormula }}</p>
      </section>

      <section class="eval-what">
        <div class="section-head">
          <h4>세부 평가</h4>
          <span>막대를 클릭해 항목별 근거 보기</span>
        </div>

        <div class="metric-layout">
          <div class="metric-bars">
            <button
              v-for="metric in metrics"
              :key="metric.key"
              type="button"
              class="eval-bar"
              :class="{ active: activeMetricKey === metric.key }"
              @click="activeMetricKey = metric.key"
            >
              <span>{{ metric.label }} <small>{{ metric.weight }}%</small></span>
              <div class="eval-bar__meter">
                <div class="eval-bar__track"><i :style="{ width: `${selectedProposal.scores[metric.key]}%` }" /></div>
                <strong>{{ selectedProposal.scores[metric.key] }}</strong>
              </div>
            </button>
          </div>

          <aside class="metric-evidence">
            <div class="metric-evidence__head">
              <span>{{ activeMetric.label }} 근거</span>
              <strong>{{ selectedProposal.scores[activeMetric.key] }}점</strong>
            </div>
            <p>{{ activeMetricEvidence }}</p>
            <ul>
              <li v-for="reason in activeMetricDetails.reasons" :key="reason">{{ reason }}</li>
            </ul>
            <div class="metric-compare">
              <div>
                <span>선택 제안</span>
                <b>{{ selectedProposal.scores[activeMetric.key] }}</b>
              </div>
              <div>
                <span>같은 목표 평균</span>
                <b>{{ selectedProposal.comparison.goalAverage }}</b>
              </div>
              <div>
                <span>파트너 과거 평균</span>
                <b>{{ selectedProposal.comparison.partnerAverage }}</b>
              </div>
              <div>
                <span>평가 분포</span>
                <b>{{ selectedProposal.comparison.percentile }}</b>
              </div>
              <div class="metric-compare__bar">
                <i :style="{ width: selectedProposal.scores[activeMetric.key] + '%' }" />
                <em :style="{ left: selectedProposal.comparison.goalAverage + '%' }" />
              </div>
              <small>같은 목표 유형 {{ selectedProposal.comparison.goalSample }}건 기준 · 과거 {{ selectedProposal.comparison.partnerCases }}건 비교</small>
            </div>
          </aside>
        </div>
      </section>

      <section class="eval-composition">
        <div class="section-head">
          <h4>조합 구성과 운영 정보</h4>
        </div>

        <dl class="composition-grid">
          <div v-for="card in selectedProposal.detailCards" :key="card.label">
            <dt>{{ card.label }}</dt>
            <dd>
              <strong>{{ card.value }}</strong>
              <small>{{ card.meta }}</small>
            </dd>
          </div>
        </dl>
      </section>

      <section class="eval-now">
        <div class="section-head">
          <h4>목표와 실행</h4>
        </div>

        <div class="now-grid">
          <section class="now-panel">
            <h5>목표 KPI</h5>
            <ul v-if="selectedProposal.targetKpis.length" class="target-kpi-list">
              <li v-for="kpi in selectedProposal.targetKpis" :key="kpi">
                {{ kpi }}
              </li>
            </ul>
            <p v-else class="target-kpi-empty">KPI가 설정되지 않았습니다.</p>
          </section>

          <section class="now-panel now-panel--actions">
            <h5>결정</h5>
            <p class="decision-help">결정 버튼을 누르면 자동 처리 내용을 확인한 뒤 확정합니다.</p>
            <div class="decision-actions decision-actions--modal">
              <button type="button" class="decision-btn decision-btn--primary" @click="openDecisionConfirm('proceed')">
                진행하기
              </button>
              <button type="button" class="decision-btn decision-btn--ghost" @click="openDecisionConfirm('hold')">
                보류
              </button>
              <button type="button" class="decision-btn decision-btn--danger-ghost" @click="openDecisionConfirm('exclude')">
                제외
              </button>
            </div>
          </section>
        </div>
      </section>

      <section class="eval-collab">
        <section class="collab-panel">
          <div class="section-head">
            <h4>코멘트</h4>
            <span>{{ selectedProposal.comments.length }}건</span>
          </div>
          <ol class="comment-list">
            <li v-for="comment in selectedProposal.comments" :key="comment.author + comment.time">
              <strong>{{ comment.author }}</strong>
              <small>{{ comment.time }}</small>
              <p>{{ comment.text }}</p>
            </li>
          </ol>
          <button type="button" class="subtle-add">+ 코멘트 작성</button>
        </section>
      </section>
    </article>

    <article v-else class="eval-detail eval-detail--empty">
      <strong>평가할 후보가 없습니다.</strong>
      <p>추천 조합 탭에서 후보를 평가로 보내면 이곳에 표시됩니다.</p>
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
