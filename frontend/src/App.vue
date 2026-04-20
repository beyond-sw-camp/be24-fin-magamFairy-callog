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
  <RouterView />
</template>
