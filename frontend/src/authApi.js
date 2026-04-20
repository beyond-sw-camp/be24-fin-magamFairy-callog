import api from '../plugins/interceptor'

export function loginRequest(credentials) {
  return api.post('/auth/login', credentials)
}

export async function fetchMyInfo() {
  const response = await api.get('/auth/me')
  return response.data
}

export async function logoutRequest() {
  const response = await api.post('/auth/logout')
  return response.data
}
