<script setup>
defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const summaryStats = [
  { label: '등록 자산', value: '12', helper: '활성 9' },
  { label: '신규 제안', value: '8', helper: '오늘 2' },
  { label: '최우선 추천', value: '3', helper: '90점 이상' },
  { label: '진행 캠페인', value: '5', helper: '운영 중' },
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
  },
  {
    id: 2,
    partnerName: '나이키 코리아',
    benefitSummary: '러닝앱 멤버십 공동 챌린지 및 리미티드 굿즈',
    totalScore: 85,
    grade: '우선 검토',
    date: '04.27',
    owner: '호텔마케팅팀',
  },
  {
    id: 3,
    partnerName: 'CGV',
    benefitSummary: 'VIP 고객 대상 프리미엄 관람권 1+1 혜택',
    totalScore: 76,
    grade: '조건부 검토',
    date: '04.25',
    owner: 'CRM팀',
  },
  {
    id: 4,
    partnerName: 'LG 생활건강',
    benefitSummary: '신규 뷰티 브랜드 런칭 기념 샘플링 키트 제공',
    totalScore: 65,
    grade: '보완 필요',
    date: '04.24',
    owner: '브랜드제휴팀',
  },
]

const pipelines = [
  { label: '제안 접수', value: 8 },
  { label: '자동 평가', value: 6 },
  { label: '조합 추천', value: 3 },
  { label: '운영 전환', value: 2 },
]

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
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <small>{{ stat.helper }}</small>
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
          >
            <strong>{{ proposal.partnerName }}</strong>
            <span>{{ proposal.benefitSummary }}</span>
            <span>{{ proposal.owner }}</span>
            <b>{{ proposal.totalScore }}</b>
            <em :class="gradeClass(proposal.totalScore)">{{ proposal.grade }}</em>
          </button>
        </div>
      </article>

      <aside class="dash-panel">
        <div class="dash-panel__head">
          <h3>진행 흐름</h3>
          <span>오늘 기준</span>
        </div>

        <div class="dash-pipeline">
          <div v-for="item in pipelines" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
  min-height: 4.4rem;
  padding: 0.72rem 0.8rem;
}

.dash-stat span,
.dash-stat small,
.dash-panel__head span,
.dash-table__head span,
.dash-table__row span,
.dash-pipeline span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 700;
}

.dash-stat strong {
  display: block;
  margin-top: 0.12rem;
  color: var(--text-primary);
  font-size: 1.3rem;
  line-height: 1;
}

.dash-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(240px, 0.55fr);
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

@media (max-width: 1180px) {
  .dash-stat-grid,
  .dash-layout {
    grid-template-columns: 1fr;
  }
}
</style>
