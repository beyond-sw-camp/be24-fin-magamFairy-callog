import api from '/plugins/interceptor.js'

export const createCampaignRequest = (payload) => {
  return api.post('/campaigns', payload)
}

export const inviteCampaignPartnersRequest = (campaignId, partners) => {
  return api.post(`/campaigns/${campaignId}/partners/invitations`, { partners })
}

export default {
  createCampaignRequest,
  inviteCampaignPartnersRequest,
}
