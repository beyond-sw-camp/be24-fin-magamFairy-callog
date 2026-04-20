import axios from 'axios'
import { clearStoredAuth, readStoredToken } from '../src/authStorage'

const LOGIN_PATH = '/user/login'

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
  (error) => {
    const status = error?.response?.status
    const requestUrl = error?.config?.url ?? ''
    const isAuthRequest =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/logout') ||
      requestUrl.includes('/auth/refresh')

    if (status === 401 && !isAuthRequest) {
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
