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

export const ListCampaign = async (filters = {}) => {
  return unwrapResponse(await api.get('/campaigns', { params: filters }))
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

export const InvitePartners = async (campaignId, partners, role = 'PARTNER') => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/partners/invitations`, { partners, role }))
}

export const ListInvitations = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/invitations`))
}

export const AcceptInvitation = async (campaignId, invitationId) => {
  return unwrapResponse(await api.patch(`/campaigns/${campaignId}/invitations/${invitationId}/accept`))
}

export const RejectInvitation = async (campaignId, invitationId, message = '') => {
  return unwrapResponse(
    await api.patch(`/campaigns/${campaignId}/invitations/${invitationId}/reject`, { message }),
  )
}

export const CancelInvitation = async (campaignId, invitationId) => {
  return unwrapResponse(await api.delete(`/campaigns/${campaignId}/invitations/${invitationId}`))
}

export const ListParticipants = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/participants`))
}

export const UpdateParticipantRole = async (campaignId, participantId, role) => {
  return unwrapResponse(
    await api.patch(`/campaigns/${campaignId}/participants/${participantId}/role`, { role }),
  )
}

export const RequestApproval = async (campaignId, payload) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/approval-requests`, payload))
}

export const ListApprovals = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/approval-requests`))
}

export const ApproveApproval = async (campaignId, approvalId, comment = '') => {
  return unwrapResponse(
    await api.patch(`/campaigns/${campaignId}/approval-requests/${approvalId}/approve`, { comment }),
  )
}

export const RejectApproval = async (campaignId, approvalId, comment = '') => {
  return unwrapResponse(
    await api.patch(`/campaigns/${campaignId}/approval-requests/${approvalId}/reject`, { comment }),
  )
}

export const ListComments = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/comments`))
}

export const AddComment = async (campaignId, payload) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/comments`, payload))
}

export const ResolveComment = async (campaignId, commentId) => {
  return unwrapResponse(await api.patch(`/campaigns/${campaignId}/comments/${commentId}/resolve`))
}

export const ListVersions = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/versions`))
}

export const GetAnalytics = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/analytics`))
}

export const UpdateAnalytics = async (campaignId, payload) => {
  return unwrapResponse(await api.put(`/campaigns/${campaignId}/analytics`, payload))
}

export const CloneCampaign = async (campaignId, payload) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/clone`, payload))
}

export const ListContents = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/contents`))
}

export const AddContent = async (campaignId, payload) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/contents`, payload))
}

export default {
  ListCampaign,
  CreateCampaign,
  UpdateCampaign,
  UpdateCampaignStatus,
  InvitePartners,
  ListInvitations,
  AcceptInvitation,
  RejectInvitation,
  CancelInvitation,
  ListParticipants,
  UpdateParticipantRole,
  RequestApproval,
  ListApprovals,
  ApproveApproval,
  RejectApproval,
  ListComments,
  AddComment,
  ResolveComment,
  ListVersions,
  GetAnalytics,
  UpdateAnalytics,
  CloneCampaign,
  ListContents,
  AddContent,
}
