<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const form = reactive({
  loginId: '',
  password: '',
});

const isLoading = ref(false);

const handleLogin = async () => {
  if (!form.loginId || !form.password) return;

  isLoading.value = true;

  try {
    await new Promise((resolve) => setTimeout(resolve, 1500));
    router.push('/dashboard');
  } catch (error) {
    alert('Unable to sign in. Please try again.');
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <section class="login-view min-h-screen px-6 py-10">
    <div class="mx-auto flex min-h-[calc(100vh-5rem)] max-w-5xl flex-col items-center justify-center">
      <div class="mb-10 flex flex-col items-center fade-in">
        <div class="mb-4 inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-900 text-white shadow-sm">
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
        <p class="mt-2 text-center text-sm font-medium text-slate-500">
          Shared scheduling workspace for operational teams.
        </p>
      </div>

      <div class="slide-up w-full max-w-3xl">
        <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm md:p-8">
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Access</p>
            <h2 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">Sign in</h2>
            <p class="mt-2 text-sm text-slate-500">
              Continue to your CALLOG workspace.
            </p>
          </div>

          <form @submit.prevent="handleLogin" class="flex flex-col items-stretch gap-4 md:flex-row md:gap-5">
            <div class="flex-1 space-y-4">
              <label class="block space-y-1.5">
                <span class="ml-1 block text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">아이디</span>
                <span class="relative block">
                  <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
                    <svg class="h-4.5 w-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <rect x="2" y="4" width="20" height="16" rx="2" stroke-width="2" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
                    </svg>
                  </span>
                  <input
                    v-model="form.loginId"
                    type="text"
                    required
                    placeholder="예: CALLOG_team1_홍길동"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 py-4 pl-12 pr-4 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </span>
              </label>

              <label class="block space-y-1.5">
                <span class="ml-1 block text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">비밀번호</span>
                <span class="relative block">
                  <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
                    <svg class="h-4.5 w-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <rect x="3" y="11" width="18" height="10" rx="2" stroke-width="2" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                  </span>
                  <input
                    v-model="form.password"
                    type="password"
                    required
                    placeholder="12자리 임시 비밀번호"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 py-4 pl-12 pr-4 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </span>
              </label>
            </div>

            <div class="md:flex md:w-36">
              <button
                type="submit"
                :disabled="isLoading"
                class="flex min-h-[120px] w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-4 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70 md:min-h-0 md:h-full md:flex-col"
              >
                <template v-if="!isLoading">
                  <span class="inline-flex h-10 w-10 items-center justify-center rounded-full bg-white/10">
                    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12h14m-7-7 7 7-7 7" />
                    </svg>
                  </span>
                  <span>Log in</span>
                </template>
                <template v-else>
                  <span class="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white"></span>
                  <span>Signing in</span>
                </template>
              </button>
            </div>
          </form>
        </div>

        <div class="mt-8 flex flex-wrap items-center justify-center gap-6 text-sm text-slate-500 fade-in delay-200">
          <div class="flex items-center gap-2">
            <strong class="text-slate-900">120%</strong>
            <span class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Efficiency up</span>
          </div>
          <div class="h-1.5 w-1.5 rounded-full bg-slate-300"></div>
          <div class="flex items-center gap-2">
            <strong class="text-slate-900">ESG</strong>
            <span class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Certified flow</span>
          </div>
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

.delay-200 {
  animation-delay: 0.2s;
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
