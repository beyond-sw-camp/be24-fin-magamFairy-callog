import api from '/plugins/interceptor.js'

export const getNoti = (count) => {
  if (count) {
    return api.get('/notifications/list', {
      params: {
        count,
      },
    })
  }

  return api.get('/notifications/list')
}

export const confirm = (idx) =>
  api.patch('/notifications/confirm', null, {
    params: {
      idx,
    },
  })

export const confirmAll = () => api.patch('/notifications/confirm-all')

export const getNotificationSettings = () => api.get('/notifications/settings')

export const updateNotificationSettings = (body) => api.patch('/notifications/settings', body)

export const getNotificationAdminPolicies = () => api.get('/notifications/admin-policies')

export const updateNotificationAdminPolicies = (body) => api.patch('/notifications/admin-policies', body)

export const sendNoti = (body) => api.post('/notifications', body)

export default {
  getNoti,
  confirm,
  confirmAll,
  getNotificationSettings,
  updateNotificationSettings,
  getNotificationAdminPolicies,
  updateNotificationAdminPolicies,
  sendNoti,
}
