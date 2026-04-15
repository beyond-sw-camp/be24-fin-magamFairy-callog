<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import MetricCard from '@/components/dashboard/MetricCard.vue'
import PageTabs from '@/components/common/PageTabs.vue'
import { usePlannerStore } from '@/stores/planner'
import { todayKey } from '@/utils/calendar'

const router = useRouter()
const store = usePlannerStore()

const dashboardTabs = [
  {
    value: 'overview',
    label: '개요',
    icon: 'M3 3h8v8H3V3Zm10 0h8v5h-8V3ZM13 10h8v11h-8V10ZM3 13h8v8H3v-8Z',
  },
  {
    value: 'calendar',
    label: '캘린더',
    icon: 'M7 2v3M17 2v3M3 8h18M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z',
  },
  {
    value: 'table',
    label: '메인 테이블',
    icon: 'M3 3h8v8H3V3Zm10 0h8v5h-8V3ZM13 10h8v11h-8V10ZM3 13h8v8H3v-8Z',
  },
  {
    value: 'gantt',
    label: '간트차트',
    icon: 'M4 18h4V9H4v9Zm6 0h4V5h-4v13Zm6 0h4v-7h-4v7Z',
  },
]

const taskStats = computed(() => {
  const tasks = store.filteredTasks
  const done = tasks.filter((task) => task.status === 'done').length
  const inProgress = tasks.filter((task) => task.status === 'in_progress').length
  const overdue = tasks.filter((task) => task.status !== 'done' && task.dueDate < todayKey()).length
  const completionRate = tasks.length ? Math.round((done / tasks.length) * 100) : 0

  return {
    total: tasks.length,
    done,
    inProgress,
    overdue,
    completionRate,
  }
})

const workload = computed(() => {
  return store.members
    .map((member) => {
      const count = store.filteredTasks.filter((task) => task.assigneeId === member.id).length
      return { ...member, count }
    })
    .sort((left, right) => right.count - left.count)
})

const urgentTasks = computed(() => {
  return [...store.filteredTasks]
    .filter((task) => task.status !== 'done')
    .sort((left, right) => left.dueDate.localeCompare(right.dueDate))
    .slice(0, 5)
})

const weeklyVolume = computed(() => [
  { label: '1주차', value: 4 },
  { label: '2주차', value: 8 },
  { label: '3주차', value: 6 },
  { label: '4주차', value: 9 },
  { label: '5주차', value: 5 },
])

function handleTab(tab) {
  if (tab === 'overview') {
    return
  }

  store.setCalendarTab(tab)
  router.push('/calendar')
}
</script>

<template>
  <section class="dashboard">
    <div class="dashboard__header">
      <div>
        <p class="section-eyebrow">보드 개요</p>
        <h2>일정, 작업량, 마감 현황을 한눈에 보고 바로 보드 작업으로 이어갈 수 있습니다.</h2>
      </div>

      <PageTabs active="overview" :tabs="dashboardTabs" @select="handleTab" />
    </div>

    <div class="dashboard__metrics">
      <MetricCard
        label="완료"
        :value="String(taskStats.done)"
        accent="#2f80ed"
        helper="완료된 카드"
      />
      <MetricCard
        label="진행 중"
        :value="String(taskStats.inProgress)"
        accent="#59c36d"
        helper="현재 진행 중인 항목"
      />
      <MetricCard
        label="지연"
        :value="String(taskStats.overdue)"
        accent="#df5f75"
        helper="마감일이 지난 항목"
      />
      <MetricCard
        label="완료율"
        :value="`${taskStats.completionRate}%`"
        accent="#7c4dff"
        helper="현재 필터 기준"
      />
    </div>

    <div class="dashboard__grid">
      <article class="dashboard__panel dashboard__panel--wide">
        <div class="dashboard__panel-head">
          <div>
            <p>주간 작업량</p>
            <h3>현재 화면에 {{ taskStats.total }}개 항목이 있습니다</h3>
          </div>

          <button class="ghost-button" type="button" @click="router.push('/calendar')">
            캘린더 열기
          </button>
        </div>

        <div class="volume-chart">
          <div v-for="bar in weeklyVolume" :key="bar.label" class="volume-chart__item">
            <div class="volume-chart__bar">
              <span :style="{ height: `${bar.value * 12}px` }" />
            </div>
            <strong>{{ bar.label }}</strong>
          </div>
        </div>
      </article>

      <article class="dashboard__panel">
        <div class="dashboard__panel-head">
          <div>
            <p>담당자별 작업량</p>
            <h3>담당자 기준 작업 수</h3>
          </div>
        </div>

        <div class="workload-list">
          <div v-for="member in workload" :key="member.id" class="workload-list__row">
            <div class="workload-list__member">
              <span class="workload-list__avatar" :style="{ backgroundColor: member.accent }">
                {{ member.initials }}
              </span>
              <div>
                <strong>{{ member.name }}</strong>
                <p>{{ member.role }}</p>
              </div>
            </div>

            <span>{{ member.count }}</span>
          </div>
        </div>
      </article>

      <article class="dashboard__panel dashboard__panel--wide">
        <div class="dashboard__panel-head">
          <div>
            <p>다음 일정</p>
            <h3>우선 확인이 필요한 카드</h3>
          </div>
        </div>

        <div class="urgent-list">
          <button
            v-for="task in urgentTasks"
            :key="task.id"
            class="urgent-list__item"
            type="button"
            @click="store.openTask(task.id)"
          >
            <div>
              <strong>{{ task.title }}</strong>
              <p>{{ task.requirementId }} · {{ task.customer }}</p>
            </div>
            <span>{{ task.dueDate }}</span>
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 1rem;
}

.dashboard__header,
.dashboard__panel-head,
.workload-list__row,
.workload-list__member,
.urgent-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.dashboard__header h2 {
  margin-top: 0.35rem;
  max-width: 780px;
  font-size: 1.55rem;
  line-height: 1.3;
}

.dashboard__metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.dashboard__grid {
  display: grid;
  grid-template-columns: 1.7fr 1fr;
  gap: 1rem;
}

.dashboard__panel {
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: var(--panel-color);
  box-shadow: var(--shadow-soft);
  padding: 1.1rem;
}

.dashboard__panel--wide {
  min-height: 320px;
}

.dashboard__panel-head {
  margin-bottom: 1rem;
}

.dashboard__panel-head p,
.workload-list__member p,
.urgent-list__item p,
.urgent-list__item span {
  color: var(--muted-text);
}

.volume-chart {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.85rem;
  align-items: end;
  min-height: 240px;
}

.volume-chart__item {
  display: grid;
  gap: 0.55rem;
  justify-items: center;
}

.volume-chart__bar {
  width: 100%;
  min-height: 190px;
  border-radius: 24px;
  background: var(--panel-muted);
  display: flex;
  align-items: flex-end;
  padding: 0.8rem;
}

.volume-chart__bar span {
  width: 100%;
  border-radius: 18px;
  background: linear-gradient(180deg, var(--accent-color), var(--accent-strong));
}

.workload-list,
.urgent-list {
  display: grid;
  gap: 0.75rem;
}

.workload-list__row,
.urgent-list__item {
  padding: 0.85rem 0.95rem;
  border-radius: 18px;
  background: var(--panel-muted);
}

.workload-list__avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.72rem;
  font-weight: 700;
}

.urgent-list__item {
  cursor: pointer;
  text-align: left;
}

@media (max-width: 1180px) {
  .dashboard__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .dashboard__metrics {
    grid-template-columns: 1fr;
  }

  .volume-chart {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
