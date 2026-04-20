// interceptor.js에서 뼈대를 잡아둔 공통 axios 인스턴스(api)를 가져옵니다.
import api from '/plugins/interceptor'

// ==========================================
// 템플릿 관련 API 엔드포인트(길목) 명세
// - Vue 컴포넌트에서는 백엔드 주소를 직접 적지 않고 이 함수들만 호출합니다.
// ==========================================

/**
 * 1. AI 템플릿 생성 요청
 * @param {Object} params - { prompt: string, format: string }
 */
export const generateAITemplate = (params) => {
  return api.post('/ai/generate', params)
}

/**
 * 2. AI 문맥 교정 요청 (변수 삭제 시)
 * @param {Object} params - { content: string, removedKey: string }
 */
export const refineAITemplate = (params) => {
  return api.post('/ai/refine', params)
}

/**
 * 3. 템플릿 초안 저장 요청
 * @param {Object} params - { format, templateData, inputValues, clientRequest }
 */
export const saveTemplateDraft = (params) => {
  return api.post('/templates/draft', params)
}

export default { saveTemplateDraft, refineAITemplate, generateAITemplate }
