<script setup>
import { computed, ref, watch } from 'vue'
import { usePlannerStore } from '@/stores/planner'
import { formatMonthLabel, formatShortDate } from '@/utils/calendar'

const props = defineProps({
  tasks: {
    type: Array,
    default: () => [],
  },
  currentDate: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['open-task', 'create-task'])

const store = usePlannerStore()
const groupState = ref({})

const groupedTasks = computed(() => {
  const bucket = new Map()

  props.tasks.forEach((task) => {
    const monthKey = `${task.startDate.slice(0, 7)}-01`

    if (!bucket.has(monthKey)) {
      bucket.set(monthKey, {
        key: monthKey,
        label: formatMonthLabel(monthKey),
        dateKey: monthKey,
        tasks: [],
      })
    }

    bucket.get(monthKey).tasks.push(task)
  })

  return [...bucket.values()]
    .sort((left, right) => left.key.localeCompare(right.key))
    .map((group) => ({
      ...group,
      tasks: [...group.tasks].sort((left, right) => left.startDate.localeCompare(right.startDate)),
    }))
})

watch(
  groupedTasks,
  (groups) => {
    const next = { ...groupState.value }

    groups.forEach((group) => {
      if (!(group.key in next)) {
        next[group.key] = true
      }
    })

    groupState.value = next
  },
  { immediate: true },
)

const statusTones = {
  planned: '#9aa7bd',
  in_progress: '#59c36d',
  review: '#f5b64e',
  at_risk: '#df5f75',
  done: '#2f80ed',
}

const priorityTones = {
  low: '#a6d73f',
  medium: '#f7a8a8',
  high: '#e468c8',
  critical: '#ef5b5b',
}

function isExpanded(key) {
  return groupState.value[key] !== false
}

function toggleGroup(key) {
  groupState.value = {
    ...groupState.value,
    [key]: !isExpanded(key),
  }
}

function memberFor(memberId) {
  return store.findMember(memberId)
}

function averageProgress(tasks) {
  if (!tasks.length) {
    return 0
  }

  return Math.round(tasks.reduce((sum, task) => sum + task.progress, 0) / tasks.length)
}
</script>

<template>
  <section class="table-board">
    <div class="table-board__scroll">
      <table class="table-board__table">
        <thead>
          <tr>
            <th>항목</th>
            <th>우선순위</th>
            <th>담당자</th>
            <th>시작일</th>
            <th>유형</th>
            <th>상태</th>
            <th>진행률</th>
            <th>발행일</th>
          </tr>
        </thead>

        <tbody>
          <template v-for="group in groupedTasks" :key="group.key">
            <tr class="table-board__group-row">
              <td colspan="8">
                <button
                  class="table-board__group-toggle"
                  type="button"
                  @click="toggleGroup(group.key)"
                >
                  <span>{{ isExpanded(group.key) ? '▾' : '▸' }}</span>
                  <strong>{{ group.label }}</strong>
                  <small>{{ group.tasks.length }}개 항목</small>
                </button>
              </td>
            </tr>

            <template v-if="isExpanded(group.key)">
              <tr v-for="task in group.tasks" :key="task.id" class="table-board__task-row">
                <td class="table-board__item-cell">
                  <span
                    class="table-board__indicator"
                    :style="{ backgroundColor: task.palette.accent }"
                  />
                  <button
                    class="table-board__item-button"
                    type="button"
                    @click="emit('open-task', task.id)"
                  >
                    <strong>{{ task.title }}</strong>
                    <small>{{ task.requirementId }}</small>
                  </button>
                </td>

                <td>
                  <span
                    class="table-board__pill"
                    :style="{ backgroundColor: priorityTones[task.priority], color: '#fff' }"
                  >
                    {{ store.priorityLabels[task.priority] ?? task.priority }}
                  </span>
                </td>

                <td>
                  <div class="table-board__owner">
                    <span
                      class="table-board__avatar"
                      :style="{ backgroundColor: memberFor(task.assigneeId)?.accent ?? '#94a3b8' }"
                    >
                      {{ memberFor(task.assigneeId)?.initials ?? 'NA' }}
                    </span>
                    <span>{{ memberFor(task.assigneeId)?.name ?? '미지정' }}</span>
                  </div>
                </td>

                <td>{{ formatShortDate(task.startDate) }}</td>

                <td>
                  <span
                    class="table-board__pill"
                    :style="{ backgroundColor: task.palette.accent, color: '#fff' }"
                  >
                    {{ task.contentType }}
                  </span>
                </td>

                <td>
                  <span
                    class="table-board__pill"
                    :style="{ backgroundColor: statusTones[task.status], color: '#fff' }"
                  >
                    {{ store.statusLabels[task.status] ?? task.status }}
                  </span>
                </td>

                <td>
                  <div class="table-board__progress">
                    <span :style="{ width: `${task.progress}%` }" />
                  </div>
                  <strong class="table-board__progress-value">{{ task.progress }}%</strong>
                </td>

                <td>{{ formatShortDate(task.dueDate) }}</td>
              </tr>

              <tr class="table-board__add-row">
                <td colspan="8">
                  <button type="button" @click="emit('create-task', group.dateKey)">
                    + 항목 추가
                  </button>
                </td>
              </tr>

              <tr class="table-board__summary-row">
                <td colspan="5">
                  <strong>{{ group.label }}</strong>
                  <small>이 그룹에 {{ group.tasks.length }}개 항목</small>
                </td>
                <td>
                  <small
                    >{{ group.tasks.filter((task) => task.status === 'done').length }}개 완료</small
                  >
                </td>
                <td>
                  <strong>{{ averageProgress(group.tasks) }}%</strong>
                </td>
                <td>
                  <small>{{ formatShortDate(group.tasks[0].startDate) }}부터</small>
                </td>
              </tr>
            </template>
          </template>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.table-board {
  overflow: auto;
}

.table-board__scroll {
  min-width: 100%;
}

.table-board__table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.table-board thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--panel-subtle);
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
  text-align: left;
  padding: 0.95rem 0.85rem;
  border-bottom: 1px solid var(--border-color);
}

.table-board__task-row td,
.table-board__summary-row td,
.table-board__add-row td {
  padding: 0.85rem;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-color);
  vertical-align: middle;
}

.table-board__group-row td {
  padding: 1rem 0.85rem 0.55rem;
  background: var(--panel-color);
}

.table-board__group-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  cursor: pointer;
}

.table-board__group-toggle strong {
  font-size: 1.3rem;
}

.table-board__group-toggle small,
.table-board__item-button small,
.table-board__summary-row small {
  color: var(--muted-text);
}

.table-board__item-cell,
.table-board__owner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.table-board__indicator {
  width: 4px;
  min-height: 44px;
  border-radius: 999px;
  flex-shrink: 0;
}

.table-board__item-button {
  display: grid;
  gap: 0.1rem;
  cursor: pointer;
  text-align: left;
}

.table-board__pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 92px;
  min-height: 2.1rem;
  border-radius: 10px;
  padding: 0 0.7rem;
  font-size: 0.78rem;
  font-weight: 700;
}

.table-board__avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.72rem;
  font-weight: 800;
}

.table-board__progress {
  width: 92px;
  height: 1.4rem;
  border: 1px solid color-mix(in srgb, var(--success-color) 40%, var(--border-color));
  border-radius: 4px;
  background: color-mix(in srgb, var(--success-color) 7%, white);
  overflow: hidden;
  display: inline-flex;
  vertical-align: middle;
}

.table-board__progress span {
  height: 100%;
  background: #478f55;
}

.table-board__progress-value {
  margin-left: 0.6rem;
  font-size: 0.86rem;
}

.table-board__add-row button {
  color: var(--muted-text);
  cursor: pointer;
}

.table-board__summary-row strong {
  margin-right: 0.35rem;
}

.table-board__task-row:hover td {
  background: color-mix(in srgb, var(--panel-muted) 62%, var(--panel-color));
}
</style>
