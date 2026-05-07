<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useDashboardStore } from '@/stores/dashboard'

const router = useRouter()
const authStore = useAuthStore()
const dashboardStore = useDashboardStore()

/* 현재 분기 코드 (예: 2026-Q2) */
const currentPeriod = computed(() => {
  const d = new Date()
  const q = Math.ceil((d.getMonth() + 1) / 3)
  return `${d.getFullYear()}-Q${q}`
})

onMounted(async () => {
  await dashboardStore.loadAll(currentPeriod.value)
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

/* ═══════════ Row 1 — KPI 6-up ═══════════ */
const TODAY_KPIS = [
  { key: 'progress', label: '전사 진행률', value: 73, unit: '%',
    delta: '+6.2%p 지난주', deltaPositive: true,
    icon: '📈', bg: '#E7E1FF', iconBg: '#9D85FF' },
  { key: 'pass', label: '검수 패스율', value: 87, unit: '%',
    delta: '+3%p 어제', deltaPositive: true,
    icon: '✅', bg: '#FFE8DD', iconBg: '#FF8A5C' },
  { key: 'match', label: '매칭 평균 (5축)', value: 76, unit: '점',
    delta: '-1.4% 어제', deltaPositive: false,
    icon: '🤝', bg: '#DCEEFA', iconBg: '#5DAFD8' },
  { key: 'asset', label: '자산 LIVE', value: 108, unit: '개',
    delta: '+4 신규', deltaPositive: true,
    icon: '🛍', bg: '#D7EFDD', iconBg: '#6FBF87' },
  { key: 'partner', label: '신규 협력사', value: 47, unit: '곳',
    delta: '+6 30일내', deltaPositive: true,
    icon: '🏢', bg: '#FFE2DD', iconBg: '#FF7A6B' },
  { key: 'rfp', label: 'RFP 응모', value: 28, unit: '건',
    delta: '+8 이번주', deltaPositive: true,
    icon: '📜', bg: '#FFF1D6', iconBg: '#FFC36B' },
]

const ROLE_KPI = computed(() => {
  const s = dashboardStore.summary
  if (role.value === 'GM') return {
    key: 'gm', label: '분기 달성률',
    value: s?.progressPct ?? 73, unit: '%',
    delta: `+${s?.trend ?? 4.2}%p 지난주`,
    deltaPositive: (s?.trend ?? 4.2) >= 0,
    icon: '🎯', bg: '#E7E1FF', iconBg: '#9D85FF' }
  if (role.value === 'MGR') return {
    key: 'mgr', label: '진행 중 캠페인',
    value: s?.activeCampaigns ?? 18, unit: '건',
    delta: '권한 범위 내', deltaPositive: true,
    icon: '👥', bg: '#DCEEFA', iconBg: '#5DAFD8' }
  return {
    key: 'usr', label: '내 검수 대기',
    value: s?.pendingReviews ?? 5, unit: '건',
    delta: '본인 할당분', deltaPositive: true,
    icon: '✅', bg: '#D7EFDD', iconBg: '#6FBF87' }
})

/* KPI 6-up — dashboardStore.summary 우선, mock fallback */
const KPI_LIST = computed(() => {
  const s = dashboardStore.summary
  const mapped = TODAY_KPIS.map((k) => ({ ...k }))
  if (s?.progressPct != null) mapped[0].value = s.progressPct
  // miniStats: [검수 패스율, 매칭 평균, 자산 LIVE]
  if (s?.miniStats?.[0]) mapped[1].value = parseNumeric(s.miniStats[0].value, mapped[1].value)
  if (s?.miniStats?.[1]) mapped[2].value = parseNumeric(s.miniStats[1].value, mapped[2].value)
  if (s?.miniStats?.[2]) mapped[3].value = parseNumeric(s.miniStats[2].value, mapped[3].value)
  // 신규 협력사 / RFP 응모
  if (s?.newPartnerCount != null) mapped[4].value = s.newPartnerCount
  if (s?.rfpCount != null) mapped[5].value = s.rfpCount
  // 1번은 권한별 ROLE_KPI로 교체
  return [ROLE_KPI.value, ...mapped.slice(1)]
})

/* ═══════════ Row 2-1 — 캠페인 트래픽 12개월 멀티라인 ═══════════ */
const TRAFFIC = {
  months: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12'],
  exposure:    [220, 240, 260, 280, 300, 320, 340, 360, 350, 380, 410, 430],
  engagement:  [120, 150, 180, 200, 240, 280, 260, 290, 310, 330, 360, 400],
  conversion:  [ 80,  90, 110, 130, 140, 150, 160, 170, 180, 190, 210, 240],
}
const trafficMax = computed(() =>
  Math.max(...TRAFFIC.exposure, ...TRAFFIC.engagement, ...TRAFFIC.conversion),
)
const trafficStats = computed(() => {
  const sumE = TRAFFIC.exposure.reduce((s, v) => s + v, 0)
  const sumP = TRAFFIC.engagement.reduce((s, v) => s + v, 0)
  const sumC = TRAFFIC.conversion.reduce((s, v) => s + v, 0)
  return {
    totalExp: sumE,
    avgExp: Math.round(sumE / TRAFFIC.exposure.length),
    peakExp: Math.max(...TRAFFIC.exposure),
    peakMonth: TRAFFIC.months[TRAFFIC.exposure.indexOf(Math.max(...TRAFFIC.exposure))],
    convRate: Math.round((sumC / sumE) * 100),
  }
})

/* ═══════════ Row 2-2 — 권한별 stat 카드 ═══════════ */
const ROLE_CARD = computed(() => {
  if (role.value === 'GM') {
    const goalColors = ['#9D85FF', '#FF8A5C', '#6FBF87', '#FFC36B']
    const fallbackStats = [
      { label: '노출',  short: '노', value: 87, delta:  3, unit: '%', color: '#9D85FF' },
      { label: '전환',  short: '전', value: 64, delta:  5, unit: '%', color: '#FF8A5C' },
      { label: 'ESG',   short: 'E',  value: 58, delta: -1, unit: '%', color: '#6FBF87' },
      { label: '매출',  short: '매', value: 81, delta:  2, unit: '%', color: '#FFC36B' },
    ]
    const goals = dashboardStore.quarterGoals ?? []
    const stats = goals.length > 0
      ? goals.slice(0, 4).map((g, i) => ({
          label: g.label,
          short: (g.label || '').charAt(0),
          value: g.percent ?? 0,
          delta: 0,
          unit: '%',
          color: goalColors[i % goalColors.length],
        }))
      : fallbackStats
    return {
      title: '분기 KPI 달성률',
      subtitle: `${currentPeriod.value} · 자기 조직 OrgKpi 평균`,
      mainPct: dashboardStore.summary?.progressPct ?? 73,
      stats,
      cta: '분기 KPI 보기', ctaTo: '/organization-kpis',
    }
  }
  if (role.value === 'MGR') return {
    title: '우리 팀',
    stats: [
      { label: '팀원',      value:  8, delta:  0, unit: '명', color: '#5DAFD8' },
      { label: '진행 중',   value: 18, delta:  3, unit: '건', color: '#9D85FF' },
      { label: '검수 대기', value:  4, delta:  1, unit: '건', color: '#FF8A5C' },
      { label: '지연',      value:  2, delta: -1, unit: '건', color: '#FF7A6B' },
    ],
    cta: '팀 보드', ctaTo: '/team-board',
  }
  return {
    title: '내 할 일',
    stats: [
      { label: '오늘',     value:  3, delta:  1, unit: '건', color: '#9D85FF' },
      { label: '이번 주',  value:  5, delta:  2, unit: '건', color: '#5DAFD8' },
      { label: '지연',     value:  1, delta: -1, unit: '건', color: '#FF7A6B' },
      { label: '완료',     value: 12, delta:  4, unit: '건', color: '#6FBF87' },
    ],
    cta: '캘린더', ctaTo: '/calendar',
  }
})

/* ═══════════ Row 3-1 — 목표 vs 실적 그룹막대 (6개월) ═══════════ */
const TARGET_REALITY = [
  { month: '1월', actual:  85, target: 100 },
  { month: '2월', actual: 120, target: 110 },
  { month: '3월', actual:  95, target: 130 },
  { month: '4월', actual: 140, target: 125 },
  { month: '5월', actual: 130, target: 145 },
  { month: '6월', actual: 165, target: 150 },
]
const targetStats = computed(() => {
  const totalA = TARGET_REALITY.reduce((s, m) => s + m.actual, 0)
  const totalT = TARGET_REALITY.reduce((s, m) => s + m.target, 0)
  const achieveRate = Math.round((totalA / totalT) * 100)
  const overMonths = TARGET_REALITY.filter((m) => m.actual >= m.target).length
  return { totalA, totalT, achieveRate, overMonths }
})

/* ═══════════ Row 3-2 — 자산 카테고리 도넛 (store 우선, mock fallback) ═══════════ */
const ASSET_CAT_LABELS = {
  EVENT: '이벤트/프로모션',
  PRODUCT: '제품 협찬',
  DIGITAL: '디지털 콘텐츠',
  OFFLINE: '매장/오프라인',
  MEDIA: '미디어 노출',
  UNKNOWN: '기타',
}
const ASSET_COLORS = ['#9D85FF', '#FF8A5C', '#5DAFD8', '#6FBF87', '#FFC36B', '#FF7A6B']
const ASSET_CATS_FALLBACK = [
  { type: '이벤트/프로모션', count: 42, color: '#9D85FF' },
  { type: '제품 협찬',       count: 28, color: '#FF8A5C' },
  { type: '디지털 콘텐츠',   count: 18, color: '#5DAFD8' },
  { type: '매장/오프라인',   count: 12, color: '#6FBF87' },
  { type: '미디어 노출',     count:  8, color: '#FFC36B' },
]
const ASSET_CATS = computed(() => {
  const map = dashboardStore.assetCategories ?? {}
  const keys = Object.keys(map)
  if (keys.length === 0) return ASSET_CATS_FALLBACK
  return keys
    .map((k) => ({
      type: ASSET_CAT_LABELS[k] ?? k,
      count: Number(map[k]) || 0,
      color: '',
    }))
    .filter((c) => c.count > 0)
    .sort((a, b) => b.count - a.count)
    .map((c, i) => ({ ...c, color: ASSET_COLORS[i % ASSET_COLORS.length] }))
})
const assetTotal = computed(() => ASSET_CATS.value.reduce((s, c) => s + c.count, 0))
const assetSegments = computed(() => {
  const C = 2 * Math.PI * 50  // r=50
  const total = assetTotal.value || 1
  let acc = 0
  return ASSET_CATS.value.map((c) => {
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
const MY_CAMPAIGNS = computed(() => {
  const list = dashboardStore.myCampaigns ?? []
  if (list.length === 0) return []
  return list.slice(0, 6).map((c, i) => ({
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
const PARTNER_RANK_FALLBACK = [
  { rank: 1, name: '한화호텔',   score: 94, prevRank: 1, delta:  2.1, spark: [88, 89, 91, 92, 92, 93, 94], color: '#9D85FF' },
  { rank: 2, name: '한화생명',   score: 89, prevRank: 3, delta:  4.0, spark: [82, 83, 85, 86, 87, 88, 89], color: '#FF8A5C' },
  { rank: 3, name: '한화이글스', score: 82, prevRank: 2, delta: -1.5, spark: [85, 84, 84, 83, 83, 82, 82], color: '#5DAFD8' },
  { rank: 4, name: '한화시스템', score: 76, prevRank: 4, delta:  0.8, spark: [74, 74, 75, 75, 75, 76, 76], color: '#6FBF87' },
  { rank: 5, name: '한화토탈',   score: 71, prevRank: 6, delta:  3.2, spark: [65, 66, 68, 69, 69, 70, 71], color: '#FFC36B' },
]
const PARTNER_RANK = computed(() => {
  const fromStore = dashboardStore.partnerProgress ?? []
  if (fromStore.length === 0) return PARTNER_RANK_FALLBACK
  const colors = ['#9D85FF', '#FF8A5C', '#5DAFD8', '#6FBF87', '#FFC36B']
  return fromStore.slice(0, 5).map((p, i) => {
    const score = p.averageKpiAchievementPercent ?? p.progress ?? p.score ?? 0
    const spark = (p.recent7d && p.recent7d.length > 0)
      ? p.recent7d
      : (p.spark ?? PARTNER_RANK_FALLBACK[i]?.spark ?? [])
    return {
      rank: i + 1,
      name: p.organizationName ?? p.name ?? '제휴사',
      score,
      prevRank: i + 1,
      delta: p.delta ?? 0,
      spark,
      color: colors[i],
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
    <!-- ─── Greet (한 줄, callog-header 보강) ─── -->
    <header class="greet">
      <p class="greet__hello">안녕하세요, <strong>{{ userName }}</strong> · {{ orgName }}</p>
      <span class="greet__role">{{ roleLabel }}</span>
      <span v-if="dashboardStore.usingMock" class="greet__mock">[mock 모드]</span>
    </header>

    <!-- ═══════════ Row 1 — KPI 6-up ═══════════ -->
    <section class="grid row-1">
      <article v-for="k in KPI_LIST" :key="k.key" class="kpi">
        <div class="kpi__top">
          <div class="kpi__icon" :style="{ background: k.iconBg }">{{ k.icon }}</div>
          <span class="kpi__pill" :style="{ color: k.iconBg, background: k.bg }">Today</span>
        </div>
        <div class="kpi__value">{{ k.value.toLocaleString() }}<small>{{ k.unit }}</small></div>
        <div class="kpi__label">{{ k.label }}</div>
        <div class="kpi__delta" :class="k.deltaPositive ? 'kpi__delta--pos' : 'kpi__delta--neg'">
          <svg v-if="k.deltaPositive" width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 8 L6 4 L10 8"/></svg>
          <svg v-else width="10" height="10" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2 4 L6 8 L10 4"/></svg>
          {{ k.delta }}
        </div>
      </article>
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

        <!-- GM: 캠페인 카드 형태 (chip + label + 컬러 bar + pct + delta) -->
        <template v-if="role === 'GM'">
          <div class="kpi-main">
            <span class="kpi-main__val">{{ ROLE_CARD.mainPct }}<small>%</small></span>
            <span class="kpi-main__sub">분기 평균</span>
          </div>
          <div class="kpi-bars">
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
        <div v-else class="role-stats">
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
          </div>
        </header>
        <div class="stat-strip">
          <div class="stat-mini"><span class="stat-mini__val">{{ targetStats.totalA }}</span><span class="stat-mini__lbl">실적 합계</span></div>
          <div class="stat-mini"><span class="stat-mini__val">{{ targetStats.totalT }}</span><span class="stat-mini__lbl">목표 합계</span></div>
          <div class="stat-mini stat-mini--accent"><span class="stat-mini__val">{{ targetStats.achieveRate }}<small>%</small></span><span class="stat-mini__lbl">달성률</span></div>
          <div class="stat-mini"><span class="stat-mini__val">{{ targetStats.overMonths }}<small>/6</small></span><span class="stat-mini__lbl">초과 월</span></div>
        </div>
        <svg viewBox="0 0 540 320" class="chart chart--target" aria-hidden="true">
          <g class="grid-lines">
            <line v-for="(y, i) in [20, 80, 140, 200, 260]" :key="i" :x1="40" :x2="528" :y1="y" :y2="y" />
          </g>
          <g class="axis-text axis-text--lg">
            <text x="6" y="24">200</text>
            <text x="6" y="84">150</text>
            <text x="6" y="144">100</text>
            <text x="6" y="204">50</text>
            <text x="6" y="264">0</text>
          </g>
          <g v-for="(m, i) in TARGET_REALITY" :key="m.month">
            <!-- actual 막대 (보라) -->
            <rect :x="64 + i * 78" :y="260 - m.actual * 1.2" width="28" :height="m.actual * 1.2" rx="6" fill="#9D85FF" />
            <!-- target 막대 (노랑) -->
            <rect :x="96 + i * 78" :y="260 - m.target * 1.2" width="28" :height="m.target * 1.2" rx="6" fill="#FFC36B" />
            <!-- actual 값 (보라막대 위 중앙) -->
            <text
              :x="78 + i * 78"
              :y="260 - m.actual * 1.2 - 8"
              text-anchor="middle"
              class="axis-x--val axis-x--val-actual"
            >{{ m.actual }}</text>
            <!-- target 값 (노랑막대 위 중앙) -->
            <text
              :x="110 + i * 78"
              :y="260 - m.target * 1.2 - 8"
              text-anchor="middle"
              class="axis-x--val axis-x--val-target"
            >{{ m.target }}</text>
            <!-- 월 라벨 (그룹 중심) -->
            <text :x="94 + i * 78" y="296" text-anchor="middle" class="axis-x axis-x--lg">{{ m.month }}</text>
          </g>
        </svg>
      </article>

      <!-- 자산 카테고리 도넛 (4/12) — 단순 분포 -->
      <article class="card asset-card">
        <header class="card__head">
          <div class="card__title-wrap">
            <h2 class="card__title">자산 카테고리</h2>
            <p class="card__sub">총 {{ assetTotal }}개 · 5개 분류</p>
          </div>
        </header>
        <div class="donut-wrap">
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
        <ul class="asset-legend">
          <li v-for="s in assetSegments" :key="s.type">
            <span class="asset-legend__dot" :style="{ background: s.color }"></span>
            <span class="asset-legend__label">{{ s.type }}</span>
            <span class="asset-legend__count">{{ s.count }}</span>
            <span class="asset-legend__pct">{{ s.pct }}%</span>
          </li>
        </ul>
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
        <table class="ctab">
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

      <article class="card">
        <header class="card__head">
          <div class="card__title-wrap">
            <h2 class="card__title">제휴사 TOP 5</h2>
            <p class="card__sub">달성률 · 7일 추이</p>
          </div>
          <button type="button" class="card__link" @click="goTo('/operations')">전체</button>
        </header>
        <table class="ptab">
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
                <span class="ptab__delta" :class="p.delta >= 0 ? 'pos' : 'neg'">
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

/* Greet */
.greet { display: flex; align-items: center; gap: 8px; }
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
