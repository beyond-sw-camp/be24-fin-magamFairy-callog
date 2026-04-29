<script setup>
import { computed } from 'vue'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
  handoff: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['requestMatching'])

const hasHandoff = computed(() => Boolean(props.handoff))
const campaign = computed(() => ({
  title: props.handoff?.title ?? '',
  partner: props.handoff?.partner ?? '',
  score: props.handoff?.score ?? 0,
  period: props.handoff?.schedule ?? '운영 일정 미정',
  source: props.handoff?.source ?? '',
  objective: props.handoff?.target ?? '선택된 매칭 조합 없음',
  handoffStatus: '실행 업무 생성 완료',
}))

const columns = [
  { id: 'todo', title: '실행 준비', helper: '자동 생성된 업무를 담당자에게 배정' },
  { id: 'progress', title: '진행 중', helper: '콘텐츠/운영 작업 수행' },
  { id: 'review', title: '검수', helper: '파트너 공유 전 내부 확인' },
  { id: 'done', title: '완료', helper: '성과 기록 및 회고 대기' },
]

const tasks = [
  { id: 1, title: '푸시/알림톡 문구 생성', column: 'progress', tag: '카피', owner: '김콘텐츠', due: '05.02', output: '초안 3종' },
  { id: 2, title: '운영 일정 잠금', column: 'progress', tag: '운영', owner: '박운영', due: '05.01', output: 'D-day 일정표' },
  { id: 3, title: '랜딩/배너 콘텐츠 제작', column: 'todo', tag: '디자인', owner: '이디자인', due: '05.05', output: '랜딩 1식, 배너 2종' },
  { id: 4, title: '유의사항 검수 요청', column: 'todo', tag: '법무', owner: '최법무', due: '05.08', output: '고지 문안' },
  { id: 5, title: '파트너 공유 패키지 전달', column: 'review', tag: '공유', owner: '박운영', due: '05.10', output: '공유 링크' },
  { id: 6, title: '최종 승인 관리', column: 'todo', tag: '관리', owner: '정리드', due: '05.12', output: '승인 로그' },
  { id: 7, title: '성과 기록 템플릿 생성', column: 'done', tag: '데이터', owner: '박운영', due: '06.10', output: '리포트 시트' },
]

const handoffItems = [
  { label: '추천 근거 저장', done: true },
  { label: '필수 산출물 생성', done: true },
  { label: '담당자 배정', done: true },
  { label: '파트너 승인 대기', done: false },
]

const progressRate = computed(() => Math.round((tasks.filter((task) => task.column === 'done').length / tasks.length) * 100))

function tasksByColumn(columnId) {
  return tasks.filter((task) => task.column === columnId)
}
</script>

<template>
  <section v-if="!hasHandoff" class="operation-empty">
    <div class="operation-empty__panel">
      <span class="operation-badge">운영 전환 대기</span>
      <h3>아직 운영 보드로 전환된 매칭 조합이 없습니다.</h3>
      <p>
        이 탭은 추천 조합을 확정한 뒤 실행 업무, 담당자, 산출물, 검수 상태를 추적하는 공간입니다.
      </p>
      <button type="button" @click="emit('requestMatching')">추천 조합에서 선택하기</button>
    </div>
  </section>

  <section v-else class="operation-workspace">
    <aside class="operation-summary">
      <div>
        <span class="operation-badge">매칭 실행 전환</span>
        <h3>{{ campaign.title }}</h3>
        <p>{{ campaign.partner }} · {{ campaign.period }}</p>
      </div>

      <div class="operation-purpose">
        <span>이 탭의 역할</span>
        <p>추천된 제휴 조합을 실제 캠페인 실행 업무로 변환하고, 산출물·담당자·검수 상태를 추적합니다.</p>
      </div>

      <dl class="operation-meta">
        <div>
          <dt>전환 소스</dt>
          <dd>{{ campaign.source }}</dd>
        </div>
        <div>
          <dt>목표</dt>
          <dd>{{ campaign.objective }}</dd>
        </div>
        <div>
          <dt>상태</dt>
          <dd>{{ campaign.handoffStatus }}</dd>
        </div>
      </dl>

      <div class="operation-summary-grid">
        <div class="operation-score">
          <span>추천 점수</span>
          <strong>{{ campaign.score }}</strong>
        </div>

        <div class="operation-progress">
          <span>진행률 {{ progressRate }}%</span>
          <div><i :style="{ width: `${progressRate}%` }" /></div>
        </div>
      </div>

      <div class="operation-checklist">
        <span>전환 체크</span>
        <ul>
          <li v-for="item in handoffItems" :key="item.label" :class="{ done: item.done }">
            {{ item.label }}
          </li>
        </ul>
      </div>
    </aside>

    <div class="operation-main">
      <div class="operation-main__head">
        <div>
          <h3>실행 업무 보드</h3>
          <p>매칭 결과에서 자동 생성된 업무를 실행 단계별로 관리합니다.</p>
        </div>
        <span>{{ tasks.length }}개 업무</span>
      </div>

      <div class="operation-board">
        <section v-for="column in columns" :key="column.id" class="operation-column">
          <div class="operation-column__head">
            <div>
              <h3>{{ column.title }}</h3>
              <p>{{ column.helper }}</p>
            </div>
            <span>{{ tasksByColumn(column.id).length }}</span>
          </div>

          <article v-for="task in tasksByColumn(column.id)" :key="task.id" class="operation-task">
            <div>
              <span>{{ task.tag }}</span>
              <small>{{ task.due }}</small>
            </div>
            <strong>{{ task.title }}</strong>
            <p>{{ task.owner }} · {{ task.output }}</p>
          </article>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
.operation-workspace {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 0.7rem;
  height: 100%;
  min-height: 0;
}

.operation-empty {
  display: grid;
  height: 100%;
  min-height: 0;
  place-items: center;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-color) 92%, var(--panel-muted)),
      var(--panel-color)
    );
  padding: 1rem;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
}

.operation-empty__panel {
  display: grid;
  width: min(28rem, 100%);
  justify-items: start;
  gap: 0.7rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 1rem;
}

.operation-empty__panel h3 {
  color: var(--text-primary);
  font-size: 1rem;
}

.operation-empty__panel p {
  color: var(--muted-text);
  font-size: 0.82rem;
  line-height: 1.5;
}

.operation-empty__panel button {
  min-height: 2.3rem;
  border-radius: 7px;
  background: var(--accent-color);
  color: #fff;
  padding: 0 0.8rem;
  font-size: 0.82rem;
  font-weight: 800;
}

.operation-summary,
.operation-main,
.operation-column {
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
  min-height: 0;
}

.operation-summary {
  display: grid;
  align-content: start;
  gap: 0.65rem;
  padding: 0.8rem;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-muted) 86%, var(--accent-soft)),
      var(--panel-muted)
    );
  border-color: color-mix(in srgb, var(--border-strong) 72%, var(--accent-color));
  box-shadow: inset -1px 0 0 color-mix(in srgb, var(--border-color) 72%, transparent);
}

.operation-badge {
  display: inline-flex;
  min-height: 1.55rem;
  align-items: center;
  border-radius: 999px;
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  padding: 0 0.55rem;
  font-size: 0.68rem;
  font-weight: 900;
}

.operation-summary h3 {
  margin-top: 0.55rem;
  color: var(--text-primary);
  font-size: 0.98rem;
  line-height: 1.3;
}

.operation-summary p,
.operation-purpose span,
.operation-meta dt,
.operation-meta dd,
.operation-score span,
.operation-progress span,
.operation-checklist span,
.operation-checklist li,
.operation-main__head p,
.operation-main__head span,
.operation-column__head p,
.operation-task p,
.operation-task small {
  color: var(--muted-text);
  font-size: 0.74rem;
}

.operation-purpose,
.operation-meta,
.operation-score,
.operation-progress,
.operation-checklist {
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.65rem;
}

.operation-purpose span,
.operation-meta dt,
.operation-checklist span {
  display: block;
  margin-bottom: 0.25rem;
  color: var(--text-secondary);
  font-size: 0.68rem;
  font-weight: 900;
}

.operation-purpose p {
  line-height: 1.45;
}

.operation-meta {
  display: grid;
  gap: 0.42rem;
  margin: 0;
}

.operation-meta div {
  display: grid;
  gap: 0.12rem;
}

.operation-meta dd {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
  line-height: 1.35;
}

.operation-summary-grid {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 0.5rem;
}

.operation-score strong {
  display: block;
  color: var(--accent-color);
  font-size: 1.55rem;
  line-height: 1;
}

.operation-progress div {
  height: 0.55rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-color);
  margin-top: 0.45rem;
}

.operation-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-color);
}

.operation-checklist ul {
  display: grid;
  gap: 0.32rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.operation-checklist li {
  position: relative;
  padding-left: 1rem;
  font-weight: 700;
}

.operation-checklist li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0.42rem;
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 999px;
  background: var(--color-warning);
}

.operation-checklist li.done::before {
  background: var(--color-success);
}

.operation-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 0.6rem;
  min-width: 0;
  min-height: 0;
  background: var(--panel-color);
  padding: 0.7rem;
}

.operation-main__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.55rem;
}

.operation-main__head h3 {
  color: var(--text-primary);
  font-size: 0.95rem;
}

.operation-main__head span {
  border-radius: 999px;
  background: var(--panel-muted);
  padding: 0.25rem 0.55rem;
  font-weight: 900;
}

.operation-board {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.55rem;
  min-height: 0;
}

.operation-column {
  min-width: 0;
  overflow: auto;
  padding: 0.65rem;
  background: var(--panel-color);
}

.operation-column__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 3rem;
  margin-bottom: 0.5rem;
}

.operation-column__head h3 {
  color: var(--text-primary);
  font-size: 0.84rem;
}

.operation-column__head p {
  margin-top: 0.15rem;
  line-height: 1.25;
}

.operation-column__head span {
  display: inline-flex;
  min-width: 1.35rem;
  height: 1.35rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.operation-task {
  display: grid;
  gap: 0.35rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.58rem;
}

.operation-task + .operation-task {
  margin-top: 0.45rem;
}

.operation-task div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.operation-task span {
  display: inline-flex;
  min-height: 1.35rem;
  align-items: center;
  border-radius: 5px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0 0.45rem;
  font-size: 0.66rem;
  font-weight: 800;
}

.operation-task strong {
  color: var(--text-primary);
  font-size: 0.82rem;
}

@media (max-width: 1180px) {
  .operation-workspace,
  .operation-board {
    grid-template-columns: 1fr;
  }
}
</style>
