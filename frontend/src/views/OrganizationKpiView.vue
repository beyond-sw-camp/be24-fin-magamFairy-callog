<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { useOrganizationKpiStore } from '@/stores/organizationKpi'
import KpiList from '@/components/kpi/KpiList.vue'
import KpiEditModal from '@/components/kpi/KpiEditModal.vue'

const authStore = useAuthStore()
const store = useOrganizationKpiStore()

const orgType = computed(() => {
  const fromOrg = authStore.user?.organization?.type
  if (fromOrg) return String(fromOrg).toUpperCase()
  const fromClaim = authStore.user?.orgType
  if (fromClaim) return String(fromClaim).toUpperCase()
  // dev/mock 환경 fallback — 등록 버튼이 항상 노출되도록 HQ로 default
  return 'HQ'
})
const orgId = computed(() =>
  authStore.user?.organization?.idx
  ?? authStore.user?.organizationId
  ?? authStore.user?.orgId
  ?? null
)

const isHqAdmin = computed(() => orgType.value === 'HQ')
const isAffiliateAdmin = computed(() => orgType.value === 'AFFILIATE')
const isExternalPartner = computed(() => orgType.value === 'EXTERNAL_PARTNER')

// HQ KPI 편집 가능: HQ 소속 + GM/MANAGER/ADMIN
const canEditHqKpi = computed(() =>
  isHqAdmin.value &&
  (authStore.isGeneralManager || authStore.isManager || authStore.isAdmin)
)

// 계열사 KPI 편집 가능: 계열사 소속 + GM/MANAGER/ADMIN
const canEditAffiliateKpi = computed(() =>
  isAffiliateAdmin.value &&
  (authStore.isGeneralManager || authStore.isManager || authStore.isAdmin)
)

// 자사 KPI 편집 가능: 외부파트너 소속 + GM/MANAGER/ADMIN
const canEditPartnerKpi = computed(() =>
  isExternalPartner.value &&
  (authStore.isGeneralManager || authStore.isManager || authStore.isAdmin)
)

/* ───── Status 탭 (활성 default) ───── */
const STATUS_TABS = [
  { key: 'ACTIVE', label: '활성' },
  { key: 'DRAFT', label: '초안' },
  { key: 'ARCHIVED', label: '보관' },
]
const activeStatus = ref('ACTIVE')   // ★ 첫 화면 항상 활성

/* ───── Period chip ───── */
function nowQuarter() {
  const now = new Date()
  return { year: now.getFullYear(), q: Math.ceil((now.getMonth() + 1) / 3) }
}

function quarterCode(year, q) {
  return `${year}-Q${q}`
}

function shiftQuarter(year, q, delta) {
  let qq = q + delta
  let yy = year
  while (qq < 1) { qq += 4; yy -= 1 }
  while (qq > 4) { qq -= 4; yy += 1 }
  return { year: yy, q: qq }
}

const PERIOD_CHIPS = computed(() => {
  const cur = nowQuarter()
  const prev = shiftQuarter(cur.year, cur.q, -1)
  const next = shiftQuarter(cur.year, cur.q, +1)
  return [
    { key: 'PREV', label: '지난 분기', code: quarterCode(prev.year, prev.q) },
    { key: 'CURRENT', label: '이번 분기', code: quarterCode(cur.year, cur.q) },
    { key: 'NEXT', label: '다음 분기', code: quarterCode(next.year, next.q) },
    { key: 'YEAR', label: `${cur.year}년 전체`, code: `${cur.year}-FY` },
    { key: 'ALL', label: '모두', code: '' },
  ]
})
const activePeriodChip = ref('CURRENT')   // ★ default 이번 분기
const customPeriodCode = ref('')          // [+] 직접 지정 시 사용
const customPickerOpen = ref(false)

const activePeriodCode = computed(() => {
  if (activePeriodChip.value === 'CUSTOM') return customPeriodCode.value
  const chip = PERIOD_CHIPS.value.find((c) => c.key === activePeriodChip.value)
  return chip?.code ?? ''
})

const customOptions = computed(() => {
  // 직접 지정 popover에 보여줄 후보
  const cur = nowQuarter()
  const set = new Set()
  for (let d = -2; d <= 4; d += 1) {
    const sh = shiftQuarter(cur.year, cur.q, d)
    set.add(quarterCode(sh.year, sh.q))
  }
  set.add(`${cur.year}-FY`)
  set.add(`${cur.year + 1}-FY`)
  // 데이터에서 발견된 코드도 합침
  ;(store.items ?? []).forEach((k) => k.periodCode && set.add(k.periodCode))
  return Array.from(set).sort()
})

/* ───── Editor 상태 ───── */
const editorOpen = ref(false)
const editorMode = ref('create')
const editorTarget = ref(null)
const editorOwnerOrgType = ref('HQ')

onMounted(async () => {
  await applyFilters()
})

async function applyFilters() {
  store.setFilter({
    period: activePeriodCode.value || '',
    status: null,   // 목록은 전체 받고 클라에서 status 탭 필터
  })
  await store.fetch()
}

function pickPeriodChip(key) {
  customPickerOpen.value = false
  if (key === 'CUSTOM') {
    customPickerOpen.value = true
    return
  }
  activePeriodChip.value = key
  applyFilters()
}

function pickCustomPeriod(code) {
  customPeriodCode.value = code
  activePeriodChip.value = 'CUSTOM'
  customPickerOpen.value = false
  applyFilters()
}

/* status별 카운트 (전체 items 기준) */
const statusCounts = computed(() => {
  const counts = { ACTIVE: 0, DRAFT: 0, ARCHIVED: 0 }
  ;(store.items ?? []).forEach((k) => {
    if (counts[k.status] !== undefined) counts[k.status] += 1
  })
  return counts
})

/* 표시 items: status 필터 적용 */
const filteredItems = computed(() =>
  (store.items ?? []).filter((k) => k.status === activeStatus.value),
)

/**
 * 본사 KPI 판별:
 * 1) ownerOrgType === 'HQ'  ← backend DTO 정상
 * 2) DTO 미반영 환경 fallback: caller 본인 조직이 HQ면 ownerOrgId가 자기 조직과 같으면 HQ
 * 3) 그래도 모르면 kind === 'STRATEGIC' 기준 (HQ default kind는 STRATEGIC)
 */
function isHqOwned(k) {
  if (k.ownerOrgType === 'HQ') return true
  if (k.ownerOrgType === 'AFFILIATE' || k.ownerOrgType === 'EXTERNAL_PARTNER') return false
  if (isHqAdmin.value && orgId.value && k.ownerOrgId === orgId.value) return true
  return k.kind === 'STRATEGIC'
}
const visibleHqItems = computed(() => filteredItems.value.filter(isHqOwned))
/**
 * 계열사 섹션 가시성:
 * - HQ: 모든 계열사 KPI 노출 (전사 monitoring)
 * - AFFILIATE/EXTERNAL: 자기 조직 KPI만
 */
const visibleOrgItems = computed(() => {
  const nonHq = filteredItems.value.filter((k) => !isHqOwned(k))
  if (isHqAdmin.value) return nonHq
  if (!orgId.value) return nonHq   // mock fallback
  return nonHq.filter((k) => k.ownerOrgId === orgId.value)
})

/* CRUD 핸들러 */
function openCreate(targetOrgType) {
  editorMode.value = 'create'
  editorTarget.value = null
  editorOwnerOrgType.value = targetOrgType
  editorOpen.value = true
}

function openEdit(kpi) {
  editorMode.value = 'edit'
  editorTarget.value = { ...kpi }
  editorOwnerOrgType.value = kpi.ownerOrgType ?? 'HQ'
  editorOpen.value = true
}

const submitError = ref('')

async function handleSubmit(payload) {
  submitError.value = ''
  try {
    if (editorMode.value === 'edit' && editorTarget.value?.idx) {
      await store.update(editorTarget.value.idx, payload)
    } else {
      await store.create(payload)
      // 새 KPI는 DRAFT로 만들어지므로 사용자가 바로 볼 수 있도록 초안 탭으로 전환
      activeStatus.value = 'DRAFT'
    }
    editorOpen.value = false
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'KPI 저장에 실패했습니다.'
    submitError.value = msg
    console.error('[OrganizationKpi] save 실패:', err)
    window.alert(`KPI 저장 실패\n\n${msg}\n\n브라우저 DevTools Network 탭에서 /organization-kpis 응답을 확인하세요.`)
  }
}

async function handleArchive(kpi) {
  if (!window.confirm(`'${kpi.name}' KPI를 보관하시겠습니까?`)) return
  try {
    await store.updateStatus(kpi.idx, 'ARCHIVED')
  } catch (err) {
    window.alert('상태 변경 실패: ' + (err?.response?.data?.message || err?.message || err))
  }
}
async function handleActivate(kpi) {
  try {
    await store.updateStatus(kpi.idx, 'ACTIVE')
  } catch (err) {
    window.alert('활성화 실패: ' + (err?.response?.data?.message || err?.message || err))
  }
}

const periodLabel = computed(() => {
  if (activePeriodChip.value === 'CUSTOM') return customPeriodCode.value || '직접 지정'
  return PERIOD_CHIPS.value.find((c) => c.key === activePeriodChip.value)?.label ?? '전체'
})
</script>

<template>
  <div class="org-kpi-root">
    <!-- 페이지 헤더 -->
    <header class="page-bar">
      <div class="page-bar__copy">
        <span class="page-bar__eyebrow">분기 KPI 관리</span>
        <h1 class="page-bar__title">분기 KPI · {{ periodLabel }}</h1>
        <p class="page-bar__hint">
          본사 전략 → 계열사 전술 → 캠페인 운영으로 cascade 되는 3-tier KPI를 관리합니다.
          <span v-if="store.usingMock" class="page-bar__mock">[mock 모드]</span>
        </p>
      </div>

      <div class="page-bar__actions">
        <button
          v-if="canEditHqKpi"
          type="button"
          class="btn btn--primary"
          @click="openCreate('HQ')"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span>새 본사 KPI</span>
        </button>
        <button
          v-if="isHqAdmin || canEditAffiliateKpi"
          type="button"
          class="btn btn--secondary"
          @click="openCreate('AFFILIATE')"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span>{{ isHqAdmin ? '계열사 KPI' : '우리 조직 KPI' }}</span>
        </button>
        <button
          v-if="canEditPartnerKpi"
          type="button"
          class="btn btn--secondary"
          @click="openCreate('EXTERNAL_PARTNER')"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span>자사 KPI 추가</span>
        </button>
      </div>
    </header>

    <!-- ─── Status 탭 (메인 분류) ─── -->
    <nav class="status-tabs" role="tablist" aria-label="KPI 상태 탭">
      <button
        v-for="t in STATUS_TABS"
        :key="t.key"
        type="button"
        role="tab"
        :aria-selected="activeStatus === t.key"
        class="status-tab"
        :class="{ 'is-active': activeStatus === t.key }"
        @click="activeStatus = t.key"
      >
        <span>{{ t.label }}</span>
        <span class="status-tab__count">{{ statusCounts[t.key] ?? 0 }}</span>
      </button>
    </nav>

    <!-- ─── Period chip (보조 필터) ─── -->
    <div class="period-row">
      <div class="period-chips">
        <button
          v-for="c in PERIOD_CHIPS"
          :key="c.key"
          type="button"
          class="chip"
          :class="{ 'is-active': activePeriodChip === c.key }"
          @click="pickPeriodChip(c.key)"
        >{{ c.label }}</button>
        <div class="custom-wrap">
          <button
            type="button"
            class="chip chip--add"
            :class="{ 'is-active': activePeriodChip === 'CUSTOM' }"
            @click="customPickerOpen = !customPickerOpen"
            aria-haspopup="listbox"
            :aria-expanded="customPickerOpen"
          >
            <span v-if="activePeriodChip === 'CUSTOM'">{{ customPeriodCode || '직접 지정' }}</span>
            <span v-else>+ 직접 지정</span>
          </button>
          <div v-if="customPickerOpen" class="custom-pop" role="listbox">
            <button
              v-for="code in customOptions"
              :key="code"
              type="button"
              class="custom-pop__item"
              :class="{ 'is-active': customPeriodCode === code }"
              @click="pickCustomPeriod(code)"
            >{{ code }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── KPI 섹션 (status·period 필터 적용된 items 표시) ─── -->

    <!-- 본사·계열사 섹션: 외부파트너에게는 숨김 -->
    <template v-if="!isExternalPartner">
      <section class="kpi-section">
        <div class="kpi-section__head">
          <div>
            <span class="kpi-section__pill kpi-section__pill--strategic">Strategic</span>
            <h2 class="kpi-section__title">본사 KPI</h2>
          </div>
          <span class="kpi-section__count">{{ visibleHqItems.length }}개</span>
        </div>
        <KpiList
          :items="visibleHqItems"
          :editable="canEditHqKpi"
          empty-text="이 조건에 해당하는 본사 KPI가 없습니다."
          @edit="openEdit"
          @archive="handleArchive"
          @activate="handleActivate"
        />
      </section>

      <section class="kpi-section">
        <div class="kpi-section__head">
          <div>
            <span class="kpi-section__pill kpi-section__pill--tactical">Tactical</span>
            <h2 class="kpi-section__title">{{ isHqAdmin ? '계열사 KPI' : '우리 조직 KPI' }}</h2>
          </div>
          <span class="kpi-section__count">{{ visibleOrgItems.length }}개</span>
        </div>
        <KpiList
          :items="visibleOrgItems"
          :editable="canEditAffiliateKpi"
          :empty-text="isHqAdmin ? '이 조건에 해당하는 계열사 KPI가 없습니다.' : '이 조건에 해당하는 우리 조직 KPI가 없습니다.'"
          @edit="openEdit"
          @archive="handleArchive"
          @activate="handleActivate"
        />
      </section>
    </template>

    <!-- 자사 KPI 섹션: 외부파트너 전용 -->
    <section v-if="isExternalPartner" class="kpi-section">
      <div class="kpi-section__head">
        <div>
          <span class="kpi-section__pill kpi-section__pill--tactical">Own</span>
          <h2 class="kpi-section__title">자사 KPI</h2>
        </div>
        <span class="kpi-section__count">{{ visibleOrgItems.length }}개</span>
      </div>
      <KpiList
        :items="visibleOrgItems"
        :editable="canEditPartnerKpi"
        empty-text="이 조건에 해당하는 자사 KPI가 없습니다."
        @edit="openEdit"
        @archive="handleArchive"
        @activate="handleActivate"
      />
    </section>

    <KpiEditModal
      v-if="editorOpen"
      :mode="editorMode"
      :initial-values="editorTarget"
      :default-owner-org-id="orgId"
      :default-owner-org-type="editorOwnerOrgType"
      @close="editorOpen = false"
      @submit="handleSubmit"
    />
  </div>
</template>

<style scoped>
.org-kpi-root {
  margin: calc(-1 * var(--density-page-padding, 24px));
  padding: 28px 32px 80px;
  min-height: calc(100% + 2 * var(--density-page-padding, 24px));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--color-primary-500) 8%, transparent), transparent 35%),
    linear-gradient(180deg, var(--color-primary-50) 0%, var(--surface-page) 70%);
  display: flex;
  flex-direction: column;
  gap: 20px;
  font-family: 'Pretendard Variable', 'Pretendard', 'Noto Sans KR', sans-serif;
  font-feature-settings: 'tnum' 1;
  color: var(--text-primary);
}

:root[data-theme='dark'] .org-kpi-root {
  background:
    radial-gradient(circle at top right, rgba(168, 85, 247, 0.18), transparent 40%),
    linear-gradient(180deg, #10141d 0%, #181024 100%);
}

/* ───── 페이지 헤더 ───── */
.page-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}
.page-bar__copy { display: flex; flex-direction: column; gap: 4px; }
.page-bar__eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-primary-700);
  text-transform: uppercase;
}
.page-bar__title {
  font-size: 26px;
  font-weight: 800;
  margin: 0;
  letter-spacing: -0.018em;
  color: var(--text-primary);
}
.page-bar__hint {
  margin: 0;
  font-size: 12px;
  color: var(--muted-text);
}
.page-bar__mock {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 700;
  border-radius: 4px;
  background: #fef3c7;
  color: #b45309;
}

.page-bar__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}
.btn--primary {
  background: var(--color-primary-500);
  color: #fff;
  border: 1px solid var(--color-primary-500);
}
.btn--primary:hover { background: var(--color-primary-600); border-color: var(--color-primary-600); }
.btn--secondary {
  background: var(--panel-color);
  color: var(--color-primary-700);
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 30%, var(--border-color));
}
.btn--secondary:hover { background: var(--color-primary-50); }

/* ───── Status 탭 (Q1 ③ 메인) ───── */
.status-tabs {
  display: inline-flex;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 4px;
  gap: 2px;
  width: fit-content;
}
.status-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 999px;
  background: transparent;
  border: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  cursor: pointer;
  font-family: inherit;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.status-tab:hover { color: var(--text-primary); }
.status-tab.is-active {
  background: var(--color-primary-500);
  color: #fff;
}
.status-tab__count {
  display: inline-flex;
  min-width: 22px;
  height: 18px;
  padding: 0 6px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.status-tab.is-active .status-tab__count {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

/* ───── Period chip (Q1 ① 보조) ───── */
.period-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.period-chips {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.chip {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  cursor: pointer;
  font-family: inherit;
  font-variant-numeric: tabular-nums;
  transition: all var(--transition-fast);
}
.chip:hover {
  border-color: color-mix(in srgb, var(--color-primary-500) 35%, var(--border-color));
  color: var(--color-primary-700);
}
.chip.is-active {
  background: var(--color-primary-100);
  border-color: var(--color-primary-300);
  color: var(--color-primary-700);
}
.chip--add {
  border-style: dashed;
}
.custom-wrap { position: relative; }
.custom-pop {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.1);
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 140px;
  z-index: 30;
}
.custom-pop__item {
  background: transparent;
  border: 0;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-align: left;
  border-radius: 6px;
  cursor: pointer;
  font-variant-numeric: tabular-nums;
  font-family: inherit;
}
.custom-pop__item:hover { background: var(--panel-muted); color: var(--text-primary); }
.custom-pop__item.is-active {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

/* ───── 섹션 ───── */
.kpi-section { display: flex; flex-direction: column; gap: 14px; }
.kpi-section__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}
.kpi-section__pill {
  display: inline-block;
  padding: 3px 10px;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  border-radius: 999px;
  margin-bottom: 6px;
}
.kpi-section__pill--strategic {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}
.kpi-section__pill--tactical {
  background: #dbeafe;
  color: #2563eb;
}
.kpi-section__title {
  font-size: 18px;
  font-weight: 800;
  margin: 0;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}
.kpi-section__count {
  font-size: 11px;
  color: var(--muted-text);
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}
</style>
