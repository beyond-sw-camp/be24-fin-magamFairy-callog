import api from '/plugins/interceptor'

// ==========================================
// 운영 프레임 관련 API 엔드포인트 명세
// - Vue 컴포넌트에서는 백엔드 주소를 직접 적지 않고 이 함수들만 호출합니다.
// ==========================================

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

export default { saveFrameDraft, refineAIFrame, generateAIFrame }
