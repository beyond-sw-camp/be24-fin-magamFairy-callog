<script setup>
import { computed } from 'vue'
import TaskCard from '@/components/common/TaskCard.vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

const columns = computed(() => [
  { value: 'planned', label: store.statusLabels.planned },
  { value: 'in_progress', label: store.statusLabels.in_progress },
  { value: 'review', label: store.statusLabels.review },
  { value: 'done', label: store.statusLabels.done },
])

const groupedTasks = computed(() => {
  return columns.value.map((column) => ({
    ...column,
    tasks: store.filteredTasks.filter((task) => task.status === column.value),
  }))
})

function memberFor(memberId) {
  return store.findMember(memberId)
}
</script>

<template>
  <section class="tasks-view">
    <div class="tasks-view__header">
      <div>
        <p class="section-eyebrow">작업 보드</p>
        <h2>
          같은 검색과 필터 상태를 유지한 채 칸반 형태로 작업 흐름을 빠르게 확인할 수 있습니다.
        </h2>
      </div>
    </div>

    <div class="tasks-view__board">
      <article v-for="column in groupedTasks" :key="column.value" class="tasks-view__column">
        <header>
          <strong>{{ column.label }}</strong>
          <span>{{ column.tasks.length }}</span>
        </header>

        <div class="tasks-view__stack">
          <TaskCard
            v-for="task in column.tasks"
            :key="task.id"
            :task="task"
            :member="memberFor(task.assigneeId)"
            @select="store.openTask($event)"
          />

          <p v-if="column.tasks.length === 0" class="tasks-view__empty">
            이 컬럼에 카드가 없습니다.
          </p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.tasks-view {
  display: grid;
  gap: 1rem;
}

.tasks-view__header h2 {
  margin-top: 0.35rem;
  font-size: 1.5rem;
  line-height: 1.3;
}

.tasks-view__board {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
}

.tasks-view__column {
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: var(--panel-color);
  box-shadow: var(--shadow-soft);
  padding: 1rem;
  display: grid;
  gap: 1rem;
}

.tasks-view__column header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tasks-view__column span,
.tasks-view__empty {
  color: var(--muted-text);
}

.tasks-view__stack {
  display: grid;
  gap: 0.85rem;
}

@media (max-width: 1180px) {
  .tasks-view__board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .tasks-view__board {
    grid-template-columns: 1fr;
  }
}
</style>
