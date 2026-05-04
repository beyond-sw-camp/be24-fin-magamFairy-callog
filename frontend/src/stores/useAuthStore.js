import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginRequest, logoutRequest, reissueRequest } from '@/authApi'
import { usePlannerStore } from '@/stores/planner'
import { AUTH_TOKEN_REFRESHED_EVENT } from '../../plugins/interceptor'
import {
  ACCESS_TOKEN_KEY,
  USER_INFO_KEY,
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

const TOKEN_REFRESH_SKEW_MS = 30 * 1000

function getAccessTokenExpiresAt(accessToken) {
  const decodedToken = decodeJwtPayload(accessToken)
  const expiresAtSeconds = decodedToken?.exp

  return typeof expiresAtSeconds === 'number'
    ? expiresAtSeconds * 1000
    : null
}

function isAccessTokenExpired(accessToken, skewMs = TOKEN_REFRESH_SKEW_MS) {
  const expiresAt = getAccessTokenExpiresAt(accessToken)

  if (!expiresAt) {
    return true
  }

  return Date.now() + skewMs >= expiresAt
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(null)
  const user = ref(null)
  const isLogin = ref(false)
  const isHydrated = ref(false)
  const isAuthenticated = computed(() => isLogin.value && Boolean(token.value))
  const isAdmin = computed(() => normalizeRole(user.value?.role) === 'ROLE_ADMIN')
  const isGeneralManager = computed(() => normalizeRole(user.value?.role) === 'ROLE_GENERAL_MANAGER')
  const isManager = computed(() => normalizeRole(user.value?.role) === 'ROLE_MANAGER')
  const canCreateUsers = computed(() => isAdmin.value || isGeneralManager.value || isManager.value)
  const canCreateCampaign = computed(() => {
    const decoded = decodeJwtPayload(token.value)
    return decoded?.orgType !== 'EXTERNAL_PARTNER'
  })

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
    usePlannerStore().resetCampaigns()
  }

  function applyStoredAuth(accessToken) {
    token.value = accessToken
    user.value = readStoredUser() ?? normalizeUserInfo(null, accessToken)
    isLogin.value = true

    persistUserInfo(user.value)
  }

  function restore() {
    const savedToken = readStoredToken()

    if (!savedToken || isAccessTokenExpired(savedToken)) {
      token.value = null
      user.value = null
      isLogin.value = false

      if (savedToken) {
        persistAccessToken(null)
      }

      isHydrated.value = true
      return false
    }

    applyStoredAuth(savedToken)
    isHydrated.value = true

    return true
  }

  async function refreshSession() {
    try {
      const { accessToken } = await reissueRequest()

      applyAuth(accessToken)
      isHydrated.value = true
      return true
    } catch (error) {
      console.warn('Token reissue failed.', error)
      clearAuthState()
      isHydrated.value = true
      return false
    }
  }

  function hasFreshAccessToken() {
    return Boolean(token.value) && !isAccessTokenExpired(token.value)
  }

  async function ensureAuthenticated() {
    if (!isHydrated.value) {
      restore()
    }

    if (hasFreshAccessToken()) {
      return true
    }

    return refreshSession()
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
      void usePlannerStore().loadCampaignsFromServer()

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

  if (typeof window !== 'undefined') {
    window.addEventListener(AUTH_TOKEN_REFRESHED_EVENT, (event) => {
      const accessToken = event?.detail?.accessToken

      if (!accessToken) {
        return
      }

      token.value = accessToken
      user.value = normalizeUserInfo(null, accessToken)
      isLogin.value = true
      isHydrated.value = true

      persistUserInfo(user.value)
    })

    window.addEventListener('storage', (event) => {
      const shouldSync =
        event.key === null ||
        event.key === ACCESS_TOKEN_KEY ||
        event.key === USER_INFO_KEY

      if (!shouldSync) {
        return
      }

      const savedToken = readStoredToken()

      if (!savedToken || isAccessTokenExpired(savedToken, 0)) {
        token.value = null
        user.value = null
        isLogin.value = false
        isHydrated.value = true
        return
      }

      applyStoredAuth(savedToken)
      isHydrated.value = true
    })
  }


  return {
    isAuthenticated,
    isAdmin,
    isGeneralManager,
    isManager,
    canCreateUsers,
    canCreateCampaign,
    isHydrated,
    isLogin,
    user,
    token,
    login,
    setToken,
    restore,
    refreshSession,
    ensureAuthenticated,
    hasFreshAccessToken,
    checkLogin: restore,
    fetchMe,
    logout,
    clearAuthState,
  }
})
