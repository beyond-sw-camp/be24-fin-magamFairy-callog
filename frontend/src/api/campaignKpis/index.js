import api from '/plugins/interceptor.js'

function unwrap(res) {
  const p = res?.data
  if (!p) throw new Error('응답이 비어있습니다.')
  if (!p.success && !p.isSuccess) throw new Error(p?.message ?? 'KPI 요청이 실패했습니다.')
  return p.data
}

const base = (campaignId) => `/campaigns/${campaignId}/kpis`

export const getKpis = async (campaignId) =>
  unwrap(await api.get(base(campaignId)))

export const createKpi = async (campaignId, payload) =>
  unwrap(await api.post(base(campaignId), payload))

export const updateKpiMeta = async (campaignId, kpiId, payload) =>
  unwrap(await api.patch(`${base(campaignId)}/${kpiId}`, payload))

export const updateKpiActual = async (campaignId, kpiId, payload) =>
  unwrap(await api.patch(`${base(campaignId)}/${kpiId}/actual`, payload))

export const removeKpi = async (campaignId, kpiId) =>
  unwrap(await api.delete(`${base(campaignId)}/${kpiId}`))

export const saveAnalysis = async (campaignId, kpiAnalysis) =>
  unwrap(await api.patch(`${base(campaignId)}/analysis`, { kpiAnalysis }))

export const getFrameworks = async () =>
  unwrap(await api.get('/campaigns/kpis/frameworks'))

export const importFramework = async (campaignId, frameworkKey) =>
  unwrap(await api.post(`${base(campaignId)}/import-framework`, { frameworkKey }))
