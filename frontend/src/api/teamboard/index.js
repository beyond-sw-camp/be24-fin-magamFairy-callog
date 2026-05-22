import api from '/plugins/interceptor.js'

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
    throw new Error('팀보드 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? '팀보드 요청이 실패했습니다.')
  }

  return payload.data
}

// ===== Milestones =====
export const ListMilestones = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/milestones`))
}

export const GetMilestone = async (milestoneId) => {
  return unwrapResponse(await api.get(`/milestones/${milestoneId}`))
}

export const CreateMilestone = async (campaignId, payload) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/milestones`, payload))
}

export const UpdateMilestone = async (milestoneId, payload) => {
  return unwrapResponse(await api.put(`/milestones/${milestoneId}`, payload))
}

export const DeleteMilestone = async (milestoneId) => {
  return unwrapResponse(await api.delete(`/milestones/${milestoneId}`))
}

// ===== Task Parts =====
export const ListTaskParts = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/task-parts`))
}

export const GetTaskPart = async (taskPartId) => {
  return unwrapResponse(await api.get(`/task-parts/${taskPartId}`))
}

export const CreateTaskPart = async (campaignId, milestoneId, payload) => {
  return unwrapResponse(
    await api.post(`/campaigns/${campaignId}/task-parts`, payload, {
      params: { milestoneId },
    }),
  )
}

export const UpdateTaskPart = async (taskPartId, milestoneId, payload) => {
  return unwrapResponse(
    await api.put(`/task-parts/${taskPartId}`, payload, {
      params: milestoneId ? { milestoneId } : undefined,
    }),
  )
}

export const DeleteTaskPart = async (taskPartId) => {
  return unwrapResponse(await api.delete(`/task-parts/${taskPartId}`))
}

// ===== Tasks =====
export const ListAllTasks = async () => {
  return unwrapResponse(await api.get('/tasks'))
}

export const ListTasksByCampaign = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/tasks`))
}

export const GetTask = async (taskId) => {
  return unwrapResponse(await api.get(`/tasks/${taskId}`))
}

export const CreateTask = async (campaignId, payload) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/tasks`, payload))
}

// 개인 업무 생성 (캠페인 없이, 담당=본인)
export const CreatePersonalTask = async (payload) => {
  return unwrapResponse(await api.post('/tasks', payload))
}

export const UpdateTask = async (taskId, payload) => {
  return unwrapResponse(await api.put(`/tasks/${taskId}`, payload))
}

// ===== 캘린더 가져오기/내보내기 (.ics) =====
// 내보내기 — Blob 반환 (호출 측에서 다운로드 처리)
export const ExportCalendarIcs = async ({ from, to, types }) => {
  const res = await api.get('/calendar/export', {
    params: { from, to, types: Array.isArray(types) ? types.join(',') : types },
    responseType: 'blob',
    timeout: 30000,
  })
  return res.data
}

// 가져오기 — .ics 파일 업로드 (mode: 'overwrite' | 'append')
// Content-Type은 지정하지 않음 → axios가 multipart boundary를 자동 설정.
export const ImportCalendarIcs = async (file, mode) => {
  const fd = new FormData()
  fd.append('file', file)
  return unwrapResponse(await api.post('/calendar/import', fd, {
    params: { mode },
    timeout: 30000,
  }))
}

export const DeleteTask = async (taskId) => {
  return unwrapResponse(await api.delete(`/tasks/${taskId}`))
}

export default {
  ListMilestones,
  GetMilestone,
  CreateMilestone,
  UpdateMilestone,
  DeleteMilestone,
  ListTaskParts,
  GetTaskPart,
  CreateTaskPart,
  UpdateTaskPart,
  DeleteTaskPart,
  ListAllTasks,
  ListTasksByCampaign,
  GetTask,
  CreateTask,
  CreatePersonalTask,
  UpdateTask,
  DeleteTask,
}
