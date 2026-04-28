import api from '/plugins/interceptor.js'

/**
 * 공통 API 호출 래퍼
 * - 응답의 success 여부 확인
 * - 에러 로깅 및 커스텀 에러 던지기
 */
const apiCall = async (label, request, fallback = undefined) => {
  try {
    const response = await request()
    const baseResponse = response.data

    if (baseResponse?.success === false) {
      const code = baseResponse?.code ?? 'UNKNOWN'
      const message = baseResponse?.message ?? '알 수 없는 오류가 발생했습니다.'
      console.error(`[${label}] 실패 — [${code}] ${message}`)
      const error = new Error(message) 
      error.code = code
      error.baseResponse = baseResponse
      throw error
    }

    // baseResponse.data 혹은 객체 전체를 반환하는 extractBody가 있다고 가정
    return typeof extractBody === 'function' ? extractBody(baseResponse) : baseResponse.data
  } catch (error) {
    if (error.baseResponse) throw error

    const serverData = error.response?.data
    if (serverData?.success === false) {
      const code = serverData?.code ?? error.response?.status ?? 'NETWORK'
      const message = serverData?.message ?? error.message
      console.error(`[${label}] 실패 — [${code}] ${message}`)
      const wrappedError = new Error(message)
      wrappedError.code = code
      wrappedError.baseResponse = serverData
      throw wrappedError
    }

    console.error(`[${label}] 오류 —`, error)
    if (fallback !== undefined) return fallback
    throw error
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 컨텐츠 카드 (Content Card) CRUD
// ─────────────────────────────────────────────────────────────────────────────

const saveEditorContent = async (idx, body) => {
  if (!idx || idx === 'new') {
    // 신규 생성: POST /content
    apiCall('saveEditorContent', () => api.post('/content', body))
  } else {
    // 기존 수정: PATCH /content/123
    apiCall('updateEditorContent', () => api.patch(`/content/${idx}`, body))
  }
}

const loadContent = async (idx) => 
  apiCall('loadContent', () => api.get(`/content/${idx}`))

const deleteContent = async (idx) =>
  apiCall('deleteContent', () => api.patch(`/content/${idx}`))

const listContent = async () =>
  apiCall('listContent', () => api.get(`/content/list`))

// ─────────────────────────────────────────────────────────────────────────────

export default {
  saveEditorContent,
  loadContent,
  deleteContent,
  listContent
}