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
  return fromClaim ? String(fromClaim).toUpperCase() : ''
})
const orgId = computed(() => authStore.user?.organization?.idx ?? authStore.user?.organizationId ?? null)

const isHqAdmin = computed(() => orgType.value === 'HQ')
const isAffiliateAdmin = computed(() => ['AFFILIATE', 'EXTERNAL_PARTNER'].includes(orgType.value))

// 헤더 + 모달 상태
const periods = computed(() => {
  const base = ['2026-Q2', '2026-Q3', '2026-Q4', '2027-Q1', '2026-FY']
  const fromData = (store.items ?? []).map((k) => k.periodCode).filter(Boolean)
  return Array.from(new Set([...base, ...fromData])).sort()
})

const activePeriod = ref('')
const showArchived = ref(false)
const editorOpen = ref(false)
const editorMode = ref('create')
const editorTarget = ref(null)
const editorOwnerOrgType = ref('HQ')

onMounted(async () => {
  // 기본 기간: 현재 분기
  if (!activePeriod.value) {
    const now = new Date()
    const q = Math.ceil((now.getMonth() + 1) / 3)
    activePeriod.value = `${now.getFullYear()}-Q${q}`
  }
  store.setFilter({ period: activePeriod.value, status: showArchived.value ? null : 'ACTIVE' })
  await store.fetch()
})

async function applyFilters() {
  store.setFilter({
    period: activePeriod.value || '',
    status: showArchived.value ? null : 'ACTIVE',
  })
  await store.fetch()
}

// 보관 토글 시 ARCHIVED 필터 추가
const visibleHqItems = computed(() => filterByArchive(store.hqItems))
const visibleOrgItems = computed(() => filterByArchive(store.orgItems))

function filterByArchive(arr) {
  if (showArchived.value) return arr
  return (arr ?? []).filter((k) => k.status !== 'ARCHIVED')
}

const archivedItems = computed(() =>
  (store.items ?? []).filter((k) => k.status === 'ARCHIVED'),
)

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

async function handleSubmit(payload) {
  if (editorMode.value === 'edit' && editorTarget.value?.idx) {
    await store.update(editorTarget.value.idx, payload)
  } else {
    await store.create(payload)
  }
  editorOpen.value = false
}

async function handleArchive(kpi) {
  if (!window.confirm(`'${kpi.name}' KPI를 보관하시겠습니까?`)) return
  await store.updateStatus(kpi.idx, 'ARCHIVED')
}
async function handleActivate(kpi) {
  await store.updateStatus(kpi.idx, 'ACTIVE')
}

async function reload() {
  await applyFilters()
}
</script>

<template>
  <div class="org-kpi-root">
    <header class="page-bar">
      <div class="page-bar__copy">
        <span class="page-bar__eyebrow">분기 KPI 관리</span>
        <h1 class="page-bar__title">분기 KPI · {{ activePeriod || '전체' }}</h1>
        <p class="page-bar__hint">
          본사 전략 → 계열사 전술 → 캠페인 운영으로 cascade 되는 3-tier KPI를 관리합니다.
          <span v-if="store.usingMock" class="page-bar__mock">[mock 모드]</span>
        </p>
      </div>

      <div class="page-bar__actions">
        <label class="filter">
          <span>분기</span>
          <select v-model="activePeriod" @change="applyFilters">
            <option value="">전체</option>
            <option v-for="p in periods" :key="p" :value="p">{{ p }}</option>
          </select>
        </label>
        <label class="filter filter--toggle">
          <input v-model="showArchived" type="checkbox" @change="applyFilters" />
          <span>보관함 포함</span>
        </label>
        <button
          v-if="isHqAdmin"
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
          v-if="isHqAdmin || isAffiliateAdmin"
          type="button"
          class="btn btn--secondary"
          @click="openCreate(isHqAdmin ? 'AFFILIATE' : 'AFFILIATE')"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <span>{{ isHqAdmin ? '계열사 KPI' : '우리 조직 KPI' }}</span>
        </button>
      </div>
    </header>

    <section class="kpi-section">
      <div class="kpi-section__head">
        <div>
          <span class="kpi-section__pill kpi-section__pill--strategic">Strategic</span>
          <h2 class="kpi-section__title">본사 KPI</h2>
        </div>
        <span class="kpi-section__count">
          {{ visibleHqItems.length }}개 · 활성 {{ visibleHqItems.filter(k => k.status === 'ACTIVE').length }}
        </span>
      </div>
      <KpiList
        :items="visibleHqItems"
        :editable="isHqAdmin"
        empty-text="본사 KPI가 아직 없습니다."
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
        <span class="kpi-section__count">
          {{ visibleOrgItems.length }}개 · 활성 {{ visibleOrgItems.filter(k => k.status === 'ACTIVE').length }}
        </span>
      </div>
      <KpiList
        :items="visibleOrgItems"
        :editable="isAffiliateAdmin"
        :empty-text="isHqAdmin ? '계열사 KPI가 아직 없습니다.' : '우리 조직 KPI가 아직 없습니다.'"
        @edit="openEdit"
        @archive="handleArchive"
        @activate="handleActivate"
      />
    </section>

    <section v-if="archivedItems.length > 0" class="kpi-section kpi-section--archive">
      <button
        type="button"
        class="kpi-section__archive-toggle"
        @click="showArchived = !showArchived; reload()"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"
             :style="{ transform: showArchived ? 'rotate(180deg)' : 'none' }">
          <polyline points="6 9 12 15 18 9" />
        </svg>
        <span>보관(ARCHIVED) {{ archivedItems.length }}개 {{ showArchived ? '숨기기' : '펼치기' }}</span>
      </button>
      <KpiList
        v-if="showArchived"
        :items="archivedItems"
        :editable="false"
        empty-text="보관된 KPI가 없습니다."
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
  gap: 24px;
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

.filter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 4px 10px 4px 14px;
  height: 36px;
}
.filter > select {
  height: 28px;
  border: 0;
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: inherit;
  outline: none;
  cursor: pointer;
}
.filter--toggle {
  cursor: pointer;
  padding: 0 12px;
  user-select: none;
}
.filter--toggle input[type='checkbox'] { accent-color: var(--color-primary-500); }

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
.btn--secondary:hover {
  background: var(--color-primary-50);
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

/* ───── 보관함 토글 ───── */
.kpi-section--archive { gap: 8px; }
.kpi-section__archive-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  cursor: pointer;
  width: fit-content;
}
.kpi-section__archive-toggle:hover {
  border-color: color-mix(in srgb, var(--color-primary-500) 30%, var(--border-color));
  color: var(--color-primary-700);
}
.kpi-section__archive-toggle svg { transition: transform 0.2s ease; }
</style>
