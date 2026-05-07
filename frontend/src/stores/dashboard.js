import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dashApi from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref(null)
  const quarterGoals = ref([])
  const partnerProgress = ref([])
  const reviewQueue = ref([])
  const blockers = ref([])
  const loading = ref(false)
  const errorMessage = ref(null)
  const usingMock = ref(false)

  async function loadAll(period) {
    loading.value = true
    errorMessage.value = null
    usingMock.value = false

    const tasks = [
      ['summary', () => dashApi.GetDashboardSummary()],
      ['quarterGoals', () => dashApi.GetQuarterGoals(period)],
      ['partnerProgress', () => dashApi.GetPartnerProgress()],
      ['reviewQueue', () => dashApi.GetReviewQueue()],
      ['blockers', () => dashApi.GetBlockers()],
    ]

    const results = await Promise.allSettled(tasks.map(([, fn]) => fn()))

    results.forEach((result, idx) => {
      const [key] = tasks[idx]
      if (result.status === 'fulfilled') {
        if (key === 'summary') summary.value = result.value
        if (key === 'quarterGoals') quarterGoals.value = normalizeArray(result.value)
        if (key === 'partnerProgress') partnerProgress.value = normalizeArray(result.value)
        if (key === 'reviewQueue') reviewQueue.value = normalizeArray(result.value)
        if (key === 'blockers') blockers.value = normalizeArray(result.value)
      } else {
        usingMock.value = true
        console.warn(`[mock fallback] dashboard.${key} 실패`, result.reason)
      }
    })

    if (usingMock.value) {
      const mock = buildMockDashboard()
      if (!summary.value) summary.value = mock.summary
      if (!quarterGoals.value.length) quarterGoals.value = mock.quarterGoals
      if (!partnerProgress.value.length) partnerProgress.value = mock.partnerProgress
      if (!reviewQueue.value.length) reviewQueue.value = mock.reviewQueue
      if (!blockers.value.length) blockers.value = mock.blockers
    }

    loading.value = false
  }

  return {
    summary,
    quarterGoals,
    partnerProgress,
    reviewQueue,
    blockers,
    loading,
    errorMessage,
    usingMock,
    loadAll,
  }
})

function normalizeArray(value) {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.items)) return value.items
  return []
}

function buildMockDashboard() {
  return {
    summary: {
      title: '전사 캠페인 진행',
      subtitle: '24개 캠페인 진행 중 · 이번 주 +6.2%p 향상',
      progressPct: 73,
      trend: 6.2,
      trendSpark: [62, 64, 66, 68, 69, 71, 73],
      miniStats: [
        { label: '검수 패스율', value: '87%' },
        { label: '매칭 평균', value: '76점' },
        { label: '자산 LIVE', value: '108' },
      ],
      activeCampaignCount: 24,
      myCampaignCount: 12,
    },
    quarterGoals: [
      { label: '신규 협력사', current: 12, target: 20, percent: 60, tone: 'low' },
      { label: '캠페인 런칭', current: 8, target: 12, percent: 67, tone: 'mid' },
      { label: 'KPI 달성률', current: 73, target: 85, percent: 86, tone: 'mid' },
      { label: '매칭 평균', current: 76, target: 80, percent: 95, tone: 'high' },
    ],
    partnerProgress: [
      { name: '한화호텔앤드리조트', progress: 94, sub: '8 캠페인 / 21 자산', initial: '한', cls: 'actor-violet' },
      { name: '한화생명', progress: 89, sub: '6 캠페인 / 17 자산', initial: '생', cls: 'actor-blue' },
      { name: '한화이글스', progress: 82, sub: '5 캠페인 / 14 자산', initial: '이', cls: 'actor-rose' },
      { name: '한화시스템', progress: 76, sub: '4 캠페인 / 11 자산', initial: '시', cls: 'actor-emerald' },
      { name: '한화토탈에너지스', progress: 71, sub: '3 캠페인 / 9 자산', initial: '토', cls: 'actor-amber' },
      { name: '한화갤러리아', progress: 64, sub: '4 캠페인 / 14 자산', initial: '갤', cls: 'actor-purple' },
      { name: '외부 대행사', progress: 58, sub: '2 캠페인 / 4 자산', initial: '외', cls: 'actor-rose' },
    ],
    reviewQueue: [
      { count: 3, slaHours: 14, slaTotalHours: 72 },
    ],
    blockers: [
      { count: 2, kind: 'PARTNER_LOW' },
    ],
  }
}
