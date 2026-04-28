import api from '/plugins/interceptor.js'

export const getCampagin = async () => {
  return res = await api.get(`/campagins/list`)
}

export default {
  getCampagin
}