<script setup>
import { computed } from 'vue'
import TaskCard from '@/components/common/TaskCard.vue'
import { buildLaneSegments, getMonthWeeks } from '@/utils/calendar'
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
  spanMode: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['open-task', 'move-task', 'create-task'])

const store = usePlannerStore()

const weeks = computed(() => getMonthWeeks(props.currentDate))
const days = computed(() => weeks.value.flat())
const weekdayLabels = ['월', '화', '수', '목', '금', '토', '일']

const weekSegments = computed(() => {
  return weeks.value.map((week) => buildLaneSegments(props.tasks, week[0].key, week[6].key))
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

function tasksForDay(dateKey) {
  return props.tasks.filter((task) => task.startDate === dateKey)
}

function visibleTasksForDay(dateKey) {
  return tasksForDay(dateKey).slice(0, 3)
}

function overflowCount(dateKey) {
  return Math.max(tasksForDay(dateKey).length - 3, 0)
}

function weekStyle(index) {
  const laneCount = weekSegments.value[index]?.laneCount ?? 1

  return {
    minHeight: `${132 + laneCount * 40}px`,
  }
}

function barStyle(segment) {
  return {
    gridColumn: `${segment.startOffset + 1} / span ${segment.span}`,
    gridRow: `${segment.lane + 1}`,
  }
}
</script>

<template>
  <section class="month-board">
    <div v-if="!props.spanMode" class="month-board__frame">
      <div class="month-board__head">
        <div v-for="label in weekdayLabels" :key="label" class="month-board__head-cell">
          {{ label }}
        </div>
      </div>

      <div class="month-board__grid">
        <article
          v-for="day in days"
          :key="day.key"
          class="month-board__cell"
          :class="{
            'month-board__cell--muted': !day.isCurrentMonth,
            'month-board__cell--today': day.isToday,
          }"
          @dragover.prevent
          @drop="moveTask(day.key, $event)"
        >
          <div class="month-board__cell-top">
            <button class="month-board__add" type="button" @click="emit('create-task', day.key)">
              +
            </button>
            <strong>{{ day.dayNumber }}</strong>
          </div>

          <div class="month-board__events">
            <TaskCard
              v-for="task in visibleTasksForDay(day.key)"
              :key="task.id"
              :task="task"
              :member="memberFor(task.assigneeId)"
              mode="calendar"
              draggable
              @select="openTask"
            />

            <button
              v-if="overflowCount(day.key)"
              class="month-board__more"
              type="button"
              @click="emit('create-task', day.key)"
            >
              +{{ overflowCount(day.key) }}개 더
            </button>
          </div>
        </article>
      </div>
    </div>

    <div v-else class="month-board__timeline">
      <div class="month-board__head month-board__head--tight">
        <div v-for="label in weekdayLabels" :key="label" class="month-board__head-cell">
          {{ label }}
        </div>
      </div>

      <article
        v-for="(week, weekIndex) in weeks"
        :key="week[0].key"
        class="month-board__timeline-week"
        :style="weekStyle(weekIndex)"
      >
        <div
          v-for="columnIndex in 6"
          :key="`${week[0].key}-divider-${columnIndex}`"
          class="month-board__timeline-divider"
          :style="{ left: `calc(${(columnIndex * 100) / 7}% - 0.5px)` }"
        />

        <div class="month-board__timeline-days">
          <div
            v-for="day in week"
            :key="day.key"
            class="month-board__timeline-day"
            :class="{ 'month-board__timeline-day--muted': !day.isCurrentMonth }"
            @dragover.prevent
            @drop="moveTask(day.key, $event)"
          >
            <div class="month-board__timeline-top">
              <button
                class="month-board__add month-board__add--timeline"
                type="button"
                @click="emit('create-task', day.key)"
              >
                +
              </button>
              <strong>{{ day.dayNumber }}</strong>
            </div>
          </div>
        </div>

        <div
          class="month-board__bars"
          :style="{ gridTemplateRows: `repeat(${weekSegments[weekIndex].laneCount}, 34px)` }"
        >
          <TaskCard
            v-for="segment in weekSegments[weekIndex].segments"
            :key="`${segment.task.id}-${segment.segmentStart}`"
            :task="segment.task"
            :member="memberFor(segment.task.assigneeId)"
            mode="bar"
            draggable
            :style="barStyle(segment)"
            @select="openTask"
          />
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.month-board {
  display: grid;
}

.month-board__frame,
.month-board__timeline {
  border-radius: 18px;
  overflow: hidden;
  background: var(--panel-color);
}

.month-board__head,
.month-board__grid,
.month-board__timeline-days,
.month-board__bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.month-board__head {
  background: var(--panel-subtle);
}

.month-board__head--tight {
  border-bottom: 1px solid var(--border-color);
}

.month-board__head-cell {
  min-height: 40px;
  padding: 0.75rem 0.8rem;
  border-right: 1px solid var(--border-color);
  color: var(--muted-text);
  font-size: 0.78rem;
  font-weight: 700;
  text-align: center;
}

.month-board__head-cell:last-child {
  border-right: 0;
}

.month-board__grid {
  grid-auto-rows: minmax(138px, 1fr);
}

.month-board__cell {
  min-height: 138px;
  padding: 0.55rem 0.55rem 0.7rem;
  border-right: 1px solid var(--border-color);
  border-top: 1px solid var(--border-color);
  background: var(--panel-color);
  display: grid;
  align-content: start;
  gap: 0.45rem;
}

.month-board__cell:nth-child(7n) {
  border-right: 0;
}

.month-board__cell--muted,
.month-board__timeline-day--muted {
  background: color-mix(in srgb, var(--panel-muted) 78%, var(--panel-color));
}

.month-board__cell-top,
.month-board__timeline-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.month-board__cell-top strong,
.month-board__timeline-top strong {
  font-size: 0.82rem;
  color: var(--muted-text);
  font-weight: 700;
}

.month-board__cell--today .month-board__cell-top strong {
  color: var(--accent-color);
}

.month-board__events {
  display: grid;
  gap: 0.28rem;
  align-content: start;
}

.month-board__add {
  width: 1.55rem;
  height: 1.55rem;
  border-radius: 8px;
  background: var(--panel-muted);
  color: var(--muted-text);
  display: grid;
  place-items: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.month-board__cell:hover .month-board__add,
.month-board__timeline-day:hover .month-board__add,
.month-board__add:focus-visible {
  opacity: 1;
}

.month-board__more {
  color: var(--muted-text);
  font-size: 0.68rem;
  text-align: left;
  cursor: pointer;
}

.month-board__timeline-week {
  position: relative;
  border-top: 1px solid var(--border-color);
}

.month-board__timeline-divider {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--border-color);
  z-index: 0;
}

.month-board__timeline-days {
  grid-auto-rows: minmax(110px, 1fr);
  position: relative;
  z-index: 1;
}

.month-board__timeline-day {
  min-height: 110px;
  padding: 0.45rem 0.55rem;
  border-right: 1px solid var(--border-color);
  background: var(--panel-color);
}

.month-board__timeline-day:nth-child(7n) {
  border-right: 0;
}

.month-board__bars {
  position: absolute;
  inset: 2rem 0.45rem 0.6rem;
  gap: 0.3rem;
  pointer-events: none;
  z-index: 2;
}

.month-board__bars :deep(.task-card) {
  pointer-events: auto;
}

@media (max-width: 1180px) {
  .month-board {
    min-width: 980px;
  }
}

@media (hover: none) {
  .month-board__add {
    opacity: 1;
  }
}
</style>
