<script setup>
/**
 * Zone 4 — 2 페이지 캐러셀 (하단 화살표/점 전환)
 *  ① 성과 트래커 + 매출 추이 — 한 섹션(한 몸): 위 퍼플 게이지 카드 + 아래 매출 차트 카드 (프리뷰 그대로)
 *  ② 캠페인 파이프라인 — 도넛 + 단계 리스트
 */
import { computed, onMounted, ref } from 'vue'
import VueApexCharts from 'vue3-apexcharts'
import { useDashboardStore } from '@/stores/dashboard'
import { GetRevenueQuarters } from '@/api/dashboard'

const ApexChart = VueApexCharts
const store = useDashboardStore()

const PAGE_COUNT = 2 // ① 성과 트래커 + 매출 추이  ② 캠페인 파이프라인
const page = ref(0)  // 항상 매출추이(첫 페이지)로 시작 — 저장된 stale 기본값 무시
function shift(d) { page.value = (page.value + d + PAGE_COUNT) % PAGE_COUNT }

/* ─── ₩ 컴팩트 포맷 ─── */
function fmtWon(v) {
  const n = Number(v) || 0
  if (n >= 100_000_000) return '₩' + (n / 100_000_000).toFixed(1).replace(/\.0$/, '') + '억'
  if (n >= 10_000) return '₩' + (Math.round(n / 1000) / 10).toString().replace(/\.0$/, '') + '만'
  return '₩' + Math.round(n).toLocaleString()
}

const now = new Date()

/* ─── 성과 트래커 (분기 달성률 게이지 + 3스탯) ─── */
const finGoal = computed(() => {
  const fins = (store.quarterGoals ?? []).filter(
    (g) => String(g.category ?? '').toUpperCase() === 'FINANCIAL',
  )
  if (!fins.length) return { target: 0, actual: 0, pct: 0, months: [0, 0, 0] }
  const target = fins.reduce((s, g) => s + (Number(g.targetValue) || 0), 0)
  const actual = fins.reduce((s, g) => s + (Number(g.actualValue) || 0), 0)
  const pct = target > 0 ? Math.round((actual / target) * 100) : (actual > 0 ? 100 : 0)
  const months = [0, 1, 2].map((i) =>
    fins.reduce((s, g) => s + (Number(g.monthlyActuals?.[i]) || 0), 0),
  )
  return { target, actual, pct, months }
})
const achievePct = computed(() => Math.max(0, Math.min(100, finGoal.value.pct)))
const revenueTotal = computed(() => finGoal.value.actual)
const campaignCount = computed(() => (store.myCampaigns ?? []).length)
const assetLive = computed(() =>
  Object.values(store.assetCategories ?? {}).reduce((s, v) => s + (Number(v) || 0), 0),
)
const GAUGE_R = 90
const GAUGE_LEN = Math.PI * GAUGE_R
const gaugeDash = computed(() => `${(achievePct.value / 100) * GAUGE_LEN} ${GAUGE_LEN}`)
const gaugeKnob = computed(() => {
  const th = ((180 - 1.8 * achievePct.value) * Math.PI) / 180
  return { x: 110 + GAUGE_R * Math.cos(th), y: 110 - GAUGE_R * Math.sin(th) }
})

/* ─── 매출 추이 (월간 / 분기) ─── */
const revMode = ref('month') // 'month' | 'quarter'
const quarterData = ref([])
async function loadQuarters() {
  try {
    const res = await GetRevenueQuarters(now.getFullYear())
    quarterData.value = Array.isArray(res) ? res : (Array.isArray(res?.items) ? res.items : [])
  } catch (e) {
    quarterData.value = []
    console.warn('[zone4] revenue-quarters 실패', e)
  }
}
onMounted(loadQuarters)

const monthLabels = computed(() => {
  const q = Math.ceil((now.getMonth() + 1) / 3)
  const first = (q - 1) * 3 + 1
  return [first, first + 1, first + 2].map((m) => `${m}월`)
})
const revView = computed(() => {
  if (revMode.value === 'quarter') {
    return {
      labels: quarterData.value.map((p) => p.label),
      data: quarterData.value.map((p) => Number(p.value) || 0),
    }
  }
  return { labels: monthLabels.value, data: finGoal.value.months }
})
const revTotal = computed(() => revView.value.data.reduce((s, v) => s + v, 0))
const hasRev = computed(() => revView.value.data.some((v) => v > 0))
const revChartSeries = computed(() => [{ name: '매출', data: revView.value.data }])
const revChartOptions = computed(() => ({
  chart: {
    type: 'area', toolbar: { show: false }, fontFamily: "'Pretendard Variable', sans-serif",
    animations: { enabled: true, easing: 'easeinout', speed: 800 }, foreColor: '#9991AE',
  },
  colors: ['#9D85FF'],
  stroke: { curve: 'smooth', width: 3 },
  fill: { type: 'gradient', gradient: { shadeIntensity: 0.8, opacityFrom: 0.30, opacityTo: 0, stops: [0, 90] } },
  markers: { size: 4, colors: ['#9D85FF'], strokeColors: '#fff', strokeWidth: 2, hover: { size: 6 } },
  grid: { borderColor: '#E7DECF', strokeDashArray: 4, xaxis: { lines: { show: false } } },
  dataLabels: { enabled: false },
  xaxis: {
    categories: revView.value.labels,
    axisBorder: { show: false }, axisTicks: { show: false },
    labels: { style: { fontSize: '11px', colors: '#9991AE' } },
  },
  yaxis: { labels: { style: { fontSize: '10px', colors: '#9991AE' }, formatter: (v) => fmtWon(v) } },
  tooltip: { y: { formatter: (v) => '₩' + Number(v).toLocaleString() } },
}))

/* ─── 캠페인 파이프라인 (도넛 + 사이드 리스트) ─── */
const STAGE_LABELS = {
  PLANNING: '기획', PLAN: '기획', DRAFT: '기획',
  EXECUTION: '실행', LIVE: '실행', RUNNING: '실행', ACTIVE: '실행', IN_PROGRESS: '실행',
  REVIEW: '검수', IN_REVIEW: '검수',
  COMPLETED: '완료', DONE: '완료', ARCHIVED: '완료',
}
const STAGE_COLORS = ['#9D85FF', '#5DAFD8', '#FF8A5C', '#6FBF87', '#FFC36B']
const pipeline = computed(() => {
  const list = store.campaignPipeline ?? []
  return list.map((s, i) => ({
    label: STAGE_LABELS[String(s.stage ?? '').toUpperCase()] ?? s.stage,
    count: Number(s.count) || 0,
    color: STAGE_COLORS[i % STAGE_COLORS.length],
  }))
})
const pipeTotal = computed(() => pipeline.value.reduce((s, p) => s + p.count, 0))
const pipeMax = computed(() => Math.max(1, ...pipeline.value.map((p) => p.count)))
const hasPipeline = computed(() => pipeTotal.value > 0)
const pipeRows = computed(() => {
  const total = pipeTotal.value || 1
  const max = pipeMax.value
  return pipeline.value.map((p) => ({
    ...p,
    share: Math.round((p.count / total) * 100),
    barPct: Math.round((p.count / max) * 100),
  }))
})
const DONUT_R = 76
const DONUT_C = 2 * Math.PI * DONUT_R
const donutSegs = computed(() => {
  const total = pipeTotal.value || 1
  let offset = 0
  return pipeline.value
    .filter((p) => p.count > 0)
    .map((p) => {
      const frac = p.count / total
      const len = frac * DONUT_C
      const seg = { color: p.color, dash: `${len} ${DONUT_C - len}`, offset: -offset }
      offset += len
      return seg
    })
})
</script>

<template>
  <section class="card zone4" :class="{ 'zone4--bare': page === 0 }" aria-label="캠페인 흐름">
    <Transition name="page-slide" mode="out-in">
      <!-- ① 성과 트래커 + 매출 추이 (한 몸: 위/아래 카드) -->
      <div v-if="page === 0" :key="'tracker-rev'" class="z4-page z4-stack">
        <!-- 성과 트래커 (퍼플 카드) -->
        <div class="z4-card z4-card--tracker">
          <div class="z4-pg-h">
            <h2>성과 트래커</h2>
            <span class="z4-pg-dot" />
            <p>분기 매출 vs 목표 추이</p>
          </div>
          <div class="z4-gauge-wrap">
            <svg width="220" height="120" viewBox="0 0 220 124" aria-hidden="true">
              <path d="M 20 110 A 90 90 0 0 1 200 110" fill="none" stroke="rgba(255,255,255,.22)" stroke-width="16" stroke-linecap="round" />
              <path d="M 20 110 A 90 90 0 0 1 200 110" fill="none" stroke="#fff" stroke-width="16" stroke-linecap="round" :stroke-dasharray="gaugeDash" class="z4-gauge-arc" />
              <circle :cx="gaugeKnob.x" :cy="gaugeKnob.y" r="8" fill="#fff" />
            </svg>
            <div class="z4-gauge-pill">
              <span class="z4-gauge-pct">{{ achievePct }}%</span>
              <span class="z4-gauge-l">분기 달성률</span>
            </div>
          </div>
          <div class="z4-stats">
            <div class="z4-stat"><span class="z4-stat-l">매출 합계</span><span class="z4-stat-v">{{ fmtWon(revenueTotal) }}</span></div>
            <div class="z4-stat"><span class="z4-stat-l">캠페인</span><span class="z4-stat-v">{{ campaignCount }}건</span></div>
            <div class="z4-stat"><span class="z4-stat-l">자산 LIVE</span><span class="z4-stat-v">{{ assetLive }}</span></div>
          </div>
        </div>

        <!-- 매출 추이 (라이트 카드) -->
        <div class="z4-card z4-card--rev">
          <div class="z4-rev-h">
            <div class="z4-rev-h-l">
              <h2>매출 추이</h2>
              <span>{{ revMode === 'month' ? '이번 분기 · 월간' : `${now.getFullYear()} · 분기` }}</span>
            </div>
            <div class="z4-seg">
              <button :class="{ on: revMode === 'month' }" @click="revMode = 'month'">월간</button>
              <button :class="{ on: revMode === 'quarter' }" @click="revMode = 'quarter'">분기</button>
            </div>
          </div>
          <div class="z4-rev-total">{{ fmtWon(revTotal) }}</div>
          <div v-if="hasRev" class="z4-rev-chart">
            <ApexChart type="area" height="100%" :options="revChartOptions" :series="revChartSeries" />
          </div>
          <div v-else class="z4-empty z4-empty--sm">매출 데이터가 없습니다.</div>
        </div>
      </div>

      <!-- ② 캠페인 파이프라인 (흰 카드) -->
      <div v-else :key="'pipeline'" class="z4-page z4-scroll">
        <div class="z4-pg-h z4-pg-h--light">
          <h2>캠페인 파이프라인</h2>
          <span class="z4-pg-dot" />
          <p>내 캠페인 단계별 분포</p>
        </div>
        <template v-if="hasPipeline">
          <div class="z4-donut-block">
            <div class="z4-donut">
              <svg width="184" height="184" viewBox="0 0 184 184" aria-hidden="true">
                <circle cx="92" cy="92" :r="DONUT_R" fill="none" stroke="var(--lp-surface-soft)" stroke-width="18" />
                <circle
                  v-for="(s, i) in donutSegs" :key="i"
                  cx="92" cy="92" :r="DONUT_R" fill="none"
                  :stroke="s.color" stroke-width="18" stroke-linecap="butt"
                  :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset"
                  transform="rotate(-90 92 92)" class="z4-donut-arc"
                />
              </svg>
              <div class="z4-donut-center">
                <span class="z4-donut-v">{{ pipeTotal }}</span>
                <span class="z4-donut-l">전체 캠페인</span>
              </div>
            </div>
          </div>
          <ul class="z4-stage-list">
            <li v-for="(r, i) in pipeRows" :key="i" class="z4-stage-row">
              <span class="z4-stage-lbl" :style="{ '--c': r.color }">{{ r.label }}</span>
              <span class="z4-stage-v">{{ r.count }}<small>{{ r.share }}%</small></span>
              <div class="z4-stage-bar">
                <span class="z4-stage-bar-fill" :style="{ width: r.barPct + '%', background: r.color }" />
              </div>
            </li>
          </ul>
        </template>
        <div v-else class="z4-empty">파이프라인 데이터가 없습니다.</div>
      </div>
    </Transition>

    <!-- 하단 캐러셀: 화살표 + 점 (활성 = 보라 알약) -->
    <div class="z4-dots">
      <button class="z4-dots-arw" aria-label="이전" @click="shift(-1)">‹</button>
      <span
        v-for="i in PAGE_COUNT"
        :key="i"
        class="z4-dot"
        :class="{ on: page === i - 1 }"
        role="button"
        :aria-label="`${i}번째 페이지`"
        @click="page = i - 1"
      />
      <button class="z4-dots-arw" aria-label="다음" @click="shift(1)">›</button>
    </div>
  </section>
</template>

<style scoped>
.zone4 {
  display: flex; flex-direction: column; height: 100%;
  --urgent: #E25B49; --urgent-soft: rgba(226,91,73,.14);
}
/* 성과트래커+매출추이 페이지 — 외곽 카드 배경/그림자 숨김 (안쪽 카드가 자체 배경) */
.zone4--bare { background: transparent !important; box-shadow: none !important; padding: 12px !important; }

.z4-page { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.z4-scroll { overflow-y: auto; overflow-x: hidden; }
.z4-stack { gap: 14px; overflow-y: auto; }

/* 섹션 헤더 */
.z4-pg-h { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.z4-pg-h h2 { margin: 0; font-size: 18px; font-weight: 800; letter-spacing: -0.01em; }
.z4-pg-h p { margin: 0; font-size: 12px; font-weight: 500; }
.z4-pg-dot { width: 8px; height: 8px; border-radius: 999px; align-self: center; flex-shrink: 0; }
.z4-pg-h--light h2 { color: var(--lp-text); }
.z4-pg-h--light p { color: var(--lp-text-muted); }
.z4-pg-h--light .z4-pg-dot { background: var(--lp-primary); }

/* 공통 카드 */
.z4-card { border-radius: 18px; flex-shrink: 0; display: flex; flex-direction: column; }

/* ── 성과 트래커 (퍼플) ── */
.z4-card--tracker {
  background: linear-gradient(155deg, #BCA9EA 0%, #9D85FF 60%, #8E72F2 100%);
  color: #fff; padding: 18px 20px 16px; position: relative; overflow: hidden; gap: 8px;
}
.z4-card--tracker::before { content:''; position:absolute; top:-60px; right:-40px; width:180px; height:180px; border-radius:999px; background:rgba(255,255,255,.10); }
.z4-card--tracker .z4-pg-h { position: relative; }
.z4-card--tracker .z4-pg-h h2 { color: #fff; }
.z4-card--tracker .z4-pg-h p { color: rgba(255,255,255,.82); }
.z4-card--tracker .z4-pg-dot { background: rgba(255,255,255,.7); }
.z4-gauge-wrap { position: relative; display: flex; justify-content: center; }
.z4-gauge-arc { transition: stroke-dasharray .8s cubic-bezier(.4,0,.2,1); }
.z4-gauge-pill {
  position: absolute; left: 50%; top: 64%; transform: translate(-50%, -50%);
  background: rgba(255,255,255,.18); border-radius: 14px; padding: 8px 22px;
  display: flex; flex-direction: column; align-items: center; line-height: 1.05;
}
.z4-gauge-pct { font-size: 26px; font-weight: 800; letter-spacing: -0.02em; }
.z4-gauge-l { font-size: 10.5px; font-weight: 500; color: rgba(255,255,255,.85); margin-top: 2px; }
.z4-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; position: relative; }
.z4-stat { background: rgba(255,255,255,.16); border-radius: 13px; padding: 11px 8px; text-align: center; }
.z4-stat-l { display: block; font-size: 10.5px; font-weight: 600; color: rgba(255,255,255,.82); }
.z4-stat-v { display: block; font-size: 16px; font-weight: 800; margin-top: 4px; letter-spacing: -0.01em; font-variant-numeric: tabular-nums; }

/* ── 매출 추이 (라이트) ── */
.z4-card--rev { background: var(--lp-cream, #F4EFE6); padding: 16px 18px 14px; }
.z4-rev-h { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.z4-rev-h-l { display: flex; align-items: baseline; gap: 7px; min-width: 0; }
.z4-rev-h-l h2 { margin: 0; font-size: 16px; font-weight: 800; color: var(--lp-text); }
.z4-rev-h-l span { font-size: 11px; color: var(--lp-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.z4-seg { display: inline-flex; background: rgba(63,52,99,.07); border-radius: 999px; padding: 3px; gap: 2px; flex-shrink: 0; }
.z4-seg button { border: 0; background: transparent; font: inherit; font-size: 11.5px; font-weight: 700; color: var(--lp-text-muted); padding: 5px 13px; border-radius: 999px; cursor: pointer; transition: background .15s, color .15s; }
.z4-seg button.on { background: var(--lp-surface); color: var(--lp-primary-deep); box-shadow: 0 1px 3px rgba(63,52,99,.14); }
.z4-rev-total { font-size: 24px; font-weight: 800; letter-spacing: -0.02em; color: var(--lp-text); font-variant-numeric: tabular-nums; margin: 10px 0 4px; }
.z4-rev-chart { height: 248px; }
.z4-rev-chart :deep(.apexcharts-canvas) { margin: 0 auto; }

/* ── 도넛 ── */
.z4-donut-block { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 14px 0 18px; }
.z4-donut { position: relative; width: 184px; height: 184px; animation: lp-rise .5s cubic-bezier(.4,0,.2,1) both; }
.z4-donut-arc { transition: stroke-dasharray .9s cubic-bezier(.4,0,.2,1); }
.z4-donut-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.z4-donut-v { font-size: 36px; font-weight: 800; letter-spacing: -0.03em; color: var(--lp-text); font-variant-numeric: tabular-nums; line-height: 1; }
.z4-donut-l { font-size: 11px; color: var(--lp-text-muted); margin-top: 4px; }
.z4-stage-list { display: flex; flex-direction: column; gap: 12px; }
.z4-stage-row { display: grid; grid-template-columns: 1fr auto; gap: 4px 12px; align-items: center; padding: 9px 10px; border-radius: 10px; cursor: pointer; transition: background .15s; }
.z4-stage-row:hover { background: var(--lp-surface-soft); }
.z4-stage-lbl { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 700; color: var(--lp-text); }
.z4-stage-lbl::before { content: ''; width: 9px; height: 9px; border-radius: 2px; background: var(--c); flex-shrink: 0; }
.z4-stage-v { font-size: 14px; font-weight: 800; color: var(--lp-text); font-variant-numeric: tabular-nums; }
.z4-stage-v small { font-size: 10px; font-weight: 600; color: var(--lp-text-muted); margin-left: 4px; }
.z4-stage-bar { grid-column: 1 / -1; height: 5px; border-radius: 999px; background: var(--lp-surface-soft); overflow: hidden; }
.z4-stage-bar-fill { display: block; height: 100%; border-radius: 999px; transition: width .9s cubic-bezier(.4,0,.2,1); transform-origin: left; animation: lp-grow-x .7s cubic-bezier(.4,0,.2,1) both; }

.z4-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 24px; text-align: center; }
.z4-empty--sm { height: 160px; flex: none; }

/* ── 하단 캐러셀 점 + 화살표 ── */
.z4-dots { display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 12px; flex-shrink: 0; }
.z4-dots-arw { width: 30px; height: 30px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-text-muted); cursor: pointer; font-size: 14px; line-height: 1; transition: background .15s, transform .12s; }
.z4-dots-arw:hover { background: var(--lp-surface-soft); }
.z4-dots-arw:active { transform: scale(0.94); }
.z4-dot { width: 8px; height: 8px; border-radius: 999px; background: #D8CEEC; cursor: pointer; transition: width .25s ease, background .25s ease; }
.z4-dot.on { width: 22px; background: var(--lp-primary); }

.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(16px); }
.page-slide-leave-to { opacity: 0; transform: translateX(-16px); }
</style>
