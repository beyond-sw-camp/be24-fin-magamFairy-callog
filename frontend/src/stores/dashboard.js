import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as dashApi from '@/api/dashboard'
import { ListCampaign } from '@/api/campaigns'

export const useDashboardStore = defineStore('dashboard', () => {
  const summary = ref(null)
  const quarterGoals = ref([])
  const partnerProgress = ref([])
  const assetCategories = ref({})       // { "EVENT": 42, ... }
  const myCampaigns = ref([])           // [{ idx, name, status, color, ... }]
  const loading = ref(false)
  const errorMessage = ref(null)
  const currentPeriod = ref(null)
  const comparePeriod = ref(null)

  /* ─── 비교 모드 데이터 (이전 기간 스냅샷) ─── */
  const compareSummary = ref(null)
  const compareQuarterGoals = ref([])
  const comparePartnerProgress = ref([])
  const compareLoading = ref(false)

  /**
   * 영역별 로드 상태: 'loading' | 'success' | 'empty' | 'error'
   * View 레이어에서 각 카드 영역의 skeleton·페이드인·에러 표시 분기에 사용.
   */
  const status = ref({
    summary: 'loading',
    quarterGoals: 'loading',
    partnerProgress: 'loading',
    assetCategories: 'loading',
    myCampaigns: 'loading',
  })

  function isEmptyResult(key, value) {
    if (value == null) return true
    if (key === 'summary') return false   // summary 는 객체 자체가 의미
    if (Array.isArray(value)) return value.length === 0
    if (typeof value === 'object') return Object.keys(value).length === 0
    return false
  }

  async function loadAll(period) {
    loading.value = true
    errorMessage.value = null
    currentPeriod.value = period ?? null
    Object.keys(status.value).forEach((k) => { status.value[k] = 'loading' })

    const tasks = [
      ['summary', () => dashApi.GetDashboardSummary(period)],
      ['quarterGoals', () => dashApi.GetQuarterGoals(period)],
      ['partnerProgress', () => dashApi.GetPartnerProgress(period)],
      ['assetCategories', () => dashApi.GetAssetCategories(period)],
      ['myCampaigns', () => ListCampaign({ scope: 'mine' })],
    ]

    const results = await Promise.allSettled(tasks.map(([, fn]) => fn()))

    results.forEach((result, idx) => {
      const [key] = tasks[idx]
      if (result.status === 'fulfilled') {
        const v = result.value
        if (key === 'summary') summary.value = v
        if (key === 'quarterGoals') quarterGoals.value = normalizeArray(v)
        if (key === 'partnerProgress') partnerProgress.value = normalizeArray(v)
        if (key === 'assetCategories') assetCategories.value = v ?? {}
        if (key === 'myCampaigns') myCampaigns.value = normalizeArray(v)
        status.value[key] = isEmptyResult(key, v) ? 'empty' : 'success'
      } else {
        status.value[key] = 'error'
        console.warn(`[dashboard] ${key} 실패`, result.reason)
      }
    })

    loading.value = false
  }

  /**
   * 비교 기간 스냅샷 로드. KPI delta · 차트 overlay · 제휴사 랭킹 변화 계산용.
   * summary / quarterGoals / partnerProgress 만 필요 (도넛·캠페인은 비교 없음).
   * 실패 시에도 메인 화면을 깨뜨리지 않도록 조용히 무시 (compare* 값은 null/[]).
   */
  async function loadCompare(period) {
    if (!period) {
      clearCompare()
      return
    }
    compareLoading.value = true
    comparePeriod.value = period

    const [summaryRes, goalsRes, partnerRes] = await Promise.allSettled([
      dashApi.GetDashboardSummary(period),
      dashApi.GetQuarterGoals(period),
      dashApi.GetPartnerProgress(period),
    ])
    compareSummary.value = summaryRes.status === 'fulfilled' ? summaryRes.value : null
    compareQuarterGoals.value = goalsRes.status === 'fulfilled' ? normalizeArray(goalsRes.value) : []
    comparePartnerProgress.value = partnerRes.status === 'fulfilled' ? normalizeArray(partnerRes.value) : []
    compareLoading.value = false
  }

  function clearCompare() {
    compareSummary.value = null
    compareQuarterGoals.value = []
    comparePartnerProgress.value = []
    comparePeriod.value = null
    compareLoading.value = false
  }

  return {
    summary,
    quarterGoals,
    partnerProgress,
    assetCategories,
    myCampaigns,
    loading,
    errorMessage,
    status,
    currentPeriod,
    comparePeriod,
    compareSummary,
    compareQuarterGoals,
    comparePartnerProgress,
    compareLoading,
    loadAll,
    loadCompare,
    clearCompare,
  }
})

function normalizeArray(value) {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.items)) return value.items
  return []
}
