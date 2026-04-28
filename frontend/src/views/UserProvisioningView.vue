<script setup>
import { computed, reactive, ref } from 'vue'
import { createUserRequest } from '@/authApi'
import { useAuthStore } from '@/stores/useAuthStore'

const fixedCompany = 'CALLOG'
const authStore = useAuthStore()
const isSubmitting = ref(false)
const showResultModal = ref(false)
const copySuccess = ref(false)
const errorMessage = ref('')
const resultData = ref({ id: '', password: '', role: '' })

const roleOptions = computed(() => {
  if (authStore.isAdmin) {
    return [
      { value: 'MANAGER', label: 'MANAGER - 협력사 책임자' },
      { value: 'USER', label: 'USER - 사원' },
    ]
  }

  return [{ value: 'USER', label: 'USER - 사원' }]
})

const newUser = reactive({
  teamCode: 'team1',
  name: '',
  email: '',
  role: authStore.isAdmin ? 'MANAGER' : 'USER',
})

const selectedRoleLabel = computed(
  () => roleOptions.value.find((role) => role.value === newUser.role)?.label ?? 'USER - 사원',
)

function getPreviewId() {
  const teamCode = newUser.teamCode.trim() || 'team1'
  const name = newUser.name.trim() || 'user'
  return `${fixedCompany}_${teamCode}_${name}`
}

function ensureAllowedRole() {
  if (roleOptions.value.some((role) => role.value === newUser.role)) {
    return
  }

  newUser.role = roleOptions.value[0]?.value ?? 'USER'
}

function openResultModal(id, password, role) {
  resultData.value = { id, password, role }
  copySuccess.value = false
  showResultModal.value = true
}

async function handleCreateUser() {
  ensureAllowedRole()

  if (!newUser.teamCode.trim() || !newUser.name.trim() || isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    const payload = {
      teamCode: newUser.teamCode.trim(),
      name: newUser.name.trim(),
      email: newUser.email.trim() || null,
      role: newUser.role,
    }

    const createdUser = await createUserRequest(payload)
    const loginId = createdUser.id ?? createdUser.loginId
    const temporaryPassword = createdUser.password

    if (!loginId || !temporaryPassword) {
      throw new Error('계정 생성 응답에서 아이디 또는 임시 비밀번호를 찾지 못했습니다.')
    }

    openResultModal(loginId, temporaryPassword, createdUser.role ?? newUser.role)
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message ??
      error?.message ??
      '계정 생성에 실패했습니다. 권한과 입력 정보를 확인해 주세요.'
  } finally {
    isSubmitting.value = false
  }
}

function resetForm() {
  newUser.teamCode = 'team1'
  newUser.name = ''
  newUser.email = ''
  newUser.role = authStore.isAdmin ? 'MANAGER' : 'USER'
  showResultModal.value = false
  copySuccess.value = false
  errorMessage.value = ''
}

async function copyAllToClipboard() {
  const textToCopy = `계정 정보\n권한: ${resultData.value.role}\n아이디: ${resultData.value.id}\n비밀번호: ${resultData.value.password}`

  try {
    await navigator.clipboard.writeText(textToCopy)
    copySuccess.value = true
    window.setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch {
    copySuccess.value = false
  }
}
</script>

<template>
  <section class="min-h-[calc(100vh-7rem)] px-6 py-10">
    <div class="mx-auto w-full max-w-3xl">
      <div class="mb-8">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
          Account provisioning
        </p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight text-slate-900">
          계정 발급
        </h1>
        <p class="mt-2 text-sm text-slate-500">
          ADMIN은 MANAGER와 USER를 만들 수 있고, MANAGER는 USER만 만들 수 있습니다.
        </p>
      </div>

      <div class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <p
          v-if="errorMessage"
          class="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600"
        >
          {{ errorMessage }}
        </p>

        <form class="grid gap-4" @submit.prevent="handleCreateUser">
          <label class="grid gap-2">
            <span class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
              Role
            </span>
            <select
              v-model="newUser.role"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
            >
              <option v-for="role in roleOptions" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
          </label>

          <label class="grid gap-2">
            <span class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
              Team code
            </span>
            <input
              v-model="newUser.teamCode"
              type="text"
              required
              placeholder="team1"
              class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
            />
          </label>

          <div class="grid gap-4 md:grid-cols-2">
            <label class="grid gap-2">
              <span class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
                Name
              </span>
              <input
                v-model="newUser.name"
                type="text"
                required
                placeholder="홍길동"
                class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
              />
            </label>

            <label class="grid gap-2">
              <span class="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">
                Email
              </span>
              <input
                v-model="newUser.email"
                type="email"
                placeholder="user@company.com"
                class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3.5 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
              />
            </label>
          </div>

          <div class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-500">
            발급 권한:
            <strong class="font-semibold text-slate-900">{{ selectedRoleLabel }}</strong>
            <span class="mx-2 text-slate-300">/</span>
            발급 아이디 예시:
            <strong class="font-semibold text-slate-900">{{ getPreviewId() }}</strong>
          </div>

          <button
            type="submit"
            :disabled="isSubmitting"
            class="mt-2 inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-3.5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
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

    <div
      v-if="showResultModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm"
    >
      <div class="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl">
        <div class="border-b border-slate-200 pb-5">
          <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">
            Provisioning complete
          </p>
          <h3 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">
            계정 발급 완료
          </h3>
          <p class="mt-2 text-sm text-slate-500">
            발급된 권한, 아이디, 임시 비밀번호를 사용자에게 전달해 주세요.
          </p>
        </div>

        <div class="mt-5 grid gap-3">
          <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Role</p>
            <strong class="mt-2 block break-all font-mono text-sm font-semibold text-slate-900">
              {{ resultData.role }}
            </strong>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">ID</p>
            <strong class="mt-2 block break-all font-mono text-sm font-semibold text-slate-900">
              {{ resultData.id }}
            </strong>
          </div>
          <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">
              Password
            </p>
            <strong class="mt-2 block break-all font-mono text-sm font-semibold text-slate-900">
              {{ resultData.password }}
            </strong>
          </div>
        </div>

        <div class="mt-5 grid gap-2">
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold transition"
            :class="
              copySuccess
                ? 'border border-emerald-200 bg-emerald-50 text-emerald-700'
                : 'bg-slate-900 text-white hover:bg-slate-800'
            "
            @click="copyAllToClipboard"
          >
            <span>{{ copySuccess ? '계정 정보가 복사되었습니다' : '계정 정보 복사' }}</span>
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
