<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { SignupRequest } from '@/authApi'

const router = useRouter()

const form = reactive({
  companyName: '',
  department: '',
  name: '',
  email: '',
  phone: '',
})

const isSubmitting = ref(false)
const errorMessage = ref('')
const createdAccount = ref(null)

function resolveErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.detail ||
    error?.message ||
    '회원가입을 완료하지 못했습니다. 입력 정보를 확인해 주세요.'
  )
}

function validateForm() {
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (!form.companyName.trim() || !form.department.trim() || !form.name.trim() || !form.email.trim()) {
    return '회사명, 부서/팀명, 담당자명, 이메일을 모두 입력해 주세요.'
  }

  if (!emailPattern.test(form.email.trim())) {
    return '올바른 이메일 형식으로 입력해 주세요.'
  }

  return ''
}

async function handleSignup() {
  if (isSubmitting.value) {
    return
  }

  errorMessage.value = validateForm()
  if (errorMessage.value) {
    return
  }

  isSubmitting.value = true

  try {
    createdAccount.value = await SignupRequest({
      companyName: form.companyName.trim(),
      department: form.department.trim(),
      name: form.name.trim(),
      email: form.email.trim(),
      phone: form.phone.trim() || null,
    })
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function goToLogin() {
  router.push({
    name: 'login',
    query: {
      registered: 'partner',
      id: createdAccount.value?.id || form.email.trim(),
    },
  })
}
</script>

<template>
  <section class="partner-signup-view min-h-screen px-6 py-10">
    <div class="mx-auto flex min-h-screen max-w-5xl flex-col items-center justify-center">
      <div class="mb-8 flex flex-col items-center text-center">
        <div class="partner-signup-mark mb-4 inline-flex h-16 w-16 items-center justify-center">
          <svg class="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 12h6m-3-3v6M4 7h16M6 7v12h12V7M9 7V5h6v2"
            />
          </svg>
        </div>
        <h1 class="partner-signup-title text-4xl font-bold tracking-tight">CALLOG</h1>
        <p class="partner-signup-copy mt-3 text-sm">
          외부 제휴사 계정을 만들고 캠페인 협업 흐름에 참여하세요.
        </p>
      </div>

      <div class="w-full max-w-3xl">
        <article class="partner-signup-card p-6 shadow-sm md:p-8">
          <template v-if="!createdAccount">
            <div class="mb-6">
              <p class="partner-signup-eyebrow text-xs font-semibold uppercase tracking-[0.24em]">
                Partner access
              </p>
              <h2 class="partner-signup-heading mt-2 text-2xl font-bold tracking-tight">
                제휴사 회원가입
              </h2>
              <p class="partner-signup-copy mt-2 text-sm">
                회사와 담당자 정보를 입력하면 이메일로 로그인할 수 있는 계정이 생성됩니다.
              </p>
            </div>

            <p
              v-if="errorMessage"
              class="partner-signup-alert partner-signup-alert--danger mb-4 px-4 py-3 text-sm font-medium"
            >
              {{ errorMessage }}
            </p>

            <form class="partner-signup-form" @submit.prevent="handleSignup">
              <label class="partner-signup-field">
                <span>회사명</span>
                <input
                  v-model.trim="form.companyName"
                  type="text"
                  autocomplete="organization"
                  placeholder="예: 스타브랜드 코리아"
                />
              </label>

              <label class="partner-signup-field">
                <span>부서 / 팀명</span>
                <input
                  v-model.trim="form.department"
                  type="text"
                  autocomplete="organization-title"
                  placeholder="예: 영업팀"
                />
              </label>

              <label class="partner-signup-field">
                <span>담당자명</span>
                <input
                  v-model.trim="form.name"
                  type="text"
                  autocomplete="name"
                  placeholder="예: 홍길동"
                />
              </label>

              <label class="partner-signup-field">
                <span>업무 이메일</span>
                <input
                  v-model.trim="form.email"
                  type="email"
                  autocomplete="email"
                  placeholder="partner@company.com"
                />
              </label>

              <label class="partner-signup-field">
                <span>연락처</span>
                <input
                  v-model.trim="form.phone"
                  type="tel"
                  autocomplete="tel"
                  placeholder="010-0000-0000"
                />
              </label>

              <div class="partner-signup-actions">
                <RouterLink :to="{ name: 'login' }" class="partner-signup-link">
                  로그인으로 돌아가기
                </RouterLink>
                <button type="submit" :disabled="isSubmitting" class="partner-signup-submit">
                  <span v-if="!isSubmitting">회원가입</span>
                  <span v-else>가입 처리 중</span>
                </button>
              </div>
            </form>
          </template>

          <template v-else>
            <div class="partner-signup-success">
              <p class="partner-signup-eyebrow text-xs font-semibold uppercase tracking-[0.24em]">
                Account created
              </p>
              <h2 class="partner-signup-heading mt-2 text-2xl font-bold tracking-tight">
                회원가입이 완료되었습니다
              </h2>
              <p class="partner-signup-copy mt-2 text-sm">
                아래 로그인 ID와 임시 비밀번호로 CALLOG에 접속할 수 있습니다.
              </p>

              <dl class="partner-signup-result">
                <div>
                  <dt>회사</dt>
                  <dd>{{ createdAccount.companyName }}</dd>
                </div>
                <div>
                  <dt>담당자</dt>
                  <dd>{{ createdAccount.name }}</dd>
                </div>
                <div>
                  <dt>로그인 ID</dt>
                  <dd>{{ createdAccount.id }}</dd>
                </div>
                <div>
                  <dt>임시 비밀번호</dt>
                  <dd>{{ createdAccount.password }}</dd>
                </div>
              </dl>

              <button type="button" class="partner-signup-submit" @click="goToLogin">
                로그인하러 가기
              </button>
            </div>
          </template>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.partner-signup-view {
  background: var(--surface-page);
  color: var(--text-body);
  transition:
    background var(--transition-normal),
    color var(--transition-normal);
}

.partner-signup-mark {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--accent-strong);
  color: var(--toggle-thumb);
}

.partner-signup-title,
.partner-signup-heading {
  color: var(--text-heading);
}

.partner-signup-card {
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-soft);
}

.partner-signup-eyebrow,
.partner-signup-copy,
.partner-signup-field span,
.partner-signup-result dt {
  color: var(--text-muted);
}

.partner-signup-alert {
  border-radius: var(--radius-md);
}

.partner-signup-alert--danger {
  border: 1px solid color-mix(in srgb, var(--danger-color) 30%, var(--line-soft));
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.partner-signup-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.partner-signup-field {
  display: grid;
  gap: 8px;
}

.partner-signup-field span {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.partner-signup-field input {
  width: 100%;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 500;
  outline: none;
  padding: 14px 16px;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.partner-signup-field input::placeholder {
  color: var(--text-muted);
}

.partner-signup-field input:focus {
  border-color: var(--line-strong);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent-color) 14%, transparent);
}

.partner-signup-actions {
  display: flex;
  grid-column: 1 / -1;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 4px;
}

.partner-signup-link,
.partner-signup-submit {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 700;
  padding: 0 18px;
  text-decoration: none;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.partner-signup-link {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-heading);
}

.partner-signup-link:hover {
  border-color: var(--line-strong);
  background: var(--control-focus-color);
  color: var(--accent-color);
}

.partner-signup-submit {
  border: 0;
  background: var(--accent-strong);
  color: var(--toggle-thumb);
  cursor: pointer;
}

.partner-signup-submit:hover:not(:disabled) {
  background: var(--accent-color);
}

.partner-signup-submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.partner-signup-success {
  display: grid;
  gap: 18px;
}

.partner-signup-result {
  display: grid;
  gap: 10px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  padding: 16px;
}

.partner-signup-result div {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
}

.partner-signup-result dt {
  font-size: 13px;
  font-weight: 700;
}

.partner-signup-result dd {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

@media (max-width: 720px) {
  .partner-signup-form {
    grid-template-columns: 1fr;
  }

  .partner-signup-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .partner-signup-link,
  .partner-signup-submit {
    width: 100%;
  }

  .partner-signup-result div {
    grid-template-columns: 1fr;
  }
}
</style>
