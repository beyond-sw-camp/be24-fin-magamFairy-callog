<script setup>
/**
 * Zone 3 — KPI & 검수 (2 페이지)
 *  P1: KPI 트래커 (기존 유지) — quarterGoals 카테고리별 달성률 막대 + [주간|월간]
 *  P2: 검수 (review-queue) — 권한별 버튼
 *      PM/매니저: "내가 검수할 것" / 파트너: "내가 제출한 것"
 */
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardZonePrefs } from '@/composables/useDashboardZonePrefs'

const router = useRouter()
const auth = useAuthStore()
const store = useDashboardStore()
const { prefs, setZoneDefault } = useDashboardZonePrefs()

const PAGE_COUNT = 2
const page = ref(prefs.value.zone3 ?? 0)
function shift(d) { page.value = (page.value + d + PAGE_COUNT) % PAGE_COUNT }
watch(() => prefs.value.zone3, (v) => { if (Number.isInteger(v)) page.value = v })

/* ─── 권한 (HqDashboardView 와 동일 규칙) ─── */
const orgType = computed(() => String(auth.user?.organization?.type ?? '').toUpperCase())
const isPartner = computed(() => orgType.value === 'EXTERNAL_PARTNER')

/* ─── P1: KPI 트래커 ─── */
const granularity = ref('month')
const KPI_CATS = [
  { key: 'IMPRESSION', label: '노출' },
  { key: 'ENGAGEMENT', label: '참여' },
  { key: 'CONVERSION', label: '전환' },
  { key: 'REVENUE', label: '매출' },
  { key: 'BRAND', label: '브랜드' },
  { key: 'ESG', label: 'ESG' },
]
function granularityFactor(g) { return g === 'week' ? 0.78 : 1 }
function fmtCompact(v) {
  const n = Number(v) || 0
  if (n >= 100_000_000) return (n / 100_000_000).toFixed(1).replace(/\.0$/, '') + '억'
  if (n >= 10_000) return (Math.round(n / 1000) / 10).toString().replace(/\.0$/, '') + '만'
  if (n >= 1000) return (Math.round(n / 100) / 10).toString() + 'k'
  return Math.round(n).toLocaleString()
}
/* 자기 조직 KPI 의 카테고리별 목표값·실제값 합계 → 목표 vs 실제 듀얼 막대.
   막대 높이는 카테고리 목표=100% 기준으로 정규화(단위가 카테고리마다 달라 절대값 공유 스케일 불가). */
const kpiBars = computed(() => {
  const goals = store.quarterGoals ?? []
  const buckets = {}
  goals.forEach((g) => {
    const key = g.esgCategory != null ? 'ESG' : g.category
    if (!key) return
    if (!buckets[key]) buckets[key] = { target: 0, actual: 0 }
    buckets[key].target += Number(g.targetValue) || 0
    buckets[key].actual += Number(g.actualValue) || 0
  })
  const factor = granularityFactor(granularity.value)
  return KPI_CATS.map((c) => {
    const b = buckets[c.key] ?? { target: 0, actual: 0 }
    const actualScaled = b.actual * factor
    const actualPct = b.target > 0 ? Math.max(0, Math.min(100, Math.round((actualScaled / b.target) * 100))) : 0
    return {
      lbl: c.label,
      target: b.target,
      actual: actualScaled,
      targetPct: b.target > 0 ? 100 : 0,
      actualPct,
      hasData: b.target > 0,
    }
  })
})
const hasKpi = computed(() => kpiBars.value.some((b) => b.hasData))

/* ─── P2: 검수 ─── */
const reviewMode = computed(() => (isPartner.value ? 'submitted' : 'toReview'))
const reviewLabel = computed(() => (isPartner.value ? '내가 제출한 것' : '내가 검수할 것'))

function publicIdByCampaignId(id) {
  if (id == null) return null
  const hit = (store.myCampaigns ?? []).find((c) => (c.idx ?? c.id) === id)
  return hit?.id ?? hit?.publicId ?? null
}
function openCampaign(id) {
  const pid = publicIdByCampaignId(id)
  if (pid) router.push(`/campaigns/${pid}`)
}
const PRIORITY_LABEL = { CRITICAL: '긴급', HIGH: '높음', MEDIUM: '보통', LOW: '낮음' }
const reviewItems = computed(() => (store.reviewQueue ?? []).map((r) => ({
  id: r.taskId,
  campaignId: r.campaignId,
  name: r.taskName ?? '검수 항목',
  campaign: r.campaignName ?? '',
  assignee: r.assigneeName ?? '',
  priority: PRIORITY_LABEL[String(r.priority ?? '').toUpperCase()] ?? '',
})))

/* ⋯ 기본 화면 지정 */
const menuOpen = ref(false)
function makeDefault() { setZoneDefault('zone3', page.value); menuOpen.value = false }
</script>

<template>
  <section class="card zone3" aria-label="KPI와 검수">
    <div class="card-h">
      <div>
        <h2>{{ page === 0 ? 'KPI 트래커' : '검수' }}</h2>
        <p class="lede">{{ page === 0 ? '카테고리별 달성률' : reviewLabel }}</p>
      </div>
      <div class="z3-controls">
        <div v-if="page === 0" class="gn-seg">
          <button class="gn-btn" :class="{ 'is-on': granularity === 'week' }" @click="granularity = 'week'">주간</button>
          <button class="gn-btn" :class="{ 'is-on': granularity === 'month' }" @click="granularity = 'month'">월간</button>
        </div>
        <div v-else class="gn-seg">
          <button class="gn-btn is-on">{{ reviewLabel }}</button>
        </div>
        <div class="z3-menu-wrap">
          <button class="z3-dots" aria-label="옵션" @click="menuOpen = !menuOpen">⋯</button>
          <Transition name="z3-pop">
            <div v-if="menuOpen" class="z3-menu" @mouseleave="menuOpen = false">
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
      <!-- P1: KPI 트래커 -->
      <div v-if="page === 0" :key="'kpi'" class="z3-body">
        <template v-if="hasKpi">
          <div class="z3-legend">
            <span class="z3-leg z3-leg--actual">실제</span>
            <span class="z3-leg z3-leg--target">목표</span>
          </div>
          <div class="z3-bars-wrap">
            <div class="z3-bars dual">
              <div
                v-for="(b, i) in kpiBars"
                :key="i"
                class="z3-bar-col"
                :title="b.hasData ? `목표 ${fmtCompact(b.target)} · 실제 ${fmtCompact(b.actual)} (${b.actualPct}%)` : '데이터 없음'"
              >
                <div class="z3-bar-pair">
                  <div class="z3-bar z3-bar--actual" :style="{ height: b.actualPct + '%' }" />
                  <div class="z3-bar z3-bar--target" :style="{ height: b.targetPct + '%' }" />
                </div>
                <span class="z3-lbl">{{ b.lbl }}</span>
              </div>
            </div>
          </div>
        </template>
        <div v-else class="z3-empty">KPI 데이터가 없습니다.</div>
      </div>

      <!-- P2: 검수 -->
      <div v-else :key="'review'" class="z3-body">
        <ul v-if="reviewItems.length" class="z3-review-list">
          <li
            v-for="it in reviewItems"
            :key="it.id"
            class="z3-review-row"
            :class="{ 'is-click': it.campaignId != null }"
            @click="openCampaign(it.campaignId)"
          >
            <div class="z3-review-mid">
              <div class="z3-review-name">{{ it.name }}</div>
              <div class="z3-review-sub">
                <span v-if="it.campaign">{{ it.campaign }}</span>
                <span v-if="it.assignee" class="z3-review-who">· {{ isPartner ? '검수자' : '제출자' }} {{ it.assignee }}</span>
              </div>
            </div>
            <span v-if="it.priority" class="z3-review-pri">{{ it.priority }}</span>
          </li>
        </ul>
        <div v-else class="z3-empty">
          {{ isPartner ? '제출한 검수 항목이 없습니다.' : '검수할 항목이 없습니다.' }}
        </div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.zone3 { display: flex; flex-direction: column; }
.card-h { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; gap: 12px; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; }
.card-h .lede { margin: 4px 0 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); }

.z3-controls { display: inline-flex; align-items: center; gap: 10px; }
.gn-seg { display: inline-flex; background: var(--lp-surface-soft); border-radius: 999px; padding: 3px; gap: 2px; }
.gn-btn { padding: 4px 10px; font-size: 11px; font-weight: 600; color: var(--lp-text-muted); border: 0; background: transparent; border-radius: 999px; cursor: pointer; }
.gn-btn.is-on { background: var(--lp-surface); color: var(--lp-primary-deep); box-shadow: 0 1px 3px rgba(63,52,99,.10); }
.z3-menu-wrap { position: relative; }
.z3-dots { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-text-muted); cursor: pointer; font-size: 15px; line-height: 1; }
.z3-dots:hover { background: var(--lp-surface-soft); }
.z3-menu { position: absolute; top: calc(100% + 6px); right: 0; background: var(--lp-surface); border: 1px solid var(--lp-border); border-radius: 10px; box-shadow: 0 8px 24px rgba(63,52,99,.16); padding: 5px; z-index: 30; white-space: nowrap; }
.z3-menu button { display: block; padding: 7px 12px; font-size: 12.5px; font-weight: 600; color: var(--lp-text); background: transparent; border: 0; border-radius: 7px; cursor: pointer; }
.z3-menu button:hover { background: var(--lp-surface-soft); color: var(--lp-primary-deep); }
.z3-pop-enter-active, .z3-pop-leave-active { transition: opacity .15s ease, transform .15s ease; }
.z3-pop-enter-from, .z3-pop-leave-to { opacity: 0; transform: translateY(-4px); }

.zone-nav { display: inline-flex; align-items: center; gap: 6px; }
.nav-btn { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-primary-deep); cursor: pointer; font-size: 14px; line-height: 1; transition: background .15s, transform .12s; }
.nav-btn:hover { background: var(--lp-surface-soft); }
.nav-btn:active { transform: scale(0.94); }
.nav-ind { font-size: 11px; font-weight: 600; color: var(--lp-text-faint); min-width: 36px; text-align: center; font-variant-numeric: tabular-nums; }

.z3-body { flex: 1; min-height: 0; display: flex; flex-direction: column; }

/* KPI dual bars (목표 vs 실제) */
.z3-legend { display: flex; gap: 16px; justify-content: flex-end; margin-bottom: 6px; }
.z3-leg { display: inline-flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 600; color: var(--lp-text-muted); }
.z3-leg::before { content: ''; width: 9px; height: 9px; border-radius: 3px; }
.z3-leg--target::before { background: var(--lp-primary); }
.z3-leg--actual::before { background: var(--lp-lime); }

.z3-bars-wrap { flex: 1; min-height: 0; display: flex; }
.z3-bars.dual { flex: 1; display: grid; grid-auto-flow: column; grid-auto-columns: 1fr; align-items: stretch; gap: 12px; padding: 0 4px 24px; position: relative; }
.z3-bars.dual::before, .z3-bars.dual::after { content: ''; position: absolute; left: 0; right: 0; border-top: 1px dashed var(--lp-border); pointer-events: none; }
.z3-bars.dual::before { top: 33%; }
.z3-bars.dual::after { top: 66%; }
.z3-bar-col { display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; min-width: 0; }
.z3-bar-pair { flex: 1; min-height: 0; width: 100%; display: flex; align-items: flex-end; justify-content: center; gap: 6px; }
.z3-bar { width: 18px; min-height: 6px; border-radius: 999px; transition: height .6s cubic-bezier(.4,0,.2,1); }
.z3-bar--target { background: linear-gradient(180deg, var(--lp-primary), var(--lp-primary-strong)); }
.z3-bar--actual { background: linear-gradient(180deg, #E2F079, var(--lp-lime)); }
.z3-lbl { margin-top: 8px; font-size: 10.5px; font-weight: 600; color: var(--lp-text-muted); }

/* review list */
.z3-review-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 7px; overflow-y: auto; flex: 1; }
.z3-review-row { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-radius: 12px; background: var(--lp-surface-soft); }
.z3-review-row.is-click { cursor: pointer; }
.z3-review-row.is-click:hover { background: var(--lp-border); }
.z3-review-mid { flex: 1; min-width: 0; }
.z3-review-name { font-size: 13px; font-weight: 700; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z3-review-sub { font-size: 11px; color: var(--lp-text-faint); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z3-review-who { color: var(--lp-text-muted); }
.z3-review-pri { font-size: 10.5px; font-weight: 700; color: #FF7A4D; background: rgba(255,138,92,.16); padding: 3px 10px; border-radius: 999px; flex-shrink: 0; }

.z3-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 24px; text-align: center; }

.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(16px); }
.page-slide-leave-to { opacity: 0; transform: translateX(-16px); }
</style>
