<script setup>
import { computed, ref } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const selectedGoal = ref('vip')
const selectedComboId = ref(1)
const detailMode = ref('summary')
const operationHandoff = ref(null)

const goals = [
  { id: 'vip', name: 'VIP 혜택 강화', count: 2 },
  { id: 'new_cust', name: '신규 고객 유입', count: 1 },
  { id: 'room_res', name: '객실 예약 증가', count: 1 },
  { id: 'app_join', name: '앱 가입 증가', count: 1 },
  { id: 'brand_exp', name: '브랜드 노출', count: 2 },
]

const combinations = [
  {
    id: 1,
    goal: 'vip',
    title: '갤러리아 VIP 프리미엄 리프레시',
    grade: '최우선 추천',
    partner: '스타벅스 코리아',
    asset: '갤러리아 VIP 고객층, 앱 배너',
    offer: '리저브 사이즈업 쿠폰, 전용 굿즈',
    target: 'VIP App 활성 고객 약 5만 명',
    channels: '갤러리아 앱, 알림톡, 스타벅스 앱',
    outputs: '랜딩 페이지, 알림톡 문구, 쿠폰 난수',
    schedule: '기획 2주 / 운영 3주',
    risk: '쿠폰 소진 속도 제한 필요',
    score: 94,
  },
  {
    id: 2,
    goal: 'room_res',
    title: '호텔앤드리조트 액티브 스테이',
    grade: '우선 검토',
    partner: '나이키 코리아',
    asset: '객실 패키지, 리조트 이용권',
    offer: 'NRC 챌린지, 리워드 굿즈',
    target: '2030 액티브 레저 고객',
    channels: '리조트 공홈, 나이키 NRC 앱',
    outputs: '패키지 페이지, SNS 소재 3종',
    schedule: '기획 3주 / 운영 1개월',
    risk: '클래스 일정 확정 필요',
    score: 88,
  },
  {
    id: 3,
    goal: 'app_join',
    title: '앱 신규 가입 시네마 베네핏',
    grade: '우선 검토',
    partner: 'CGV',
    asset: '앱 가입 온보딩, 신규 쿠폰함',
    offer: '프리미엄 상영관 1+1 예매권',
    target: '앱 미가입 기존 구매 고객',
    channels: '앱 배너, 푸시, 문자',
    outputs: '가입 배너, 쿠폰 유의사항',
    schedule: '기획 1주 / 운영 2주',
    risk: '예매권 조건 문구 검수',
    score: 82,
  },
]

const visibleCombinations = computed(() => {
  const filtered = combinations.filter((combo) => combo.goal === selectedGoal.value)
  return filtered.length ? filtered : combinations
})

const selectedCombo = computed(
  () => combinations.find((combo) => combo.id === selectedComboId.value) ?? visibleCombinations.value[0],
)

function selectGoal(goalId) {
  selectedGoal.value = goalId
  selectedComboId.value = visibleCombinations.value[0]?.id ?? combinations[0].id
}

function scoreTone(score) {
  if (score >= 90) return 'match-tone--strong'
  if (score >= 80) return 'match-tone--info'
  return 'match-tone--warning'
}

function moveToOperationBoard() {
  if (!selectedCombo.value) return
  operationHandoff.value = {
    ...selectedCombo.value,
    source: `추천 조합 #M-${String(selectedCombo.value.id).padStart(3, '0')}`,
  }
  detailMode.value = 'operation'
}

function selectDetailMode(mode) {
  if (mode === 'operation' && !operationHandoff.value) return
  detailMode.value = mode
}

const operationTasks = [
  { title: '랜딩/배너 제작', owner: '이디자인', due: '05.05', status: '실행 준비' },
  { title: '푸시 문구 생성', owner: '김콘텐츠', due: '05.02', status: '진행 중' },
  { title: '유의사항 검수', owner: '최법무', due: '05.08', status: '검수 대기' },
]
</script>

<template>
  <section class="matching-workspace">
    <aside class="match-panel match-goals">
      <div class="match-panel__head">
        <h3>목표</h3>
        <span>{{ goals.length }}</span>
      </div>

      <button
        v-for="goal in goals"
        :key="goal.id"
        type="button"
        class="match-goal"
        :class="{ 'match-goal--active': selectedGoal === goal.id }"
        @click="selectGoal(goal.id)"
      >
        <strong>{{ goal.name }}</strong>
        <span>{{ goal.count }}건</span>
      </button>
    </aside>

    <div class="match-panel match-list">
      <div class="match-panel__head">
        <h3>추천 조합</h3>
        <span>{{ visibleCombinations.length }}건</span>
      </div>

      <button
        v-for="combo in visibleCombinations"
        :key="combo.id"
        type="button"
        class="match-row"
        :class="{ 'match-row--active': selectedCombo?.id === combo.id }"
        @click="selectedComboId = combo.id"
      >
        <span class="match-score" :class="scoreTone(combo.score)">{{ combo.score }}</span>
        <span class="match-row__main">
          <strong>{{ combo.title }}</strong>
          <small>{{ combo.partner }} · {{ combo.target }}</small>
          <em>{{ combo.asset }} · {{ combo.offer }}</em>
        </span>
        <span class="match-badge" :class="scoreTone(combo.score)">{{ combo.grade }}</span>
      </button>
    </div>

    <aside v-if="selectedCombo" class="match-panel match-detail">
      <div class="match-panel__head">
        <h3>{{ detailMode === 'summary' ? '추천 상세' : '운영 전환' }}</h3>
        <span class="match-badge" :class="scoreTone(selectedCombo.score)">
          {{ selectedCombo.score }}점
        </span>
      </div>

      <div class="match-detail-switch" role="tablist" aria-label="추천 조합 상세 단계">
        <button
          type="button"
          :class="{ active: detailMode === 'summary' }"
          @click="selectDetailMode('summary')"
        >
          추천 상세
        </button>
        <button
          type="button"
          :class="{ active: detailMode === 'operation', disabled: !operationHandoff }"
          :disabled="!operationHandoff"
          @click="selectDetailMode('operation')"
        >
          운영 전환
        </button>
      </div>

      <template v-if="detailMode === 'summary'">
        <div class="match-detail__title">
          <strong>{{ selectedCombo.title }}</strong>
          <p>{{ selectedCombo.partner }}</p>
        </div>

        <dl class="match-detail__grid">
          <div>
            <dt>한화 자산</dt>
            <dd>{{ selectedCombo.asset }}</dd>
          </div>
          <div>
            <dt>파트너 혜택</dt>
            <dd>{{ selectedCombo.offer }}</dd>
          </div>
          <div>
            <dt>채널</dt>
            <dd>{{ selectedCombo.channels }}</dd>
          </div>
          <div>
            <dt>산출물</dt>
            <dd>{{ selectedCombo.outputs }}</dd>
          </div>
          <div>
            <dt>일정</dt>
            <dd>{{ selectedCombo.schedule }}</dd>
          </div>
          <div>
            <dt>리스크</dt>
            <dd>{{ selectedCombo.risk }}</dd>
          </div>
        </dl>

        <button type="button" class="match-primary" @click="moveToOperationBoard">
          운영 보드로 전환
        </button>
      </template>

      <template v-else>
        <div class="operation-inline">
          <div class="operation-inline__head">
            <strong>{{ operationHandoff.title }}</strong>
            <span>{{ operationHandoff.source }}</span>
          </div>

          <div class="operation-inline__meta">
            <div>
              <span>목표</span>
              <strong>{{ operationHandoff.target }}</strong>
            </div>
            <div>
              <span>예상 일정</span>
              <strong>{{ operationHandoff.schedule }}</strong>
            </div>
          </div>

          <div class="operation-task-list">
            <article v-for="task in operationTasks" :key="task.title">
              <span>{{ task.status }}</span>
              <strong>{{ task.title }}</strong>
              <p>{{ task.owner }} · {{ task.due }}</p>
            </article>
          </div>
        </div>
      </template>
    </aside>
  </section>
</template>

<style scoped>
.matching-workspace {
  display: grid;
  grid-template-columns: minmax(220px, 0.48fr) minmax(320px, 0.62fr) minmax(520px, 1.55fr);
  gap: 0.7rem;
  height: 100%;
  min-height: 0;
}

.match-panel {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  padding: 0.8rem;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
}

.match-goals,
.match-list {
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-muted) 86%, var(--accent-soft)),
      var(--panel-muted)
    );
  border-color: color-mix(in srgb, var(--border-strong) 72%, var(--accent-color));
  box-shadow: inset -1px 0 0 color-mix(in srgb, var(--border-color) 72%, transparent);
}

.match-detail {
  background: var(--panel-color);
  border-color: var(--border-strong);
}

.match-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}

.match-panel__head h3 {
  color: var(--text-primary);
  font-size: 0.95rem;
  line-height: 1.1;
}

.match-panel__head span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 800;
}

.match-goals,
.match-list,
.match-detail {
  display: grid;
  align-content: start;
  gap: 0.5rem;
}

.match-goal,
.match-row {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: color-mix(in srgb, var(--panel-color) 66%, var(--panel-muted));
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
}

.match-goal {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  min-height: 2.65rem;
  padding: 0.58rem 0.65rem;
}

.match-goal strong,
.match-row__main strong,
.match-detail__title strong {
  color: var(--text-primary);
  font-size: 0.9rem;
}

.match-goal span,
.match-row__main small,
.match-row__main em,
.match-detail__title p {
  color: var(--muted-text);
  font-size: 0.74rem;
}

.match-goal--active,
.match-row--active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 11%, var(--panel-color));
  box-shadow:
    0 5px 14px rgba(19, 35, 68, 0.06),
    inset 3px 0 0 var(--accent-color);
}

.match-row {
  display: grid;
  grid-template-columns: 2.7rem minmax(0, 1fr);
  align-items: center;
  gap: 0.65rem;
  min-height: 5.2rem;
  padding: 0.65rem;
}

.match-row__main {
  display: grid;
  min-width: 0;
  gap: 0.12rem;
}

.match-row__main strong,
.match-row__main small,
.match-row__main em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-row__main em {
  font-size: 0.68rem;
  font-style: normal;
  font-weight: 700;
}

.match-score {
  display: grid;
  width: 2.45rem;
  height: 2.45rem;
  place-items: center;
  border-radius: 7px;
  font-size: 0.9rem;
  font-weight: 900;
}

.match-badge {
  display: inline-flex;
  grid-column: 2;
  justify-self: start;
  min-height: 1.55rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 0.55rem;
  font-size: 0.68rem;
  font-weight: 900;
  white-space: nowrap;
}

.match-tone--strong {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}

.match-tone--info {
  background: var(--color-info-light);
  color: var(--color-info-dark);
}

.match-tone--warning {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.match-detail__title {
  display: grid;
  gap: 0.15rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.65rem;
}

.match-detail-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.3rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.28rem;
}

.match-detail-switch button {
  min-height: 2.15rem;
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
}

.match-detail-switch button.active {
  background: var(--panel-color);
  color: var(--accent-color);
  box-shadow:
    0 4px 12px rgba(19, 35, 68, 0.06),
    inset 0 -2px 0 var(--accent-color);
}

.match-detail-switch button.disabled {
  cursor: not-allowed;
  color: var(--subtle-text);
  opacity: 0.62;
}

.match-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin: 0;
}

.match-detail__grid div {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.58rem 0.65rem;
}

.match-detail__grid dt {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 800;
}

.match-detail__grid dd {
  margin: 0.16rem 0 0;
  color: var(--text-secondary);
  font-size: 0.78rem;
  line-height: 1.35;
}

.match-primary {
  min-height: 2.35rem;
  border-radius: 7px;
  background: var(--accent-color);
  color: #fff;
  font-size: 0.82rem;
  font-weight: 800;
}

.operation-inline {
  display: grid;
  gap: 0.6rem;
}

.operation-inline__head,
.operation-inline__meta div,
.operation-task-list article {
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.62rem;
}

.operation-inline__head {
  display: grid;
  gap: 0.16rem;
}

.operation-inline__head strong,
.operation-inline__meta strong,
.operation-task-list strong {
  color: var(--text-primary);
  font-size: 0.84rem;
}

.operation-inline__head span,
.operation-inline__meta span,
.operation-task-list span,
.operation-task-list p {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 800;
}

.operation-inline__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
}

.operation-task-list {
  display: grid;
  gap: 0.45rem;
}

.operation-task-list article {
  display: grid;
  gap: 0.18rem;
}

.operation-task-list span {
  color: var(--accent-color);
}

.operation-task-list p {
  font-weight: 600;
}

@media (max-width: 1180px) {
  .matching-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
