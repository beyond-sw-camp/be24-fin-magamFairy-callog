<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import AppHeader from '@/components/Header.vue'
import AppSidebar from '@/layout/Sidebar.vue'
import AppSidebar2 from '@/layout/Sidebar2.vue'
import TaskModal from '@/components/common/TaskModal.vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

onMounted(() => {
  store.initialize()
})
</script>

<template>
  <div class="callog-layout">
    <AppSidebar />
    <AppSidebar2 />
    <div class="callog-main">
      <AppHeader />
      <main class="callog-content">
        <RouterView />
      </main>
    </div>
  </div>
  <TaskModal />
</template>
