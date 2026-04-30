<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  id: '',
  password: '',
})

const isLoading = ref(false)
const errorMessage = ref('')

function getRedirectTarget() {
  const redirect = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect

  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/dashboard'
}

const handleLogin = async () => {
  if (isLoading.value) {
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    await authStore.login({
      id: form.id,
      password: form.password,
    })

    router.push(getRedirectTarget())
  } catch (error) {
    errorMessage.value =
      error?.message ?? '로그인에 실패했습니다. 아이디와 비밀번호를 확인해 주세요.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <section class="login-view min-h-screen px-6 py-10">
    <div class="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center">
      <div class="mb-10 flex flex-col items-center fade-in">
        <img src="@/assets/callog.png" alt="CALLOG" class="login-brand-icon mb-4" />
        <img src="@/assets/callog2.png" alt="CALLOG" class="login-brand-wordmark" />
      </div>

      <div class="slide-up w-full max-w-3xl">
        <div class="login-card p-6 shadow-sm md:p-8">
          <div class="mb-6">
            <p class="login-eyebrow text-xs font-semibold uppercase tracking-[0.24em]">Access</p>
            <h2 class="login-title mt-2 text-2xl font-bold tracking-tight">Sign in</h2>
            <p class="login-description mt-2 text-sm">Continue to your CALLOG workspace.</p>
          </div>

          <p v-if="errorMessage" class="login-error mb-4 px-4 py-3 text-sm font-medium">
            {{ errorMessage }}
          </p>

          <form
            class="flex flex-col items-stretch gap-4 md:flex-row md:gap-5"
            @submit.prevent="handleLogin"
          >
            <div class="flex-1 space-y-4">
              <label class="block space-y-1.5">
                <span
                  class="login-label ml-1 block text-[11px] font-semibold uppercase tracking-[0.22em]"
                >
                  ID
                </span>
                <span class="relative block">
                  <span
                    class="login-input-icon pointer-events-none absolute left-4 top-1/2 -translate-y-1/2"
                  >
                    <svg class="h-4.5 w-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <rect x="2" y="4" width="20" height="16" rx="2" stroke-width="2" />
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"
                      />
                    </svg>
                  </span>
                  <input
                    v-model="form.id"
                    type="text"
                    autocomplete="username"
                    placeholder="예: CALLOG_team1_admin"
                    class="login-input w-full py-4 pl-12 pr-4 text-sm font-medium outline-none transition"
                  />
                </span>
              </label>

              <label class="block space-y-1.5">
                <span
                  class="login-label ml-1 block text-[11px] font-semibold uppercase tracking-[0.22em]"
                >
                  Password
                </span>
                <span class="relative block">
                  <span
                    class="login-input-icon pointer-events-none absolute left-4 top-1/2 -translate-y-1/2"
                  >
                    <svg class="h-4.5 w-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <rect x="3" y="11" width="18" height="10" rx="2" stroke-width="2" />
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M7 11V7a5 5 0 0 1 10 0v4"
                      />
                    </svg>
                  </span>
                  <input
                    v-model="form.password"
                    type="password"
                    autocomplete="current-password"
                    placeholder="비밀번호를 입력하세요"
                    class="login-input w-full py-4 pl-12 pr-4 text-sm font-medium outline-none transition"
                  />
                </span>
              </label>
            </div>

            <div class="md:flex md:w-36">
              <button
                type="submit"
                :disabled="isLoading"
                class="login-submit flex min-h-[120px] w-full items-center justify-center gap-2 px-4 py-4 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-70 md:h-full md:min-h-0 md:flex-col"
              >
                <template v-if="!isLoading">
                  <span class="login-submit-icon inline-flex h-10 w-10 items-center justify-center">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M5 12h14m-7-7 7 7-7 7"
                      />
                    </svg>
                  </span>
                  <span>Log in</span>
                </template>
                <template v-else>
                  <span class="login-spinner h-5 w-5 animate-spin rounded-full border-2"></span>
                  <span>Signing in</span>
                </template>
              </button>
            </div>
          </form>
        </div>

        <p class="login-signup-hint mt-5 text-center text-sm">
          제휴사 계정이 없으신가요?
          <RouterLink :to="{ name: 'partner-signup' }" class="login-signup-link font-semibold">
            회원가입
          </RouterLink>
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.login-view {
  background: var(--surface-page);
  color: var(--text-body);
  transition:
    background var(--transition-normal),
    color var(--transition-normal);
}

.login-brand-icon {
  display: block;
  margin: 0 auto;
  height: 64px;
  width: auto;
  object-fit: contain;
  transform: translateX(7px);
}

.login-brand-wordmark {
  display: block;
  margin: 0 auto;
  height: 80px;
  width: auto;
  object-fit: contain;
}

.login-title {
  color: var(--text-heading);
  transition: color var(--transition-normal);
}

.login-card {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-body);
  box-shadow: var(--shadow-soft);
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal),
    color var(--transition-normal),
    box-shadow var(--transition-normal);
}

.login-eyebrow,
.login-label,
.login-input-icon {
  color: var(--text-muted);
  transition: color var(--transition-normal);
}

.login-description {
  color: var(--text-muted);
  transition: color var(--transition-normal);
}

.login-error {
  border: 1px solid color-mix(in srgb, var(--danger-color) 30%, var(--line-soft));
  border-radius: var(--radius-md);
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.login-input {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  color: var(--text-heading);
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    color var(--transition-normal);
}

.login-input::placeholder {
  color: var(--text-muted);
}

.login-input:focus {
  border-color: var(--line-strong);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent-color) 14%, transparent);
}

.login-submit {
  border-radius: var(--radius-md);
  background: var(--accent-strong);
  color: var(--toggle-thumb);
}

.login-submit:hover:not(:disabled) {
  background: var(--accent-color);
}

.login-submit-icon {
  border-radius: var(--radius-full);
  background: color-mix(in srgb, var(--toggle-thumb) 14%, transparent);
}

.login-spinner {
  border-color: color-mix(in srgb, var(--toggle-thumb) 28%, transparent);
  border-top-color: var(--toggle-thumb);
}

.login-signup-hint {
  color: var(--text-muted);
}

.login-signup-link {
  color: var(--accent-color);
  text-decoration: none;
}

.login-signup-link:hover {
  text-decoration: underline;
}

.fade-in {
  animation: fadeIn 0.8s ease-out forwards;
}

.slide-up {
  animation: slideUp 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(24px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
