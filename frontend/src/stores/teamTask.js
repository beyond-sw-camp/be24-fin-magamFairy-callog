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

  // 캠페인별 태스크 완료율 (0~100 정수). DONE 상태 태스크 / 전체 태스크.
  const completionRateByCampaignId = computed(() => {
    const total = {}
    const done = {}
    for (const t of tasks.value) {
      if (t.campaignIdx == null) continue
      const key = String(t.campaignIdx)
      total[key] = (total[key] ?? 0) + 1
      if (String(t.status).toUpperCase() === 'DONE') {
        done[key] = (done[key] ?? 0) + 1
      }
    }
    const rate = {}
    for (const key of Object.keys(total)) {
      rate[key] = total[key] > 0 ? Math.round((done[key] ?? 0) / total[key] * 100) : 0
    }
    return rate
  })

  let inFlight = null
  async function fetch() {
    // 진행 중 요청이 있으면 그 Promise를 반환 (조용히 skip하지 않음 → awaiter가 최신값 보장)
    if (inFlight) return inFlight
    loading.value = true
    inFlight = (async () => {
      try {
        const data = await ListAllTasks()
        tasks.value = Array.isArray(data) ? data : []
      } catch {
        // 사이드바 카운트는 비필수 — 실패 시 무시
      } finally {
        loading.value = false
        inFlight = null
      }
    })()
    return inFlight
  }

  return { tasks, loading, countByCampaignId, completionRateByCampaignId, fetch }
})
