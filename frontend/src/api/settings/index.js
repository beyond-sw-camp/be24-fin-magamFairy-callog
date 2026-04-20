import api from '/plugins/interceptor.js'

export const getSettings = async () => {
  return res = await api.get("/settings")
}

export default {
  getSettings
}