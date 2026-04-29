<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

onMounted(() => {
  store.initialize()
})
</script>

<template>
  <main class="first-layout ui-page">
    <RouterView />
  </main>
</template>

<style scoped>
.first-layout {
  min-height: 100vh;
  background: var(--surface-page);
}
</style>
