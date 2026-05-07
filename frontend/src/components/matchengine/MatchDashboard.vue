<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
  stats: {
    type: Object,
    default: () => ({
      proposals: 8,
      pendingReview: 6,
      running: 2,
      averageScore: 82.4,
      matchingCount: 128,
      processHours: 3.7,
    }),
  },
  days: {
    type: Array,
    default: () => [
      { date: '2026-04-23', score: 78.2, matches: 8, hours: 4.2 },
      { date: '2026-04-24', score: 80.1, matches: 9, hours: 4.0 },
      { date: '2026-04-25', score: 79.5, matches: 7, hours: 3.9 },
      { date: '2026-04-26', score: 81.3, matches: 10, hours: 3.8 },
      { date: '2026-04-27', score: 80.7, matches: 9, hours: 4.1 },
      { date: '2026-04-28', score: 82.0, matches: 11, hours: 3.7 },
      { date: '2026-04-29', score: 81.5, matches: 10, hours: 3.6 },
      { date: '2026-04-30', score: 83.2, matches: 12, hours: 3.5 },
      { date: '2026-05-01', score: 82.8, matches: 11, hours: 3.6 },
      { date: '2026-05-02', score: 84.0, matches: 13, hours: 3.4 },
      { date: '2026-05-03', score: 83.5, matches: 12, hours: 3.5 },
      { date: '2026-05-04', score: 82.9, matches: 11, hours: 3.7 },
      { date: '2026-05-05', score: 81.8, matches: 10, hours: 3.8 },
      { date: '2026-05-06', score: 82.4, matches: 13, hours: 3.7 },
    ],
  },
  actions: {
    type: Array,
    default: () => [
      {
        id: 'pending_review',
        label: '검토 대기',
        count: 3,
        description: '파트너 혜택 제안 확인 필요',
        actionLabel: '검토 시작',
        target: { tab: 'benefits', filter: 'new' },
        tone: 'primary',
      },
      {
        id: 'needs_supplement',
        label: '보완 요청 대기',
        count: 1,
        description: '비용 부담/유효 기간 미입력',
        actionLabel: '보완 요청',
        target: { tab: 'benefits', filter: 'incomplete' },
        tone: 'warning',
      },
      {
        id: 'ready_to_run',
        label: '운영 시작 대기',
        count: 2,
        description: '진행 결정 완료, 운영 시작 가능',
        actionLabel: '운영 시작',
        target: { tab: 'evaluation', filter: 'proceed' },
        tone: 'success',
      },
    ],
  },
})

const emit = defineEmits(['action', 'matching-complete'])

const matchingSteps = ['입력 조건 분석', '목표 조건 매핑', '파트너 풀 검색', '점수 산정', '추천 조합 생성']

const visibleActions = computed(() => props.actions.filter((action) => action.count > 0))
const hasActions = computed(() => visibleActions.value.length > 0)
const totalActions = computed(() =>
  visibleActions.value.reduce((sum, action) => sum + Number(action.count || 0), 0),
)
const averageScore = computed(() => props.stats.averageScore ?? 82.4)
const matchingCount = computed(() => props.stats.matchingCount ?? 128)
const processHours = computed(() => props.stats.processHours ?? 3.7)
const selectedIndex = ref(Math.max(props.days.length - 1, 0))
const selectedDay = computed(
  () =>
    props.days[selectedIndex.value] ??
    props.days[props.days.length - 1] ?? {
      date: '',
      score: averageScore.value,
      matches: matchingCount.value,
      hours: processHours.value,
    },
)
const maxScore = computed(() => Math.max(...props.days.map((day) => day.score), averageScore.value) * 1.1)
const minScore = computed(() => Math.min(...props.days.map((day) => day.score), averageScore.value) * 0.85)
const avgScore = computed(() => {
  if (!props.days.length) return Number(averageScore.value)
  const sum = props.days.reduce((acc, day) => acc + Number(day.score || 0), 0)
  return Number((sum / props.days.length).toFixed(1))
})
const avgMatches = computed(() => {
  if (!props.days.length) return Number(matchingCount.value)
  const sum = props.days.reduce((acc, day) => acc + Number(day.matches || 0), 0)
  return Math.round(sum / props.days.length)
})
const avgHours = computed(() => {
  if (!props.days.length) return Number(processHours.value)
  const sum = props.days.reduce((acc, day) => acc + Number(day.hours || 0), 0)
  return Number((sum / props.days.length).toFixed(1))
})
const scoreDiff = computed(() => Number((selectedDay.value.score - avgScore.value).toFixed(1)))
const matchesDiff = computed(() => Number(selectedDay.value.matches || 0) - avgMatches.value)
const hoursDiff = computed(() => Number((selectedDay.value.hours - avgHours.value).toFixed(1)))
const kpis = computed(() => [
  {
    id: 'proposals',
    label: '받은 제안',
    value: props.stats.proposals,
    unit: '건',
    sub: '오늘 접수',
    delta: '+3',
    tone: 'neutral',
    icon: 'box',
    spark: 'M0,22 L15,18 L30,20 L45,12 L60,15 L75,8 L100,4',
  },
  {
    id: 'pending',
    label: '검토 대기',
    value: props.stats.pendingReview,
    unit: '건',
    sub: '혜택 제안 확인 필요 · 평균 SLA 4h',
    tone: 'brand',
    icon: 'clock',
    spark: 'M0,10 L20,14 L40,8 L60,18 L80,12 L100,15',
  },
  {
    id: 'running',
    label: '운영 중',
    value: props.stats.running,
    unit: '건',
    sub: '진행 결정 완료',
    tone: 'green',
    icon: 'check',
    spark: 'M0,20 L20,18 L40,16 L60,10 L80,8 L100,5',
  },
  {
    id: 'score',
    label: '평균 매칭 점수',
    value: averageScore.value,
    unit: '',
    sub: '지난주 대비',
    delta: '+4.1',
    tone: 'neutral',
    icon: 'score',
    spark: 'M0,20 L15,18 L30,14 L45,16 L60,10 L75,12 L100,6',
  },
])

const isMatching = ref(false)
const activeStepIndex = ref(0)
let matchingTimer = null

function clearMatchingTimer() {
  if (matchingTimer) {
    window.clearInterval(matchingTimer)
    matchingTimer = null
  }
}

function startMatching() {
  clearMatchingTimer()
  isMatching.value = true
  activeStepIndex.value = 0

  matchingTimer = window.setInterval(() => {
    if (activeStepIndex.value < matchingSteps.length - 1) {
      activeStepIndex.value += 1
      return
    }

    clearMatchingTimer()
    isMatching.value = false
    emit('matching-complete', { tab: 'evaluation', filter: 'new' })
  }, 850)
}

function cancelMatching() {
  clearMatchingTimer()
  isMatching.value = false
  activeStepIndex.value = 0
}

function handleAction(action) {
  emit('action', action)
}

function barHeight(score) {
  const range = maxScore.value - minScore.value
  if (!range) return 50
  return ((score - minScore.value) / range) * 100
}

function selectBar(index) {
  selectedIndex.value = index
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}월 ${date.getDate()}일`
}

function formatWeekday(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return ['일', '월', '화', '수', '목', '금', '토'][date.getDay()]
}

onBeforeUnmount(clearMatchingTimer)
</script>

<template>
  <section class="matching-overview" :class="{ 'matching-overview--dark': isDark }">
    <div class="matching-overview__main">
      <section class="matching-hero">
        <div class="matching-hero__copy">
          <span class="matching-hero__eyebrow">
            <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
              <path
                d="M12 3 4 7l8 4 8-4-8-4ZM4 12l8 4 8-4M4 17l8 4 8-4"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            매칭 워크스페이스
          </span>
          <h3>저장된 조건으로<br />추천 후보를 생성합니다.</h3>

          <div class="matching-hero__actions">
            <button type="button" class="mf-btn mf-btn--primary mf-btn--lg" @click="startMatching">
              매칭 실행
              <svg viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
                <path
                  d="M5 12h14m-6-6 6 6-6 6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </button>
          </div>

          <div class="matching-hero__helper">
            이번 주 평균 매칭 시간 <b>{{ processHours }}시간</b> · 평균 점수 <b>{{ averageScore }}</b>
          </div>
        </div>

        <aside class="score-panel">
          <header class="score-panel__head">
            <h4>최근 14일 매칭 점수 추이</h4>
            <span>실시간</span>
          </header>
          <div class="score-bars" role="group" aria-label="일별 매칭 점수">
            <button
              v-for="(day, index) in days"
              :key="day.date"
              type="button"
              class="score-bar"
              :class="{ 'score-bar--active': selectedIndex === index }"
              :aria-label="`${formatDate(day.date)} 점수 ${day.score}`"
              :aria-pressed="selectedIndex === index"
              @click="selectBar(index)"
            >
              <span class="score-bar__fill" :style="{ height: `${barHeight(day.score)}%` }">
                <span v-if="selectedIndex === index" class="score-bar__tooltip">
                  <strong>{{ day.score.toFixed(1) }}</strong>
                  <small>{{ formatDate(day.date) }} ({{ formatWeekday(day.date) }})</small>
                </span>
              </span>
              <span v-if="selectedIndex === index" class="score-bar__indicator" />
            </button>
          </div>
          <div class="score-panel__stats">
            <article>
              <span>평균 점수</span>
              <strong>{{ selectedDay.score.toFixed(1) }}</strong>
              <em :class="scoreDiff >= 0 ? 'up' : 'down'">
                {{ scoreDiff >= 0 ? '▲' : '▼' }} {{ Math.abs(scoreDiff) }}
              </em>
            </article>
            <article>
              <span>매칭 수</span>
              <strong>{{ selectedDay.matches }}</strong>
              <em :class="matchesDiff >= 0 ? 'up' : 'down'">
                {{ matchesDiff >= 0 ? '▲' : '▼' }} {{ Math.abs(matchesDiff) }}
              </em>
            </article>
            <article>
              <span>처리 시간</span>
              <strong>{{ selectedDay.hours }}h</strong>
              <em :class="hoursDiff <= 0 ? 'up' : 'down'">
                {{ hoursDiff <= 0 ? '▼' : '▲' }} {{ Math.abs(hoursDiff) }}h
              </em>
            </article>
          </div>
        </aside>
      </section>

      <aside class="today-panel">
        <header class="today-panel__head">
          <div>
            <span>Today</span>
            <h3>오늘 처리</h3>
          </div>
          <strong v-if="hasActions">{{ totalActions }}건</strong>
        </header>

        <div v-if="hasActions" class="today-list">
          <button
            v-for="action in visibleActions"
            :key="action.id"
            type="button"
            class="today-item"
            :class="`today-item--${action.tone}`"
            @click="handleAction(action)"
          >
            <span class="today-item__count">{{ action.count }}</span>
            <span class="today-item__copy">
              <strong>{{ action.label }}</strong>
              <small>{{ action.description }}</small>
            </span>
            <span class="today-item__go">
              {{ action.actionLabel }}
              <svg viewBox="0 0 24 24" width="12" height="12" aria-hidden="true">
                <path
                  d="m9 18 6-6-6-6"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </span>
          </button>
        </div>

        <div v-else class="today-empty">
          <svg viewBox="0 0 24 24" width="34" height="34" aria-hidden="true">
            <path
              d="M5 13l4 4L19 7"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <p>지금 처리할 일이 없습니다.</p>
        </div>
      </aside>
    </div>

    <section class="kpi-grid" aria-label="매칭 현황 요약">
      <article v-for="kpi in kpis" :key="kpi.id" class="kpi-card" :class="`kpi-card--${kpi.tone}`">
        <div class="kpi-card__label">
          <svg v-if="kpi.icon === 'box'" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
            <path
              d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linejoin="round"
            />
          </svg>
          <svg v-else-if="kpi.icon === 'clock'" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
            <circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="2" />
            <path
              d="M12 6v6l4 2"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <svg v-else-if="kpi.icon === 'check'" viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
            <path
              d="M20 6 9 17l-5-5"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <svg v-else viewBox="0 0 24 24" width="14" height="14" aria-hidden="true">
            <path
              d="M22 11.08V12a10 10 0 1 1-5.93-9.14M22 4 12 14.01l-3-3"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          {{ kpi.label }}
        </div>
        <strong class="kpi-card__value">
          {{ kpi.value }}<small v-if="kpi.unit">{{ kpi.unit }}</small>
        </strong>
        <p class="kpi-card__sub">
          <span v-if="kpi.delta" class="kpi-card__delta">▲ {{ kpi.delta.replace('+', '') }}</span>
          {{ kpi.sub }}
        </p>
        <svg class="kpi-card__spark" viewBox="0 0 100 30" fill="none" aria-hidden="true">
          <path :d="kpi.spark" />
        </svg>
      </article>
    </section>

    <Teleport to="body">
      <Transition name="matching-modal">
        <section v-if="isMatching" class="matching-overlay" role="dialog" aria-modal="true">
          <div class="matching-modal">
            <header class="matching-modal__head">
              <span>Matching</span>
              <h3>매칭 진행 중</h3>
              <p>추천 후보를 만들고 있습니다. 완료되면 파트너 평가 화면으로 이동합니다.</p>
            </header>

            <div class="matching-progress" aria-hidden="true">
              <span
                v-for="(_, index) in matchingSteps"
                :key="index"
                :class="{
                  done: index < activeStepIndex,
                  active: index === activeStepIndex,
                }"
              />
            </div>

            <ol class="matching-steps">
              <li
                v-for="(step, index) in matchingSteps"
                :key="step"
                :class="{
                  done: index < activeStepIndex,
                  active: index === activeStepIndex,
                }"
              >
                <span>
                  <svg
                    v-if="index < activeStepIndex"
                    viewBox="0 0 24 24"
                    width="13"
                    height="13"
                    aria-hidden="true"
                  >
                    <path
                      d="M5 13l4 4L19 7"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.4"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                  <i v-else-if="index === activeStepIndex" />
                  <b v-else />
                </span>
                <strong>{{ step }}</strong>
                <em v-if="index === activeStepIndex">진행 중</em>
                <em v-else-if="index < activeStepIndex">완료</em>
                <em v-else>대기</em>
              </li>
            </ol>

            <footer class="matching-modal__foot">
              <span>예상 소요: 약 5초</span>
              <button type="button" @click="cancelMatching">취소</button>
            </footer>
          </div>
        </section>
      </Transition>
    </Teleport>

  </section>
</template>

<style scoped>
.matching-overview {
  --mf-bg: #f7f7f9;
  --mf-surface: #ffffff;
  --mf-surface-2: #fafafb;
  --mf-line: #ecedf0;
  --mf-line-strong: #dee0e5;
  --mf-text: #0f1115;
  --mf-text-2: #4a4f5a;
  --mf-text-3: #8a8f99;
  --mf-text-4: #b6bac2;
  --mf-brand: #5b5bf5;
  --mf-brand-strong: #4848e0;
  --mf-brand-soft: #eef0ff;
  --mf-green: #16a368;
  --mf-green-soft: #e5f6ee;
  --mf-amber: #c97a0e;
  --mf-amber-soft: #fbefd7;
  --mf-rose: #d0395f;
  --mf-shadow-1: 0 1px 0 rgba(15, 17, 21, 0.04), 0 1px 2px rgba(15, 17, 21, 0.04);
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 16px;
  padding: 0;
  color: var(--mf-text);
}

.matching-overview__main {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 1fr);
  gap: 16px;
}

.matching-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(310px, 0.92fr);
  gap: 28px;
  align-items: stretch;
  border: 1px solid var(--mf-line);
  border-radius: 16px;
  background:
    radial-gradient(120% 90% at 100% 0%, #eceeff 0%, transparent 55%),
    radial-gradient(90% 90% at 0% 100%, #f2ebff 0%, transparent 55%),
    var(--mf-surface);
  padding: 28px;
}

.matching-hero__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
}

.matching-hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1b1e59;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.matching-hero__eyebrow svg {
  color: var(--mf-brand);
}

.matching-hero h3 {
  margin: 14px 0 8px;
  color: var(--mf-text);
  font-size: 30px;
  font-weight: 800;
  line-height: 1.25;
}

.matching-hero h3 em {
  color: var(--mf-brand);
  font-style: normal;
}

.matching-hero p {
  max-width: 480px;
  margin: 0;
  color: var(--mf-text-2);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.65;
}

.matching-hero__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 22px;
}

.matching-hero__helper {
  margin-top: 14px;
  color: var(--mf-text-3);
  font-size: 12px;
  font-weight: 600;
}

.matching-hero__helper b {
  color: var(--mf-text-2);
  font-weight: 800;
}

.mf-btn {
  display: inline-flex;
  height: 36px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--mf-line);
  border-radius: 8px;
  background: var(--mf-surface);
  color: var(--mf-text);
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}

.mf-btn:hover {
  background: var(--mf-surface-2);
}

.mf-btn--primary {
  border-color: var(--mf-brand);
  background: var(--mf-brand);
  color: #ffffff;
  box-shadow:
    0 1px 0 rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.mf-btn--primary:hover {
  border-color: var(--mf-brand-strong);
  background: var(--mf-brand-strong);
}

.mf-btn--lg {
  height: 42px;
  border-radius: 10px;
  padding: 0 18px;
  font-size: 14px;
}

.score-panel {
  display: flex;
  min-height: 235px;
  flex-direction: column;
  gap: 14px;
  border: 1px solid var(--mf-line);
  border-radius: 14px;
  background: var(--mf-surface);
  box-shadow: var(--mf-shadow-1);
  padding: 18px;
}

.score-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.score-panel__head h4 {
  margin: 0;
  color: var(--mf-text);
  font-size: 13px;
  font-weight: 800;
}

.score-panel__head span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--mf-green);
  font-size: 11px;
  font-weight: 800;
}

.score-panel__head span::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: var(--mf-green);
  box-shadow: 0 0 0 3px rgba(22, 163, 104, 0.15);
}

.score-bars {
  display: grid;
  grid-template-columns: repeat(14, minmax(0, 1fr));
  height: 80px;
  align-items: flex-end;
  gap: 5px;
  padding: 18px 2px 4px;
}

.score-bar {
  position: relative;
  display: flex;
  height: 100%;
  align-items: flex-end;
  justify-content: center;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.score-bar:hover {
  transform: translateY(-2px);
}

.score-bar__fill {
  position: relative;
  width: 100%;
  min-height: 8px;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, #eef0ff, #c9ccff);
  transition:
    background 0.2s,
    height 0.3s ease;
}

.score-bar:hover .score-bar__fill {
  background: linear-gradient(180deg, #dddfff, #b8bcff);
}

.score-bar--active .score-bar__fill {
  background: linear-gradient(180deg, var(--mf-brand), #8e8eff);
}

.score-bar__indicator {
  position: absolute;
  bottom: -6px;
  left: 50%;
  width: 8px;
  height: 8px;
  border: 2px solid var(--mf-brand);
  border-radius: 999px;
  background: #ffffff;
  transform: translateX(-50%);
}

.score-bar__tooltip {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  border-radius: 7px;
  background: var(--mf-text);
  color: #ffffff;
  padding: 6px 8px;
  pointer-events: none;
  transform: translateX(-50%);
  white-space: nowrap;
}

.score-bar__tooltip::after {
  position: absolute;
  top: 100%;
  left: 50%;
  border: 4px solid transparent;
  border-top-color: var(--mf-text);
  content: '';
  transform: translateX(-50%);
}

.score-bar__tooltip strong {
  font-size: 12px;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.score-bar__tooltip small {
  font-size: 10px;
  font-weight: 700;
  opacity: 0.86;
}

.score-panel__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.score-panel__stats article {
  border: 1px solid var(--mf-line);
  border-radius: 10px;
  padding: 10px 12px;
}

.score-panel__stats span {
  color: var(--mf-text-3);
  font-size: 11px;
  font-weight: 700;
}

.score-panel__stats strong {
  display: block;
  margin-top: 2px;
  color: var(--mf-text);
  font-size: 18px;
  font-weight: 850;
  line-height: 1.1;
}

.score-panel__stats em {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
}

.score-panel__stats .up {
  color: var(--mf-green);
}

.score-panel__stats .down {
  color: var(--mf-rose);
}

.today-panel {
  border: 1px solid var(--mf-line);
  border-radius: 16px;
  background: var(--mf-surface);
  padding: 20px;
}

.today-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.today-panel__head span {
  color: var(--mf-text-3);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.today-panel__head h3 {
  margin: 2px 0 0;
  color: var(--mf-text);
  font-size: 16px;
  font-weight: 850;
}

.today-panel__head strong {
  border-radius: 999px;
  background: var(--mf-brand-soft);
  color: var(--mf-brand);
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 800;
}

.today-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.today-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  border: 1px solid var(--mf-line);
  border-radius: 12px;
  background: var(--mf-surface);
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

.today-item:hover {
  border-color: var(--mf-brand);
  box-shadow: 0 0 0 3px var(--mf-brand-soft);
}

.today-item__count {
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--mf-brand-soft);
  color: var(--mf-brand);
  font-size: 14px;
  font-weight: 850;
  font-variant-numeric: tabular-nums;
}

.today-item--warning .today-item__count {
  background: var(--mf-amber-soft);
  color: var(--mf-amber);
}

.today-item--success .today-item__count {
  background: var(--mf-green-soft);
  color: var(--mf-green);
}

.today-item__copy {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 2px;
}

.today-item__copy strong {
  overflow: hidden;
  color: var(--mf-text);
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.today-item__copy small {
  overflow: hidden;
  color: var(--mf-text-3);
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.today-item__go {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--mf-line);
  border-radius: 8px;
  background: var(--mf-surface-2);
  color: var(--mf-text-2);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.today-item:hover .today-item__go {
  border-color: var(--mf-brand);
  background: var(--mf-brand);
  color: #ffffff;
}

.today-empty {
  display: grid;
  min-height: 180px;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--mf-text-3);
  text-align: center;
}

.today-empty svg {
  color: var(--mf-green);
}

.today-empty p {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.kpi-card {
  position: relative;
  min-height: 110px;
  border: 1px solid var(--mf-line);
  border-radius: 16px;
  background: var(--mf-surface);
  padding: 18px 20px;
  overflow: hidden;
}

.kpi-card__label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--mf-text-3);
  font-size: 12px;
  font-weight: 700;
}

.kpi-card__label svg {
  color: var(--mf-text-4);
}

.kpi-card__value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 8px;
  color: var(--mf-text);
  font-size: 28px;
  font-weight: 850;
  line-height: 1;
}

.kpi-card__value small {
  color: var(--mf-text-3);
  font-size: 14px;
  font-weight: 700;
}

.kpi-card--brand .kpi-card__value {
  color: var(--mf-brand);
}

.kpi-card--green .kpi-card__value {
  color: var(--mf-green);
}

.kpi-card__sub {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 0 0;
  color: var(--mf-text-3);
  font-size: 12px;
  font-weight: 650;
}

.kpi-card__delta {
  color: var(--mf-green);
  font-weight: 850;
}

.kpi-card__spark {
  position: absolute;
  right: 14px;
  bottom: 14px;
  width: 80px;
  height: 30px;
  opacity: 0.86;
}

.kpi-card__spark path {
  stroke: var(--mf-brand);
  stroke-width: 1.5;
}

.kpi-card--green .kpi-card__spark path {
  stroke: var(--mf-green);
}

.matching-overlay {
  --mf-bg: #f7f7f9;
  --mf-surface: #ffffff;
  --mf-surface-2: #fafafb;
  --mf-line: #ecedf0;
  --mf-text: #0f1115;
  --mf-text-2: #4a4f5a;
  --mf-text-3: #8a8f99;
  --mf-brand: #5b5bf5;
  --mf-brand-soft: #eef0ff;
  --mf-green: #16a368;
  --mf-green-soft: #e5f6ee;
  position: fixed;
  inset: 0;
  z-index: 220;
  display: grid;
  place-items: center;
  background: rgba(15, 17, 21, 0.56);
  padding: 1rem;
}

.matching-modal {
  width: min(460px, 100%);
  overflow: hidden;
  border: 1px solid var(--mf-line);
  border-radius: 16px;
  background: var(--mf-surface);
  box-shadow: 0 24px 70px rgba(15, 17, 21, 0.3);
}

.matching-modal__head {
  display: grid;
  gap: 0.34rem;
  background:
    radial-gradient(120% 100% at 100% 0%, #eceeff 0%, transparent 55%),
    #ffffff;
  border-bottom: 1px solid var(--mf-line);
  padding: 1.2rem;
}

.matching-modal__head span {
  color: var(--mf-brand);
  font-size: 0.68rem;
  font-weight: 950;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.matching-modal__head h3 {
  margin: 0;
  color: var(--mf-text);
  font-size: 1.18rem;
  font-weight: 950;
}

.matching-modal__head p {
  margin: 0;
  color: var(--mf-text-2);
  font-size: 0.76rem;
  font-weight: 700;
  line-height: 1.45;
}

.matching-progress {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.38rem;
  padding: 1rem 1.2rem 0;
}

.matching-progress span {
  height: 0.38rem;
  border-radius: 999px;
  background: var(--mf-brand-soft);
}

.matching-progress span.done,
.matching-progress span.active {
  background: var(--mf-brand);
}

.matching-steps {
  display: grid;
  gap: 0.48rem;
  margin: 0;
  padding: 0.95rem 1.2rem 0;
  list-style: none;
}

.matching-steps li {
  display: grid;
  grid-template-columns: 1.45rem minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.55rem;
  border: 1px solid var(--mf-line);
  border-radius: 9px;
  background: var(--mf-surface);
  padding: 0.58rem 0.68rem;
}

.matching-steps li.active {
  border-color: var(--mf-brand);
  background: var(--mf-brand-soft);
}

.matching-steps span {
  display: inline-flex;
  width: 1.25rem;
  height: 1.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--mf-surface-2);
  color: var(--mf-brand);
}

.matching-steps i {
  width: 0.48rem;
  height: 0.48rem;
  border-radius: 999px;
  background: var(--mf-brand);
  animation: matching-pulse 0.9s ease-in-out infinite;
}

.matching-steps b {
  width: 0.42rem;
  height: 0.42rem;
  border-radius: 999px;
  background: var(--mf-text-4);
}

.matching-steps strong {
  color: var(--mf-text);
  font-size: 0.8rem;
  font-weight: 900;
}

.matching-steps em {
  color: var(--mf-text-3);
  font-size: 0.68rem;
  font-style: normal;
  font-weight: 800;
}

.matching-steps li.active em {
  color: var(--mf-brand);
}

.matching-modal__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  border-top: 1px solid var(--mf-line);
  margin-top: 1rem;
  padding: 0.85rem 1.2rem 1.05rem;
}

.matching-modal__foot span {
  color: var(--mf-text-3);
  font-size: 0.74rem;
  font-weight: 800;
}

.matching-modal__foot button {
  height: 2rem;
  border: 1px solid var(--mf-line);
  border-radius: 7px;
  background: var(--mf-surface);
  color: var(--mf-text-2);
  padding: 0 0.8rem;
  font-size: 0.74rem;
  font-weight: 900;
  cursor: pointer;
}

.matching-modal-enter-active,
.matching-modal-leave-active {
  transition: opacity 0.18s ease;
}

.matching-modal-enter-active .matching-modal,
.matching-modal-leave-active .matching-modal {
  transition:
    transform 0.18s ease,
    opacity 0.18s ease;
}

.matching-modal-enter-from,
.matching-modal-leave-to {
  opacity: 0;
}

.matching-modal-enter-from .matching-modal,
.matching-modal-leave-to .matching-modal {
  transform: scale(0.96);
  opacity: 0;
}

@keyframes matching-pulse {
  0%,
  100% {
    opacity: 0.45;
    transform: scale(0.84);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 1180px) {
  .matching-overview__main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 920px) {
  .matching-hero,
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .matching-hero,
  .today-panel,
  .kpi-card {
    padding: 16px;
  }

  .matching-hero h3 {
    font-size: 24px;
  }

  .matching-hero__actions,
  .matching-modal__foot {
    align-items: stretch;
    flex-direction: column;
  }

  .mf-btn,
  .matching-modal__foot button {
    width: 100%;
  }

  .score-panel__stats {
    grid-template-columns: 1fr;
  }

  .today-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .today-item__copy small,
  .today-item__copy strong {
    white-space: normal;
  }

  .today-item__go {
    width: 100%;
    justify-content: center;
  }
}
</style>
