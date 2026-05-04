import api from '/plugins/interceptor'

const BASE_URL = '/references'

export const getReferences = async (params) => {
  return await api.get(`${BASE_URL}/list`, { params })
}

export const getReferenceById = async (id) => {
  return await api.get(`${BASE_URL}/detail/${id}`)
}

export const createReference = async (data) => {
  return await api.post(`${BASE_URL}/create`, data)
}

export const updateReference = async (id, data) => {
  return await api.put(`${BASE_URL}/update/${id}`, data)
}

export const deleteReference = async (id) => {
  return await api.delete(`${BASE_URL}/delete/${id}`)
}
