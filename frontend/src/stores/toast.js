import { defineStore } from 'pinia'
import { ref } from 'vue'

let _id = 0

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

  function push({ type = 'info', title = '', message = '', duration = 3500 }) {
    const id = ++_id
    toasts.value.push({ id, type, title, message })
    if (duration > 0) {
      setTimeout(() => dismiss(id), duration)
    }
    return id
  }

  function dismiss(id) {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx >= 0) toasts.value.splice(idx, 1)
  }

  const success = (msg, title) => push({ type: 'success', message: msg, title })
  const error   = (msg, title) => push({ type: 'error',   message: msg, title, duration: 5000 })
  const info    = (msg, title) => push({ type: 'info',    message: msg, title })
  const warn    = (msg, title) => push({ type: 'warn',    message: msg, title })

  return { toasts, push, dismiss, success, error, info, warn }
})
