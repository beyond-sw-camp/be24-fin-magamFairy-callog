<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { createUserRequest, deleteUserRequest, resetPasswordRequest } from '@/authApi'
import { useAuthStore } from '@/stores/useAuthStore'

const authStore = useAuthStore()

const activeTab = ref('create')
const tabs = [
  { id: 'create', label: '계정 생성' },
  { id: 'delete', label: '계정 삭제' },
  { id: 'reset', label: '비밀번호 재발급' },
]

const roleOptions = computed(() => {
  if (authStore.isAdmin) {
    return [
      { value: 'GENERAL_MANAGER', label: 'GENERAL_MANAGER - 협력사 총괄' },
      { value: 'USER', label: 'USER - 구성원' },
    ]
  }

  if (authStore.isGeneralManager) {
    return [
      { value: 'MANAGER', label: 'MANAGER - 부서 책임자' },
      { value: 'USER', label: 'USER - 구성원' },
    ]
  }

  return [{ value: 'USER', label: 'USER - 구성원' }]
})

const managerCompanyName = computed(() => authStore.user?.companyName ?? '')
const managerDepartment = computed(() => authStore.user?.department ?? '')

function getDefaultCreateRole() {
  if (authStore.isAdmin) {
    return 'GENERAL_MANAGER'
  }

  if (authStore.isGeneralManager) {
    return 'MANAGER'
  }

  return 'USER'
}

const createForm = reactive({
  companyName: authStore.isGeneralManager || authStore.isManager ? managerCompanyName.value : '',
  department: authStore.isManager ? managerDepartment.value : '',
  name: '',
  email: '',
  role: getDefaultCreateRole(),
})

const deleteForm = reactive({
  id: '',
  confirmId: '',
})

const resetForm = reactive({
  id: '',
})

const status = reactive({
  createLoading: false,
  deleteLoading: false,
  resetLoading: false,
  createError: '',
  deleteError: '',
  resetError: '',
})

const resultModal = reactive({
  open: false,
  title: '',
  description: '',
  rows: [],
  copyText: '',
  copied: false,
})

const selectedRoleLabel = computed(
  () => roleOptions.value.find((role) => role.value === createForm.role)?.label ?? 'USER - 구성원',
)

const previewId = computed(() => {
  const company = createForm.companyName.trim() || '회사명'
  const department = createForm.department.trim() || '부서명'
  const name = createForm.name.trim() || '이름'

  return `${company}_${department}_${name}`
})

watch(
  () => [authStore.isGeneralManager, authStore.isManager, managerCompanyName.value, managerDepartment.value],
  () => {
    if (authStore.isGeneralManager || authStore.isManager) {
      createForm.companyName = managerCompanyName.value
    }

    if (authStore.isManager) {
      createForm.department = managerDepartment.value
      createForm.role = 'USER'
      return
    }

    if (authStore.isGeneralManager) {
      createForm.role = 'MANAGER'
    }
  },
  { immediate: true },
)

function ensureAllowedRole() {
  if (!roleOptions.value.some((role) => role.value === createForm.role)) {
    createForm.role = roleOptions.value[0]?.value ?? 'USER'
  }
}

function openResultModal({ title, description, rows, copyText }) {
  resultModal.open = true
  resultModal.title = title
  resultModal.description = description
  resultModal.rows = rows
  resultModal.copyText = copyText
  resultModal.copied = false
}

function closeResultModal() {
  resultModal.open = false
  resultModal.rows = []
  resultModal.copyText = ''
  resultModal.copied = false
}

function resetCreateForm() {
  createForm.companyName = authStore.isGeneralManager || authStore.isManager ? managerCompanyName.value : ''
  createForm.department = authStore.isManager ? managerDepartment.value : ''
  createForm.name = ''
  createForm.email = ''
  createForm.role = getDefaultCreateRole()
  status.createError = ''
}

function resetDeleteForm() {
  deleteForm.id = ''
  deleteForm.confirmId = ''
  status.deleteError = ''
}

function resetResetForm() {
  resetForm.id = ''
  status.resetError = ''
}

function resolveErrorMessage(error, fallback) {
  return error?.response?.data?.message ?? error?.message ?? fallback
}

async function handleCreateUser() {
  ensureAllowedRole()

  if (status.createLoading) return
  if (!createForm.companyName.trim() || !createForm.department.trim() || !createForm.name.trim()) {
    status.createError = '회사명, 부서명, 이름을 입력해 주세요.'
    return
  }

  status.createLoading = true
  status.createError = ''

  try {
    const createdUser = await createUserRequest({
      companyName: createForm.companyName.trim(),
      department: createForm.department.trim(),
      name: createForm.name.trim(),
      email: createForm.email.trim() || null,
      role: createForm.role,
    })

    openResultModal({
      title: '계정 생성 완료',
      description: '아래 계정 정보를 사용자에게 전달해 주세요.',
      rows: [
        { label: '권한', value: createdUser.role ?? createForm.role },
        { label: '아이디', value: createdUser.id },
        { label: '임시 비밀번호', value: createdUser.password },
      ],
      copyText: `계정 정보\n권한: ${createdUser.role ?? createForm.role}\n아이디: ${createdUser.id}\n임시 비밀번호: ${createdUser.password}`,
    })
    resetCreateForm()
  } catch (error) {
    status.createError = resolveErrorMessage(
      error,
      '계정 생성에 실패했습니다. 권한과 입력 정보를 확인해 주세요.',
    )
  } finally {
    status.createLoading = false
  }
}

async function handleDeleteUser() {
  if (status.deleteLoading) return

  const id = deleteForm.id.trim()
  const confirmId = deleteForm.confirmId.trim()
  if (!id || !confirmId) {
    status.deleteError = '삭제할 계정 아이디를 두 번 입력해 주세요.'
    return
  }

  if (id !== confirmId) {
    status.deleteError = '확인 아이디가 일치하지 않습니다.'
    return
  }

  status.deleteLoading = true
  status.deleteError = ''

  try {
    const deletedUser = await deleteUserRequest({ id })

    openResultModal({
      title: '계정 삭제 완료',
      description: '해당 계정은 비활성화되었고 기존 로그인 세션은 만료됩니다.',
      rows: [
        { label: '아이디', value: deletedUser.id },
        { label: '이름', value: deletedUser.name },
        { label: '권한', value: deletedUser.role },
        { label: '소속', value: `${deletedUser.companyName ?? '-'} / ${deletedUser.department ?? '-'}` },
      ],
      copyText: `계정 삭제 완료\n아이디: ${deletedUser.id}\n이름: ${deletedUser.name}\n권한: ${deletedUser.role}`,
    })
    resetDeleteForm()
  } catch (error) {
    status.deleteError = resolveErrorMessage(
      error,
      '계정 삭제에 실패했습니다. 아이디와 권한 범위를 확인해 주세요.',
    )
  } finally {
    status.deleteLoading = false
  }
}

async function handleResetPassword() {
  if (status.resetLoading) return

  const id = resetForm.id.trim()
  if (!id) {
    status.resetError = '아이디를 입력해 주세요.'
    return
  }

  status.resetLoading = true
  status.resetError = ''

  try {
    const resetResult = await resetPasswordRequest({ id })

    openResultModal({
      title: '비밀번호 재발급 완료',
      description: '새 임시 비밀번호를 사용자에게 전달해 주세요.',
      rows: [
        { label: '아이디', value: resetResult.id },
        { label: '새 임시 비밀번호', value: resetResult.password },
      ],
      copyText: `비밀번호 재발급\n아이디: ${resetResult.id}\n새 임시 비밀번호: ${resetResult.password}`,
    })
    resetResetForm()
  } catch (error) {
    status.resetError = resolveErrorMessage(
      error,
      '비밀번호 재발급에 실패했습니다. 아이디와 권한 범위를 확인해 주세요.',
    )
  } finally {
    status.resetLoading = false
  }
}

async function copyResult() {
  try {
    await navigator.clipboard.writeText(resultModal.copyText)
    resultModal.copied = true
    window.setTimeout(() => {
      resultModal.copied = false
    }, 2000)
  } catch {
    resultModal.copied = false
  }
}
</script>

<template>
  <section class="hr-page ui-page min-h-[calc(100vh-7rem)] px-6 py-10">
    <div class="mx-auto w-full max-w-4xl">
      <header class="hr-header">
        <p class="hr-eyebrow">Human resources</p>
        <h1>인사관리</h1>
        <p>
          계정 생성, 계정 삭제, 비밀번호 재발급을 한 곳에서 처리합니다.
          GENERAL_MANAGER는 같은 회사의 MANAGER/USER를, MANAGER는 같은 회사와 같은 부서의 USER만 관리할 수 있습니다.
        </p>
      </header>

      <nav class="hr-tabs" aria-label="인사관리 메뉴">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="hr-tab"
          :class="{ 'is-active': activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </nav>

      <article v-if="activeTab === 'create'" class="hr-card">
        <div class="hr-card__header">
          <p class="hr-eyebrow">Account creation</p>
          <h2>계정 생성</h2>
          <span>ADMIN은 GENERAL_MANAGER/USER를, GENERAL_MANAGER는 같은 회사의 MANAGER/USER를, MANAGER는 같은 회사와 부서의 USER를 생성합니다.</span>
        </div>

        <p v-if="status.createError" class="hr-alert">{{ status.createError }}</p>

        <form class="hr-form" @submit.prevent="handleCreateUser">
          <label class="hr-field">
            <span>권한</span>
            <select v-model="createForm.role">
              <option v-for="role in roleOptions" :key="role.value" :value="role.value">
                {{ role.label }}
              </option>
            </select>
          </label>

          <div class="hr-grid">
            <label class="hr-field">
              <span>회사명</span>
              <div v-if="authStore.isGeneralManager || authStore.isManager" class="hr-fixed">{{ createForm.companyName || '-' }}</div>
              <input v-else v-model.trim="createForm.companyName" type="text" placeholder="예: CALLOG" />
            </label>

            <label class="hr-field">
              <span>부서명</span>
              <div v-if="authStore.isManager" class="hr-fixed">{{ createForm.department || '-' }}</div>
              <input v-else v-model.trim="createForm.department" type="text" placeholder="예: 마케팅팀" />
            </label>
          </div>

          <div class="hr-grid">
            <label class="hr-field">
              <span>이름</span>
              <input v-model.trim="createForm.name" type="text" placeholder="예: 홍길동" />
            </label>

            <label class="hr-field">
              <span>이메일</span>
              <input v-model.trim="createForm.email" type="email" placeholder="user@company.com" />
            </label>
          </div>

          <div class="hr-preview">
            <span>발급 권한: <strong>{{ selectedRoleLabel }}</strong></span>
            <span>아이디 예시: <strong>{{ previewId }}</strong></span>
          </div>

          <button type="submit" class="hr-primary" :disabled="status.createLoading">
            {{ status.createLoading ? '생성 중' : '계정 생성' }}
          </button>
        </form>
      </article>

      <article v-else-if="activeTab === 'delete'" class="hr-card">
        <div class="hr-card__header">
          <p class="hr-eyebrow">Account deletion</p>
          <h2>계정 삭제</h2>
          <span>삭제된 계정은 즉시 비활성화되며 더 이상 로그인할 수 없습니다.</span>
        </div>

        <p v-if="status.deleteError" class="hr-alert">{{ status.deleteError }}</p>

        <form class="hr-form" @submit.prevent="handleDeleteUser">
          <label class="hr-field">
            <span>삭제할 아이디</span>
            <input v-model.trim="deleteForm.id" type="text" placeholder="예: CALLOG_마케팅팀_홍길동" />
          </label>

          <label class="hr-field">
            <span>아이디 확인</span>
            <input v-model.trim="deleteForm.confirmId" type="text" placeholder="삭제할 아이디를 한 번 더 입력" />
          </label>

          <button type="submit" class="hr-danger" :disabled="status.deleteLoading">
            {{ status.deleteLoading ? '삭제 중' : '계정 삭제' }}
          </button>
        </form>
      </article>

      <article v-else class="hr-card">
        <div class="hr-card__header">
          <p class="hr-eyebrow">Password reset</p>
          <h2>비밀번호 재발급</h2>
          <span>관리 가능한 계정에 새 임시 비밀번호를 발급합니다.</span>
        </div>

        <p v-if="status.resetError" class="hr-alert">{{ status.resetError }}</p>

        <form class="hr-form" @submit.prevent="handleResetPassword">
          <label class="hr-field">
            <span>아이디</span>
            <input v-model.trim="resetForm.id" type="text" placeholder="예: CALLOG_마케팅팀_홍길동" />
          </label>

          <button type="submit" class="hr-primary" :disabled="status.resetLoading">
            {{ status.resetLoading ? '재발급 중' : '비밀번호 재발급' }}
          </button>
        </form>
      </article>
    </div>

    <div
      v-if="resultModal.open"
      class="hr-modal-backdrop fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
    >
      <div class="hr-modal w-full max-w-md">
        <div class="hr-modal__header">
          <p class="hr-eyebrow">Complete</p>
          <h3>{{ resultModal.title }}</h3>
          <p>{{ resultModal.description }}</p>
        </div>

        <dl class="hr-result-list">
          <div v-for="row in resultModal.rows" :key="row.label">
            <dt>{{ row.label }}</dt>
            <dd>{{ row.value }}</dd>
          </div>
        </dl>

        <div class="hr-modal__actions">
          <button type="button" class="hr-primary" @click="copyResult">
            {{ resultModal.copied ? '복사되었습니다' : '결과 복사' }}
          </button>
          <button type="button" class="hr-secondary" @click="closeResultModal">닫기</button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hr-page {
  color: var(--text-body);
}

.hr-header {
  margin-bottom: 24px;
}

.hr-eyebrow {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hr-header h1,
.hr-card__header h2,
.hr-modal h3 {
  color: var(--text-heading);
  font-weight: 800;
}

.hr-header h1 {
  margin-top: 8px;
  font-size: 32px;
}

.hr-header p:last-child,
.hr-card__header span,
.hr-modal__header p:last-child {
  color: var(--text-muted);
  font-size: 14px;
  line-height: 1.6;
  margin-top: 8px;
}

.hr-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.hr-tab {
  min-height: 40px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  color: var(--text-body);
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  padding: 0 16px;
}

.hr-tab.is-active {
  border-color: transparent;
  background: var(--accent-strong);
  color: #fff;
}

.hr-card,
.hr-modal {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-soft);
  padding: 24px;
}

.hr-card__header {
  margin-bottom: 20px;
}

.hr-card__header h2,
.hr-modal h3 {
  margin-top: 6px;
  font-size: 22px;
}

.hr-form {
  display: grid;
  gap: 16px;
}

.hr-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.hr-field {
  display: grid;
  gap: 8px;
}

.hr-field span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.hr-field input,
.hr-field select,
.hr-fixed {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 600;
  outline: none;
  padding: 0 14px;
}

.hr-fixed {
  display: flex;
  align-items: center;
  opacity: 0.72;
}

.hr-field input:focus,
.hr-field select:focus {
  border-color: var(--line-strong);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent-color) 13%, transparent);
}

.hr-preview,
.hr-alert,
.hr-result-list {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
}

.hr-preview {
  display: grid;
  gap: 8px;
  color: var(--text-muted);
  font-size: 14px;
  padding: 14px;
}

.hr-preview strong {
  color: var(--text-heading);
}

.hr-alert {
  border-color: color-mix(in srgb, var(--danger-color) 30%, var(--line-soft));
  background: color-mix(in srgb, var(--danger-color) 12%, var(--surface-card));
  color: var(--danger-text-strong);
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 16px;
  padding: 12px 14px;
}

.hr-primary,
.hr-danger,
.hr-secondary {
  min-height: 44px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 800;
  padding: 0 18px;
}

.hr-primary {
  border: 1px solid transparent;
  background: var(--accent-strong);
  color: #fff;
}

.hr-danger {
  border: 1px solid color-mix(in srgb, var(--danger-color) 38%, transparent);
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.hr-secondary {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-body);
}

.hr-primary:disabled,
.hr-danger:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.hr-modal-backdrop {
  background: rgba(15, 23, 42, 0.38);
}

.hr-modal {
  box-shadow: var(--shadow-elevated);
}

.hr-modal__header {
  border-bottom: 1px solid var(--line-soft);
  padding-bottom: 18px;
}

.hr-result-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
  padding: 16px;
}

.hr-result-list div {
  display: grid;
  gap: 8px;
  grid-template-columns: 120px minmax(0, 1fr);
}

.hr-result-list dt {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.hr-result-list dd {
  color: var(--text-heading);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.hr-modal__actions {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

@media (max-width: 720px) {
  .hr-tabs,
  .hr-grid {
    grid-template-columns: 1fr;
  }

  .hr-tabs {
    display: grid;
  }

  .hr-result-list div {
    grid-template-columns: 1fr;
  }
}
</style>
