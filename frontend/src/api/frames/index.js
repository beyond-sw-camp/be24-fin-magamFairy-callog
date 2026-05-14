import api from '/plugins/interceptor'

// ==========================================
// 운영 프레임 관련 API 엔드포인트 명세
// - Vue 컴포넌트에서는 백엔드 주소를 직접 적지 않고 이 함수들만 호출합니다.
// ==========================================

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
    throw new Error('프레임 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? '프레임 요청이 실패했습니다.')
  }

  return payload.data
}

export const listFrames = async () => {
  return unwrapResponse(await api.get('/frames/list'))
}

export const getFrame = async (frameId) => {
  return unwrapResponse(await api.get(`/frames/detail/${frameId}`))
}

export const createFrame = async (payload) => {
  return unwrapResponse(await api.post('/frames/create', payload))
}

export const updateFrame = async (frameId, payload) => {
  return unwrapResponse(await api.put(`/frames/update/${frameId}`, payload))
}

export const deleteFrame = async (frameId) => {
  return unwrapResponse(await api.delete(`/frames/delete/${frameId}`))
}

/**
 * 1. AI 운영 프레임 초안 생성 요청
 * @param {Object} params - { prompt: string, format: string, campaignPurpose?: string, targetAudience?: string, channel?: string }
 */
export const generateAIFrame = (params) => {
  return api.post('/ai/frames/generate', params)
}

/**
 * 2. AI 운영 프레임 문맥 교정 요청
 * @param {Object} params - { content: string, removedKey?: string, frameField?: string }
 */
export const refineAIFrame = (params) => {
  return api.post('/ai/frames/refine', params)
}

/**
 * 3. 운영 프레임 초안 저장 요청
 * @param {Object} params - { workType, frameData, requiredFields, reviewSteps, submissionRules, clientRequest }
 */
export const saveFrameDraft = (params) => {
  return api.post('/frames/draft', params)
}

export default {
  listFrames,
  getFrame,
  createFrame,
  updateFrame,
  deleteFrame,
  saveFrameDraft,
  refineAIFrame,
  generateAIFrame,
}
