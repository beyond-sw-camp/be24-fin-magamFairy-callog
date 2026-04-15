<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import Header from '@/components/Header.vue'
import Sidebar from '@/components/Sidebar.vue'
import TaskModal from '@/components/common/TaskModal.vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

onMounted(() => {
  store.initialize()
})
</script>

<template>
  <div class="app-shell">
    <Sidebar />
    <button
      class="app-shell__overlay"
      :class="{ 'app-shell__overlay--visible': !store.sidebarCollapsed }"
      type="button"
      aria-label="사이드바 닫기"
      @click="store.setSidebarCollapsed(true)"
    />

    <div class="app-shell__main">
      <Header />

      <main class="app-shell__content">
        <RouterView />
      </main>
    </div>

    <TaskModal />
  </div>
</template>
