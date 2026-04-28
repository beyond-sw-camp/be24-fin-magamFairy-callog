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
    throw new Error('캠페인 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? '캠페인 요청이 실패했습니다.')
  }

  return payload.data
}

export const ListCampaign = async () => {
  return unwrapResponse(await api.get('/campaigns'))
}

export const CreateCampaign = async (payload) => {
  return unwrapResponse(await api.post('/campaigns/new', payload))
}

export const UpdateCampaign = async (campaignId, payload) => {
  return unwrapResponse(await api.put(`/campaigns/${campaignId}`, payload))
}

export const UpdateCampaignStatus = async (campaignId, status) => {
  return unwrapResponse(await api.patch(`/campaigns/${campaignId}/status`, { status }))
}

export const InvitePartners = async (campaignId, partners) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/partners/invitations`, { partners }))
}

export default {
  ListCampaign,
  CreateCampaign,
  UpdateCampaign,
  UpdateCampaignStatus,
  InvitePartners,
}
