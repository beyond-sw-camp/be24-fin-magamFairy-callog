<script setup>
import { computed, reactive, ref, watch } from 'vue'
import {
  createUserRequest,
  deleteUserRequest,
  fetchManageableUsersRequest,
  manageUserRoleRequest,
  resetPasswordRequest,
} from '@/authApi'
import { useAuthStore } from '@/stores/useAuthStore'

const authStore = useAuthStore()

const showCreateModal = ref(false)
const manageableUsers = ref([])
const memberSearch = ref('')
const memberActionId = ref('')

const status = reactive({
  createLoading: false,
  listLoading: false,
  roleLoading: false,
  deleteLoading: false,
  resetLoading: false,
  createError: '',
  listError: '',
  actionError: '',
})

const resultModal = reactive({
  open: false,
  title: '',
  description: '',
  rows: [],
  copyText: '',
  copied: false,
})

const managerCompanyName = computed(() => authStore.user?.companyName ?? '')
const managerDepartment = computed(() => authStore.user?.department ?? '')

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

const selectedRoleLabel = computed(
  () => roleOptions.value.find((role) => role.value === createForm.role)?.label ?? 'USER - 구성원',
)

const previewId = computed(() => {
  const company = createForm.companyName.trim() || '회사명'
  const department = createForm.department.trim() || '부서명'
  const name = createForm.name.trim() || '이름'

  return `${company}_${department}_${name}`
})

const filteredManageableUsers = computed(() => {
  const keyword = memberSearch.value.trim().toLowerCase()

  if (!keyword) {
    return manageableUsers.value
  }

  return manageableUsers.value.filter((user) => {
    const searchable = [
      user.name,
      user.id,
      user.email,
      user.companyName,
      user.department,
      getMemberRoleLabel(user.role),
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    return searchable.includes(keyword)
  })
})

const visibleMemberCount = computed(() => filteredManageableUsers.value.length)
const totalMemberCount = computed(() => manageableUsers.value.length)

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

    ensureAllowedRole()
  },
  { immediate: true },
)

watch(
  () => [
    authStore.isAuthenticated,
    authStore.user?.id,
    authStore.user?.role,
    managerCompanyName.value,
    managerDepartment.value,
  ],
  () => {
    fetchManageableUsers()
  },
  { immediate: true },
)

function ensureAllowedRole() {
  if (!roleOptions.value.some((role) => role.value === createForm.role)) {
    createForm.role = roleOptions.value[0]?.value ?? 'USER'
  }
}

function openCreateModal() {
  ensureAllowedRole()
  status.createError = ''
  showCreateModal.value = true
}

function closeCreateModal() {
  if (status.createLoading) return
  showCreateModal.value = false
  resetCreateForm()
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

function resolveErrorMessage(error, fallback) {
  return error?.response?.data?.message ?? error?.message ?? fallback
}

function normalizeMemberRole(role) {
  if (!role) return ''
  return role.startsWith('ROLE_') ? role : `ROLE_${role}`
}

function getMemberRoleLabel(role) {
  const normalizedRole = normalizeMemberRole(role)
  if (normalizedRole === 'ROLE_GENERAL_MANAGER') return 'GENERAL_MANAGER'
  if (normalizedRole === 'ROLE_MANAGER') return 'MANAGER'
  if (normalizedRole === 'ROLE_USER') return 'USER'
  return role ?? '-'
}

function getMemberNextRole(role) {
  const normalizedRole = normalizeMemberRole(role)
  if (normalizedRole === 'ROLE_USER') return 'MANAGER'
  if (normalizedRole === 'ROLE_MANAGER') return 'USER'
  return ''
}

function getMemberActionLabel(role) {
  const nextRole = getMemberNextRole(role)
  if (nextRole === 'MANAGER') return 'MANAGER로 승격'
  if (nextRole === 'USER') return 'USER로 강등'
  return '변경 불가'
}

function canChangeMemberRole(user) {
  return authStore.isGeneralManager && Boolean(getMemberNextRole(user.role))
}

function isMemberAction(action, user) {
  return memberActionId.value === `${action}-${user.id}`
}

async function fetchManageableUsers({ silent = false } = {}) {
  if (!authStore.isAuthenticated) {
    manageableUsers.value = []
    return
  }

  if (!silent) {
    status.listLoading = true
    status.listError = ''
  }

  try {
    const users = await fetchManageableUsersRequest()
    manageableUsers.value = Array.isArray(users) ? users : []
  } catch (error) {
    if (!silent) {
      status.listError = resolveErrorMessage(
        error,
        '구성원 목록을 불러오지 못했습니다.',
      )
    }
  } finally {
    if (!silent) {
      status.listLoading = false
    }
  }
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

    showCreateModal.value = false
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
    await fetchManageableUsers({ silent: true })
  } catch (error) {
    status.createError = resolveErrorMessage(
      error,
      '계정 생성에 실패했습니다. 권한과 입력 정보를 확인해 주세요.',
    )
  } finally {
    status.createLoading = false
  }
}

async function handleMemberRoleChange(user) {
  if (status.roleLoading) return

  const nextRole = getMemberNextRole(user.role)
  if (!nextRole) {
    status.actionError = '변경할 수 없는 권한입니다.'
    return
  }

  status.roleLoading = true
  status.actionError = ''
  memberActionId.value = `role-${user.id}`

  try {
    const managedUser = await manageUserRoleRequest({
      id: user.id,
      role: nextRole,
    })

    manageableUsers.value = manageableUsers.value.map((member) => {
      if (member.id !== managedUser.id) {
        return member
      }

      return {
        ...member,
        name: managedUser.name ?? member.name,
        role: managedUser.role,
        companyName: managedUser.companyName ?? member.companyName,
        department: managedUser.department ?? member.department,
      }
    })

    openResultModal({
      title: '권한 변경 완료',
      description: '대상 계정의 권한이 변경되었습니다.',
      rows: [
        { label: '아이디', value: managedUser.id },
        { label: '이름', value: managedUser.name },
        { label: '이전 권한', value: managedUser.previousRole },
        { label: '변경 권한', value: managedUser.role },
        { label: '소속', value: `${managedUser.companyName ?? '-'} / ${managedUser.department ?? '-'}` },
      ],
      copyText: `권한 변경 완료\n아이디: ${managedUser.id}\n이름: ${managedUser.name}\n이전 권한: ${managedUser.previousRole}\n변경 권한: ${managedUser.role}`,
    })
  } catch (error) {
    status.actionError = resolveErrorMessage(
      error,
      '권한 변경에 실패했습니다. 권한 범위를 확인해 주세요.',
    )
  } finally {
    status.roleLoading = false
    memberActionId.value = ''
  }
}

async function handleMemberPasswordReset(user) {
  if (status.resetLoading) return

  status.resetLoading = true
  status.actionError = ''
  memberActionId.value = `reset-${user.id}`

  try {
    const resetResult = await resetPasswordRequest({ id: user.id })

    openResultModal({
      title: '비밀번호 재설정 완료',
      description: '새 임시 비밀번호를 사용자에게 전달해 주세요.',
      rows: [
        { label: '아이디', value: resetResult.id },
        { label: '새 임시 비밀번호', value: resetResult.password },
      ],
      copyText: `비밀번호 재설정\n아이디: ${resetResult.id}\n새 임시 비밀번호: ${resetResult.password}`,
    })
  } catch (error) {
    status.actionError = resolveErrorMessage(
      error,
      '비밀번호 재설정에 실패했습니다. 권한 범위를 확인해 주세요.',
    )
  } finally {
    status.resetLoading = false
    memberActionId.value = ''
  }
}

async function handleMemberDelete(user) {
  if (status.deleteLoading) return

  const confirmed = window.confirm(`${user.name ?? user.id} 계정을 삭제할까요?`)
  if (!confirmed) {
    return
  }

  status.deleteLoading = true
  status.actionError = ''
  memberActionId.value = `delete-${user.id}`

  try {
    const deletedUser = await deleteUserRequest({ id: user.id })

    manageableUsers.value = manageableUsers.value.filter((member) => member.id !== deletedUser.id)

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
  } catch (error) {
    status.actionError = resolveErrorMessage(
      error,
      '계정 삭제에 실패했습니다. 권한 범위를 확인해 주세요.',
    )
  } finally {
    status.deleteLoading = false
    memberActionId.value = ''
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
    <div class="mx-auto w-full max-w-6xl">
      <header class="hr-header">
        <div>
          <p class="hr-eyebrow">Human resources</p>
          <h1>인사관리</h1>
          <p>
            같은 조직의 구성원을 확인하고 비밀번호 재설정, 권한 변경, 계정 삭제를 바로 처리합니다.
          </p>
        </div>

        <button type="button" class="hr-primary" @click="openCreateModal">
          유저 생성
        </button>
      </header>

      <article class="hr-card">
        <div class="hr-card__header hr-list-header">
          <div>
            <p class="hr-eyebrow">Members</p>
            <h2>구성원 목록</h2>
            <span>총 {{ totalMemberCount }}명 중 {{ visibleMemberCount }}명을 표시합니다.</span>
          </div>

          <button
            type="button"
            class="hr-secondary hr-compact"
            :disabled="status.listLoading"
            @click="fetchManageableUsers()"
          >
            {{ status.listLoading ? '불러오는 중' : '새로고침' }}
          </button>
        </div>

        <p v-if="status.listError" class="hr-alert">{{ status.listError }}</p>
        <p v-if="status.actionError" class="hr-alert">{{ status.actionError }}</p>

        <div class="hr-manage-toolbar">
          <label class="hr-field">
            <span>구성원 검색</span>
            <input v-model.trim="memberSearch" type="text" placeholder="이름, 아이디, 이메일, 부서, 권한 검색" />
          </label>
        </div>

        <div v-if="status.listLoading" class="hr-empty">구성원 목록을 불러오는 중입니다.</div>

        <div v-else-if="filteredManageableUsers.length" class="hr-member-list">
          <div v-for="user in filteredManageableUsers" :key="user.id" class="hr-member-row">
            <div class="hr-member-main">
              <strong>{{ user.name || '-' }}</strong>
              <span>{{ user.id }}</span>
              <small>{{ user.email || '이메일 없음' }}</small>
            </div>

            <div class="hr-member-meta">
              <span>{{ user.companyName || '-' }} / {{ user.department || '-' }}</span>
              <b class="hr-role-pill">{{ getMemberRoleLabel(user.role) }}</b>
            </div>

            <div class="hr-member-actions">
              <button
                type="button"
                class="hr-secondary hr-compact"
                :disabled="memberActionId !== ''"
                @click="handleMemberPasswordReset(user)"
              >
                {{ isMemberAction('reset', user) ? '재설정 중' : '비밀번호 재설정' }}
              </button>

              <button
                v-if="canChangeMemberRole(user)"
                type="button"
                class="hr-primary hr-compact"
                :disabled="memberActionId !== ''"
                @click="handleMemberRoleChange(user)"
              >
                {{ isMemberAction('role', user) ? '처리 중' : getMemberActionLabel(user.role) }}
              </button>

              <button
                type="button"
                class="hr-danger hr-compact"
                :disabled="memberActionId !== ''"
                @click="handleMemberDelete(user)"
              >
                {{ isMemberAction('delete', user) ? '삭제 중' : '삭제' }}
              </button>
            </div>
          </div>
        </div>

        <div v-else class="hr-empty">관리할 수 있는 구성원이 없습니다.</div>
      </article>
    </div>

    <div
      v-if="showCreateModal"
      class="hr-modal-backdrop fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-sm"
    >
      <div class="hr-modal hr-modal--form w-full max-w-2xl">
        <div class="hr-modal__header hr-modal__header--split">
          <div>
            <p class="hr-eyebrow">Account creation</p>
            <h3>유저 생성</h3>
            <p>관리 범위 안에서 새 계정을 만들고 임시 비밀번호를 발급합니다.</p>
          </div>

          <button type="button" class="hr-secondary hr-compact" @click="closeCreateModal">
            닫기
          </button>
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
      </div>
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
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
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

.hr-list-header,
.hr-modal__header--split {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
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

.hr-manage-toolbar {
  margin-bottom: 16px;
}

.hr-member-list {
  display: grid;
  gap: 10px;
}

.hr-member-row {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr) auto;
  align-items: center;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
  padding: 14px;
}

.hr-member-main,
.hr-member-meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.hr-member-main strong {
  color: var(--text-heading);
  font-size: 15px;
  font-weight: 800;
}

.hr-member-main span,
.hr-member-main small,
.hr-member-meta span {
  color: var(--text-muted);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.hr-role-pill {
  width: fit-content;
  border: 1px solid color-mix(in srgb, var(--accent-color) 28%, var(--line-soft));
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 10%, transparent);
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 800;
  padding: 4px 10px;
}

.hr-member-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.hr-compact {
  min-height: 38px;
  padding: 0 12px;
  white-space: nowrap;
}

.hr-empty {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 700;
  padding: 18px;
  text-align: center;
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
.hr-danger:disabled,
.hr-secondary:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.hr-modal-backdrop {
  background: rgba(15, 23, 42, 0.38);
}

.hr-modal {
  box-shadow: var(--shadow-elevated);
}

.hr-modal--form {
  max-height: calc(100vh - 48px);
  overflow: auto;
}

.hr-modal__header {
  border-bottom: 1px solid var(--line-soft);
  margin-bottom: 18px;
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

@media (max-width: 820px) {
  .hr-header,
  .hr-list-header,
  .hr-modal__header--split,
  .hr-grid,
  .hr-member-row {
    grid-template-columns: 1fr;
  }

  .hr-header,
  .hr-list-header,
  .hr-modal__header--split {
    display: grid;
  }

  .hr-member-actions {
    display: grid;
    grid-template-columns: 1fr;
    justify-content: stretch;
  }

  .hr-result-list div {
    grid-template-columns: 1fr;
  }
}
</style>
