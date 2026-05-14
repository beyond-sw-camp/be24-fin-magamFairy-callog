import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 확인 다이얼로그 store — Promise<boolean> 반환.
 *   const ok = await confirm.ask({ title, message, confirmText, cancelText, tone })
 *   if (ok) { ... }
 */
export const useConfirmStore = defineStore('confirm', () => {
  const open = ref(false)
  const title = ref('')
  const message = ref('')
  const confirmText = ref('네')
  const cancelText = ref('아니오')
  const tone = ref('primary')  // 'primary' | 'danger' | 'warn'

  let resolver = null

  function ask(opts = {}) {
    title.value = opts.title || '확인'
    message.value = opts.message || '진행하시겠습니까?'
    confirmText.value = opts.confirmText || '네'
    cancelText.value = opts.cancelText || '아니오'
    tone.value = opts.tone || 'primary'
    open.value = true
    return new Promise((resolve) => { resolver = resolve })
  }

  function close(result) {
    open.value = false
    if (resolver) {
      resolver(!!result)
      resolver = null
    }
  }

  return { open, title, message, confirmText, cancelText, tone, ask, close }
})
