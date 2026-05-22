<script setup>
/**
 * Zone 4 — 흐름 (2 페이지)
 *  P1: 캠페인 파이프라인 — 도넛(4 segment) + 단계 리스트 (변형 F)
 *  P2: 매출 추이 — 연/분기 selector + YoY 가로 듀얼 막대 (변형 N)
 */
import { computed, onMounted, ref, watch } from 'vue'
import VueApexCharts from 'vue3-apexcharts'
import { useDashboardStore } from '@/stores/dashboard'
import { useDashboardZonePrefs } from '@/composables/useDashboardZonePrefs'
import { GetRevenueYoY } from '@/api/dashboard'

const ApexChart = VueApexCharts
const store = useDashboardStore()
const { prefs } = useDashboardZonePrefs()

const PAGE_COUNT = 2
const page = ref(prefs.value.zone4 ?? 0)
function shift(d) { page.value = (page.value + d + PAGE_COUNT) % PAGE_COUNT }
watch(() => prefs.value.zone4, (v) => { if (Number.isInteger(v)) page.value = v })

/* ─── ₩ 컴팩트 포맷 ─── */
function fmtWon(v) {
  const n = Number(v) || 0
  if (n >= 100_000_000) return '₩' + (n / 100_000_000).toFixed(1).replace(/\.0$/, '') + '억'
  if (n >= 10_000) return '₩' + (Math.round(n / 1000) / 10).toString().replace(/\.0$/, '') + '만'
  return '₩' + Math.round(n).toLocaleString()
}

/* ─── P1: 캠페인 파이프라인 (도넛 + 사이드 리스트) ─── */
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

/* 단계별 점유율(%) */
const pipeRows = computed(() => {
  const total = pipeTotal.value || 1
  const max = pipeMax.value
  return pipeline.value.map((p) => ({
    ...p,
    share: Math.round((p.count / total) * 100),
    barPct: Math.round((p.count / max) * 100),
  }))
})

/* 도넛 호(arc) 분할 — r=76, stroke 18, viewBox 184 */
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

/* ─── P2: 매출 추이 (YoY 가로 듀얼 막대) ─── */
const now = new Date()
const z4Year = ref(now.getFullYear())
const z4Quarter = ref(Math.ceil((now.getMonth() + 1) / 3))
const YEARS = [2024, 2025, 2026]
const QUARTERS = [1, 2, 3, 4]

const yoyData = ref([])
const yoyLoading = ref(false)
async function loadYoY() {
  yoyLoading.value = true
  try {
    const res = await GetRevenueYoY(z4Year.value, z4Quarter.value)
    yoyData.value = Array.isArray(res) ? res : (Array.isArray(res?.items) ? res.items : [])
  } catch (e) {
    yoyData.value = []
    console.warn('[zone4] revenue-yoy 실패', e)
  } finally {
    yoyLoading.value = false
  }
}
onMounted(loadYoY)
watch([z4Year, z4Quarter], loadYoY)

const yoyRows = computed(() =>
  (yoyData.value ?? []).map((d) => ({
    label: d.label,
    value: Number(d.value) || 0,
    prev: Number(d.prev) || 0,
  })))
const hasYoY = computed(() => yoyRows.value.some((r) => r.value > 0 || r.prev > 0))
const yoyMax = computed(() => Math.max(1, ...yoyRows.value.flatMap((r) => [r.value, r.prev])))
const yoyTotalCur = computed(() => yoyRows.value.reduce((s, r) => s + r.value, 0))
const yoyTotalPrev = computed(() => yoyRows.value.reduce((s, r) => s + r.prev, 0))
const yoyDelta = computed(() => {
  const prev = yoyTotalPrev.value
  if (prev === 0) return yoyTotalCur.value > 0 ? 100 : null
  return Math.round(((yoyTotalCur.value - prev) / prev) * 100)
})
function rowYoY(r) {
  if (r.prev === 0) return r.value > 0 ? 100 : null
  return Math.round(((r.value - r.prev) / r.prev) * 100)
}
const yy = (full) => String(full % 100).padStart(2, '0')

/* 월별 라인 차트 (올해 vs 작년) — 주식 차트 느낌 */
const yoySeries = computed(() => [
  { name: `올해(${z4Year.value})`, data: yoyRows.value.map((r) => r.value) },
  { name: `작년(${z4Year.value - 1})`, data: yoyRows.value.map((r) => r.prev) },
])
const yoyChartOptions = computed(() => ({
  chart: {
    type: 'area', toolbar: { show: false }, fontFamily: "'Pretendard Variable', sans-serif",
    animations: { enabled: true, easing: 'easeinout', speed: 700 }, foreColor: '#9991AE',
  },
  colors: ['#3F3463', '#C6BAE6'],
  stroke: { curve: 'smooth', width: [3, 2], dashArray: [0, 5] },
  fill: { type: 'gradient', gradient: { shadeIntensity: 0.7, opacityFrom: 0.28, opacityTo: 0, stops: [0, 95] } },
  markers: { size: 4, strokeColors: '#fff', strokeWidth: 2, hover: { size: 6 } },
  grid: { borderColor: '#E5DDF0', strokeDashArray: 4, xaxis: { lines: { show: false } } },
  dataLabels: { enabled: false },
  legend: { show: true, position: 'top', horizontalAlign: 'right', fontSize: '11px', markers: { width: 9, height: 9, radius: 3 }, labels: { colors: '#6B6582' } },
  xaxis: {
    categories: yoyRows.value.map((r) => r.label),
    axisBorder: { show: false }, axisTicks: { show: false },
    labels: { style: { fontSize: '11px', colors: '#9991AE' } },
  },
  yaxis: { labels: { style: { fontSize: '10px', colors: '#9991AE' }, formatter: (v) => fmtWon(v) } },
  tooltip: { theme: 'light', y: { formatter: (v) => fmtWon(v) } },
}))
</script>

<template>
  <section class="card zone4" aria-label="캠페인 흐름">
    <div class="card-h">
      <div class="card-h-ttl">
        <h2>{{ page === 0 ? '캠페인 파이프라인' : '매출 추이' }}</h2>
        <span class="card-dot" />
        <p class="lede">{{ page === 0 ? '내 캠페인 단계별 분포' : '연/분기 YoY 비교' }}</p>
      </div>
      <div class="z4-controls">
        <div class="zone-nav">
          <button class="nav-btn" aria-label="이전" @click="shift(-1)">‹</button>
          <span class="nav-ind">{{ page + 1 }} / {{ PAGE_COUNT }}</span>
          <button class="nav-btn" aria-label="다음" @click="shift(1)">›</button>
        </div>
      </div>
    </div>

    <Transition name="page-slide" mode="out-in">
      <!-- P1: 도넛 + 단계 리스트 -->
      <div v-if="page === 0" :key="'pipeline'" class="z4-body">
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

      <!-- P2: YoY 가로 듀얼 막대 -->
      <div v-else :key="'yoy'" class="z4-body">
        <div class="z4n-toggle">
          <select v-model.number="z4Year" aria-label="연도 선택">
            <option v-for="y in YEARS" :key="y" :value="y">{{ y }}년</option>
          </select>
          <select v-model.number="z4Quarter" aria-label="분기 선택">
            <option v-for="q in QUARTERS" :key="q" :value="q">{{ q }}분기</option>
          </select>
        </div>

        <template v-if="hasYoY">
          <div class="z4n-head">
            <div class="z4n-total">
              {{ fmtWon(yoyTotalCur) }}
            </div>
            <span class="z4n-delta" :class="{ down: (yoyDelta ?? 0) < 0 }">
              {{ yoyDelta == null ? '–' : ((yoyDelta >= 0 ? '▲ ' : '▼ ') + Math.abs(yoyDelta) + '% YoY') }}
            </span>
          </div>
          <p class="z4n-sub">전년 동기 {{ fmtWon(yoyTotalPrev) }} · {{ z4Year }} Q{{ z4Quarter }} 기준</p>

          <div class="z4n-legend">
            <span class="z4n-leg z4n-leg--cur">올해 {{ z4Year }}년</span>
            <span class="z4n-leg z4n-leg--prev">작년 {{ z4Year - 1 }}년</span>
          </div>

          <div class="z4n-rows">
            <div v-for="(r, i) in yoyRows" :key="i" class="z4n-row">
              <div class="z4n-row-h">
                <span class="z4n-row-m">{{ r.label }}</span>
                <span class="z4n-row-yoy" :class="{ down: (rowYoY(r) ?? 0) < 0 }">
                  {{ rowYoY(r) == null ? '–' : ((rowYoY(r) >= 0 ? '▲' : '▼') + Math.abs(rowYoY(r)) + '% YoY') }}
                </span>
              </div>
              <div class="z4n-bars">
                <div class="z4n-bar-row">
                  <span class="z4n-bar-tag">{{ yy(z4Year) }}</span>
                  <div class="z4n-bar-track">
                    <span class="z4n-bar-fill cur" :style="{ width: Math.round((r.value / yoyMax) * 100) + '%' }" />
                  </div>
                  <span class="z4n-bar-v">{{ fmtWon(r.value) }}</span>
                </div>
                <div class="z4n-bar-row">
                  <span class="z4n-bar-tag">{{ yy(z4Year - 1) }}</span>
                  <div class="z4n-bar-track">
                    <span class="z4n-bar-fill prev" :style="{ width: Math.round((r.prev / yoyMax) * 100) + '%' }" />
                  </div>
                  <span class="z4n-bar-v">{{ fmtWon(r.prev) }}</span>
                </div>
              </div>
            </div>
          </div>
          <!-- 월별 라인 차트 (올해 vs 작년) — 주식 차트 느낌 -->
          <div class="z4n-chart">
            <ApexChart type="area" height="100%" :options="yoyChartOptions" :series="yoySeries" />
          </div>
        </template>
        <div v-else class="z4-empty">매출 데이터가 없습니다.</div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.zone4 {
  display: flex; flex-direction: column; height: 100%;
  /* base.css에 없는 urgent 톤 로컬 선언 (YoY 음수·마감 등) */
  --urgent: #E25B49; --urgent-soft: rgba(226,91,73,.14);
}
.card-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 12px; }
.card-h-ttl { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.card-dot { width: 9px; height: 9px; border-radius: 999px; background: var(--lp-primary); align-self: center; flex-shrink: 0; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; flex-shrink: 0; }
.card-h .lede { margin: 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.z4-controls { display: inline-flex; align-items: center; gap: 10px; }

.zone-nav { display: inline-flex; align-items: center; gap: 6px; }
.nav-btn { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-primary-deep); cursor: pointer; font-size: 14px; line-height: 1; transition: background .15s, transform .12s; }
.nav-btn:hover { background: var(--lp-surface-soft); }
.nav-btn:active { transform: scale(0.94); }
.nav-ind { font-size: 11px; font-weight: 600; color: var(--lp-text-faint); min-width: 36px; text-align: center; font-variant-numeric: tabular-nums; }

.z4-body { flex: 1; min-height: 0; display: flex; flex-direction: column; }

/* ── P1: 도넛 ── */
.z4-donut-block { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 8px 0 18px; }
.z4-donut { position: relative; width: 184px; height: 184px; animation: lp-rise .5s cubic-bezier(.4,0,.2,1) both; }
.z4-donut-arc { transition: stroke-dasharray .9s cubic-bezier(.4,0,.2,1); }
.z4-donut-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.z4-donut-v { font-size: 36px; font-weight: 800; letter-spacing: -0.03em; color: var(--lp-text); font-variant-numeric: tabular-nums; line-height: 1; }
.z4-donut-l { font-size: 11px; color: var(--lp-text-muted); margin-top: 4px; }

.z4-stage-list { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; padding-right: 4px; }
.z4-stage-row { display: grid; grid-template-columns: 1fr auto; gap: 4px 12px; align-items: center; padding: 9px 10px; border-radius: 10px; cursor: pointer; transition: background .15s; }
.z4-stage-row:hover { background: var(--lp-surface-soft); }
.z4-stage-lbl { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 700; color: var(--lp-text); }
.z4-stage-lbl::before { content: ''; width: 9px; height: 9px; border-radius: 2px; background: var(--c); flex-shrink: 0; }
.z4-stage-v { font-size: 14px; font-weight: 800; color: var(--lp-text); font-variant-numeric: tabular-nums; }
.z4-stage-v small { font-size: 10px; font-weight: 600; color: var(--lp-text-muted); margin-left: 4px; }
.z4-stage-bar { grid-column: 1 / -1; height: 5px; border-radius: 999px; background: var(--lp-surface-soft); overflow: hidden; }
.z4-stage-bar-fill { display: block; height: 100%; border-radius: 999px; transition: width .9s cubic-bezier(.4,0,.2,1); transform-origin: left; animation: lp-grow-x .7s cubic-bezier(.4,0,.2,1) both; }

/* ── P2: YoY ── */
.z4n-toggle { display: flex; gap: 6px; margin-bottom: 12px; }
.z4n-toggle select {
  flex: 1; padding: 6px 26px 6px 10px;
  font-size: 11.5px; font-weight: 700;
  color: var(--lp-primary-deep);
  background-color: var(--lp-surface-soft);
  border: 1px solid transparent; border-radius: var(--r-md, 14px);
  cursor: pointer; font-family: inherit; appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M2 4l4 4 4-4' fill='none' stroke='%233F3463' stroke-width='1.6' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 10px center;
}
.z4n-toggle select:focus { outline: none; border-color: var(--lp-primary); }

.z4n-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.z4n-total { font-size: 26px; font-weight: 800; letter-spacing: -0.02em; color: var(--lp-text); font-variant-numeric: tabular-nums; }
.z4n-delta { padding: 3px 9px; border-radius: 999px; font-size: 10.5px; font-weight: 800; background: rgba(168,189,66,.20); color: #4F7A2E; white-space: nowrap; }
.z4n-delta.down { background: var(--urgent-soft); color: var(--urgent); }
.z4n-sub { font-size: 11px; color: var(--lp-text-muted); margin: 4px 0 12px; }

.z4n-legend { display: flex; gap: 16px; margin-bottom: 12px; }
.z4n-leg { display: inline-flex; align-items: center; gap: 6px; font-size: 11px; font-weight: 600; color: var(--lp-text-muted); }
.z4n-leg::before { content: ''; width: 9px; height: 9px; border-radius: 3px; }
.z4n-leg--cur::before { background: var(--lp-primary-deep); }
.z4n-leg--prev::before { background: #C6BAE6; }

.z4n-rows { flex: 0 0 auto; display: flex; flex-direction: column; gap: 12px; padding-right: 2px; }
.z4n-chart { flex: 1; min-height: 150px; margin-top: 10px; }
.z4n-chart :deep(.apexcharts-canvas) { margin: 0 auto; }
.z4n-row-h { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 5px; }
.z4n-row-m { font-size: 11.5px; font-weight: 800; color: var(--lp-text); }
.z4n-row-yoy { font-size: 10.5px; font-weight: 700; color: #4F7A2E; }
.z4n-row-yoy.down { color: var(--urgent); }
.z4n-bars { display: flex; flex-direction: column; gap: 3px; }
.z4n-bar-row { display: grid; grid-template-columns: 26px 1fr auto; align-items: center; gap: 8px; }
.z4n-bar-tag { font-size: 9px; font-weight: 800; color: var(--lp-text-faint); font-variant-numeric: tabular-nums; }
.z4n-bar-track { height: 10px; border-radius: 4px; background: var(--lp-surface-soft); overflow: hidden; }
.z4n-bar-fill { display: block; height: 100%; border-radius: 4px; transition: width .9s cubic-bezier(.4,0,.2,1); transform-origin: left; animation: lp-grow-x .7s cubic-bezier(.4,0,.2,1) both; }
.z4n-bar-fill.cur { background: var(--lp-primary-deep); }
.z4n-bar-fill.prev { background: #C6BAE6; }
.z4n-bar-v { font-size: 10.5px; font-weight: 700; color: var(--lp-text-muted); min-width: 52px; text-align: right; font-variant-numeric: tabular-nums; }

.z4-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 24px; text-align: center; }

.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(16px); }
.page-slide-leave-to { opacity: 0; transform: translateX(-16px); }
</style>
