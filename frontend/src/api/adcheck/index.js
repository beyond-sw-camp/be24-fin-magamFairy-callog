import api from '/plugins/interceptor.js'

const AD_CHECK_TIMEOUT_MS = 180000

function unwrapResponse(response) {
  const payload = response?.data
  if (!payload) throw new Error('응답이 비어있습니다.')
  if (!payload.isSuccess && !payload.success) {
    throw new Error(
      typeof payload?.data === 'string'
        ? payload.data
        : payload?.message ?? 'AI 검수 요청이 실패했습니다.',
    )
  }
  return payload.data
}

function toAdCheckError(error) {
  const payload = error?.response?.data
  return new Error(
    (typeof payload?.data === 'string' ? payload.data : null) ??
    payload?.message ??
    payload?.error ??
    error?.message ??
    'AI 검수 요청이 실패했습니다.',
  )
}

export const CheckAdCopy = async (copy) => {
  try {
    return unwrapResponse(
      await api.post('/ad/check', { copy }, { timeout: AD_CHECK_TIMEOUT_MS })
    )
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const CheckAdFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    return unwrapResponse(
      await api.post('/ad/check/file', formData, {
        timeout: AD_CHECK_TIMEOUT_MS,
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    )
  } catch (error) {
    throw toAdCheckError(error)
  }
}
