<script setup>
import { computed } from 'vue'
import { usePlannerStore } from '@/stores/planner'
import { differenceInDays } from '@/utils/calendar'

const props = defineProps({
  task: {
    type: Object,
    required: true,
  },
  member: {
    type: Object,
    default: null,
  },
  mode: {
    type: String,
    default: 'compact',
  },
  draggable: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'drag-task'])

const store = usePlannerStore()

const statusLabel = computed(() => store.statusLabels[props.task.status] ?? props.task.status)
const priorityLabel = computed(
  () => store.priorityLabels[props.task.priority] ?? props.task.priority,
)
const durationLabel = computed(
  () => `${differenceInDays(props.task.dueDate, props.task.startDate) + 1}일`,
)

function handleClick() {
  emit('select', props.task.id)
}

function handleDragStart(event) {
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', props.task.id)
  emit('drag-task', props.task.id)
}
</script>

<template>
  <button
    class="task-card"
    :class="[`task-card--${props.mode}`]"
    :style="{
      '--task-surface': props.task.palette.surface,
      '--task-accent': props.task.palette.accent,
      '--task-text': props.task.palette.text,
    }"
    type="button"
    :draggable="props.draggable"
    @click="handleClick"
    @dragstart="handleDragStart"
  >
    <template v-if="props.mode === 'compact'">
      <div class="task-card__topline">
        <span class="task-card__status-chip">{{ statusLabel }}</span>
        <span class="task-card__priority-chip">{{ priorityLabel }}</span>
      </div>

      <strong class="task-card__title">{{ props.task.title }}</strong>

      <div class="task-card__footer">
        <span
          class="task-card__avatar"
          :style="{ backgroundColor: props.member?.accent ?? '#94a3b8' }"
        >
          {{ props.member?.initials ?? 'NA' }}
        </span>
        <div>
          <strong>{{ props.member?.name ?? '미지정' }}</strong>
        </div>
      </div>
    </template>

    <template v-else-if="props.mode === 'bar'">
      <div class="task-card__bar">
        <div class="task-card__bar-copy">
          <strong>{{ props.task.title }}</strong>
          <small>{{ statusLabel }} · {{ durationLabel }}</small>
        </div>

        <span class="task-card__bar-owner">{{ props.member?.initials ?? 'NA' }}</span>
      </div>
    </template>

    <template v-else>
      <div class="task-card__calendar-pill">
        <span class="task-card__calendar-dot" />
        <strong>{{ props.task.title }}</strong>
      </div>
    </template>
  </button>
</template>

<style scoped>
.task-card {
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    filter 0.16s ease;
}

.task-card:hover {
  transform: translateY(-1px);
}

.task-card--compact {
  border: 1px solid color-mix(in srgb, var(--task-accent) 22%, var(--border-color));
  border-radius: 20px;
  background:
    linear-gradient(
      180deg,
      var(--task-surface),
      color-mix(in srgb, var(--task-surface) 76%, white)
    ),
    var(--task-surface);
  box-shadow: 0 18px 28px rgba(18, 38, 63, 0.08);
  color: var(--task-text);
  min-height: 108px;
  padding: 0.9rem 0.95rem;
  display: grid;
  gap: 0.55rem;
}

.task-card__topline,
.task-card__footer {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 0.6rem;
}

.task-card__topline {
  flex-wrap: wrap;
}

.task-card__status-chip,
.task-card__priority-chip {
  border-radius: 999px;
  padding: 0.24rem 0.56rem;
  font-size: 0.68rem;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.65);
}

.task-card__title {
  font-size: 0.98rem;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.task-card__footer > div {
  display: grid;
}

.task-card__footer strong {
  font-size: 0.86rem;
  line-height: 1.15;
}

.task-card__avatar {
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 800;
  flex-shrink: 0;
}

.task-card--bar {
  border: 0;
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.task-card__bar {
  min-height: 38px;
  height: 100%;
  border-radius: 10px;
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--task-accent) 92%, white),
    color-mix(in srgb, var(--task-accent) 82%, black 4%)
  );
  color: #fff;
  padding: 0.48rem 0.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  box-shadow: 0 10px 20px color-mix(in srgb, var(--task-accent) 22%, transparent);
}

.task-card__bar-copy {
  min-width: 0;
}

.task-card__bar-copy strong,
.task-card__bar-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card__bar-copy strong {
  font-size: 0.8rem;
}

.task-card__bar-copy small,
.task-card__bar-owner {
  font-size: 0.7rem;
  opacity: 0.88;
}

.task-card__bar-owner {
  min-width: 1.6rem;
  text-align: right;
  font-weight: 700;
}

.task-card--calendar {
  border: 0;
  background: var(--task-accent);
  color: #fff;
  border-radius: 6px;
  box-shadow: none;
  min-height: 0;
  padding: 0.35rem 0.55rem;
}

.task-card--calendar:hover {
  filter: brightness(1.03);
}

.task-card__calendar-pill {
  display: flex;
  align-items: center;
  gap: 0.42rem;
  min-width: 0;
}

.task-card__calendar-dot {
  width: 0.42rem;
  height: 0.42rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  flex-shrink: 0;
}

.task-card__calendar-pill strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.72rem;
  font-weight: 700;
}

@media (max-width: 920px) {
  .task-card__bar {
    padding-inline: 0.6rem;
  }
}
</style>
