<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  loginId: '',
  password: '',
})

const isLoading = ref(false)
const errorMessage = ref('')

function getRedirectTarget() {
  const redirect = Array.isArray(route.query.redirect)
    ? route.query.redirect[0]
    : route.query.redirect

  return typeof redirect === 'string' && redirect.startsWith('/')
    ? redirect
    : '/dashboard'
}

const handleLogin = async () => {
  if (isLoading.value) {
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    await authStore.login({
      loginId: form.loginId,
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
        <div
          class="mb-4 inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-sm"
        >
          <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M8 2v4M16 2v4M3 10h18M5 6h14a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2Z"
            />
          </svg>
        </div>
        <h1 class="text-4xl font-bold tracking-tight text-slate-900">CALLOG</h1>
      </div>

      <div class="slide-up w-full max-w-3xl">
        <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm md:p-8">
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
              Access
            </p>
            <h2 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">Sign in</h2>
            <p class="mt-2 text-sm text-slate-500">Continue to your CALLOG workspace.</p>
          </div>

          <p
            v-if="errorMessage"
            class="mb-4 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600"
          >
            {{ errorMessage }}
          </p>

          <form
            class="flex flex-col items-stretch gap-4 md:flex-row md:gap-5"
            @submit.prevent="handleLogin"
          >
            <div class="flex-1 space-y-4">
              <label class="block space-y-1.5">
                <span
                  class="ml-1 block text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400"
                >
                  Login ID
                </span>
                <span class="relative block">
                  <span
                    class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                  >
                    <svg
                      class="h-4.5 w-4.5"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
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
                    v-model="form.loginId"
                    type="text"
                    autocomplete="username"
                    placeholder="예: CALLOG_team1_admin"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 py-4 pl-12 pr-4 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </span>
              </label>

              <label class="block space-y-1.5">
                <span
                  class="ml-1 block text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400"
                >
                  Password
                </span>
                <span class="relative block">
                  <span
                    class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400"
                  >
                    <svg
                      class="h-4.5 w-4.5"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
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
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 py-4 pl-12 pr-4 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </span>
              </label>
            </div>

            <div class="md:flex md:w-36">
              <button
                type="submit"
                :disabled="isLoading"
                class="flex min-h-[120px] w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-4 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70 md:h-full md:min-h-0 md:flex-col"
              >
                <template v-if="!isLoading">
                  <span
                    class="inline-flex h-10 w-10 items-center justify-center rounded-full bg-white/10"
                  >
                    <svg
                      class="h-5 w-5"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
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
                  <span
                    class="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white"
                  ></span>
                  <span>Signing in</span>
                </template>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.login-view {
  background:
    linear-gradient(180deg, rgba(233, 243, 255, 0.95) 0%, rgba(244, 248, 255, 0.88) 26%, rgba(248, 250, 252, 0.92) 52%, #f8fafc 100%),
    radial-gradient(circle at top left, rgba(47, 128, 237, 0.16), transparent 34%),
    radial-gradient(circle at top right, rgba(124, 77, 255, 0.06), transparent 24%),
    var(--app-bg);
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
