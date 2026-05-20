<script setup>
import { computed, onMounted, onBeforeUnmount, nextTick, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useDashboardStore } from '@/stores/dashboard'
import { useTeamTaskStore } from '@/stores/teamTask'
import { useNotificationsStore } from '@/stores/notifications'
import { useUserSettingsStore } from '@/stores/userSettings'
import { ListCampaignMembers } from '@/api/campaigns'
import VueApexCharts from 'vue3-apexcharts'
import { gsap } from 'gsap'
import { CountUp } from 'countup.js'

const ApexChart = VueApexCharts
const notiStore = useNotificationsStore()
const userSettings = useUserSettingsStore()

const router = useRouter()
const authStore = useAuthStore()
const dashboardStore = useDashboardStore()
const teamTaskStore = useTeamTaskStore()

/* ═══════════ 분기 헬퍼 ═══════════ */
function currentQuarterCode() {
  const d = new Date()
  const q = Math.ceil((d.getMonth() + 1) / 3)
  return `${d.getFullYear()}-Q${q}`
}
function previousQuarter(periodCode) {
  const m = String(periodCode || '').match(/^(\d{4})-Q([1-4])$/)
  if (!m) return periodCode
  let y = Number(m[1]); let q = Number(m[2])
  q -= 1
  if (q < 1) { q = 4; y -= 1 }
  return `${y}-Q${q}`
}
function yoyQuarter(periodCode) {
  const m = String(periodCode || '').match(/^(\d{4})-Q([1-4])$/)
  if (!m) return periodCode
  return `${Number(m[1]) - 1}-Q${m[2]}`
}

/* ═══════════ 필터 상태 ═══════════ */
/** 'current' | 'prev' | 'yoy' */
const quarterKey = ref('current')
const compareMode = ref(false)
/** 캠페인 진행 영역 클라이언트 필터: 'all' | 'live' | 'done' | 'cancelled' */
const campaignStatusFilter = ref('all')

const currentPeriod = computed(() => {
  const today = currentQuarterCode()
  if (quarterKey.value === 'prev') return previousQuarter(today)
  if (quarterKey.value === 'yoy')  return yoyQuarter(today)
  return today
})
const comparePeriod = computed(() => previousQuarter(currentPeriod.value))

const PERIOD_TABS = [
  { key: 'current', label: '이번 분기' },
  { key: 'prev',    label: '전 분기' },
  { key: 'yoy',     label: '전년 동기' },
]

onMounted(async () => {
  await dashboardStore.loadAll(currentPeriod.value)
  if (compareMode.value) await dashboardStore.loadCompare(comparePeriod.value)
  // 캠페인 진행률 동기화 — Sidebar2와 동일한 teamTaskStore 사용
  teamTaskStore.fetch()
})

/* 분기 키 변경 시 → 메인 + (옵션) 비교 데이터 재로드 */
watch(currentPeriod, async (period) => {
  await dashboardStore.loadAll(period)
  if (compareMode.value) await dashboardStore.loadCompare(comparePeriod.value)
})

/* 비교 토글 변경 시 → 비교 스냅샷만 로드/클리어 */
watch(compareMode, async (on) => {
  if (on) await dashboardStore.loadCompare(comparePeriod.value)
  else dashboardStore.clearCompare()
})

async function retryDashboard() {
  await dashboardStore.loadAll(currentPeriod.value)
  if (compareMode.value) await dashboardStore.loadCompare(comparePeriod.value)
}

/**
 * 모든 endpoint 응답이 끝난 후에만 에러 판단.
 * Promise.allSettled 라 일부 endpoint 가 먼저 실패해도 나머지가 아직 'loading' 인 동안엔
 * 배너 노출 보류 (= 로딩 중 깜빡임 방지).
 */
const hasError = computed(() => {
  if (dashboardStore.loading) return false
  const statuses = Object.values(dashboardStore.status ?? {})
  if (statuses.length === 0) return false
  if (statuses.some((s) => s === 'loading')) return false
  return statuses.some((s) => s === 'error')
})

function parseNumeric(v, fallback = 0) {
  if (typeof v === 'number') return v
  if (typeof v !== 'string') return fallback
  const cleaned = v.replace(/[^0-9.\-]/g, '')
  const n = Number(cleaned)
  return Number.isNaN(n) ? fallback : n
}

/* ───── 사용자·권한 ───── */
const userName = computed(() => authStore.user?.name ?? authStore.user?.id ?? '운영자')
const orgName = computed(
  () => authStore.user?.organization?.name ?? authStore.user?.companyName ?? '한화 그룹',
)
const role = computed(() => {
  const r = String(authStore.user?.role ?? '').toUpperCase()
  if (r === 'ROLE_GENERAL_MANAGER' || r === 'ROLE_ADMIN') return 'GM'
  if (r === 'ROLE_MANAGER') return 'MGR'
  return 'USR'
})
const roleLabel = computed(() => ({ GM: '총괄 매니저', MGR: '매니저', USR: '실무자' }[role.value]))

/* 조직 스코프 — 백엔드 summary.scope: HQ | AFFILIATE | EXTERNAL_PARTNER | STAFF */
const orgScope = computed(() => {
  const fromApi = dashboardStore.summary?.scope
  if (fromApi) return fromApi
  // summary 미응답 시 사용자 organization.type 으로 폴백 (실제 인증 사용자 데이터)
  const t = String(authStore.user?.organization?.type ?? '').toUpperCase()
  if (t === 'HQ' || t === 'AFFILIATE' || t === 'EXTERNAL_PARTNER') return t
  return 'HQ'
})
const orgScopeLabel = computed(() => ({
  HQ: '본사 · 자기 조직',
  AFFILIATE: '계열사 · 자기 조직',
  EXTERNAL_PARTNER: '외부 파트너 · 참여 캠페인',
  STAFF: '실무자 · 본인 캠페인',
}[orgScope.value] ?? '본사 · 자기 조직'))

/* ═══════════ Row 1 — KPI 6-up (모든 value 0 default — 데이터 없으면 0 표시) ═══════════ */
const TODAY_KPIS = [
  { key: 'progress', label: '진행률',        value: 0, unit: '%',  delta: '', deltaPositive: true,  icon: '📈', bg: '#E7E1FF', iconBg: '#9D85FF' },
  { key: 'pass',     label: '검수 패스율',   value: 0, unit: '%',  delta: '', deltaPositive: true,  icon: '✅', bg: '#FFE8DD', iconBg: '#FF8A5C' },
  { key: 'match',    label: '매칭 평균 (5축)', value: 0, unit: '점', delta: '', deltaPositive: true,  icon: '🤝', bg: '#DCEEFA', iconBg: '#5DAFD8' },
  { key: 'asset',    label: '자산 LIVE',     value: 0, unit: '개',  delta: '', deltaPositive: true,  icon: '🛍', bg: '#D7EFDD', iconBg: '#6FBF87' },
  { key: 'partner',  label: '신규 협력사',   value: 0, unit: '곳',  delta: '', deltaPositive: true,  icon: '🏢', bg: '#FFE2DD', iconBg: '#FF7A6B' },
  { key: 'rfp',      label: 'RFP 응모',      value: 0, unit: '건',  delta: '', deltaPositive: true,  icon: '📜', bg: '#FFF1D6', iconBg: '#FFC36B' },
]

const ROLE_KPI = computed(() => {
  const s = dashboardStore.summary
  const cs = dashboardStore.compareSummary
  const useCompare = compareMode.value && cs

  if (role.value === 'GM') {
    const curr = s?.progressPct ?? 0
    let delta = ''
    let deltaPositive = true
    if (useCompare) {
      const d = pctDelta(curr, cs?.progressPct ?? 0)
      delta = `${d >= 0 ? '+' : ''}${d}%`
      deltaPositive = d >= 0
    } else if (s?.trend != null && s.trend !== 0) {
      // Backend 가 실 비교 데이터 있을 때만 숫자 반환. null/0 이면 "지난주" 표시 생략.
      delta = `${s.trend >= 0 ? '+' : ''}${s.trend}%p 지난주`
      deltaPositive = s.trend >= 0
    }
    return {
      key: 'gm', label: '분기 달성률',
      value: curr, unit: '%',
      delta, deltaPositive,
      icon: '🎯', bg: '#E7E1FF', iconBg: '#9D85FF',
    }
  }
  if (role.value === 'MGR') {
    const curr = s?.activeCampaigns ?? 0
    let delta = ''
    let deltaPositive = true
    if (useCompare) {
      const d = pctDelta(curr, cs?.activeCampaigns ?? 0)
      delta = `${d >= 0 ? '+' : ''}${d}%`
      deltaPositive = d >= 0
    }
    return {
      key: 'mgr', label: '진행 중 캠페인',
      value: curr, unit: '건',
      delta, deltaPositive,
      icon: '👥', bg: '#DCEEFA', iconBg: '#5DAFD8',
    }
  }
  const curr = s?.pendingReviews ?? 0
  let delta = ''
  let deltaPositive = true
  if (useCompare) {
    const d = pctDelta(curr, cs?.pendingReviews ?? 0)
    delta = `${d >= 0 ? '+' : ''}${d}%`
    deltaPositive = d >= 0
  }
  return {
    key: 'usr', label: '내 검수 대기',
    value: curr, unit: '건',
    delta, deltaPositive,
    icon: '✅', bg: '#D7EFDD', iconBg: '#6FBF87',
  }
})

/* scope별 4·5번 카드 라벨 — KPI 6-up 컨텍스트화 */
const KPI_SCOPE_OVERRIDES = {
  HQ:               { partner: { label: '신규 협력사', unit: '곳' }, rfp: { label: 'RFP 응모',  unit: '건' } },
  AFFILIATE:        { partner: { label: '참여 협력사', unit: '곳' }, rfp: { label: '우리 RFP',  unit: '건' } },
  EXTERNAL_PARTNER: { partner: { label: '활성 캠페인', unit: '건' }, rfp: { label: '내 RFP 응모', unit: '건' } },
  STAFF:            { partner: { label: '내 협력 캠페인', unit: '건' }, rfp: { label: '내 RFP 응모', unit: '건' } },
}

/* KPI 6-up — dashboardStore.summary 우선. 응답에 필드 없으면 0 표시. */
function summaryToKpiValues(s) {
  return [
    s?.progressPct ?? 0,
    s?.miniStats?.[0] ? parseNumeric(s.miniStats[0].value, 0) : 0,
    s?.miniStats?.[1] ? parseNumeric(s.miniStats[1].value, 0) : 0,
    s?.miniStats?.[2] ? parseNumeric(s.miniStats[2].value, 0) : 0,
    s?.newPartnerCount ?? 0,
    s?.rfpCount ?? 0,
  ]
}
function pctDelta(curr, prev) {
  const p = Number(prev) || 0
  const c = Number(curr) || 0
  if (p === 0) return c === 0 ? 0 : 100
  return Math.round(((c - p) / Math.abs(p)) * 100)
}

const KPI_LIST = computed(() => {
  const s = dashboardStore.summary
  const cs = dashboardStore.compareSummary
  const currVals = summaryToKpiValues(s)
  const prevVals = summaryToKpiValues(cs)

  const mapped = TODAY_KPIS.map((k, i) => {
    const v = currVals[i]
    const next = { ...k, value: v }
    if (compareMode.value && cs) {
      const d = pctDelta(v, prevVals[i])
      next.delta = `${d >= 0 ? '+' : ''}${d}%`
      next.deltaPositive = d >= 0
    } else {
      next.delta = ''
      next.deltaPositive = true
    }
    return next
  })
  const ov = KPI_SCOPE_OVERRIDES[orgScope.value] ?? KPI_SCOPE_OVERRIDES.HQ
  mapped[4].label = ov.partner.label; mapped[4].unit = ov.partner.unit
  mapped[5].label = ov.rfp.label;     mapped[5].unit = ov.rfp.unit
  return [ROLE_KPI.value, ...mapped.slice(1)]
})

/* ═══════════ Row 2-2 — 권한별 stat 카드 (mock 제거. backend 데이터만 사용) ═══════════ */
/**
 * GM 분기 KPI 달성률 — /organization-kpis 6개 표준 카테고리로 그루핑한 평균.
 * (노출 / 참여 / 전환 / 매출 / 브랜드 / ESG)
 */
const KPI_CATEGORY_BUCKETS = [
  { key: 'IMPRESSION', label: '노출',    short: '노', color: '#9D85FF' },
  { key: 'ENGAGEMENT', label: '참여',    short: '참', color: '#5DAFD8' },
  { key: 'CONVERSION', label: '전환',    short: '전', color: '#FF8A5C' },
  { key: 'REVENUE',    label: '매출',    short: '매', color: '#FFC36B' },
  { key: 'BRAND',      label: '브랜드',  short: '브', color: '#6FBF87' },
  { key: 'ESG',        label: 'ESG',     short: 'E',  color: '#FF7A6B' },
]
const ROLE_CARD = computed(() => {
  const s = dashboardStore.summary
  if (role.value === 'GM') {
    const goals = dashboardStore.quarterGoals ?? []
    // 카테고리별 평균: ESG는 esgCategory != null 인 것 모두 / 나머지는 category 일치
    const stats = KPI_CATEGORY_BUCKETS.map((b) => {
      const matched = goals.filter((g) => {
        if (b.key === 'ESG') return g.esgCategory != null
        return g.category === b.key
      })
      const pcts = matched
        .map((g) => g.achievementPercent ?? g.percent)
        .filter((v) => typeof v === 'number')
      const avg = pcts.length === 0 ? 0
        : Math.round(pcts.reduce((sum, v) => sum + v, 0) / pcts.length)
      return {
        label: b.label, short: b.short, color: b.color,
        value: avg, delta: 0, unit: '%',
      }
    })
    const subtitleByScope = {
      HQ:               `${currentPeriod.value} · 본사 OrgKpi 평균`,
      AFFILIATE:        `${currentPeriod.value} · 우리 조직 OrgKpi 평균`,
      EXTERNAL_PARTNER: `${currentPeriod.value} · 참여 캠페인 KPI 평균`,
      STAFF:            `${currentPeriod.value} · 내 캠페인 KPI 평균`,
    }
    return {
      title: '분기 KPI 달성률',
      subtitle: subtitleByScope[orgScope.value] ?? subtitleByScope.HQ,
      mainPct: s?.progressPct ?? 0,
      stats,
      cta: '분기 KPI 보기', ctaTo: '/organization-kpis',
    }
  }
  if (role.value === 'MGR') return {
    title: '우리 팀',
    stats: [
      { label: '진행 중',   value: s?.activeCampaigns ?? 0, delta: 0, unit: '건', color: '#9D85FF' },
      { label: '검수 대기', value: s?.pendingReviews ?? 0,  delta: 0, unit: '건', color: '#FF8A5C' },
    ],
    cta: '팀 보드', ctaTo: '/team-board',
  }
  return {
    title: '내 할 일',
    stats: [
      { label: '검수 대기', value: s?.pendingReviews ?? 0, delta: 0, unit: '건', color: '#9D85FF' },
    ],
    cta: '캘린더', ctaTo: '/calendar',
  }
})

/* ═══════════ Row 3-1 — 목표 vs 실적 그룹막대 (3 슬롯, 우측 정렬) ═══════════ */
function quarterMonthLabels(periodCode) {
  const m = String(periodCode || '').match(/^(\d{4})-Q([1-4])$/)
  if (!m) {
    const d = new Date()
    const q = Math.ceil((d.getMonth() + 1) / 3)
    const first = (q - 1) * 3 + 1
    return [`${first}월`, `${first + 1}월`, `${first + 2}월`]
  }
  const q = Number(m[2])
  const first = (q - 1) * 3 + 1
  return [`${first}월`, `${first + 1}월`, `${first + 2}월`]
}
/**
 * 3 슬롯 array 반환. 데이터 없는 슬롯(과거 미수집 달)은 null.
 * 우측 정렬 — 가장 최근 달이 항상 마지막 슬롯.
 *   예) 5월만 데이터 → [null, null, {5월}]
 *       5월·6월 데이터 → [null, {5월}, {6월}]
 *       4·5·6월 모두 → [{4월}, {5월}, {6월}]
 */
const TARGET_REALITY = computed(() => {
  const goals = dashboardStore.quarterGoals ?? []
  const labels = quarterMonthLabels(goals[0]?.periodCode || currentPeriod.value)
  if (goals.length === 0) return [null, null, null]
  // 슬롯 i: actual/target 합계 (null 슬롯은 어떤 KPI도 데이터 없음)
  const slots = [null, null, null]
  for (let i = 0; i < 3; i++) {
    let hasAny = false
    let sumA = 0
    let sumT = 0
    goals.forEach((g) => {
      const a = g.monthlyActuals?.[i]
      const t = g.monthlyTargets?.[i]
      if (a != null) { sumA += Number(a) || 0; hasAny = true }
      if (t != null) { sumT += Number(t) || 0; hasAny = true }
    })
    if (hasAny) slots[i] = { month: labels[i], actual: sumA, target: sumT }
  }
  // 우측 정렬: 데이터 있는 슬롯들을 추출해 마지막부터 채움
  const filled = slots.filter((s) => s != null)
  const out = [null, null, null]
  for (let i = 0; i < filled.length; i++) {
    out[3 - filled.length + i] = filled[i]
  }
  return out
})
const targetStats = computed(() => {
  const data = TARGET_REALITY.value.filter((m) => m != null)
  const totalA = data.reduce((s, m) => s + m.actual, 0)
  const totalT = data.reduce((s, m) => s + m.target, 0)
  const achieveRate = totalT > 0 ? Math.round((totalA / totalT) * 100) : 0
  const overMonths = data.filter((m) => m.actual >= m.target).length
  return { totalA, totalT, achieveRate, overMonths, monthCount: data.length }
})

/* 비교 기간의 월별 actual 합 (overlay 라인용) */
const COMPARE_REALITY = computed(() => {
  if (!compareMode.value) return [null, null, null]
  const goals = dashboardStore.compareQuarterGoals ?? []
  if (goals.length === 0) return [null, null, null]
  const slots = [null, null, null]
  for (let i = 0; i < 3; i++) {
    let hasAny = false
    let sumA = 0
    goals.forEach((g) => {
      const a = g.monthlyActuals?.[i]
      if (a != null) { sumA += Number(a) || 0; hasAny = true }
    })
    if (hasAny) slots[i] = sumA
  }
  // 우측 정렬 (현재 차트와 동일한 정렬 규칙)
  const filled = slots.filter((s) => s != null)
  const out = [null, null, null]
  for (let i = 0; i < filled.length; i++) {
    out[3 - filled.length + i] = filled[i]
  }
  return out
})

/* 차트 dynamic scale (3 슬롯 중 데이터 있는 것만 기준 — 비교 overlay 포함) */
const targetMax = computed(() => {
  const data = TARGET_REALITY.value.filter((m) => m != null)
  const compareVals = COMPARE_REALITY.value.filter((v) => v != null)
  const peak = Math.max(
    0,
    ...data.flatMap((m) => [m.actual, m.target]),
    ...compareVals,
  )
  return peak > 0 ? Math.ceil(peak * 1.15) : 200
})
function targetBarY(v) { return 260 - (v / targetMax.value) * 240 }
function targetBarH(v) { return Math.max(0, (v / targetMax.value) * 240) }
function fmtYAxis(v) {
  if (v >= 1_000_000) return (v / 1_000_000).toFixed(1) + 'M'
  if (v >= 1000) return Math.round(v / 100) / 10 + 'k'
  return Math.round(v).toString()
}

/* 비교 라인 path: 그룹 중심(105, 265, 425)에 점 찍어 polyline */
const compareLinePoints = computed(() => {
  const arr = COMPARE_REALITY.value
  const xs = [105, 265, 425] // 그룹 중심 (각 그룹 폭 160 / 시작 50+50/2)
  const pts = []
  for (let i = 0; i < 3; i++) {
    const v = arr[i]
    if (v != null) pts.push(`${xs[i]},${targetBarY(v)}`)
  }
  return pts.join(' ')
})
const compareLineDots = computed(() => {
  const arr = COMPARE_REALITY.value
  const xs = [105, 265, 425]
  const dots = []
  for (let i = 0; i < 3; i++) {
    const v = arr[i]
    if (v != null) dots.push({ x: xs[i], y: targetBarY(v), value: v })
  }
  return dots
})

/* ═══════════ Row 3-2 — 자산 카테고리 도넛 (backend assetCategories 기반) ═══════════ */
const ASSET_CAT_LABELS = {
  customer: '고객 자산',
  channel:  '채널 자산',
  space:    '공간 자산',
  voucher:  '상품/이용권 자산',
  content:  '콘텐츠/IP 자산',
}
const ASSET_COLORS = ['#9D85FF', '#FF8A5C', '#5DAFD8', '#6FBF87', '#FFC36B']
const ASSET_CATS = computed(() => {
  const map = dashboardStore.assetCategories ?? {}
  return Object.entries(ASSET_CAT_LABELS).map(([k, label], i) => ({
    key: k,
    type: label,
    count: Number(map[k]) || 0,
    color: ASSET_COLORS[i % ASSET_COLORS.length],
  }))
})
const assetTotal = computed(() => ASSET_CATS.value.reduce((s, c) => s + c.count, 0))
const assetSegments = computed(() => {
  const C = 2 * Math.PI * 50  // r=50
  const nonZero = ASSET_CATS.value.filter((c) => c.count > 0)
  const total = nonZero.reduce((s, c) => s + c.count, 0) || 1
  let acc = 0
  return nonZero.map((c) => {
    const len = (c.count / total) * C
    const seg = { ...c, length: len, gap: C - len, offset: -acc, pct: Math.round((c.count / total) * 100) }
    acc += len
    return seg
  })
})

/* ═══════════ Row 4-1 — 캠페인 진행 table (backend myCampaigns 기반) ═══════════ */
const STATUS_MAP = {
  live:      { label: 'LIVE',   cls: 'st--live' },
  running:   { label: 'LIVE',   cls: 'st--live' },
  active:    { label: 'LIVE',   cls: 'st--live' },
  review:    { label: 'REVIEW', cls: 'st--review' },
  in_review: { label: 'REVIEW', cls: 'st--review' },
  draft:     { label: 'DRAFT',  cls: 'st--draft' },
  completed: { label: 'DONE',   cls: 'st--draft' },
  archived:  { label: 'DONE',   cls: 'st--draft' },
}
const CAMPAIGN_PALETTE = ['#9D85FF', '#FF8A5C', '#5DAFD8', '#6FBF87', '#FFC36B', '#FF7A6B']
function deriveOwnerInitials(c) {
  if (c.initials) return c.initials
  if (!c.name) return '··'
  return c.name.replace(/\s+/g, '').slice(0, 2).toUpperCase()
}
function calcDDay(endDate) {
  if (!endDate) return null
  const end = new Date(endDate)
  if (Number.isNaN(end.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  end.setHours(0, 0, 0, 0)
  return Math.round((end - today) / 86400000)
}
function deriveProgress(c) {
  // Sidebar2와 동일하게 teamTaskStore의 태스크 완료율만 사용 — task가 없으면 0%
  const id = c.idx ?? c.id
  if (id == null) return 0
  const rate = teamTaskStore.completionRateByCampaignId[String(id)]
  return typeof rate === 'number' ? rate : 0
}
function statusOf(s) { return STATUS_MAP[s] ?? STATUS_MAP.draft }
function fmtDDay(d) { return d == null ? '·' : (d >= 0 ? `D-${d}` : `D+${-d}`) }
/** 캠페인 status 필터 키 → 매칭 status 집합 */
const CAMPAIGN_FILTER_MAP = {
  all:       null,
  live:      new Set(['live', 'running', 'active', 'review', 'in_review']),
  done:      new Set(['completed', 'archived']),
  cancelled: new Set(['cancelled', 'canceled', 'draft']),
}
const CAMPAIGN_FILTER_TABS = [
  { key: 'all',       label: '전체' },
  { key: 'live',      label: '진행중' },
  { key: 'done',      label: '완료' },
  { key: 'cancelled', label: '취소' },
]

const MY_CAMPAIGNS = computed(() => {
  const list = dashboardStore.myCampaigns ?? []
  if (list.length === 0) return []
  const set = CAMPAIGN_FILTER_MAP[campaignStatusFilter.value]
  const filtered = set == null
    ? list
    : list.filter((c) => set.has((c.status ?? 'draft').toLowerCase()))
  return filtered.slice(0, 6).map((c, i) => ({
    id: c.idx ?? c.id ?? i,
    name: c.name ?? '제목 없음',
    owner: deriveOwnerInitials(c),
    progress: deriveProgress(c),
    dDay: calcDDay(c.endDate),
    status: (c.status ?? 'draft').toLowerCase(),
    color: c.color || CAMPAIGN_PALETTE[i % CAMPAIGN_PALETTE.length],
  }))
})

/* ═══════════ Row 4-2 — 제휴사 ranking + sparkline ═══════════ */
function partnerKey(p) {
  return p.organizationId ?? p.partnerId ?? p.id ?? p.organizationName ?? p.name
}
/** 비교 기간의 partner → rank 인덱스 맵. 비교 모드 OFF 또는 데이터 없을 때 빈 맵. */
const COMPARE_PARTNER_RANK = computed(() => {
  if (!compareMode.value) return new Map()
  const arr = dashboardStore.comparePartnerProgress ?? []
  const map = new Map()
  arr.forEach((p, i) => {
    const k = partnerKey(p)
    if (k != null) map.set(k, i + 1)
  })
  return map
})

const PARTNER_RANK = computed(() => {
  const fromStore = dashboardStore.partnerProgress ?? []
  if (fromStore.length === 0) return []
  const colors = ['#9D85FF', '#FF8A5C', '#5DAFD8', '#6FBF87', '#FFC36B']
  const prevRanks = COMPARE_PARTNER_RANK.value
  return fromStore.slice(0, 5).map((p, i) => {
    const currRank = i + 1
    const k = partnerKey(p)
    const prevRank = prevRanks.has(k) ? prevRanks.get(k) : null
    let rankBadge = null
    if (compareMode.value) {
      if (prevRank == null) rankBadge = { type: 'new', label: 'NEW' }
      else if (prevRank === currRank) rankBadge = { type: 'same', label: '—' }
      else if (prevRank > currRank) rankBadge = { type: 'up', label: `▲${prevRank - currRank}` }
      else rankBadge = { type: 'down', label: `▼${currRank - prevRank}` }
    }
    return {
      rank: currRank,
      name: p.organizationName ?? p.name ?? '제휴사',
      score: p.averageKpiAchievementPercent ?? p.progress ?? p.score ?? 0,
      prevRank,
      delta: p.delta ?? 0,
      spark: (p.recent7d && p.recent7d.length > 0) ? p.recent7d : [],
      color: colors[i],
      rankBadge,
    }
  })
})

/* ═══════════ helpers ═══════════ */
function goTo(p) { if (p) router.push(p) }

function buildLinePoints(arr, w, h, padTop = 6, padBottom = 18, padX = 24) {
  if (!arr || arr.length === 0) return ''
  const max = Math.max(...arr); const min = 0
  const range = max - min || 1
  const usableW = w - padX * 2; const usableH = h - padTop - padBottom
  return arr
    .map((v, i) => {
      const x = padX + (i / (arr.length - 1)) * usableW
      const y = padTop + (1 - (v - min) / range) * usableH
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

function buildSmoothPath(arr, w, h, padTop = 6, padBottom = 18, padX = 24) {
  if (!arr || arr.length === 0) return ''
  const max = Math.max(...arr); const min = 0
  const range = max - min || 1
  const usableW = w - padX * 2; const usableH = h - padTop - padBottom
  const pts = arr.map((v, i) => ({
    x: padX + (i / (arr.length - 1)) * usableW,
    y: padTop + (1 - (v - min) / range) * usableH,
  }))
  if (pts.length < 2) return ''
  let d = `M ${pts[0].x.toFixed(1)} ${pts[0].y.toFixed(1)}`
  for (let i = 1; i < pts.length; i += 1) {
    const cp1x = (pts[i - 1].x + pts[i].x) / 2
    const cp1y = pts[i - 1].y
    const cp2x = (pts[i - 1].x + pts[i].x) / 2
    const cp2y = pts[i].y
    d += ` C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${pts[i].x.toFixed(1)} ${pts[i].y.toFixed(1)}`
  }
  return d
}

function buildAreaPath(arr, w, h, padTop = 6, padBottom = 18, padX = 24) {
  const line = buildSmoothPath(arr, w, h, padTop, padBottom, padX)
  if (!line) return ''
  return `${line} L ${(w - padX).toFixed(1)} ${(h - padBottom).toFixed(1)} L ${padX.toFixed(1)} ${(h - padBottom).toFixed(1)} Z`
}

function sparkPath(arr, w = 60, h = 22) {
  if (!arr || arr.length === 0) return ''
  const min = Math.min(...arr); const max = Math.max(...arr); const range = max - min || 1
  return arr.map((v, i) => {
    const x = (i / (arr.length - 1)) * w
    const y = h - ((v - min) / range) * (h - 4) - 2
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

/* ═══════════ Lavender Pop · 슬라이드/토글 상태 ═══════════ */
const zone1Page = ref(0)
const zone2Page = ref(0)
const zone3Page = ref(0)
const zone4Page = ref(0)
const zone3Granularity = ref('month')
const zone4Granularity = ref('month')
const ZONE_PAGE_COUNT = 3
function shiftZone1(delta) { zone1Page.value = (zone1Page.value + delta + ZONE_PAGE_COUNT) % ZONE_PAGE_COUNT }
function shiftZone2(delta) { zone2Page.value = (zone2Page.value + delta + ZONE_PAGE_COUNT) % ZONE_PAGE_COUNT }
function shiftZone3(delta) { zone3Page.value = (zone3Page.value + delta + ZONE_PAGE_COUNT) % ZONE_PAGE_COUNT }
function shiftZone4(delta) { zone4Page.value = (zone4Page.value + delta + ZONE_PAGE_COUNT) % ZONE_PAGE_COUNT }

/* ═══════════ 대시보드 설정 (localStorage 영속) ═══════════ */
const SETTINGS_KEY = 'callog-dashboard-settings'

const settingsOpen = ref(false)

/* A. 자동 전환 + 간격 */
const autoRotate = ref(false)
const rotateIntervalMs = ref(5000)
const ROTATE_OPTIONS = [
  { ms: 3000,  label: '3초' },
  { ms: 5000,  label: '5초' },
  { ms: 10000, label: '10초' },
  { ms: 15000, label: '15초' },
]

/* B. 실시간 동기화 (SSE 기반, 폴링 없음) */
const realtimeSync = ref(true)

/* G. 섹션 표시/숨김 */
const sectionVisible = reactive({ zone1: true, zone2: true, zone3: true, zone4: true })

/* H. 기본 페이지 + 기본 시간 단위 */
const defaultZone3Page = ref(0)
const defaultZone4Page = ref(0)
const defaultGranularity = ref('month') // zone3·4 공통

/* C. 모션 줄이기 — userSettings.themeUi.reduceMotion 양방향 바인딩 */
const reduceMotion = computed({
  get: () => userSettings.themeUi.reduceMotion,
  set: (v) => userSettings.updateThemeUi({ reduceMotion: Boolean(v) }),
})

/* F. 마지막 동기화 시각 + 상대 시간 */
const lastSyncAt = ref(Date.now())
const nowTick = ref(Date.now())
const relativeSync = computed(() => {
  const diff = Math.max(0, nowTick.value - lastSyncAt.value)
  if (diff < 30_000) return '방금 전'
  if (diff < 60_000) return `${Math.floor(diff / 1000)}초 전`
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}분 전`
  return `${Math.floor(diff / 3_600_000)}시간 전`
})

let __rotateTimer = null
let __outsideHandler = null
let __tickTimer = null
let __refreshing = false

function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    if (!raw) return
    const obj = JSON.parse(raw)
    if (typeof obj.autoRotate === 'boolean') autoRotate.value = obj.autoRotate
    if ([3000, 5000, 10000, 15000].includes(obj.rotateIntervalMs)) rotateIntervalMs.value = obj.rotateIntervalMs
    if (typeof obj.realtimeSync === 'boolean') realtimeSync.value = obj.realtimeSync
    if (obj.sectionVisible && typeof obj.sectionVisible === 'object') {
      ['zone1', 'zone2', 'zone3', 'zone4'].forEach((k) => {
        if (typeof obj.sectionVisible[k] === 'boolean') sectionVisible[k] = obj.sectionVisible[k]
      })
    }
    if ([0, 1, 2].includes(obj.defaultZone3Page)) defaultZone3Page.value = obj.defaultZone3Page
    if ([0, 1, 2].includes(obj.defaultZone4Page)) defaultZone4Page.value = obj.defaultZone4Page
    if (['week', 'month'].includes(obj.defaultGranularity)) defaultGranularity.value = obj.defaultGranularity
  } catch (_) { /* noop */ }
}
function persistSettings() {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify({
      autoRotate: autoRotate.value,
      rotateIntervalMs: rotateIntervalMs.value,
      realtimeSync: realtimeSync.value,
      sectionVisible: { ...sectionVisible },
      defaultZone3Page: defaultZone3Page.value,
      defaultZone4Page: defaultZone4Page.value,
      defaultGranularity: defaultGranularity.value,
    }))
  } catch (_) { /* noop */ }
}
function applyDefaults() {
  zone3Page.value = defaultZone3Page.value
  zone4Page.value = defaultZone4Page.value
  zone3Granularity.value = defaultGranularity.value
  zone4Granularity.value = defaultGranularity.value
}
function startRotate() {
  stopRotate()
  __rotateTimer = setInterval(() => {
    if (sectionVisible.zone1) shiftZone1(1)
    if (sectionVisible.zone2) shiftZone2(1)
    if (sectionVisible.zone3) shiftZone3(1)
    if (sectionVisible.zone4) shiftZone4(1)
  }, rotateIntervalMs.value)
}
function stopRotate() {
  if (__rotateTimer) {
    clearInterval(__rotateTimer)
    __rotateTimer = null
  }
}
async function doRefresh() {
  if (__refreshing) return
  __refreshing = true
  try {
    await Promise.all([
      dashboardStore.loadAll(currentPeriod.value),
      teamTaskStore.fetch(),
    ])
    if (compareMode.value) await dashboardStore.loadCompare(comparePeriod.value)
    lastSyncAt.value = Date.now()
  } finally {
    __refreshing = false
  }
}
function toggleSettings() {
  settingsOpen.value = !settingsOpen.value
}
function closeSettings() {
  settingsOpen.value = false
}

/* B. SSE 채널 watch — calendar.refresh / my-campaigns.refresh */
watch(() => notiStore.lastCalendarRefresh, (v) => {
  if (!v || !realtimeSync.value) return
  doRefresh()
})
watch(() => notiStore.lastMyCampaignsRefresh, (v) => {
  if (!v || !realtimeSync.value) return
  doRefresh()
})

/* 마감 임박 캠페인 status 필터 — 여러 곳에서 사용 (TDZ 회피 차원에서 위에 선언) */
const DEADLINE_EXCLUDED_STATUSES = new Set(['completed', 'archived', 'cancelled', 'canceled', 'done'])
/* 마감 임박 멤버 watch 는 loadDeadlineMembers 정의 후에 등록 (아래쪽 참조) */

function openCampaign(card) {
  if (!card?.clickable || !card.publicId) return
  router.push(`/campaigns/${card.publicId}`)
}

watch(autoRotate, (on) => {
  persistSettings()
  if (on) startRotate()
  else stopRotate()
})
watch(rotateIntervalMs, () => {
  persistSettings()
  if (autoRotate.value) startRotate()
})
watch(realtimeSync, persistSettings)
watch(sectionVisible, persistSettings, { deep: true })
watch(defaultZone3Page, persistSettings)
watch(defaultZone4Page, persistSettings)
watch(defaultGranularity, persistSettings)

/* ═══════════ Zone 1 — 오늘의 작업 보드 ═══════════ */
const TONES = ['ptask-1', 'ptask-2', 'ptask-3']
const AVATAR_CLASSES = ['a-violet', 'a-lavender', 'a-rose', 'a-lime', 'a-cream']

function avatarsForCampaign(c) {
  const name = (c.name ?? '').replace(/\s+/g, '')
  const seed = (c.idx ?? c.id ?? 0) % AVATAR_CLASSES.length
  return [
    { initial: name.slice(0, 1) || '·', cls: AVATAR_CLASSES[seed] },
    { initial: name.slice(1, 2) || '·', cls: AVATAR_CLASSES[(seed + 1) % AVATAR_CLASSES.length] },
    { initial: name.slice(2, 3) || '·', cls: AVATAR_CLASSES[(seed + 2) % AVATAR_CLASSES.length] },
  ]
}
function ptaskFromCampaign(c, i) {
  const id = c.idx ?? c.id
  const rate = typeof teamTaskStore.completionRateByCampaignId?.[String(id)] === 'number'
    ? teamTaskStore.completionRateByCampaignId[String(id)]
    : null
  // Backend 가 CampaignDto.Res.totalTaskCount 채워줌. 캠페인에 task 가 없으면 null/0 — 진행률 표시 생략.
  const total = Number(c.totalTaskCount) || 0
  const done = total > 0 && rate != null ? Math.round((rate / 100) * total) : 0
  const name = c.name ?? '캠페인'
  const half = Math.ceil(name.length / 2)
  const lines = name.length > 7 ? [name.slice(0, half), name.slice(half)] : [name]
  return {
    id: id ?? `pk-${i}`,
    tone: TONES[i % TONES.length],
    lines,
    pill: 'HIGH PRIORITY',
    // task 없으면 "-" 로 표시 (가짜 "X / 12" 회피)
    progress: total > 0 ? `${done} / ${total}` : '-',
    avatars: avatarsForCampaign(c),
  }
}

/* 업무(task) 우선순위 점수 — 사용자 정의 로직
 *  1) 오늘까지 마감(<=오늘 23:59) 가점 1000
 *  2) priority HIGH 가점 300 / CRITICAL 가점 500
 *  3) CRITICAL(긴급) 추가 가점 200, REVIEW(검수중) 가점 150
 *  4) 마감 임박/경과 가산, IN_PROGRESS 약간 가산
 *  5) DONE은 후보에서 제외 (-1)
 */
function priorityScore(t) {
  const status = String(t.status ?? '').toUpperCase()
  if (status === 'DONE') return -1
  const priority = String(t.priority ?? '').toUpperCase()
  const due = t.dueDate ? new Date(t.dueDate) : null
  const now = new Date()
  const todayEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59)
  let score = 0

  if (due && !Number.isNaN(due.getTime()) && due.getTime() <= todayEnd.getTime()) score += 1000

  if (priority === 'CRITICAL') score += 500
  else if (priority === 'HIGH') score += 300
  else if (priority === 'MEDIUM') score += 80

  if (priority === 'CRITICAL') score += 200
  if (status === 'REVIEW') score += 150
  if (status === 'BLOCKED') score += 80
  if (status === 'IN_PROGRESS') score += 30

  if (due && !Number.isNaN(due.getTime())) {
    const daysLeft = Math.floor((due.getTime() - now.getTime()) / 86_400_000)
    if (daysLeft < 0) score += Math.min(120, Math.abs(daysLeft) * 6)
    else if (daysLeft === 0) score += 60
    else if (daysLeft <= 3) score += 25
  }
  return score
}

function dDayLabel(t) {
  if (!t.dueDate) return '미정'
  const due = new Date(t.dueDate)
  if (Number.isNaN(due.getTime())) return '미정'
  const now = new Date()
  const dayMs = 86_400_000
  const startToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const dueDay = new Date(due.getFullYear(), due.getMonth(), due.getDate()).getTime()
  const days = Math.round((dueDay - startToday) / dayMs)
  if (days === 0) return '오늘'
  if (days > 0) return `D-${days}`
  return `D+${-days}`
}

function pillForTask(t) {
  const status = String(t.status ?? '').toUpperCase()
  const priority = String(t.priority ?? '').toUpperCase()
  if (priority === 'CRITICAL') return '긴급'
  if (status === 'REVIEW') return '검수중'
  if (priority === 'HIGH') return '높음'
  if (status === 'BLOCKED') return '차단'
  if (status === 'IN_PROGRESS') return '진행중'
  return '오늘 마감'
}

function publicIdOfCampaignByIdx(campaignIdx) {
  if (campaignIdx == null) return null
  const list = dashboardStore.myCampaigns ?? []
  const hit = list.find((c) => (c.idx ?? c.id) === campaignIdx || c.idx === campaignIdx)
  return hit?.id ?? hit?.publicId ?? null
}

function taskCardFromTask(t, i) {
  const name = t.name ?? '업무'
  const half = Math.ceil(name.length / 2)
  const lines = name.length > 7 ? [name.slice(0, half), name.slice(half)] : [name]
  const assignee = t.assigneeName ?? '미배정'
  const part = t.taskPartName ?? t.milestoneName ?? ''
  const accent = AVATAR_CLASSES[i % AVATAR_CLASSES.length]
  const publicId = publicIdOfCampaignByIdx(t.campaignIdx)
  return {
    id: t.idx ?? `task-${i}`,
    publicId,
    clickable: !!publicId,
    tone: TONES[i % TONES.length],
    pill: pillForTask(t),
    lines,
    progress: dDayLabel(t),
    sub: part,
    avatars: [
      { initial: (assignee || '·').slice(0, 1), cls: accent },
    ],
  }
}

const ZONE1_TODAY = computed(() => {
  const list = teamTaskStore.tasks ?? []
  if (list.length === 0) return []
  const scored = list
    .map((t) => ({ task: t, score: priorityScore(t) }))
    .filter((x) => x.score >= 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 3)
  return scored.map(({ task }, i) => taskCardFromTask(task, i))
})

/* 마감 임박 카드 — 본인 소속 캠페인 중 endDate 가장 빠른 3건 (왼→오) */
/* DEADLINE_EXCLUDED_STATUSES 는 watch 위(TDZ 회피)로 이동했음 */

/* { [publicId]: [{ name, profileImageUrl, ... }, ...] } — 본인 회사 멤버만 */
const deadlineMembersByCampaign = ref({})
let __loadedDeadlineKey = ''

async function loadDeadlineMembers(publicIds) {
  const ids = (publicIds ?? []).filter(Boolean)
  if (ids.length === 0) return
  const key = ids.join(',')
  if (__loadedDeadlineKey === key) return
  __loadedDeadlineKey = key

  const results = await Promise.all(ids.map(async (pid) => {
    try {
      const res = await ListCampaignMembers(pid)
      const meOrgIdx = res?.me?.organizationIdx ?? null
      const members = Array.isArray(res?.members) ? res.members : []
      const sameOrg = meOrgIdx != null
        ? members.filter((m) => m.organizationIdx === meOrgIdx)
        : members
      return [pid, sameOrg]
    } catch (_) {
      return [pid, []]
    }
  }))

  const map = { ...deadlineMembersByCampaign.value }
  results.forEach(([pid, list]) => { map[pid] = list })
  deadlineMembersByCampaign.value = map
}

/* 마감 임박 캠페인 변경 시 본인 회사 멤버 로드 — loadDeadlineMembers 선언 후에 등록 (TDZ 회피) */
watch(
  () => (dashboardStore.myCampaigns ?? [])
    .filter((c) => c.endDate && !DEADLINE_EXCLUDED_STATUSES.has(String(c.status ?? '').toLowerCase()))
    .sort((a, b) => new Date(a.endDate).getTime() - new Date(b.endDate).getTime())
    .slice(0, 3)
    .map((c) => c.id ?? c.publicId)
    .filter(Boolean)
    .join(','),
  (key) => {
    if (!key) return
    __loadedDeadlineKey = ''
    loadDeadlineMembers(key.split(','))
  },
  { immediate: true },
)

function deadlineCardFromCampaign(c, i) {
  const dd = calcDDay(c.endDate)
  let pill
  if (dd === 0) pill = '오늘 마감'
  else if (dd === 1) pill = '내일 마감'
  else if (dd != null && dd > 0) pill = `D-${dd}`
  else pill = '마감 경과'

  const idx = c.idx ?? c.id
  const publicId = c.id ?? c.publicId ?? null  // 라우팅용
  const rate = typeof teamTaskStore.completionRateByCampaignId?.[String(idx)] === 'number'
    ? teamTaskStore.completionRateByCampaignId[String(idx)]
    : null
  const name = c.name ?? '캠페인'
  const half = Math.ceil(name.length / 2)
  const lines = name.length > 7 ? [name.slice(0, half), name.slice(half)] : [name]

  const end = c.endDate ? new Date(c.endDate) : null
  const endLabel = end && !Number.isNaN(end.getTime())
    ? `${end.getMonth() + 1}/${end.getDate()} 마감`
    : ''
  const sub = rate != null ? `${endLabel} · 진행 ${rate}%` : endLabel

  // 본인 회사 참여자 프로필 (비동기 로드 후 채워짐)
  const sameOrgMembers = deadlineMembersByCampaign.value[publicId] ?? []
  const avatars = sameOrgMembers.length > 0
    ? sameOrgMembers.slice(0, 4).map((m, mi) => ({
      initial: (m.name ?? '·').slice(0, 1),
      cls: AVATAR_CLASSES[mi % AVATAR_CLASSES.length],
      imageUrl: m.profileImageUrl || null,
      name: m.name,
    }))
    : avatarsForCampaign(c)

  return {
    id: idx ?? `dl-${i}`,
    publicId,
    clickable: !!publicId,
    tone: TONES[i % TONES.length],
    pill,
    lines,
    progress: rate != null ? `${rate}%` : (endLabel || '·'),
    sub,
    avatars,
  }
}

const ZONE1_DEADLINE = computed(() => {
  const todayStart = new Date()
  todayStart.setHours(0, 0, 0, 0)
  const list = (dashboardStore.myCampaigns ?? [])
    .filter((c) => {
      const st = String(c.status ?? '').toLowerCase()
      if (DEADLINE_EXCLUDED_STATUSES.has(st)) return false
      if (!c.endDate) return false
      const end = new Date(c.endDate)
      if (Number.isNaN(end.getTime())) return false
      // 오늘 자정 이후 마감인 것만 (이미 지난 캠페인 제외)
      return end.getTime() >= todayStart.getTime()
    })
    .sort((a, b) => {
      // endDate 빠른 순 → 왼쪽부터 채움
      const da = new Date(a.endDate).getTime()
      const db = new Date(b.endDate).getTime()
      if (da !== db) return da - db
      // 동률 시 이름 사전순
      return String(a.name ?? '').localeCompare(String(b.name ?? ''), 'ko')
    })
    .slice(0, 3)
  return list.map(deadlineCardFromCampaign)
})

const KPI_CATEGORY_LABELS = {
  IMPRESSION: '노출',
  ENGAGEMENT: '참여',
  CONVERSION: '전환',
  REVENUE: '매출',
  BRAND: '브랜드',
  ESG: 'ESG',
}

function fmtCompactByUnit(value, unit) {
  const n = Number(value) || 0
  if (unit === '원') {
    if (n >= 100_000_000) return '₩' + (n / 100_000_000).toFixed(1) + '억'
    if (n >= 10_000) return '₩' + Math.round(n / 1000) / 10 + '만'
    if (n >= 1000) return '₩' + Math.round(n / 100) / 10 + 'k'
    return '₩' + n.toLocaleString()
  }
  let suffix = unit || ''
  let display
  if (n >= 1_000_000) display = (n / 1_000_000).toFixed(1) + 'M'
  else if (n >= 1000) display = (n / 1000).toFixed(1) + 'k'
  else display = n.toLocaleString()
  return suffix ? `${display}${suffix === '%' ? '' : ''}${suffix}` : display
}

function kpiCardsFromGoals(goals, titlePrefix) {
  if (!goals || goals.length === 0) return []
  // 카테고리 그룹핑: 단위, actualValue 합계, 달성률 평균, monthlyActuals 합
  const buckets = {}
  goals.forEach((g) => {
    const key = g.esgCategory != null ? 'ESG' : g.category
    if (!key) return
    if (!buckets[key]) buckets[key] = { pcts: [], actuals: 0, unit: g.unit || '', monthly: [0, 0, 0] }
    const pct = g.achievementPercent ?? g.percent
    if (typeof pct === 'number') buckets[key].pcts.push(pct)
    const actual = Number(g.actualValue) || 0
    buckets[key].actuals += actual
    if (!buckets[key].unit && g.unit) buckets[key].unit = g.unit
    const monthly = Array.isArray(g.monthlyActuals) ? g.monthlyActuals : []
    for (let i = 0; i < 3; i++) {
      const v = Number(monthly[i]) || 0
      buckets[key].monthly[i] += v
    }
  })

  const rows = Object.entries(buckets).map(([k, b]) => ({
    key: k,
    avg: b.pcts.length ? Math.round(b.pcts.reduce((s, v) => s + v, 0) / b.pcts.length) : 0,
    actual: b.actuals,
    monthly: b.monthly,
    unit: b.unit || (k === 'IMPRESSION' ? '회' : (k === 'REVENUE' ? '원' : (k === 'ENGAGEMENT' || k === 'CONVERSION' ? '%' : '점'))),
  }))

  if (rows.length === 0) return []

  // 전 카테고리 평균 달성률 — 기준선
  const globalAvg = Math.round(rows.reduce((s, r) => s + r.avg, 0) / rows.length)

  // 달성률 내림차순 상위 3개
  const top = rows.sort((a, b) => b.avg - a.avg).slice(0, 3)

  return top.map((r, i) => {
    const delta = r.avg - globalAvg
    const direction = delta > 0 ? 'up' : delta < 0 ? 'down' : 'flat'
    const valueLabel = r.unit === '%'
      ? `${r.avg}%`
      : fmtCompactByUnit(r.actual, r.unit)
    // 스파크라인 — 실제 월별 actual 만 표시. 데이터 없으면 빈 차트 (가짜 우상향 생성 금지)
    const spark = (r.monthly ?? []).filter((v) => Number.isFinite(v))
    return {
      id: `kpi-${r.key}`,
      tone: TONES[i % TONES.length],
      pill: titlePrefix,
      catName: KPI_CATEGORY_LABELS[r.key] ?? r.key,
      lines: [KPI_CATEGORY_LABELS[r.key] ?? r.key],
      valueLabel,
      direction,
      delta,
      avg: r.avg,
      spark,
      sparkOptions: buildSparklineOptions(direction),
      sparkSeries: [{ name: KPI_CATEGORY_LABELS[r.key] ?? r.key, data: spark }],
      kind: 'kpi-summary',
      avatars: [],
    }
  })
}

function buildSparklineOptions(direction) {
  const color = direction === 'up' ? '#4F7A2E' : direction === 'down' ? '#8B2A22' : '#6F5A9B'
  return {
    chart: {
      type: 'area',
      sparkline: { enabled: true },
      animations: { enabled: true, easing: 'easeinout', speed: 600 },
    },
    colors: [color],
    stroke: { curve: 'smooth', width: 2 },
    fill: {
      type: 'gradient',
      gradient: { shadeIntensity: 0.7, opacityFrom: 0.32, opacityTo: 0, stops: [0, 100] },
    },
    markers: { size: 0 },
    tooltip: { enabled: false },
    dataLabels: { enabled: false },
  }
}


const ZONE1_BY_SCOPE = computed(() => {
  if (orgScope.value === 'HQ') return kpiCardsFromGoals(dashboardStore.quarterGoals, '본사 KPI')
  if (orgScope.value === 'AFFILIATE') return kpiCardsFromGoals(dashboardStore.quarterGoals, '본사 KPI')
  if (orgScope.value === 'EXTERNAL_PARTNER') return kpiCardsFromGoals(dashboardStore.quarterGoals, '참여 KPI')
  return kpiCardsFromGoals(dashboardStore.quarterGoals, '내 KPI')
})

const ZONE1_TITLE = computed(() => ['오늘의 업무', '마감 임박', scopeLabelShort.value + ' KPI 요약'][zone1Page.value])
const ZONE1_LEDE = computed(() => [
  '우선순위가 높은 작업 3건',
  'D-day 가장 가까운 캠페인 3건',
  '카테고리 평균 달성률 Top 3',
][zone1Page.value])
const ZONE1_PAGE = computed(() => [ZONE1_TODAY.value, ZONE1_DEADLINE.value, ZONE1_BY_SCOPE.value][zone1Page.value])
const ZONE1_RENDER = computed(() => ZONE1_PAGE.value ?? [])

/* ═══════════ Zone 2 — Top5 협력사 (3 페이지) ═══════════ */
function partnersPage(list, mode) {
  if (!list || list.length === 0) return []
  const colors = ['a-violet', 'a-lime', 'a-lavender', 'a-rose', 'a-cream']
  let sorted = list.slice()
  if (mode === 'active') {
    sorted.sort((a, b) => {
      const av = (a.recent7d ?? []).reduce((s, v) => s + v, 0)
      const bv = (b.recent7d ?? []).reduce((s, v) => s + v, 0)
      return bv - av
    })
  } else {
    sorted.sort((a, b) => (b.averageKpiAchievementPercent ?? b.progress ?? b.score ?? 0)
      - (a.averageKpiAchievementPercent ?? a.progress ?? a.score ?? 0))
  }
  return sorted.slice(0, 5).map((p, i) => {
    const score = p.averageKpiAchievementPercent ?? p.progress ?? p.score ?? 0
    return {
      rank: i + 1,
      name: p.organizationName ?? p.name ?? '제휴사',
      avatar: (p.organizationName ?? p.name ?? '·').slice(0, 2),
      cls: colors[i],
      crown: i === 0,
      pct: Math.max(0, Math.min(100, Math.round(score))),
      badges: [`KPI ${score}%`, p.delta != null && p.delta !== 0 ? `Δ ${p.delta > 0 ? '+' : ''}${p.delta}` : '안정'],
    }
  })
}

const ZONE2_SCORE = computed(() => partnersPage(dashboardStore.partnerProgress, 'score'))
const ZONE2_ACTIVE = computed(() => partnersPage(dashboardStore.partnerProgress, 'active'))
// ⚡ F1: 백엔드가 partner-progress 응답 시점에 본인 조직을 일괄 제외함 → frontend 분기 불필요
const ZONE2_BY_SCOPE = computed(() => partnersPage(dashboardStore.partnerProgress, 'score'))

const ZONE2_TITLE = computed(() => ['Top5 협력사', '활성도 Top5', scopeLabelShort.value + ' 협력사'][zone2Page.value])
const ZONE2_LEDE = computed(() => ['KPI 누적 점수 기준', '최근 7일 활성도', '권한 스코프 기준'][zone2Page.value])
const ZONE2_PAGE = computed(() => [ZONE2_SCORE.value, ZONE2_ACTIVE.value, ZONE2_BY_SCOPE.value][zone2Page.value])
const ZONE2_RENDER = computed(() => ZONE2_PAGE.value ?? [])

/* ═══════════ Zone 3 — KPI 트래커 (3 페이지 + 주간/월간) ═══════════ */
function granularityFactor(g) { return g === 'week' ? 0.25 : 1 }

const ZONE3_BAR_DATA = computed(() => {
  const factor = granularityFactor(zone3Granularity.value)
  const stats = (ROLE_CARD.value?.stats ?? []).slice(0, 6)
  if (stats.length === 0) return []
  return stats.map((st) => {
    const v = Number(st.value) || 0
    const adj = Math.round(v * factor + (1 - factor) * Math.min(100, v * 1.2))
    return {
      lbl: st.short ?? st.label?.[0] ?? '·',
      primary: Math.max(6, Math.min(100, adj)),
      lime: Math.max(6, Math.min(100, Math.round(adj * 0.72))),
    }
  })
})

const ZONE3_DONUT_SERIES = computed(() => {
  const map = dashboardStore.assetCategories ?? {}
  const labels = []
  const series = []
  Object.entries(map).forEach(([k, v]) => {
    const n = Number(v) || 0
    if (n > 0) {
      labels.push(k.toString().toUpperCase())
      series.push(n)
    }
  })
  return { labels, series }
})
const hasZone3DonutData = computed(() => ZONE3_DONUT_SERIES.value.series.length > 0)

const ZONE3_KPI_BY_SCOPE = computed(() => {
  const goals = dashboardStore.quarterGoals ?? []
  const buckets = {}
  goals.forEach((g) => {
    const key = g.esgCategory != null ? 'ESG' : g.category
    if (!key) return
    if (!buckets[key]) buckets[key] = []
    const pct = g.achievementPercent ?? g.percent
    if (typeof pct === 'number') buckets[key].push(pct)
  })
  const arr = ['IMPRESSION', 'ENGAGEMENT', 'CONVERSION', 'REVENUE', 'BRAND', 'ESG'].map((k) => {
    const vals = buckets[k] ?? []
    const avg = vals.length ? Math.round(vals.reduce((s, v) => s + v, 0) / vals.length) : 0
    return { lbl: KPI_CATEGORY_LABELS[k], primary: avg, lime: Math.round(avg * 0.6) }
  })
  return arr
})

const ZONE3_TITLE = computed(() => ['KPI 트래커', '자산 분포', scopeLabelShort.value + ' KPI'][zone3Page.value])
const ZONE3_LEDE = computed(() => ['카테고리 달성률 vs 참여', '카테고리별 자산 LIVE', '권한 스코프 KPI'][zone3Page.value])

const ZONE3_HAS_TOGGLE = computed(() => zone3Page.value !== 1) // 도넛 페이지엔 주간/월간 없음

const apexDonutOptions = computed(() => ({
  chart: { type: 'donut', animations: { enabled: true, easing: 'easeinout', speed: 700 } },
  labels: ZONE3_DONUT_SERIES.value.labels,
  colors: ['#B79BD9', '#A8BD42', '#C58FA3', '#D7B97C', '#8E72BA', '#6F5A9B'],
  stroke: { width: 2, colors: ['#fff'] },
  legend: {
    position: 'bottom',
    horizontalAlign: 'center',
    fontSize: '11px',
    fontWeight: 500,
    labels: { colors: '#6B6582' },
    markers: { width: 9, height: 9, radius: 9 },
    itemMargin: { horizontal: 6, vertical: 2 },
    offsetY: 4,
  },
  plotOptions: {
    pie: {
      donut: {
        size: '68%',
        labels: {
          show: true,
          name: { fontSize: '11px', color: '#9991AE', offsetY: 18 },
          value: { fontSize: '24px', color: '#2A2440', fontWeight: 700, offsetY: -14 },
          total: {
            show: true,
            label: '총 자산',
            color: '#9991AE',
            fontSize: '11px',
            fontWeight: 500,
            formatter: () => ZONE3_DONUT_SERIES.value.series.reduce((s, v) => s + v, 0).toString(),
          },
        },
      },
    },
  },
  dataLabels: { enabled: false },
  tooltip: { theme: 'light' },
}))

/* ═══════════ Zone 4 — 성과 트래커 (3 페이지 + 주간/월간 + ApexCharts 라인) ═══════════ */
function quarterMonthLabelsLocal(periodCode) {
  const m = String(periodCode || '').match(/^(\d{4})-Q([1-4])$/)
  if (!m) {
    const d = new Date()
    const q = Math.ceil((d.getMonth() + 1) / 3)
    const first = (q - 1) * 3 + 1
    return [`${first}월`, `${first + 1}월`, `${first + 2}월`]
  }
  const q = Number(m[2])
  const first = (q - 1) * 3 + 1
  return [`${first}월`, `${first + 1}월`, `${first + 2}월`]
}
function weekLabels() {
  return ['1주', '2주', '3주', '4주', '5주', '6주', '7주', '8주', '9주', '10주', '11주', '12주']
}

const ZONE4_REVENUE_SERIES = computed(() => {
  const goals = dashboardStore.quarterGoals ?? []
  if (goals.length === 0) return { labels: [], data: [], target: [] }
  if (zone4Granularity.value === 'month') {
    const labels = quarterMonthLabelsLocal(goals[0]?.periodCode || currentPeriod.value)
    const data = [0, 0, 0]
    const target = [0, 0, 0]
    for (let i = 0; i < 3; i++) {
      goals.forEach((g) => {
        const a = g.monthlyActuals?.[i]
        const t = g.monthlyTargets?.[i]
        if (a != null) data[i] += Number(a) || 0
        if (t != null) target[i] += Number(t) || 0
      })
    }
    return { labels, data, target }
  }
  const labels = weekLabels()
  const data = labels.map((_, i) => {
    const monthIdx = Math.min(2, Math.floor(i / 4))
    const weekInMonth = (i % 4) + 1
    const factor = weekInMonth / 4
    let sum = 0
    goals.forEach((g) => { sum += (g.monthlyActuals?.[monthIdx] ?? 0) * factor })
    return Math.round(sum)
  })
  const target = labels.map((_, i) => {
    const monthIdx = Math.min(2, Math.floor(i / 4))
    const weekInMonth = (i % 4) + 1
    const factor = weekInMonth / 4
    let sum = 0
    goals.forEach((g) => { sum += (g.monthlyTargets?.[monthIdx] ?? 0) * factor })
    return Math.round(sum)
  })
  return { labels, data, target }
})

/**
 * Z4/P1 — 캠페인 누적 추이.
 * Backend 의 myCampaigns[].createdAt 을 기준으로 분기 내 buckets (월간 3 / 주간 12) 마다
 * 그 시점까지의 누적 캠페인 수를 계산. 가짜 균등 분배 없음.
 *
 * - 분기 코드(예: 2026-Q2) → 분기 시작/끝일 산출
 * - 각 bucket 의 종료 시점에 created_at <= 종료 인 캠페인 수
 * - target: 누적 캠페인 * 1.15 (시각화 비교용 baseline)
 */
const ZONE4_CAMPAIGN_SERIES = computed(() => {
  const list = dashboardStore.myCampaigns ?? []
  const isWeek = zone4Granularity.value === 'week'
  const labels = isWeek ? weekLabels() : quarterMonthLabelsLocal(currentPeriod.value)
  if (list.length === 0) return { labels, data: [], target: [] }

  // 분기 범위 계산 (currentPeriod = "YYYY-QN")
  const m = String(currentPeriod.value || '').match(/^(\d{4})-Q([1-4])$/)
  if (!m) return { labels, data: [], target: [] }
  const year = Number(m[1])
  const quarter = Number(m[2])
  const startMonth = (quarter - 1) * 3 // 0-based month for Date()
  const quarterStart = new Date(year, startMonth, 1)
  const quarterEnd = new Date(year, startMonth + 3, 0, 23, 59, 59, 999) // 분기 마지막 날 23:59

  // 각 bucket 의 종료 시점 (월간: 각 월의 마지막 날 / 주간: 분기 시작 + 7일씩)
  const bucketEnds = labels.map((_, i) => {
    if (isWeek) {
      const end = new Date(quarterStart)
      end.setDate(end.getDate() + 7 * (i + 1) - 1)
      end.setHours(23, 59, 59, 999)
      return Math.min(end.getTime(), quarterEnd.getTime())
    }
    return new Date(year, startMonth + i + 1, 0, 23, 59, 59, 999).getTime()
  })

  // 캠페인 createdAt 추출 (Date 또는 ISO 문자열 모두 처리)
  const createdAts = list
    .map((c) => {
      const v = c.createdAt
      if (!v) return null
      const t = new Date(v).getTime()
      return Number.isFinite(t) ? t : null
    })
    .filter((t) => t != null)
    .sort((a, b) => a - b)

  // 각 bucket 종료까지 created_at <= 종료 인 캠페인 누적 수
  const data = bucketEnds.map((endMs) => createdAts.filter((t) => t <= endMs).length)
  const target = data.map((v) => Math.round(v * 1.15))
  return { labels, data, target }
})

/**
 * Z4/P2 — 스코프 평균 달성률 (%) 시계열.
 * 🐛 버그 픽스 v2: backend 의 monthlyActuals/monthlyTargets 가 현재 진행 중 달은
 *   "분기 누적 actual / 분기 전체 target" 으로 채워서 단위 불일치 → 비정상치 발생.
 * 해결: KPI 별 backend 가 이미 계산한 g.achievementPercent (분기 누적 달성률 %) 평균을
 *      모든 시점에 동일하게 표시. 0~100% clamp.
 */
const ZONE4_SCOPE_SERIES = computed(() => {
  const isWeek = zone4Granularity.value === 'week'
  const goals = dashboardStore.quarterGoals ?? []
  const labels = isWeek ? weekLabels() : quarterMonthLabelsLocal(currentPeriod.value)
  if (goals.length === 0) {
    // 데이터 없으면 빈 series 반환 → 템플릿이 empty state ("데이터가 없습니다") 표시
    return { labels, data: [], target: [] }
  }
  const pcts = goals.map((g) => g.achievementPercent ?? g.percent)
                    .filter((v) => typeof v === 'number')
  if (pcts.length === 0) return { labels, data: [], target: [] }
  const avg = Math.max(0, Math.min(100, Math.round(
      pcts.reduce((s, v) => s + v, 0) / pcts.length)))
  const data = labels.map(() => avg)
  const target = labels.map(() => 100)
  return { labels, data, target }
})

const scopeLabelShort = computed(() => ({
  HQ: '본사',
  AFFILIATE: '우리 조직',
  EXTERNAL_PARTNER: '내 참여',
  STAFF: '내 담당',
}[orgScope.value] ?? '본사'))

const ZONE4_TITLE = computed(() => ['성과 트래커', '캠페인 누적 추이', scopeLabelShort.value + ' 진행률'][zone4Page.value])
const ZONE4_LEDE = computed(() => ['분기 매출 vs 목표 추이', '내 캠페인 누적 진행', '권한 스코프 평균 달성률'][zone4Page.value])
const ZONE4_BOTTOM_TITLE = computed(() => ['매출 추이', '캠페인 누적', '평균 달성률'][zone4Page.value])
const ZONE4_SERIES = computed(() => [ZONE4_REVENUE_SERIES.value, ZONE4_CAMPAIGN_SERIES.value, ZONE4_SCOPE_SERIES.value][zone4Page.value])

const hasZone4Data = computed(() => {
  const arr = ZONE4_SERIES.value?.data ?? []
  return arr.length > 0 && arr.some((v) => Number(v) > 0)
})

/* 반원 게이지: 페이지별 진행률 + 칩 */
const gaugePct = computed(() => {
  if (zone4Page.value === 0) {
    return Math.max(0, Math.min(100, dashboardStore.summary?.progressPct ?? 0))
  }
  if (zone4Page.value === 1) {
    const list = dashboardStore.myCampaigns ?? []
    if (list.length === 0) return 0
    const done = list.filter((c) => ['completed', 'archived', 'done'].includes((c.status ?? '').toLowerCase())).length
    return Math.round((done / list.length) * 100)
  }
  // 🐛 버그 픽스: backend 의 achievementPercent 가 단위 불일치로 비정상 % 들어올 수 있어 0~100% clamp.
  const goals = dashboardStore.quarterGoals ?? []
  if (goals.length === 0) return 0
  const pcts = goals
    .map((g) => g.achievementPercent ?? g.percent)
    .filter((v) => typeof v === 'number')
  if (pcts.length === 0) return 0
  const avg = Math.round(pcts.reduce((s, v) => s + v, 0) / pcts.length)
  return Math.max(0, Math.min(100, avg))
})

const gaugeLabel = computed(() => ['분기 달성률', '캠페인 완료율', scopeLabelShort.value + ' 평균'][zone4Page.value])

const gaugeChips = computed(() => {
  const s = dashboardStore.summary ?? {}
  const list = dashboardStore.myCampaigns ?? []
  if (zone4Page.value === 0) {
    return [
      ['매출 합계', fmtCompact(ZONE4_REVENUE_SERIES.value.data?.reduce((a, b) => a + b, 0) ?? 0)],
      ['캠페인', `${list.length}건`],
      ['자산 LIVE', `${(Object.values(dashboardStore.assetCategories ?? {}).reduce((a, b) => a + Number(b || 0), 0))}`],
    ]
  }
  if (zone4Page.value === 1) {
    const live = list.filter((c) => ['live', 'running', 'active'].includes((c.status ?? '').toLowerCase())).length
    const review = list.filter((c) => ['review', 'in_review'].includes((c.status ?? '').toLowerCase())).length
    const done = list.filter((c) => ['completed', 'archived', 'done'].includes((c.status ?? '').toLowerCase())).length
    return [
      ['LIVE', `${live}건`],
      ['검수', `${review}건`],
      ['완료', `${done}건`],
    ]
  }
  return [
    ['목표 KPI', `${(dashboardStore.quarterGoals ?? []).length}건`],
    ['신규 협력', `${s.newPartnerCount ?? 0}곳`],
    ['RFP', `${s.rfpCount ?? 0}건`],
  ]
})

const gaugeArcLength = computed(() => {
  const C = Math.PI * 80
  return `${(gaugePct.value / 100) * C} ${C}`
})

function fmtCompact(v) {
  const n = Number(v) || 0
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1000) return Math.round(n / 100) / 10 + 'k'
  return Math.round(n).toString()
}

const apexLineOptions = computed(() => ({
  chart: {
    type: 'area',
    toolbar: { show: false },
    sparkline: { enabled: false },
    fontFamily: "'Pretendard Variable', sans-serif",
    animations: { enabled: true, easing: 'easeinout', speed: 800 },
    foreColor: '#9991AE',
  },
  colors: ['#3F3463', '#D8EB75'],
  stroke: { curve: 'smooth', width: [3, 2], dashArray: [0, 4] },
  fill: {
    type: 'gradient',
    gradient: { shadeIntensity: 0.8, opacityFrom: 0.32, opacityTo: 0, stops: [0, 90] },
  },
  markers: {
    size: 4,
    colors: ['#3F3463', '#D8EB75'],
    strokeColors: '#fff',
    strokeWidth: 2,
    hover: { size: 6 },
  },
  grid: {
    borderColor: '#E5DDF0',
    strokeDashArray: 4,
    xaxis: { lines: { show: false } },
    yaxis: { lines: { show: true } },
  },
  xaxis: {
    categories: ZONE4_SERIES.value.labels,
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: { style: { fontSize: '11px', colors: '#9991AE' } },
  },
  yaxis: {
    labels: {
      style: { fontSize: '10.5px', colors: '#9991AE' },
      formatter: (v) => {
        if (zone4Page.value === 0) return fmtCompact(v)
        if (zone4Page.value === 1) return Math.round(Number(v) || 0).toString() + '건'
        return Math.round(Number(v) || 0).toString() + '%'
      },
    },
  },
  legend: { show: false },
  tooltip: {
    theme: 'light',
    x: { show: true },
    y: {
      formatter: (v) => {
        const n = Number(v) || 0
        if (zone4Page.value === 0) return '₩' + n.toLocaleString()
        if (zone4Page.value === 1) return n.toLocaleString() + '건'
        return n.toLocaleString() + '%'
      },
    },
  },
  dataLabels: { enabled: false },
}))

const apexLineSeries = computed(() => [
  { name: '실적', data: ZONE4_SERIES.value.data ?? [] },
  { name: '목표', data: ZONE4_SERIES.value.target ?? [] },
])

/* Zone 3 KPI 트래커 — 캠페인 누적 라인 차트 (월별) */
const zone3LineOptions = computed(() => ({
  chart: {
    type: 'area',
    toolbar: { show: false },
    sparkline: { enabled: false },
    fontFamily: "'Pretendard Variable', sans-serif",
    animations: { enabled: true, easing: 'easeinout', speed: 700 },
    foreColor: '#9991AE',
  },
  colors: ['#6F5A9B', '#D8EB75'],
  stroke: { curve: 'smooth', width: [2.5, 2], dashArray: [0, 3] },
  fill: {
    type: 'gradient',
    gradient: { shadeIntensity: 0.7, opacityFrom: 0.22, opacityTo: 0, stops: [0, 90] },
  },
  markers: { size: 3, colors: ['#6F5A9B', '#D8EB75'], strokeColors: '#fff', strokeWidth: 1.5 },
  grid: { borderColor: '#E5DDF0', strokeDashArray: 4, xaxis: { lines: { show: false } } },
  xaxis: {
    categories: ZONE4_CAMPAIGN_SERIES.value.labels ?? [],
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: { style: { fontSize: '10px', colors: '#9991AE' } },
  },
  yaxis: { show: false },
  legend: { show: false },
  tooltip: { theme: 'light', y: { formatter: (v) => `${v}건` } },
  dataLabels: { enabled: false },
}))
const zone3LineSeries = computed(() => [
  { name: '캠페인 누적', data: ZONE4_CAMPAIGN_SERIES.value.data ?? [] },
  { name: '목표', data: ZONE4_CAMPAIGN_SERIES.value.target ?? [] },
])
const hasZone3LineData = computed(() => (ZONE4_CAMPAIGN_SERIES.value.data ?? []).some((v) => Number(v) > 0))

const zone4UnitPrefix = computed(() => (zone4Page.value === 0 ? '₩' : ''))
const zone4UnitSuffix = computed(() => {
  if (zone4Page.value === 1) return '건'
  if (zone4Page.value === 2) return '%'
  return ''
})

/**
 * 헤드라인 큰 숫자 = "실적" (현재 시점 누적).
 */
const totalRevenueLabel = computed(() => {
  const arr = ZONE4_SERIES.value?.data ?? []
  if (arr.length === 0) return '0'
  if (zone4Page.value === 0) {
    return arr.reduce((s, v) => s + (Number(v) || 0), 0).toLocaleString()
  }
  return Number(arr[arr.length - 1] ?? 0).toLocaleString()
})

/**
 * 🐛 버그 픽스: 오른쪽 작은 % = "목표 대비 얼마나 상승/하락" (이전 시점 대비 X).
 */
const totalRevenueDelta = computed(() => {
  const dataArr = ZONE4_SERIES.value?.data ?? []
  const targetArr = ZONE4_SERIES.value?.target ?? []
  if (dataArr.length === 0 || targetArr.length === 0) return 0
  let actualTotal
  let targetTotal
  if (zone4Page.value === 0) {
    actualTotal = dataArr.reduce((s, v) => s + (Number(v) || 0), 0)
    targetTotal = targetArr.reduce((s, v) => s + (Number(v) || 0), 0)
  } else {
    actualTotal = Number(dataArr[dataArr.length - 1]) || 0
    targetTotal = Number(targetArr[targetArr.length - 1]) || 0
  }
  if (targetTotal === 0) return actualTotal > 0 ? 100 : 0
  return Math.round(((actualTotal - targetTotal) / targetTotal) * 100)
})

/* ═══════════ GSAP fade-up + CountUp 통합 ═══════════ */
const dashRef = ref(null)
const totalNumRef = ref(null)
let __countUpInstance = null
let __keyHandler = null

function enterAnim() {
  if (!dashRef.value || reduceMotion.value) return
  const cards = dashRef.value.querySelectorAll('.card, .carousel, .z4-top, .z4-bottom')
  if (cards.length) {
    gsap.fromTo(
      cards,
      { y: 14 },
      { y: 0, duration: 0.45, stagger: 0.06, ease: 'power3.out', clearProps: 'transform' },
    )
  }
  const bars = dashRef.value.querySelectorAll('.bar')
  if (bars.length) {
    gsap.fromTo(
      bars,
      { scaleY: 0 },
      { scaleY: 1, transformOrigin: 'bottom', duration: 0.6, stagger: 0.04, ease: 'power2.out', delay: 0.15, clearProps: 'transform' },
    )
  }
  const scoreBars = dashRef.value.querySelectorAll('.ts-bar-fill')
  if (scoreBars.length) {
    gsap.fromTo(
      scoreBars,
      { width: 0 },
      { width: (i, el) => (el.dataset.pct ?? 0) + '%', duration: 0.8, stagger: 0.06, ease: 'power2.out', delay: 0.2 },
    )
  }
}

function bindTotalCounter() {
  if (!totalNumRef.value) return
  const num = Number(String(totalRevenueLabel.value).replace(/,/g, '')) || 0
  if (reduceMotion.value) {
    totalNumRef.value.textContent = num.toLocaleString()
    if (__countUpInstance) { __countUpInstance = null }
    return
  }
  if (__countUpInstance) {
    __countUpInstance.update(num)
  } else {
    __countUpInstance = new CountUp(totalNumRef.value, num, {
      duration: 1.4,
      separator: ',',
      useEasing: true,
    })
    if (!__countUpInstance.error) __countUpInstance.start()
  }
}

watch([totalRevenueLabel, zone4Page, zone4Granularity], () => {
  nextTick(bindTotalCounter)
})

onMounted(() => {
  __keyHandler = (e) => {
    const tag = document.activeElement?.tagName?.toLowerCase()
    if (tag === 'input' || tag === 'textarea') return
    if (e.key === 'ArrowLeft') shiftZone4(-1)
    if (e.key === 'ArrowRight') shiftZone4(1)
  }
  window.addEventListener('keydown', __keyHandler)

  loadSettings()
  applyDefaults()
  if (autoRotate.value) startRotate()
  lastSyncAt.value = Date.now()
  __tickTimer = setInterval(() => { nowTick.value = Date.now() }, 30_000)

  __outsideHandler = (e) => {
    if (!settingsOpen.value) return
    const wrap = document.querySelector('.page-menu-wrap')
    if (wrap && !wrap.contains(e.target)) closeSettings()
  }
  document.addEventListener('mousedown', __outsideHandler)

  nextTick(() => {
    enterAnim()
    bindTotalCounter()
    // vue3-apexcharts: navigate 진입 시 일부 컨테이너 width 측정 실패 → resize 트리거로 redraw
    setTimeout(() => {
      try { window.dispatchEvent(new Event('resize')) } catch (_) { /* noop */ }
    }, 80)
  })
})

onBeforeUnmount(() => {
  stopRotate()
  if (__tickTimer) { clearInterval(__tickTimer); __tickTimer = null }
  if (__keyHandler) window.removeEventListener('keydown', __keyHandler)
  if (__outsideHandler) document.removeEventListener('mousedown', __outsideHandler)
})

/* Zone 2 (Top5 협력사) 페이지 변경 시 — 가로 막대만 다시 그림 */
watch(zone2Page, () => {
  if (reduceMotion.value) return
  nextTick(() => {
    if (!dashRef.value) return
    const scoreBars = dashRef.value.querySelectorAll('.ts-bar-fill')
    if (!scoreBars.length) return
    gsap.fromTo(
      scoreBars,
      { width: 0 },
      { width: (i, el) => (el.dataset.pct ?? 0) + '%', duration: 0.7, stagger: 0.05, ease: 'power2.out' },
    )
  })
})

/* Zone 3 (KPI 트래커) 페이지/주간-월간 변경 시 — 수직 막대만 다시 그림 */
watch([zone3Page, zone3Granularity], () => {
  if (reduceMotion.value) return
  nextTick(() => {
    if (!dashRef.value) return
    const bars = dashRef.value.querySelectorAll('.bar')
    if (!bars.length) return
    gsap.fromTo(
      bars,
      { scaleY: 0 },
      { scaleY: 1, transformOrigin: 'bottom', duration: 0.6, stagger: 0.03, ease: 'power2.out', clearProps: 'transform' },
    )
  })
})

/* Zone 1, Zone 4 — ApexCharts/Vue Transition 자체 애니메이션 사용 (별도 GSAP 불필요) */
</script>

<template>
  <div class="page" data-cycle="lavender-pop" ref="dashRef">
    <div class="page-h">
      <div>
        <h1>오늘의 캠페인 데스크</h1>
        <p class="sub">
          {{ orgName }} · {{ roleLabel }} {{ userName }} · {{ orgScopeLabel }}
        </p>
      </div>
      <div class="page-menu-wrap">
        <button class="page-menu" aria-label="대시보드 설정" @click="toggleSettings">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round">
            <circle cx="5" cy="12" r="1" />
            <circle cx="12" cy="12" r="1" />
            <circle cx="19" cy="12" r="1" />
          </svg>
        </button>
        <Transition name="settings-pop">
          <div v-if="settingsOpen" class="page-settings" role="dialog" aria-label="대시보드 설정">
            <div class="page-settings__h">대시보드 설정</div>

            <!-- A. 자동 전환 -->
            <label class="page-settings__row">
              <span class="page-settings__lbl">자동 화면 전환</span>
              <input class="lp-switch" type="checkbox" v-model="autoRotate" />
            </label>
            <div v-if="autoRotate" class="page-settings__sub">
              <span class="page-settings__sub-lbl">간격</span>
              <div class="lp-mini-seg">
                <button
                  v-for="opt in ROTATE_OPTIONS"
                  :key="opt.ms"
                  type="button"
                  :class="{ 'is-on': rotateIntervalMs === opt.ms }"
                  @click="rotateIntervalMs = opt.ms"
                >{{ opt.label }}</button>
              </div>
            </div>

            <!-- B. 실시간 동기화 (SSE) -->
            <label class="page-settings__row">
              <span class="page-settings__lbl">실시간 동기화</span>
              <input class="lp-switch" type="checkbox" v-model="realtimeSync" />
            </label>
            <p class="page-settings__hint">{{ realtimeSync ? '서버에서 변경 알림(SSE)을 받으면 자동 갱신합니다.' : '수동 새로고침만 사용합니다.' }}</p>

            <!-- C. 모션 줄이기 -->
            <label class="page-settings__row">
              <span class="page-settings__lbl">모션 줄이기</span>
              <input class="lp-switch" type="checkbox" v-model="reduceMotion" />
            </label>

            <hr class="page-settings__div" />

            <!-- G. 섹션 표시/숨김 -->
            <div class="page-settings__h sub">표시할 섹션</div>
            <label class="page-settings__row tight">
              <span class="page-settings__lbl">오늘의 작업 보드</span>
              <input class="lp-switch" type="checkbox" v-model="sectionVisible.zone1" />
            </label>
            <label class="page-settings__row tight">
              <span class="page-settings__lbl">Top5 협력사</span>
              <input class="lp-switch" type="checkbox" v-model="sectionVisible.zone2" />
            </label>
            <label class="page-settings__row tight">
              <span class="page-settings__lbl">KPI 트래커</span>
              <input class="lp-switch" type="checkbox" v-model="sectionVisible.zone3" />
            </label>
            <label class="page-settings__row tight">
              <span class="page-settings__lbl">성과 트래커</span>
              <input class="lp-switch" type="checkbox" v-model="sectionVisible.zone4" />
            </label>

            <hr class="page-settings__div" />

            <!-- H. 기본 페이지 -->
            <div class="page-settings__h sub">기본 페이지</div>
            <div class="page-settings__row tight">
              <span class="page-settings__lbl">KPI 트래커</span>
              <select v-model.number="defaultZone3Page" class="lp-select">
                <option :value="0">막대</option>
                <option :value="1">자산 도넛</option>
                <option :value="2">스코프 KPI</option>
              </select>
            </div>
            <div class="page-settings__row tight">
              <span class="page-settings__lbl">성과 트래커</span>
              <select v-model.number="defaultZone4Page" class="lp-select">
                <option :value="0">매출 추이</option>
                <option :value="1">캠페인 누적</option>
                <option :value="2">스코프 평균</option>
              </select>
            </div>
            <div class="page-settings__row tight">
              <span class="page-settings__lbl">기본 시간 단위</span>
              <div class="lp-mini-seg">
                <button type="button" :class="{ 'is-on': defaultGranularity === 'week' }" @click="defaultGranularity = 'week'">주간</button>
                <button type="button" :class="{ 'is-on': defaultGranularity === 'month' }" @click="defaultGranularity = 'month'">월간</button>
              </div>
            </div>

            <hr class="page-settings__div" />

            <!-- F. 수동 새로고침 + 동기화 시각 -->
            <div class="page-settings__sync">
              <div>
                <div class="page-settings__sync-lbl">마지막 동기화</div>
                <div class="page-settings__sync-rel">{{ relativeSync }}</div>
              </div>
              <button type="button" class="page-settings__refresh" :disabled="dashboardStore.loading" @click="doRefresh">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="23 4 23 10 17 10" />
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
                </svg>
                <span>지금 갱신</span>
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <div v-if="hasError" class="err-banner">
      <span>대시보드 데이터를 불러오지 못했습니다.</span>
      <button class="err-retry" @click="retryDashboard">다시 시도</button>
    </div>

    <div class="dash">
      <!-- Zone 1 -->
      <section v-if="sectionVisible.zone1" class="card zone-today" aria-label="오늘의 작업 보드">
        <div class="card-h">
          <div>
            <h2>{{ ZONE1_TITLE }}</h2>
            <p class="lede">{{ ZONE1_LEDE }}</p>
          </div>
          <div class="zone-nav">
            <button class="nav-btn" aria-label="이전" @click="shiftZone1(-1)">‹</button>
            <span class="nav-ind">{{ zone1Page + 1 }} / 3</span>
            <button class="nav-btn" aria-label="다음" @click="shiftZone1(1)">›</button>
          </div>
        </div>
        <Transition name="page-slide" mode="out-in">
          <div
            class="today-grid"
            :class="{ 'today-grid--tall': zone1Page === 1 }"
            :key="zone1Page"
            v-if="ZONE1_RENDER.length"
          >
            <article
              v-for="(t, i) in ZONE1_RENDER"
              :key="t.id ?? i"
              class="ptask"
              :class="[t.tone, { 'ptask--clickable': t.clickable }]"
              :role="t.clickable ? 'button' : null"
              :tabindex="t.clickable ? 0 : null"
              @click="openCampaign(t)"
              @keydown.enter="openCampaign(t)"
            >
              <span class="pill" :class="{
                'pill--urgent': t.pill === '긴급' || t.pill === '오늘 마감',
                'pill--review': t.pill === '검수중',
                'pill--tomorrow': t.pill === '내일 마감',
              }">{{ t.pill }}</span>

              <!-- KPI 요약 카드 (Zone 1 page 3) -->
              <template v-if="t.kind === 'kpi-summary'">
                <h3 class="kpi-title-row">
                  <span class="kpi-cat">{{ t.catName }}</span>
                  <span class="kpi-num" :class="'kpi-num--' + t.direction">{{ t.valueLabel }}</span>
                </h3>
                <p class="kpi-delta" :class="'kpi-delta--' + t.direction">
                  <span v-if="t.direction === 'up'">▲</span>
                  <span v-else-if="t.direction === 'down'">▼</span>
                  <span v-else>—</span>
                  평균 대비 {{ t.delta > 0 ? '+' : '' }}{{ t.delta }}%p
                </p>
                <div class="kpi-spark">
                  <ApexChart
                    type="area"
                    height="100%"
                    :options="t.sparkOptions"
                    :series="t.sparkSeries"
                  />
                </div>
              </template>

              <!-- 일반 작업 카드 -->
              <template v-else>
                <h3>
                  <template v-for="(line, idx) in t.lines" :key="idx">
                    {{ line }}<br v-if="idx < t.lines.length - 1" />
                  </template>
                </h3>
                <p v-if="t.sub" class="ptask-sub">{{ t.sub }}</p>
                <div class="meta">
                  <div class="avs">
                    <span
                      v-for="(av, ai) in t.avatars"
                      :key="ai"
                      class="av"
                      :class="av.cls"
                      :style="av.imageUrl ? { backgroundImage: 'url(' + av.imageUrl + ')', backgroundSize: 'cover', backgroundPosition: 'center', color: 'transparent' } : null"
                      :title="av.name || av.initial"
                    >{{ av.initial }}</span>
                  </div>
                  <span class="count">{{ t.progress }}</span>
                </div>
              </template>
            </article>
          </div>
          <div v-else class="zone-empty" :key="'empty-' + zone1Page">표시할 데이터가 없습니다.</div>
        </Transition>
      </section>

      <!-- Zone 2 + Zone 3 -->
      <div v-if="sectionVisible.zone2 || sectionVisible.zone3" class="zone-bottom">
        <!-- Zone 2 -->
        <section v-if="sectionVisible.zone2" class="card" aria-label="Top5 협력사">
          <div class="card-h">
            <div>
              <h2>{{ ZONE2_TITLE }}</h2>
              <p class="lede">{{ ZONE2_LEDE }}</p>
            </div>
            <div class="zone-nav">
              <button class="nav-btn" aria-label="이전" @click="shiftZone2(-1)">‹</button>
              <span class="nav-ind">{{ zone2Page + 1 }} / 3</span>
              <button class="nav-btn" aria-label="다음" @click="shiftZone2(1)">›</button>
            </div>
          </div>
          <Transition name="page-slide" mode="out-in">
            <div class="ts-list" :key="zone2Page" v-if="ZONE2_RENDER.length">
              <div
                v-for="row in ZONE2_RENDER"
                :key="row.rank"
                class="ts-row"
              >
                <span class="av av-lg" :class="[row.cls, row.crown ? 'crown' : '']">{{ row.avatar }}</span>
                <div class="ts-mid">
                  <div class="name">{{ row.name }}</div>
                  <div class="sub">
                    <span v-for="(b, bi) in row.badges" :key="bi">{{ b }}</span>
                  </div>
                  <div class="ts-bar">
                    <span class="ts-bar-fill" :data-pct="row.pct" :style="{ width: row.pct + '%' }"></span>
                  </div>
                </div>
                <span class="score">{{ row.pct }}%</span>
              </div>
            </div>
            <div v-else class="zone-empty" :key="'empty-z2-' + zone2Page">표시할 협력사가 없습니다.</div>
          </Transition>
        </section>

        <!-- Zone 3 -->
        <section v-if="sectionVisible.zone3" class="card" aria-label="KPI 트래커">
          <div class="card-h">
            <div>
              <h2>{{ ZONE3_TITLE }}</h2>
              <p class="lede">{{ ZONE3_LEDE }}</p>
            </div>
            <div class="z3-controls">
              <div v-if="ZONE3_HAS_TOGGLE" class="gn-seg">
                <button
                  class="gn-btn"
                  :class="{ 'is-on': zone3Granularity === 'week' }"
                  @click="zone3Granularity = 'week'"
                >주간</button>
                <button
                  class="gn-btn"
                  :class="{ 'is-on': zone3Granularity === 'month' }"
                  @click="zone3Granularity = 'month'"
                >월간</button>
              </div>
              <div class="zone-nav">
                <button class="nav-btn" aria-label="이전" @click="shiftZone3(-1)">‹</button>
                <span class="nav-ind">{{ zone3Page + 1 }} / 3</span>
                <button class="nav-btn" aria-label="다음" @click="shiftZone3(1)">›</button>
              </div>
            </div>
          </div>
          <Transition name="page-slide" mode="out-in">
            <div :key="zone3Page" class="z3-body">
              <!-- Page 0: KPI bars — Y축 % 라벨 + 100% clamp -->
              <template v-if="zone3Page === 0">
                <template v-if="ZONE3_BAR_DATA.length">
                  <div class="kpi-legend">
                    <span class="l-primary">달성</span>
                    <span class="l-lime">참여</span>
                  </div>
                  <div class="kpi-bar-chart">
                    <div class="kpi-y-axis">
                      <span>100%</span>
                      <span>75%</span>
                      <span>50%</span>
                      <span>25%</span>
                      <span>0%</span>
                    </div>
                    <div class="bars bars--tall">
                      <div
                        v-for="(b, i) in ZONE3_BAR_DATA"
                        :key="i"
                        class="bar-col"
                      >
                        <div class="bar bar-primary" :style="{ height: Math.min(100, b.primary) + '%' }"></div>
                        <div class="bar bar-lime" :style="{ height: Math.min(100, b.lime) + '%' }"></div>
                        <span class="lbl">{{ b.lbl }}</span>
                      </div>
                    </div>
                  </div>
                </template>
                <div v-else class="zone-empty">KPI 데이터가 없습니다.</div>
              </template>
              <!-- Page 1: donut -->
              <template v-else-if="zone3Page === 1">
                <ApexChart
                  v-if="hasZone3DonutData"
                  type="donut"
                  height="250"
                  :options="apexDonutOptions"
                  :series="ZONE3_DONUT_SERIES.series"
                />
                <div v-else class="zone-empty">자산 데이터가 없습니다.</div>
              </template>
              <!-- Page 2: scope KPI — Y축 % 라벨 + 100% clamp -->
              <template v-else>
                <template v-if="ZONE3_KPI_BY_SCOPE.some(b => b.primary > 0)">
                  <div class="kpi-legend">
                    <span class="l-primary">달성</span>
                    <span class="l-lime">참여</span>
                  </div>
                  <div class="kpi-bar-chart">
                    <div class="kpi-y-axis">
                      <span>100%</span>
                      <span>75%</span>
                      <span>50%</span>
                      <span>25%</span>
                      <span>0%</span>
                    </div>
                    <div class="bars bars--tall">
                      <div
                        v-for="(b, i) in ZONE3_KPI_BY_SCOPE"
                        :key="i"
                        class="bar-col"
                      >
                        <div class="bar bar-primary" :style="{ height: Math.min(100, b.primary) + '%' }"></div>
                        <div class="bar bar-lime" :style="{ height: Math.min(100, b.lime) + '%' }"></div>
                        <span class="lbl">{{ b.lbl }}</span>
                      </div>
                    </div>
                  </div>
                </template>
                <div v-else class="zone-empty">스코프 KPI 데이터가 없습니다.</div>
              </template>
            </div>
          </Transition>
        </section>
      </div>

      <!-- Zone 4: 반원 게이지 + 라인 차트 (성과 트래커) -->
      <aside v-if="sectionVisible.zone4" class="z4-stack zone-carousel" aria-label="성과 트래커">
        <!-- 상단: 라벤더 그라데이션 + 반원 게이지 + 칩 -->
        <div class="z4-top">
          <div class="car-h">
            <div>
              <h2 class="title">{{ ZONE4_TITLE }}</h2>
              <p class="csub">{{ ZONE4_LEDE }}</p>
            </div>
          </div>

          <Transition name="chart-fade" mode="out-in">
            <div class="z4-gauge-block" :key="'g-' + zone4Page + '-' + zone4Granularity">
              <div class="gauge-wrap">
                <svg width="200" height="120" viewBox="0 0 200 120">
                  <path
                    d="M 20 100 A 80 80 0 0 1 180 100"
                    fill="none"
                    stroke="rgba(255,255,255,0.20)"
                    stroke-width="14"
                    stroke-linecap="round"
                  />
                  <path
                    d="M 20 100 A 80 80 0 0 1 180 100"
                    fill="none"
                    stroke="#fff"
                    stroke-width="14"
                    stroke-linecap="round"
                    :stroke-dasharray="gaugeArcLength"
                    style="transition: stroke-dasharray .8s cubic-bezier(.4,0,.2,1);"
                  />
                </svg>
                <div class="gauge-pill">
                  <span class="v">{{ gaugePct }}%</span>
                  <span class="lbl">{{ gaugeLabel }}</span>
                </div>
              </div>

              <div class="chips" v-if="gaugeChips.length">
                <div v-for="(c, i) in gaugeChips" :key="i" class="chip">
                  <span class="l">{{ c[0] }}</span>
                  <span class="v">{{ c[1] }}</span>
                </div>
              </div>
            </div>
          </Transition>
        </div>

        <!-- 하단: 흰 카드 + Sales Target 라인 차트 -->
        <div class="z4-bottom">
          <div class="z4-bottom-h">
            <div>
              <h3 class="z4-bottom-title">{{ ZONE4_BOTTOM_TITLE }}</h3>
              <p class="z4-bottom-sub">{{ zone4Granularity === 'week' ? '이번 분기 · 주간' : '이번 분기 · 월간' }}</p>
            </div>
            <div class="gn-seg">
              <button
                class="gn-btn"
                :class="{ 'is-on': zone4Granularity === 'week' }"
                @click="zone4Granularity = 'week'"
              >주간</button>
              <button
                class="gn-btn"
                :class="{ 'is-on': zone4Granularity === 'month' }"
                @click="zone4Granularity = 'month'"
              >월간</button>
            </div>
          </div>

          <div class="z4-headline">
            <span class="z4-total">
              <span v-if="zone4UnitPrefix" class="z4-unit z4-unit--prefix">{{ zone4UnitPrefix }}</span>
              <span ref="totalNumRef" class="z4-total-num">0</span>
              <span v-if="zone4UnitSuffix" class="z4-unit z4-unit--suffix">{{ zone4UnitSuffix }}</span>
            </span>
            <span class="z4-delta" :class="totalRevenueDelta >= 0 ? 'up' : 'down'" v-if="hasZone4Data">
              <svg v-if="totalRevenueDelta >= 0" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="18 15 12 9 6 15" />
              </svg>
              <svg v-else width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9" />
              </svg>
              <span>{{ totalRevenueDelta >= 0 ? '+' : '' }}{{ totalRevenueDelta }}%</span>
            </span>
          </div>

          <div class="z4-chart-wrap">
            <Transition name="chart-fade" mode="out-in">
              <ApexChart
                v-if="hasZone4Data"
                :key="zone4Page + '-' + zone4Granularity"
                type="area"
                height="100%"
                :options="apexLineOptions"
                :series="apexLineSeries"
              />
              <div v-else class="z4-empty">데이터가 없습니다.</div>
            </Transition>
          </div>
        </div>

        <!-- 페이지 네비게이션 -->
        <div class="z4-pager">
          <button class="z4-nav" aria-label="이전 지표" @click="shiftZone4(-1)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </button>
          <span class="page-dots">
            <span
              v-for="i in ZONE_PAGE_COUNT"
              :key="i"
              class="dot"
              :class="{ 'is-active': zone4Page === i - 1 }"
              @click="zone4Page = i - 1"
            ></span>
          </span>
          <button class="z4-nav" aria-label="다음 지표" @click="shiftZone4(1)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.page[data-cycle="lavender-pop"] {
  /* --lp-* tokens cascade from :root (base.css). Light/dark theme via [data-theme]. */
  --r-md: 14px;
  --r-lg: 18px;
  --r-xl: 24px;
  --r-pill: 999px;

  --shadow-card: 0 1px 2px rgba(63,52,99,.04), 0 6px 18px rgba(63,52,99,.06);
  --shadow-carousel: 0 14px 36px rgba(63,52,99,.18);

  background: var(--lp-bg);
  color: var(--lp-text);
  font-family: 'Pretendard Variable', 'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  font-feature-settings: 'tnum' 1, 'ss01' 1;
  /* DefaultLayout 의 .callog-content padding(24px) 무시하고 viewport 끝까지 꽉 채움 */
  margin: calc(-1 * var(--density-page-padding, 24px));
  width: calc(100% + 2 * var(--density-page-padding, 24px));
  min-height: calc(100% + 2 * var(--density-page-padding, 24px));
  max-width: none;
  padding: 20px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-sizing: border-box;
}

.page-h {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}
.page-h h1 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: -0.02em; line-height: 1.15; color: var(--lp-text); }
.page-h .sub { margin: 6px 0 0; font-size: 13px; font-weight: 500; color: var(--lp-text-muted); }

/* 우상단 ⋯ 메뉴 + 설정 popover */
.page-menu-wrap { position: relative; }
.page-menu {
  width: 38px;
  height: 38px;
  border-radius: 999px;
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  color: var(--lp-primary-deep);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s ease, transform .12s ease, box-shadow .15s ease;
  box-shadow: var(--shadow-card);
}
.page-menu:hover { background: var(--lp-surface-soft); }
.page-menu:active { transform: scale(0.94); }

.page-settings {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 280px;
  background: var(--lp-surface);
  border-radius: var(--r-lg);
  padding: 16px 18px 14px;
  box-shadow: 0 4px 12px rgba(63,52,99,.10), 0 16px 40px rgba(63,52,99,.18);
  z-index: 50;
  border: 1px solid var(--lp-border);
}
.page-settings__h {
  font-size: 12px;
  font-weight: 700;
  color: var(--lp-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 12px;
}
.page-settings__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  cursor: pointer;
}
.page-settings__row.tight { padding: 5px 0; }
.page-settings__lbl { font-size: 13px; font-weight: 600; color: var(--lp-text); }
.page-settings__hint { margin: 2px 0 6px; font-size: 11px; color: var(--lp-text-faint); line-height: 1.45; }

.page-settings__h.sub {
  margin-top: 2px;
  margin-bottom: 8px;
  font-size: 11px;
}
.page-settings__div {
  border: 0;
  border-top: 1px solid var(--lp-border);
  margin: 12px 0;
}

.page-settings__sub {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 6px;
  gap: 8px;
}
.page-settings__sub-lbl { font-size: 11.5px; color: var(--lp-text-muted); }

.lp-mini-seg {
  display: inline-flex;
  background: var(--lp-surface-soft);
  border-radius: 999px;
  padding: 2px;
  gap: 2px;
}
.lp-mini-seg button {
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--lp-text-muted);
  background: transparent;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
  transition: background .15s ease, color .15s ease;
}
.lp-mini-seg button:hover { color: var(--lp-text); }
.lp-mini-seg button.is-on {
  background: var(--lp-surface);
  color: var(--lp-primary-deep);
  box-shadow: 0 1px 3px rgba(63,52,99,.10);
}

.lp-select {
  padding: 5px 8px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--lp-text);
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  border-radius: 8px;
  cursor: pointer;
  outline: none;
}
.lp-select:focus { border-color: var(--lp-primary); }

.page-settings__sync {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding-top: 4px;
}
.page-settings__sync-lbl { font-size: 11px; color: var(--lp-text-faint); margin-bottom: 2px; }
.page-settings__sync-rel { font-size: 13px; font-weight: 700; color: var(--lp-text); }
.page-settings__refresh {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 0;
  background: var(--lp-button-bg);
  color: var(--lp-button-text);
  font-size: 11.5px;
  font-weight: 600;
  border-radius: 999px;
  cursor: pointer;
  transition: background .15s ease, opacity .15s ease;
}
.page-settings__refresh:hover { background: var(--lp-button-bg-hover); }
.page-settings__refresh:disabled { opacity: 0.55; cursor: not-allowed; }

/* iOS-style switch */
.lp-switch {
  appearance: none;
  -webkit-appearance: none;
  width: 38px;
  height: 22px;
  background: var(--lp-border);
  border-radius: 999px;
  position: relative;
  cursor: pointer;
  transition: background .2s ease;
  flex-shrink: 0;
  outline: none;
}
.lp-switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0,0,0,.18);
  transition: left .22s cubic-bezier(.4,0,.2,1);
}
.lp-switch:checked { background: var(--lp-primary); }
.lp-switch:checked::after { left: 18px; }

.settings-pop-enter-active, .settings-pop-leave-active { transition: opacity .2s ease, transform .2s ease; }
.settings-pop-enter-from { opacity: 0; transform: translateY(-6px) scale(0.96); }
.settings-pop-leave-to   { opacity: 0; transform: translateY(-6px) scale(0.96); }

.period-bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.period-tabs { display: inline-flex; background: var(--lp-surface); border: 1px solid var(--lp-border); border-radius: var(--r-pill); padding: 3px; gap: 2px; }
.ptab { padding: 7px 16px; font-size: 12px; font-weight: 600; color: var(--lp-text-muted); border: 0; background: transparent; border-radius: var(--r-pill); cursor: pointer; transition: background .15s, color .15s; }
.ptab:hover { background: var(--lp-surface-soft); color: var(--lp-text); }
.ptab.is-on { background: var(--lp-button-bg); color: var(--lp-button-text); }
.cmp-toggle { display: inline-flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); cursor: pointer; user-select: none; }
.cmp-toggle input { accent-color: var(--lp-primary-deep); }

.err-banner { display: flex; justify-content: space-between; align-items: center; padding: 12px 18px; background: #FFF1D6; border: 1px solid #FFC36B; border-radius: var(--r-md); font-size: 12.5px; color: var(--lp-text); }
.err-retry { border: 0; background: var(--lp-button-bg); color: var(--lp-button-text); padding: 6px 14px; font-size: 11.5px; font-weight: 600; border-radius: var(--r-pill); cursor: pointer; }

.dash {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  /* row 1 = Zone 1 카드 (280) / row 2 = Zone 2-3 카드 (380) — 페이지 전환에도 grid 행 변동 0 */
  /* Zone 1 row + Zone 2/3 row 살짝 키움. Zone 4(span 2) = 320+420+gap = 760 */
  grid-template-rows: 320px 420px;
  gap: 20px;
}
.zone-today    { grid-column: 1; grid-row: 1; }
.zone-bottom   { grid-column: 1; grid-row: 2; display: grid; grid-template-columns: minmax(0,1fr) minmax(0,1.3fr); gap: 20px; }
.zone-carousel { grid-column: 2; grid-row: 1 / span 2; }

.card { background: var(--lp-surface); border-radius: var(--r-xl); padding: 22px 24px; box-shadow: var(--shadow-card); position: relative; overflow: hidden; }
.card-h { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; gap: 12px; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; letter-spacing: -0.01em; color: var(--lp-text); }
.card-h .lede { margin: 4px 0 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); }

.zone-nav { display: inline-flex; align-items: center; gap: 6px; }
.nav-btn { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-primary-deep); cursor: pointer; font-size: 14px; line-height: 1; transition: background .15s, transform .12s; }
.nav-btn:hover { background: var(--lp-surface-soft); }
.nav-btn:active { transform: scale(0.94); }
.nav-ind { font-size: 11px; font-weight: 600; color: var(--lp-text-faint); min-width: 36px; text-align: center; font-variant-numeric: tabular-nums; }

.z3-controls { display: inline-flex; align-items: center; gap: 10px; }
.gn-seg { display: inline-flex; background: var(--lp-surface-soft); border-radius: 999px; padding: 3px; gap: 2px; }
.gn-btn { padding: 4px 10px; font-size: 11px; font-weight: 600; color: var(--lp-text-muted); border: 0; background: transparent; border-radius: 999px; cursor: pointer; transition: background .15s, color .15s; }
.gn-btn:hover { color: var(--lp-text); }
.gn-btn.is-on { background: var(--lp-surface); color: var(--lp-primary-deep); box-shadow: 0 1px 3px rgba(63,52,99,.10); }

/* Page slide transition */
.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(16px); }
.page-slide-leave-to   { opacity: 0; transform: translateX(-16px); }

/* Zone 1 */
.today-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: 320px;   /* Zone 1 row 살짝 키움 */
  gap: 14px;
}
.ptask {
  border-radius: 18px;
  padding: 20px 22px 16px;
  height: 320px;
  min-height: 200px;
  max-height: 210px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
}
.ptask-1 { background: var(--lp-card-lavender-1); }
.ptask-2 { background: var(--lp-lime-soft); }
.ptask-3 { background: var(--lp-card-cream); }

.ptask--clickable {
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease;
}
.ptask--clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(63,52,99,.14);
}
.ptask--clickable:focus-visible {
  outline: 2px solid var(--lp-primary-deep);
  outline-offset: 2px;
}
.ptask .pill { align-self: flex-start; display: inline-flex; align-items: center; gap: 6px; background: rgba(255,255,255,.65); backdrop-filter: blur(2px); padding: 5px 12px 5px 8px; border-radius: var(--r-pill); font-size: 10.5px; font-weight: 700; letter-spacing: 0.02em; color: var(--lp-primary-deep); }
.ptask .pill::before { content: ''; width: 6px; height: 6px; border-radius: 999px; background: var(--lp-primary-strong); display: inline-block; }
.ptask h3 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.025em; line-height: 1.05; color: var(--lp-text); }
.ptask-sub { margin: 0; font-size: 11.5px; font-weight: 500; color: var(--lp-text-muted); line-height: 1.35; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* KPI 요약 카드 (Zone 1 page 3) */
.kpi-title-row {
  margin: 0;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
}
.kpi-cat {
  font-size: 22px;
  color: var(--lp-text);
}
.kpi-num {
  font-size: 22px;
  font-variant-numeric: tabular-nums;
  font-weight: 800;
}
/* 라벤더 팝 톤 — 디자인 적합 */
.kpi-num--up   { color: #4F7A2E; }   /* 라임 톤 → 어두운 올리브-그린 (가독성) */
.kpi-num--down { color: #8B2A22; }   /* 코랄 톤 → 깊은 와인-레드 */
.kpi-num--flat { color: var(--lp-text); }

.kpi-delta {
  margin: -2px 0 0;
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.kpi-delta--up   { color: #5A8C36; }   /* 더 부드러운 그린 */
.kpi-delta--down { color: #A8443B; }   /* 더 부드러운 레드 */
.kpi-delta--flat { color: var(--lp-text-muted); }

/* KPI 요약 카드 — 미니 라인 차트(sparkline) 영역 — 카드 남은 공간 모두 차지 */
.kpi-spark {
  margin-left: -8px;
  margin-right: -8px;
  margin-bottom: -10px;
  margin-top: 4px;
  pointer-events: none;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.kpi-spark > :deep(.apexcharts-canvas),
.kpi-spark > :deep(.vue-apexcharts) {
  width: 100% !important;
  height: 100% !important;
  flex: 1;
}

.ptask .pill--urgent { background: rgba(192, 68, 56, 0.16); color: #8B2A22; }
.ptask .pill--urgent::before { background: #C04438; }
.ptask .pill--review { background: rgba(168, 189, 66, 0.32); color: var(--lp-primary-deep); }
.ptask .pill--review::before { background: #A8BD42; }
.ptask .pill--tomorrow { background: rgba(215, 185, 124, 0.30); color: #6B4F1F; }
.ptask .pill--tomorrow::before { background: #D7B97C; }
.ptask .meta { margin-top: auto; display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.ptask .avs { display: inline-flex; }
.ptask .count { background: rgba(255,255,255,.65); color: var(--lp-primary-deep); padding: 5px 12px; font-size: 12px; font-weight: 700; border-radius: var(--r-pill); font-variant-numeric: tabular-nums; }

.av { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; border-radius: 999px; border: 2px solid #fff; color: #fff; font-size: 11px; font-weight: 700; margin-left: -8px; box-sizing: border-box; }
.avs .av:first-child { margin-left: 0; }
.a-violet   { background: #8E72BA; }
.a-lime     { background: #A8BD42; color: var(--lp-primary-deep); }
.a-lavender { background: #B0A4DA; }
.a-cream    { background: #D7B97C; }
.a-rose     { background: #C58FA3; }

/* Zone 2 — Top5 협력사 + 가로 막대 */
.ts-list { display: flex; flex-direction: column; }
.ts-row { display: grid; grid-template-columns: 38px 1fr auto; align-items: center; gap: 14px; padding: 12px 0; border-top: 1px solid var(--lp-border); }
.ts-row:first-child { border-top: 0; padding-top: 4px; }
.ts-row .av-lg { width: 38px; height: 38px; font-size: 12px; margin: 0; position: relative; }
.ts-row .av-lg.crown::after { content: '★'; position: absolute; top: -4px; right: -4px; width: 18px; height: 18px; border-radius: 999px; background: var(--lp-lime); color: var(--lp-primary-deep); font-size: 11px; font-weight: 800; display: inline-flex; align-items: center; justify-content: center; }
.ts-mid { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.ts-row .name { font-size: 14px; font-weight: 600; color: var(--lp-text); line-height: 1.2; }
.ts-row .sub { display: inline-flex; gap: 10px; font-size: 11px; color: var(--lp-text-faint); }
.ts-bar { width: 100%; height: 6px; background: var(--lp-surface-soft); border-radius: 999px; overflow: hidden; }
.ts-bar-fill { display: block; height: 100%; background: linear-gradient(90deg, var(--lp-primary) 0%, var(--lp-primary-deep) 100%); border-radius: 999px; transition: width .9s cubic-bezier(.4,0,.2,1); }
.ts-row .score { font-size: 22px; font-weight: 700; letter-spacing: -0.02em; color: var(--lp-text); font-variant-numeric: tabular-nums; }

/* Zone 3 — KPI bars + donut */
.kpi-legend { display: inline-flex; gap: 14px; font-size: 11.5px; color: var(--lp-text-muted); margin-bottom: 12px; }
.kpi-legend span { display: inline-flex; align-items: center; gap: 6px; }
.kpi-legend span::before { content: ''; width: 9px; height: 9px; display: inline-block; }
.kpi-legend .l-primary::before { background: var(--lp-primary); }
.kpi-legend .l-lime::before { background: var(--lp-lime); }
.bars { display: grid; grid-template-columns: repeat(6, 1fr); align-items: end; height: 240px; padding: 50px 4px 28px; margin-top: 14px; gap: 14px; position: relative; flex: 1; }
.bars--tall { height: auto; min-height: 240px; flex: 1; padding: 50px 4px 32px; margin-top: 8px; }

/* Y축 % 라벨 + 막대 차트 묶음 (Z3/P0 KPI 트래커, Z3/P2 스코프 KPI) */
.kpi-bar-chart {
  display: flex;
  flex-direction: row;
  align-items: stretch;
  gap: 10px;
  flex: 1;
  min-height: 240px;
}
.kpi-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 40px;
  padding: 50px 0 32px;
  margin-top: 8px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--lp-text-muted);
  text-align: right;
}
.kpi-y-axis span { line-height: 1; }
.kpi-bar-chart .bars--tall { margin-top: 8px; }
/* 점선 gridline 제거 — 빈 카테고리에서 점선만 떠보이는 시각적 노이즈 회피 */

/* Zone 3 body — row 2 (420px) - 카드 헤더(~60) + padding(~44) 빼고 영역 채움 */
.z3-body {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.z3-body > .zone-empty { flex: 1; min-height: 280px; }
.bars::before, .bars::after { content: ''; position: absolute; left: 0; right: 0; border-top: 1px dashed var(--lp-border); pointer-events: none; }
.bars::before { top: 33%; }
.bars::after  { top: 66%; }
.bar-col { display: flex; align-items: end; justify-content: center; gap: 4px; height: 100%; position: relative; }
.bar { width: 18px; border-radius: 999px; transition: height .5s cubic-bezier(.4,0,.2,1); transform-origin: bottom; }
.bar-primary { background: var(--lp-primary); }
.bar-lime    { background: var(--lp-lime); }
.bar-col .lbl { position: absolute; bottom: -22px; left: 50%; transform: translateX(-50%); font-size: 11px; font-weight: 500; color: var(--lp-text-faint); }

/* Zone 4 — Sales Target 라인 차트 (carousel 컨테이너) */
.carousel { background: linear-gradient(165deg, #C5ADE0 0%, #A98DCC 55%, #8E72BA 100%); border-radius: var(--r-xl); padding: 24px 22px; display: flex; flex-direction: column; gap: 14px; color: #fff; box-shadow: var(--shadow-carousel); position: relative; overflow: hidden; min-height: 540px; }
.carousel::before { content: ''; position: absolute; top: -50px; right: -60px; width: 220px; height: 220px; border-radius: 999px; background: rgba(255,255,255,.08); pointer-events: none; }
.car-h { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; position: relative; }
.car-h .title { margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -0.02em; line-height: 1.2; color: #fff; }
.car-h .csub { margin: 4px 0 0; font-size: 11px; color: rgba(255,255,255,.7); }
.menu { width: 28px; height: 28px; border-radius: 999px; background: rgba(255,255,255,.18); border: 0; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.menu:hover { background: rgba(255,255,255,.28); }

.z4-headline { display: flex; align-items: baseline; gap: 10px; padding: 4px 2px 0; }
/* 첫 번째 .z4-total 정의는 사용 안 함 (아래 inline-flex 정의 우선) — ::before 의 고정 ₩ 도 제거 */
.z4-delta { display: inline-flex; align-items: center; gap: 4px; padding: 4px 9px; border-radius: 999px; font-size: 11px; font-weight: 700; background: rgba(255,255,255,.20); }
.z4-delta.up { color: #ECFCCB; }
.z4-delta.down { color: #FED7AA; }

.z4-chart-wrap { background: var(--lp-bg); border-radius: 18px; padding: 8px 6px 0; flex: 1; min-height: 200px; }
.z4-controls { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.gn-seg--inverse { background: rgba(255,255,255,.18); }
.gn-seg--inverse .gn-btn { color: rgba(255,255,255,.78); }
.gn-seg--inverse .gn-btn:hover { color: #fff; }
.gn-seg--inverse .gn-btn.is-on { background: rgba(255,255,255,.92); color: var(--lp-primary-deep); box-shadow: none; }

.car-arrows { display: inline-flex; align-items: center; gap: 8px; }
.car-arrows button { width: 32px; height: 32px; border-radius: 999px; background: rgba(255,255,255,.18); border: 0; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; transition: background .15s; }
.car-arrows button:hover { background: rgba(255,255,255,.28); }
.page-dots { display: inline-flex; gap: 6px; padding: 0 4px; }
.page-dots .dot { width: 6px; height: 6px; border-radius: 999px; background: rgba(255,255,255,.4); cursor: pointer; transition: width .25s ease, background .25s ease; }
.page-dots .dot.is-active { width: 22px; background: var(--lp-lime); }

.chart-fade-enter-active, .chart-fade-leave-active { transition: opacity .3s ease, transform .3s ease; }
.chart-fade-enter-from { opacity: 0; transform: translateY(8px); }
.chart-fade-leave-to   { opacity: 0; transform: translateY(-8px); }

/* Empty state — 데이터 없어도 카드 크기 유지 */
.zone-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  max-height: 300px;
  padding: 24px 12px;
  border-radius: 14px;
  background: var(--lp-surface-soft);
  font-size: 12.5px;
  font-weight: 500;
  color: var(--lp-text-faint);
  letter-spacing: -0.005em;
}
.z3-body .zone-empty { min-height: 280px; flex: 1; }
/* Zone 1 빈 상태 — 카드 row(320px) 와 동일 크기 */
.today-grid > .zone-empty {
  grid-column: 1 / -1;
  height: 320px;
  min-height: 320px;
  max-height: 320px;
}

/* ═══ Zone 4 — Sales Target 스택 (반원 게이지 + 라인 차트) ═══ */
.z4-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  /* row 1(320) + row 2(420) + gap(20) = 760 */
  height: 100%;
  min-height: 0;
}
.z4-top {
  background: var(--lp-hero-gradient);
  border-radius: var(--r-xl);
  padding: 18px 20px 20px;
  color: var(--lp-hero-text);
  box-shadow: var(--shadow-carousel);
  position: relative;
  overflow: hidden;
  /* Zone 1 카드(320)와 동일 → 파란선 위치에 맞춤 */
  height: 320px;
  min-height: 320px;
  max-height: 320px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.z4-top::before {
  content: '';
  position: absolute;
  top: -50px; right: -60px;
  width: 220px; height: 220px;
  border-radius: 999px;
  background: rgba(255,255,255,.08);
  pointer-events: none;
}
.z4-top .car-h { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; position: relative; }
.z4-top .title { margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -0.02em; line-height: 1.2; color: #fff; }
.z4-top .csub { margin: 4px 0 0; font-size: 11px; color: rgba(255,255,255,.78); }

.z4-gauge-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-top: 8px;
  position: relative;
}
.gauge-wrap { position: relative; }
.gauge-pill {
  position: absolute;
  left: 50%;
  top: 76%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  background: rgba(255,255,255,.18);
  backdrop-filter: blur(4px);
  padding: 8px 18px;
  border-radius: var(--r-md);
  min-width: 110px;
  text-align: center;
}
.gauge-pill .v { font-size: 22px; font-weight: 800; color: #fff; font-variant-numeric: tabular-nums; }
.gauge-pill .lbl { font-size: 11px; color: rgba(255,255,255,.82); white-space: nowrap; }

.chips {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  width: 100%;
}
.chip {
  background: rgba(255,255,255,.18);
  border-radius: 14px;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.chip .l { font-size: 9.5px; color: rgba(255,255,255,.66); }
.chip .v { font-size: 14px; font-weight: 700; color: #fff; font-variant-numeric: tabular-nums; }

.z4-bottom {
  background: var(--lp-surface);
  border-radius: var(--r-xl);
  padding: 16px 18px 14px;
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.z4-bottom-h { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.z4-bottom-title { margin: 0; font-size: 17px; font-weight: 700; letter-spacing: -0.01em; color: var(--lp-text); }
.z4-bottom-sub { margin: 3px 0 0; font-size: 11px; color: var(--lp-text-muted); }

.z4-headline { display: flex; align-items: baseline; gap: 10px; }
.z4-total { display: inline-flex; align-items: baseline; gap: 2px; font-size: 30px; font-weight: 800; letter-spacing: -0.02em; color: var(--lp-text); font-variant-numeric: tabular-nums; }
.z4-total-num { display: inline-block; }
.z4-unit { color: var(--lp-primary-strong); font-weight: 700; }
.z4-unit--prefix { font-size: 18px; margin-right: 2px; }
.z4-unit--suffix { font-size: 16px; margin-left: 2px; }
.z4-delta { display: inline-flex; align-items: center; gap: 4px; padding: 3px 9px; border-radius: 999px; font-size: 11px; font-weight: 700; background: var(--lp-surface-soft); }
.z4-delta.up { color: var(--lp-primary-deep); }
.z4-delta.down { color: #C04438; }

.z4-chart-wrap { flex: 1; min-height: 130px; display: flex; flex-direction: column; }
.z4-chart-wrap > :deep(.apexcharts-canvas) { flex: 1; }
.z4-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 130px;
  font-size: 12px;
  color: var(--lp-text-faint);
}

.z4-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 2px 0 0;
}
.z4-nav {
  width: 30px; height: 30px;
  border-radius: 999px;
  background: var(--lp-surface);
  border: 1px solid var(--lp-border);
  color: var(--lp-primary-deep);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background .15s, transform .12s;
}
.z4-nav:hover { background: var(--lp-surface-soft); }
.z4-nav:active { transform: scale(0.92); }
.z4-pager .page-dots .dot { background: var(--lp-border); }
.z4-pager .page-dots .dot.is-active { width: 22px; background: var(--lp-primary); }

@media (max-width: 1100px) {
  .dash { grid-template-columns: minmax(0, 1fr); grid-template-rows: auto auto auto; }
  .zone-today    { grid-column: 1; grid-row: 1; }
  .zone-bottom   { grid-column: 1; grid-row: 2; grid-template-columns: minmax(0,1fr); }
  .zone-carousel { grid-column: 1; grid-row: 3; }
  .today-grid { grid-template-columns: 1fr; }
  .carousel { min-height: 480px; }
}

@media (max-width: 720px) {
  .page[data-cycle="lavender-pop"] { padding: 18px 16px 36px; }
  .page-h h1 { font-size: 22px; }
  .ptask h3 { font-size: 20px; }
}
</style>
