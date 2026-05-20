<script setup>
/**
 * Zone 4 — 흐름 (퍼널 & 추세) (2 페이지)
 *  P1: 캠페인 파이프라인 퍼널 — campaign-pipeline(stage별 count) 막대 깔때기
 *  P2: 매출 추이 — revenue-trend(REVENUE KPI 월별) ApexCharts 라인
 */
import { computed, ref, watch } from 'vue'
import VueApexCharts from 'vue3-apexcharts'
import { useDashboardStore } from '@/stores/dashboard'
import { useUserSettingsStore } from '@/stores/userSettings'
import { useDashboardZonePrefs } from '@/composables/useDashboardZonePrefs'

const ApexChart = VueApexCharts
const store = useDashboardStore()
const userSettings = useUserSettingsStore()
const { prefs, setZoneDefault } = useDashboardZonePrefs()

const PAGE_COUNT = 2
const page = ref(prefs.value.zone4 ?? 0)
function shift(d) { page.value = (page.value + d + PAGE_COUNT) % PAGE_COUNT }
watch(() => prefs.value.zone4, (v) => { if (Number.isInteger(v)) page.value = v })

/* ─── P1: 파이프라인 퍼널 ─── */
const STAGE_LABELS = {
  PLANNING: '기획', PLAN: '기획', DRAFT: '기획',
  EXECUTION: '실행', LIVE: '실행', RUNNING: '실행', ACTIVE: '실행', IN_PROGRESS: '실행',
  REVIEW: '검수', IN_REVIEW: '검수',
  COMPLETED: '완료', DONE: '완료', ARCHIVED: '완료',
}
const STAGE_COLORS = ['#9D85FF', '#5DAFD8', '#FF8A5C', '#6FBF87', '#FFC36B']
const funnel = computed(() => {
  const list = store.campaignPipeline ?? []
  const max = Math.max(1, ...list.map((s) => Number(s.count) || 0))
  return list.map((s, i) => ({
    label: STAGE_LABELS[String(s.stage ?? '').toUpperCase()] ?? s.stage,
    count: Number(s.count) || 0,
    pct: Math.round(((Number(s.count) || 0) / max) * 100),
    color: STAGE_COLORS[i % STAGE_COLORS.length],
  }))
})
const hasFunnel = computed(() => funnel.value.some((f) => f.count > 0))
const funnelTotal = computed(() => funnel.value.reduce((s, f) => s + (f.count || 0), 0))

/* 완료율 게이지: '완료' 단계 비중 (있는 데이터로 산출) */
const completedPct = computed(() => {
  const total = funnelTotal.value
  if (!total) return 0
  const done = funnel.value.filter((f) => f.label === '완료').reduce((s, f) => s + (f.count || 0), 0)
  return Math.round((done / total) * 100)
})
const GAUGE_LEN = Math.PI * 80 // 반원 호 전체 길이 (r=80)
const gaugeDash = computed(() => `${(completedPct.value / 100) * GAUGE_LEN} ${GAUGE_LEN}`)
/* 미니바 플러리시: 단계별 count 정규화 (장식) */
const miniBars = computed(() => {
  const max = Math.max(1, ...funnel.value.map((f) => f.count || 0))
  return funnel.value.map((f) => Math.max(4, Math.round(((f.count || 0) / max) * 26)))
})

/* ─── P2: 매출 추이 라인 ─── */
const trend = computed(() => store.revenueTrend ?? [])
const hasTrend = computed(() => trend.value.length > 0 && trend.value.some((p) => Number(p.value) > 0))
const trendLabels = computed(() => trend.value.map((p) => p.label))
const trendSeries = computed(() => [{ name: '매출', data: trend.value.map((p) => Number(p.value) || 0) }])

function fmtCompact(v) {
  const n = Number(v) || 0
  if (n >= 100_000_000) return (n / 100_000_000).toFixed(1) + '억'
  if (n >= 10_000) return Math.round(n / 1000) / 10 + '만'
  if (n >= 1000) return Math.round(n / 100) / 10 + 'k'
  return Math.round(n).toString()
}
const apexLineOptions = computed(() => ({
  chart: {
    type: 'area', toolbar: { show: false }, fontFamily: "'Pretendard Variable', sans-serif",
    animations: { enabled: !userSettings.themeUi.reduceMotion, easing: 'easeinout', speed: 800 },
    foreColor: '#9991AE',
  },
  colors: ['#9D85FF'],
  stroke: { curve: 'smooth', width: 3 },
  fill: { type: 'gradient', gradient: { shadeIntensity: 0.8, opacityFrom: 0.32, opacityTo: 0, stops: [0, 90] } },
  markers: { size: 4, colors: ['#9D85FF'], strokeColors: '#fff', strokeWidth: 2, hover: { size: 6 } },
  grid: { borderColor: '#E5DDF0', strokeDashArray: 4, xaxis: { lines: { show: false } } },
  dataLabels: { enabled: false },
  xaxis: {
    categories: trendLabels.value, axisBorder: { show: false }, axisTicks: { show: false },
    labels: { style: { fontSize: '11px', colors: '#9991AE' } },
  },
  yaxis: { labels: { style: { fontSize: '10.5px', colors: '#9991AE' }, formatter: (v) => fmtCompact(v) } },
  tooltip: { y: { formatter: (v) => '₩' + Number(v).toLocaleString() } },
}))

/* ⋯ 기본 화면 지정 */
const menuOpen = ref(false)
function makeDefault() { setZoneDefault('zone4', page.value); menuOpen.value = false }
</script>

<template>
  <section class="card zone4" aria-label="캠페인 흐름">
    <div class="card-h">
      <div>
        <h2>{{ page === 0 ? '캠페인 파이프라인' : '매출 추이' }}</h2>
        <p class="lede">{{ page === 0 ? '내 캠페인 단계별 분포' : 'REVENUE KPI 월별 실적' }}</p>
      </div>
      <div class="z4-controls">
        <div class="z4-menu-wrap">
          <button class="z4-dots" aria-label="옵션" @click="menuOpen = !menuOpen">⋯</button>
          <Transition name="z4-pop">
            <div v-if="menuOpen" class="z4-menu" @mouseleave="menuOpen = false">
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
      <!-- P1: 퍼널 (퍼플 그라데이션 히어로) -->
      <div v-if="page === 0" :key="'funnel'" class="z4-body z4-body--hero">
        <div v-if="hasFunnel" class="z4-hero">
          <div class="z4-hero-top">
            <div class="z4-gauge">
              <svg width="124" height="74" viewBox="0 0 200 120" aria-hidden="true">
                <path d="M 20 100 A 80 80 0 0 1 180 100" fill="none" stroke="rgba(255,255,255,0.20)" stroke-width="14" stroke-linecap="round" />
                <path d="M 20 100 A 80 80 0 0 1 180 100" fill="none" stroke="#fff" stroke-width="14" stroke-linecap="round" :stroke-dasharray="gaugeDash" class="z4-gauge-arc" />
              </svg>
              <div class="z4-gauge-pill">
                <span class="z4-gauge-v">{{ completedPct }}%</span>
                <span class="z4-gauge-lbl">완료율</span>
              </div>
            </div>
            <div class="z4-hero-meta">
              <span class="z4-hero-total"><span class="z4-hero-total-num">{{ funnelTotal }}</span><span class="z4-hero-total-unit">건</span></span>
              <span class="z4-hero-cap">진행 중 캠페인</span>
              <div class="z4-mini-bars">
                <span v-for="(h, i) in miniBars" :key="i" :style="{ height: h + 'px' }" />
              </div>
            </div>
          </div>
          <ul class="z4-funnel">
            <li v-for="(f, i) in funnel" :key="i" class="z4-stage">
              <span class="z4-stage-lbl">{{ f.label }}</span>
              <div class="z4-stage-track">
                <span class="z4-stage-fill" :style="{ width: Math.max(6, f.pct) + '%', background: f.color }" />
              </div>
              <span class="z4-stage-count">{{ f.count }}</span>
            </li>
          </ul>
        </div>
        <div v-else class="z4-empty">파이프라인 데이터가 없습니다.</div>
      </div>

      <!-- P2: 매출 라인 -->
      <div v-else :key="'revenue'" class="z4-body">
        <div v-if="hasTrend" class="z4-chart-wrap">
          <ApexChart type="area" height="100%" :options="apexLineOptions" :series="trendSeries" />
        </div>
        <div v-else class="z4-empty">매출 추이 데이터가 없습니다.</div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.zone4 { display: flex; flex-direction: column; height: 100%; }
.card-h { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; gap: 12px; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; }
.card-h .lede { margin: 4px 0 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); }

.z4-controls { display: inline-flex; align-items: center; gap: 10px; }
.z4-menu-wrap { position: relative; }
.z4-dots { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-text-muted); cursor: pointer; font-size: 15px; line-height: 1; }
.z4-dots:hover { background: var(--lp-surface-soft); }
.z4-menu { position: absolute; top: calc(100% + 6px); right: 0; background: var(--lp-surface); border: 1px solid var(--lp-border); border-radius: 10px; box-shadow: 0 8px 24px rgba(63,52,99,.16); padding: 5px; z-index: 30; white-space: nowrap; }
.z4-menu button { display: block; padding: 7px 12px; font-size: 12.5px; font-weight: 600; color: var(--lp-text); background: transparent; border: 0; border-radius: 7px; cursor: pointer; }
.z4-menu button:hover { background: var(--lp-surface-soft); color: var(--lp-primary-deep); }
.z4-pop-enter-active, .z4-pop-leave-active { transition: opacity .15s ease, transform .15s ease; }
.z4-pop-enter-from, .z4-pop-leave-to { opacity: 0; transform: translateY(-4px); }

.zone-nav { display: inline-flex; align-items: center; gap: 6px; }
.nav-btn { width: 26px; height: 26px; border-radius: 999px; border: 1px solid var(--lp-border); background: var(--lp-surface); color: var(--lp-primary-deep); cursor: pointer; font-size: 14px; line-height: 1; transition: background .15s, transform .12s; }
.nav-btn:hover { background: var(--lp-surface-soft); }
.nav-btn:active { transform: scale(0.94); }
.nav-ind { font-size: 11px; font-weight: 600; color: var(--lp-text-faint); min-width: 36px; text-align: center; font-variant-numeric: tabular-nums; }

.z4-body { flex: 1; min-height: 0; display: flex; flex-direction: column; }

/* funnel — 퍼플 그라데이션 히어로 (원본 .z4-top 톤) */
.z4-body--hero {
  /* 카드 패딩(22px 24px) 밖으로 풀-블리드 + 헤더와의 간격(margin-bottom:18px) 복원 */
  margin: 0 -24px -22px;
}
.z4-hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
  justify-content: center;
  padding: 22px 24px 24px;
  background: var(--lp-hero-gradient);
  color: var(--lp-hero-text);
  position: relative;
  overflow: hidden;
}
.z4-hero::before {
  content: '';
  position: absolute;
  top: -50px; right: -60px;
  width: 220px; height: 220px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .08);
  pointer-events: none;
}
.z4-hero-top { display: flex; align-items: center; gap: 16px; position: relative; }
.z4-gauge { position: relative; flex-shrink: 0; width: 124px; height: 74px; }
.z4-gauge-arc { transition: stroke-dasharray .6s cubic-bezier(.4,0,.2,1); }
.z4-gauge-pill { position: absolute; left: 50%; top: 66%; transform: translate(-50%, -50%); display: flex; flex-direction: column; align-items: center; line-height: 1.1; }
.z4-gauge-v { font-size: 19px; font-weight: 800; color: #fff; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; }
.z4-gauge-lbl { font-size: 10px; font-weight: 500; color: rgba(255,255,255,.8); margin-top: 1px; }
.z4-hero-meta { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.z4-hero-total { display: inline-flex; align-items: baseline; gap: 2px; font-size: 30px; font-weight: 800; letter-spacing: -0.02em; color: #fff; font-variant-numeric: tabular-nums; line-height: 1; }
.z4-hero-total-unit { font-size: 16px; font-weight: 700; color: rgba(255, 255, 255, .82); margin-left: 2px; }
.z4-hero-cap { font-size: 11.5px; font-weight: 500; color: rgba(255, 255, 255, .78); }
.z4-mini-bars { display: flex; align-items: flex-end; gap: 4px; height: 28px; margin-top: 4px; }
.z4-mini-bars span { width: 5px; border-radius: 999px; background: rgba(255,255,255,.45); flex-shrink: 0; }

.z4-funnel { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px; position: relative; }
.z4-stage { display: flex; align-items: center; gap: 12px; }
.z4-stage-lbl { width: 44px; font-size: 13px; font-weight: 700; color: #fff; flex-shrink: 0; }
.z4-stage-track { flex: 1; height: 26px; background: rgba(255, 255, 255, .18); border-radius: 8px; overflow: hidden; }
.z4-stage-fill { display: block; height: 100%; border-radius: 8px; transition: width .6s cubic-bezier(.4,0,.2,1); }
.z4-stage-count { width: 28px; text-align: right; font-size: 15px; font-weight: 800; color: #fff; flex-shrink: 0; font-variant-numeric: tabular-nums; }

/* revenue chart */
.z4-chart-wrap { flex: 1; min-height: 0; }
.z4-chart-wrap :deep(.apexcharts-canvas) { margin: 0 auto; }

.z4-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 24px; text-align: center; }

.page-slide-enter-active, .page-slide-leave-active { transition: opacity .25s ease, transform .25s ease; }
.page-slide-enter-from { opacity: 0; transform: translateX(16px); }
.page-slide-leave-to { opacity: 0; transform: translateX(-16px); }
</style>
