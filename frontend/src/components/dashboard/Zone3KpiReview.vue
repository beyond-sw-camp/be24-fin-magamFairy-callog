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
import { ApproveAdReviewRequest, RejectAdReviewRequest } from '@/api/adcheck'

const router = useRouter()
const auth = useAuthStore()
const store = useDashboardStore()
const { prefs } = useDashboardZonePrefs()

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
  { key: 'GROWTH', label: '성장' },
  { key: 'FINANCIAL', label: '재무' },
  { key: 'BRAND', label: '브랜드' },
  { key: 'OPERATIONAL', label: '운영' },
  { key: 'SUSTAINABILITY', label: '지속가능성' },
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
    const key = g.category ?? (g.esgCategory != null ? 'SUSTAINABILITY' : null)
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

/* ─── P2: 검수 (검수 목록 ↔ 검수 결과 토글) ─── */
const reviewMode = ref('list') // 'list'=검수 목록(내가 검수할 것) / 'result'=검수 결과(우리가 제출한 것)
const canReview = computed(() => reviewMode.value === 'list')
const reviewLabel = computed(() => (reviewMode.value === 'list' ? '검수 목록' : '검수 결과'))

function publicIdByCampaignId(id) {
  if (id == null) return null
  const hit = (store.myCampaigns ?? []).find((c) => (c.idx ?? c.id) === id)
  return hit?.id ?? hit?.publicId ?? null
}
function openCampaign(id) {
  const pid = publicIdByCampaignId(id)
  if (pid) router.push(`/campaigns/${pid}`)
}
const reviewSource = computed(() => {
  const q = store.adReviewQueue ?? {}
  return reviewMode.value === 'list' ? (q.toReview ?? []) : (q.mine ?? [])
})
const reviewItems = computed(() => reviewSource.value.map((r) => ({
  requestId: r.requestId,
  campaignId: r.campaignId,
  name: r.fileName ?? '검수 요청',
  campaign: r.campaignName ?? '',
  requester: r.requesterName ?? '',
  status: String(r.requestStatus ?? '').toUpperCase(),
})))

/* 요청자(파트너) 입장 — 내 제출물의 검수 결과 상태 배지 */
function statusLabel(s) {
  return ({ APPROVED: '승인됨', REJECTED: '반려됨', REQUESTED: '검수중' })[s] ?? '검수중'
}
function statusTone(s) {
  return ({ APPROVED: 'ok', REJECTED: 'no', REQUESTED: 'wait' })[s] ?? 'wait'
}

/* ✓/✗ → 컨펌 모달 → 실제 PATCH 호출 */
const confirmState = ref(null) // { item, action: 'approve' | 'reject' }
const submitting = ref(false)
function askConfirm(item, action) { confirmState.value = { item, action } }
function cancelConfirm() { if (!submitting.value) confirmState.value = null }
async function doConfirm() {
  if (!confirmState.value) return
  const { item, action } = confirmState.value
  submitting.value = true
  try {
    if (action === 'approve') {
      await ApproveAdReviewRequest(item.campaignId, item.requestId, {})
    } else {
      await RejectAdReviewRequest(item.campaignId, item.requestId, { reason: '대시보드에서 반려' })
    }
    await store.loadZoneExtras?.() // 검수큐 등 재로드
  } catch (e) {
    console.warn('[zone3] 검수 처리 실패', e)
  } finally {
    submitting.value = false
    confirmState.value = null
  }
}

</script>

<template>
  <section class="card zone3" aria-label="KPI와 검수">
    <div class="card-h">
      <div class="card-h-ttl">
        <h2>{{ page === 0 ? 'KPI 트래커' : '검수' }}</h2>
        <span class="card-dot" />
        <p class="lede">{{ page === 0 ? '카테고리별 달성률' : reviewLabel }}</p>
      </div>
      <div class="z3-controls">
        <div v-if="page === 0" class="gn-seg">
          <button class="gn-btn" :class="{ 'is-on': granularity === 'week' }" @click="granularity = 'week'">주간</button>
          <button class="gn-btn" :class="{ 'is-on': granularity === 'month' }" @click="granularity = 'month'">월간</button>
        </div>
        <div v-else class="gn-seg">
          <button class="gn-btn" :class="{ 'is-on': reviewMode === 'list' }" @click="reviewMode = 'list'">검수 목록</button>
          <button class="gn-btn" :class="{ 'is-on': reviewMode === 'result' }" @click="reviewMode = 'result'">검수 결과</button>
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

      <!-- P2: 검수 (광고검수 요청 — 인라인 ✓/✗ + 컨펌 모달) -->
      <div v-else :key="'review'" class="z3-body">
        <ul v-if="reviewItems.length" class="z3-review-list">
          <li v-for="it in reviewItems" :key="it.requestId" class="z3-review-row">
            <div
              class="z3-review-mid"
              :class="{ 'is-click': it.campaignId != null }"
              @click="openCampaign(it.campaignId)"
            >
              <div class="z3-review-name">{{ it.name }}</div>
              <div class="z3-review-sub">
                <span v-if="it.campaign">{{ it.campaign }}</span>
                <span v-if="it.requester" class="z3-review-who">· 제출자 {{ it.requester }}</span>
              </div>
            </div>
            <div class="z3-acts">
              <template v-if="canReview">
                <button class="z3-act approve" title="승인" @click.stop="askConfirm(it, 'approve')">✓</button>
                <button class="z3-act reject" title="반려" @click.stop="askConfirm(it, 'reject')">✗</button>
              </template>
              <span v-else class="z3-status" :class="statusTone(it.status)">{{ statusLabel(it.status) }}</span>
              <button class="z3-act open" title="검수 페이지 열기" @click.stop="openCampaign(it.campaignId)">›</button>
            </div>
          </li>
        </ul>
        <div v-else class="z3-empty">
          {{ reviewMode === 'list' ? '검수할 요청이 없습니다.' : '제출한 검수 요청이 없습니다.' }}
        </div>

        <!-- 승인/반려 컨펌 모달 -->
        <Transition name="z3-modal">
          <div v-if="confirmState" class="z3-modal-backdrop" @click="cancelConfirm">
            <div class="z3-modal" role="dialog" aria-modal="true" @click.stop>
              <div class="z3-modal-msg">
                <strong>{{ confirmState.item.name }}</strong>
                <span>{{ confirmState.action === 'approve' ? ' 검수를 승인하시겠습니까?' : ' 검수를 반려하시겠습니까?' }}</span>
              </div>
              <div class="z3-modal-acts">
                <button class="z3-modal-btn no" :disabled="submitting" @click="cancelConfirm">아니오</button>
                <button class="z3-modal-btn yes" :class="confirmState.action" :disabled="submitting" @click="doConfirm">
                  {{ submitting ? '처리 중…' : '예' }}
                </button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.zone3 { display: flex; flex-direction: column; }
.card-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 12px; }
.card-h-ttl { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.card-dot { width: 9px; height: 9px; border-radius: 999px; background: var(--lp-primary); align-self: center; flex-shrink: 0; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; flex-shrink: 0; }
.card-h .lede { margin: 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.z3-controls { display: inline-flex; align-items: center; gap: 10px; }
.gn-seg { display: inline-flex; background: var(--lp-surface-soft); border-radius: 999px; padding: 3px; gap: 2px; }
.gn-btn { padding: 4px 10px; font-size: 11px; font-weight: 600; color: var(--lp-text-muted); border: 0; background: transparent; border-radius: 999px; cursor: pointer; }
.gn-btn.is-on { background: var(--lp-surface); color: var(--lp-primary-deep); box-shadow: 0 1px 3px rgba(63,52,99,.10); }

.zone-nav { display: inline-flex; align-items: center; gap: 6px; }
.nav-btn { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-primary-deep); cursor: pointer; font-size: 14px; line-height: 1; transition: background .15s, transform .12s; }
.nav-btn:hover { background: var(--lp-surface-soft); }
.nav-btn:active { transform: scale(0.94); }
.nav-ind { font-size: 11px; font-weight: 600; color: var(--lp-text-faint); min-width: 36px; text-align: center; font-variant-numeric: tabular-nums; }

.z3-body { flex: 1; min-height: 0; display: flex; flex-direction: column; position: relative; }

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
.z3-bar { width: 18px; min-height: 6px; border-radius: 999px; transition: height .6s cubic-bezier(.4,0,.2,1); transform-origin: bottom; animation: lp-grow-y .55s cubic-bezier(.4,0,.2,1) both; }
.z3-bar--actual { animation-delay: .06s; }
.z3-bar--target { background: linear-gradient(180deg, var(--lp-primary), var(--lp-primary-strong)); }
.z3-bar--actual { background: linear-gradient(180deg, #E2F079, var(--lp-lime)); }
.z3-lbl { margin-top: 8px; font-size: 10.5px; font-weight: 600; color: var(--lp-text-muted); }

/* review list */
.z3-review-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 7px; overflow-y: auto; flex: 1; }
.z3-review-row { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-radius: 12px; background: var(--lp-surface-soft); animation: lp-rise .4s cubic-bezier(.4,0,.2,1) both; }
.z3-review-row:nth-child(2) { animation-delay: .06s; }
.z3-review-row:nth-child(3) { animation-delay: .12s; }
.z3-review-row:nth-child(n+4) { animation-delay: .18s; }
.z3-review-mid { flex: 1; min-width: 0; }
.z3-review-mid.is-click { cursor: pointer; }
.z3-review-mid.is-click:hover .z3-review-name { color: var(--lp-primary-deep); }
.z3-review-name { font-size: 13px; font-weight: 700; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z3-review-sub { font-size: 11px; color: var(--lp-text-faint); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z3-review-who { color: var(--lp-text-muted); }
/* 인라인 ✓/✗/› 액션 */
.z3-acts { display: flex; gap: 6px; flex-shrink: 0; }
.z3-act { width: 30px; height: 30px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; cursor: pointer; border: 0; transition: transform .12s, box-shadow .15s; }
.z3-act:hover { transform: scale(1.08); box-shadow: 0 4px 10px rgba(63,52,99,.18); }
.z3-act.approve { background: var(--lp-lime); color: var(--lp-primary-deep); }
.z3-act.reject { background: rgba(226,91,73,.16); color: #E25B49; }
.z3-act.open { background: var(--lp-surface); color: var(--lp-primary-deep); border: 1px solid var(--lp-border); }
/* 요청자 입장 — 결과 상태 배지 */
.z3-status { font-size: 10.5px; font-weight: 800; padding: 4px 11px; border-radius: 999px; flex-shrink: 0; white-space: nowrap; }
.z3-status.ok { background: var(--lp-lime-soft, #EAF2A8); color: #4F7A2E; }
.z3-status.no { background: rgba(226,91,73,.16); color: #E25B49; }
.z3-status.wait { background: var(--accent-soft); color: var(--lp-primary-deep); }

/* 승인/반려 컨펌 모달 */
.z3-modal-backdrop { position: absolute; inset: 0; background: rgba(42,36,64,.42); backdrop-filter: blur(2px); display: flex; align-items: center; justify-content: center; z-index: 40; }
.z3-modal { background: var(--lp-surface); border-radius: var(--r-lg, 18px); padding: 20px; width: min(280px, 86%); box-shadow: 0 16px 40px rgba(63,52,99,.28); text-align: center; }
.z3-modal-msg { font-size: 13.5px; color: var(--lp-text); line-height: 1.5; }
.z3-modal-msg strong { color: var(--lp-primary-deep); }
.z3-modal-acts { display: flex; gap: 8px; margin-top: 16px; }
.z3-modal-btn { flex: 1; padding: 9px 0; border-radius: 999px; font-size: 12.5px; font-weight: 700; cursor: pointer; border: 0; }
.z3-modal-btn.no { background: var(--lp-surface-soft); color: var(--lp-text-muted); }
.z3-modal-btn.yes.approve { background: var(--lp-lime); color: var(--lp-primary-deep); }
.z3-modal-btn.yes.reject { background: #E25B49; color: #fff; }
.z3-modal-btn:disabled { opacity: .6; cursor: not-allowed; }
.z3-modal-enter-active, .z3-modal-leave-active { transition: opacity .2s ease; }
.z3-modal-enter-from, .z3-modal-leave-to { opacity: 0; }

.z3-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 24px; text-align: center; }

.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(16px); }
.page-slide-leave-to { opacity: 0; transform: translateX(-16px); }
</style>
