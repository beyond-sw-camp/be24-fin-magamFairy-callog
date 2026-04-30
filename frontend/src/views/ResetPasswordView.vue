<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { resetPasswordRequest } from '@/authApi'

const router = useRouter()

const id = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')
const result = ref(null)
const copySuccess = ref(false)

async function handleReset() {
  if (isSubmitting.value) return

  const trimmedId = id.value.trim()
  if (!trimmedId) {
    errorMessage.value = '아이디를 입력해 주세요.'
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    result.value = await resetPasswordRequest({ id: trimmedId })
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message ||
      error?.message ||
      '비밀번호 재발급에 실패했습니다. 아이디를 확인해 주세요.'
  } finally {
    isSubmitting.value = false
  }
}

async function copyPassword() {
  try {
    await navigator.clipboard.writeText(result.value.password)
    copySuccess.value = true
    window.setTimeout(() => { copySuccess.value = false }, 2000)
  } catch {
    copySuccess.value = false
  }
}

function goToLogin() {
  router.push({ name: 'login' })
}
</script>

<template>
  <section class="reset-view min-h-screen px-6 py-10">
    <div class="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center">
      <div class="mb-8 flex flex-col items-center text-center">
        <div class="reset-mark mb-4 inline-flex h-16 w-16 items-center justify-center">
          <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15 7a2 2 0 0 1 2 2m4 0a6 6 0 0 1-7.74 5.74L11 17H9v2H7v2H4a1 1 0 0 1-1-1v-2.59c0-.26.1-.52.29-.7L11.26 9.74A6 6 0 0 1 21 9Z" />
          </svg>
        </div>
        <h1 class="reset-title text-4xl font-bold tracking-tight">CALLOG</h1>
        <p class="reset-copy mt-3 text-sm">비밀번호 재발급</p>
      </div>

      <div class="w-full max-w-md">
        <article class="reset-card p-6 shadow-sm md:p-8">
          <template v-if="!result">
            <div class="mb-6">
              <p class="reset-eyebrow text-xs font-semibold uppercase tracking-[0.24em]">
                Password reset
              </p>
              <h2 class="reset-heading mt-2 text-2xl font-bold tracking-tight">
                임시 비밀번호 발급
              </h2>
              <p class="reset-copy mt-2 text-sm">
                아이디를 입력하면 새 임시 비밀번호가 발급됩니다.
              </p>
            </div>

            <p
              v-if="errorMessage"
              class="reset-alert reset-alert--danger mb-4 px-4 py-3 text-sm font-medium"
            >
              {{ errorMessage }}
            </p>

            <form class="flex flex-col gap-4" @submit.prevent="handleReset">
              <label class="reset-field">
                <span>아이디</span>
                <input
                  v-model="id"
                  type="text"
                  autocomplete="username"
                  placeholder="예: CALLOG_마케팅팀_홍길동"
                />
              </label>

              <div class="reset-actions">
                <RouterLink :to="{ name: 'login' }" class="reset-link">
                  로그인으로 돌아가기
                </RouterLink>
                <button type="submit" :disabled="isSubmitting" class="reset-submit">
                  <span v-if="!isSubmitting">임시 비밀번호 발급</span>
                  <span v-else>처리 중</span>
                </button>
              </div>
            </form>
          </template>

          <template v-else>
            <div class="reset-success">
              <p class="reset-eyebrow text-xs font-semibold uppercase tracking-[0.24em]">
                Password issued
              </p>
              <h2 class="reset-heading mt-2 text-2xl font-bold tracking-tight">
                임시 비밀번호가 발급되었습니다
              </h2>
              <p class="reset-copy mt-2 text-sm">
                아래 임시 비밀번호로 로그인 후 변경해 주세요.
              </p>

              <dl class="reset-result">
                <div>
                  <dt>아이디</dt>
                  <dd>{{ result.id }}</dd>
                </div>
                <div>
                  <dt>임시 비밀번호</dt>
                  <dd>{{ result.password }}</dd>
                </div>
              </dl>

              <div class="flex flex-col gap-2">
                <button
                  type="button"
                  class="reset-copy-button"
                  :class="copySuccess ? 'is-success' : 'is-primary'"
                  @click="copyPassword"
                >
                  {{ copySuccess ? '복사되었습니다' : '비밀번호 복사' }}
                </button>
                <button type="button" class="reset-link-button" @click="goToLogin">
                  로그인하러 가기
                </button>
              </div>
            </div>
          </template>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.reset-view {
  background: var(--surface-page);
  color: var(--text-body);
  transition: background var(--transition-normal), color var(--transition-normal);
}

.reset-mark {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--accent-strong);
  color: var(--toggle-thumb);
}

.reset-title,
.reset-heading {
  color: var(--text-heading);
}

.reset-card {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-soft);
}

.reset-eyebrow,
.reset-copy,
.reset-field span,
.reset-result dt {
  color: var(--text-muted);
}

.reset-alert {
  border-radius: var(--radius-md);
}

.reset-alert--danger {
  border: 1px solid color-mix(in srgb, var(--danger-color) 30%, var(--line-soft));
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.reset-field {
  display: grid;
  gap: 8px;
}

.reset-field span {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.reset-field input {
  width: 100%;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 500;
  outline: none;
  padding: 14px 16px;
  transition: background var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.reset-field input::placeholder {
  color: var(--text-muted);
}

.reset-field input:focus {
  border-color: var(--line-strong);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent-color) 14%, transparent);
}

.reset-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 4px;
}

.reset-link,
.reset-submit,
.reset-copy-button,
.reset-link-button {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  padding: 0 18px;
  text-decoration: none;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast), color var(--transition-fast);
}

.reset-link {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-heading);
}

.reset-link:hover {
  border-color: var(--line-strong);
  background: var(--control-focus-color);
  color: var(--accent-color);
}

.reset-submit {
  border: 0;
  background: var(--accent-strong);
  color: var(--toggle-thumb);
}

.reset-submit:hover:not(:disabled) {
  background: var(--accent-color);
}

.reset-submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.reset-success {
  display: grid;
  gap: 18px;
}

.reset-result {
  display: grid;
  gap: 10px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  padding: 16px;
}

.reset-result div {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  gap: 12px;
}

.reset-result dt {
  font-size: 13px;
  font-weight: 700;
}

.reset-result dd {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  overflow-wrap: anywhere;
  font-family: monospace;
}

.reset-copy-button.is-primary {
  width: 100%;
  border: 0;
  background: var(--accent-strong);
  color: var(--toggle-thumb);
}

.reset-copy-button.is-primary:hover {
  background: var(--accent-color);
}

.reset-copy-button.is-success {
  width: 100%;
  border: 1px solid color-mix(in srgb, var(--success-color) 38%, transparent);
  background: color-mix(in srgb, var(--success-color) 14%, var(--surface-card));
  color: var(--success-color);
}

.reset-link-button {
  width: 100%;
  border: 1px solid var(--line-soft);
  background: var(--surface-card);
  color: var(--text-body);
}

.reset-link-button:hover {
  background: var(--surface-control);
}

@media (max-width: 480px) {
  .reset-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .reset-link,
  .reset-submit {
    width: 100%;
  }

  .reset-result div {
    grid-template-columns: 1fr;
  }
}
</style>
