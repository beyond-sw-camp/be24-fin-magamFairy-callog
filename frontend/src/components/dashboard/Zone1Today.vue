<script setup>
/**
 * Zone 1 — 오늘의 할 일 & 활동 (2 페이지 슬라이드)
 *  P1: 좌 "오늘의 액션 업무"(review-queue + blockers + 마감임박 task) / 우 "최근 활동"(recent-activity)
 *  P2: "오늘의 업무 / 마감 임박" 좌측 토글 — 기본값은 개인설정(zone1P2Mode)
 *
 * 모든 데이터는 dashboard store(실 백엔드) 사용. 빈/에러 시 빈 상태 문구.
 */
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'
import { useTeamTaskStore } from '@/stores/teamTask'
import { useDashboardZonePrefs } from '@/composables/useDashboardZonePrefs'

const router = useRouter()
const store = useDashboardStore()
const teamTask = useTeamTaskStore()
const { prefs, setZoneDefault, setZone1P2Mode } = useDashboardZonePrefs()

const PAGE_COUNT = 2
const page = ref(prefs.value.zone1 ?? 0)
function shift(d) { page.value = (page.value + d + PAGE_COUNT) % PAGE_COUNT }
// 개인설정 변경(다른 화면에서) 시 동기화
watch(() => prefs.value.zone1, (v) => { if (Number.isInteger(v)) page.value = v })

const TITLE = computed(() => ['오늘의 캠페인 데스크', '오늘의 업무 / 마감 임박'][page.value])

/* ─── publicId 매핑 (campaignId(idx) → 라우팅용 publicId) ─── */
function publicIdByCampaignId(id) {
  if (id == null) return null
  const hit = (store.myCampaigns ?? []).find((c) => (c.idx ?? c.id) === id)
  return hit?.id ?? hit?.publicId ?? null
}
function openCampaign(id) {
  const pid = publicIdByCampaignId(id)
  if (pid) router.push(`/campaigns/${pid}`)
}

/* ─── P1 좌: 오늘의 액션 업무 ─── */
const reviewCount = computed(() => (store.reviewQueue ?? []).length)
const blockedCount = computed(
  () => (store.blockers ?? []).filter((b) => b.type === 'TASK_BLOCKED').length,
)
const noGmCount = computed(
  () => (store.blockers ?? []).filter((b) => b.type === 'CAMPAIGN_NO_GM').length,
)
const dueSoonCount = computed(() => dueSoonTasks.value.length)

const actionItems = computed(() => {
  const out = []
  if (reviewCount.value > 0) out.push({ key: 'review', icon: '🔍', label: `검수 대기 ${reviewCount.value}건`, tone: 't-review' })
  if (dueSoonCount.value > 0) out.push({ key: 'due', icon: '⏰', label: `마감 임박 업무 ${dueSoonCount.value}건`, tone: 't-due' })
  if (blockedCount.value > 0) out.push({ key: 'block', icon: '🚧', label: `차단(Blocked) ${blockedCount.value}건`, tone: 't-block' })
  if (noGmCount.value > 0) out.push({ key: 'nogm', icon: '👤', label: `GM 미배정 캠페인 ${noGmCount.value}건`, tone: 't-nogm' })
  return out
})

/* ─── P1 우: 최근 활동 피드 ─── */
const ACTIVITY_ICON = {
  TASK_DONE: '✅', TASK_UPDATE: '✏️', ASSET_ADD: '🆕',
  REVIEW_SUBMIT: '📤', REVIEW_APPROVE: '👍', STATUS_CHANGE: '🔁',
}
function relTime(iso) {
  if (!iso) return ''
  const t = new Date(iso).getTime()
  if (Number.isNaN(t)) return ''
  const diff = Date.now() - t
  if (diff < 60_000) return '방금 전'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}분 전`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}시간 전`
  if (diff < 172_800_000) return '어제'
  return `${Math.floor(diff / 86_400_000)}일 전`
}
const activities = computed(() => (store.recentActivity ?? []).map((a) => ({
  idx: a.idx,
  campaignId: a.campaignId,
  icon: ACTIVITY_ICON[a.type] ?? '•',
  text: a.description || a.type,
  actor: a.actorName,
  campaign: a.campaignName,
  rel: relTime(a.createdAt),
})))

/* ─── P2: 오늘의 업무 / 마감 임박 (toggle) ─── */
const p2Mode = ref(prefs.value.zone1P2Mode ?? 'today')
watch(() => prefs.value.zone1P2Mode, (v) => { if (v) p2Mode.value = v })

function dDay(dueDate) {
  if (!dueDate) return null
  const due = new Date(dueDate); if (Number.isNaN(due.getTime())) return null
  const now = new Date()
  const a = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const b = new Date(due.getFullYear(), due.getMonth(), due.getDate()).getTime()
  return Math.round((b - a) / 86_400_000)
}
const DEADLINE_WINDOW = 3 // 마감 임박 = 오늘+3일 이내
const myTasks = computed(() => (teamTask.tasks ?? []).filter((t) => String(t.status).toUpperCase() !== 'DONE'))
const todayTasks = computed(() => myTasks.value.filter((t) => dDay(t.dueDate) === 0))
const dueSoonTasks = computed(() => myTasks.value
  .filter((t) => { const d = dDay(t.dueDate); return d != null && d >= 0 && d <= DEADLINE_WINDOW })
  .sort((a, b) => (dDay(a.dueDate) ?? 99) - (dDay(b.dueDate) ?? 99)))

const PRIORITY_LABEL = { CRITICAL: '긴급', HIGH: '높음', MEDIUM: '보통', LOW: '낮음' }
function taskRow(t) {
  const d = dDay(t.dueDate)
  return {
    id: t.idx ?? t.id,
    campaignId: t.campaignIdx,
    name: t.name ?? '업무',
    when: d == null ? '미정' : d === 0 ? '오늘' : d > 0 ? `D-${d}` : `D+${-d}`,
    priority: PRIORITY_LABEL[String(t.priority ?? '').toUpperCase()] ?? '보통',
    high: String(t.priority ?? '').toUpperCase() === 'HIGH' || String(t.priority ?? '').toUpperCase() === 'CRITICAL',
  }
}
const p2Rows = computed(() => (p2Mode.value === 'today' ? todayTasks.value : dueSoonTasks.value).map(taskRow))

function setP2Mode(m) { p2Mode.value = m }

/* ─── ⋯ 메뉴: 이 화면을 기본으로 ─── */
const menuOpen = ref(false)
function makeDefault() {
  setZoneDefault('zone1', page.value)
  if (page.value === 1) setZone1P2Mode(p2Mode.value)
  menuOpen.value = false
}
</script>

<template>
  <section class="card zone1" aria-label="오늘의 할 일과 활동">
    <div class="card-h">
      <div>
        <h2>{{ TITLE }}</h2>
        <p class="lede">{{ page === 0 ? '내 캠페인 액션 업무 · 최근 활동' : '내 담당 업무 빠른 보기' }}</p>
      </div>
      <div class="z1-controls">
        <div class="z1-menu-wrap">
          <button class="z1-dots" aria-label="옵션" @click="menuOpen = !menuOpen">⋯</button>
          <Transition name="z1-pop">
            <div v-if="menuOpen" class="z1-menu" @mouseleave="menuOpen = false">
              <button @click="makeDefault">이 화면을 기본으로</button>
            </div>
          </Transition>
        </div>
        <div class="zone-nav">
          <button class="nav-btn" aria-label="이전" @click="shift(-1)">‹</button>
          <span class="nav-ind">{{ page + 1 }} / {{ PAGE_COUNT }}</span>
          <button class="nav-btn" aria-label="다음" @click="shift(1)">›</button>
        </div>
      </div>
    </div>

    <Transition name="page-slide" mode="out-in">
      <!-- ───── Page 1: 분할 카드 ───── -->
      <div v-if="page === 0" :key="'p1'" class="z1-split">
        <!-- 좌: 오늘의 액션 업무 -->
        <div class="z1-col">
          <h3 class="z1-col-h">오늘의 액션 업무</h3>
          <ul v-if="actionItems.length" class="z1-actions">
            <li v-for="it in actionItems" :key="it.key" class="z1-action" :class="it.tone">
              <span class="z1-action-ic">{{ it.icon }}</span>
              <span class="z1-action-lb">{{ it.label }}</span>
            </li>
          </ul>
          <div v-else class="z1-empty">지금 처리할 액션 업무가 없습니다.</div>
        </div>
        <div class="z1-divider" />
        <!-- 우: 최근 활동 피드 -->
        <div class="z1-col">
          <h3 class="z1-col-h">내 캠페인 최근 활동</h3>
          <ul v-if="activities.length" class="z1-feed">
            <li
              v-for="a in activities"
              :key="a.idx"
              class="z1-feed-row"
              :class="{ 'is-click': a.campaignId != null }"
              @click="openCampaign(a.campaignId)"
            >
              <span class="z1-feed-ic">{{ a.icon }}</span>
              <div class="z1-feed-mid">
                <div class="z1-feed-text">{{ a.text }}</div>
                <div class="z1-feed-sub">
                  <span v-if="a.actor">{{ a.actor }}</span>
                  <span v-if="a.campaign" class="z1-feed-camp">· {{ a.campaign }}</span>
                </div>
              </div>
              <span class="z1-feed-time">{{ a.rel }}</span>
            </li>
          </ul>
          <div v-else class="z1-empty">최근 활동이 없습니다.</div>
        </div>
      </div>

      <!-- ───── Page 2: 오늘의 업무 / 마감 임박 토글 ───── -->
      <div v-else :key="'p2'" class="z1-p2">
        <div class="z1-toggle">
          <button :class="{ 'is-on': p2Mode === 'today' }" @click="setP2Mode('today')">오늘의 업무</button>
          <button :class="{ 'is-on': p2Mode === 'deadline' }" @click="setP2Mode('deadline')">마감 임박</button>
        </div>
        <ul v-if="p2Rows.length" class="z1-tasklist">
          <li
            v-for="r in p2Rows"
            :key="r.id"
            class="z1-task"
            :class="{ 'is-click': r.campaignId != null }"
            @click="openCampaign(r.campaignId)"
          >
            <span class="z1-task-dot" :class="{ hi: r.high }" />
            <span class="z1-task-name">{{ r.name }}</span>
            <span class="z1-task-when">{{ r.when }}</span>
            <span class="z1-task-pri" :class="{ hi: r.high }">{{ r.priority }}</span>
          </li>
        </ul>
        <div v-else class="z1-empty">
          {{ p2Mode === 'today' ? '오늘 마감인 업무가 없습니다.' : '마감 임박 업무가 없습니다.' }}
        </div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.zone1 { grid-column: 1; grid-row: 1; display: flex; flex-direction: column; }
.card-h { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; gap: 12px; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; }
.card-h .lede { margin: 4px 0 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); }

.z1-controls { display: inline-flex; align-items: center; gap: 8px; }
.z1-menu-wrap { position: relative; }
.z1-dots { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-text-muted); cursor: pointer; font-size: 15px; line-height: 1; }
.z1-dots:hover { background: var(--lp-surface-soft); color: var(--lp-text); }
.z1-menu { position: absolute; top: calc(100% + 6px); right: 0; background: var(--lp-surface); border: 1px solid var(--lp-border); border-radius: 10px; box-shadow: 0 8px 24px rgba(63,52,99,.16); padding: 5px; z-index: 30; white-space: nowrap; }
.z1-menu button { display: block; width: 100%; text-align: left; padding: 7px 12px; font-size: 12.5px; font-weight: 600; color: var(--lp-text); background: transparent; border: 0; border-radius: 7px; cursor: pointer; }
.z1-menu button:hover { background: var(--lp-surface-soft); color: var(--lp-primary-deep); }
.z1-pop-enter-active, .z1-pop-leave-active { transition: opacity .15s ease, transform .15s ease; }
.z1-pop-enter-from, .z1-pop-leave-to { opacity: 0; transform: translateY(-4px); }

.zone-nav { display: inline-flex; align-items: center; gap: 6px; }
.nav-btn { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-primary-deep); font-size: 14px; cursor: pointer; line-height: 1; transition: background .15s, transform .12s; }
.nav-btn:hover { background: var(--lp-surface-soft); }
.nav-btn:active { transform: scale(0.94); }
.nav-ind { font-size: 11px; font-weight: 600; color: var(--lp-text-faint); min-width: 36px; text-align: center; font-variant-numeric: tabular-nums; }

/* P1 split */
.z1-split { display: grid; grid-template-columns: minmax(0,1fr) 1px minmax(0,1.1fr); gap: 0; flex: 1; min-height: 0; }
.z1-col { display: flex; flex-direction: column; min-height: 0; padding: 0 4px; overflow: hidden; }
.z1-divider { background: var(--lp-border); margin: 0 16px; }
.z1-col-h { margin: 0 0 10px; font-size: 12.5px; font-weight: 700; color: var(--lp-text-muted); }

.z1-actions { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.z1-action { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border-radius: var(--r-lg, 18px); background: var(--lp-surface-soft); font-size: 13.5px; font-weight: 700; color: var(--lp-text); position: relative; overflow: hidden; transition: transform .12s ease, box-shadow .15s ease; }
.z1-action:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(63,52,99,.08); }
.z1-action-ic { width: 34px; height: 34px; border-radius: 999px; background: var(--lp-frost-strong); display: inline-flex; align-items: center; justify-content: center; font-size: 17px; flex-shrink: 0; box-shadow: 0 1px 2px rgba(63,52,99,.08); }
.z1-action-lb { flex: 1; min-width: 0; }
.z1-action.t-review { background: var(--lp-card-lavender-1); }
.z1-action.t-due    { background: var(--lp-card-cream); }
.z1-action.t-block  { background: var(--lp-card-peach); }
.z1-action.t-nogm   { background: var(--lp-lime-soft); }

.z1-feed { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }
.z1-feed-row { display: flex; align-items: center; gap: 10px; padding: 8px 6px; border-radius: 12px; }
.z1-feed-row.is-click { cursor: pointer; }
.z1-feed-row.is-click:hover { background: var(--lp-surface-soft); }
.z1-feed-ic { width: 28px; height: 28px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; font-size: 13px; flex-shrink: 0; background: var(--accent-soft); box-shadow: inset 0 0 0 1px var(--lp-border); }
.z1-feed-mid { flex: 1; min-width: 0; }
.z1-feed-text { font-size: 12.5px; font-weight: 600; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-feed-sub { font-size: 11px; color: var(--lp-text-faint); margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-feed-camp { color: var(--lp-text-muted); }
.z1-feed-time { font-size: 11px; color: var(--lp-text-faint); white-space: nowrap; flex-shrink: 0; }

/* P2 toggle */
.z1-p2 { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.z1-toggle { display: inline-flex; background: var(--lp-surface-soft); border-radius: 999px; padding: 3px; gap: 2px; margin-bottom: 12px; align-self: flex-start; }
.z1-toggle button { padding: 6px 16px; font-size: 12.5px; font-weight: 600; color: var(--lp-text-muted); background: transparent; border: 0; border-radius: 999px; cursor: pointer; }
.z1-toggle button.is-on { background: var(--lp-surface); color: var(--lp-primary-deep); box-shadow: 0 1px 3px rgba(63,52,99,.10); }
.z1-tasklist { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 6px; overflow-y: auto; }
.z1-task { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-radius: 12px; background: var(--lp-surface-soft); }
.z1-task.is-click { cursor: pointer; }
.z1-task.is-click:hover { background: var(--lp-border); }
.z1-task-dot { width: 8px; height: 8px; border-radius: 999px; background: var(--lp-primary); flex-shrink: 0; }
.z1-task-dot.hi { background: #FF7A6B; }
.z1-task-name { flex: 1; min-width: 0; font-size: 13px; font-weight: 600; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-task-when { font-size: 12px; font-weight: 600; color: var(--lp-text-muted); flex-shrink: 0; }
.z1-task-pri { font-size: 10.5px; font-weight: 700; color: var(--lp-primary-deep); background: var(--accent-soft); padding: 3px 10px; border-radius: 999px; flex-shrink: 0; }
.z1-task-pri.hi { color: #FF6A5A; background: rgba(255,122,107,.16); }

.z1-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 20px; text-align: center; }

.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(14px); }
.page-slide-leave-to { opacity: 0; transform: translateX(-14px); }
</style>
