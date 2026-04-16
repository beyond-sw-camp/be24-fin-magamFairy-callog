<script setup>
import MonthCalendar from '@/components/calendar/MonthCalendar.vue'
import WeekCalendar from '@/components/calendar/WeekCalendar.vue'

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
  spanMode: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['open-task', 'move-task', 'create-task'])
</script>

<template>
  <WeekCalendar
    v-if="props.viewMode === 'week'"
    :tasks="props.tasks"
    :current-date="props.currentDate"
    :span-mode="props.spanMode"
    @create-task="emit('create-task', $event)"
    @move-task="emit('move-task', $event)"
    @open-task="emit('open-task', $event)"
  />

  <MonthCalendar
    v-else
    :tasks="props.tasks"
    :current-date="props.currentDate"
    :span-mode="props.spanMode"
    @create-task="emit('create-task', $event)"
    @move-task="emit('move-task', $event)"
    @open-task="emit('open-task', $event)"
  />
</template>
