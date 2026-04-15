<script setup>
import { computed } from 'vue'
import TaskCard from '@/components/common/TaskCard.vue'
import { buildLaneSegments, endOfWeek, getWeekDays, startOfWeek } from '@/utils/calendar'
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

const days = computed(() => getWeekDays(props.currentDate))
const weekStart = computed(() => startOfWeek(props.currentDate))
const weekEnd = computed(() => endOfWeek(props.currentDate))
const laneData = computed(() => buildLaneSegments(props.tasks, weekStart.value, weekEnd.value))

function memberFor(memberId) {
  return store.findMember(memberId)
}

function tasksForDay(dateKey) {
  return props.tasks.filter((task) => task.startDate === dateKey)
}

function selectTask(taskId) {
  emit('open-task', taskId)
}

function moveTask(dateKey, event) {
  const taskId = event.dataTransfer.getData('text/plain')

  if (taskId) {
    emit('move-task', { taskId, dateKey })
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
  <section class="week-board">
    <div class="week-board__header">
      <div
        v-for="day in days"
        :key="day.key"
        class="week-board__heading"
        :class="{ 'week-board__heading--today': day.isToday }"
      >
        <span>{{ day.weekdayLabel }}</span>
        <strong>{{ day.dayNumber }}</strong>
      </div>
    </div>

    <div v-if="!props.spanMode" class="week-board__columns">
      <article
        v-for="day in days"
        :key="day.key"
        class="week-board__column"
        @dragover.prevent
        @drop="moveTask(day.key, $event)"
      >
        <div class="week-board__top">
          <button class="week-board__add" type="button" @click="emit('create-task', day.key)">
            +
          </button>
        </div>

        <div class="week-board__stack">
          <TaskCard
            v-for="task in tasksForDay(day.key)"
            :key="task.id"
            :task="task"
            :member="memberFor(task.assigneeId)"
            draggable
            @select="selectTask"
          />

          <p v-if="tasksForDay(day.key).length === 0" class="week-board__empty">
            예정된 항목이 없습니다.
          </p>
        </div>
      </article>
    </div>

    <div v-else class="week-board__timeline">
      <div
        class="week-board__timeline-grid"
        :style="{ gridTemplateRows: `repeat(${laneData.laneCount}, minmax(72px, auto))` }"
      >
        <template v-for="laneIndex in laneData.laneCount" :key="`lane-${laneIndex}`">
          <div
            v-for="day in days"
            :key="`${day.key}-${laneIndex}`"
            class="week-board__timeline-cell"
            @dragover.prevent
            @drop="moveTask(day.key, $event)"
          />
        </template>

        <TaskCard
          v-for="segment in laneData.segments"
          :key="segment.task.id"
          :task="segment.task"
          :member="memberFor(segment.task.assigneeId)"
          mode="bar"
          draggable
          :style="barStyle(segment)"
          @select="selectTask"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.week-board {
  display: grid;
  gap: 0.8rem;
}

.week-board__header,
.week-board__columns {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.7rem;
}

.week-board__columns {
  align-items: start;
}

.week-board__heading {
  padding: 0.85rem 0.95rem;
  border-radius: 16px;
  background: var(--panel-subtle);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--muted-text);
}

.week-board__heading strong {
  color: var(--text-primary);
  font-size: 1rem;
}

.week-board__heading--today {
  border-color: color-mix(in srgb, var(--accent-color) 44%, var(--border-color));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent-color) 18%, transparent);
}

.week-board__column {
  min-height: 320px;
  border-radius: 22px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
  overflow: visible;
}

.week-board__top {
  display: flex;
  justify-content: flex-end;
}

.week-board__add {
  width: 1.9rem;
  height: 1.9rem;
  border-radius: 10px;
  background: var(--panel-muted);
  color: var(--muted-text);
  display: grid;
  place-items: center;
  cursor: pointer;
  opacity: 0;
}

.week-board__column:hover .week-board__add,
.week-board__add:focus-visible {
  opacity: 1;
}

.week-board__stack {
  display: grid;
  gap: 0.7rem;
  align-content: start;
  grid-auto-rows: max-content;
}

.week-board__empty {
  color: var(--muted-text);
  font-size: 0.84rem;
  padding: 0.35rem 0.15rem;
}

.week-board__timeline {
  border-radius: 24px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  overflow: auto;
  padding: 0.9rem;
}

.week-board__timeline-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(140px, 1fr));
  gap: 0.65rem;
}

.week-board__timeline-cell {
  min-height: 72px;
  border-radius: 16px;
  border: 1px dashed color-mix(in srgb, var(--border-color) 86%, transparent);
  background: var(--panel-muted);
}

@media (max-width: 1180px) {
  .week-board__header,
  .week-board__columns {
    min-width: 980px;
  }
}

@media (hover: none) {
  .week-board__add {
    opacity: 1;
  }
}
</style>
