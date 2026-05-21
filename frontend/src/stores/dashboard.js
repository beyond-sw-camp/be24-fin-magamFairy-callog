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

  /* ─── 대시보드 재설계 (Zone 1~4) ─── */
  const recentActivity = ref([])        // Zone1 P1우 [{ idx, campaignId, campaignName, type, description, actorName, createdAt }]
  const campaignPipeline = ref([])      // Zone4 P1 [{ stage, count }]
  const campaignProgress = ref([])      // Zone2 [{ campaignId, campaignName, color, isMine, completionPct }]
  const reviewQueue = ref([])           // Zone1 P1좌 (Task REVIEW 기반)
  const adReviewQueue = ref({ toReview: [], mine: [] })  // Zone3 P2 — { 검수목록(toReview), 검수결과(mine) }
  const blockers = ref([])              // Zone1 P1좌 (마감초과/반려검수)

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
    recentActivity: 'loading',
    campaignPipeline: 'loading',
    campaignProgress: 'loading',
    reviewQueue: 'loading',
    adReviewQueue: 'loading',
    blockers: 'loading',
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

    // 재설계 Zone 데이터는 메인 로드를 막지 않도록 비차단(detached) 로드.
    // 각 zone 은 자기 status 만 갱신하며 실패 시 해당 zone 만 빈/에러 상태.
    loadZoneExtras()
  }

  /**
   * Zone1~4 신규 엔드포인트 일괄 best-effort 로드.
   * 개별 실패는 해당 zone status='error' (→ View 에서 빈 상태) 로 격리, 화면 크래시 없음.
   */
  async function loadZoneExtras() {
    const tasks = [
      ['recentActivity', dashApi.GetRecentActivity, recentActivity],
      ['campaignPipeline', dashApi.GetCampaignPipeline, campaignPipeline],
      ['campaignProgress', dashApi.GetCampaignProgress, campaignProgress],
      ['reviewQueue', dashApi.GetReviewQueue, reviewQueue],
      ['blockers', dashApi.GetBlockers, blockers],
    ]
    await Promise.all(tasks.map(async ([key, fn, target]) => {
      try {
        const data = normalizeArray(await fn())
        target.value = data
        status.value[key] = isEmptyResult(key, data) ? 'empty' : 'success'
      } catch (e) {
        target.value = []
        status.value[key] = 'error'
        console.warn(`[dashboard] ${key} 실패`, e)
      }
    }))

    // 검수 큐 — { toReview, mine } 객체 형태라 별도 처리
    try {
      const d = await dashApi.GetAdReviewQueue()
      const toReview = normalizeArray(d?.toReview)
      const mine = normalizeArray(d?.mine)
      adReviewQueue.value = { toReview, mine }
      status.value.adReviewQueue = (toReview.length || mine.length) ? 'success' : 'empty'
    } catch (e) {
      adReviewQueue.value = { toReview: [], mine: [] }
      status.value.adReviewQueue = 'error'
      console.warn('[dashboard] adReviewQueue 실패', e)
    }
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
    recentActivity,
    campaignPipeline,
    campaignProgress,
    reviewQueue,
    adReviewQueue,
    blockers,
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
    loadZoneExtras,
    loadCompare,
    clearCompare,
  }
})

function normalizeArray(value) {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.items)) return value.items
  return []
}
