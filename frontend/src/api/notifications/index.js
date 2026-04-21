import api from '/plugins/interceptor.js'

export const getNoti = async (count) => {
  return res = await api.get(`/notifications/list?count=${count}`)
}

export const sendNoti = async (body) => {
  return res = await api.patch("/notifications/", body)
}

export default {
  getNoti,
  sendNoti
}