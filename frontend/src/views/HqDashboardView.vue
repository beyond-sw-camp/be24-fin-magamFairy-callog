<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useDashboardStore } from '@/stores/dashboard'

const router = useRouter()
const authStore = useAuthStore()
const dashboardStore = useDashboardStore()

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

const hasError = computed(() =>
  Object.values(dashboardStore.status ?? {}).some((s) => s === 'error'),
)

function parseNumeric(v, fallback = 0) {
  if (typeof v === 'number') return v
  if (typeof v !== 'string') return fallback
  const cleaned = v.replace(/[^0-9.\-]/g, '')
  const n = Number(cleaned)
  return Number.isNaN(n) ? fallback : n
}
function formatKpiValue(v) {
  if (v === null || v === undefined || v === '') return '0'
  const n = Number(v)
  if (Number.isNaN(n)) return String(v)
  return n.toLocaleString()
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
  // mock fallback — 사용자 organization.type 기반
  const t = String(authStore.user?.organization?.type ?? '').toUpperCase()
  if (t === 'HQ' || t === 'AFFILIATE' || t === 'EXTERNAL_PARTNER') return t
  return 'HQ'
})
const orgScopeLabel = computed(() => ({
  HQ: '본사 · 전사',
  AFFILIATE: '계열사 · 자기 조직',
  EXTERNAL_PARTNER: '외부 파트너 · 참여 캠페인',
  STAFF: '실무자 · 본인 캠페인',
}[orgScope.value] ?? '본사 · 전사'))

/* ═══════════ Row 1 — KPI 6-up (모든 value 0 default — 데이터 없으면 0 표시) ═══════════ */
const TODAY_KPIS = [
  { key: 'progress', label: '전사 진행률',   value: 0, unit: '%',  delta: '', deltaPositive: true,  icon: '📈', bg: '#E7E1FF', iconBg: '#9D85FF' },
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
    } else if (
      (orgScope.value === 'AFFILIATE' || orgScope.value === 'EXTERNAL_PARTNER')
      && s?.companyAveragePct != null
    ) {
      // AFFILIATE/EXTERNAL GM: 전사 평균 대비 위치 표시
      const diff = curr - s.companyAveragePct
      delta = `전사 평균 ${s.companyAveragePct}% (${diff >= 0 ? '+' : ''}${diff}%p)`
      deltaPositive = diff >= 0
    } else if (s?.trend != null) {
      delta = `${s.trend >= 0 ? '+' : ''}${s.trend}%p 지난주`
      deltaPositive = (s?.trend ?? 0) >= 0
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
      HQ:               `${currentPeriod.value} · 전사 OrgKpi 평균`,
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

/* ═══════════ Row 3-2 — 자산 카테고리 도넛 (store 우선, mock fallback) ═══════════ */
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

/* ═══════════ Row 4-1 — 캠페인 진행 table (store 우선, mock fallback) ═══════════ */
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
  if (typeof c.progress === 'number') return c.progress
  const s = (c.status ?? '').toLowerCase()
  if (s === 'completed' || s === 'archived') return 100
  if (s === 'review' || s === 'in_review') return 70
  if (s === 'live' || s === 'running' || s === 'active') return 55
  return 20
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
</script>

<template>
  <div class="dash-d">
    <!-- ─── 필터 바 (좌측 상단) ─── -->
    <div class="filter-bar">
      <div class="filter-group" role="tablist" aria-label="기간 선택">
        <button
          v-for="t in PERIOD_TABS"
          :key="t.key"
          type="button"
          role="tab"
          :aria-selected="quarterKey === t.key"
          class="filter-pill"
          :class="{ 'filter-pill--active': quarterKey === t.key }"
          @click="quarterKey = t.key"
        >{{ t.label }}</button>
      </div>
      <button
        type="button"
        class="filter-compare"
        :class="{ 'filter-compare--on': compareMode }"
        :aria-pressed="compareMode"
        @click="compareMode = !compareMode"
      >
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 7H3M3 7l4-4M3 7l4 4M3 17h18M21 17l-4 4M21 17l-4-4"/>
        </svg>
        비교 {{ compareMode ? 'ON' : 'OFF' }}
      </button>
      <span class="filter-meta">{{ currentPeriod }}<template v-if="compareMode"> vs {{ comparePeriod }}</template></span>
    </div>

    <!-- ─── Greet (한 줄, callog-header 보강) ─── -->
    <header class="greet">
      <p class="greet__hello">안녕하세요, <strong>{{ userName }}</strong> · {{ orgName }}</p>
      <span class="greet__role">{{ roleLabel }}</span>
      <span class="greet__scope" :data-scope="orgScope">{{ orgScopeLabel }}</span>
      <button
        v-if="hasError"
        type="button"
        class="greet__retry"
        :disabled="dashboardStore.loading"
        @click="retryDashboard"
      >
        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M3 12a9 9 0 1 0 3-6.7M3 4v5h5"/>
        </svg>
        다시 시도
      </button>
    </header>

    <!-- ═══════════ Row 1 — KPI 6-up ═══════════ -->
    <section class="grid row-1">
      <template v-if="dashboardStore.status.summary === 'loading'">
        <article v-for="i in 6" :key="`sk-${i}`" class="kpi kpi--skeleton">
          <div class="sk-shimmer sk-icon"></div>
          <div class="sk-shimmer sk-value"></div>
          <div class="sk-shimmer sk-label"></div>
        </article>
      </template>
      <template v-else-if="dashboardStore.status.summary === 'error'">
        <article class="kpi kpi--error" style="grid-column: 1 / -1">
          <p class="state-err">데이터를 불러오지 못했습니다. 다시 시도해주세요.</p>
        </article>
      </template>
      <template v-else>
        <article v-for="k in KPI_LIST" :key="k.key" class="kpi fade-in">
          <div class="kpi__top">
            <div class="kpi__icon" :style="{ background: k.iconBg }">{{ k.icon }}</div>
            <span class="kpi__pill" :style="{ color: k.iconBg, background: k.bg }">Today</span>
          </div>
          <div class="kpi__value">{{ formatKpiValue(k.value) }}<small>{{ k.unit }}</small></div>
          <div class="kpi__label">{{ k.label }}</div>
          <div class="kpi__delta" :class="k.deltaPositive ? 'kpi__delta--pos' : 'kpi__delta--neg'">
            <svg v-if="k.deltaPositive" width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 8 L6 4 L10 8"/></svg>
            <svg v-else width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 4 L6 8 L10 4"/></svg>
            {{ k.delta }}
          </div>
        </article>
      </template>
    </section>

    <!-- ═══════════ Row 2 — 권한 + 목표 vs 실적 + 자산 도넛 (3-col) ═══════════ -->
    <section class="grid row-2">
      <!-- 권한 카드 (1/3) -->
      <article class="card role-card">
        <header class="card__head">
          <div class="card__title-wrap">
            <h2 class="card__title">{{ ROLE_CARD.title }}</h2>
            <p class="card__sub">{{ ROLE_CARD.subtitle ?? `${roleLabel} · 오늘` }}</p>
          </div>
        </header>

        <div v-if="dashboardStore.status.quarterGoals === 'loading'" class="card-skeleton">
          <div class="sk-shimmer sk-line sk-line--lg"></div>
          <div class="sk-shimmer sk-line"></div>
          <div class="sk-shimmer sk-line"></div>
          <div class="sk-shimmer sk-line"></div>
        </div>
        <p v-else-if="dashboardStore.status.quarterGoals === 'error'" class="state-err">
          데이터를 불러오지 못했습니다. 다시 시도해주세요.
        </p>
        <template v-else>
          <!-- GM: 캠페인 카드 형태 (chip + label + 컬러 bar + pct + delta) -->
          <template v-if="role === 'GM'">
            <div class="kpi-main fade-in">
              <span class="kpi-main__val">{{ ROLE_CARD.mainPct ?? 0 }}<small>%</small></span>
              <span class="kpi-main__sub">분기 평균</span>
            </div>
            <div class="kpi-bars fade-in">
              <div v-for="s in ROLE_CARD.stats" :key="s.label" class="kpi-bar">
                <span class="kpi-bar__chip" :style="{ background: s.color }">{{ s.short }}</span>
                <div class="kpi-bar__body">
                  <span class="kpi-bar__label">{{ s.label }}</span>
                  <div class="kpi-bar__track">
                    <div class="kpi-bar__fill" :style="{ width: s.value + '%', background: s.color }"></div>
                  </div>
                </div>
                <div class="kpi-bar__right">
                  <span class="kpi-bar__pct">{{ s.value }}%</span>
                  <span class="kpi-bar__delta" :class="s.delta >= 0 ? 'pos' : 'neg'">
                    {{ s.delta >= 0 ? '+' : '' }}{{ s.delta }}
                  </span>
                </div>
              </div>
            </div>
          </template>

          <!-- MGR/USR: 기존 stat list -->
          <div v-else class="role-stats fade-in">
            <div v-for="s in ROLE_CARD.stats" :key="s.label" class="role-stat">
              <span class="role-stat__bar" :style="{ background: s.color }"></span>
              <div class="role-stat__body">
                <p class="role-stat__label">{{ s.label }}</p>
                <p class="role-stat__val">{{ s.value }}<small>{{ s.unit }}</small></p>
              </div>
              <span class="role-stat__delta" :class="s.delta >= 0 ? 'pos' : 'neg'">
                {{ s.delta >= 0 ? '+' : '' }}{{ s.delta }}
              </span>
            </div>
          </div>
        </template>

        <button type="button" class="role-cta" @click="goTo(ROLE_CARD.ctaTo)">
          {{ ROLE_CARD.cta }} →
        </button>
      </article>
      <article class="card">
        <header class="card__head">
          <div class="card__title-wrap">
            <h2 class="card__title">목표 vs 실적</h2>
            <p class="card__sub">2026 상반기 · 6개월</p>
          </div>
          <div class="legend">
            <span class="legend__item"><i class="legend__dot" style="background:#9D85FF"></i>실적</span>
            <span class="legend__item"><i class="legend__dot" style="background:#FFC36B"></i>목표</span>
            <span v-if="compareMode" class="legend__item legend__item--compare"><i class="legend__dot legend__dot--compare"></i>비교 ({{ comparePeriod }})</span>
          </div>
        </header>
        <div v-if="dashboardStore.status.quarterGoals === 'loading'" class="card-skeleton card-skeleton--chart">
          <div class="sk-shimmer sk-line"></div>
          <div class="sk-shimmer sk-block"></div>
        </div>
        <p v-else-if="dashboardStore.status.quarterGoals === 'error'" class="state-err">
          데이터를 불러오지 못했습니다. 다시 시도해주세요.
        </p>
        <template v-else>
        <div class="stat-strip fade-in">
          <div class="stat-mini"><span class="stat-mini__val">{{ formatKpiValue(targetStats.totalA) }}</span><span class="stat-mini__lbl">실적 합계</span></div>
          <div class="stat-mini"><span class="stat-mini__val">{{ formatKpiValue(targetStats.totalT) }}</span><span class="stat-mini__lbl">목표 합계</span></div>
          <div class="stat-mini stat-mini--accent"><span class="stat-mini__val">{{ targetStats.achieveRate }}<small>%</small></span><span class="stat-mini__lbl">달성률</span></div>
          <div class="stat-mini"><span class="stat-mini__val">{{ targetStats.overMonths }}<small>/{{ targetStats.monthCount }}</small></span><span class="stat-mini__lbl">초과 월</span></div>
        </div>
        <svg viewBox="0 0 540 320" class="chart chart--target fade-in" aria-hidden="true">
          <g class="grid-lines">
            <line v-for="(y, i) in [20, 80, 140, 200, 260]" :key="i" :x1="40" :x2="528" :y1="y" :y2="y" />
          </g>
          <g class="axis-text axis-text--lg">
            <text x="6" y="24">{{ fmtYAxis(targetMax) }}</text>
            <text x="6" y="84">{{ fmtYAxis(targetMax * 0.75) }}</text>
            <text x="6" y="144">{{ fmtYAxis(targetMax * 0.5) }}</text>
            <text x="6" y="204">{{ fmtYAxis(targetMax * 0.25) }}</text>
            <text x="6" y="264">0</text>
          </g>
          <template v-for="(m, i) in TARGET_REALITY" :key="i">
            <g v-if="m">
              <!-- actual 막대 (보라) -->
              <rect :x="50 + i * 160" :y="targetBarY(m.actual)" width="50" :height="targetBarH(m.actual)" rx="6" fill="#9D85FF" />
              <!-- target 막대 (노랑) -->
              <rect :x="110 + i * 160" :y="targetBarY(m.target)" width="50" :height="targetBarH(m.target)" rx="6" fill="#FFC36B" />
              <!-- actual 값 (보라막대 위 중앙) -->
              <text
                :x="75 + i * 160"
                :y="targetBarY(m.actual) - 8"
                text-anchor="middle"
                class="axis-x--val axis-x--val-actual"
              >{{ fmtYAxis(m.actual) }}</text>
              <!-- target 값 (노랑막대 위 중앙) -->
              <text
                :x="135 + i * 160"
                :y="targetBarY(m.target) - 8"
                text-anchor="middle"
                class="axis-x--val axis-x--val-target"
              >{{ fmtYAxis(m.target) }}</text>
              <!-- 월 라벨 (그룹 중심) -->
              <text :x="105 + i * 160" y="296" text-anchor="middle" class="axis-x axis-x--lg">{{ m.month }}</text>
            </g>
          </template>
          <!-- 비교 기간 actual overlay (희미한 회색 라인 + 점) -->
          <g v-if="compareMode && compareLineDots.length > 0" class="chart-compare-overlay">
            <polyline
              v-if="compareLineDots.length >= 2"
              :points="compareLinePoints"
              fill="none"
              stroke-width="2"
              stroke-dasharray="4 4"
              stroke-linejoin="round"
              stroke-linecap="round"
            />
            <g v-for="d in compareLineDots" :key="`cmp-${d.x}`">
              <circle :cx="d.x" :cy="d.y" r="4" />
              <text :x="d.x" :y="d.y - 9" text-anchor="middle" class="chart-compare-overlay__val">{{ fmtYAxis(d.value) }}</text>
            </g>
          </g>
        </svg>
        </template>
      </article>

      <!-- 자산 카테고리 도넛 (4/12) — 단순 분포 -->
      <article class="card asset-card">
        <header class="card__head">
          <div class="card__title-wrap">
            <h2 class="card__title">자산 카테고리</h2>
            <p class="card__sub">총 {{ assetTotal }}개 · 5개 분류</p>
          </div>
        </header>
        <div v-if="dashboardStore.status.assetCategories === 'loading'" class="card-skeleton card-skeleton--donut">
          <div class="sk-shimmer sk-circle"></div>
          <div class="sk-shimmer sk-line"></div>
          <div class="sk-shimmer sk-line"></div>
        </div>
        <p v-else-if="dashboardStore.status.assetCategories === 'error'" class="state-err">
          데이터를 불러오지 못했습니다. 다시 시도해주세요.
        </p>
        <template v-else>
        <div class="donut-wrap fade-in">
          <svg viewBox="0 0 140 140" class="donut" aria-hidden="true">
            <circle cx="70" cy="70" r="50" fill="none" stroke="#F1F2F6" stroke-width="20" />
            <circle
              v-for="(s, i) in assetSegments"
              :key="i"
              cx="70" cy="70" r="50"
              fill="none" :stroke="s.color" stroke-width="20"
              :stroke-dasharray="`${s.length} ${s.gap}`"
              :stroke-dashoffset="s.offset"
              transform="rotate(-90 70 70)"
              stroke-linecap="butt"
            />
          </svg>
          <div class="donut__center">
            <span class="donut__value">{{ assetTotal }}</span>
            <span class="donut__label">자산</span>
          </div>
        </div>
        <ul class="asset-legend fade-in">
          <li v-for="s in ASSET_CATS" :key="s.key">
            <span class="asset-legend__dot" :style="{ background: s.color }"></span>
            <span class="asset-legend__label">{{ s.type }}</span>
            <span class="asset-legend__count">{{ s.count }}</span>
            <span class="asset-legend__pct">{{ assetTotal > 0 ? Math.round((s.count / assetTotal) * 100) : 0 }}%</span>
          </li>
        </ul>
        </template>
      </article>
    </section>

    <!-- ═══════════ Row 3 — 캠페인 진행 + 제휴사 ranking (좁게) ═══════════ -->
    <section class="grid row-3">
      <article class="card">
        <header class="card__head">
          <div class="card__title-wrap">
            <h2 class="card__title">캠페인 진행</h2>
            <p class="card__sub">참여 중 {{ MY_CAMPAIGNS.length }}건</p>
          </div>
          <button type="button" class="card__link" @click="goTo('/calendar')">전체</button>
        </header>
        <div class="campaign-filter" role="tablist" aria-label="캠페인 상태 필터">
          <button
            v-for="t in CAMPAIGN_FILTER_TABS"
            :key="t.key"
            type="button"
            role="tab"
            :aria-selected="campaignStatusFilter === t.key"
            class="filter-chip"
            :class="{ 'filter-chip--active': campaignStatusFilter === t.key }"
            @click="campaignStatusFilter = t.key"
          >{{ t.label }}</button>
        </div>
        <div v-if="dashboardStore.status.myCampaigns === 'loading'" class="card-skeleton">
          <div v-for="i in 4" :key="i" class="sk-shimmer sk-row"></div>
        </div>
        <p v-else-if="dashboardStore.status.myCampaigns === 'error'" class="state-err">
          데이터를 불러오지 못했습니다. 다시 시도해주세요.
        </p>
        <p v-else-if="MY_CAMPAIGNS.length === 0" class="state-empty">참여 중인 캠페인이 0건입니다.</p>
        <table v-else class="ctab fade-in">
          <thead>
            <tr>
              <th>캠페인</th>
              <th>진행률</th>
              <th>D-day</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in MY_CAMPAIGNS" :key="c.id" @click="goTo(`/campaigns/${c.id}`)">
              <td>
                <div class="ctab__name">
                  <span class="ctab__avatar" :style="{ background: c.color }">{{ c.owner }}</span>
                  <span>{{ c.name }}</span>
                </div>
              </td>
              <td>
                <div class="ctab__progress">
                  <div class="ctab__bar"><div class="ctab__fill" :style="{ width: c.progress + '%', background: c.color }"></div></div>
                  <span class="ctab__pct">{{ c.progress }}%</span>
                </div>
              </td>
              <td>
                <span class="ctab__dday" :class="c.dDay != null && c.dDay <= 7 ? 'urgent' : ''">{{ fmtDDay(c.dDay) }}</span>
              </td>
              <td>
                <span class="status" :class="statusOf(c.status).cls">{{ statusOf(c.status).label }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </article>

      <article v-if="orgScope === 'HQ' || orgScope === 'AFFILIATE'" class="card">
        <header class="card__head">
          <div class="card__title-wrap">
            <h2 class="card__title">제휴사 TOP 5</h2>
            <p class="card__sub">달성률 · 7일 추이</p>
          </div>
          <button type="button" class="card__link" @click="goTo('/operations')">전체</button>
        </header>
        <div v-if="dashboardStore.status.partnerProgress === 'loading'" class="card-skeleton">
          <div v-for="i in 5" :key="i" class="sk-shimmer sk-row"></div>
        </div>
        <p v-else-if="dashboardStore.status.partnerProgress === 'error'" class="state-err">
          데이터를 불러오지 못했습니다. 다시 시도해주세요.
        </p>
        <p v-else-if="PARTNER_RANK.length === 0" class="state-empty">제휴사 데이터가 0건입니다.</p>
        <table v-else class="ptab fade-in">
          <thead>
            <tr>
              <th>#</th>
              <th>제휴사</th>
              <th>점수</th>
              <th>추이</th>
              <th>변화</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in PARTNER_RANK" :key="p.rank">
              <td><span class="ptab__rank">{{ p.rank }}</span></td>
              <td>
                <span class="ptab__name">
                  <span class="ptab__avatar" :style="{ background: p.color }">{{ p.name.slice(2, 3) }}</span>
                  {{ p.name }}
                </span>
              </td>
              <td><strong class="ptab__score">{{ p.score }}</strong></td>
              <td>
                <svg viewBox="0 0 60 22" class="ptab__spark" aria-hidden="true">
                  <polyline fill="none" :stroke="p.color" stroke-width="1.6"
                    stroke-linejoin="round" stroke-linecap="round"
                    :points="sparkPath(p.spark)" />
                </svg>
              </td>
              <td>
                <span
                  v-if="p.rankBadge"
                  class="ptab__rank-badge"
                  :class="`ptab__rank-badge--${p.rankBadge.type}`"
                  :title="`이전 분기 대비 순위 변화`"
                >{{ p.rankBadge.label }}</span>
                <span v-else class="ptab__delta" :class="p.delta >= 0 ? 'pos' : 'neg'">
                  <svg v-if="p.delta >= 0" width="9" height="9" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 8 L6 4 L9 8"/></svg>
                  <svg v-else width="9" height="9" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 4 L6 8 L9 4"/></svg>
                  {{ Math.abs(p.delta).toFixed(1) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dash-d {
  margin: calc(-1 * var(--density-page-padding, 24px));
  padding: 16px 22px 32px;
  min-height: calc(100% + 2 * var(--density-page-padding, 24px));
  background: linear-gradient(135deg, #e0e7ff 0%, #f3e8ff 50%, #fae8ff 100%);
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-family: 'Pretendard Variable', 'Pretendard', 'Noto Sans KR', sans-serif;
  font-feature-settings: 'tnum' 1;
  color: var(--text-primary);
}
:root[data-theme='dark'] .dash-d {
  background:
    radial-gradient(circle at top right, rgba(168, 85, 247, 0.18), transparent 40%),
    radial-gradient(circle at bottom left, rgba(99, 102, 241, 0.12), transparent 40%),
    linear-gradient(180deg, #10141d 0%, #181024 100%);
}
:root[data-theme='dark'] .dash-d .card,
:root[data-theme='dark'] .dash-d .kpi {
  background: rgba(28, 35, 48, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
:root[data-theme='dark'] .dash-d .card:hover,
:root[data-theme='dark'] .dash-d .kpi:hover {
  background: rgba(35, 42, 55, 0.7);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.3);
}
/* 권한 카드도 다른 카드와 같은 글래스 톤 (다크모드) */
:root[data-theme='dark'] .dash-d .stat-strip {
  background: linear-gradient(180deg, rgba(157, 133, 255, 0.12) 0%, rgba(157, 133, 255, 0) 100%);
  border-color: rgba(157, 133, 255, 0.2);
}
:root[data-theme='dark'] .dash-d .role-stat {
  background: rgba(255, 255, 255, 0.06);
}
:root[data-theme='dark'] .dash-d .kpi-main {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}
:root[data-theme='dark'] .dash-d .kpi-bar__track {
  background: rgba(255, 255, 255, 0.08);
}

/* ─── 다크모드 pos/neg 색상 (더 밝게) ─── */
:root[data-theme='dark'] .dash-d .kpi__delta--pos,
:root[data-theme='dark'] .dash-d .ptab__delta.pos,
:root[data-theme='dark'] .dash-d .stat-mini--accent .stat-mini__val {
  color: #34D399;
}
:root[data-theme='dark'] .dash-d .kpi__delta--neg,
:root[data-theme='dark'] .dash-d .ptab__delta.neg {
  color: #F87171;
}
:root[data-theme='dark'] .dash-d .ptab__delta.pos {
  background: rgba(52, 211, 153, 0.18);
}
:root[data-theme='dark'] .dash-d .ptab__delta.neg {
  background: rgba(248, 113, 113, 0.18);
}
/* 캠페인 status 칩 */
:root[data-theme='dark'] .dash-d .st--live { background: rgba(52, 211, 153, 0.22); color: #34D399; }
:root[data-theme='dark'] .dash-d .st--review { background: rgba(255, 175, 134, 0.22); color: #FCA68C; }
:root[data-theme='dark'] .dash-d .st--draft { background: rgba(255, 255, 255, 0.1); color: rgba(213, 220, 232, 0.72); }
/* D-day urgent */
:root[data-theme='dark'] .dash-d .ctab__dday.urgent { background: rgba(248, 113, 113, 0.22); color: #F87171; }
:root[data-theme='dark'] .dash-d .ctab__dday { background: rgba(255, 255, 255, 0.1); color: rgba(213, 220, 232, 0.72); }

/* ─── Filter bar (좌측 상단) ─── */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 2px;
}
.filter-group {
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
}
.filter-pill {
  border: 0;
  background: transparent;
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  letter-spacing: -0.01em;
  transition: background 0.15s ease, color 0.15s ease, transform 0.15s ease;
}
.filter-pill:hover { color: var(--text-primary); }
.filter-pill--active {
  background: linear-gradient(180deg, #c084fc 0%, #a855f7 100%);
  color: #fff;
  box-shadow: 0 4px 10px rgba(168, 85, 247, 0.28);
}
.filter-pill--active:hover { color: #fff; }

.filter-compare {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 800;
  font-family: inherit;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}
.filter-compare:hover { color: var(--text-primary); border-color: rgba(157, 133, 255, 0.5); }
.filter-compare--on {
  background: rgba(157, 133, 255, 0.16);
  border-color: rgba(157, 133, 255, 0.5);
  color: #6D28D9;
}
.filter-meta {
  margin-left: auto;
  font-size: 10px;
  color: var(--muted-text);
  font-weight: 700;
  letter-spacing: 0.02em;
  font-variant-numeric: tabular-nums;
}

/* ─── Campaign status chip filter (Row 3-1 카드 내부) ─── */
.campaign-filter {
  display: inline-flex;
  gap: 4px;
  margin: -2px 0 6px;
  flex-wrap: wrap;
}
.filter-chip {
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: transparent;
  color: var(--muted-text);
  font-size: 9px;
  font-weight: 800;
  padding: 3px 9px;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  letter-spacing: 0.02em;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}
.filter-chip:hover { color: var(--text-primary); border-color: rgba(157, 133, 255, 0.4); }
.filter-chip--active {
  background: rgba(157, 133, 255, 0.16);
  border-color: rgba(157, 133, 255, 0.5);
  color: #6D28D9;
}

/* ─── 비교 라인 overlay (목표 vs 실적 차트) ─── */
.chart-compare-overlay polyline { stroke: rgba(15, 23, 42, 0.3); }
.chart-compare-overlay circle { fill: rgba(15, 23, 42, 0.3); }
.chart-compare-overlay__val {
  font-size: 9px;
  font-weight: 800;
  fill: rgba(15, 23, 42, 0.55);
  font-variant-numeric: tabular-nums;
}
.legend__item--compare { color: rgba(15, 23, 42, 0.55); }
.legend__dot--compare {
  background: transparent;
  border: 1.5px dashed rgba(15, 23, 42, 0.45);
}

/* ─── 제휴사 rank-change badge ─── */
.ptab__rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  font-size: 10px;
  font-weight: 800;
  padding: 2px 7px;
  border-radius: 999px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}
.ptab__rank-badge--up {
  background: rgba(111, 191, 135, 0.18);
  color: #047857;
}
.ptab__rank-badge--down {
  background: rgba(255, 122, 107, 0.18);
  color: #C04438;
}
.ptab__rank-badge--new {
  background: rgba(157, 133, 255, 0.18);
  color: #6D28D9;
}
.ptab__rank-badge--same {
  background: rgba(15, 23, 42, 0.06);
  color: var(--muted-text);
}

/* Greet */
.greet { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.greet__retry {
  margin-left: auto;
  display: inline-flex; align-items: center; gap: 5px;
  height: 26px; padding: 0 11px;
  border-radius: 999px;
  background: rgba(220, 38, 38, 0.08);
  border: 1px solid rgba(220, 38, 38, 0.32);
  color: #DC2626;
  font-size: 11px; font-weight: 800;
  font-family: inherit;
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}
.greet__retry:hover:not(:disabled) {
  background: rgba(220, 38, 38, 0.16);
  border-color: rgba(220, 38, 38, 0.5);
}
.greet__retry:disabled { opacity: 0.55; cursor: not-allowed; }
:root[data-theme='dark'] .greet__retry {
  background: rgba(248, 113, 113, 0.14);
  border-color: rgba(248, 113, 113, 0.36);
  color: #FCA5A5;
}
:root[data-theme='dark'] .greet__retry:hover:not(:disabled) {
  background: rgba(248, 113, 113, 0.24);
}
.greet__hello { font-size: 12px; color: var(--muted-text); margin: 0; line-height: 1.2; }
.greet__hello strong { color: var(--text-primary); font-weight: 800; }
.greet__role {
  font-size: 9px; font-weight: 800;
  padding: 2px 7px; border-radius: 999px;
  background: rgba(157, 133, 255, 0.14); color: #6D28D9;
  letter-spacing: 0.04em;
}
.greet__mock {
  font-size: 9px; font-weight: 800;
  padding: 2px 7px; border-radius: 999px;
  background: #FEF3C7; color: #B45309;
  letter-spacing: 0.04em;
}
.greet__scope {
  font-size: 9px; font-weight: 800;
  padding: 2px 7px; border-radius: 999px;
  letter-spacing: 0.04em;
}
.greet__scope[data-scope="HQ"]               { background: rgba(157, 133, 255, 0.14); color: #6D28D9; }
.greet__scope[data-scope="AFFILIATE"]        { background: rgba(93, 175, 216, 0.16);  color: #1E6FA0; }
.greet__scope[data-scope="EXTERNAL_PARTNER"] { background: rgba(255, 138, 92, 0.16);  color: #B0431D; }
.greet__scope[data-scope="STAFF"]            { background: rgba(111, 191, 135, 0.16); color: #2F7A48; }
.dark .greet__scope[data-scope="HQ"]               { background: rgba(157, 133, 255, 0.22); color: #C4B0FF; }
.dark .greet__scope[data-scope="AFFILIATE"]        { background: rgba(93, 175, 216, 0.22);  color: #9CD0EE; }
.dark .greet__scope[data-scope="EXTERNAL_PARTNER"] { background: rgba(255, 138, 92, 0.22);  color: #FFB48A; }
.dark .greet__scope[data-scope="STAFF"]            { background: rgba(111, 191, 135, 0.22); color: #A6DEB7; }

/* ───── 로딩 / 에러 / 빈 상태 ───── */
@keyframes dash-shimmer {
  0%   { background-position: -200px 0; }
  100% { background-position: 200px 0; }
}
@keyframes dash-fade-in {
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0);  }
}
.fade-in { animation: dash-fade-in 0.32s cubic-bezier(0.22, 0.61, 0.36, 1) both; }

.sk-shimmer {
  background: linear-gradient(90deg,
    var(--panel-muted) 0%,
    color-mix(in srgb, var(--panel-muted) 60%, transparent) 50%,
    var(--panel-muted) 100%);
  background-size: 400px 100%;
  animation: dash-shimmer 1.4s ease-in-out infinite;
  border-radius: 8px;
}
.sk-icon  { width: 32px; height: 32px; border-radius: 10px; margin-bottom: 10px; }
.sk-value { width: 60%; height: 22px; margin-bottom: 8px; }
.sk-label { width: 80%; height: 12px; }
.sk-line  { width: 100%; height: 14px; margin: 6px 0; }
.sk-line--lg { height: 22px; width: 50%; }
.sk-block { width: 100%; height: 200px; border-radius: 12px; margin-top: 8px; }
.sk-row   { width: 100%; height: 28px; margin: 6px 0; }
.sk-circle{ width: 110px; height: 110px; border-radius: 50%; margin: 6px auto 14px; }

.kpi--skeleton {
  display: flex; flex-direction: column;
  padding: 12px 14px;
}
.kpi--error {
  display: flex; align-items: center; justify-content: center;
  padding: 18px 12px;
}
.card-skeleton {
  display: flex; flex-direction: column; gap: 6px;
  padding: 4px 0 8px;
}
.card-skeleton--chart { padding: 8px 4px 12px; }
.card-skeleton--donut { align-items: center; }

.state-err {
  margin: 14px 4px;
  font-size: 12px; font-weight: 700;
  color: #DC2626;
  text-align: center;
}
.dark .state-err { color: #FCA5A5; }

.state-empty {
  margin: 14px 4px;
  font-size: 12px; font-weight: 600;
  color: var(--muted-text);
  text-align: center;
}

/* ─── 반응형 ─── */
@media (max-width: 1280px) {
  .row-1 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 960px) {
  .row-2 { grid-template-columns: minmax(0, 1fr); }
  .row-3 { grid-template-columns: minmax(0, 1fr); max-width: 100%; }
  .dash-d { padding: 14px 16px 28px; }
}
@media (max-width: 640px) {
  .row-1 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .greet { gap: 6px; }
  .greet__retry { margin-left: 0; }
  .dash-d { padding: 12px 12px 24px; gap: 8px; }
  .kpi__value { font-size: 22px; }
  .stat-mini__val { font-size: 18px; }
  .filter-bar { gap: 6px; }
  .filter-meta { margin-left: 0; flex-basis: 100%; }
  .filter-pill { font-size: 10px; padding: 4px 9px; }
}
@media (max-width: 420px) {
  .row-1 { grid-template-columns: minmax(0, 1fr); }
}

/* ─── 다크모드 — 필터/배지 보강 ─── */
:root[data-theme='dark'] .dash-d .filter-group,
:root[data-theme='dark'] .dash-d .filter-compare {
  background: rgba(28, 35, 48, 0.55);
  border-color: rgba(255, 255, 255, 0.08);
}
:root[data-theme='dark'] .dash-d .filter-pill { color: rgba(213, 220, 232, 0.7); }
:root[data-theme='dark'] .dash-d .filter-pill:hover { color: #f7f9fc; }
:root[data-theme='dark'] .dash-d .filter-pill--active {
  background: linear-gradient(180deg, #a855f7 0%, #7c3aed 100%);
  color: #fff;
  box-shadow: 0 4px 10px rgba(168, 85, 247, 0.4);
}
:root[data-theme='dark'] .dash-d .filter-compare { color: rgba(213, 220, 232, 0.72); }
:root[data-theme='dark'] .dash-d .filter-compare:hover {
  color: #f7f9fc;
  border-color: rgba(168, 85, 247, 0.45);
}
:root[data-theme='dark'] .dash-d .filter-compare--on {
  background: rgba(168, 85, 247, 0.18);
  border-color: rgba(168, 85, 247, 0.5);
  color: #C4B5FD;
}
:root[data-theme='dark'] .dash-d .filter-meta { color: rgba(213, 220, 232, 0.62); }
:root[data-theme='dark'] .dash-d .filter-chip {
  border-color: rgba(255, 255, 255, 0.12);
  color: rgba(213, 220, 232, 0.7);
}
:root[data-theme='dark'] .dash-d .filter-chip:hover {
  color: #f7f9fc;
  border-color: rgba(168, 85, 247, 0.45);
}
:root[data-theme='dark'] .dash-d .filter-chip--active {
  background: rgba(168, 85, 247, 0.2);
  border-color: rgba(168, 85, 247, 0.55);
  color: #C4B5FD;
}
:root[data-theme='dark'] .dash-d .chart-compare-overlay polyline { stroke: rgba(213, 220, 232, 0.45); }
:root[data-theme='dark'] .dash-d .chart-compare-overlay circle { fill: rgba(213, 220, 232, 0.45); }
:root[data-theme='dark'] .dash-d .chart-compare-overlay__val { fill: rgba(213, 220, 232, 0.78); }
:root[data-theme='dark'] .dash-d .legend__item--compare { color: rgba(213, 220, 232, 0.72); }
:root[data-theme='dark'] .dash-d .legend__dot--compare { border-color: rgba(213, 220, 232, 0.55); }
:root[data-theme='dark'] .dash-d .ptab__rank-badge--up {
  background: rgba(52, 211, 153, 0.2);
  color: #34D399;
}
:root[data-theme='dark'] .dash-d .ptab__rank-badge--down {
  background: rgba(248, 113, 113, 0.2);
  color: #F87171;
}
:root[data-theme='dark'] .dash-d .ptab__rank-badge--new {
  background: rgba(168, 85, 247, 0.22);
  color: #C4B5FD;
}
:root[data-theme='dark'] .dash-d .ptab__rank-badge--same {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(213, 220, 232, 0.7);
}

/* ─── 다크모드 차트 일관성 보강 ─── */
:root[data-theme='dark'] .dash-d .chart .grid-lines line { stroke: rgba(255, 255, 255, 0.08); }
:root[data-theme='dark'] .dash-d .chart .axis-text text,
:root[data-theme='dark'] .dash-d .chart .axis-x { fill: rgba(213, 220, 232, 0.72); }
:root[data-theme='dark'] .dash-d .axis-x--val-actual { fill: #C4B0FF; }
:root[data-theme='dark'] .dash-d .axis-x--val-target { fill: #FFD480; }
:root[data-theme='dark'] .dash-d .donut > circle:first-child { stroke: rgba(255, 255, 255, 0.06); }
:root[data-theme='dark'] .dash-d .ctab__bar,
:root[data-theme='dark'] .dash-d .kpi-bar__track { background: rgba(255, 255, 255, 0.08); }
:root[data-theme='dark'] .dash-d .ctab__avatar,
:root[data-theme='dark'] .dash-d .ptab__avatar { color: #fff; }
:root[data-theme='dark'] .dash-d .sk-shimmer {
  background: linear-gradient(90deg,
    rgba(255, 255, 255, 0.06) 0%,
    rgba(255, 255, 255, 0.12) 50%,
    rgba(255, 255, 255, 0.06) 100%);
  background-size: 400px 100%;
}

.grid { display: grid; gap: 10px; align-items: stretch; }
.row-1 { grid-template-columns: repeat(6, minmax(0, 1fr)); }
.row-2 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.row-3 {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  max-width: 1080px;
}

/* ─── KPI (Glass + 컬러 chip) ─── */
.kpi {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  padding: 12px 14px;
  display: flex; flex-direction: column; gap: 4px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.kpi:hover {
  background: rgba(255, 255, 255, 0.85);
  transform: translateY(-3px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}
.kpi__top { display: flex; justify-content: space-between; align-items: center; }
.kpi__icon {
  width: 26px; height: 26px;
  border-radius: 8px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 13px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}
.kpi__pill {
  font-size: 8px; font-weight: 800;
  padding: 2px 6px; border-radius: 999px;
  background: rgba(255,255,255,0.7);
  color: var(--muted-text);
  letter-spacing: 0.04em;
}
.kpi__value {
  font-size: 22px; font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.025em;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.kpi__value small { font-size: 11px; font-weight: 700; color: var(--muted-text); margin-left: 1px; }
.kpi__label { font-size: 10px; font-weight: 700; color: var(--text-secondary); }
.kpi__delta {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 9px; font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.kpi__delta--pos { color: #047857; }
.kpi__delta--neg { color: #C04438; }

/* ─── 카드 공통 (Glass) ─── */
.card {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  padding: 14px 16px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.card:hover {
  background: rgba(255, 255, 255, 0.82);
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.06);
}
.card__head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; gap: 8px; }
.card__title-wrap { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.card__title { font-size: 13px; font-weight: 800; color: var(--text-primary); margin: 0; letter-spacing: -0.01em; }
.card__sub { font-size: 10px; color: var(--muted-text); margin: 0; }
.card__link {
  background: transparent; border: 0;
  color: #6D28D9; font-size: 10px; font-weight: 700;
  cursor: pointer; font-family: inherit;
}

/* legend */
.legend { display: inline-flex; gap: 10px; flex-wrap: wrap; }
.legend__item {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 10px; color: var(--muted-text); font-weight: 700;
}
.legend__dot { width: 8px; height: 8px; border-radius: 50%; }

/* stat-strip */
.stat-strip {
  display: flex; gap: 10px;
  margin: 2px 0 8px; padding: 7px 10px;
  background: linear-gradient(180deg, rgba(157, 133, 255, 0.06) 0%, rgba(157, 133, 255, 0) 100%);
  border: 1px solid rgba(157, 133, 255, 0.14);
  border-radius: 8px;
  flex-wrap: wrap;
}
.stat-mini { flex: 1; display: flex; flex-direction: column; gap: 1px; min-width: 50px; }
.stat-mini__val {
  font-size: 14px; font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  line-height: 1.1;
}
.stat-mini__val small { font-size: 9px; font-weight: 700; color: var(--muted-text); margin-left: 1px; }
.stat-mini__lbl { font-size: 9px; color: var(--muted-text); font-weight: 600; }
.stat-mini--accent .stat-mini__val { color: #6D28D9; }

/* charts */
.chart { width: 100%; height: 160px; }
.chart--target { height: 280px; }   /* 목표 vs 실적 — 영역 키움 */
.grid-lines line { stroke: rgba(15, 23, 42, 0.06); stroke-width: 1; stroke-dasharray: 2 4; }
.axis-text text,
.chart text { font-size: 8px; font-weight: 600; fill: var(--muted-text); font-variant-numeric: tabular-nums; }
.axis-x { font-size: 8px; fill: var(--muted-text); }

/* 목표 vs 실적 — 큰 폰트 */
.axis-text--lg text { font-size: 12px; font-weight: 700; fill: var(--text-secondary); }
.axis-x--lg { font-size: 13px; font-weight: 800; fill: var(--text-primary); }
.axis-x--val { font-size: 11px; font-weight: 800; font-variant-numeric: tabular-nums; }
/* 라이트 모드: 보라/노랑 막대 위 — 진한 톤 */
.axis-x--val-actual { fill: #6D28D9; }
.axis-x--val-target { fill: #B45309; }
/* 다크 모드: 더 밝게 — 막대 위에서도 잘 보이게 */
:root[data-theme='dark'] .dash-d .axis-x--val-actual { fill: #C4B5FD; }
:root[data-theme='dark'] .dash-d .axis-x--val-target { fill: #FCD34D; }
:root[data-theme='dark'] .dash-d .axis-x--lg { fill: #f7f9fc; }
:root[data-theme='dark'] .dash-d .axis-text--lg text { fill: rgba(213, 220, 232, 0.85); }

/* 다크모드 chart 텍스트 — 흰색 톤 */
:root[data-theme='dark'] .dash-d .axis-text text,
:root[data-theme='dark'] .dash-d .chart text,
:root[data-theme='dark'] .dash-d .axis-x { fill: rgba(213, 220, 232, 0.78); }
:root[data-theme='dark'] .dash-d .grid-lines line { stroke: rgba(255, 255, 255, 0.08); }

/* ─── 권한 카드 (글래스 흰 카드 · 캠페인 행 형태) ─── */
.role-card { display: flex; flex-direction: column; gap: 10px; }
/* .card 글래스 스타일 그대로 사용 */

.role-stats { display: flex; flex-direction: column; gap: 5px; }
.role-stat {
  display: grid;
  grid-template-columns: 3px 1fr auto;
  gap: 8px;
  padding: 6px 8px;
  background: rgba(15, 23, 42, 0.04);
  border-radius: 8px;
  align-items: center;
}
.role-stat__bar { width: 3px; height: 100%; min-height: 24px; border-radius: 2px; }
.role-stat__body { min-width: 0; }
.role-stat__label { font-size: 9px; color: var(--muted-text); font-weight: 700; margin: 0; }
.role-stat__val { font-size: 15px; font-weight: 800; color: var(--text-primary); font-variant-numeric: tabular-nums; margin: 1px 0 0; }
.role-stat__val small { font-size: 9px; font-weight: 700; color: var(--muted-text); margin-left: 1px; }
.role-stat__delta {
  font-size: 9px; font-weight: 800;
  padding: 1px 6px; border-radius: 999px;
  font-variant-numeric: tabular-nums;
}
.role-stat__delta.pos { background: rgba(111, 191, 135, 0.15); color: #047857; }
.role-stat__delta.neg { background: rgba(255, 122, 107, 0.15); color: #C04438; }
.role-cta {
  margin-top: 2px;
  padding: 9px 14px;
  border: 0; border-radius: 10px;
  background: linear-gradient(180deg, #c084fc 0%, #a855f7 100%);
  color: #fff;
  font-size: 11px; font-weight: 800;
  cursor: pointer; font-family: inherit;
  transition: opacity 0.15s ease, transform 0.15s ease;
  width: 100%;
  box-shadow: 0 4px 10px rgba(168, 85, 247, 0.25);
}
.role-cta:hover { opacity: 0.95; transform: translateY(-1px); }

/* ─── GM 분기 KPI 달성률 (캠페인 행 형태) ─── */
.kpi-main {
  display: flex; align-items: baseline; gap: 8px;
  padding: 4px 4px 8px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  margin-bottom: 4px;
}
.kpi-main__val {
  font-size: 30px; font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kpi-main__val small {
  font-size: 14px; font-weight: 700;
  color: var(--muted-text);
  margin-left: 1px;
}
.kpi-main__sub {
  font-size: 10px; font-weight: 600;
  color: var(--muted-text);
}

.kpi-bars { display: flex; flex-direction: column; gap: 8px; }
.kpi-bar {
  display: grid;
  grid-template-columns: 32px 1fr 50px;
  gap: 10px;
  align-items: center;
}
.kpi-bar__chip {
  width: 32px; height: 32px;
  border-radius: 9px;
  display: inline-flex; align-items: center; justify-content: center;
  color: white; font-size: 12px; font-weight: 800;
  letter-spacing: 0.04em;
  flex-shrink: 0;
}
.kpi-bar__body {
  min-width: 0;
  display: flex; flex-direction: column; gap: 4px;
}
.kpi-bar__label {
  font-size: 12px; font-weight: 700;
  color: var(--text-primary);
}
.kpi-bar__track {
  height: 4px;
  background: rgba(15, 23, 42, 0.06);
  border-radius: 999px;
  overflow: hidden;
}
.kpi-bar__fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.kpi-bar__right {
  display: flex; flex-direction: column;
  align-items: flex-end; gap: 2px;
  min-width: 0;
}
.kpi-bar__pct {
  font-size: 13px; font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kpi-bar__delta {
  font-size: 9px; font-weight: 800;
  padding: 1px 6px; border-radius: 999px;
  font-variant-numeric: tabular-nums;
}
.kpi-bar__delta.pos { background: rgba(111, 191, 135, 0.15); color: #047857; }
.kpi-bar__delta.neg { background: rgba(255, 122, 107, 0.15); color: #C04438; }

/* ─── 자산 도넛 ─── */
.asset-card { display: flex; flex-direction: column; gap: 6px; }
.donut-wrap { position: relative; width: 100%; max-width: 110px; margin: 2px auto 6px; }
.donut { width: 100%; height: auto; aspect-ratio: 1 / 1; display: block; }
.donut__center {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  display: flex; flex-direction: column; align-items: center;
}
.donut__value { font-size: 20px; font-weight: 800; color: var(--text-primary); font-variant-numeric: tabular-nums; line-height: 1; }
.donut__label { font-size: 9px; color: var(--muted-text); font-weight: 600; margin-top: 1px; }
.asset-legend { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 3px; }
.asset-legend li { display: grid; grid-template-columns: 8px 1fr auto auto; align-items: center; gap: 6px; }
.asset-legend__dot { width: 8px; height: 8px; border-radius: 50%; }
.asset-legend__label { font-size: 10px; color: var(--text-secondary); font-weight: 600; }
.asset-legend__count { font-size: 11px; font-weight: 800; color: var(--text-primary); font-variant-numeric: tabular-nums; }
.asset-legend__pct { font-size: 9px; color: var(--muted-text); font-weight: 700; font-variant-numeric: tabular-nums; min-width: 24px; text-align: right; }

/* ─── 캠페인 table ─── */
.ctab, .ptab {
  width: 100%; border-collapse: collapse;
  font-variant-numeric: tabular-nums;
}
.ctab th, .ptab th {
  font-size: 9px; font-weight: 700;
  color: var(--muted-text); text-align: left;
  padding: 4px 6px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}
.ctab td, .ptab td {
  font-size: 11px;
  padding: 6px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.04);
}
.ctab tbody tr { cursor: pointer; transition: background 0.15s ease; }
.ctab tbody tr:hover { background: rgba(157, 133, 255, 0.06); }
.ctab__name { display: flex; align-items: center; gap: 6px; font-weight: 700; color: var(--text-primary); }
.ctab__avatar {
  width: 20px; height: 20px;
  border-radius: 6px;
  display: inline-flex; align-items: center; justify-content: center;
  color: white; font-size: 8px; font-weight: 800;
  letter-spacing: 0.04em;
  flex-shrink: 0;
}
.ctab__progress { display: flex; align-items: center; gap: 6px; }
.ctab__bar { flex: 1; min-width: 40px; height: 4px; background: rgba(15, 23, 42, 0.06); border-radius: 999px; overflow: hidden; }
.ctab__fill { height: 100%; border-radius: 999px; transition: width 0.5s ease; }
.ctab__pct { font-size: 10px; font-weight: 800; color: var(--text-primary); }
.ctab__dday {
  font-size: 9px; font-weight: 800;
  padding: 1px 6px; border-radius: 999px;
  background: rgba(15, 23, 42, 0.06); color: var(--muted-text);
}
.ctab__dday.urgent { background: rgba(255, 122, 107, 0.18); color: #C04438; }
.status { font-size: 8px; font-weight: 800; padding: 1px 6px; border-radius: 999px; }
.st--live { background: rgba(111, 191, 135, 0.18); color: #047857; }
.st--review { background: rgba(255, 138, 92, 0.18); color: #C45524; }
.st--draft { background: rgba(15, 23, 42, 0.06); color: var(--muted-text); }

/* ─── 제휴사 ranking table ─── */
.ptab__rank { font-size: 11px; font-weight: 800; color: var(--muted-text); }
.ptab__name { display: inline-flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 700; color: var(--text-primary); }
.ptab__avatar {
  width: 20px; height: 20px;
  border-radius: 50%;
  color: white; font-size: 10px; font-weight: 800;
  display: inline-flex; align-items: center; justify-content: center;
  border: 2px solid white;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.06);
  flex-shrink: 0;
}
.ptab__score { font-size: 13px; font-weight: 800; color: var(--text-primary); }
.ptab__spark { width: 50px; height: 18px; }
.ptab__delta {
  display: inline-flex; align-items: center; gap: 2px;
  font-size: 10px; font-weight: 800;
  padding: 1px 6px; border-radius: 999px;
  font-variant-numeric: tabular-nums;
}
.ptab__delta.pos { background: rgba(111, 191, 135, 0.15); color: #047857; }
.ptab__delta.neg { background: rgba(255, 122, 107, 0.15); color: #C04438; }

/* ─── 반응형 ─── */
@media (max-width: 1280px) {
  .row-1 { grid-template-columns: repeat(3, 1fr); }
  .row-2 { grid-template-columns: 1fr; }
  .row-3 { grid-template-columns: 1fr; max-width: 100%; }
}
@media (max-width: 880px) {
  .row-1 { grid-template-columns: repeat(2, 1fr); }
  .dash-d { padding: 18px 16px 60px; }
}
</style>
