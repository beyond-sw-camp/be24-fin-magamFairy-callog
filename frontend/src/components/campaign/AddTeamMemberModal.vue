<script setup>
import { computed, onMounted, ref } from 'vue'
import { getTeamCandidates, addTeamMembers } from '@/api/campaignMembers'

const props = defineProps({
  campaignId: { type: [String, Number], required: true },
})
const emit = defineEmits(['close', 'added'])

const candidates = ref([])
const selected = ref([])
const search = ref('')
const loading = ref(false)
const submitting = ref(false)
const error = ref('')

const ROLE_LABELS = {
  ROLE_ADMIN: '관리자',
  ROLE_GENERAL_MANAGER: '총괄 매니저',
  ROLE_MANAGER: '매니저',
  ROLE_USER: '담당자',
}

function formatRole(role) {
  if (!role) return '담당자'
  return ROLE_LABELS[role] ?? role.replace(/^ROLE_/, '')
}

function avatarInitial(value) {
  const trimmed = (value ?? '').trim()
  return trimmed ? trimmed.charAt(0).toUpperCase() : '?'
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return candidates.value
  return candidates.value.filter((c) =>
    (c.name || '').toLowerCase().includes(q)
    || (c.email || '').toLowerCase().includes(q)
    || (c.department || '').toLowerCase().includes(q),
  )
})

async function loadCandidates() {
  loading.value = true
  error.value = ''
  try {
    const res = await getTeamCandidates(props.campaignId)
    candidates.value = res.data?.data ?? []
  } catch (e) {
    error.value = '후보 목록을 불러오지 못했습니다.'
    candidates.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadCandidates)

function isSelected(userIdx) {
  return selected.value.includes(userIdx)
}

function toggle(userIdx) {
  const i = selected.value.indexOf(userIdx)
  if (i === -1) selected.value.push(userIdx)
  else selected.value.splice(i, 1)
}

async function submit() {
  if (selected.value.length === 0) return
  submitting.value = true
  error.value = ''
  try {
    await addTeamMembers(props.campaignId, selected.value)
    emit('added')
    emit('close')
  } catch (e) {
    const status = e?.response?.status
    if (status === 409) error.value = '이미 참여 중인 사용자가 있습니다.'
    else if (status === 403) error.value = '권한이 없습니다.'
    else error.value = '추가 중 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="cm-modal-overlay" role="presentation" @click.self="emit('close')">
      <section class="modal-shell" role="dialog" aria-modal="true">
        <div class="modal-header">
          <div>
            <div class="modal-header__eyebrow"><span>CAMPAIGN · 팀원 추가</span></div>
            <h2>팀원 추가</h2>
          </div>
          <button class="iconbtn btn-close" aria-label="닫기" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <input
            v-model="search"
            type="text"
            class="fld member-search"
            placeholder="이름·이메일·부서로 검색"
          />

          <div class="member-list" role="listbox" aria-label="팀원 후보 목록">
            <div v-if="loading" class="member-list__loading">
              <span class="member-spinner" aria-hidden="true"></span>
              <span>팀원 목록을 불러오는 중...</span>
            </div>
            <div v-else-if="error" class="member-list__empty">
              {{ error }}
              <button type="button" class="member-list__retry" @click="loadCandidates">다시 시도</button>
            </div>
            <div v-else-if="!candidates.length" class="member-list__empty">
              추가 가능한 팀원이 없습니다.
            </div>
            <div v-else-if="!filtered.length" class="member-list__empty">
              검색 결과가 없습니다.
            </div>
            <button
              v-for="user in filtered"
              v-else
              :key="user.userIdx"
              type="button"
              class="member-item"
              :class="{ selected: isSelected(user.userIdx) }"
              :aria-pressed="isSelected(user.userIdx)"
              @click="toggle(user.userIdx)"
            >
              <span class="member-item__left">
                <span class="member-item__avatar" aria-hidden="true">
                  <img v-if="user.profileImageUrl" :src="user.profileImageUrl" alt="" />
                  <span v-else>{{ avatarInitial(user.name) }}</span>
                </span>
                <span class="member-item__name">{{ user.name }}</span>
              </span>
              <span class="member-item__right">
                <span class="member-item__role">{{ formatRole(user.globalRole) }}</span>
                <span class="member-item__email">{{ user.email }}</span>
              </span>
              <span class="member-item__check" v-if="isSelected(user.userIdx)" aria-hidden="true">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
              </span>
            </button>
          </div>
        </div>

        <div class="modal-footer">
          <div class="modal-footer__hint">{{ selected.length }}명 선택됨</div>
          <div class="modal-footer__actions">
            <button type="button" class="btn btn--secondary" @click="emit('close')">취소</button>
            <button type="button" class="btn btn--primary" :disabled="submitting || selected.length === 0" @click="submit">
              추가
            </button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.cm-modal-overlay {
  position: fixed; inset: 0; z-index: 100;
  display: flex; align-items: center; justify-content: center;
  padding: 28px;
  background: rgba(15, 23, 42, 0.46);
}
.modal-shell {
  width: min(560px, 100%);
  max-height: min(720px, calc(100vh - 56px));
  display: flex; flex-direction: column;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.26);
  overflow: hidden;
  color: var(--text-primary);
}
.modal-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 22px 24px 12px; }
.modal-header__eyebrow span { font-size: 11px; font-weight: 700; letter-spacing: 0.08em; color: var(--muted-text); text-transform: uppercase; }
.modal-header h2 { margin: 4px 0 0; font-size: 20px; font-weight: 800; }
.iconbtn.btn-close {
  display: inline-grid; place-items: center;
  width: 34px; height: 34px;
  border: 1px solid var(--border-color); border-radius: var(--radius-sm);
  background: var(--panel-muted); color: var(--text-secondary);
  cursor: pointer;
}
.modal-body { padding: 12px 24px; overflow-y: auto; flex: 1; }

.fld {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--control-color, var(--panel-color));
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}
.member-search { margin-bottom: 10px; }

.member-list {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  max-height: 420px;
  overflow-y: auto;
}

.member-list__loading,
.member-list__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 16px;
  color: var(--muted-text);
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.member-list__retry {
  margin-left: 6px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 4px 10px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.member-list__retry:hover {
  background: var(--panel-muted);
  border-color: #7C3AED;
}

.member-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid color-mix(in srgb, #7C3AED 25%, transparent);
  border-top-color: #7C3AED;
  border-radius: 999px;
  animation: memberSpin 0.7s linear infinite;
}
@keyframes memberSpin {
  to { transform: rotate(360deg); }
}

.member-item {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: 0;
  border-bottom: 1px solid color-mix(in srgb, var(--border-color) 55%, transparent);
  background: transparent;
  color: var(--text-primary);
  padding: 12px 14px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;
}
.member-item:last-child { border-bottom: 0; }
.member-item:hover { background: var(--panel-muted); }
.member-item.selected {
  background: color-mix(in srgb, #7C3AED 10%, var(--panel-color));
}

.member-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.member-item__avatar {
  flex: 0 0 auto;
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: color-mix(in srgb, #7C3AED 14%, var(--panel-color));
  color: #6d28d9;
  font-size: 13px;
  font-weight: 800;
  overflow: hidden;
}
.member-item__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.member-item__name {
  font-size: 13px;
  font-weight: 800;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-item__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  min-width: 0;
}
.member-item__role {
  font-size: 11px;
  font-weight: 800;
  color: #6d28d9;
  background: color-mix(in srgb, #7C3AED 12%, transparent);
  border-radius: 999px;
  padding: 2px 9px;
  white-space: nowrap;
}
.member-item__email {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}

.member-item__check {
  flex: 0 0 auto;
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #7C3AED;
  color: #fff;
}

.modal-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 24px; border-top: 1px solid var(--border-color); }
.modal-footer__hint { font-size: 12px; font-weight: 700; color: var(--muted-text); }
.modal-footer__actions { display: flex; gap: 8px; }
.btn { display: inline-flex; align-items: center; gap: 4px; height: 36px; padding: 0 14px; border-radius: var(--radius-md); font-size: 13px; font-weight: 700; cursor: pointer; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: #7C3AED; color: #fff; border: 1px solid #7C3AED; }
.btn--primary:hover:not(:disabled) { background: #6d28d9; border-color: #6d28d9; }
.btn--secondary { background: var(--panel-color); color: var(--text-primary); border: 1px solid var(--border-color); }
.btn--secondary:hover:not(:disabled) { background: var(--panel-muted); }
</style>
