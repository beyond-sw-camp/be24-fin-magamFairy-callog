import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ListAllTasks } from '@/api/teamboard'

export const useTeamTaskStore = defineStore('teamTask', () => {
  const tasks = ref([])
  const loading = ref(false)

  const countByCampaignId = computed(() => {
    const map = {}
    for (const t of tasks.value) {
      if (t.campaignIdx != null) {
        const key = String(t.campaignIdx)
        map[key] = (map[key] ?? 0) + 1
      }
    }
    return map
  })

  async function fetch() {
    if (loading.value) return
    loading.value = true
    try {
      const data = await ListAllTasks()
      tasks.value = Array.isArray(data) ? data : []
    } catch {
      // 사이드바 카운트는 비필수 — 실패 시 무시
    } finally {
      loading.value = false
    }
  }

  return { tasks, loading, countByCampaignId, fetch }
})
