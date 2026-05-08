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
    throw new Error('OrganizationKpi 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? 'OrganizationKpi 요청이 실패했습니다.')
  }

  return payload.data
}

export const ListOrganizationKpis = async (params = {}) => {
  return unwrapResponse(await api.get('/organization-kpis', { params }))
}

export const GetOrganizationKpi = async (id) => {
  return unwrapResponse(await api.get(`/organization-kpis/${id}`))
}

export const CreateOrganizationKpi = async (payload) => {
  return unwrapResponse(await api.post('/organization-kpis', payload))
}

export const UpdateOrganizationKpi = async (id, payload) => {
  return unwrapResponse(await api.patch(`/organization-kpis/${id}`, payload))
}

export const UpdateOrganizationKpiStatus = async (id, status) => {
  return unwrapResponse(await api.patch(`/organization-kpis/${id}/status`, { status }))
}

export const ListParentKpiCandidates = async (orgId) => {
  return unwrapResponse(
    await api.get('/organization-kpis/parents', { params: { orgId } }),
  )
}

export default {
  ListOrganizationKpis,
  GetOrganizationKpi,
  CreateOrganizationKpi,
  UpdateOrganizationKpi,
  UpdateOrganizationKpiStatus,
  ListParentKpiCandidates,
}
