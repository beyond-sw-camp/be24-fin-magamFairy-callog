export const ACCESS_TOKEN_KEY = 'ACCESS_TOKEN'
export const USER_INFO_KEY = 'USERINFO'

function getStorage() {
  if (typeof window === 'undefined') {
    return null
  }

  return window.localStorage
}

export function readStoredToken() {
  return getStorage()?.getItem(ACCESS_TOKEN_KEY) ?? null
}

export function readStoredUser() {
  const rawUser = getStorage()?.getItem(USER_INFO_KEY)

  if (!rawUser || rawUser === 'undefined') {
    return null
  }

  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

export function persistAccessToken(accessToken) {
  const storage = getStorage()

  if (!storage) {
    return
  }

  if (accessToken) {
    storage.setItem(ACCESS_TOKEN_KEY, accessToken)
    return
  }

  storage.removeItem(ACCESS_TOKEN_KEY)
}

export function persistUserInfo(userInfo) {
  const storage = getStorage()

  if (!storage) {
    return
  }

  if (userInfo && typeof userInfo === 'object') {
    storage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
    return
  }

  storage.removeItem(USER_INFO_KEY)
}

export function clearStoredAuth() {
  const storage = getStorage()

  if (!storage) {
    return
  }

  storage.removeItem(ACCESS_TOKEN_KEY)
  storage.removeItem(USER_INFO_KEY)
}
