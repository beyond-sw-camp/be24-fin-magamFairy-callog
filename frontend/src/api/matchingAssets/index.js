import api from '/plugins/interceptor.js'

function unwrapResponse(response) {
  const payload = response?.data
  if (!payload) throw new Error('자산 응답이 비어있습니다.')

  if (payload.success === false || payload.isSuccess === false) {
    throw new Error(payload.message ?? '자산 요청에 실패했습니다.')
  }

  return payload.data ?? payload
}

export const ListAssets = async () => {
  return unwrapResponse(
    await api.get('/matching/asset/list', {
      params: { page: 0, size: 10 },
    }),
  )
}

export const CreateAsset = async (payload) => {
  return unwrapResponse(await api.post('/matching/asset/add', payload))
}


export default {
  ListAssets,
  CreateAsset,
}
