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
    const id = createdUser.id
    const temporaryPassword = createdUser.password

    if (!id || !temporaryPassword) {
      throw new Error('계정 생성 응답에서 아이디 또는 임시 비밀번호를 찾지 못했습니다.')
    }

    openResultModal(id, temporaryPassword, createdUser.role ?? newUser.role)
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
  <section class="account-provisioning-page ui-page min-h-[calc(100vh-7rem)] px-6 py-10">
    <div class="mx-auto w-full max-w-3xl">
      <div class="mb-8">
        <p class="provisioning-eyebrow text-xs font-semibold uppercase tracking-[0.24em]">
          Account provisioning
        </p>
        <h1 class="provisioning-title mt-2 text-3xl font-bold tracking-tight">계정 발급</h1>
        <p class="provisioning-description mt-2 text-sm">
          ADMIN은 MANAGER와 USER를 만들 수 있고, MANAGER는 USER만 만들 수 있습니다.
        </p>
      </div>

      <div class="provisioning-card rounded-[2rem] p-6 md:p-8">
        <p
          v-if="errorMessage"
          class="provisioning-alert mb-4 rounded-xl px-4 py-3 text-sm font-medium"
        >
          {{ errorMessage }}
        </p>

        <form class="grid gap-4" @submit.prevent="handleCreateUser">
          <label class="grid gap-2">
            <span class="provisioning-label text-[11px] font-semibold uppercase tracking-[0.22em]">
              Role
            </span>
            <select
              v-model="newUser.role"
              class="provisioning-control w-full rounded-xl px-4 py-3.5 text-sm font-medium outline-none transition"
            >
              <option v-for="role in roleOptions" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
          </label>

          <label class="grid gap-2">
            <span class="provisioning-label text-[11px] font-semibold uppercase tracking-[0.22em]">
              Team code
            </span>
            <input
              v-model="newUser.teamCode"
              type="text"
              required
              placeholder="team1"
              class="provisioning-control w-full rounded-xl px-4 py-3.5 text-sm font-medium outline-none transition"
            />
          </label>

          <div class="grid gap-4 md:grid-cols-2">
            <label class="grid gap-2">
              <span
                class="provisioning-label text-[11px] font-semibold uppercase tracking-[0.22em]"
              >
                Name
              </span>
              <input
                v-model="newUser.name"
                type="text"
                required
                placeholder="홍길동"
                class="provisioning-control w-full rounded-xl px-4 py-3.5 text-sm font-medium outline-none transition"
              />
            </label>

            <label class="grid gap-2">
              <span
                class="provisioning-label text-[11px] font-semibold uppercase tracking-[0.22em]"
              >
                Email
              </span>
              <input
                v-model="newUser.email"
                type="email"
                placeholder="user@company.com"
                class="provisioning-control w-full rounded-xl px-4 py-3.5 text-sm font-medium outline-none transition"
              />
            </label>
          </div>

          <div class="provisioning-preview rounded-xl px-4 py-3 text-sm">
            발급 권한:
            <strong class="provisioning-preview__strong font-semibold">{{
              selectedRoleLabel
            }}</strong>
            <span class="provisioning-separator mx-2">/</span>
            발급 아이디 예시:
            <strong class="provisioning-preview__strong font-semibold">{{ getPreviewId() }}</strong>
          </div>

          <button
            type="submit"
            :disabled="isSubmitting"
            class="provisioning-submit mt-2 inline-flex min-h-12 items-center justify-center gap-2 rounded-xl px-4 py-3.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-70"
          >
            <template v-if="!isSubmitting">
              <span>계정 생성</span>
            </template>
            <template v-else>
              <span
                class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white"
              ></span>
              <span>생성 중</span>
            </template>
          </button>
        </form>
      </div>
    </div>

    <div
      v-if="showResultModal"
      class="provisioning-modal-backdrop fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
    >
      <div class="provisioning-modal w-full max-w-md rounded-2xl p-6">
        <div class="provisioning-modal__header pb-5">
          <p class="provisioning-eyebrow text-xs font-semibold uppercase tracking-[0.24em]">
            Provisioning complete
          </p>
          <h3 class="provisioning-title mt-2 text-2xl font-bold tracking-tight">계정 발급 완료</h3>
          <p class="provisioning-description mt-2 text-sm">
            발급된 권한, 아이디, 임시 비밀번호를 사용자에게 전달해 주세요.
          </p>
        </div>

        <div class="mt-5 grid gap-3">
          <div class="provisioning-result rounded-xl p-4">
            <p class="provisioning-label text-xs font-semibold uppercase tracking-[0.22em]">Role</p>
            <strong
              class="provisioning-result__value mt-2 block break-all font-mono text-sm font-semibold"
            >
              {{ resultData.role }}
            </strong>
          </div>
          <div class="provisioning-result rounded-xl p-4">
            <p class="provisioning-label text-xs font-semibold uppercase tracking-[0.22em]">ID</p>
            <strong
              class="provisioning-result__value mt-2 block break-all font-mono text-sm font-semibold"
            >
              {{ resultData.id }}
            </strong>
          </div>
          <div class="provisioning-result rounded-xl p-4">
            <p class="provisioning-label text-xs font-semibold uppercase tracking-[0.22em]">
              Password
            </p>
            <strong
              class="provisioning-result__value mt-2 block break-all font-mono text-sm font-semibold"
            >
              {{ resultData.password }}
            </strong>
          </div>
        </div>

        <div class="mt-5 grid gap-2">
          <button
            type="button"
            class="provisioning-copy-button inline-flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-semibold transition"
            :class="copySuccess ? 'is-success' : 'is-primary'"
            @click="copyAllToClipboard"
          >
            <span>{{ copySuccess ? '계정 정보가 복사되었습니다' : '계정 정보 복사' }}</span>
          </button>
          <button
            type="button"
            class="provisioning-close-button rounded-xl px-4 py-3 text-sm font-semibold transition"
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
.account-provisioning-page {
  color: var(--text-body);
}

.provisioning-eyebrow,
.provisioning-label {
  color: var(--text-muted);
}

.provisioning-title {
  color: var(--text-heading);
}

.provisioning-description {
  color: var(--text-muted);
}

.provisioning-card,
.provisioning-modal {
  border: 1px solid var(--line-soft);
  background: var(--surface-card);
  color: var(--text-body);
  box-shadow: var(--shadow-soft);
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal),
    color var(--transition-normal),
    box-shadow var(--transition-normal);
}

.provisioning-alert {
  border: 1px solid color-mix(in srgb, var(--danger-color) 28%, transparent);
  background: color-mix(in srgb, var(--danger-color) 12%, var(--surface-card));
  color: var(--danger-text-strong);
}

.provisioning-control {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-heading);
}

.provisioning-control:focus {
  border-color: var(--line-strong);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent-color) 13%, transparent);
}

.provisioning-control::placeholder {
  color: var(--text-muted);
  opacity: 0.72;
}

.provisioning-control option {
  background: var(--surface-card);
  color: var(--text-heading);
}

.provisioning-preview,
.provisioning-result {
  border: 1px solid var(--line-soft);
  background: var(--surface-card-muted);
  color: var(--text-muted);
}

.provisioning-preview__strong,
.provisioning-result__value {
  color: var(--text-heading);
}

.provisioning-separator {
  color: var(--line-strong);
}

.provisioning-submit,
.provisioning-copy-button.is-primary {
  border: 1px solid transparent;
  background: linear-gradient(135deg, var(--accent-color), var(--accent-strong));
  color: #fff;
  box-shadow: 0 14px 30px color-mix(in srgb, var(--accent-color) 22%, transparent);
}

.provisioning-submit:hover,
.provisioning-copy-button.is-primary:hover {
  filter: brightness(1.04);
}

.provisioning-modal-backdrop {
  background: rgba(15, 23, 42, 0.38);
}

.provisioning-modal {
  box-shadow: var(--shadow-elevated);
}

.provisioning-modal__header {
  border-bottom: 1px solid var(--line-soft);
}

.provisioning-copy-button.is-success {
  border: 1px solid color-mix(in srgb, var(--success-color) 38%, transparent);
  background: color-mix(in srgb, var(--success-color) 14%, var(--surface-card));
  color: var(--success-color);
}

.provisioning-close-button {
  border: 1px solid var(--line-soft);
  background: var(--surface-card);
  color: var(--text-body);
}

.provisioning-close-button:hover {
  background: var(--surface-control-hover);
}

:global(:root[data-theme='dark']) .provisioning-modal-backdrop {
  background: rgba(0, 0, 0, 0.58);
}
</style>
