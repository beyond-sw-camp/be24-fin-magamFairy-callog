<script setup>
import { computed, ref } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const summaryStats = [
  { label: '등록 자산', value: '12', helper: '활성 9', stage: 'Asset Pool' },
  { label: '제안 접수', value: '8', helper: '오늘 2', stage: 'Intake' },
  { label: '자동 평가', value: '6', helper: '검토 대기 2', stage: 'Scoring' },
  { label: '조합 추천', value: '3', helper: '90점 이상 1', stage: 'Matching' },
  { label: '운영 전환', value: '2', helper: '보드 생성', stage: 'Handoff' },
]

const partnerProposals = [
  {
    id: 1,
    partnerName: '스타벅스 코리아',
    benefitSummary: '시즌 음료 사이즈업 쿠폰 1만장 및 앱 배너 노출',
    totalScore: 94,
    grade: '최우선 추천',
    date: '04.28',
    owner: '브랜드제휴팀',
    status: '평가 완료',
    contact: '파트너 제휴팀 / 응답 1일 이내',
    condition: '쿠폰 10,000장, 스타벅스 앱 동시 공지 가능',
    scoreBreakdown: ['고객 적합도 96', '혜택 매력도 92', '운영 난이도 88'],
    risks: ['쿠폰 조기 소진 가능성', 'VIP 고객 중복 노출 빈도 관리 필요'],
    actions: ['추천 조합 생성', '혜택 조건서 검토', '파트너 미팅 일정 등록'],
  },
  {
    id: 2,
    partnerName: '나이키 코리아',
    benefitSummary: '러닝앱 멤버십 공동 챌린지 및 리미티드 굿즈',
    totalScore: 85,
    grade: '우선 검토',
    date: '04.27',
    owner: '호텔마케팅팀',
    status: '조건 협의',
    contact: 'NRC 마케팅 담당 / 일정 확인 중',
    condition: '러닝 챌린지 4주 운영, 리워드 굿즈 2,000개',
    scoreBreakdown: ['고객 적합도 90', '수익 기여도 85', '브랜드 적합도 95'],
    risks: ['오프라인 클래스 일정 촉박', '우천 시 대체 미션 필요'],
    actions: ['운영 가능 일정 확인', '패키지 가격안 작성', '법무 유의사항 검토'],
  },
  {
    id: 3,
    partnerName: 'CGV',
    benefitSummary: 'VIP 고객 대상 프리미엄 관람권 1+1 혜택',
    totalScore: 76,
    grade: '조건부 검토',
    date: '04.25',
    owner: 'CRM팀',
    status: '검토 대기',
    contact: 'CGV 제휴 담당 / 응답 2일 이내',
    condition: '프리미엄 상영관 예매권 1+1, 앱 신규 가입 CTA 연동',
    scoreBreakdown: ['고객 적합도 78', '전환 기대 84', '운영 난이도 86'],
    risks: ['예매권 사용 조건 복잡', '가입 전환 추적 코드 필요'],
    actions: ['쿠폰 조건 단순화 요청', '가입 랜딩 문구 작성', 'A/B 테스트 설계'],
  },
  {
    id: 4,
    partnerName: 'LG 생활건강',
    benefitSummary: '신규 뷰티 브랜드 런칭 기념 샘플링 키트 제공',
    totalScore: 65,
    grade: '보완 필요',
    date: '04.24',
    owner: '브랜드제휴팀',
    status: '보완 요청',
    contact: '브랜드 BM / 샘플 수량 확인 중',
    condition: '샘플링 키트 8,500개, 객실 비치형 전환 검토',
    scoreBreakdown: ['비용 효율성 85', '고객 적합도 70', '브랜드 적합도 60'],
    risks: ['타깃 고객층 불명확', '브랜드 이미지 연결 근거 부족'],
    actions: ['대상 고객군 재정의', '제품 라인업 재요청', '객실 비치 비용 산정'],
  },
]

const selectedProposalId = ref(partnerProposals[0].id)
const selectedProposal = computed(
  () =>
    partnerProposals.find((proposal) => proposal.id === selectedProposalId.value) ??
    partnerProposals[0],
)

function gradeClass(score) {
  if (score >= 90) return 'dash-grade--strong'
  if (score >= 80) return 'dash-grade--info'
  if (score >= 70) return 'dash-grade--warning'
  return 'dash-grade--danger'
}
</script>

<template>
  <section class="match-dashboard">
    <div class="dash-stat-grid">
      <article v-for="stat in summaryStats" :key="stat.label" class="dash-stat">
        <small>{{ stat.stage }}</small>
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <em>{{ stat.helper }}</em>
      </article>
    </div>

    <div class="dash-layout">
      <article class="dash-panel dash-panel--wide">
        <div class="dash-panel__head">
          <h3>신규 파트너 제안</h3>
          <span>최신순</span>
        </div>

        <div class="dash-table">
          <div class="dash-table__head">
            <span>파트너</span>
            <span>혜택</span>
            <span>담당</span>
            <span>점수</span>
            <span>등급</span>
          </div>
          <button
            v-for="proposal in partnerProposals"
            :key="proposal.id"
            type="button"
            class="dash-table__row"
            :class="{ 'dash-table__row--active': selectedProposal.id === proposal.id }"
            @click="selectedProposalId = proposal.id"
          >
            <strong>{{ proposal.partnerName }}</strong>
            <span>{{ proposal.benefitSummary }}</span>
            <span>{{ proposal.owner }}</span>
            <b>{{ proposal.totalScore }}</b>
            <em :class="gradeClass(proposal.totalScore)">{{ proposal.grade }}</em>
          </button>
        </div>
      </article>

      <aside class="dash-panel dash-detail">
        <div class="dash-panel__head">
          <h3>제안 상세</h3>
          <span>{{ selectedProposal.status }}</span>
        </div>

        <div class="dash-detail__title">
          <strong>{{ selectedProposal.partnerName }}</strong>
          <p>{{ selectedProposal.benefitSummary }}</p>
        </div>

        <dl class="dash-detail__meta">
          <div>
            <dt>담당</dt>
            <dd>{{ selectedProposal.owner }}</dd>
          </div>
          <div>
            <dt>응답</dt>
            <dd>{{ selectedProposal.contact }}</dd>
          </div>
          <div>
            <dt>혜택 조건</dt>
            <dd>{{ selectedProposal.condition }}</dd>
          </div>
        </dl>

        <section class="dash-detail__section">
          <h4>점수 근거</h4>
          <ul>
            <li v-for="item in selectedProposal.scoreBreakdown" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="dash-detail__section">
          <h4>리스크</h4>
          <ul>
            <li v-for="item in selectedProposal.risks" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="dash-detail__section">
          <h4>다음 액션</h4>
          <ul>
            <li v-for="item in selectedProposal.actions" :key="item">{{ item }}</li>
          </ul>
        </section>
      </aside>

    </div>
  </section>
</template>

<style scoped>
.match-dashboard {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 0.7rem;
  height: 100%;
  min-height: 0;
}

.dash-stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.55rem;
}

.dash-stat,
.dash-panel {
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
}

.dash-stat {
  position: relative;
  min-height: 4.65rem;
  overflow: hidden;
  padding: 0.7rem 0.78rem;
}

.dash-stat::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: color-mix(in srgb, var(--accent-color) 62%, var(--border-strong));
}

.dash-stat span,
.dash-stat small,
.dash-stat em,
.dash-panel__head span,
.dash-table__head span,
.dash-table__row span,
.dash-pipeline span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 700;
}

.dash-stat small {
  display: block;
  margin-bottom: 0.18rem;
  color: color-mix(in srgb, var(--accent-color) 62%, var(--muted-text));
  font-size: 0.62rem;
  font-weight: 900;
  text-transform: uppercase;
}

.dash-stat strong {
  display: block;
  margin-top: 0.12rem;
  color: var(--text-primary);
  font-size: 1.3rem;
  line-height: 1;
}

.dash-stat em {
  display: block;
  margin-top: 0.18rem;
  font-style: normal;
}

.dash-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: 0.7rem;
  min-height: 0;
}

.dash-panel {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: 0.8rem;
}

.dash-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.65rem;
}

.dash-panel__head h3 {
  color: var(--text-primary);
  font-size: 0.95rem;
}

.dash-table {
  display: grid;
  gap: 0.4rem;
}

.dash-table__head,
.dash-table__row {
  display: grid;
  grid-template-columns: minmax(130px, 0.8fr) minmax(260px, 1.7fr) minmax(100px, 0.7fr) 50px 96px;
  align-items: center;
  gap: 0.6rem;
}

.dash-table__head {
  padding: 0 0.55rem 0.25rem;
}

.dash-table__row {
  min-height: 3.25rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.55rem;
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    background-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.dash-table__row:hover,
.dash-table__row--active {
  border-color: color-mix(in srgb, var(--accent-color) 42%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-color));
}

.dash-table__row--active {
  box-shadow:
    0 5px 14px rgba(19, 35, 68, 0.06),
    inset 3px 0 0 var(--accent-color);
}

.dash-table__row strong {
  color: var(--text-primary);
  font-size: 0.84rem;
}

.dash-table__row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dash-table__row b {
  color: var(--accent-color);
  font-size: 0.92rem;
}

.dash-table__row em {
  display: inline-flex;
  min-height: 1.45rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 0.48rem;
  font-size: 0.66rem;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.dash-grade--strong {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}

.dash-grade--info {
  background: var(--color-info-light);
  color: var(--color-info-dark);
}

.dash-grade--warning {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.dash-grade--danger {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.dash-pipeline {
  display: grid;
  gap: 0.5rem;
}

.dash-pipeline div {
  display: flex;
  min-height: 2.85rem;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0 0.7rem;
}

.dash-pipeline strong {
  color: var(--text-primary);
  font-size: 1rem;
}

.dash-detail {
  display: grid;
  align-content: start;
  gap: 0.6rem;
}

.dash-detail__title {
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.55rem;
}

.dash-detail__title strong {
  color: var(--text-primary);
  font-size: 0.92rem;
}

.dash-detail__title p,
.dash-detail__meta dd,
.dash-detail__section li {
  color: var(--text-secondary);
  font-size: 0.74rem;
  line-height: 1.42;
}

.dash-detail__meta {
  display: grid;
  gap: 0.45rem;
  margin: 0;
}

.dash-detail__meta div,
.dash-detail__section {
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.55rem;
}

.dash-detail__meta dt,
.dash-detail__section h4 {
  margin-bottom: 0.24rem;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.dash-detail__meta dd {
  margin: 0;
  font-weight: 700;
}

.dash-detail__section ul {
  display: grid;
  gap: 0.28rem;
  margin: 0;
  padding-left: 1rem;
}

@media (max-width: 1180px) {
  .dash-stat-grid,
  .dash-layout {
    grid-template-columns: 1fr;
  }
}
</style>
