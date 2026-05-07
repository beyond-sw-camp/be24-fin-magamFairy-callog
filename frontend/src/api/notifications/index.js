import api from '/plugins/interceptor.js'

export const getNoti = async (count) => {
  if (count) {
    return api.get('/notifications/list', {
      params: {
        count,
      },
    })
  }

  return api.get('/notifications/list')
}

export const confirm = async (idx) =>
  api.patch('/notifications/confirm', null, {
    params: {
      idx,
    },
  })

export const sendNoti = async (body) => api.patch('/notifications/', body)

export default {
  getNoti,
  confirm,
  sendNoti,
}
