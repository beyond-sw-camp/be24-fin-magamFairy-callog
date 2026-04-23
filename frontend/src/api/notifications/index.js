import api from '/plugins/interceptor.js'

export const getNoti = async (count) => {
  if(count){
    return res = await api.get(`/notifications/list?count=${count}`)
  }
  else{
    return res = await api.get(`/notifications/list`)
  }
}

export const confirm = async (idx) => {
  return res = await api.patch(`/notifications/confirm?idx=${idx}`)
}

export const sendNoti = async (body) => {
  return res = await api.patch("/notifications/", body)
}

export default {
  getNoti,
  confirm,
  sendNoti
}