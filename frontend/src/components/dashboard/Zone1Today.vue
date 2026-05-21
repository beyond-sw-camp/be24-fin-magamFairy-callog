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
const { prefs } = useDashboardZonePrefs()

const PAGE_COUNT = 2
const page = ref(prefs.value.zone1 ?? 0)
function shift(d) { page.value = (page.value + d + PAGE_COUNT) % PAGE_COUNT }
// 개인설정 변경(다른 화면에서) 시 동기화
watch(() => prefs.value.zone1, (v) => { if (Number.isInteger(v)) page.value = v })

const TITLE = computed(() => ['오늘의 캠페인 데스크', '마감 임박'][page.value])

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
// 캠페인 상세의 "검수/승인" 탭으로 바로 이동
function openCampaignReview(id) {
  const pid = publicIdByCampaignId(id)
  if (pid) router.push({ path: `/campaigns/${pid}`, query: { tab: 'review' } })
}

/* ─── P1 좌: 오늘의 액션 업무 ─── */
const reviewCount = computed(() => (store.reviewQueue ?? []).length)
const overdueCount = computed(
  () => (store.blockers ?? []).filter((b) => b.type === 'OVERDUE_TASK').length,
)
const rejectedCount = computed(
  () => (store.blockers ?? []).filter((b) => b.type === 'REJECTED_REVIEW').length,
)
const dueSoonCount = computed(() => dueSoonTasks.value.length)

/* 액션 항목 — 순서: 검수 대기 / 반려된 검수 / 마감 임박 / 마감 초과 */
const actionItems = computed(() => {
  const out = []
  if (reviewCount.value > 0) out.push({ key: 'review', icon: '🔍', label: `검수 대기 ${reviewCount.value}건`, tone: 't-review' })
  if (rejectedCount.value > 0) out.push({ key: 'rejected', icon: '↩️', label: `반려된 검수 ${rejectedCount.value}건`, tone: 't-nogm' })
  if (dueSoonCount.value > 0) out.push({ key: 'due', icon: '⏰', label: `마감 임박 ${dueSoonCount.value}건`, tone: 't-due' })
  if (overdueCount.value > 0) out.push({ key: 'overdue', icon: '🔴', label: `마감 초과 ${overdueCount.value}건`, tone: 't-block' })
  return out
})

/* 액션 클릭 → 모달(해당 리스트). 모달 항목 클릭 → 해당 캠페인/보드로 라우팅 */
const actionModal = ref(null) // { title, items: [{ id, title, sub, campaignId }] }
function openActionModal(it) {
  let items = []
  if (it.key === 'review') {
    items = (store.reviewQueue ?? []).map((r) => ({
      id: 'rv' + r.taskId, title: r.taskName ?? '검수 항목', sub: r.campaignName ?? '', campaignId: r.campaignId,
    }))
  } else if (it.key === 'rejected') {
    items = (store.blockers ?? []).filter((b) => b.type === 'REJECTED_REVIEW').map((b) => ({
      id: 'rj' + b.targetId, title: b.targetName ?? '반려 검수', sub: b.campaignName ?? '', campaignId: b.campaignId,
    }))
  } else if (it.key === 'due') {
    items = dueSoonTasks.value.map((t) => {
      const d = dDay(t.dueDate)
      return {
        id: 'du' + (t.idx ?? t.id),
        title: t.name ?? '업무',
        sub: [publicNameByCampaignId(t.campaignIdx), d === 0 ? '오늘 마감' : `D-${d}`].filter(Boolean).join(' · '),
        campaignId: t.campaignIdx,
      }
    })
  } else if (it.key === 'overdue') {
    items = (store.blockers ?? []).filter((b) => b.type === 'OVERDUE_TASK').map((b) => ({
      id: 'ov' + b.targetId, title: b.targetName ?? '업무', sub: b.campaignName ?? '', campaignId: b.campaignId,
    }))
  }
  actionModal.value = { title: it.label.replace(/\s\d+건$/, ''), items, key: it.key }
}
function closeActionModal() { actionModal.value = null }
function goItem(item) {
  const key = actionModal.value?.key
  if (item.campaignId != null) {
    // 검수 도메인(검수 대기 / 반려된 검수) → 해당 캠페인의 검수/승인 탭으로
    if (key === 'review' || key === 'rejected') openCampaignReview(item.campaignId)
    else openCampaign(item.campaignId)
  } else {
    router.push({ name: 'team-board' })
  }
  closeActionModal()
}

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

/* ─── P2: 마감 임박 (D-day 칸반) ─── */
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
const dueSoonTasks = computed(() => myTasks.value
  .filter((t) => { const d = dDay(t.dueDate); return d != null && d >= 0 && d <= DEADLINE_WINDOW })
  .sort((a, b) => (dDay(a.dueDate) ?? 99) - (dDay(b.dueDate) ?? 99)))

const PRIORITY_LABEL = { CRITICAL: '긴급', HIGH: '높음', MEDIUM: '보통', LOW: '낮음' }

/* ─── P2: D-day 칸반 (§3.2) — 오늘/내일/이번주 버킷 ─── */
function bucketOf(d) {
  if (d === 0) return 'today'
  if (d === 1) return 'tomr'
  return 'week'
}
function ddChip(t, d) {
  const due = new Date(t.dueDate)
  const md = Number.isNaN(due.getTime()) ? '' : `${due.getMonth() + 1}/${due.getDate()}`
  const lbl = d === 0 ? '오늘' : d === 1 ? '내일' : `D-${d}`
  return { md, lbl }
}
/* status != DONE & 0<=d<=7 → D-day 정렬 후 행 모델 */
const deadlineTasks = computed(() => myTasks.value
  .map((t) => ({ t, d: dDay(t.dueDate) }))
  .filter(({ d }) => d != null && d >= 0 && d <= 7)
  .sort((a, b) => a.d - b.d)
  .map(({ t, d }) => {
    const pri = String(t.priority ?? '').toUpperCase()
    return {
      id: t.idx ?? t.id,
      campaignId: t.campaignIdx,
      name: t.name ?? '업무',
      campaign: publicNameByCampaignId(t.campaignIdx),
      bucket: bucketOf(d),
      chip: ddChip(t, d),
      priority: PRIORITY_LABEL[pri] ?? '보통',
      hi: pri === 'HIGH' || pri === 'CRITICAL',
    }
  }))
const deadlineStats = computed(() => {
  const s = { today: 0, tomr: 0, week: 0 }
  for (const r of deadlineTasks.value) s[r.bucket]++
  return s
})
function publicNameByCampaignId(id) {
  if (id == null) return ''
  const hit = (store.myCampaigns ?? []).find((c) => (c.idx ?? c.id) === id)
  return hit?.name ?? hit?.title ?? ''
}

</script>

<template>
  <section class="card zone1" aria-label="오늘의 할 일과 활동">
    <div class="card-h">
      <div class="card-h-ttl">
        <h2>{{ TITLE }}</h2>
        <span class="card-dot" />
        <p class="lede">{{ page === 0 ? '내 캠페인 액션 업무 · 최근 활동' : '7일 이내 마감 · D-day 우선' }}</p>
      </div>
      <div class="z1-controls">
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
            <li v-for="it in actionItems" :key="it.key" class="z1-action is-click" :class="it.tone" @click="openActionModal(it)">
              <span class="z1-action-ic">{{ it.icon }}</span>
              <span class="z1-action-lb">{{ it.label }}</span>
              <span class="z1-action-go">›</span>
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

      <!-- ───── Page 2: 마감 임박 — D-day 칸반 (3:7 split) ───── -->
      <div v-else :key="'p2'" class="z1-p2-split">
        <!-- 좌 30%: 요약 카드 3개 세로 적층 -->
        <div class="z1-stats">
          <div class="z1-stat z1-stat--today">
            <span class="z1-stat-n">{{ deadlineStats.today }}</span>
            <span class="z1-stat-l">오늘 마감</span>
          </div>
          <div class="z1-stat z1-stat--tomr">
            <span class="z1-stat-n">{{ deadlineStats.tomr }}</span>
            <span class="z1-stat-l">내일 마감</span>
          </div>
          <div class="z1-stat z1-stat--week">
            <span class="z1-stat-n">{{ deadlineStats.week }}</span>
            <span class="z1-stat-l">이번 주</span>
          </div>
        </div>
        <!-- 우 70%: D-day 업무 리스트 -->
        <ul v-if="deadlineTasks.length" class="z1-dlist">
          <li
            v-for="r in deadlineTasks"
            :key="r.id"
            class="z1-drow"
            :class="['r-' + r.bucket, { 'is-click': r.campaignId != null }]"
            @click="openCampaign(r.campaignId)"
          >
            <span class="z1-dchip">
              <span class="z1-dchip-md">{{ r.chip.md }}</span>
              <span class="z1-dchip-l">{{ r.chip.lbl }}</span>
            </span>
            <span class="z1-dmid">
              <span class="z1-dname">{{ r.name }}</span>
              <span v-if="r.campaign" class="z1-dcamp">{{ r.campaign }}</span>
            </span>
            <span class="z1-dpri" :class="{ hi: r.hi }">{{ r.priority }}</span>
          </li>
        </ul>
        <div v-else class="z1-empty">마감 임박 업무가 없습니다.</div>
      </div>
    </Transition>

    <!-- 액션 모달: 항목 클릭 → 해당 캠페인/보드로 이동 -->
    <Transition name="z1-modal">
      <div v-if="actionModal" class="z1-amodal-backdrop" @click="closeActionModal">
        <div class="z1-amodal" role="dialog" aria-modal="true" @click.stop>
          <div class="z1-amodal-h">
            <h3>{{ actionModal.title }}</h3>
            <button class="z1-amodal-x" aria-label="닫기" @click="closeActionModal">✕</button>
          </div>
          <ul v-if="actionModal.items.length" class="z1-amodal-list">
            <li v-for="m in actionModal.items" :key="m.id" class="z1-amodal-row" @click="goItem(m)">
              <div class="z1-amodal-mid">
                <div class="z1-amodal-title">{{ m.title }}</div>
                <div v-if="m.sub" class="z1-amodal-sub">{{ m.sub }}</div>
              </div>
              <span class="z1-amodal-go">›</span>
            </li>
          </ul>
          <div v-else class="z1-amodal-empty">표시할 항목이 없습니다.</div>
        </div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.zone1 { grid-column: 1; grid-row: 1; display: flex; flex-direction: column; }
.card-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 12px; }
.card-h-ttl { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.card-dot { width: 9px; height: 9px; border-radius: 999px; background: var(--lp-primary); align-self: center; flex-shrink: 0; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; flex-shrink: 0; }
.card-h .lede { margin: 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.z1-controls { display: inline-flex; align-items: center; gap: 8px; }

.zone-nav { display: inline-flex; align-items: center; gap: 6px; }
.nav-btn { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-primary-deep); font-size: 14px; cursor: pointer; line-height: 1; transition: background .15s, transform .12s; }
.nav-btn:hover { background: var(--lp-surface-soft); }
.nav-btn:active { transform: scale(0.94); }
.nav-ind { font-size: 11px; font-weight: 600; color: var(--lp-text-faint); min-width: 36px; text-align: center; font-variant-numeric: tabular-nums; }

/* P1 split */
.z1-split { display: grid; grid-template-columns: minmax(0,3fr) 1px minmax(0,7fr); gap: 0; flex: 1; min-height: 0; }
.z1-col { display: flex; flex-direction: column; min-height: 0; padding: 0 4px; overflow: hidden; }
.z1-divider { background: var(--lp-border); margin: 0 16px; }
.z1-col-h { margin: 0 0 10px; font-size: 12.5px; font-weight: 700; color: var(--lp-text-muted); }

.z1-actions { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; flex: 1; min-height: 0; overflow-y: auto; }
.z1-action { display: flex; align-items: center; gap: 12px; padding: 14px 16px; border-radius: var(--r-lg, 18px); background: var(--lp-surface-soft); font-size: 13.5px; font-weight: 700; color: var(--lp-text); position: relative; overflow: hidden; transition: transform .12s ease, box-shadow .15s ease; }
.z1-action:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(63,52,99,.08); }
.z1-action { animation: lp-rise .42s cubic-bezier(.4,0,.2,1) both; }
.z1-action:nth-child(1) { animation-delay: .04s; }
.z1-action:nth-child(2) { animation-delay: .10s; }
.z1-action:nth-child(3) { animation-delay: .16s; }
.z1-action:nth-child(4) { animation-delay: .22s; }
.z1-action.is-click { cursor: pointer; }
.z1-action-ic { width: 34px; height: 34px; border-radius: 999px; background: var(--lp-frost-strong); display: inline-flex; align-items: center; justify-content: center; font-size: 17px; flex-shrink: 0; box-shadow: 0 1px 2px rgba(63,52,99,.08); }
.z1-action-lb { flex: 1; min-width: 0; }
.z1-action-go { font-size: 16px; font-weight: 700; color: var(--lp-primary-deep); opacity: .5; flex-shrink: 0; }
.z1-action:hover .z1-action-go { opacity: 1; }
.z1-action.t-review { background: var(--lp-card-lavender-1); }
.z1-action.t-due    { background: var(--lp-card-cream); }
.z1-action.t-block  { background: var(--lp-card-peach); }
.z1-action.t-nogm   { background: var(--lp-lime-soft); }

.z1-feed { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }
.z1-feed-row { display: flex; align-items: center; gap: 10px; padding: 8px 6px; border-radius: 12px; animation: lp-fade .4s ease both; }
.z1-feed-row.is-click { cursor: pointer; }
.z1-feed-row.is-click:hover { background: var(--lp-surface-soft); }
.z1-feed-ic { width: 28px; height: 28px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; font-size: 13px; flex-shrink: 0; background: var(--accent-soft); box-shadow: inset 0 0 0 1px var(--lp-border); }
.z1-feed-mid { flex: 1; min-width: 0; }
.z1-feed-text { font-size: 12.5px; font-weight: 600; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-feed-sub { font-size: 11px; color: var(--lp-text-faint); margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-feed-camp { color: var(--lp-text-muted); }
.z1-feed-time { font-size: 11px; color: var(--lp-text-faint); white-space: nowrap; flex-shrink: 0; }

/* P2 — D-day 칸반 (§3.2) */
.z1-p2-split {
  /* base.css에 없을 수 있는 urgent/warn 톤은 로컬로 선언 */
  --urgent: #E25B49; --urgent-soft: rgba(226,91,73,.14);
  --warn: #D77B2A; --warn-soft: rgba(215,123,42,.14);
  flex: 1; min-height: 0;
  display: grid; grid-template-columns: 3fr 7fr;
  grid-template-rows: minmax(0, 1fr); /* 행을 카드 높이로 묶어 넘침 방지 */
  gap: 14px;
}
/* 좌 30%: 요약 카드 3개 */
.z1-stats { display: flex; flex-direction: column; gap: 10px; min-height: 0; overflow: hidden; }
.z1-stat { flex: 1; min-height: 0; display: flex; flex-direction: column; justify-content: center; padding: 10px 16px; border-radius: var(--r-md, 14px); transition: transform .12s ease, box-shadow .15s ease; overflow: hidden; }
.z1-stat:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(63,52,99,.10); }
.z1-stat { animation: lp-rise .42s cubic-bezier(.4,0,.2,1) both; }
.z1-stat--today { animation-delay: .04s; }
.z1-stat--tomr { animation-delay: .10s; }
.z1-stat--week { animation-delay: .16s; }
.z1-stat-n { font-size: 28px; font-weight: 800; line-height: 1; letter-spacing: -0.03em; font-variant-numeric: tabular-nums; }
.z1-stat-l { font-size: 11.5px; font-weight: 800; margin-top: 6px; }
.z1-stat--today { background: var(--urgent-soft); color: var(--urgent); }
.z1-stat--tomr  { background: var(--warn-soft); color: var(--warn); }
.z1-stat--week  { background: var(--accent-soft); color: var(--lp-primary-deep); }

/* 우 70%: D-day 리스트 */
.z1-dlist { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; min-height: 0; }
.z1-drow { position: relative; display: grid; grid-template-columns: 56px 1fr auto; gap: 12px; align-items: center; padding: 9px 14px 9px 16px; border-radius: var(--r-md, 14px); background: var(--lp-surface-soft); transition: transform .12s ease, box-shadow .15s ease; animation: lp-rise .4s cubic-bezier(.4,0,.2,1) both; }
.z1-drow::before { content: ''; position: absolute; left: 0; top: 8px; bottom: 8px; width: 4px; border-radius: 999px; }
.z1-drow.r-today::before { background: var(--urgent); }
.z1-drow.r-tomr::before  { background: var(--warn); }
.z1-drow.r-week::before  { background: var(--lp-primary); }
.z1-drow.is-click { cursor: pointer; }
.z1-drow.is-click:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(63,52,99,.10); }
.z1-dchip { width: 46px; height: 46px; border-radius: 12px; background: var(--lp-surface); display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0; box-shadow: 0 1px 2px rgba(63,52,99,.08); }
.z1-dchip-md { font-size: 14px; font-weight: 800; color: var(--lp-text); letter-spacing: -0.02em; font-variant-numeric: tabular-nums; }
.z1-dchip-l { font-size: 8.5px; font-weight: 700; color: var(--lp-text-muted); margin-top: 1px; }
.z1-drow.r-today .z1-dchip-l { color: var(--urgent); }
.z1-drow.r-tomr .z1-dchip-l  { color: var(--warn); }
.z1-dmid { min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.z1-dname { font-size: 13px; font-weight: 700; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-dcamp { font-size: 11px; color: var(--lp-text-faint); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-dpri { font-size: 10px; font-weight: 800; color: var(--lp-primary-deep); background: var(--accent-soft); padding: 3px 9px; border-radius: 999px; flex-shrink: 0; white-space: nowrap; }
.z1-dpri.hi { color: var(--urgent); background: var(--urgent-soft); }

.z1-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 20px; text-align: center; }

.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(14px); }
.page-slide-leave-to { opacity: 0; transform: translateX(-14px); }

/* 액션 모달 */
.z1-amodal-backdrop { position: fixed; inset: 0; background: rgba(42,36,64,.42); backdrop-filter: blur(2px); display: flex; align-items: center; justify-content: center; z-index: 200; }
.z1-amodal { background: var(--lp-surface); border-radius: var(--r-lg, 18px); width: min(420px, 92vw); max-height: 70vh; display: flex; flex-direction: column; box-shadow: 0 20px 50px rgba(63,52,99,.30); overflow: hidden; }
.z1-amodal-h { display: flex; align-items: center; justify-content: space-between; padding: 16px 18px; border-bottom: 1px solid var(--lp-border); }
.z1-amodal-h h3 { margin: 0; font-size: 15px; font-weight: 800; color: var(--lp-text); }
.z1-amodal-x { width: 28px; height: 28px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-text-muted); cursor: pointer; font-size: 13px; }
.z1-amodal-x:hover { background: var(--lp-surface-soft); }
.z1-amodal-list { list-style: none; margin: 0; padding: 10px; overflow-y: auto; display: flex; flex-direction: column; gap: 6px; }
.z1-amodal-row { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-radius: 12px; background: var(--lp-surface-soft); cursor: pointer; transition: background .15s ease, transform .12s ease; }
.z1-amodal-row:hover { background: var(--lp-border); transform: translateY(-1px); }
.z1-amodal-mid { flex: 1; min-width: 0; }
.z1-amodal-title { font-size: 13px; font-weight: 700; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-amodal-sub { font-size: 11px; color: var(--lp-text-faint); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z1-amodal-go { font-size: 16px; font-weight: 700; color: var(--lp-primary-deep); flex-shrink: 0; }
.z1-amodal-empty { padding: 32px; text-align: center; color: var(--lp-text-faint); font-size: 12.5px; }
.z1-modal-enter-active, .z1-modal-leave-active { transition: opacity .18s ease; }
.z1-modal-enter-from, .z1-modal-leave-to { opacity: 0; }
</style>
