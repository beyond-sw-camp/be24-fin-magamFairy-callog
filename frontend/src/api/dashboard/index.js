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

export default {
  GetDashboardPage,
  GetDashboardSummary,
  GetQuarterGoals,
  GetPartnerProgress,
  GetAssetCategories,
}
