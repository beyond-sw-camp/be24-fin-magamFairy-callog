import api, { reissueAccessToken } from '../plugins/interceptor'

function isSuccessResponse(payload) {
  return (
    payload?.success === true ||
    payload?.success === 'true' ||
    payload?.isSuccess === true ||
    payload?.isSuccess === 'true'
  )
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
  const payload = response?.data ?? {}
  const responseBody = payload?.data && typeof payload.data === 'object' ? payload.data : payload
  const authorization = response?.headers?.authorization ?? response?.headers?.Authorization
  const headerAccessToken = authorization?.startsWith('Bearer ') ? authorization.slice(7) : null

  return {
    ...responseBody,
    accessToken: responseBody.accessToken ?? headerAccessToken,
  }
}

export async function fetchMyInfo() {
  const response = await api.get('/auth/me')
  return unwrapResponse(response)
}

export async function reissueRequest() {
  const accessToken = await reissueAccessToken()

  return {
    accessToken,
  }
}

export async function logoutRequest() {
  const response = await api.post('/auth/logout')
  return response.data
}

export async function createUserRequest(payload) {
  const response = await api.post('/auth/usercreate', payload)
  return unwrapResponse(response)
}

export async function SignupRequest(payload) {
  const response = await api.post('/auth/signup', payload)
  return unwrapResponse(response)
}

export async function resetPasswordRequest(payload) {
  const response = await api.post('/auth/resetpassword', payload)
  return unwrapResponse(response)
}

export async function changePasswordRequest(payload) {
  const response = await api.patch('/auth/password', payload)
  return unwrapResponse(response)
}

export async function deleteUserRequest(payload) {
  const response = await api.post('/auth/userdelete', payload)
  return unwrapResponse(response)
}
