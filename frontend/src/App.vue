<script setup>
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import AdCheckFloatingProgress from '@/components/adcheck/AdCheckFloatingProgress.vue'
import ToastContainer from '@/components/common/ToastContainer.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const store = usePlannerStore()

onMounted(() => {
  store.initialize()
})
</script>

<template>
  <RouterView />
  <AdCheckFloatingProgress />
  <ToastContainer />
  <ConfirmDialog />
</template>
