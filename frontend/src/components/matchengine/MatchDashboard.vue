<script setup>
import { computed } from 'vue'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
  actions: {
    type: Array,
    default: () => [
      {
        id: 'pending_review',
        label: '검토 대기',
        count: 3,
        description: '파트너 혜택 제안 확인 필요',
        recentTitle: '최근 들어온 제안:',
        recent: ['호텔앤드 (5/6)', '갤러리아 (5/6)', 'CGV (5/5)'],
        tone: 'primary',
      },
      {
        id: 'ready_to_run',
        label: '운영 시작 대기',
        count: 2,
        description: '진행 결정된 조합 운영 시작 필요',
        recentTitle: '최근 진행 결정:',
        recent: ['스타벅스 코리아 (5/6)', '나이키 코리아 (5/6)'],
        tone: 'success',
      },
    ],
  },
})

const emit = defineEmits(['action'])

const funnelSteps = [
  { id: 'received', label: '접수', count: 8, rate: 100 },
  { id: 'review', label: '검토', count: 6, rate: 75 },
  { id: 'decision', label: '결정', count: 3, rate: 50 },
  { id: 'running', label: '운영', count: 2, rate: 67 },
]

const funnelWarnings = computed(() => funnelSteps.filter((step) => step.rate < 50))
const visibleActions = computed(() => props.actions.filter((action) => Number(action.count || 0) > 0))
const totalActions = computed(() =>
  visibleActions.value.reduce((sum, action) => sum + Number(action.count || 0), 0),
)

function openBenefitTab() {
  emit('action', {
    id: 'open_benefits',
    target: { tab: 'benefits', filter: 'all' },
  })
}
</script>

<template>
  <section class="home-dashboard" :class="{ 'home-dashboard--dark': isDark }">
    <article class="today-card">
      <header class="today-card__head">
        <div>
          <span>TODAY</span>
          <strong>오늘 처리</strong>
        </div>
        <b>{{ totalActions }}건</b>
      </header>

      <div class="today-card__list">
        <article
          v-for="action in visibleActions"
          :key="action.id"
          class="today-card__item"
          :class="`today-card__item--${action.tone}`"
        >
          <header class="today-card__item-head">
            <strong>{{ action.label }} {{ action.count }}건</strong>
            <span>{{ action.description }}</span>
          </header>

          <div class="today-card__recent">
            <span>{{ action.recentTitle }}</span>
            <ul>
              <li v-for="item in action.recent" :key="item">{{ item }}</li>
            </ul>
          </div>
        </article>
      </div>

      <footer class="today-card__foot">
        <button type="button" class="today-card__action" @click="openBenefitTab">
          혜택 제안 확인 →
        </button>
      </footer>
    </article>

    <article class="funnel-card">
      <header class="funnel-card__head">
        <div>
          <h3>이번 주 퍼널</h3>
          <span>접수 → 검토 → 결정 → 운영</span>
        </div>
        <b v-if="funnelWarnings.length">경고 {{ funnelWarnings.length }}건</b>
      </header>

      <div class="funnel-flow" aria-label="이번 주 매칭 퍼널">
        <div
          v-for="step in funnelSteps"
          :key="step.id"
          class="funnel-step"
          :class="{ 'funnel-step--warning': step.rate < 50 }"
        >
          <span>{{ step.label }}</span>
          <strong>{{ step.count }}건</strong>
          <em>{{ step.rate }}%</em>
        </div>
      </div>

      <p v-if="funnelWarnings.length" class="funnel-alert">
        전환율 50% 미만 단계가 있어 확인이 필요합니다.
      </p>
      <p v-else class="funnel-alert funnel-alert--ok">
        전환율 50% 미만 단계가 없습니다.
      </p>
    </article>
  </section>
</template>

<style scoped>
.home-dashboard {
  display: grid;
  align-content: start;
  gap: 0.85rem;
  min-height: 100%;
  padding: 0.2rem;
}

.today-card,
.funnel-card {
  width: min(46rem, 100%);
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 1rem 1.1rem;
}

.today-card__head,
.funnel-card__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.85rem;
}

.today-card__head div,
.funnel-card__head div {
  display: grid;
  gap: 0.2rem;
}

.today-card__head span,
.funnel-card__head span {
  color: var(--muted-text);
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.today-card__head strong,
.today-card__head b,
.funnel-card__head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 900;
}

.today-card__list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
  gap: 0.7rem;
}

.today-card__item {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  min-height: 11rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.9rem 1rem;
}

.today-card__item--success {
  border-color: color-mix(in srgb, var(--color-success-dark, #047857) 22%, var(--border-color));
}

.today-card__item-head {
  display: grid;
  gap: 0.25rem;
}

.today-card__item-head strong {
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 900;
}

.today-card__item-head span,
.today-card__recent span {
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 800;
}

.today-card__recent {
  display: grid;
  gap: 0.35rem;
}

.today-card__recent ul {
  display: grid;
  gap: 0.25rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.today-card__recent li {
  color: var(--text-primary);
  font-size: 0.8rem;
  font-weight: 800;
}

.today-card__recent li::before {
  content: '•';
  margin-right: 0.35rem;
  color: var(--accent-color);
  font-weight: 900;
}

.today-card__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.85rem;
}

.today-card__action {
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  padding: 0.55rem 0.9rem;
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.today-card__action:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.funnel-card__head b {
  border-radius: 999px;
  background: var(--color-warning-light, #fef3c7);
  color: var(--color-warning-dark, #b45309);
  padding: 0.18rem 0.55rem;
  font-size: 0.7rem;
  font-weight: 900;
}

.funnel-flow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.55rem;
}

.funnel-step {
  position: relative;
  display: grid;
  gap: 0.28rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.75rem 0.8rem;
}

.funnel-step:not(:last-child)::after {
  content: '→';
  position: absolute;
  top: 50%;
  right: -0.48rem;
  transform: translateY(-50%);
  color: var(--muted-text);
  font-size: 0.75rem;
  font-weight: 900;
  z-index: 1;
}

.funnel-step span {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 900;
}

.funnel-step strong {
  color: var(--text-primary);
  font-size: 1.05rem;
  font-weight: 900;
}

.funnel-step em {
  color: var(--accent-color);
  font-size: 0.75rem;
  font-style: normal;
  font-weight: 900;
}

.funnel-step--warning {
  border-color: color-mix(in srgb, var(--color-warning-dark, #b45309) 35%, var(--border-color));
  background: color-mix(in srgb, var(--color-warning-light, #fef3c7) 45%, var(--panel-color));
}

.funnel-alert {
  margin: 0.7rem 0 0;
  color: var(--color-warning-dark, #b45309);
  font-size: 0.74rem;
  font-weight: 800;
}

.funnel-alert--ok {
  color: var(--muted-text);
}

@media (max-width: 680px) {
  .funnel-flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .funnel-step::after {
    display: none;
  }
}

@media (max-width: 560px) {
  .today-card__list {
    grid-template-columns: 1fr;
  }

  .today-card__foot {
    justify-content: stretch;
  }

  .today-card__action {
    width: 100%;
  }
}
</style>
