<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import Header from '@/components/Header.vue'
import TaskModal from '@/components/common/TaskModal.vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

onMounted(() => {
  store.initialize()
})
</script>

<template>
    <div class="app-shell__main">
      <Header />
      <main class="app-shell__content">
        <RouterView />
      </main>
    </div>
    <TaskModal />
</template>
