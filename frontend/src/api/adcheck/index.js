import api from '/plugins/interceptor.js'

const AD_CHECK_TIMEOUT_MS = 180000
const AD_CHECK_JOB_TIMEOUT_MS = 60000

function createAdCheckError(message, data) {
  const error = new Error(message)
  error.data = data
  return error
}

function unwrapResponse(response) {
  const payload = response?.data
  if (!payload) throw new Error('응답이 비어있습니다.')
  if (!payload.isSuccess && !payload.success) {
    throw createAdCheckError(
      typeof payload?.data === 'string'
        ? payload.data
        : payload?.data?.errorMessage ?? payload?.message ?? 'AI 검수 요청에 실패했습니다.',
      payload?.data,
    )
  }
  return payload.data
}

function toAdCheckError(error) {
  const payload = error?.response?.data
  return createAdCheckError(
    (typeof payload?.data === 'string' ? payload.data : null) ??
    payload?.data?.errorMessage ??
    payload?.message ??
    payload?.error ??
    error?.message ??
    'AI 검수 요청에 실패했습니다.',
    payload?.data ?? error?.data,
  )
}

export const CheckAdCopy = async (copy) => {
  try {
    return unwrapResponse(
      await api.post('/ad/check', { copy }, { timeout: AD_CHECK_TIMEOUT_MS }),
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
      }),
    )
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const CheckAdFileWithAiJudge = async (file, options = {}) => {
  const formData = new FormData()
  formData.append('file', file)
  if (options.campaignId) {
    formData.append('campaignId', options.campaignId)
  }
  try {
    return unwrapResponse(
      await api.post('/ad/check/file/aijudge', formData, {
        timeout: AD_CHECK_TIMEOUT_MS,
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    )
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const CreateAdCheckJob = async (file, options = {}) => {
  const formData = new FormData()
  formData.append('file', file)
  if (options.campaignId) {
    formData.append('campaignId', options.campaignId)
  }
  try {
    return unwrapResponse(
      await api.post('/ad/check/jobs', formData, {
        timeout: AD_CHECK_JOB_TIMEOUT_MS,
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    )
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const GetAdCheckJob = async (jobId) => {
  try {
    return unwrapResponse(await api.get(`/ad/check/jobs/${jobId}`))
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const ListActiveAdCheckJobs = async () => {
  try {
    return unwrapResponse(await api.get('/ad/check/jobs/active'))
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const CancelAdCheckJob = async (jobId) => {
  try {
    return unwrapResponse(await api.post(`/ad/check/jobs/${jobId}/cancel`))
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const ListAdReviewRequests = async (campaignId) => {
  try {
    return unwrapResponse(await api.get(`/campaigns/${campaignId}/ad-review-requests`))
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const CreateAdReviewRequest = async (campaignId, payload) => {
  try {
    return unwrapResponse(await api.post(`/campaigns/${campaignId}/ad-review-requests`, payload))
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const ApproveAdReviewRequest = async (campaignId, requestId, payload = {}) => {
  try {
    return unwrapResponse(
      await api.patch(`/campaigns/${campaignId}/ad-review-requests/${requestId}/approve`, payload),
    )
  } catch (error) {
    throw toAdCheckError(error)
  }
}

export const RejectAdReviewRequest = async (campaignId, requestId, payload = {}) => {
  try {
    return unwrapResponse(
      await api.patch(`/campaigns/${campaignId}/ad-review-requests/${requestId}/reject`, payload),
    )
  } catch (error) {
    throw toAdCheckError(error)
  }
}
