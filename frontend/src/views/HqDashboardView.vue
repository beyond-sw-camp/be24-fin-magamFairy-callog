<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ListCampaign } from '@/api/campaigns'
import { ListAllTasks } from '@/api/teamboard'
import { useAuthStore } from '@/stores/useAuthStore'
import HeroToggle from '@/components/dashboard/HeroToggle.vue'
import KpiStatCard from '@/components/dashboard/KpiStatCard.vue'
import CampaignProgressCard from '@/components/dashboard/CampaignProgressCard.vue'
import UpcomingTaskItem from '@/components/dashboard/UpcomingTaskItem.vue'

const router = useRouter()
const authStore = useAuthStore()

// ───────── 상태 ─────────
const scope = ref('mine')                  // "mine" | "org"
const mineCampaigns = ref([])              // 내 캠페인 (scope=mine)
const orgCampaigns = ref([])               // 조직 캠페인 (scope=org)
const allTasks = ref([])                   // ListAllTasks() 결과
const progressMode = ref('progress')       // 진행 현황 카드 필터: progress | time | task
const loading = ref(false)
const errorMsg = ref('')

const orgName = computed(() => authStore.user?.organization?.name ?? authStore.user?.companyName ?? '')
const userName = computed(() => authStore.user?.name ?? authStore.user?.id ?? '')

const currentCampaigns = computed(() =>
  scope.value === 'org' ? orgCampaigns.value : mineCampaigns.value,
)

// ───────── 데이터 로딩 ─────────
async function loadCampaignsForScope(s) {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await ListCampaign({ scope: s })
    if (s === 'org') orgCampaigns.value = Array.isArray(data) ? data : []
    else mineCampaigns.value = Array.isArray(data) ? data : []
  } catch (e) {
    errorMsg.value = e?.message ?? '캠페인을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadTasks() {
  try {
    const data = await ListAllTasks()
    allTasks.value = Array.isArray(data) ? data : (data?.taskList ?? data?.list ?? [])
  } catch {
    // 태스크 로드 실패는 치명적이지 않음 — 빈 배열로 fallback
    allTasks.value = []
  }
}

onMounted(async () => {
  await Promise.all([loadCampaignsForScope('mine'), loadTasks()])
  // 토글에 표시할 카운트를 위해 백그라운드로 org 도 미리 받아 둠
  loadCampaignsForScope('org')
})

watch(scope, async (next) => {
  const list = next === 'org' ? orgCampaigns.value : mineCampaigns.value
  if (list.length === 0) {
    await loadCampaignsForScope(next)
  }
})

// ───────── 태스크 통계 (캠페인별 done/total) ─────────
const taskStatsByCampaign = computed(() => {
  const map = new Map()
  for (const t of allTasks.value) {
    const cid = t.campaignId ?? t.campaignIdx ?? t.campaign_id
    if (cid == null) continue
    const key = String(cid)
    if (!map.has(key)) map.set(key, { done: 0, total: 0 })
    const stat = map.get(key)
    stat.total += 1
    const status = (t.status ?? '').toUpperCase()
    if (status === 'DONE' || status === 'COMPLETED') stat.done += 1
  }
  return map
})

function getTaskStats(campaign) {
  const id = campaign.id ?? campaign.idx
  return taskStatsByCampaign.value.get(String(id)) ?? { done: 0, total: 0 }
}

// ───────── KPI 카드 ─────────
const kpiActiveCount = computed(() =>
  currentCampaigns.value.filter(c => {
    const s = (c.status ?? '').toLowerCase()
    return s === 'live' || s === 'active' || s === '진행중'
  }).length,
)

function computeTimeProgress(start, end) {
  if (!start || !end) return 0
  const s = new Date(start).getTime()
  const e = new Date(end).getTime()
  if (Number.isNaN(s) || Number.isNaN(e) || e <= s) return 0
  const elapsed = Date.now() - s
  return Math.max(0, Math.min(100, Math.round((elapsed / (e - s)) * 100)))
}

function computeHybridProgress(c) {
  const stats = getTaskStats(c)
  if (stats.total > 0) return Math.round((stats.done / stats.total) * 100)
  return computeTimeProgress(c.startDate, c.endDate)
}

const kpiAvgProgress = computed(() => {
  if (currentCampaigns.value.length === 0) return 0
  const sum = currentCampaigns.value.reduce((acc, c) => acc + computeHybridProgress(c), 0)
  return Math.round(sum / currentCampaigns.value.length)
})

const kpiPartnerCount = computed(() => {
  const partnerSet = new Set()
  for (const c of currentCampaigns.value) {
    for (const p of c.partners ?? []) partnerSet.add(p)
  }
  return partnerSet.size
})

const kpiThisWeekDeadline = computed(() => {
  const now = Date.now()
  const sevenDays = 7 * 86400000
  const campaignDeadlines = currentCampaigns.value.filter(c => {
    if (!c.endDate) return false
    const t = new Date(c.endDate).getTime()
    return !Number.isNaN(t) && t >= now && t <= now + sevenDays
  }).length
  const taskDeadlines = allTasks.value.filter(t => {
    if (!t.dueDate) return false
    const ts = new Date(t.dueDate).getTime()
    if (Number.isNaN(ts) || ts < now || ts > now + sevenDays) return false
    const inScope = currentCampaigns.value.some(
      c => String(c.id ?? c.idx) === String(t.campaignId ?? t.campaignIdx),
    )
    return inScope
  }).length
  return campaignDeadlines + taskDeadlines
})

// ───────── 캠페인 진행 현황 (5개 + 더보기) ─────────
const sortedCampaigns = computed(() =>
  [...currentCampaigns.value].sort((a, b) => {
    const aT = a.endDate ? new Date(a.endDate).getTime() : Number.MAX_SAFE_INTEGER
    const bT = b.endDate ? new Date(b.endDate).getTime() : Number.MAX_SAFE_INTEGER
    return aT - bT
  }),
)

const topCampaigns = computed(() => sortedCampaigns.value.slice(0, 5))
const moreCount = computed(() => Math.max(0, sortedCampaigns.value.length - topCampaigns.value.length))

function goToCampaignList() {
  router.push({ name: 'overview' })
}

// ───────── 다가오는 일정 (7일 내 태스크) ─────────
const upcomingTasks = computed(() => {
  const now = Date.now()
  const sevenDays = 7 * 86400000
  const scopedIds = new Set(currentCampaigns.value.map(c => String(c.id ?? c.idx)))
  const campaignNameMap = new Map(currentCampaigns.value.map(c => [String(c.id ?? c.idx), c.name]))

  return allTasks.value
    .filter(t => {
      if (!t.dueDate) return false
      const ts = new Date(t.dueDate).getTime()
      if (Number.isNaN(ts) || ts < now - 86400000 || ts > now + sevenDays) return false
      const cid = String(t.campaignId ?? t.campaignIdx ?? '')
      return scopedIds.has(cid)
    })
    .map(t => ({
      ...t,
      campaignId: t.campaignId ?? t.campaignIdx,
      campaignName: campaignNameMap.get(String(t.campaignId ?? t.campaignIdx)) ?? '',
    }))
    .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())
    .slice(0, 10)
})

// ───────── 진행률 모드 토글 ─────────
const progressModes = [
  { id: 'progress', label: '진행률' },
  { id: 'time', label: '시간' },
  { id: 'task', label: '태스크' },
]
</script>

<template>
  <section class="hq-dashboard">
    <!-- HERO -->
    <header class="hq-hero">
      <div class="hq-hero__copy">
        <p class="hq-hero__eyebrow">본사 통합 대시보드</p>
        <h2 class="hq-hero__title">
          {{ userName || '사용자' }}
          <span v-if="orgName" class="hq-hero__org">· {{ orgName }}</span>
        </h2>
        <span class="hq-hero__sub">
          {{ scope === 'org' ? '우리 조직 전체 캠페인' : '내가 멤버인 캠페인' }} 기준
        </span>
      </div>
      <HeroToggle
        v-model="scope"
        :mine-count="mineCampaigns.length"
        :org-count="orgCampaigns.length"
      />
    </header>

    <p v-if="errorMsg" class="hq-error-banner">{{ errorMsg }}</p>

    <!-- KPI 4-카드 -->
    <section class="hq-stats" aria-label="핵심 지표">
      <KpiStatCard
        label="진행 중 캠페인"
        :value="kpiActiveCount"
        :caption="`전체 ${currentCampaigns.length}건 중`"
      />
      <KpiStatCard
        label="평균 진행률"
        :value="`${kpiAvgProgress}%`"
        caption="태스크 우선 · 시간 기반 폴백"
      />
      <KpiStatCard
        label="참여 협력사 수"
        :value="kpiPartnerCount"
        :caption="kpiPartnerCount === 0 ? '협력사 미등록' : '고유 협력사 합산'"
      />
      <KpiStatCard
        label="이번 주 마감"
        :value="kpiThisWeekDeadline"
        :caption="kpiThisWeekDeadline === 0 ? '7일 내 마감 없음' : '캠페인+태스크'"
        :positive="kpiThisWeekDeadline > 0"
      />
    </section>

    <!-- 그리드: 캠페인 진행 현황 + 다가오는 일정 -->
    <section class="hq-grid">
      <article class="hq-panel hq-panel--wide">
        <div class="hq-panel__header">
          <h3>내 캠페인 진행 현황</h3>
          <div class="hq-panel__filters" role="tablist">
            <button
              v-for="m in progressModes"
              :key="m.id"
              type="button"
              role="tab"
              :aria-selected="progressMode === m.id"
              class="hq-panel__filter"
              :class="{ 'hq-panel__filter--active': progressMode === m.id }"
              @click="progressMode = m.id"
            >
              {{ m.label }}
            </button>
          </div>
        </div>
        <div v-if="loading && currentCampaigns.length === 0" class="hq-empty-hint">
          불러오는 중...
        </div>
        <div v-else-if="currentCampaigns.length === 0" class="hq-empty-hint">
          {{ scope === 'org' ? '우리 조직이 참여하는 캠페인이 없습니다.' : '내가 멤버로 등록된 캠페인이 없습니다.' }}
        </div>
        <div v-else class="hq-progress-list">
          <CampaignProgressCard
            v-for="c in topCampaigns"
            :key="c.id ?? c.idx"
            :campaign="c"
            :mode="progressMode"
            :task-stats="getTaskStats(c)"
          />
        </div>
        <div v-if="moreCount > 0" class="hq-panel__more">
          <button type="button" @click="goToCampaignList">
            더보기 ({{ moreCount }}건) →
          </button>
        </div>
      </article>

      <article class="hq-panel">
        <div class="hq-panel__header">
          <h3>다가오는 일정 (7일)</h3>
        </div>
        <div v-if="upcomingTasks.length === 0" class="hq-empty-hint">
          7일 내 마감 예정 태스크가 없습니다.
        </div>
        <div v-else class="hq-upcoming-list">
          <UpcomingTaskItem v-for="t in upcomingTasks" :key="t.id ?? t.idx" :task="t" />
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.hq-dashboard {
  display: flex;
  width: 100%;
  max-width: 1600px;
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
}

/* HERO */
.hq-hero {
  display: flex;
  min-height: 88px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}
.hq-hero__copy { display: flex; flex-direction: column; gap: 4px; }
.hq-hero__eyebrow {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}
.hq-hero__title {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
}
.hq-hero__org {
  color: var(--muted-text);
  font-weight: 600;
  font-size: 16px;
  margin-left: 4px;
}
.hq-hero__sub {
  color: var(--subtle-text);
  font-size: 13px;
}

/* ERROR */
.hq-error-banner {
  padding: 10px 14px;
  border: 1px solid var(--color-danger);
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
  border-radius: var(--radius-md);
  font-size: 13px;
}

/* STATS */
.hq-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

/* GRID */
.hq-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 16px;
}

.hq-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
  padding: 20px;
}

.hq-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.hq-panel__header h3 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

/* 필터 토글 */
.hq-panel__filters {
  display: inline-flex;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  padding: 3px;
  gap: 2px;
}
.hq-panel__filter {
  padding: 4px 12px;
  border: 0;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: background 0.15s, color 0.15s;
}
.hq-panel__filter:hover { color: var(--text-primary); }
.hq-panel__filter--active {
  background: var(--color-primary-500);
  color: #fff;
}

/* 리스트 */
.hq-progress-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hq-upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 비어있음 안내 */
.hq-empty-hint {
  color: var(--muted-text);
  font-size: 13px;
  padding: 28px 0;
  text-align: center;
}

/* 더보기 */
.hq-panel__more {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
.hq-panel__more button {
  padding: 6px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: border-color 0.15s, color 0.15s;
}
.hq-panel__more button:hover {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
}

@media (max-width: 1100px) {
  .hq-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .hq-grid { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .hq-hero { flex-direction: column; align-items: flex-start; }
  .hq-stats { grid-template-columns: 1fr; }
}
</style>
