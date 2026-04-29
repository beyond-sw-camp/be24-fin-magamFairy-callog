import api from '/plugins/interceptor.js'

export const getMyProfile = async () => {
  return api.get('/user-profiles/me')
}

export const updateMyProfile = async (body) => {
  return api.patch('/user-profiles/me', body)
}

export default {
  getMyProfile,
  updateMyProfile,
}
