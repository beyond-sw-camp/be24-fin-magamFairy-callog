import api from '/plugins/interceptor.js'

export const getSettings = async () => {
  return api.get('/settings')
}

export const updateSettings = async (body) => {
  return api.patch('/settings', body)
}

export default {
  getSettings,
  updateSettings
}
