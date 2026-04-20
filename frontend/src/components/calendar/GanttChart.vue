<script setup>
import { computed } from 'vue'
import TaskCard from '@/components/common/TaskCard.vue'
import {
  compareDateKeys,
  endOfMonth,
  endOfWeek,
  formatShortDate,
  getMonthWeeks,
  getWeekDays,
  startOfMonth,
  startOfWeek,
  todayKey,
} from '@/utils/calendar'
import { usePlannerStore } from '@/stores/planner'

const props = defineProps({
  tasks: {
    type: Array,
    default: () => [],
  },
  currentDate: {
    type: String,
    required: true,
  },
  viewMode: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['open-task', 'move-task'])

const store = usePlannerStore()

const groupOrder = ['planned', 'in_progress', 'review', 'at_risk', 'done']
const groupColors = {
  planned: '#9aa7bd',
  in_progress: '#59c36d',
  review: '#f5b64e',
  at_risk: '#df5f75',
  done: '#2f80ed',
}

const days = computed(() => {
  return props.viewMode === 'week'
    ? getWeekDays(props.currentDate)
    : getMonthWeeks(props.currentDate).flat()
})

const rangeStart = computed(() =>
  props.viewMode === 'week' ? startOfWeek(props.currentDate) : startOfMonth(props.currentDate),
)
const rangeEnd = computed(() =>
  props.viewMode === 'week' ? endOfWeek(props.currentDate) : endOfMonth(props.currentDate),
)

const visibleTasks = computed(() => {
  return props.tasks
    .filter((task) => task.dueDate >= rangeStart.value && task.startDate <= rangeEnd.value)
    .sort((left, right) => {
      const startCompare = compareDateKeys(left.startDate, right.startDate)

      if (startCompare !== 0) {
        return startCompare
      }

      return compareDateKeys(left.dueDate, right.dueDate)
    })
})

const groupedTasks = computed(() => {
  return groupOrder
    .map((status) => ({
      status,
      label: store.statusLabels[status] ?? status,
      color: groupColors[status],
      tasks: visibleTasks.value.filter((task) => task.status === status),
    }))
    .filter((group) => group.tasks.length)
})

const dayColumnWidth = computed(() => (props.viewMode === 'week' ? 132 : 72))
const timelineStyle = computed(() => ({
  gridTemplateColumns: `repeat(${days.value.length}, ${dayColumnWidth.value}px)`,
}))

const monthGroups = computed(() => {
  const groups = []

  days.value.forEach((day, index) => {
    const monthLabel = new Intl.DateTimeFormat('ko-KR', {
      month: 'long',
      year: 'numeric',
    }).format(new Date(`${day.key}T12:00:00`))

    const last = groups.at(-1)

    if (last?.label === monthLabel) {
      last.span += 1
      return
    }

    groups.push({
      label: monthLabel,
      start: index + 1,
      span: 1,
    })
  })

  return groups
})

const weekGroups = computed(() => {
  if (props.viewMode === 'week') {
    return [
      {
        label: `${formatShortDate(days.value[0]?.key)} - ${formatShortDate(days.value.at(-1)?.key)}`,
        start: 1,
        span: days.value.length,
      },
    ]
  }

  const groups = []

  for (let index = 0; index < days.value.length; index += 7) {
    const startDay = days.value[index]
    const endDay = days.value[Math.min(index + 6, days.value.length - 1)]
    groups.push({
      label: `${formatShortDate(startDay.key)} - ${formatShortDate(endDay.key)}`,
      start: index + 1,
      span: Math.min(7, days.value.length - index),
    })
  }

  return groups
})

const todayLineStyle = computed(() => {
  const index = days.value.findIndex((day) => day.key === todayKey())

  if (index === -1) {
    return { display: 'none' }
  }

  return {
    left: `${index * dayColumnWidth.value + dayColumnWidth.value / 2}px`,
  }
})

function memberFor(memberId) {
  return store.findMember(memberId)
}

function openTask(taskId) {
  emit('open-task', taskId)
}

function moveTask(dateKey, event) {
  const taskId = event.dataTransfer.getData('text/plain')

  if (taskId) {
    emit('move-task', { taskId, dateKey })
  }
}

function barStyle(task) {
  const normalizedStart = task.startDate < rangeStart.value ? rangeStart.value : task.startDate
  const normalizedEnd = task.dueDate > rangeEnd.value ? rangeEnd.value : task.dueDate
  const startOffset = Math.max(
    0,
    days.value.findIndex((day) => day.key === normalizedStart),
  )
  const endOffset = days.value.findIndex((day) => day.key === normalizedEnd)

  return {
    gridColumn: `${startOffset + 1} / span ${Math.max(endOffset - startOffset + 1, 1)}`,
    gridRow: '1',
  }
}
</script>

<template>
  <section class="gantt-board">
    <div class="gantt-board__header">
      <div class="gantt-board__sidebar-head">항목</div>

      <div class="gantt-board__timeline-head">
        <div class="gantt-board__months" :style="timelineStyle">
          <div
            v-for="group in monthGroups"
            :key="`${group.label}-${group.start}`"
            class="gantt-board__month"
            :style="{ gridColumn: `${group.start} / span ${group.span}` }"
          >
            {{ group.label }}
          </div>
        </div>

        <div class="gantt-board__weeks" :style="timelineStyle">
          <div
            v-for="group in weekGroups"
            :key="`${group.label}-${group.start}`"
            class="gantt-board__week"
            :style="{ gridColumn: `${group.start} / span ${group.span}` }"
          >
            {{ group.label }}
          </div>
        </div>
      </div>
    </div>

    <div class="gantt-board__body">
      <div class="gantt-board__sidebar">
        <template v-for="group in groupedTasks" :key="group.status">
          <div class="gantt-board__group-head">
            <span class="gantt-board__group-dot" :style="{ backgroundColor: group.color }" />
            <strong>{{ group.label }}</strong>
          </div>

          <button
            v-for="task in group.tasks"
            :key="task.id"
            class="gantt-board__sidebar-row"
            type="button"
            @click="openTask(task.id)"
          >
            <div>
              <strong>{{ task.title }}</strong>
              <small
                >{{ formatShortDate(task.startDate) }} - {{ formatShortDate(task.dueDate) }}</small
              >
            </div>

            <span>{{ memberFor(task.assigneeId)?.initials ?? 'NA' }}</span>
          </button>
        </template>
      </div>

      <div class="gantt-board__timeline">
        <div class="gantt-board__today" :style="todayLineStyle" />

        <template v-for="group in groupedTasks" :key="`${group.status}-timeline`">
          <div class="gantt-board__group-head gantt-board__group-head--ghost" />

          <div
            v-for="task in group.tasks"
            :key="`${task.id}-track`"
            class="gantt-board__track-row"
            :style="timelineStyle"
          >
            <div
              v-for="day in days"
              :key="`${task.id}-${day.key}`"
              class="gantt-board__cell"
              @dragover.prevent
              @drop="moveTask(day.key, $event)"
            />

            <TaskCard
              :task="task"
              :member="memberFor(task.assigneeId)"
              mode="bar"
              draggable
              :style="barStyle(task)"
              @select="openTask"
            />
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<style scoped>
.gantt-board {
  border-radius: 20px;
  overflow: auto;
}

.gantt-board__header,
.gantt-board__body {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr);
}

.gantt-board__sidebar-head,
.gantt-board__sidebar {
  position: sticky;
  left: 0;
  z-index: 2;
  background: var(--panel-color);
}

.gantt-board__sidebar-head {
  padding: 1rem;
  border-right: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  font-weight: 700;
}

.gantt-board__timeline-head {
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-subtle);
}

.gantt-board__months,
.gantt-board__weeks,
.gantt-board__track-row {
  display: grid;
  width: max-content;
  min-width: 100%;
}

.gantt-board__month,
.gantt-board__week {
  padding: 0.7rem 0.4rem;
  text-align: center;
  font-weight: 700;
  border-right: 1px solid var(--border-color);
}

.gantt-board__week {
  background: var(--panel-color);
  font-size: 0.78rem;
  color: var(--muted-text);
}

.gantt-board__sidebar {
  border-right: 1px solid var(--border-color);
}

.gantt-board__group-head {
  min-height: 44px;
  padding: 0.8rem 1rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-subtle);
}

.gantt-board__group-head--ghost {
  background: transparent;
}

.gantt-board__group-dot {
  width: 0.8rem;
  height: 0.8rem;
  border-radius: 999px;
}

.gantt-board__sidebar-row {
  width: 100%;
  min-height: 54px;
  padding: 0.8rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
}

.gantt-board__sidebar-row small,
.gantt-board__sidebar-row span {
  color: var(--muted-text);
}

.gantt-board__timeline {
  position: relative;
  width: max-content;
  min-width: 100%;
}

.gantt-board__today {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 2px;
  background: color-mix(in srgb, var(--accent-color) 74%, transparent);
  z-index: 3;
  pointer-events: none;
}

.gantt-board__track-row {
  position: relative;
  min-height: 54px;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
}

.gantt-board__cell {
  min-height: 54px;
  border-right: 1px solid var(--border-color);
  background: linear-gradient(180deg, transparent, transparent), var(--panel-color);
}

.gantt-board__track-row :deep(.task-card) {
  align-self: center;
  z-index: 2;
}

@media (max-width: 1180px) {
  .gantt-board__header,
  .gantt-board__body {
    min-width: 680px;
  }
}
</style>
