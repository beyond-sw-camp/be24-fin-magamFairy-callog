<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import TaskModal from '@/components/common/TaskModal.vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

onMounted(() => {
  store.initialize()
})
</script>

<template>
  <RouterView />
  <TaskModal />
</template>
