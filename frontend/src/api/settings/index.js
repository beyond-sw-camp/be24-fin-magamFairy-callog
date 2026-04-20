import api from '/plugins/interceptor.js'

export const getSettings = async () => {
  return res = await api.get("/settings")
}

export const updateSettings = async (body) => {
  return res = await api.patch("/update", body)
}

export default {
  getSettings,
  updateSettings
}