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

export const GetDashboardSummary = async () => {
  return unwrapResponse(await api.get('/dashboard/summary'))
}

export const GetQuarterGoals = async (period) => {
  return unwrapResponse(await api.get('/dashboard/quarter-goals', { params: { period } }))
}

export const GetPartnerProgress = async () => {
  return unwrapResponse(await api.get('/dashboard/partner-progress'))
}

export const GetReviewQueue = async () => {
  return unwrapResponse(await api.get('/dashboard/review-queue'))
}

export const GetBlockers = async () => {
  return unwrapResponse(await api.get('/dashboard/blockers'))
}

export const GetAssetCategories = async () => {
  return unwrapResponse(await api.get('/dashboard/asset-categories'))
}

export const GetKpiCategories = async () => {
  return unwrapResponse(await api.get('/dashboard/kpi-categories'))
}

export default {
  GetDashboardSummary,
  GetQuarterGoals,
  GetPartnerProgress,
  GetReviewQueue,
  GetBlockers,
  GetAssetCategories,
  GetKpiCategories,
}
