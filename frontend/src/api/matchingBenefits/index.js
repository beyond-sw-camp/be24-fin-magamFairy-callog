import api from '/plugins/interceptor.js'

function unwrapResponse(response) {
  const payload = response?.data
  if (!payload) throw new Error('혜택 응답이 비어 있습니다.')

  if (payload.success === false || payload.isSuccess === false) {
    throw new Error(payload.message ?? '혜택 요청에 실패했습니다.')
  }

  return payload.data ?? payload
}

export const ListBenefits = async () => {
  return unwrapResponse(
    await api.get('/matching/benefit/list', {
      params: { page: 0, size: 20 },
    }),
  )
}

export const addBenefit = async (body) => {
  return await api.post('/matching/benefit/add', body)
  
}

export default {
  ListBenefits,
  addBenefit
}
