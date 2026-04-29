import axios from 'axios'
import { clearStoredAuth, persistAccessToken, readStoredToken } from '../src/authStorage'

const LOGIN_PATH = '/user/login'
export const AUTH_TOKEN_REFRESHED_EVENT = 'auth:token-refreshed'
let refreshPromise = null

function buildLoginRedirectUrl() {
  if (typeof window === 'undefined') {
    return LOGIN_PATH
  }

  const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`

  if (!currentPath || currentPath.startsWith(LOGIN_PATH)) {
    return LOGIN_PATH
  }

  return `${LOGIN_PATH}?redirect=${encodeURIComponent(currentPath)}`
}

const api = axios.create({
  baseURL: '/api',
  timeout: 5000,
  withCredentials: true,
})

function extractAccessToken(response) {
  const authorization = response?.headers?.authorization ?? response?.headers?.Authorization

  if (authorization?.startsWith('Bearer ')) {
    return authorization.slice(7)
  }

  return response?.data?.accessToken ?? response?.data?.data?.accessToken ?? null
}

function notifyAccessTokenRefreshed(accessToken) {
  if (typeof window === 'undefined') {
    return
  }

  window.dispatchEvent(
    new CustomEvent(AUTH_TOKEN_REFRESHED_EVENT, {
      detail: {
        accessToken,
      },
    }),
  )
}

export async function reissueAccessToken() {
  if (!refreshPromise) {
    refreshPromise = api
      .post('/auth/reissue', null, {
        skipAuthRefresh: true,
      })
      .then((response) => {
        const accessToken = extractAccessToken(response)

        if (!accessToken) {
          throw new Error('Access token was not returned from reissue response.')
        }

        persistAccessToken(accessToken)
        notifyAccessTokenRefreshed(accessToken)
        return accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

api.interceptors.request.use(
  (config) => {
    const accessToken = readStoredToken()

    if (accessToken) {
      config.headers = config.headers ?? {}
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status
    const originalRequest = error?.config ?? {}
    const requestUrl = error?.config?.url ?? ''
    const shouldSkipRefresh = Boolean(originalRequest.skipAuthRefresh)
    const isAuthRequest =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/logout') ||
      requestUrl.includes('/auth/reissue')

    if (status === 401 && !shouldSkipRefresh && !isAuthRequest && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const accessToken = await reissueAccessToken()

        originalRequest.headers = originalRequest.headers ?? {}
        originalRequest.headers.Authorization = `Bearer ${accessToken}`

        return api(originalRequest)
      } catch {
        clearStoredAuth()

        if (
          typeof window !== 'undefined' &&
          !window.location.pathname.startsWith(LOGIN_PATH)
        ) {
          window.location.replace(buildLoginRedirectUrl())
        }
      }
    } else if (status === 401 && !isAuthRequest) {
      clearStoredAuth()

      if (
        typeof window !== 'undefined' &&
        !window.location.pathname.startsWith(LOGIN_PATH)
      ) {
        window.location.replace(buildLoginRedirectUrl())
      }
    }

    return Promise.reject(error)
  },
)

export const apiFetch = async (url, options = {}) => {
  const method = (options.method || 'GET').toUpperCase()
  const data = options.body
  const headers = options.headers || {}

  const response = await api.request({
    url,
    method,
    data,
    headers,
  })

  return response.data
}

export default api
