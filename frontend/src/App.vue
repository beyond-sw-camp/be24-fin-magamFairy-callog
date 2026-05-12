<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import ToastContainer from '@/components/common/ToastContainer.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const store = usePlannerStore()

onMounted(() => {
  store.initialize()
})
</script>

<template>
  <RouterView />
  <ToastContainer />
  <ConfirmDialog />
</template>
