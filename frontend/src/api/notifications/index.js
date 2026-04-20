import api from '/plugins/interceptor.js'

export const getNoti = async (count) => {
  return res = await api.get(`/notifications/list?count=${count}`)
}

export const updateSettings = async (body) => {
  return res = await api.patch("/update", body)
}

export default {
  getNoti,
  updateSettings
}