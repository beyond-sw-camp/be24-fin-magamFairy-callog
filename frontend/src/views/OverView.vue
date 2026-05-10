<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainCalendar from '@/components/overview/MainCalendar.vue'
import MainTable from '@/components/overview/MainTable.vue'
import MainTimeline from '@/components/overview/MainTimeline.vue'
import WeekCalendar from '@/components/overview/WeekCalendar.vue'
import AgendaCalendar from '@/components/overview/AgendaCalendar.vue'
import EventDetailPanel from '@/components/overview/EventDetailPanel.vue'
import DayEventsModal from '@/components/overview/DayEventsModal.vue'
import QuickAddPopover from '@/components/overview/QuickAddPopover.vue'
import CalendarFilterChips from '@/components/overview/CalendarFilterChips.vue'
import CalendarSidebar from '@/components/overview/CalendarSidebar.vue'
import CommandPalette from '@/components/overview/CommandPalette.vue'
import ShortcutCheatsheet from '@/components/overview/ShortcutCheatsheet.vue'
import { usePlannerStore } from '@/stores/planner'
import { usePartnershipsStore } from '@/stores/partnerships'
import { useAuthStore } from '@/stores/useAuthStore'
import { useTeamTaskStore } from '@/stores/teamTask'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirmDialog'
import { ListCalendarEvents, UpdateCampaign, UpdateCampaignIntro } from '@/api/campaigns'
import { UpdateMilestone, UpdateTask, CreateMilestone, CreateTask } from '@/api/teamboard'

const store = usePlannerStore()
const partnershipStore = usePartnershipsStore()
const authStore = useAuthStore()
const teamTaskStore = useTeamTaskStore()
const toast = useToastStore()
const confirm = useConfirmStore()
const route = useRoute()
const router = useRouter()

/* ─── URL 쿼리 동기화 + localStorage 토글 ─── */
const TOGGLES_KEY = 'overviewToggles'
function readQueryString(key, fallback = '') {
  const v = route.query[key]
  return typeof v === 'string' ? v : fallback
}
function readToggles() {
  try {
    const raw = window.localStorage.getItem(TOGGLES_KEY)
    if (raw) return { campaign: true, deadline: true, milestone: true, task: true, ...JSON.parse(raw) }
  } catch { /* ignore */ }
  return { campaign: true, deadline: true, milestone: true, task: true }
}
const isDark = computed(() => store.theme === 'dark')

/* ─── 뷰 / 검색 / 필터 (URL 쿼리에서 초기값 복원) ─── */
const searchQuery = ref(readQueryString('q'))
const currentView = ref(readQueryString('view', 'calendar'))
const anchorDate = ref(readQueryString('date') ? new Date(readQueryString('date')) : new Date())
const filter = ref({ mineOnly: readQueryString('mine') === '1' })

const viewOptions = [
  { id: 'calendar', name: '월간',     icon: 'calendar_month' },
  { id: 'week',     name: '주간',     icon: 'view_week' },
  { id: 'agenda',   name: '아젠다',   icon: 'view_agenda' },
  { id: 'timeline', name: '타임라인', icon: 'timeline' },
  { id: 'table',    name: '테이블',   icon: 'table_rows' },
]

/* ─── 사이드바 토글 (4종 일정, localStorage에서 복원) ─── */
const toggles = ref(readToggles())

/* ─── 이벤트 데이터 (백엔드 API에서만 로드) ─── */
const campaigns = ref([])      // 캠페인 목록 (start ~ end)
const intros = ref([])         // 캠페인 인트로 (recruitDeadline)
const milestones = ref([])     // 마일스톤 (start ~ end)
const loading = ref(false)     // 초기 로드 상태
const loadError = ref(null)    // 로드 에러 메시지

function isoOf(dt) {
  if (!dt) return null
  const d = typeof dt === 'string' ? new Date(dt) : dt
  if (Number.isNaN(d.getTime())) return null
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/* URL 쿼리 자동 동기화 */
function fmtIsoDate(d) {
  if (!(d instanceof Date) || Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
watch([currentView, anchorDate, searchQuery, () => filter.value.mineOnly], () => {
  const q = {}
  if (currentView.value && currentView.value !== 'calendar') q.view = currentView.value
  const dStr = fmtIsoDate(anchorDate.value)
  if (dStr) q.date = dStr
  if (searchQuery.value) q.q = searchQuery.value
  if (filter.value.mineOnly) q.mine = '1'
  router.replace({ query: q }).catch(() => { /* ignore navigation duplication */ })
}, { deep: true })

/* 토글 localStorage 저장 */
watch(toggles, (val) => {
  try { window.localStorage.setItem(TOGGLES_KEY, JSON.stringify(val)) } catch { /* ignore */ }
}, { deep: true })

async function loadAll() {
  loading.value = true
  loadError.value = null
  try {
    // 백엔드 일괄 API — 캠페인 + 모집마감 + 마일스톤을 1회 호출로 모두 가져옴
    const data = await ListCalendarEvents({ scope: 'mine' })
    const arr = Array.isArray(data?.campaigns) ? data.campaigns : []
    campaigns.value = arr.map(c => ({
      ...c,
      start: c.startDate,
      end: c.endDate,
      title: c.title ?? c.name,
      id: c.publicId,        // 캠페인 controller가 사용하는 publicId
      idx: c.idx,            // numeric primary key (식별용)
    }))
    intros.value = (data?.deadlines ?? []).map(d => ({
      campaignId: d.campaignPublicId,
      campaignIdx: d.campaignIdx,
      campaignName: d.campaignName,
      recruitDeadline: d.recruitDeadline,
    }))
    milestones.value = (data?.milestones ?? []).map(m => ({
      idx: m.idx,
      name: m.name,
      startDate: m.startDate,
      endDate: m.endDate,
      campaignId: m.campaignPublicId,
      campaignIdx: m.campaignIdx,
      campaignName: m.campaignName,
    }))
    teamTaskStore.fetch()
  } catch (error) {
    console.error('캘린더 데이터 로드 실패', error)
    loadError.value = error?.response?.data?.message || error?.message || '캘린더 데이터를 불러오지 못했습니다.'
    toast.error(loadError.value, '로드 실패')
    campaigns.value = []
    intros.value = []
    milestones.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

/* ─── 4종 이벤트로 변환 ─── */
function colorByType(type) {
  return {
    campaign: 'evt-violet',
    deadline: 'evt-amber',
    milestone: 'evt-blue',
    task: 'evt-emerald',
  }[type] ?? 'evt-violet'
}

const campaignEvents = computed(() => campaigns.value
  .filter(c => c.start && c.end)
  .map(c => ({
    id: `cmp-${c.id ?? c.idx}`,
    type: 'campaign',
    title: c.title ?? c.name ?? '제목 없음',
    start: c.start, end: c.end,
    projectManager: c.ownerName ?? c.projectManager,
    campaignId: c.id ?? c.idx,
    myCampaignRole: c.myCampaignRole ?? null,
    organizationIsPm: c.organizationIsPm ?? false,
    // Sidebar2와 동일한 캠페인 컬러 (없으면 기본 보라)
    customColor: c.color || '#8B5CF6',
    icon: c.icon || '',
    colorClass: colorByType('campaign'),
  })))

const deadlineEvents = computed(() => intros.value
  .filter(i => i.recruitDeadline)
  .map(i => ({
    id: `dl-${i.campaignId}`,
    type: 'deadline',
    title: `⏰ 모집 마감: ${i.campaignName}`,
    start: isoOf(i.recruitDeadline),
    end: isoOf(i.recruitDeadline),
    campaignId: i.campaignId,
    colorClass: colorByType('deadline'),
  })))

const milestoneEvents = computed(() => milestones.value
  .filter(m => m.startDate || m.endDate)
  .map(m => ({
    id: `ms-${m.idx}`,
    type: 'milestone',
    title: `🚩 ${m.name}`,
    start: isoOf(m.startDate ?? m.endDate),
    end: isoOf(m.endDate ?? m.startDate),
    campaignId: m.campaignId,
    projectManager: m.campaignName,
    colorClass: colorByType('milestone'),
  })))

const taskEvents = computed(() => {
  const myIdx = authStore.user?.idx
  return (teamTaskStore.tasks ?? [])
    .filter(t => t.dueDate && (!myIdx || t.assignee?.idx === myIdx))
    .map(t => ({
      id: `tsk-${t.idx}`,
      type: 'task',
      title: `✅ ${t.name}`,
      start: isoOf(t.dueDate),
      end: isoOf(t.dueDate),
      projectManager: t.assignee?.name ?? '',
      colorClass: colorByType('task'),
    }))
})

const formattedEvents = computed(() => [
  ...campaignEvents.value,
  ...deadlineEvents.value,
  ...milestoneEvents.value,
  ...taskEvents.value,
])

const filteredEvents = computed(() => {
  let arr = formattedEvents.value
  // 사이드바 토글 — type별 ON/OFF
  arr = arr.filter(e => toggles.value[e.type] !== false)
  // 내 캠페인만 — 현재 사용자가 ownerName인 항목 + 그 캠페인에 속한 마감/마일스톤
  if (filter.value.mineOnly) {
    const myName = authStore.user?.name
    if (!myName) { arr = [] }
    else {
      const myCampaignIds = new Set(
        campaigns.value.filter(c => c.ownerName === myName).map(c => c.id)
      )
      arr = arr.filter(e =>
        (e.type === 'campaign' && e.projectManager === myName) ||
        (e.campaignId && myCampaignIds.has(e.campaignId)) ||
        (e.type === 'task' && e.projectManager === myName)
      )
    }
  }
  // 검색
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    arr = arr.filter(e =>
      (e.title ?? '').toLowerCase().includes(q) ||
      (e.projectManager ?? '').toLowerCase().includes(q),
    )
  }
  return arr
})

/* ─── 이벤트 패널 / 모달 / Quick-add 상태 ─── */
const selectedEvent = ref(null)
const dayModal = ref({ date: '', events: [] })
const quickAdd = ref({ date: '', position: { x: 0, y: 0 } })

function onEventClick(ev) {
  selectedEvent.value = ev
}
function onDayClick({ date, event: mouseEvt }) {
  // 이벤트 막대 클릭과 겹치면 day-click 무시 (mouseEvt.target가 .main-cal__event 면 stop된 상태이므로 도달 X)
  // 빈 셀 클릭 → quick-add
  const x = mouseEvt?.clientX ?? window.innerWidth / 2
  const y = mouseEvt?.clientY ?? window.innerHeight / 2
  // 화면 가장자리 보정
  const popoverW = 280
  const popoverH = 140
  const adjX = Math.min(x - popoverW / 2, window.innerWidth - popoverW - 16)
  const adjY = Math.min(y, window.innerHeight - popoverH - 16)
  quickAdd.value = { date, position: { x: Math.max(8, adjX), y: Math.max(8, adjY) } }
}
function onMoreClick({ date, events: dayEvents }) {
  // 셀의 raw 이벤트 → filteredEvents에서 매칭 (colorClass/customColor가 이미 들어있음)
  const enriched = dayEvents.map(raw => {
    const formatted = formattedEvents.value.find(f => f.id === raw.id || f.campaignId === raw.campaignId)
    return formatted ?? raw
  })
  dayModal.value = { date, events: enriched }
}
function closeDayModal() { dayModal.value = { date: '', events: [] } }
function closePanel() { selectedEvent.value = null }
function closeQuickAdd() { quickAdd.value = { date: '', position: { x: 0, y: 0 } } }

/* ─── 권한 체크 ─── */
function canEdit(event) {
  if (!authStore.user) return false
  const camp = campaigns.value.find(c => c.id === event.campaignId)
  if (!camp) return event.type === 'task'  // 캠페인 정보 없는 task는 본인 dueDate만
  const role = camp.myCampaignRole
  // GENERAL_MANAGER는 모두 가능, MANAGER는 마일스톤/업무, 본인 task는 항상
  if (event.type === 'campaign' || event.type === 'deadline') {
    return role === 'GENERAL_MANAGER'
  }
  if (event.type === 'milestone') {
    return role === 'GENERAL_MANAGER' || role === 'MANAGER'
  }
  if (event.type === 'task') {
    return role === 'GENERAL_MANAGER' || role === 'MANAGER' || event.projectManager === authStore.user.name
  }
  return false
}

/* ─── 드래그앤드롭 → 백엔드 update (변경 전 확인 다이얼로그) ─── */
function shortDate(s) {
  if (!s) return ''
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return s
  return `${d.getMonth() + 1}/${String(d.getDate()).padStart(2, '0')}`
}

async function onEventDrop({ event, newStart, newEnd }) {
  if (!canEdit(event)) {
    toast.warn('이 일정을 수정할 권한이 없습니다.')
    return
  }
  // 변경 사항 없으면 무시
  if (event.start === newStart && event.end === newEnd) return

  const TYPE_LABEL = { campaign: '캠페인', milestone: '마일스톤', deadline: '모집 마감', task: '업무' }
  const label = TYPE_LABEL[event.type] ?? '일정'
  const oldRange = event.start === event.end ? shortDate(event.start) : `${shortDate(event.start)} ~ ${shortDate(event.end)}`
  const newRange = newStart === newEnd ? shortDate(newStart) : `${shortDate(newStart)} ~ ${shortDate(newEnd)}`

  const ok = await confirm.ask({
    title: `${label} 일정 변경`,
    message: `'${event.title}'\n\n${oldRange}  →  ${newRange}\n\n변경하시겠습니까?`,
    confirmText: '네, 변경',
    cancelText: '아니오',
    tone: 'primary',
  })
  if (!ok) return

  if (event.type === 'campaign') {
    await updateCampaignDate(event, newStart, newEnd)
  } else if (event.type === 'milestone') {
    await updateMilestoneDate(event, newStart, newEnd)
  } else if (event.type === 'deadline') {
    await updateDeadlineDate(event, newStart)
  } else if (event.type === 'task') {
    await updateTaskDate(event, newStart)
  }
}

async function updateCampaignDate(event, newStart, newEnd) {
  const idx = campaigns.value.findIndex(c => c.id === event.campaignId)
  if (idx < 0) return
  const prev = { start: campaigns.value[idx].start, end: campaigns.value[idx].end }
  campaigns.value[idx] = { ...campaigns.value[idx], start: newStart, end: newEnd }  // optimistic
  try {
    await UpdateCampaign(event.campaignId, { startDate: newStart, endDate: newEnd })
    toast.success(`'${event.title}' 일정 이동`, '캠페인')
  } catch (e) {
    campaigns.value[idx] = { ...campaigns.value[idx], ...prev }  // rollback
    toast.error(e?.response?.data?.message || '캠페인 일정 변경에 실패했습니다.')
  }
}

async function updateMilestoneDate(event, newStart, newEnd) {
  const ms = milestones.value.find(m => `ms-${m.idx}` === event.id)
  if (!ms) return
  const prev = { startDate: ms.startDate, endDate: ms.endDate }
  ms.startDate = newStart
  ms.endDate = newEnd
  try {
    await UpdateMilestone(ms.idx, {
      name: event.title.replace(/^🚩\s*/, ''),
      startDate: `${newStart}T00:00:00`,
      endDate: `${newEnd}T23:59:59`,
    })
    toast.success(`'${ms.name}' 일정 이동`, '마일스톤')
  } catch (e) {
    Object.assign(ms, prev)
    toast.error(e?.response?.data?.message || '마일스톤 일정 변경에 실패했습니다.')
  }
}

async function updateDeadlineDate(event, newDate) {
  const intro = intros.value.find(i => i.campaignId === event.campaignId)
  if (!intro) return
  const prev = intro.recruitDeadline
  intro.recruitDeadline = `${newDate}T23:59:59`
  try {
    await UpdateCampaignIntro(event.campaignId, { recruitDeadline: intro.recruitDeadline })
    toast.success(`'${intro.campaignName}' 모집 마감 변경`, '모집 마감')
  } catch (e) {
    intro.recruitDeadline = prev
    toast.error(e?.response?.data?.message || '모집 마감 변경에 실패했습니다.')
  }
}

async function updateTaskDate(event, newDate) {
  const taskIdx = Number(event.id.replace('tsk-', ''))
  const task = teamTaskStore.tasks.find(t => t.idx === taskIdx)
  if (!task) return
  const prev = task.dueDate
  task.dueDate = `${newDate}T23:59:59`
  try {
    await UpdateTask(taskIdx, { dueDate: task.dueDate })
    toast.success(`'${task.name}' 마감일 변경`, '내 업무')
  } catch (e) {
    task.dueDate = prev
    toast.error(e?.response?.data?.message || '업무 마감일 변경에 실패했습니다.')
  }
}

/* ─── Quick-add ─── */
async function createTaskFromQuick({ title, date, campaignId }) {
  try {
    await CreateTask(campaignId, {
      name: title,
      dueDate: `${date}T23:59:59`,
      status: 'TODO',
    })
    toast.success(`'${title}' 업무 생성`, '업무 추가')
    teamTaskStore.fetch()
  } catch (e) {
    toast.error(e?.response?.data?.message || '업무 생성에 실패했습니다.')
  }
}

async function createMilestoneFromQuick({ title, date, campaignId }) {
  try {
    await CreateMilestone(campaignId, {
      name: title,
      startDate: `${date}T00:00:00`,
      endDate: `${date}T23:59:59`,
    })
    toast.success(`'${title}' 마일스톤 생성`, '마일스톤 추가')
    loadAll()  // milestones 재로드
  } catch (e) {
    toast.error(e?.response?.data?.message || '마일스톤 생성에 실패했습니다.')
  }
}

function openCampaignFullModal({ date }) {
  // 캠페인 생성 모달 라우팅 — 별도 페이지/모달이 있다면 여기서 호출
  toast.info(`${date}에 시작하는 새 캠페인 만들기 — 상세 모달은 캠페인 페이지에서 진행하세요.`)
}

function resetFilters() {
  filter.value = { mineOnly: false }
  searchQuery.value = ''
  toggles.value = { campaign: true, deadline: true, milestone: true, task: true }
}

/* ─── 명령 팔레트 / 단축키 ─── */
const cmdkOpen = ref(false)
const cheatOpen = ref(false)
const searchInputRef = ref(null)

function shiftAnchor(deltaUnits) {
  const d = new Date(anchorDate.value)
  if (currentView.value === 'week') d.setDate(d.getDate() + 7 * deltaUnits)
  else d.setMonth(d.getMonth() + deltaUnits)
  anchorDate.value = d
}
function gotoToday() { anchorDate.value = new Date() }

function onKeyDown(e) {
  // Skip if typing in input/textarea
  const tag = (e.target?.tagName ?? '').toLowerCase()
  const isTyping = tag === 'input' || tag === 'textarea' || e.target?.isContentEditable
  // Allow ⌘K / Ctrl+K even while typing
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    cmdkOpen.value = !cmdkOpen.value
    return
  }
  if (e.key === 'Escape') {
    if (cmdkOpen.value) { cmdkOpen.value = false; return }
    if (cheatOpen.value) { cheatOpen.value = false; return }
    if (selectedEvent.value) { selectedEvent.value = null; return }
    if (dayModal.value.date) { closeDayModal(); return }
    if (quickAdd.value.date) { closeQuickAdd(); return }
  }
  if (isTyping) return
  switch (e.key.toLowerCase()) {
    case 't': gotoToday(); break
    case 'j': shiftAnchor(1); break
    case 'k': shiftAnchor(-1); break
    case 'm': currentView.value = 'calendar'; break
    case 'w': currentView.value = 'week'; break
    case 'a': currentView.value = 'agenda'; break
    case '/': e.preventDefault(); searchInputRef.value?.focus(); break
    case '?': cheatOpen.value = true; break
  }
}

/* ─── 모바일 감지 → Agenda 자동 전환 ─── */
const isMobile = ref(false)
function checkMobile() {
  isMobile.value = window.innerWidth <= 720
}
watch(isMobile, (mobile) => {
  if (mobile && currentView.value === 'calendar') currentView.value = 'agenda'
})

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('resize', checkMobile)
  checkMobile()
})
onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('resize', checkMobile)
})
</script>

<template>
  <div class="overview" :class="{ 'overview--dark': isDark }">

    <!-- Header -->
    <header class="overview__header">
      <h2 class="overview__title">캠페인 캘린더</h2>

      <div class="overview__view-tabs">
        <template v-for="(v, i) in viewOptions" :key="v.id">
          <button
            class="overview__view-btn"
            :class="{ 'overview__view-btn--active': currentView === v.id }"
            @click="currentView = v.id"
          >
            <span class="material-symbols-outlined">{{ v.icon }}</span>
            {{ v.name }}
          </button>
          <span v-if="i < viewOptions.length - 1" class="overview__view-sep" />
        </template>
      </div>

      <div class="overview__controls">
        <CalendarFilterChips :filter="filter" @update:filter="filter = $event" />
        <div class="overview__search">
          <span class="material-symbols-outlined">search</span>
          <input
            ref="searchInputRef"
            v-model="searchQuery"
            type="text"
            placeholder="검색... (단축키 /)"
            class="overview__search-input"
          />
        </div>
        <button class="overview__icon-btn" title="단축키 (?)" @click="cheatOpen = true">
          <span class="material-symbols-outlined">keyboard</span>
        </button>
        <button class="overview__icon-btn" title="명령 팔레트 (⌘K)" @click="cmdkOpen = true">
          <span class="material-symbols-outlined">bolt</span>
        </button>
      </div>
    </header>

    <!-- Body -->
    <div class="overview__body">
      <!-- Main view -->
      <main class="overview__main">
        <!-- 로딩 스켈레톤 (초기 로드) -->
        <div v-if="loading && !campaigns.length" class="overview__skeleton" aria-busy="true">
          <div class="overview__skel-bar overview__skel-bar--head"></div>
          <div class="overview__skel-grid">
            <div v-for="i in 7" :key="i" class="overview__skel-cell"></div>
          </div>
          <div class="overview__skel-grid">
            <div v-for="i in 35" :key="`g${i}`" class="overview__skel-cell overview__skel-cell--day"></div>
          </div>
        </div>

        <!-- 에러 상태 -->
        <div v-else-if="loadError && !campaigns.length" class="overview__empty overview__empty--error">
          <span class="material-symbols-outlined">error_outline</span>
          <p>{{ loadError }}</p>
          <button class="overview__retry" @click="loadAll">
            <span class="material-symbols-outlined">refresh</span>
            다시 시도
          </button>
        </div>

        <!-- 빈 데이터 상태 -->
        <div v-else-if="!loading && !filteredEvents.length" class="overview__empty">
          <span class="material-symbols-outlined">{{ formattedEvents.length ? 'filter_alt' : 'event_busy' }}</span>
          <p v-if="!formattedEvents.length">아직 등록된 일정이 없습니다.</p>
          <p v-else>현재 필터에 맞는 일정이 없습니다.</p>
          <button v-if="formattedEvents.length" class="overview__retry" @click="resetFilters">
            필터 초기화
          </button>
        </div>

        <transition v-else name="view-fade" mode="out-in">
          <MainCalendar
            v-if="currentView === 'calendar'"
            key="calendar"
            :events-data="filteredEvents"
            :anchor-date="anchorDate"
            @event-click="onEventClick"
            @day-click="onDayClick"
            @more-click="onMoreClick"
            @event-drop="onEventDrop"
            @update:anchor-date="anchorDate = $event"
          />
          <WeekCalendar
            v-else-if="currentView === 'week'"
            key="week"
            :events-data="filteredEvents"
            :anchor-date="anchorDate"
            @event-click="onEventClick"
            @update:anchor-date="anchorDate = $event"
          />
          <AgendaCalendar
            v-else-if="currentView === 'agenda'"
            key="agenda"
            :events-data="filteredEvents"
            @event-click="onEventClick"
          />
          <MainTimeline
            v-else-if="currentView === 'timeline'"
            key="timeline"
            :events-data="filteredEvents"
          />
          <MainTable
            v-else-if="currentView === 'table'"
            key="table"
            :events-data="filteredEvents"
          />
        </transition>
      </main>

      <!-- Sidebar (오른쪽) -->
      <CalendarSidebar
        v-if="!isMobile"
        :current-date="anchorDate"
        :events="formattedEvents"
        :toggles="toggles"
        @update:current-date="anchorDate = $event"
        @update:toggles="toggles = $event"
        @event-click="onEventClick"
      />
    </div>

    <!-- Overlays -->
    <EventDetailPanel :event="selectedEvent" @close="closePanel" />
    <DayEventsModal
      :date="dayModal.date"
      :events="dayModal.events"
      @close="closeDayModal"
      @event-click="onEventClick"
    />
    <QuickAddPopover
      :date="quickAdd.date"
      :position="quickAdd.position"
      :campaigns="campaigns"
      @close="closeQuickAdd"
      @create-task="createTaskFromQuick"
      @create-milestone="createMilestoneFromQuick"
      @open-campaign-modal="openCampaignFullModal"
    />
    <CommandPalette
      :open="cmdkOpen"
      :events="formattedEvents"
      @close="cmdkOpen = false"
      @jump-today="gotoToday"
      @change-view="(v) => { currentView = v }"
      @select-event="onEventClick"
    />
    <ShortcutCheatsheet :open="cheatOpen" @close="cheatOpen = false" />

  </div>
</template>

<style scoped>
/* === Layout === */
.overview {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--panel-color);
  color: var(--text-primary);
}

/* === Header === */
.overview__header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-color);
  position: sticky;
  top: 0;
  z-index: 20;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.overview__title {
  font-size: 17px;
  font-weight: 750;
  color: var(--text-primary);
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: -0.01em;
}
.overview__view-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  background: var(--panel-muted);
  padding: 3px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  flex-shrink: 0;
}
.overview__view-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
  color: var(--muted-text);
  cursor: pointer;
  transition: all 0.15s;
  border: none;
  background: none;
}
.overview__view-btn .material-symbols-outlined { font-size: 15px; }
.overview__view-btn:hover { color: var(--text-primary); }
.overview__view-btn--active {
  background: var(--panel-color);
  color: var(--accent-color);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
.overview__view-sep { width: 1px; height: 12px; background: var(--border-color); }

.overview__controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-wrap: wrap;
}
.overview__search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--control-color);
  transition: border-color 0.15s;
}
.overview__search:focus-within { border-color: var(--accent-color); }
.overview__search .material-symbols-outlined { font-size: 14px; color: var(--subtle-text); flex-shrink: 0; }
.overview__search-input {
  border: none;
  background: none;
  outline: none;
  font-size: 12.5px;
  color: var(--text-primary);
  width: 160px;
}
.overview__search-input::placeholder { color: var(--subtle-text); }

.overview__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--control-color);
  color: var(--muted-text);
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}
.overview__icon-btn:hover { color: var(--accent-color); border-color: var(--accent-color); }
.overview__icon-btn .material-symbols-outlined { font-size: 16px; }

/* === Body === */
.overview__body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}
.overview__main {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

/* === View Transition === */
.view-fade-enter-active,
.view-fade-leave-active { transition: opacity 0.18s ease; }
.view-fade-enter-from,
.view-fade-leave-to { opacity: 0; }

/* === Skeleton (초기 로드) === */
.overview__skeleton {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  height: 100%;
  background: var(--panel-color);
}
.overview__skel-bar { height: 28px; border-radius: 6px; }
.overview__skel-bar--head { width: 220px; margin-bottom: 14px; }
.overview__skel-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.overview__skel-cell { height: 22px; border-radius: 4px; }
.overview__skel-cell--day { height: 60px; }
.overview__skel-bar,
.overview__skel-cell {
  background: linear-gradient(90deg, var(--panel-muted) 0%, var(--border-color) 50%, var(--panel-muted) 100%);
  background-size: 200% 100%;
  animation: skel-shimmer 1.4s ease-in-out infinite;
}
@keyframes skel-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* === Empty / Error states === */
.overview__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: var(--muted-text);
  background: var(--panel-color);
}
.overview__empty .material-symbols-outlined { font-size: 48px; color: var(--subtle-text); }
.overview__empty p { font-size: 14px; margin: 0; }
.overview__empty--error .material-symbols-outlined { color: #EF4444; }
.overview__retry {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-primary);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}
.overview__retry:hover { border-color: var(--accent-color, #8B5CF6); color: var(--accent-color, #8B5CF6); }
.overview__retry .material-symbols-outlined { font-size: 16px; color: inherit; }

/* === Color palette for events === */
:deep(.evt-violet)  { background: rgba(139, 92, 246, 0.12) !important; color: #5B21B6 !important; border-color: rgba(139, 92, 246, 0.25) !important; }
:deep(.evt-fuchsia) { background: rgba(217, 70, 239, 0.12) !important; color: #86198F !important; border-color: rgba(217, 70, 239, 0.25) !important; }
:deep(.evt-blue)    { background: rgba(59, 130, 246, 0.12) !important; color: #1E40AF !important; border-color: rgba(59, 130, 246, 0.25) !important; }
:deep(.evt-emerald) { background: rgba(16, 185, 129, 0.12) !important; color: #047857 !important; border-color: rgba(16, 185, 129, 0.25) !important; }
:deep(.evt-amber)   { background: rgba(245, 158, 11, 0.14) !important; color: #92400E !important; border-color: rgba(245, 158, 11, 0.3) !important; }

:root[data-theme='dark'] :deep(.evt-violet)  { background: rgba(139, 92, 246, 0.22) !important; color: #DDD6FE !important; }
:root[data-theme='dark'] :deep(.evt-fuchsia) { background: rgba(217, 70, 239, 0.22) !important; color: #F0ABFC !important; }
:root[data-theme='dark'] :deep(.evt-blue)    { background: rgba(59, 130, 246, 0.22) !important; color: #BFDBFE !important; }
:root[data-theme='dark'] :deep(.evt-emerald) { background: rgba(16, 185, 129, 0.22) !important; color: #6EE7B7 !important; }
:root[data-theme='dark'] :deep(.evt-amber)   { background: rgba(245, 158, 11, 0.22) !important; color: #FCD34D !important; }

@media (max-width: 720px) {
  .overview__view-tabs { order: 3; width: 100%; }
  .overview__controls { width: 100%; }
}
</style>
