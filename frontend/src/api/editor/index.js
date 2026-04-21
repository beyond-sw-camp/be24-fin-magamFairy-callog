import api from '/plugins/interceptor.js'

export const saveEditorContent = async (body) => {
  const response = await api.patch('/editor/edit', body)
  return response.data
}

export default {
  saveEditorContent,
}
