<script setup>
import { computed, ref, watch } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const proposals = [
  {
    id: 1,
    partnerName: '나이키 코리아',
    benefitSummary: '러닝 앱 멤버십 공동 챌린지 및 리워드 굿즈',
    scores: { customerFit: 90, revenue: 85, cost: 80, operation: 70, brand: 95 },
    warnings: ['운영 일정 촉박'],
    reason: '2030 액티브 레저 고객층과 브랜드 타겟이 일치하며 앱 가입 전환을 기대할 수 있습니다.',
    kpis: [
      { label: '예상 참여', value: '18,000명', baseline: '최근 유사 캠페인 대비 +24%' },
      { label: '가입 전환', value: '+9.5%', baseline: '앱 미가입 고객 기준' },
      { label: '쿠폰 사용', value: '62%', baseline: '할당 쿠폰 1만 장 가정' },
    ],
    evidence: [
      '2030 액티브 고객군과 러닝 앱 주 이용층이 높게 겹칩니다.',
      '공동 챌린지 방식은 앱 가입과 SNS 확산을 동시에 만들기 쉽습니다.',
      '리워드 굿즈 제공으로 고객 체감 가치는 높지만 직접 비용 부담은 제한적입니다.',
      '랜딩, 고지, 리워드 지급 조건을 짧은 기간 안에 확정해야 합니다.',
      '스포츠·건강 이미지가 캠페인 브랜드 톤과 자연스럽게 맞습니다.',
    ],
    conditions: [
      '챌린지 시작 2주 전 랜딩/고지 문안 확정',
      '추천 안내는 앱 내 피트니스 미션으로 대체',
      '참여 리워드는 쿠폰형과 추첨형을 분리 운영',
    ],
    nextActions: ['나이키 담당자 일정 확정', '랜딩 와이어프레임 작성', '법무 유의사항 1차 검토'],
    manualScore: null,
  },
  {
    id: 2,
    partnerName: 'LG 생활건강',
    benefitSummary: '신규 뷰티 브랜드 제휴 기념 샘플링 키트 제공',
    scores: { customerFit: 70, revenue: 60, cost: 85, operation: 80, brand: 60 },
    warnings: ['고객층 불명확', '브랜드 적합도 검토'],
    reason: '비용 효율성은 좋지만 수익 기여와 고객 적합도는 보완 검토가 필요합니다.',
    kpis: [
      { label: '샘플 소진', value: '8,500개', baseline: '객실 비치 후 전환 가정' },
      { label: '재방문 기여', value: '+3.1%', baseline: '뷰티 관심군 기준' },
      { label: '후기 확보', value: '420건', baseline: 'QR 설문 응답률 5%' },
    ],
    evidence: [
      '뷰티 관심 고객군에는 맞지만 전체 캠페인 타겟과의 접점은 제한적입니다.',
      '직접 구매나 예약 전환으로 이어지는 연결 구조가 약합니다.',
      '샘플 비용을 파트너가 부담해 비용 효율은 높습니다.',
      '객실 비치 방식은 운영 부담이 비교적 낮습니다.',
      '신규 브랜드라 프리미엄 이미지와의 연결 근거가 부족합니다.',
    ],
    conditions: [
      '대상 고객군과 제품 라인업 재정의 필요',
      '샘플 수량, 배송비, 재고 회수 기준 확정',
      '브랜드별 검토 후 VIP/일반 고객 노출 분리',
    ],
    nextActions: ['파트너 샘플 조건 재요청', '고객군 세그먼트 재평가', '객실 비치 시나리오 비용 산정'],
    manualScore: 70.5,
  },
]

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

const selectedId = ref(proposals[0].id)
const activeMetricKey = ref(metrics[0].key)
const isFormulaOpen = ref(false)
const isConditionsOpen = ref(false)

const selectedProposal = computed(() => proposals.find((proposal) => proposal.id === selectedId.value) ?? proposals[0])
const selectedScore = computed(() => calculateScore(selectedProposal.value))
const activeMetric = computed(() => metrics.find((metric) => metric.key === activeMetricKey.value) ?? metrics[0])
const activeMetricIndex = computed(() => metrics.findIndex((metric) => metric.key === activeMetric.value.key))
const activeMetricEvidence = computed(() => {
  return selectedProposal.value.evidence[activeMetricIndex.value] ?? selectedProposal.value.reason
})
const activeMetricDetails = computed(() => metricDetails[activeMetric.value.key] ?? metricDetails.customerFit)
const topMetric = computed(() => {
  return metrics.reduce((top, metric) => {
    return selectedProposal.value.scores[metric.key] > selectedProposal.value.scores[top.key] ? metric : top
  }, metrics[0])
})
const weakestMetric = computed(() => {
  return metrics.reduce((weakest, metric) => {
    return selectedProposal.value.scores[metric.key] < selectedProposal.value.scores[weakest.key] ? metric : weakest
  }, metrics[0])
})
const headlineSummaries = computed(() => {
  const proposal = selectedProposal.value
  const warningsText = proposal.warnings.length
    ? `${proposal.warnings[0]} (리스크 ${proposal.warnings.length}건)`
    : `${weakestMetric.value.label} ${proposal.scores[weakestMetric.value.key]}점 보완 필요`

  return [
    `${topMetric.value.label} ${proposal.scores[topMetric.value.key]}점으로 강점이 뚜렷합니다.`,
    proposal.reason,
    warningsText,
  ]
})
const scoreFormula = computed(() => {
  return metrics
    .map((metric) => {
      const score = selectedProposal.value.scores[metric.key]
      return `${metric.label} ${score}×${(metric.weight / 100).toFixed(2)}`
    })
    .join(' + ')
})

watch(selectedId, () => {
  activeMetricKey.value = metrics[0].key
  isFormulaOpen.value = false
  isConditionsOpen.value = false
})

function calculateScore(proposal) {
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
          <strong>{{ proposal.partnerName }}</strong>
          <small>{{ proposal.benefitSummary }}</small>
        </span>
        <b>{{ calculateScore(proposal) }}</b>
      </button>
    </aside>

    <article class="eval-detail">
      <section class="eval-hero">
        <div class="eval-hero__main">
          <div>
            <h3>{{ selectedProposal.partnerName }}</h3>
            <p>{{ selectedProposal.benefitSummary }}</p>
          </div>
          <div class="eval-score">
            <strong>{{ selectedScore }}</strong>
            <span>{{ grade(selectedScore) }}</span>
          </div>
        </div>

        <ul class="eval-summary">
          <li
            v-for="(summary, index) in headlineSummaries"
            :key="summary"
            :class="{ primary: index === 0, risk: index === 2 }"
          >
            {{ index === 2 ? '!' : '✓' }} {{ summary }}
          </li>
        </ul>

        <button type="button" class="formula-toggle" @click="isFormulaOpen = !isFormulaOpen">
          점수 산식 {{ isFormulaOpen ? '접기 ▴' : '보기 ▾' }}
        </button>
        <p v-if="isFormulaOpen" class="score-formula">{{ selectedScore }} = {{ scoreFormula }}</p>
      </section>

      <section class="eval-what">
        <div class="section-head">
          <h4>세부 평가</h4>
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
                <span>타 파트너 평균</span>
                <b>{{ activeMetricDetails.benchmark }}</b>
              </div>
              <div class="metric-compare__bar">
                <i :style="{ width: `${selectedProposal.scores[activeMetric.key]}%` }" />
                <em :style="{ left: `${activeMetricDetails.benchmark}%` }" />
              </div>
            </div>
          </aside>
        </div>
      </section>

      <section class="eval-now">
        <div class="section-head">
          <h4>기대 효과와 실행</h4>
        </div>

        <div class="now-grid">
          <section class="now-panel">
            <h5>KPI 예측</h5>
            <div class="kpi-list">
              <div v-for="kpi in selectedProposal.kpis" :key="kpi.label">
                <span>{{ kpi.label }}</span>
                <strong>{{ kpi.value }}</strong>
                <small>{{ kpi.baseline }}</small>
              </div>
            </div>

            <button type="button" class="conditions-toggle" @click="isConditionsOpen = !isConditionsOpen">
              운영 조건 {{ isConditionsOpen ? '접기' : '펼치기' }}
            </button>
            <ol v-if="isConditionsOpen" class="condition-list">
              <li v-for="item in selectedProposal.conditions" :key="item">{{ item }}</li>
            </ol>
          </section>

          <section class="now-panel now-panel--actions">
            <h5>다음 액션</h5>
            <ol class="action-list">
              <li v-for="item in selectedProposal.nextActions" :key="item">
                <span aria-hidden="true"></span>
                {{ item }}
              </li>
            </ol>
            <div class="decision-actions">
              <button type="button" class="primary-action">진행하기</button>
              <button type="button">보류</button>
              <button type="button">제외</button>
            </div>
          </section>
        </div>
      </section>
    </article>
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
.eval-now {
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

.eval-summary {
  display: grid;
  gap: 0.38rem;
  margin: 0.75rem 0 0;
  padding: 0;
  list-style: none;
}

.eval-summary li {
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.42;
}

.eval-summary li.primary {
  color: var(--text-primary);
  font-size: 0.86rem;
  font-weight: 900;
}

.eval-summary li.risk {
  color: var(--color-warning-dark, #b45309);
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

.decision-actions .primary-action {
  border-color: var(--accent-color);
  background: var(--accent-color);
  color: white;
}

@media (max-width: 1180px) {
  .eval-workspace,
  .metric-layout,
  .now-grid {
    grid-template-columns: 1fr;
  }
}
</style>
