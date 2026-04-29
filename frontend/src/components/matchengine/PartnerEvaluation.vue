<script setup>
import { computed, ref } from 'vue'

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
    benefitSummary: '러닝앱 멤버십 공동 챌린지 및 리미티드 굿즈',
    scores: { customerFit: 90, revenue: 85, cost: 80, operation: 70, brand: 95 },
    warnings: ['운영 일정 촉박'],
    reason: '2030 액티브 레저 고객층과 브랜드 타겟이 일치하며 앱 가입 전환이 기대됩니다.',
    kpis: [
      { label: '예상 참여', value: '18,000명', baseline: '최근 레저 캠페인 대비 +24%' },
      { label: '가입 전환', value: '+9.5%', baseline: '앱 미가입 타깃 기준' },
      { label: '쿠폰 사용', value: '62%', baseline: '선착순 1만 장 가정' },
    ],
    evidence: [
      '한화리조트 2030 액티브 고객군과 NRC 주 이용층 중복률 68%',
      '러닝 챌린지는 SNS 공유 소재로 전환하기 쉬워 콘텐츠 확산 가능',
      '리워드 굿즈 제공으로 혜택 체감가는 높지만 직접 비용 부담은 제한적',
    ],
    conditions: [
      '챌린지 시작 2주 전 랜딩/푸시 문안 확정',
      '우천 시 실내 피트니스 미션으로 대체',
      '참여 리워드는 선착순과 추첨형을 분리 운영',
    ],
    nextActions: ['나이키 담당자 일정 확정', '랜딩 와이어프레임 작성', '법무 유의사항 1차 검토'],
    manualScore: null,
  },
  {
    id: 2,
    partnerName: 'LG 생활건강',
    benefitSummary: '신규 뷰티 브랜드 런칭 기념 샘플링 키트 제공',
    scores: { customerFit: 70, revenue: 60, cost: 85, operation: 80, brand: 60 },
    warnings: ['고객층 불명확', '브랜드 적합도 검토'],
    reason: '비용 효율성은 좋지만 수익 기여도와 고객 적합도 보완이 필요합니다.',
    kpis: [
      { label: '샘플 소진', value: '8,500개', baseline: '객실 비치형 전환 시' },
      { label: '재방문 기여', value: '+3.1%', baseline: '뷰티 관심군 기준' },
      { label: '후기 확보', value: '420건', baseline: 'QR 설문 응답률 5%' },
    ],
    evidence: [
      '샘플 비용을 파트너가 부담해 비용 효율성은 높음',
      '신규 브랜드라 기존 한화 프리미엄 이미지와의 연결 근거가 부족',
      '호텔 객실 비치형으로 전환하면 고객 접점은 명확해질 수 있음',
    ],
    conditions: [
      '대상 고객군과 제품 라인업 재정의 필요',
      '샘플 수량, 배송비, 재고 회수 기준 확정',
      '브랜드 톤 검수 후 VIP/일반 고객 노출 분리',
    ],
    nextActions: ['파트너 혜택 조건 재요청', '고객군 세그먼트 재평가', '객실 비치 시나리오 비용 산정'],
    manualScore: 70.5,
  },
]

const metrics = [
  { key: 'customerFit', label: '고객 적합도', weight: 25 },
  { key: 'revenue', label: '수익 기여도', weight: 25 },
  { key: 'cost', label: '비용 효율성', weight: 20 },
  { key: 'operation', label: '운영 난이도', weight: 15 },
  { key: 'brand', label: '브랜드 적합도', weight: 15 },
]

const selectedId = ref(proposals[0].id)
const selectedProposal = computed(() => proposals.find((proposal) => proposal.id === selectedId.value) ?? proposals[0])
const reviewBrief = computed(() => {
  const proposal = selectedProposal.value
  const score = calculateScore(proposal)

  return [
    { label: '판정', value: grade(score) },
    { label: '보정', value: proposal.manualScore === null ? '미적용' : '적용됨' },
    { label: '리스크', value: `${proposal.warnings.length}건` },
  ]
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
      <div class="eval-title">
        <div>
          <h3>{{ selectedProposal.partnerName }}</h3>
          <p>{{ selectedProposal.benefitSummary }}</p>
        </div>
        <div class="eval-score">
          <strong>{{ calculateScore(selectedProposal) }}</strong>
          <span>{{ grade(calculateScore(selectedProposal)) }}</span>
        </div>
      </div>

      <div class="eval-grid">
        <div class="eval-metrics">
          <div v-for="metric in metrics" :key="metric.key" class="eval-bar">
            <span>{{ metric.label }} <small>{{ metric.weight }}%</small></span>
            <div><i :style="{ width: `${selectedProposal.scores[metric.key]}%` }" /></div>
            <strong>{{ selectedProposal.scores[metric.key] }}</strong>
          </div>
        </div>

        <aside class="eval-note">
          <h4>추천 사유</h4>
          <p>{{ selectedProposal.reason }}</p>
          <h4>검토 필요</h4>
          <ul>
            <li v-for="warning in selectedProposal.warnings" :key="warning">{{ warning }}</li>
          </ul>
        </aside>
      </div>

      <div class="eval-review-grid">
        <section class="review-panel review-panel--summary">
          <div class="review-panel__head">
            <h4>검토 요약</h4>
            <span>Review brief</span>
          </div>
          <div class="review-brief">
            <div v-for="item in reviewBrief" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>

        <section class="review-panel">
          <div class="review-panel__head">
            <h4>KPI 가정</h4>
            <span>Forecast</span>
          </div>
          <div class="review-kpi-list">
            <div v-for="kpi in selectedProposal.kpis" :key="kpi.label">
              <span>{{ kpi.label }}</span>
              <strong>{{ kpi.value }}</strong>
              <small>{{ kpi.baseline }}</small>
            </div>
          </div>
        </section>

        <section class="review-panel">
          <div class="review-panel__head">
            <h4>평가 근거</h4>
            <span>Evidence</span>
          </div>
          <ol class="review-list">
            <li v-for="item in selectedProposal.evidence" :key="item">{{ item }}</li>
          </ol>
        </section>

        <section class="review-panel">
          <div class="review-panel__head">
            <h4>운영 조건</h4>
            <span>Constraints</span>
          </div>
          <ol class="review-list">
            <li v-for="item in selectedProposal.conditions" :key="item">{{ item }}</li>
          </ol>
        </section>

        <section class="review-panel">
          <div class="review-panel__head">
            <h4>다음 액션</h4>
            <span>Owner tasks</span>
          </div>
          <ol class="review-list review-list--action">
            <li v-for="item in selectedProposal.nextActions" :key="item">{{ item }}</li>
          </ol>
        </section>
      </div>
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
  background: var(--panel-color);
  border-color: var(--border-strong);
}

.eval-head,
.eval-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.65rem;
}

.eval-head h3,
.eval-title h3,
.eval-note h4 {
  color: var(--text-primary);
  font-size: 0.95rem;
}

.eval-head span,
.eval-title p,
.eval-item small,
.eval-note p,
.eval-note li {
  color: var(--muted-text);
  font-size: 0.76rem;
}

.eval-list {
  display: grid;
  grid-auto-rows: max-content;
  align-content: start;
  gap: 0.5rem;
  overflow: auto;
}

.eval-detail {
  overflow: auto;
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

.eval-title {
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.65rem;
}

.eval-score {
  min-width: 7rem;
  text-align: right;
}

.eval-score strong {
  display: block;
  font-size: 1.55rem;
  line-height: 1;
}

.eval-score span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 800;
}

.eval-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.62fr);
  gap: 0.7rem;
  min-height: 0;
}

.eval-metrics {
  display: grid;
  gap: 0.58rem;
}

.eval-bar {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) 34px;
  align-items: center;
  gap: 0.55rem;
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

.eval-bar div {
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

.eval-bar strong {
  color: var(--text-primary);
  font-size: 0.78rem;
  text-align: right;
}

.eval-note {
  display: grid;
  align-content: start;
  gap: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.7rem;
}

.eval-note ul {
  display: grid;
  gap: 0.35rem;
  margin: 0;
  padding-left: 1rem;
}

.eval-review-grid {
  display: grid;
  grid-template-columns: minmax(180px, 0.65fr) minmax(260px, 1fr) repeat(3, minmax(0, 1fr));
  gap: 0.6rem;
  margin-top: 0.7rem;
}

.review-panel {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-color) 76%, var(--panel-muted)),
      var(--panel-muted)
    );
  padding: 0.65rem;
}

.review-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.42rem;
}

.review-panel__head h4 {
  color: var(--text-primary);
  font-size: 0.82rem;
}

.review-panel__head span {
  color: var(--muted-text);
  font-size: 0.62rem;
  font-weight: 900;
  text-transform: uppercase;
}

.review-brief,
.review-kpi-list {
  display: grid;
  gap: 0.42rem;
}

.review-brief div,
.review-kpi-list div {
  display: grid;
  gap: 0.08rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  padding: 0.45rem 0.5rem;
}

.review-brief span,
.review-kpi-list span,
.review-kpi-list small {
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 800;
}

.review-brief strong,
.review-kpi-list strong {
  color: var(--accent-color);
  font-size: 0.84rem;
  line-height: 1.15;
}

.review-kpi-list small {
  overflow: hidden;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-list {
  display: grid;
  gap: 0.34rem;
  margin: 0;
  padding-left: 1.05rem;
}

.review-list li {
  color: var(--text-secondary);
  font-size: 0.7rem;
  line-height: 1.42;
}

.review-list li::marker {
  color: var(--accent-color);
  font-weight: 900;
}

.review-list--action li {
  font-weight: 700;
}

@media (max-width: 1180px) {
  .eval-workspace,
  .eval-grid,
  .eval-review-grid {
    grid-template-columns: 1fr;
  }
}
</style>
