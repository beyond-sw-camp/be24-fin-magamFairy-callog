import api from '/plugins/interceptor'

const base = (campaignId) => `/campaigns/${campaignId}/members`

export const getCampaignMembers = (campaignId) => api.get(base(campaignId))
export const getTeamCandidates = (campaignId) => api.get(`${base(campaignId)}/candidates/team`)
export const getPartnerGmCandidates = (campaignId) => api.get(`${base(campaignId)}/candidates/partner-gm`)
export const addTeamMembers = (campaignId, userIdxList) =>
  api.post(base(campaignId), { userIdxList })
export const invitePartnerGm = (campaignId, userIdx) =>
  api.post(`${base(campaignId)}/invite-partner`, { userIdx })
export const updateMemberRole = (campaignId, memberId, campaignRole) =>
  api.patch(`${base(campaignId)}/${memberId}`, { campaignRole })
export const removeMember = (campaignId, memberId) =>
  api.delete(`${base(campaignId)}/${memberId}`)
