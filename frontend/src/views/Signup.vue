<script setup>
import { reactive, ref } from 'vue';

const fixedCompany = 'CALLOG';
const isSubmitting = ref(false);
const showResultModal = ref(false);
const copySuccess = ref(false);
const resultData = ref({ id: '', password: '' });

const newUser = reactive({
  teamCode: 'team1',
  name: '',
});

const generateId = () => `${fixedCompany}_${newUser.teamCode.trim()}_${newUser.name.trim()}`;

const generatePassword = () => {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$';
  return Array.from({ length: 12 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
};

const handleCreateUser = async () => {
  if (!newUser.teamCode || !newUser.name) return;

  isSubmitting.value = true;
  await new Promise((resolve) => setTimeout(resolve, 700));

  resultData.value = {
    id: generateId(),
    password: generatePassword(),
  };

  isSubmitting.value = false;
  showResultModal.value = true;
};

const resetForm = () => {
  newUser.teamCode = 'team1';
  newUser.name = '';
  showResultModal.value = false;
  copySuccess.value = false;
};

const copyAllToClipboard = async () => {
  const textToCopy = `계정 정보\n아이디: ${resultData.value.id}\n비밀번호: ${resultData.value.password}`;

  try {
    await navigator.clipboard.writeText(textToCopy);
    copySuccess.value = true;
    window.setTimeout(() => {
      copySuccess.value = false;
    }, 2000);
  } catch (error) {
    copySuccess.value = false;
  }
};
</script>

<template>
  <section class="signup-view min-h-screen px-6 py-10">
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
          Workspace account provisioning
        </p>
      </div>

      <div class="slide-up w-full max-w-2xl">
        <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm md:p-8">
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Account setup</p>
            <h2 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">새 계정 발급</h2>
            <p class="mt-2 text-sm text-slate-500">
              생성자 권한에 따라 발급 가능한 계정 권한이 달라집니다.
            </p>
          </div>

          <form class="grid gap-4" @submit.prevent="handleCreateUser">
            <label class="grid gap-2">
              <span class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">팀 코드</span>
              <input
                v-model="newUser.teamCode"
                type="text"
                required
                placeholder="team1"
                class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
              />
            </label>

            <label class="grid gap-2">
              <span class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">이름</span>
              <input
                v-model="newUser.name"
                type="text"
                required
                placeholder="홍길동"
                class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
              />
            </label>

            <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
              발급 아이디 형식: <strong class="font-semibold text-slate-900">{{ fixedCompany }}_{{ newUser.teamCode || 'team1' }}_{{ newUser.name || '홍길동' }}</strong>
            </div>

            <button
              type="submit"
              :disabled="isSubmitting"
              class="mt-2 inline-flex items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-3.5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
            >
              <template v-if="!isSubmitting">
                <span>계정 생성</span>
              </template>
              <template v-else>
                <span class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white"></span>
                <span>생성 중</span>
              </template>
            </button>
          </form>
        </div>
      </div>
    </div>

    <div v-if="showResultModal" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm">
      <div class="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
        <div class="border-b border-slate-200 pb-5">
          <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Provisioning complete</p>
          <h3 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">계정 발급 완료</h3>
          <p class="mt-2 text-sm text-slate-500">발급된 아이디와 12자리 난수 비밀번호를 복사해서 전달하세요.</p>
        </div>

        <div class="mt-5 grid gap-3">
          <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">ID</p>
            <strong class="mt-2 block break-all font-mono text-sm font-semibold text-slate-900">{{ resultData.id }}</strong>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Password</p>
            <strong class="mt-2 block break-all font-mono text-sm font-semibold text-slate-900">{{ resultData.password }}</strong>
          </div>
        </div>

        <div class="mt-5 grid gap-2">
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold transition"
            :class="copySuccess ? 'border border-emerald-200 bg-emerald-50 text-emerald-700' : 'bg-slate-900 text-white hover:bg-slate-800'"
            @click="copyAllToClipboard"
          >
            <span>{{ copySuccess ? '아이디와 비밀번호가 복사되었습니다' : '아이디와 비밀번호 복사' }}</span>
          </button>
          <button
            type="button"
            class="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            @click="resetForm"
          >
            닫기
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.signup-view {
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
