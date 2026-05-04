import api from '/plugins/interceptor.js'

function unwrapResponse(response) {
  const payload = response?.data
  if (!payload) throw new Error('목표 응답이 비어 있습니다.')

  if (payload.success === false || payload.isSuccess === false) {
    throw new Error(payload.message ?? '목표 요청에 실패했습니다.')
  }

  return payload.data ?? payload
}

export const CreateGoal = async (payload) => {
  return unwrapResponse(await api.post('/matching/goal/add', payload))
}

export const ListGoals = async () => {
  return unwrapResponse(
    await api.get('/matching/goal/list', {
      params: { page: 0, size: 10 },
    }),
  )
}

export default {
  CreateGoal,
  ListGoals,
}
