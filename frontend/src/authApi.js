import api from '../plugins/interceptor'

function isSuccessResponse(payload) {
  return payload?.success === true || payload?.success === 'true'
}

function unwrapResponse(response) {
  const payload = response?.data

  if (!payload) {
    throw new Error('응답 데이터가 없습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? '요청에 실패했습니다.')
  }

  return payload.data
}

export async function loginRequest(credentials) {
  const response = await api.post('/auth/login', credentials)
  return unwrapResponse(response)
}

export async function fetchMyInfo() {
  const response = await api.get('/auth/me')
  return unwrapResponse(response)
}

export async function logoutRequest() {
  const response = await api.post('/auth/logout')
  return unwrapResponse(response)
}

export async function createUserRequest(payload) {
  const response = await api.post('/admin/users', payload)
  return unwrapResponse(response)
}
