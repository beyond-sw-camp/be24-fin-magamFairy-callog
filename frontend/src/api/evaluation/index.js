import api from '/plugins/interceptor.js'

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
    throw new Error('평가 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? '평가 요청이 실패했습니다.')
  }

  return payload.data
}

export const startEvaluation = async (payload) => {
  return unwrapResponse(await api.post('/evaluation/start', payload))
}

export const getEvaluationResult = async (campaignIdx) => {
  return unwrapResponse(
    await api.get(`/evaluation/result?campaignIdx=${campaignIdx}`)
  )
}

export const collectEvaluation = async (payload) => {
  return unwrapResponse(await api.post('/evaluation/collect', payload))
}

export default {
  startEvaluation,
  getEvaluationResult,
  collectEvaluation,
}
