import api from '/plugins/interceptor'

// ==========================================
// 운영 프레임 관련 API 엔드포인트 명세
// - Vue 컴포넌트에서는 백엔드 주소를 직접 적지 않고 이 함수들만 호출합니다.
// ==========================================

const LOCAL_FRAME_STORAGE_KEY = 'callog-local-campaign-frames'

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

function shouldUseLocalFallback(error) {
  const status = error?.response?.status
  return !error?.response || status >= 500
}

function readLocalFrames() {
  if (typeof window === 'undefined') {
    return []
  }

  try {
    const parsed = JSON.parse(window.localStorage.getItem(LOCAL_FRAME_STORAGE_KEY) ?? '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writeLocalFrames(frames) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(LOCAL_FRAME_STORAGE_KEY, JSON.stringify(frames))
}

function asList(value) {
  return Array.isArray(value) ? value.filter(Boolean) : []
}

function normalizeLocalFrame(payload = {}, existing = {}) {
  const performance = payload.performance ?? existing.performance ?? {}
  const id =
    payload.id ??
    existing.id ??
    `local-frame-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

  return {
    ...existing,
    ...payload,
    idx: payload.idx ?? existing.idx ?? Date.now(),
    id,
    category: payload.category ?? existing.category ?? '공통',
    version: payload.version ?? existing.version ?? 'v1.0',
    title: payload.title ?? existing.title ?? '새 프레임',
    score: payload.score ?? existing.score ?? 0,
    status: payload.status ?? existing.status ?? 'draft',
    overview: payload.overview ?? existing.overview ?? '',
    required_fields: asList(payload.required_fields ?? payload.requiredFields ?? existing.required_fields),
    banned_expressions: asList(payload.banned_expressions ?? payload.bannedExpressions ?? existing.banned_expressions),
    recommended_expressions: asList(
      payload.recommended_expressions ?? payload.recommendedExpressions ?? existing.recommended_expressions,
    ),
    tone_guide: payload.tone_guide ?? payload.toneGuide ?? existing.tone_guide ?? '',
    approval_process: asList(payload.approval_process ?? payload.approvalProcess ?? existing.approval_process),
    preview: payload.preview ?? existing.preview,
    performance: {
      usage_count: performance.usage_count ?? performance.usageCount ?? 0,
      pass_rate: performance.pass_rate ?? performance.passRate ?? payload.score ?? existing.score ?? 0,
      avg_revisions: performance.avg_revisions ?? performance.avgRevisions ?? 0,
    },
    localOnly: true,
  }
}

export const listFrames = async () => {
  try {
    return unwrapResponse(await api.get('/frames/list'))
  } catch (error) {
    if (shouldUseLocalFallback(error)) {
      return readLocalFrames()
    }

    throw error
  }
}

export const getFrame = async (frameId) => {
  try {
    return unwrapResponse(await api.get(`/frames/detail/${frameId}`))
  } catch (error) {
    const frame = readLocalFrames().find((item) => item.id === frameId)

    if (frame && (shouldUseLocalFallback(error) || error?.response?.status === 404)) {
      return frame
    }

    throw error
  }
}

export const createFrame = async (payload) => {
  try {
    return unwrapResponse(await api.post('/frames/create', payload))
  } catch (error) {
    if (shouldUseLocalFallback(error)) {
      const frames = readLocalFrames()
      const createdFrame = normalizeLocalFrame(payload)
      writeLocalFrames([createdFrame, ...frames.filter((frame) => frame.id !== createdFrame.id)])
      return createdFrame
    }

    throw error
  }
}

export const updateFrame = async (frameId, payload) => {
  try {
    return unwrapResponse(await api.put(`/frames/update/${frameId}`, payload))
  } catch (error) {
    const frames = readLocalFrames()
    const targetFrame = frames.find((frame) => frame.id === frameId)

    if (targetFrame && (shouldUseLocalFallback(error) || error?.response?.status === 404)) {
      const updatedFrame = normalizeLocalFrame(payload, targetFrame)
      writeLocalFrames(frames.map((frame) => (frame.id === frameId ? updatedFrame : frame)))
      return updatedFrame
    }

    throw error
  }
}

export const deleteFrame = async (frameId) => {
  try {
    return unwrapResponse(await api.delete(`/frames/delete/${frameId}`))
  } catch (error) {
    const frames = readLocalFrames()
    const targetFrame = frames.find((frame) => frame.id === frameId)

    if (targetFrame && (shouldUseLocalFallback(error) || error?.response?.status === 404)) {
      writeLocalFrames(frames.filter((frame) => frame.id !== frameId))
      return {
        id: frameId,
        title: targetFrame?.title ?? '',
        deleted: true,
      }
    }

    throw error
  }
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
