<script setup>
import { computed, ref } from 'vue'

const statusColumns = [
  { id: 'backlog', label: '백로그', sub: 'Backlog' },
  { id: 'in_progress', label: '진행 중', sub: 'WIP' },
  { id: 'review', label: '검수/피드백', sub: 'Review' },
  { id: 'blocked', label: '보류/지연', sub: 'Blocked' },
  { id: 'done', label: '완료', sub: 'Done' },
]

const companies = [
  {
    id: 'hq',
    name: '한화 본사',
    role: '총괄 기획 및 최종 검수',
    scope: '전체 캠페인',
    progress: 78,
    color: 'primary',
  },
  {
    id: 'galleria',
    name: '갤러리아',
    role: 'VIP 타겟 및 오프라인 운영',
    scope: '프리미엄 라이프스타일',
    progress: 64,
    color: 'green',
  },
  {
    id: 'hotel',
    name: '호텔앤드리조트',
    role: '예약 페이지 및 패키지 운영',
    scope: '프리미엄 라이프스타일',
    progress: 58,
    color: 'blue',
  },
  {
    id: 'agency',
    name: '대행사 A',
    role: '콘텐츠 제작 및 매체 집행',
    scope: '공통 운영',
    progress: 71,
    color: 'pink',
  },
  {
    id: 'media',
    name: '미디어 랩 B',
    role: '광고 세팅 및 성과 리포트',
    scope: '디지털 캠페인',
    progress: 49,
    color: 'amber',
  },
]

const tasks = [
  {
    id: 'main-01',
    companyId: 'hq',
    campaign: '프리미엄 라이프스타일',
    status: 'review',
    title: '총괄 기획안 최종 검수',
    part: '마케팅',
    dueDate: '05.18 지연',
    ownerInitial: '한',
    priority: 'critical',
    comments: 6,
    attachments: 2,
    blockedReason: '',
  },
  {
    id: 'main-02',
    companyId: 'hq',
    campaign: '친환경 브랜드 위크',
    status: 'backlog',
    title: '본사 승인 기준 정리',
    part: '검수 정책',
    dueDate: '05.24',
    ownerInitial: '본',
    priority: 'medium',
    comments: 1,
    attachments: 1,
    blockedReason: '',
  },
  {
    id: 'main-03',
    companyId: 'galleria',
    campaign: '프리미엄 라이프스타일',
    status: 'in_progress',
    title: 'VIP 고객 타겟팅 명단 추출',
    part: 'CRM',
    dueDate: '05.21',
    ownerInitial: 'G',
    priority: 'high',
    comments: 4,
    attachments: 1,
    blockedReason: '',
  },
  {
    id: 'main-04',
    companyId: 'galleria',
    campaign: '프리미엄 라이프스타일',
    status: 'review',
    title: '오프라인 초청장 문구 검수',
    part: '콘텐츠',
    dueDate: '05.23',
    ownerInitial: 'G',
    priority: 'medium',
    comments: 8,
    attachments: 3,
    blockedReason: '',
  },
  {
    id: 'main-05',
    companyId: 'hotel',
    campaign: '프리미엄 라이프스타일',
    status: 'blocked',
    title: '예약 페이지 객실 재고 연동',
    part: '시스템 개발',
    dueDate: '05.19 지연',
    ownerInitial: 'H',
    priority: 'critical',
    comments: 9,
    attachments: 2,
    blockedReason: '갤러리아 타겟 명단 확정 필요',
  },
  {
    id: 'main-06',
    companyId: 'hotel',
    campaign: '프리미엄 라이프스타일',
    status: 'done',
    title: '패키지 상세 이미지 업로드',
    part: '온오프라인 운영',
    dueDate: '완료',
    ownerInitial: 'H',
    priority: 'low',
    comments: 2,
    attachments: 4,
    blockedReason: '',
  },
  {
    id: 'main-07',
    companyId: 'agency',
    campaign: '프리미엄 라이프스타일',
    status: 'in_progress',
    title: 'SNS 카드뉴스 1차 시안',
    part: '디자인',
    dueDate: '05.22',
    ownerInitial: 'A',
    priority: 'high',
    comments: 5,
    attachments: 5,
    blockedReason: '',
  },
  {
    id: 'main-08',
    companyId: 'agency',
    campaign: '친환경 브랜드 위크',
    status: 'done',
    title: '브랜드 키비주얼 확정본 전달',
    part: '디자인',
    dueDate: '완료',
    ownerInitial: 'A',
    priority: 'medium',
    comments: 3,
    attachments: 6,
    blockedReason: '',
  },
  {
    id: 'main-09',
    companyId: 'media',
    campaign: '디지털 전환 프로모션',
    status: 'backlog',
    title: '성과 리포트 템플릿 합의',
    part: '리포트',
    dueDate: '05.27',
    ownerInitial: 'M',
    priority: 'medium',
    comments: 0,
    attachments: 1,
    blockedReason: '',
  },
  {
    id: 'main-10',
    companyId: 'media',
    campaign: '프리미엄 라이프스타일',
    status: 'review',
    title: '전환 캠페인 소재 세트 검수',
    part: '광고',
    dueDate: '05.26',
    ownerInitial: 'M',
    priority: 'high',
    comments: 7,
    attachments: 3,
    blockedReason: '',
  },
]

const priorityLabels = {
  low: '낮음',
  medium: '보통',
  high: '높음',
  critical: '긴급',
}

const searchText = ref('')
const selectedPriority = ref('all')
const selectedCompanyId = ref(null)

const selectedCompany = computed(() => companies.find((company) => company.id === selectedCompanyId.value))

const filteredTasks = computed(() => {
  const query = searchText.value.trim().toLowerCase()

  return tasks.filter((task) => {
    const matchesQuery =
      !query ||
      [task.title, task.part, task.campaign, task.blockedReason].some((value) =>
        String(value ?? '').toLowerCase().includes(query),
      )
    const matchesPriority = selectedPriority.value === 'all' || task.priority === selectedPriority.value

    return matchesQuery && matchesPriority
  })
})

const boardMetrics = computed(() => ({
  active: tasks.filter((task) => task.status === 'in_progress').length,
  review: tasks.filter((task) => task.status === 'review').length,
  blocked: tasks.filter((task) => task.status === 'blocked').length,
}))

function getTasks(companyId, statusId, source = filteredTasks.value) {
  return source.filter((task) => task.companyId === companyId && task.status === statusId)
}

function getCompanyTasks(statusId) {
  if (!selectedCompany.value) {
    return []
  }

  return getTasks(selectedCompany.value.id, statusId, tasks)
}

function openCompanyBoard(companyId) {
  selectedCompanyId.value = companyId
}

function closeCompanyBoard() {
  selectedCompanyId.value = null
}

function getStatusTone(statusId) {
  if (statusId === 'done') return 'success'
  if (statusId === 'blocked') return 'warning'
  if (statusId === 'review') return 'primary'
  return 'info'
}
</script>

<template>
  <section class="team-board-page">
    <header class="team-board-hero">
      <div>
        <span class="eyebrow">Team Board</span>
        <h1>전체 협업 팀보드</h1>
        <p>본사와 모든 참여사의 업무 흐름을 한 화면에서 보고, 회사별 전용 보드로 바로 들어갑니다.</p>
      </div>
      <div class="team-board-summary" aria-label="전체 팀보드 요약">
        <div>
          <span>진행 중</span>
          <strong>{{ boardMetrics.active }}</strong>
        </div>
        <div>
          <span>검수/피드백</span>
          <strong>{{ boardMetrics.review }}</strong>
        </div>
        <div>
          <span>보류/지연</span>
          <strong>{{ boardMetrics.blocked }}</strong>
        </div>
      </div>
    </header>

    <div class="team-board-toolbar">
      <input v-model.trim="searchText" type="search" placeholder="캠페인, 업무명, 파트를 검색..." />
      <select v-model="selectedPriority" aria-label="우선순위 필터">
        <option value="all">전체 우선순위</option>
        <option value="critical">긴급</option>
        <option value="high">높음</option>
        <option value="medium">보통</option>
        <option value="low">낮음</option>
      </select>
    </div>

    <div class="main-board" :style="{ '--board-column-count': statusColumns.length }">
      <div class="main-board__head">
        <span>참여사</span>
        <span v-for="column in statusColumns" :key="column.id">
          {{ column.label }} <small>{{ column.sub }}</small>
        </span>
      </div>

      <div v-for="company in companies" :key="company.id" class="main-board__row">
        <aside class="company-cell">
          <div class="company-cell__title">
            <div>
              <strong>{{ company.name }}</strong>
              <span>{{ company.role }}</span>
            </div>
            <button type="button" aria-label="회사 전용 보드 열기" @click="openCompanyBoard(company.id)">↗</button>
          </div>
          <p>{{ company.scope }}</p>
          <div class="company-progress">
            <i :class="`fill-${company.color}`" :style="{ width: `${company.progress}%` }"></i>
          </div>
          <small>{{ company.progress }}% 진행</small>
        </aside>

        <div v-for="column in statusColumns" :key="column.id" class="board-column">
          <article
            v-for="task in getTasks(company.id, column.id)"
            :key="task.id"
            class="board-task"
            :class="[`board-task--${getStatusTone(task.status)}`, { 'board-task--urgent': task.priority === 'critical' }]"
          >
            <div class="board-task__top">
              <span>{{ task.part }}</span>
              <small>{{ priorityLabels[task.priority] }}</small>
            </div>
            <strong>{{ task.title }}</strong>
            <p>{{ task.campaign }}</p>
            <p v-if="task.blockedReason" class="board-task__risk">선행: {{ task.blockedReason }}</p>
            <footer>
              <span :class="{ warning: task.dueDate.includes('지연') }">{{ task.dueDate }}</span>
              <em>{{ task.ownerInitial }}</em>
              <small>댓글 {{ task.comments }} · 파일 {{ task.attachments }}</small>
            </footer>
          </article>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="selectedCompany" class="company-modal-backdrop" @click.self="closeCompanyBoard">
        <section class="company-modal" role="dialog" aria-modal="true" :aria-label="`${selectedCompany.name} 전용 보드`">
          <header class="company-modal__header">
            <div>
              <h2>{{ selectedCompany.name }} 전용 보드</h2>
              <p>해당 협력사가 로그인했을 때 보게 되는 단독 칸반 보드입니다. 타사의 업무나 예산 등 민감 정보는 제외됩니다.</p>
            </div>
            <button type="button" aria-label="닫기" @click="closeCompanyBoard">×</button>
          </header>

          <div class="company-modal__board">
            <section v-for="column in statusColumns" :key="column.id" class="company-modal__column">
              <h3>{{ column.label }}</h3>
              <article
                v-for="task in getCompanyTasks(column.id)"
                :key="task.id"
                class="board-task"
                :class="[`board-task--${getStatusTone(task.status)}`, { 'board-task--urgent': task.priority === 'critical' }]"
              >
                <div class="board-task__top">
                  <span>{{ task.part }}</span>
                  <small>{{ priorityLabels[task.priority] }}</small>
                </div>
                <strong>{{ task.title }}</strong>
                <p>{{ task.campaign }}</p>
                <p v-if="task.blockedReason" class="board-task__risk">선행: {{ task.blockedReason }}</p>
                <footer>
                  <span :class="{ warning: task.dueDate.includes('지연') }">{{ task.dueDate }}</span>
                  <em>{{ task.ownerInitial }}</em>
                  <small>댓글 {{ task.comments }} · 파일 {{ task.attachments }}</small>
                </footer>
              </article>
            </section>
          </div>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.team-board-page {
  display: grid;
  gap: 18px;
  padding: 24px;
}

.team-board-hero,
.team-board-toolbar,
.main-board {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.team-board-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
}

.eyebrow {
  color: var(--color-primary-700);
  font-size: 12px;
  font-weight: 850;
}

.team-board-hero h1 {
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 850;
}

.team-board-hero p {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.team-board-summary {
  display: grid;
  grid-template-columns: repeat(3, 110px);
  gap: 10px;
}

.team-board-summary div {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  padding: 12px;
}

.team-board-summary span,
.company-cell p,
.company-cell small,
.board-task p,
.board-task footer small,
.main-board__head small,
.company-modal__header p {
  color: var(--muted-text);
  font-size: 12px;
}

.team-board-summary strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 850;
}

.team-board-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px;
}

.team-board-toolbar input,
.team-board-toolbar select {
  min-height: 38px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--control-color);
  color: var(--text-primary);
  font-size: 13px;
  padding: 0 12px;
}

.team-board-toolbar input {
  width: min(360px, 100%);
}

.main-board {
  overflow-x: auto;
}

.main-board__head,
.main-board__row {
  display: grid;
  min-width: 1380px;
  grid-template-columns: 230px repeat(var(--board-column-count, 5), minmax(210px, 1fr));
}

.main-board__head {
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 850;
}

.main-board__head > span,
.company-cell,
.board-column {
  padding: 14px;
}

.main-board__row {
  border-top: 1px solid var(--border-color);
}

.company-cell {
  display: grid;
  align-content: start;
  gap: 10px;
  border-right: 1px solid var(--border-color);
}

.company-cell__title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.company-cell strong {
  display: block;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 850;
}

.company-cell__title span {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.company-cell button {
  display: inline-grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--color-danger);
  cursor: pointer;
  font-size: 16px;
  font-weight: 900;
}

.company-progress {
  height: 8px;
  overflow: hidden;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
}

.company-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.fill-primary {
  background: var(--color-primary-500);
}

.fill-green {
  background: var(--color-success);
}

.fill-blue {
  background: var(--color-info);
}

.fill-pink {
  background: #db2777;
}

.fill-amber {
  background: var(--color-warning);
}

.board-column {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 148px;
  border-right: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--panel-muted) 52%, var(--panel-color));
}

.board-column:last-child {
  border-right: 0;
}

.board-task {
  display: grid;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--color-info);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 12px;
}

.board-task--primary {
  border-left-color: var(--color-primary-500);
}

.board-task--success {
  border-left-color: var(--color-success);
}

.board-task--warning,
.board-task--urgent {
  border-left-color: var(--color-warning);
}

.board-task__top,
.board-task footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.board-task__top span,
.board-task__top small {
  display: inline-flex;
  width: fit-content;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 800;
  padding: 2px 7px;
}

.board-task__top span {
  background: var(--panel-muted);
  color: var(--text-secondary);
}

.board-task__top small {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.board-task strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 850;
  line-height: 1.35;
}

.board-task__risk {
  color: var(--color-warning-dark);
  font-weight: 800;
}

.board-task footer {
  flex-wrap: wrap;
}

.board-task footer span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 850;
}

.board-task footer span.warning {
  color: var(--color-danger);
}

.board-task footer em {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  font-size: 11px;
  font-style: normal;
  font-weight: 850;
}

.company-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  overflow-y: auto;
  background: rgba(15, 23, 42, 0.5);
  padding: 24px;
}

.company-modal {
  width: min(980px, 100%);
  max-height: calc(100vh - 48px);
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.26);
}

.company-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid var(--border-color);
  padding: 22px 24px 18px;
}

.company-modal__header h2 {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 850;
}

.company-modal__header p {
  margin-top: 7px;
  line-height: 1.6;
}

.company-modal__header button {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 20px;
}

.company-modal__board {
  display: grid;
  grid-template-columns: repeat(5, minmax(170px, 1fr));
  gap: 0;
  max-height: calc(100vh - 190px);
  overflow: auto;
}

.company-modal__column {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 360px;
  border-right: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--panel-muted) 52%, var(--panel-color));
  padding: 14px;
}

.company-modal__column:last-child {
  border-right: 0;
}

.company-modal__column h3 {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 850;
}

@media (max-width: 900px) {
  .team-board-hero,
  .team-board-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .team-board-summary {
    grid-template-columns: repeat(3, 1fr);
  }

  .company-modal__board {
    grid-template-columns: repeat(5, minmax(180px, 1fr));
  }
}
</style>
