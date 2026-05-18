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

  /**
   * ⚡ B4: 단일 통합 endpoint + ListCampaign 1번 = 2회 호출.
   * 이전: summary/quarter-goals/partner-progress/asset-categories + campaigns = 5회.
   * Backend 에서 user/scope/visibleCampaigns 가 1번만 계산되어 약 5배 효율.
   */
  async function loadAll(period) {
    loading.value = true
    errorMessage.value = null
    currentPeriod.value = period ?? null
    Object.keys(status.value).forEach((k) => { status.value[k] = 'loading' })

    const [pageRes, campaignsRes] = await Promise.allSettled([
      dashApi.GetDashboardPage(period),
      ListCampaign({ scope: 'mine' }),
    ])

    if (pageRes.status === 'fulfilled') {
      const page = pageRes.value ?? {}
      summary.value = page.summary ?? null
      quarterGoals.value = normalizeArray(page.quarterGoals)
      partnerProgress.value = normalizeArray(page.partnerProgress)
      assetCategories.value = page.assetCategories ?? {}
      status.value.summary = page.summary ? 'success' : 'empty'
      status.value.quarterGoals = isEmptyResult('quarterGoals', quarterGoals.value) ? 'empty' : 'success'
      status.value.partnerProgress = isEmptyResult('partnerProgress', partnerProgress.value) ? 'empty' : 'success'
      status.value.assetCategories = isEmptyResult('assetCategories', assetCategories.value) ? 'empty' : 'success'
    } else {
      console.warn('[dashboard] page load 실패', pageRes.reason)
      status.value.summary = 'error'
      status.value.quarterGoals = 'error'
      status.value.partnerProgress = 'error'
      status.value.assetCategories = 'error'
    }

    if (campaignsRes.status === 'fulfilled') {
      myCampaigns.value = normalizeArray(campaignsRes.value)
      status.value.myCampaigns = isEmptyResult('myCampaigns', myCampaigns.value) ? 'empty' : 'success'
    } else {
      status.value.myCampaigns = 'error'
      console.warn('[dashboard] myCampaigns 실패', campaignsRes.reason)
    }

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
