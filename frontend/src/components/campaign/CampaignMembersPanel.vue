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
import InvitePartnerGroupModal from './InvitePartnerGroupModal.vue'

const props = defineProps({
  campaignId: { type: [String, Number], required: true },
})

const members = ref([])
const me = ref(null)
const isPm = ref(false)
const pmOrganizationIdx = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const showAddTeam = ref(false)
const showInvitePartner = ref(false)
const showInvitePartnerGroup = ref(false)

const expandedMap = reactive({})
const manageModeMap = reactive({})
const expelLoading = reactive({})

const callerKey = computed(() => {
  if (!me.value) return null
  return me.value.organizationIdx != null
    ? `org:${me.value.organizationIdx}`
    : `name:${me.value.companyName ?? ''}`
})

function canShowGroupManage(g) {
  if (me.value?.campaignRole !== 'GENERAL_MANAGER') return false
  if (isPm.value) return true
  return g.key === callerKey.value
}

function toggleGroupManage(g) {
  manageModeMap[g.key] = !manageModeMap[g.key]
}

function canExpel(member) {
  if (!member || member.userIdx === me.value?.userIdx) return false
  return canManage(member)
}

async function expelMember(member) {
  const ok = window.confirm(`${member.name}님을 캠페인에서 제거합니다.\n작성한 자료실/레퍼런스/업무는 유지됩니다.\n계속하시겠습니까?`)
  if (!ok) return
  expelLoading[member.idx] = true
  try {
    await removeMember(props.campaignId, member.idx)
    await fetchMembers()
  } catch (e) {
    errorMsg.value = '추방 처리 실패'
  } finally {
    delete expelLoading[member.idx]
  }
}

const groupedMembers = computed(() => {
  if (!members.value.length) return []

  const groups = new Map()
  for (const m of members.value) {
    const key = m.organizationIdx != null ? `org:${m.organizationIdx}` : `name:${m.companyName ?? ''}`
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        organizationIdx: m.organizationIdx ?? null,
        organizationName: m.companyName ?? '미지정',
        members: [],
      })
    }
    groups.get(key).members.push(m)
  }

  const callerKey = me.value
    ? (me.value.organizationIdx != null ? `org:${me.value.organizationIdx}` : `name:${me.value.companyName ?? ''}`)
    : null

  const list = Array.from(groups.values())
  list.forEach((g) => {
    g.isPm = pmOrganizationIdx.value != null && g.organizationIdx === pmOrganizationIdx.value
    if (expandedMap[g.key] === undefined) expandedMap[g.key] = true
  })

  list.sort((a, b) => {
    if (a.isPm !== b.isPm) return a.isPm ? -1 : 1
    return (a.organizationName || '').localeCompare(b.organizationName || '', 'ko')
  })
  return list
})

function toggleGroup(g) {
  expandedMap[g.key] = !expandedMap[g.key]
}

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
    pmOrganizationIdx.value = payload.pmOrganizationIdx ?? null
  } catch (e) {
    errorMsg.value = '참여자 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchMembers)

function canManage(member) {
  const role = me.value?.campaignRole
  if (role === 'GENERAL_MANAGER') {
    // PM 조직 GM은 모든 멤버 관리 가능 (기존 정책 유지)
    if (isPm.value) return true
    // 그 외 GM은 같은 조직 멤버만
    return member.organizationIdx != null
      && member.organizationIdx === me.value?.organizationIdx
  }
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
        <button v-if="canInvitePartner" type="button" class="btn btn--primary" @click="showInvitePartnerGroup = true">
          그룹 초대
        </button>
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

      <section v-for="g in groupedMembers" :key="g.key" class="members-group">
        <div class="members-group__header-row">
          <button type="button" class="members-group__header" @click="toggleGroup(g)">
            <span class="members-group__chev" :class="{ 'is-open': expandedMap[g.key] }" aria-hidden="true">▶</span>
            <strong>{{ g.organizationName }}</strong>
            <small>· 인원 {{ g.members.length }}명</small>
            <span class="members-group__tag" :data-pm="g.isPm">{{ g.isPm ? 'PM' : '협력사' }}</span>
          </button>
          <button
            v-if="canShowGroupManage(g)"
            type="button"
            class="members-group__manage"
            :class="{ 'is-active': manageModeMap[g.key] }"
            @click="toggleGroupManage(g)"
          >
            {{ manageModeMap[g.key] ? '완료' : '관리' }}
          </button>
        </div>
        <div v-show="expandedMap[g.key]" class="members-group__body">
          <CampaignMemberRow
            v-for="m in g.members"
            :key="m.idx"
            :member="m"
            :can-manage="canManage(m)"
            :manage-mode="Boolean(manageModeMap[g.key])"
            :can-expel="canExpel(m)"
            :expel-loading="Boolean(expelLoading[m.idx])"
            @manage="openPopover"
            @expel="expelMember"
          />
        </div>
      </section>
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

    <InvitePartnerGroupModal
      v-if="showInvitePartnerGroup"
      :campaign-id="campaignId"
      @close="showInvitePartnerGroup = false"
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
.members-group { border-bottom: 1px solid var(--border-color); }
.members-group:last-child { border-bottom: none; }
.members-group__header { display: flex; align-items: center; gap: 8px; width: 100%; padding: 10px 16px; background: transparent; border: none; cursor: pointer; color: var(--text-primary); text-align: left; }
.members-group__header:hover { background: var(--panel-muted); }
.members-group__chev { display: inline-block; transition: transform 0.15s ease; font-size: 10px; color: var(--muted-text); }
.members-group__chev.is-open { transform: rotate(90deg); }
.members-group__header strong { font-size: 13px; font-weight: 700; }
.members-group__header small { font-size: 12px; color: var(--muted-text); }
.members-group__tag { margin-left: auto; padding: 2px 8px; font-size: 11px; font-weight: 700; border-radius: 999px; background: var(--panel-muted); color: var(--text-secondary); }
.members-group__tag[data-pm="true"] { background: color-mix(in srgb, var(--color-primary-500) 18%, transparent); color: var(--color-primary-600); }
.members-group__body { display: contents; }
.members-group__header-row { display: flex; align-items: center; gap: 8px; }
.members-group__header-row > .members-group__header { flex: 1; }
.members-group__manage { margin-right: 16px; padding: 4px 12px; font-size: 12px; font-weight: 700; border: 1px solid var(--border-color); border-radius: var(--radius-sm); background: var(--panel-color); color: var(--text-primary); cursor: pointer; }
.members-group__manage:hover { background: var(--panel-muted); }
.members-group__manage.is-active { background: var(--color-primary-500); color: #fff; border-color: var(--color-primary-500); }
</style>
