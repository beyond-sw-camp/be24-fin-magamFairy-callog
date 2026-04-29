<script setup>
import { computed, ref } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const summaryStats = [
  { label: '등록 자산', value: '12', helper: '활성 9건', stage: 'Asset Pool' },
  { label: '제안 접수', value: '8', helper: '오늘 3건', stage: 'Intake' },
  { label: '자동 평가', value: '6', helper: '검토 대기 3건', stage: 'Scoring' },
  { label: '조합 추천', value: '3', helper: '90점 이상 1건', stage: 'Matching' },
  { label: '운영 전환', value: '2', helper: '보드 생성', stage: 'Handoff' },
]

const stageNotes = {
  'Asset Pool': {
    title: '등록 자산 단계 · 중복 자산 3건 정리',
    description: '동일 키워드 자산이 중복 등록되어 있어요. 병합하면 추천 정확도가 올라갑니다.',
    action: '정리하기',
  },
  Intake: {
    title: '제안 접수 단계 · 담당자 배정 대기 3건',
    description: '오늘 들어온 제안 중 3건이 담당자 배정 대기 중입니다.',
    action: '배정 시작',
  },
  Scoring: {
    title: '자동 평가 단계 · 점수 검토 후 컷오프 적용',
    description: '85점 이상 6건 중 3건이 사람 검토 대기입니다. 평균 처리 18분.',
    action: '검토 열기',
  },
  Matching: {
    title: '조합 추천 단계 · 추천 조합 1건 승인 대기',
    description: '나이키 코리아 × 러닝 멤버십 · 매칭 점수 85 · 담당자 확인 필요',
    action: '승인 요청',
  },
  Handoff: {
    title: '운영 전환 단계 · 운영 보드로 전달',
    description: '승인된 조합은 운영팀 칸반에 카드로 생성됩니다.',
    action: '전달하기',
  },
}

const partnerProposals = [
  {
    id: 1,
    logo: 'SBX',
    partnerName: '스타벅스 코리아',
    benefitSummary: '시즌 음료 사이즈업 쿠폰 1만장 및 앱 배너 노출',
    totalScore: 94,
    grade: '최우선 추천',
    date: '04.28',
    owner: '브랜드제휴팀',
    status: '평가 완료',
    contact: '파트너 제휴팀 / 응답 1일 이내',
    condition: '쿠폰 10,000장, 스타벅스 앱 동시 공지 가능',
    scoreBreakdown: [
      { label: '고객 적합도', value: 96 },
      { label: '혜택 매력도', value: 92 },
      { label: '운영 난이도', value: 88 },
    ],
    risks: ['쿠폰 조기 소진 가능성', 'VIP 고객 중복 노출 빈도 관리 필요'],
    actions: ['추천 조합 생성', '혜택 조건서 검토', '파트너 미팅 일정 등록'],
  },
  {
    id: 2,
    logo: 'NIKE',
    partnerName: '나이키 코리아',
    benefitSummary: '러닝앱 멤버십 공동 챌린지 및 리미티드 굿즈',
    totalScore: 85,
    grade: '우선 검토',
    date: '04.27',
    owner: '호텔마케팅팀',
    status: '조건 협의',
    contact: 'NRC 마케팅 담당 / 일정 확인 중',
    condition: '러닝 챌린지 4주 운영, 리워드 굿즈 2,000개',
    scoreBreakdown: [
      { label: '고객 적합도', value: 90 },
      { label: '수익 기여도', value: 85 },
      { label: '브랜드 적합도', value: 95 },
    ],
    risks: ['오프라인 클래스 일정 촉박', '우천 시 대체 미션 필요'],
    actions: ['운영 가능 일정 확인', '패키지 가격안 작성', '법무 유의사항 검토'],
  },
  {
    id: 3,
    logo: 'CGV',
    partnerName: 'CGV',
    benefitSummary: 'VIP 고객 대상 프리미엄 관람권 1+1 혜택',
    totalScore: 76,
    grade: '조건부 검토',
    date: '04.25',
    owner: 'CRM팀',
    status: '검토 대기',
    contact: 'CGV 제휴 담당 / 응답 2일 이내',
    condition: '프리미엄 상영관 예매권 1+1, 앱 신규 가입 CTA 연동',
    scoreBreakdown: [
      { label: '고객 적합도', value: 78 },
      { label: '전환 기대', value: 84 },
      { label: '운영 난이도', value: 86 },
    ],
    risks: ['예매권 사용 조건 복잡', '가입 전환 추적 코드 필요'],
    actions: ['쿠폰 조건 단순화 요청', '가입 랜딩 문구 작성', 'A/B 테스트 설계'],
  },
  {
    id: 4,
    logo: 'LG',
    partnerName: 'LG 생활건강',
    benefitSummary: '신규 뷰티 브랜드 런칭 기념 샘플링 키트 제공',
    totalScore: 65,
    grade: '보완 필요',
    date: '04.24',
    owner: '브랜드제휴팀',
    status: '보완 요청',
    contact: '브랜드 BM / 샘플 수량 확인 중',
    condition: '샘플링 키트 8,500개, 객실 비치형 전환 검토',
    scoreBreakdown: [
      { label: '비용 효율성', value: 85 },
      { label: '고객 적합도', value: 70 },
      { label: '브랜드 적합도', value: 60 },
    ],
    risks: ['타깃 고객층 불명확', '브랜드 이미지 연결 근거 부족'],
    actions: ['대상 고객군 재정의', '제품 라인업 재요청', '객실 비치 비용 산정'],
  },
]

const selectedProposalId = ref(partnerProposals[1].id)
const activeStage = ref('Matching')
const activeStageIndex = computed(() =>
  summaryStats.findIndex((stat) => stat.stage === activeStage.value),
)
const activeStageNote = computed(() => stageNotes[activeStage.value] ?? stageNotes.Matching)
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
    <div class="dash-flow">
      <div class="dash-pipeline-strip" aria-label="매칭 처리 단계">
        <button
          v-for="(stat, index) in summaryStats"
          :key="stat.label"
          type="button"
          class="dash-pipeline-step"
          :class="{
            'dash-pipeline-step--active': activeStage === stat.stage,
            'dash-pipeline-step--done': index < activeStageIndex,
          }"
          @click="activeStage = stat.stage"
        >
          <span>{{ index + 1 }}</span>
          <strong>{{ stat.label }}</strong>
          <em>{{ stat.helper }}</em>
          <b>{{ stat.value }}</b>
        </button>
      </div>

      <div class="dash-stage-note">
        <span class="dash-stage-note__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8Z" />
          </svg>
        </span>
        <div class="dash-stage-note__copy">
          <strong>{{ activeStageNote.title }}</strong>
          <span>{{ activeStageNote.description }}</span>
        </div>
        <div class="dash-stage-note__actions">
          <button type="button" class="dash-stage-note__secondary">미루기</button>
          <button type="button" class="dash-stage-note__primary">{{ activeStageNote.action }} →</button>
        </div>
      </div>
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
          <button type="button" class="dash-detail__link">조건 협상</button>
        </div>

        <div class="dash-detail__summary">
          <span class="dash-detail__logo">{{ selectedProposal.logo }}</span>
          <div class="dash-detail__title">
            <strong>{{ selectedProposal.partnerName }}</strong>
            <p>{{ selectedProposal.benefitSummary }}</p>
          </div>
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
          <dl class="dash-score-list">
            <div
              v-for="item in selectedProposal.scoreBreakdown"
              :key="item.label"
              class="dash-score-line"
            >
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </section>

        <section class="dash-detail__section">
          <h4>리스크</h4>
          <ul>
            <li v-for="item in selectedProposal.risks" :key="item">{{ item }}</li>
          </ul>
        </section>

        <div class="dash-detail__actions">
          <button type="button" class="dash-detail__reject">반려</button>
          <button type="button" class="dash-detail__approve">승인 요청 →</button>
        </div>
      </aside>

    </div>
  </section>
</template>

<style scoped>
.match-dashboard {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 0.8rem;
  height: 100%;
  min-height: 0;
}

.dash-panel {
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
}

.dash-panel__head span,
.dash-table__head span,
.dash-table__row span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 700;
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

.dash-flow {
  display: grid;
  gap: 0.55rem;
}

.dash-pipeline-strip {
  display: flex;
  min-width: 0;
  border-radius: 6px;
  overflow: hidden;
  background: var(--panel-color);
}

.dash-pipeline-step {
  position: relative;
  display: flex;
  flex: 1;
  min-width: 0;
  height: 56px;
  align-items: center;
  gap: 0.56rem;
  border: 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0 1.1rem 0 1.75rem;
  text-align: left;
  cursor: pointer;
  transition:
    background-color var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.dash-pipeline-step + .dash-pipeline-step {
  padding-left: 2rem;
}

.dash-pipeline-step:first-child {
  border-left: 1px solid var(--border-color);
  border-radius: 6px 0 0 6px;
  padding-left: 1rem;
}

.dash-pipeline-step:last-child {
  border-right: 1px solid var(--border-color);
  border-radius: 0 6px 6px 0;
  padding-right: 1rem;
}

.dash-pipeline-step::before,
.dash-pipeline-step::after {
  content: '';
  position: absolute;
  width: 0;
  height: 0;
  top: 0;
  right: -13px;
  z-index: 1;
  border-top: 28px solid transparent;
  border-bottom: 28px solid transparent;
  border-left: 13px solid var(--border-color);
}

.dash-pipeline-step::after {
  right: -12px;
  z-index: 2;
  border-left-color: var(--panel-color);
}

.dash-pipeline-step:last-child::before,
.dash-pipeline-step:last-child::after {
  display: none;
}

.dash-pipeline-step:hover {
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-color));
}

.dash-pipeline-step:hover::after {
  border-left-color: color-mix(in srgb, var(--accent-color) 5%, var(--panel-color));
}

.dash-pipeline-step--done {
  border-color: color-mix(in srgb, var(--accent-color) 16%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: color-mix(in srgb, var(--accent-color) 76%, var(--text-primary));
}

.dash-pipeline-step--done::before {
  border-left-color: color-mix(in srgb, var(--accent-color) 16%, var(--border-color));
}

.dash-pipeline-step--done::after {
  border-left-color: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
}

.dash-pipeline-step--active {
  z-index: 2;
  border-color: #14152b;
  background: #14152b;
  color: #ffffff;
}

.dash-pipeline-step--active::before,
.dash-pipeline-step--active::after {
  border-left-color: #14152b;
}

.dash-pipeline-step span {
  display: inline-flex;
  width: 1.5rem;
  height: 1.5rem;
  flex: 0 0 1.5rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 18%, var(--panel-color));
  color: var(--accent-color);
  font-size: 0.7rem;
  font-weight: 900;
}

.dash-pipeline-step strong {
  overflow: hidden;
  flex: 1;
  color: var(--text-primary);
  font-size: 0.8rem;
  font-weight: 900;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dash-pipeline-step b {
  color: var(--text-primary);
  flex: 0 0 auto;
  font-size: 1.08rem;
  font-variant-numeric: tabular-nums;
  font-weight: 900;
  line-height: 1;
}

.dash-pipeline-step em {
  overflow: hidden;
  flex: 0 1 auto;
  color: var(--muted-text);
  font-size: 0.65rem;
  font-style: normal;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dash-pipeline-step--done span {
  background: color-mix(in srgb, var(--accent-color) 18%, #ffffff);
  color: var(--accent-color);
}

.dash-pipeline-step--done strong,
.dash-pipeline-step--done b {
  color: color-mix(in srgb, var(--accent-color) 74%, var(--text-primary));
}

.dash-pipeline-step--active span {
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}

.dash-pipeline-step--active strong,
.dash-pipeline-step--active b {
  color: #ffffff;
}

.dash-pipeline-step--active em {
  color: rgba(255, 255, 255, 0.72);
}

.dash-stage-note {
  display: grid;
  grid-template-columns: 2rem minmax(0, 1fr) auto;
  min-height: 2.9rem;
  align-items: center;
  gap: 0.65rem;
  border: 1px solid color-mix(in srgb, var(--accent-color) 18%, var(--border-color));
  border-radius: 8px;
  background: color-mix(in srgb, var(--accent-color) 6%, var(--panel-color));
  padding: 0.52rem 0.72rem;
}

.dash-stage-note__icon {
  display: grid;
  width: 1.75rem;
  height: 1.75rem;
  place-items: center;
  border-radius: 5px;
  background: var(--accent-color);
  color: #ffffff;
}

.dash-stage-note__icon svg {
  width: 0.95rem;
  height: 0.95rem;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.dash-stage-note__copy {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
}

.dash-stage-note__copy strong {
  color: var(--text-primary);
  font-size: 0.78rem;
}

.dash-stage-note__copy span {
  overflow: hidden;
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dash-stage-note__actions {
  display: flex;
  gap: 0.35rem;
  justify-content: flex-end;
}

.dash-stage-note__actions button {
  min-height: 1.8rem;
  border-radius: 6px;
  font-size: 0.7rem;
  font-weight: 900;
  padding: 0 0.68rem;
  white-space: nowrap;
}

.dash-stage-note__secondary {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.dash-stage-note__primary {
  border: 0;
  background: #14152b;
  color: #ffffff;
}

.dash-detail {
  display: grid;
  align-content: start;
  gap: 0.72rem;
  padding-bottom: 0;
}

.dash-detail__link {
  border: 0;
  background: transparent;
  color: var(--accent-color);
  font-size: 0.7rem;
  font-weight: 900;
  padding: 0;
}

.dash-detail__summary {
  display: grid;
  grid-template-columns: 2.25rem minmax(0, 1fr);
  align-items: center;
  gap: 0.62rem;
}

.dash-detail__logo {
  display: inline-flex;
  width: 2.25rem;
  height: 2.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #ff671f;
  color: #ffffff;
  font-size: 0.66rem;
  font-weight: 900;
}

.dash-detail__title {
  min-width: 0;
}

.dash-detail__title strong {
  color: var(--text-primary);
  font-size: 0.9rem;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.55rem 0.7rem;
  margin: 0;
}

.dash-detail__meta div:nth-child(3) {
  grid-column: 1 / -1;
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
  gap: 0.32rem;
  margin: 0;
  padding-left: 0;
  list-style: none;
}

.dash-score-list {
  display: grid;
  gap: 0.34rem;
  margin: 0;
}

.dash-score-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
  min-height: 1.05rem;
}

.dash-score-line dt {
  position: relative;
  min-width: 0;
  padding-left: 0.8rem;
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 800;
  line-height: 1.2;
}

.dash-score-line dt::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 0.34rem;
  height: 0.34rem;
  border-radius: 999px;
  background: var(--accent-color);
  transform: translateY(-50%);
}

.dash-score-line dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.74rem;
  font-variant-numeric: tabular-nums;
  font-weight: 900;
  line-height: 1.2;
}

.dash-detail__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.42rem;
  border-top: 1px solid var(--border-color);
  margin: 0 -0.8rem;
  padding: 0.62rem 0.8rem 0.8rem;
}

.dash-detail__actions button {
  min-height: 2.2rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 900;
}

.dash-detail__reject {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.dash-detail__approve {
  border: 0;
  background: var(--accent-color);
  color: #ffffff;
}

@media (max-width: 1180px) {
  .dash-layout {
    grid-template-columns: 1fr;
  }

  .dash-pipeline-strip {
    display: grid;
    grid-template-columns: 1fr;
  }

  .dash-pipeline-step {
    width: 100%;
    border-right: 1px solid var(--border-color);
    border-bottom: 1px solid var(--border-color);
    padding-left: 1rem;
  }

  .dash-pipeline-step + .dash-pipeline-step {
    padding-left: 1rem;
  }

  .dash-pipeline-step:first-child,
  .dash-pipeline-step:last-child {
    border-radius: 0;
  }

  .dash-pipeline-step::before,
  .dash-pipeline-step::after {
    display: none;
  }

  .dash-stage-note {
    align-items: stretch;
    grid-template-columns: 1fr;
  }

  .dash-stage-note__actions {
    justify-content: stretch;
  }

  .dash-stage-note__actions button {
    flex: 1;
  }
}
</style>
