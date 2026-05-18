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
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const organizationTextPattern = /^[\p{L}\p{N}\s().,&+\-_/·]+$/u
const personNamePattern = /^[\p{L}\s.\-·]+$/u

const resultModal = reactive({
  open: false,
  title: '',
  description: '',
  rows: [],
  copyText: '',
  copied: false,
})

const roleConfirmModal = reactive({
  open: false,
  user: null,
  nextRole: '',
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

function closeRoleConfirmModal() {
  if (status.roleLoading) return
  roleConfirmModal.open = false
  roleConfirmModal.user = null
  roleConfirmModal.nextRole = ''
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

function validateStructuredText(value, label, min, max, pattern, allowDigitOnly = true) {
  const normalized = value.trim()
  if (!normalized) {
    return `${label}을(를) 입력해 주세요.`
  }

  if (normalized.length < min || normalized.length > max) {
    return `${label}은(는) ${min}자 이상 ${max}자 이하로 입력해 주세요.`
  }

  const hasLetter = /\p{L}/u.test(normalized)
  const hasDigit = /\p{N}/u.test(normalized)
  if (!hasLetter && (!allowDigitOnly || !hasDigit)) {
    return `${label}에는 한글, 영문${allowDigitOnly ? ' 또는 숫자' : ''}를 포함해 주세요.`
  }

  if (!pattern.test(normalized)) {
    return `${label}에 사용할 수 없는 문자가 포함되어 있습니다.`
  }

  return ''
}

function validateCreateForm() {
  const companyError = validateStructuredText(createForm.companyName, '회사명', 2, 60, organizationTextPattern)
  if (companyError) return companyError

  const departmentError = validateStructuredText(createForm.department, '부서명', 2, 40, organizationTextPattern)
  if (departmentError) return departmentError

  const nameError = validateStructuredText(createForm.name, '이름', 2, 30, personNamePattern, false)
  if (nameError) return nameError

  const email = createForm.email.trim()
  if (email && (!emailPattern.test(email) || email.length > 254)) {
    return '올바른 이메일 형식으로 입력해 주세요.'
  }

  return ''
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

function getMemberPhotoUrl(user) {
  return user?.profileImageUrl ?? user?.imageDataUrl ?? user?.profileImage ?? user?.avatar ?? ''
}

function getMemberInitial(user) {
  return String(user?.name || user?.id || '?').trim().slice(0, 1).toUpperCase()
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

  status.createError = validateCreateForm()
  if (status.createError) {
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

function handleMemberRoleChange(user) {
  if (status.roleLoading) return

  const nextRole = getMemberNextRole(user.role)
  if (!nextRole) {
    status.actionError = '변경할 수 없는 권한입니다.'
    return
  }

  status.actionError = ''
  roleConfirmModal.open = true
  roleConfirmModal.user = user
  roleConfirmModal.nextRole = nextRole
}

async function confirmMemberRoleChange() {
  const user = roleConfirmModal.user
  const nextRole = roleConfirmModal.nextRole

  if (!user || !nextRole || status.roleLoading) return

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

    roleConfirmModal.open = false
    roleConfirmModal.user = null
    roleConfirmModal.nextRole = ''
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
      title: '삭제되었습니다.',
      description: `${deletedUser.name ?? deletedUser.id} 계정이 삭제되었습니다.`,
      rows: [],
      copyText: '',
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
  <section class="hr-page ui-page">
    <div class="hr-shell">
      <header class="hr-topbar">
        <div class="hr-title">
          <p>Human resources</p>
          <h1>사용자관리</h1>
        </div>

        <div class="hr-toolbar">
          <label class="hr-search" aria-label="구성원 검색">
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.6-3.6" />
            </svg>
            <input v-model.trim="memberSearch" type="text" placeholder="이름, 아이디, 회사명, 부서, 권한 검색" />
          </label>

          <button type="button" class="hr-primary" @click="openCreateModal">
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <path d="M12 5v14M5 12h14" />
            </svg>
            <span>유저 생성</span>
          </button>

          <button
            type="button"
            class="hr-icon-button"
            :disabled="status.listLoading"
            aria-label="새로고침"
            @click="fetchManageableUsers()"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <path d="M20 12a8 8 0 1 1-2.3-5.6M20 4v6h-6" />
            </svg>
          </button>
        </div>
      </header>

      <article class="hr-panel">
        <div class="hr-panel__summary">
          <span>전체 {{ totalMemberCount }}명</span>
          <span>검색 결과 {{ visibleMemberCount }}명</span>
        </div>

        <p v-if="status.listError" class="hr-alert">{{ status.listError }}</p>
        <p v-if="status.actionError" class="hr-alert">{{ status.actionError }}</p>

        <div v-if="status.listLoading" class="hr-empty">구성원 목록을 불러오는 중입니다.</div>

        <div v-else-if="filteredManageableUsers.length" class="hr-member-list">
          <article v-for="user in filteredManageableUsers" :key="user.id" class="hr-member-card">
            <div class="hr-member-photo" aria-hidden="true">
              <img v-if="getMemberPhotoUrl(user)" :src="getMemberPhotoUrl(user)" alt="" />
              <span v-else>{{ getMemberInitial(user) }}</span>
            </div>

            <div class="hr-member-identity">
              <div class="hr-member-name-row">
                <strong>{{ user.name || '-' }}</strong>
                <b class="hr-role-pill">{{ getMemberRoleLabel(user.role) }}</b>
              </div>
              <div class="hr-member-sub">
                <span>{{ user.id }}</span>
                <span>{{ user.email || '이메일 없음' }}</span>
              </div>
            </div>

            <div class="hr-member-org">
              <span>회사명</span>
              <strong>{{ user.companyName || '-' }}</strong>
            </div>

            <div class="hr-member-org">
              <span>부서</span>
              <strong>{{ user.department || '-' }}</strong>
            </div>

            <div class="hr-member-actions">
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
                class="hr-secondary hr-compact"
                :disabled="memberActionId !== ''"
                @click="handleMemberPasswordReset(user)"
              >
                비밀번호
              </button>



              <button
                type="button"
                class="hr-danger hr-compact"
                :disabled="memberActionId !== ''"
                @click="handleMemberDelete(user)"
              >
                삭제
              </button>
            </div>
          </article>
        </div>

        <div v-else class="hr-empty">관리할 수 있는 구성원이 없습니다.</div>
      </article>
    </div>

    <div v-if="showCreateModal" class="hr-modal-backdrop">
      <div class="hr-modal hr-modal--form">
        <div class="hr-modal__header hr-modal__header--split">
          <div>
            <p class="hr-eyebrow">Account creation</p>
            <h3>유저 생성</h3>
            <p>관리 범위 안에서 새 계정을 만들고 임시 비밀번호를 발급합니다.</p>
          </div>

          <button type="button" class="hr-secondary hr-compact" @click="closeCreateModal">닫기</button>
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
              <div v-if="authStore.isGeneralManager || authStore.isManager" class="hr-fixed">
                {{ createForm.companyName || '-' }}
              </div>
              <input v-else v-model.trim="createForm.companyName" type="text" maxlength="60" required placeholder="예: CALLOG" />
            </label>

            <label class="hr-field">
              <span>부서명</span>
              <div v-if="authStore.isManager" class="hr-fixed">{{ createForm.department || '-' }}</div>
              <input v-else v-model.trim="createForm.department" type="text" maxlength="40" required placeholder="예: 마케팅팀" />
            </label>
          </div>

          <div class="hr-grid">
            <label class="hr-field">
              <span>이름</span>
              <input v-model.trim="createForm.name" type="text" maxlength="30" required placeholder="예: 홍길동" />
            </label>

            <label class="hr-field">
              <span>이메일</span>
              <input v-model.trim="createForm.email" type="email" maxlength="254" placeholder="user@company.com" />
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

    <div v-if="roleConfirmModal.open" class="hr-modal-backdrop">
      <div class="hr-modal hr-modal--confirm">
        <div class="hr-modal__header">
          <p class="hr-eyebrow">Role change</p>
          <h3>권한을 변경할까요?</h3>
          <p>
            {{ roleConfirmModal.user?.name || roleConfirmModal.user?.id }} 사용자의 권한을
            {{ roleConfirmModal.nextRole }}로 변경합니다.
          </p>
        </div>

        <div class="hr-confirm-box">
          <div>
            <span>현재 권한</span>
            <strong>{{ getMemberRoleLabel(roleConfirmModal.user?.role) }}</strong>
          </div>
          <div>
            <span>변경 권한</span>
            <strong>{{ roleConfirmModal.nextRole }}</strong>
          </div>
        </div>

        <div class="hr-modal__actions">
          <button type="button" class="hr-primary" :disabled="status.roleLoading" @click="confirmMemberRoleChange">
            {{ status.roleLoading ? '처리 중' : '예' }}
          </button>
          <button type="button" class="hr-secondary" :disabled="status.roleLoading" @click="closeRoleConfirmModal">
            아니오
          </button>
        </div>
      </div>
    </div>

    <div v-if="resultModal.open" class="hr-modal-backdrop">
      <div class="hr-modal hr-modal--result">
        <div class="hr-modal__header">
          <p class="hr-eyebrow">Complete</p>
          <h3>{{ resultModal.title }}</h3>
          <p v-if="resultModal.description">{{ resultModal.description }}</p>
        </div>

        <dl v-if="resultModal.rows.length" class="hr-result-list">
          <div v-for="row in resultModal.rows" :key="row.label">
            <dt>{{ row.label }}</dt>
            <dd>{{ row.value }}</dd>
          </div>
        </dl>

        <div class="hr-modal__actions" :class="{ 'hr-modal__actions--single': !resultModal.copyText }">
          <button v-if="resultModal.copyText" type="button" class="hr-primary" @click="copyResult">
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
  --hr-panel: var(--surface-card);
  --hr-panel-muted: var(--surface-card-muted);
  --hr-panel-hover: var(--surface-control-hover);
  --hr-line: var(--line-soft);
  --hr-line-strong: var(--line-strong);
  --hr-text: var(--text-heading);
  --hr-muted: var(--text-muted);
  --hr-subtle: var(--subtle-text);
  --hr-accent: var(--accent-color);
  --hr-accent-strong: var(--accent-strong);
  --hr-danger: var(--danger-color);
  min-height: calc(100vh - var(--header-height, 56px));
  margin: calc(var(--density-page-padding, 24px) * -1);
  background: var(--surface-page);
  color: var(--hr-text);
  padding: 18px;
}

.hr-shell {
  display: grid;
  width: min(1180px, 100%);
  gap: 10px;
  margin: 0 auto;
}

.hr-topbar,
.hr-panel,
.hr-modal {
  border: 1px solid var(--hr-line);
  border-radius: 8px;
  background: var(--hr-panel);
  box-shadow: var(--shadow-soft);
}

.hr-topbar {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(180px, 0.45fr) minmax(420px, 1fr);
  align-items: center;
  padding: 14px;
}

.hr-title p,
.hr-eyebrow,
.hr-field span,
.hr-result-list dt {
  color: var(--hr-subtle);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hr-title h1 {
  margin-top: 3px;
  color: var(--hr-text);
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
}

.hr-toolbar {
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(220px, 1fr) auto 42px;
  align-items: center;
}

.hr-search {
  position: relative;
  min-width: 0;
}

.hr-search svg,
.hr-primary svg,
.hr-icon-button svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.3;
}

.hr-search svg {
  position: absolute;
  top: 50%;
  left: 12px;
  color: var(--hr-subtle);
  transform: translateY(-50%);
}

.hr-search input,
.hr-field input,
.hr-field select,
.hr-fixed {
  width: 100%;
  min-height: 42px;
  border: 1px solid var(--hr-line);
  border-radius: 8px;
  background: var(--surface-control);
  color: var(--hr-text);
  font-size: 13px;
  font-weight: 700;
  outline: none;
}

.hr-search input {
  padding: 0 12px 0 38px;
}

.hr-field input,
.hr-field select,
.hr-fixed {
  padding: 0 12px;
}

.hr-search input::placeholder,
.hr-field input::placeholder {
  color: var(--hr-subtle);
}

.hr-search input:focus,
.hr-field input:focus,
.hr-field select:focus {
  border-color: var(--hr-accent);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--hr-accent) 13%, transparent);
}

.hr-primary,
.hr-secondary,
.hr-danger,
.hr-icon-button {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
  transition:
    background 0.16s ease,
    border-color 0.16s ease,
    transform 0.16s ease;
}

.hr-primary {
  border: 1px solid transparent;
  background: linear-gradient(135deg, var(--hr-accent), var(--hr-accent-strong));
  color: #fff;
  padding: 0 14px;
}

.hr-secondary,
.hr-icon-button {
  border: 1px solid var(--hr-line);
  background: var(--surface-control);
  color: var(--text-body);
  padding: 0 12px;
}

.hr-danger {
  border: 1px solid color-mix(in srgb, var(--hr-danger) 36%, var(--hr-line));
  background: var(--danger-surface);
  color: var(--danger-text-strong);
  padding: 0 12px;
}

.hr-icon-button {
  width: 42px;
  padding: 0;
}

.hr-primary:hover,
.hr-secondary:hover,
.hr-danger:hover,
.hr-icon-button:hover {
  transform: translateY(-1px);
}

.hr-primary:disabled,
.hr-secondary:disabled,
.hr-danger:disabled,
.hr-icon-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
  transform: none;
}

.hr-panel {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.hr-panel__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hr-panel__summary span {
  border: 1px solid var(--hr-line);
  border-radius: 999px;
  background: var(--hr-panel-muted);
  color: var(--hr-muted);
  font-size: 12px;
  font-weight: 800;
  padding: 5px 10px;
}

.hr-member-list {
  display: grid;
  gap: 8px;
}

.hr-member-card {
  display: grid;
  gap: 12px;
  grid-template-columns: 48px minmax(210px, 1.15fr) minmax(140px, 0.7fr) minmax(120px, 0.55fr) auto;
  align-items: center;
  min-height: 76px;
  border: 1px solid var(--hr-line);
  border-radius: 8px;
  background: var(--hr-panel);
  padding: 10px;
}

.hr-member-card:hover {
  border-color: color-mix(in srgb, var(--hr-accent) 34%, var(--hr-line));
  background: var(--hr-panel-hover);
}

.hr-member-photo {
  display: inline-flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--hr-line);
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--hr-accent-strong);
  font-size: 17px;
  font-weight: 900;
}

.hr-member-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hr-member-identity,
.hr-member-org {
  min-width: 0;
}

.hr-member-name-row {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 7px;
  align-items: center;
}

.hr-member-name-row strong {
  color: var(--hr-text);
  font-size: 15px;
  font-weight: 900;
}

.hr-role-pill {
  border: 1px solid color-mix(in srgb, var(--hr-accent) 30%, var(--hr-line));
  border-radius: 999px;
  background: color-mix(in srgb, var(--hr-accent) 12%, transparent);
  color: var(--hr-accent-strong);
  font-size: 11px;
  font-weight: 900;
  padding: 3px 8px;
}

.hr-member-sub {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  color: var(--hr-muted);
  font-size: 12px;
  font-weight: 700;
}

.hr-member-sub span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.hr-member-org {
  display: grid;
  gap: 3px;
}

.hr-member-org span {
  color: var(--hr-subtle);
  font-size: 11px;
  font-weight: 800;
}

.hr-member-org strong {
  min-width: 0;
  color: var(--hr-text);
  font-size: 13px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.hr-member-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.hr-compact {
  min-height: 32px;
  padding: 0 10px;
  font-size: 12px;
}

.hr-empty,
.hr-alert,
.hr-preview,
.hr-result-list {
  border: 1px solid var(--hr-line);
  border-radius: 8px;
  background: var(--hr-panel-muted);
}

.hr-empty {
  color: var(--hr-muted);
  font-size: 14px;
  font-weight: 800;
  padding: 22px;
  text-align: center;
}

.hr-alert {
  border-color: color-mix(in srgb, var(--hr-danger) 34%, var(--hr-line));
  background: var(--danger-surface);
  color: var(--danger-text-strong);
  font-size: 13px;
  font-weight: 800;
  padding: 10px 12px;
}

.hr-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.38);
  padding: 20px;
  backdrop-filter: blur(10px);
}

.hr-modal {
  width: min(640px, 100%);
  max-height: calc(100vh - 44px);
  overflow: auto;
  padding: 18px;
}

.hr-modal--result {
  width: min(440px, 100%);
}

.hr-modal--confirm {
  width: min(420px, 100%);
}

.hr-modal__header {
  display: grid;
  gap: 7px;
  border-bottom: 1px solid var(--hr-line);
  margin-bottom: 14px;
  padding-bottom: 14px;
}

.hr-modal__header--split {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-start;
}

.hr-modal h3 {
  color: var(--hr-text);
  font-size: 20px;
  font-weight: 900;
}

.hr-modal__header p:last-child {
  color: var(--hr-muted);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.55;
}

.hr-form,
.hr-preview,
.hr-result-list,
.hr-modal__actions {
  display: grid;
  gap: 12px;
}

.hr-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.hr-field {
  display: grid;
  gap: 7px;
}

.hr-fixed {
  display: flex;
  align-items: center;
  color: var(--hr-muted);
}

.hr-preview {
  color: var(--hr-muted);
  font-size: 13px;
  padding: 12px;
}

.hr-preview strong {
  color: var(--hr-text);
}

.hr-confirm-box {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border: 1px solid var(--hr-line);
  border-radius: 8px;
  background: var(--hr-panel-muted);
  padding: 12px;
}

.hr-confirm-box div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.hr-confirm-box span {
  color: var(--hr-subtle);
  font-size: 11px;
  font-weight: 800;
}

.hr-confirm-box strong {
  color: var(--hr-text);
  font-size: 13px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.hr-result-list {
  margin: 0;
  padding: 14px;
}

.hr-result-list div {
  display: grid;
  gap: 8px;
  grid-template-columns: 120px minmax(0, 1fr);
}

.hr-result-list dd {
  min-width: 0;
  margin: 0;
  color: var(--hr-text);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.hr-modal__actions {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14px;
}

.hr-modal__actions--single {
  grid-template-columns: 1fr;
}

@media (max-width: 1100px) {
  .hr-member-card {
    grid-template-columns: 48px minmax(180px, 1fr) minmax(140px, 0.75fr);
  }

  .hr-member-actions {
    grid-column: 2 / -1;
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .hr-page {
    padding: 12px;
  }

  .hr-topbar,
  .hr-toolbar,
  .hr-grid,
  .hr-modal__header--split,
  .hr-confirm-box,
  .hr-result-list div,
  .hr-modal__actions {
    grid-template-columns: 1fr;
  }

  .hr-member-card {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .hr-member-org,
  .hr-member-actions {
    grid-column: 1 / -1;
  }

  .hr-member-actions {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
