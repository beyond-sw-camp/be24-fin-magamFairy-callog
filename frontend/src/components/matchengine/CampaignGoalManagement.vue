<script setup>
import { computed, onMounted, ref } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['goal-count-change'])

const goalTypes = [
  {
    id: 'new_customer',
    label: '신규 고객 유입',
    kpis: ['신규 가입 수', 'CAC 한도'],
    rule: '도달 규모와 신규 전환 가능성 가중',
  },
  {
    id: 'vip_benefit',
    label: 'VIP 혜택 강화',
    kpis: ['VIP 재방문율', '객단가 상승률'],
    rule: '고객 등급과 브랜드 적합도 가중',
  },
  {
    id: 'room_booking',
    label: '객실 예약 증가',
    kpis: ['추가 예약 건수', 'ADR 유지선'],
    rule: '재고 소진과 예약 전환 가능성 가중',
  },
  {
    id: 'app_join',
    label: '앱 가입 증가',
    kpis: ['신규 다운로드', 'D7 잔존율'],
    rule: '채널 전환과 앱 행동 데이터 가중',
  },
  {
    id: 'member_revisit',
    label: '멤버십 재방문',
    kpis: ['재방문율', '쿠폰 사용률'],
    rule: '기존 고객 활성도와 혜택 매력도 가중',
  },
  {
    id: 'brand_exposure',
    label: '브랜드 노출',
    kpis: ['노출 수', '콘텐츠 공유율'],
    rule: '채널 노출 가치와 콘텐츠 확산성 가중',
  },
  {
    id: 'revenue',
    label: '매출 증대',
    kpis: ['추가 매출', '손익분기점'],
    rule: '예상 매출과 파트너 분담률 가중',
  },
]

const goals = ref([
  {
    id: 1,
    name: '2026 Q2 VIP 리프레시',
    primaryType: 'vip_benefit',
    secondaryType: 'member_revisit',
    kpiPrimary: 'VIP 재방문율 +10%',
    kpiSecondary: '객단가 +5%',
    budgetLimit: '5,000만 원',
    effortLimit: '100시간',
    periodStart: '2026-05-01',
    periodEnd: '2026-06-30',
    owner: '갤러리아 마케팅팀 김OO',
    weights: { revenue: 30, effort: 20, brand: 50 },
  },
  {
    id: 2,
    name: '앱 신규 가입 전환 캠페인',
    primaryType: 'app_join',
    secondaryType: 'new_customer',
    kpiPrimary: '신규 다운로드 30,000건',
    kpiSecondary: 'D7 잔존율 18%',
    budgetLimit: '3,000만 원',
    effortLimit: '80시간',
    periodStart: '2026-06-01',
    periodEnd: '2026-07-15',
    owner: '디지털채널팀 박OO',
    weights: { revenue: 35, effort: 35, brand: 30 },
  },
])

const form = ref(createGoalForm())

function createGoalForm() {
  return {
    name: '',
    primaryType: 'vip_benefit',
    secondaryType: '',
    kpiPrimary: '',
    kpiSecondary: '',
    budgetLimit: '',
    effortLimit: '',
    periodStart: '',
    periodEnd: '',
    owner: '',
    weights: { revenue: 40, effort: 30, brand: 30 },
  }
}

const selectedPrimaryType = computed(
  () => goalTypes.find((type) => type.id === form.value.primaryType) ?? goalTypes[0],
)

const weightTotal = computed(
  () => Number(form.value.weights.revenue) + Number(form.value.weights.effort) + Number(form.value.weights.brand),
)

const canSubmit = computed(
  () =>
    form.value.name.trim() &&
    form.value.primaryType &&
    form.value.kpiPrimary.trim() &&
    form.value.budgetLimit.trim() &&
    form.value.effortLimit.trim() &&
    form.value.periodStart &&
    form.value.periodEnd &&
    weightTotal.value === 100,
)

function typeLabel(id) {
  return goalTypes.find((type) => type.id === id)?.label ?? '-'
}

function addGoal() {
  if (!canSubmit.value) return
  goals.value.unshift({
    ...form.value,
    id: Date.now(),
    weights: { ...form.value.weights },
  })
  form.value = createGoalForm()
  emit('goal-count-change', goals.value.length)
}

onMounted(() => {
  emit('goal-count-change', goals.value.length)
})
</script>

<template>
  <section class="goal-workspace">
    <aside class="goal-list">
      <div class="goal-list__head">
        <div>
          <h3>캠페인 목표</h3>
          <p>추천 조합의 기준이 되는 목표 라이브러리입니다.</p>
        </div>
        <b>{{ goals.length }}건</b>
      </div>

      <article v-for="goal in goals" :key="goal.id" class="goal-card">
        <div class="goal-card__head">
          <div>
            <strong>{{ goal.name }}</strong>
            <span>{{ typeLabel(goal.primaryType) }}<template v-if="goal.secondaryType"> + {{ typeLabel(goal.secondaryType) }}</template></span>
          </div>
          <em>{{ goal.weights.revenue }}/{{ goal.weights.effort }}/{{ goal.weights.brand }}</em>
        </div>
        <dl>
          <div>
            <dt>KPI</dt>
            <dd>{{ goal.kpiPrimary }}<template v-if="goal.kpiSecondary"> · {{ goal.kpiSecondary }}</template></dd>
          </div>
          <div>
            <dt>한도</dt>
            <dd>{{ goal.budgetLimit }} · {{ goal.effortLimit }}</dd>
          </div>
          <div>
            <dt>기간</dt>
            <dd>{{ goal.periodStart }} ~ {{ goal.periodEnd }}</dd>
          </div>
          <div>
            <dt>등록자</dt>
            <dd>{{ goal.owner }}</dd>
          </div>
        </dl>
      </article>
    </aside>

    <article class="goal-form">
      <header class="goal-form__head">
        <div>
          <h3>목표 등록</h3>
          <p>목표 유형, KPI, 한도, 기간, 우선순위를 구조화해서 입력합니다.</p>
        </div>
        <span>수익성 / 공수 / 브랜드</span>
      </header>

      <div class="goal-form__grid">
        <label class="goal-field goal-field--full">
          <span>목표명 <em>*</em></span>
          <input v-model="form.name" placeholder="예: 2026 Q2 VIP 리프레시" />
        </label>

        <label class="goal-field">
          <span>주 목표 유형 <em>*</em></span>
          <select v-model="form.primaryType">
            <option v-for="type in goalTypes" :key="type.id" :value="type.id">{{ type.label }}</option>
          </select>
          <small>{{ selectedPrimaryType.rule }}</small>
        </label>

        <label class="goal-field">
          <span>보조 목표</span>
          <select v-model="form.secondaryType">
            <option value="">선택 안 함</option>
            <option v-for="type in goalTypes.filter((type) => type.id !== form.primaryType)" :key="type.id" :value="type.id">
              {{ type.label }}
            </option>
          </select>
          <small>복합 목표는 최대 2개까지 관리합니다.</small>
        </label>

        <label class="goal-field">
          <span>{{ selectedPrimaryType.kpis[0] }} <em>*</em></span>
          <input v-model="form.kpiPrimary" :placeholder="`예: ${selectedPrimaryType.kpis[0]} +10%`" />
        </label>

        <label class="goal-field">
          <span>{{ selectedPrimaryType.kpis[1] }}</span>
          <input v-model="form.kpiSecondary" :placeholder="`예: ${selectedPrimaryType.kpis[1]} 5,000만 원 이내`" />
        </label>

        <label class="goal-field">
          <span>한화 부담 예산 상한 <em>*</em></span>
          <input v-model="form.budgetLimit" placeholder="예: 5,000만 원" />
        </label>

        <label class="goal-field">
          <span>운영 공수 상한 <em>*</em></span>
          <input v-model="form.effortLimit" placeholder="예: 100시간" />
        </label>

        <label class="goal-field">
          <span>시작일 <em>*</em></span>
          <input v-model="form.periodStart" type="date" />
        </label>

        <label class="goal-field">
          <span>종료일 <em>*</em></span>
          <input v-model="form.periodEnd" type="date" />
        </label>

        <label class="goal-field goal-field--full">
          <span>등록자 / 등록 부서</span>
          <input v-model="form.owner" placeholder="예: 갤러리아 마케팅팀 김OO" />
        </label>
      </div>

      <section class="goal-weights">
        <div class="goal-weights__head">
          <strong>우선순위 가중치</strong>
          <span :class="{ invalid: weightTotal !== 100 }">합계 {{ weightTotal }}%</span>
        </div>
        <label>
          <span>수익성 {{ form.weights.revenue }}%</span>
          <input v-model.number="form.weights.revenue" type="range" min="0" max="100" step="5" />
        </label>
        <label>
          <span>공수 효율 {{ form.weights.effort }}%</span>
          <input v-model.number="form.weights.effort" type="range" min="0" max="100" step="5" />
        </label>
        <label>
          <span>브랜드 적합도 {{ form.weights.brand }}%</span>
          <input v-model.number="form.weights.brand" type="range" min="0" max="100" step="5" />
        </label>
      </section>

      <footer class="goal-form__actions">
        <p>목표가 만들어지면 추천 조합 탭에서 해당 목표 기준으로 캠페인 후보를 비교합니다.</p>
        <button type="button" :disabled="!canSubmit" @click="addGoal">목표 등록</button>
      </footer>
    </article>
  </section>
</template>

<style scoped>
.goal-workspace {
  display: grid;
  grid-template-columns: 24rem minmax(0, 1fr);
  gap: 0.75rem;
  height: 100%;
  min-height: 0;
}

.goal-list,
.goal-form {
  min-height: 0;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.9rem;
}

.goal-list {
  display: grid;
  align-content: start;
  gap: 0.55rem;
  overflow-y: auto;
}

.goal-list__head,
.goal-form__head,
.goal-card__head,
.goal-form__actions,
.goal-weights__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.goal-list__head h3,
.goal-form__head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 900;
}

.goal-list__head p,
.goal-form__head p,
.goal-form__actions p {
  margin: 0.18rem 0 0;
  color: var(--muted-text);
  font-size: 0.74rem;
  font-weight: 700;
}

.goal-list__head b,
.goal-form__head span,
.goal-card__head em,
.goal-weights__head span {
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: var(--accent-color);
  padding: 0.28rem 0.62rem;
  font-size: 0.7rem;
  font-style: normal;
  font-weight: 900;
}

.goal-card {
  display: grid;
  gap: 0.65rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.75rem;
}

.goal-card__head strong {
  display: block;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 900;
}

.goal-card__head span {
  display: block;
  margin-top: 0.18rem;
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 800;
}

.goal-card dl {
  display: grid;
  gap: 0.42rem;
  margin: 0;
}

.goal-card dl > div {
  display: grid;
  grid-template-columns: 3.4rem minmax(0, 1fr);
  gap: 0.45rem;
}

.goal-card dt,
.goal-card dd {
  margin: 0;
  font-size: 0.72rem;
}

.goal-card dt {
  color: var(--muted-text);
  font-weight: 900;
}

.goal-card dd {
  overflow: hidden;
  color: var(--text-secondary);
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goal-form {
  display: grid;
  grid-template-rows: auto minmax(0, auto) auto auto;
  gap: 0.85rem;
  overflow-y: auto;
}

.goal-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem;
}

.goal-field {
  display: grid;
  gap: 0.32rem;
  min-width: 0;
}

.goal-field--full {
  grid-column: 1 / -1;
}

.goal-field span {
  color: var(--text-primary);
  font-size: 0.76rem;
  font-weight: 900;
}

.goal-field em {
  color: var(--accent-color);
  font-style: normal;
}

.goal-field input,
.goal-field select {
  height: 2.55rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--text-primary);
  padding: 0 0.72rem;
  font-size: 0.82rem;
  font-weight: 750;
}

.goal-field input:focus,
.goal-field select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 16%, transparent);
}

.goal-field small {
  min-height: 0.85rem;
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 700;
}

.goal-weights {
  display: grid;
  gap: 0.55rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.75rem;
}

.goal-weights__head strong {
  color: var(--text-primary);
  font-size: 0.84rem;
  font-weight: 900;
}

.goal-weights__head span.invalid {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.goal-weights label {
  display: grid;
  grid-template-columns: 8rem minmax(0, 1fr);
  align-items: center;
  gap: 0.75rem;
}

.goal-weights label span {
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 900;
}

.goal-weights input {
  accent-color: var(--accent-color);
}

.goal-form__actions {
  border-top: 1px solid var(--border-color);
  padding-top: 0.75rem;
}

.goal-form__actions button {
  min-height: 2.4rem;
  border: 1px solid var(--accent-color);
  border-radius: 7px;
  background: var(--accent-color);
  color: #fff;
  padding: 0 1rem;
  font-size: 0.82rem;
  font-weight: 900;
  cursor: pointer;
}

.goal-form__actions button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

@media (max-width: 1180px) {
  .goal-workspace {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 760px) {
  .goal-form__grid,
  .goal-weights label {
    grid-template-columns: minmax(0, 1fr);
  }

  .goal-field--full {
    grid-column: 1;
  }
}
</style>
