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
    throw new Error('KpiTemplate 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? 'KpiTemplate 요청이 실패했습니다.')
  }

  return payload.data
}

export const ListKpiTemplates = async (params = {}) => {
  return unwrapResponse(await api.get('/kpi-templates', { params }))
}

export const CreateKpiTemplate = async (payload) => {
  return unwrapResponse(await api.post('/kpi-templates', payload))
}

export const InstantiateKpiTemplate = async (templateId, payload = {}) => {
  return unwrapResponse(
    await api.post(`/kpi-templates/${templateId}/instantiate`, payload),
  )
}

export default {
  ListKpiTemplates,
  CreateKpiTemplate,
  InstantiateKpiTemplate,
}
