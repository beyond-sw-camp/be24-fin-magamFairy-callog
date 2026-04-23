const logEditorAction = (action, payload) => {
  console.log(`[editorApi] ${action}`, payload)
}

export const saveEditorContent = async ({ contentId = null, mode = 'update', ...body } = {}) => {
  logEditorAction('saveEditorContent', { contentId, mode, body })

  return {
    id: contentId ?? null,
    contentId: contentId ?? null,
    mode,
    ...body,
  }
}

export const loadContent = async (contentId) => {
  logEditorAction('loadContent', { contentId })
  return null
}

export const deleteContent = async (contentId) => {
  logEditorAction('deleteContent', { contentId })
  return null
}

export const listContent = async () => {
  logEditorAction('listContent', { route: '/calendar' })
  return null
}

export default {
  saveEditorContent,
  loadContent,
  deleteContent,
  listContent,
}
