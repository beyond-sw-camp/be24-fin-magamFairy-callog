import api from '/plugins/interceptor' // axios 대신 세팅된 api 인스턴스 사용

const BASE_URL = '/references' // 인터셉터에 baseURL이 있다면 뒷부분만

// 레퍼런스 목록 조회
export const getReferences = async (params) => {
  return await api.get(BASE_URL, { params }) // axios.get -> api.get
}

// 단일 레퍼런스 상세 조회
export const getReferenceById = async (id) => {
  return await api.get(`${BASE_URL}/${id}`)
}

// 새 레퍼런스 등록
export const createReference = async (data) => {
  return await api.post(BASE_URL, data)
}

// 레퍼런스 수정
export const updateReference = async (id, data) => {
  return await api.put(`${BASE_URL}/${id}`, data)
}

// 레퍼런스 삭제
export const deleteReference = async (id) => {
  return await api.delete(`${BASE_URL}/${id}`)
}