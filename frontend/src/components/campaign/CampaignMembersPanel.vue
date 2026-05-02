<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getCampaignMembers,
  updateMemberRole,
  removeMember,
} from '@/api/campaignMembers'
import CampaignMemberRow from './CampaignMemberRow.vue'
import CampaignMemberManagePopover from './CampaignMemberManagePopover.vue'
import AddTeamMemberModal from './AddTeamMemberModal.vue'
import InvitePartnerGmModal from './InvitePartnerGmModal.vue'

const props = defineProps({
  campaignId: { type: [String, Number], required: true },
})

const members = ref([])
const me = ref(null)
const isPm = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const showAddTeam = ref(false)
const showInvitePartner = ref(false)

const popoverFor = ref(null) // member object
const popoverPos = reactive({ top: 0, right: 0 })
const popoverActions = ref([])

const canAddTeam = computed(() => {
  const role = me.value?.campaignRole
  return role === 'MANAGER' || role === 'GENERAL_MANAGER'
})
const canInvitePartner = computed(() => canAddTeam.value && isPm.value)

async function fetchMembers() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getCampaignMembers(props.campaignId)
    const payload = res.data?.data ?? {}
    members.value = payload.members ?? []
    me.value = payload.me ?? null
    isPm.value = payload.organizationIsPm ?? false
  } catch (e) {
    errorMsg.value = '참여자 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchMembers)

function canManage(member) {
  const role = me.value?.campaignRole
  if (role === 'GENERAL_MANAGER') return true
  if (role === 'MANAGER') {
    return member.campaignRole === 'USER'
      && member.companyName === me.value?.companyName
      && member.department === me.value?.department
  }
  return false
}

function buildActions(member) {
  const role = me.value?.campaignRole
  if (role === 'GENERAL_MANAGER') {
    const sameDept = member.companyName === me.value?.companyName
      && member.department === me.value?.department
    const acts = []
    if (sameDept && member.campaignRole === 'USER') acts.push('promote')
    if (sameDept && member.campaignRole === 'MANAGER') acts.push('demote')
    acts.push('expel')
    return acts
  }
  if (role === 'MANAGER') return ['expel']
  return []
}

function openPopover({ member, target }) {
  const rect = target.getBoundingClientRect()
  popoverPos.top = rect.bottom + 4
  popoverPos.right = window.innerWidth - rect.right
  popoverActions.value = buildActions(member)
  popoverFor.value = member
}

function closePopover() {
  popoverFor.value = null
}

async function handleAction(action) {
  const member = popoverFor.value
  closePopover()
  if (!member) return
  const target = members.value.find((m) => m.idx === member.idx)
  if (!target) return

  if (action === 'expel') {
    const ok = window.confirm(`${target.name}님을 캠페인에서 제거합니다.\n작성한 자료실/레퍼런스/업무는 유지됩니다.\n계속하시겠습니까?`)
    if (!ok) return
    try {
      await removeMember(props.campaignId, target.idx)
      await fetchMembers()
    } catch (e) { errorMsg.value = '추방 처리 실패' }
    return
  }

  if (action === 'promote' || action === 'demote') {
    const next = action === 'promote' ? 'MANAGER' : 'USER'
    const verb = action === 'promote' ? '승격' : '강등'
    const ok = window.confirm(`${target.name}님을 ${next}로 ${verb}할까요?`)
    if (!ok) return
    try {
      await updateMemberRole(props.campaignId, target.idx, next)
      await fetchMembers()
    } catch (e) { errorMsg.value = `${verb} 처리 실패` }
  }
}
</script>

<template>
  <article class="panel members-panel">
    <div class="panel__header">
      <div>
        <span class="requirement-badge">CAMPAIGN_005</span>
        <h2>캠페인 참여자 관리</h2>
      </div>
      <div class="members-panel__actions">
        <button v-if="canAddTeam" type="button" class="btn btn--secondary" @click="showAddTeam = true">팀원 추가</button>
        <button v-if="canInvitePartner" type="button" class="btn btn--primary" @click="showInvitePartner = true">협력사 초대</button>
      </div>
    </div>

    <p v-if="errorMsg" class="members-panel__error">{{ errorMsg }}</p>

    <div v-if="loading" class="members-panel__empty">로딩 중...</div>
    <div v-else-if="members.length === 0" class="members-panel__empty">참여자가 없습니다.</div>
    <div v-else class="members-panel__list">
      <div class="members-panel__head">
        <span>이름 / 이메일</span>
        <span>소속 회사·부서</span>
        <span>역할</span>
        <span>참여일</span>
        <span>관리</span>
      </div>
      <CampaignMemberRow
        v-for="m in members"
        :key="m.idx"
        :member="m"
        :can-manage="canManage(m)"
        @manage="openPopover"
      />
    </div>

    <CampaignMemberManagePopover
      v-if="popoverFor"
      :position="popoverPos"
      :actions="popoverActions"
      @action="handleAction"
      @close="closePopover"
    />

    <AddTeamMemberModal
      v-if="showAddTeam"
      :campaign-id="campaignId"
      @close="showAddTeam = false"
      @added="fetchMembers"
    />

    <InvitePartnerGmModal
      v-if="showInvitePartner"
      :campaign-id="campaignId"
      @close="showInvitePartner = false"
      @invited="fetchMembers"
    />
  </article>
</template>

<style scoped>
.members-panel { padding: 0 0 16px; }
.panel__header { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid var(--border-color); }
.panel__header h2 { margin: 4px 0 0; font-size: 18px; font-weight: 700; }
.members-panel__actions { display: flex; gap: 8px; }
.members-panel__error { padding: 8px 20px; color: var(--color-danger); font-size: 12px; }
.members-panel__empty { padding: 32px; text-align: center; color: var(--muted-text); }
.members-panel__head {
  display: grid;
  grid-template-columns: 2fr 1.5fr 80px 100px 60px;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  font-size: 11px; font-weight: 700; letter-spacing: 0.04em;
  color: var(--muted-text); text-transform: uppercase;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-muted);
}
.btn { display: inline-flex; align-items: center; gap: 4px; height: 36px; padding: 0 14px; border-radius: var(--radius-md); font-size: 13px; font-weight: 700; cursor: pointer; }
.btn--primary { background: var(--color-primary-500); color: #fff; border: 1px solid var(--color-primary-500); }
.btn--primary:hover { background: var(--color-primary-600); }
.btn--secondary { background: var(--panel-color); color: var(--text-primary); border: 1px solid var(--border-color); }
.btn--secondary:hover { background: var(--panel-muted); }
</style>
