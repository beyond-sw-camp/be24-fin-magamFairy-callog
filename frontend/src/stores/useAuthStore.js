import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginRequest, logoutRequest } from '@/authApi'
import {
  clearStoredAuth,
  persistAccessToken,
  persistUserInfo,
  readStoredToken,
  readStoredUser,
} from '@/authStorage'

function decodeJwtPayload(accessToken) {
  if (!accessToken) {
    return null
  }

  try {
    const [, payload = ''] = accessToken.split('.')
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const paddedPayload = normalizedPayload.padEnd(
      Math.ceil(normalizedPayload.length / 4) * 4,
      '=',
    )
    const decodedPayload = atob(paddedPayload)
    const percentEncoded = decodedPayload
      .split('')
      .map((character) => `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`)
      .join('')

    return JSON.parse(decodeURIComponent(percentEncoded))
  } catch {
    return null
  }
}

function isTokenExpired(accessToken) {
  const payload = decodeJwtPayload(accessToken)

  return typeof payload?.exp === 'number' && payload.exp * 1000 <= Date.now()
}

function sanitizeDecodedUser(decodedUser) {
  if (!decodedUser || typeof decodedUser !== 'object') {
    return null
  }

  const {
    aud,
    exp,
    iat,
    iss,
    jti,
    nbf,
    sub,
    ...remainingClaims
  } = decodedUser

  return Object.keys(remainingClaims).length > 0
    ? { sub, ...remainingClaims }
    : decodedUser
}

function normalizeUserInfo(rawUser, accessToken) {
  if (rawUser && typeof rawUser === 'object') {
    return rawUser
  }

  return sanitizeDecodedUser(decodeJwtPayload(accessToken))
}

function normalizeRole(role) {
  return typeof role === 'string' ? role.trim().toUpperCase() : ''
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(null)
  const user = ref(null)
  const isLogin = ref(false)
  const isHydrated = ref(false)
  const isAuthenticated = computed(() => isLogin.value && Boolean(token.value))
  const isAdmin = computed(() => normalizeRole(user.value?.role) === 'ROLE_ADMIN')
  const isManager = computed(() => normalizeRole(user.value?.role) === 'ROLE_MANAGER')
  const canCreateUsers = computed(() => isAdmin.value || isManager.value)

  function applyAuth(accessToken, rawUser = null) {
    if (!accessToken) {
      throw new Error('로그인 응답에서 access token을 찾지 못했습니다.')
    }

    token.value = accessToken
    user.value = normalizeUserInfo(rawUser, accessToken)
    isLogin.value = true

    persistAccessToken(accessToken)
    persistUserInfo(user.value)
  }

  function clearAuthState() {
    token.value = null
    user.value = null
    isLogin.value = false
    clearStoredAuth()
  }

  function restore() {
    const savedToken = readStoredToken()

    if (!savedToken || isTokenExpired(savedToken)) {
      clearAuthState()
      isHydrated.value = true
      return false
    }

    token.value = savedToken
    user.value = readStoredUser() ?? normalizeUserInfo(null, savedToken)
    isLogin.value = true
    isHydrated.value = true

    persistUserInfo(user.value)

    return true
  }

  async function login(credentialsOrToken) {
    try {
      if (typeof credentialsOrToken === 'string') {
        applyAuth(credentialsOrToken)
        isHydrated.value = true
        return user.value
      }

      const id = credentialsOrToken?.id?.trim?.()
      const password = credentialsOrToken?.password

      const loginResult = await loginRequest({
        id,
        password,
      })

      applyAuth(loginResult.accessToken, loginResult)
      isHydrated.value = true

      return user.value
    } catch (error) {
      console.warn('Login request failed.', error)
      clearAuthState()
      isHydrated.value = true

      throw new Error('로그인에 실패했습니다. 아이디와 비밀번호를 확인해 주세요.')
    }
  }

  function setToken(newAccessToken, rawUser = null) {
    applyAuth(newAccessToken, rawUser)
    isHydrated.value = true
  }

  async function fetchMe() {
    if (!token.value) {
      return null
    }

    const nextUser = user.value ?? normalizeUserInfo(null, token.value)

    user.value = nextUser
    persistUserInfo(nextUser)

    return nextUser
  }

  async function logout() {
    try {
      await logoutRequest()
    } catch (error) {
      console.warn('Logout request failed.', error)
    } finally {
      clearAuthState()
      isHydrated.value = true
    }
  }


  return {
    isAuthenticated,
    isAdmin,
    isManager,
    canCreateUsers,
    isHydrated,
    isLogin,
    user,
    token,
    login,
    setToken,
    restore,
    checkLogin: restore,
    fetchMe,
    logout,
    clearAuthState,
  }
})
