<script setup>
import { computed, onMounted, ref } from 'vue'
import { getPartnerGmCandidates, invitePartnerGm } from '@/api/campaignMembers'

const props = defineProps({
  campaignId: { type: [String, Number], required: true },
})
const emit = defineEmits(['close', 'invited'])

const candidates = ref([])
const search = ref('')
const loading = ref(false)
const submittingId = ref(null)
const error = ref('')

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return candidates.value
  return candidates.value.filter((c) =>
    (c.name || '').toLowerCase().includes(q)
    || (c.organizationName || '').toLowerCase().includes(q)
    || (c.email || '').toLowerCase().includes(q)
  )
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getPartnerGmCandidates(props.campaignId)
    candidates.value = res.data?.data ?? []
  } catch (e) {
    error.value = '후보 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
})

async function invite(c) {
  submittingId.value = c.userIdx
  error.value = ''
  try {
    await invitePartnerGm(props.campaignId, c.userIdx)
    emit('invited')
    emit('close')
  } catch (e) {
    const status = e?.response?.status
    if (status === 409) error.value = '이미 참여 중입니다.'
    else if (status === 403) error.value = '권한이 없습니다.'
    else error.value = '초대 중 오류가 발생했습니다.'
  } finally {
    submittingId.value = null
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="cm-modal-overlay" role="presentation" @click.self="emit('close')">
      <section class="modal-shell" role="dialog" aria-modal="true">
        <div class="modal-header">
          <div>
            <div class="modal-header__eyebrow"><span>CAMPAIGN · 협력사 초대</span></div>
            <h2>협력사 GM 초대</h2>
          </div>
          <button class="iconbtn btn-close" aria-label="닫기" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <input v-model="search" class="fld" placeholder="조직명·이름으로 검색" />

          <div v-if="loading" class="cm-modal-empty">로딩 중...</div>
          <div v-else-if="filtered.length === 0" class="cm-modal-empty">초대 가능한 협력사 GM이 없습니다.</div>
          <ul v-else class="cm-modal-list">
            <li v-for="c in filtered" :key="c.userIdx">
              <span class="cm-modal-item">
                <strong>{{ c.organizationName || c.companyName }}</strong>
                <small>{{ c.name }} · {{ c.email }}</small>
              </span>
              <button type="button" class="btn btn--primary cm-invite-btn" :disabled="submittingId === c.userIdx" @click="invite(c)">
                초대
              </button>
            </li>
          </ul>

          <p v-if="error" class="cm-modal-error">{{ error }}</p>
        </div>

        <div class="modal-footer">
          <div class="modal-footer__hint" />
          <div class="modal-footer__actions">
            <button type="button" class="btn btn--secondary" @click="emit('close')">닫기</button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
/* Reuse the same overlay/shell styles — copy from AddTeamMemberModal for self-contained component */
.cm-modal-overlay { position: fixed; inset: 0; z-index: 100; display: flex; align-items: center; justify-content: center; padding: 28px; background: rgba(15, 23, 42, 0.46); }
.modal-shell { width: min(560px, 100%); max-height: min(720px, calc(100vh - 56px)); display: flex; flex-direction: column; border: 1px solid var(--border-color); border-radius: var(--radius-lg); background: var(--panel-color); box-shadow: 0 24px 70px rgba(15, 23, 42, 0.26); overflow: hidden; color: var(--text-primary); }
.modal-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 22px 24px 12px; }
.modal-header__eyebrow span { font-size: 11px; font-weight: 700; letter-spacing: 0.08em; color: var(--muted-text); text-transform: uppercase; }
.modal-header h2 { margin: 4px 0 0; font-size: 20px; font-weight: 800; }
.iconbtn.btn-close { display: inline-grid; place-items: center; width: 34px; height: 34px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); background: var(--panel-muted); color: var(--text-secondary); cursor: pointer; }
.modal-body { padding: 12px 24px; overflow-y: auto; flex: 1; }
.fld { width: 100%; height: 36px; padding: 0 12px; font-size: 13px; color: var(--text-primary); background: var(--control-color, var(--panel-color)); border: 1px solid var(--border-color); border-radius: var(--radius-md); margin-bottom: 12px; }
.cm-modal-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 4px; }
.cm-modal-list li { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 10px 12px; border: 1px solid var(--border-color); border-radius: var(--radius-md); }
.cm-modal-item { display: flex; flex-direction: column; }
.cm-modal-item strong { font-size: 13px; color: var(--text-primary); }
.cm-modal-item small { font-size: 11px; color: var(--muted-text); }
.cm-modal-empty { padding: 24px; text-align: center; color: var(--muted-text); }
.cm-modal-error { color: var(--color-danger); font-size: 12px; margin-top: 8px; }
.modal-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 24px; border-top: 1px solid var(--border-color); }
.modal-footer__hint { font-size: 12px; color: var(--muted-text); }
.modal-footer__actions { display: flex; gap: 8px; }
.btn { display: inline-flex; align-items: center; gap: 4px; height: 32px; padding: 0 12px; border-radius: var(--radius-md); font-size: 12px; font-weight: 700; cursor: pointer; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary { background: var(--color-primary-500); color: #fff; border: 1px solid var(--color-primary-500); }
.btn--primary:hover:not(:disabled) { background: var(--color-primary-600); }
.btn--secondary { background: var(--panel-color); color: var(--text-primary); border: 1px solid var(--border-color); }
.btn--secondary:hover:not(:disabled) { background: var(--panel-muted); }
.cm-invite-btn { flex-shrink: 0; }
</style>
