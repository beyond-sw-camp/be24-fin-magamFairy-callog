import api from '/plugins/interceptor.js'

function isSuccessResponse(payload) {
  return (
    payload?.success === true ||
    payload?.success === 'true' ||
    payload?.isSuccess === true ||
    payload?.isSuccess === 'true'
  )
}

function unwrapResponse(response) {
  const payload = response?.data

  if (!payload) {
    throw new Error('대시보드 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? '대시보드 요청이 실패했습니다.')
  }

  return payload.data
}

/**
 * ⚡ B4: Dashboard 페이지 통합 호출.
 */
export const GetDashboardPage = async (period) => {
  const params = period ? { period } : {}
  return unwrapResponse(await api.get('/dashboard', { params }))
}

export const GetDashboardSummary = async (period) => {
  const params = period ? { period } : {}
  return unwrapResponse(await api.get('/dashboard/summary', { params }))
}

export const GetQuarterGoals = async (period) => {
  return unwrapResponse(await api.get('/dashboard/quarter-goals', { params: { period } }))
}

export const GetPartnerProgress = async (period) => {
  const params = period ? { period } : {}
  return unwrapResponse(await api.get('/dashboard/partner-progress', { params }))
}

export const GetAssetCategories = async (period) => {
  const params = period ? { period } : {}
  return unwrapResponse(await api.get('/dashboard/asset-categories', { params }))
}

/* ─── 대시보드 재설계 (Zone 1~4) 신규 엔드포인트 ─── */

/** Zone1 P1우 · 최근 활동 피드 → [{ idx, campaignId, campaignName, type, description, actorName, createdAt }] */
export const GetRecentActivity = async () => {
  return unwrapResponse(await api.get('/dashboard/recent-activity'))
}

/** Zone4 P1 · 캠페인 파이프라인(상태별 count) → [{ stage, count }] */
export const GetCampaignPipeline = async () => {
  return unwrapResponse(await api.get('/dashboard/campaign-pipeline'))
}

/** Zone2 · 캠페인 진척률 랭킹 → [{ campaignId, campaignName, color, isMine, completionPct }] (정렬 완료) */
export const GetCampaignProgress = async () => {
  return unwrapResponse(await api.get('/dashboard/campaign-progress'))
}

/** Zone4 P2 · 매출(REVENUE KPI) 월별 추이 → [{ label, value }] */
export const GetRevenueTrend = async () => {
  return unwrapResponse(await api.get('/dashboard/revenue-trend'))
}

/** Zone1 P1좌 / Zone3 P2 · 검수 대기/목록 → [{ ... }] */
export const GetReviewQueue = async () => {
  return unwrapResponse(await api.get('/dashboard/review-queue'))
}

/** Zone1 P1좌 · 차단/미배정 → [{ ... }] */
export const GetBlockers = async () => {
  return unwrapResponse(await api.get('/dashboard/blockers'))
}

export default {
  GetDashboardPage,
  GetDashboardSummary,
  GetQuarterGoals,
  GetPartnerProgress,
  GetAssetCategories,
  GetRecentActivity,
  GetCampaignPipeline,
  GetCampaignProgress,
  GetRevenueTrend,
  GetReviewQueue,
  GetBlockers,
}
