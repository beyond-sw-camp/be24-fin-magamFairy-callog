import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as orgKpiApi from '@/api/organizationKpis'
import * as templateApi from '@/api/kpiTemplates'

export const useOrganizationKpiStore = defineStore('organizationKpi', () => {
  const items = ref([])
  const templates = ref([])
  const parentCandidates = ref([])
  const loading = ref(false)
  const lastError = ref(null)

  // 필터 상태
  const filterPeriod = ref('')
  const filterOwnerOrgId = ref(null)
  const filterStatus = ref('ACTIVE')

  const hqItems = computed(() => items.value.filter((k) => k.ownerOrgType === 'HQ'))
  const orgItems = computed(() => items.value.filter((k) => k.ownerOrgType !== 'HQ'))

  async function fetch(params = {}) {
    loading.value = true
    lastError.value = null
    try {
      const merged = {
        ...(filterPeriod.value ? { period: filterPeriod.value } : {}),
        ...(filterOwnerOrgId.value ? { owner: filterOwnerOrgId.value } : {}),
        ...(filterStatus.value ? { status: filterStatus.value } : {}),
        ...params,
      }
      const data = await orgKpiApi.ListOrganizationKpis(merged)
      items.value = Array.isArray(data) ? data : data?.items ?? []
    } catch (error) {
      lastError.value = error?.message ?? String(error)
      console.warn('[mock fallback] OrganizationKpi 목록 fetch 실패. mock 사용', error)
      items.value = buildMockKpis()
    } finally {
      loading.value = false
    }
  }

  async function create(payload) {
    try {
      const created = await orgKpiApi.CreateOrganizationKpi(payload)
      await fetch()
      return created
    } catch (error) {
      console.warn('[mock fallback] OrganizationKpi 생성 실패. local push', error)
      const fakeId = Date.now()
      const local = { idx: fakeId, ...payload }
      items.value.unshift(local)
      return local
    }
  }

  async function update(id, payload) {
    try {
      const updated = await orgKpiApi.UpdateOrganizationKpi(id, payload)
      await fetch()
      return updated
    } catch (error) {
      console.warn('[mock fallback] OrganizationKpi 수정 실패. local merge', error)
      const idx = items.value.findIndex((k) => k.idx === id)
      if (idx >= 0) items.value[idx] = { ...items.value[idx], ...payload }
      return items.value[idx]
    }
  }

  async function updateStatus(id, status) {
    try {
      const result = await orgKpiApi.UpdateOrganizationKpiStatus(id, status)
      await fetch()
      return result
    } catch (error) {
      console.warn('[mock fallback] OrganizationKpi status 변경 실패', error)
      const idx = items.value.findIndex((k) => k.idx === id)
      if (idx >= 0) items.value[idx] = { ...items.value[idx], status }
    }
  }

  async function fetchTemplates(params = {}) {
    try {
      const data = await templateApi.ListKpiTemplates(params)
      templates.value = Array.isArray(data) ? data : data?.items ?? []
    } catch (error) {
      console.warn('[mock fallback] KpiTemplate 목록 fetch 실패. mock 사용', error)
      templates.value = buildMockTemplates()
    }
  }

  async function fetchParentCandidates(orgId) {
    try {
      const data = await orgKpiApi.ListParentKpiCandidates(orgId)
      parentCandidates.value = Array.isArray(data) ? data : data?.items ?? []
    } catch (error) {
      console.warn('[mock fallback] parent KPI 후보 fetch 실패', error)
      parentCandidates.value = items.value.filter(
        (k) => k.ownerOrgType === 'HQ' && k.status === 'ACTIVE',
      )
    }
  }

  function setFilter({ period, ownerOrgId, status } = {}) {
    if (period !== undefined) filterPeriod.value = period
    if (ownerOrgId !== undefined) filterOwnerOrgId.value = ownerOrgId
    if (status !== undefined) filterStatus.value = status
  }

  return {
    items,
    templates,
    parentCandidates,
    loading,
    lastError,
    filterPeriod,
    filterOwnerOrgId,
    filterStatus,
    hqItems,
    orgItems,
    fetch,
    create,
    update,
    updateStatus,
    fetchTemplates,
    fetchParentCandidates,
    setFilter,
  }
})

/* ───── mock fallback ───── */
function buildMockKpis() {
  return [
    {
      idx: 9001,
      name: '신규 협력사',
      ownerOrgId: 1,
      ownerOrgName: '한화 본사',
      ownerOrgType: 'HQ',
      parentKpiId: null,
      parentKpiName: null,
      contributionToParent: null,
      periodType: 'QUARTERLY',
      periodCode: '2026-Q2',
      periodStart: '2026-04-01',
      periodEnd: '2026-06-30',
      targetValue: 20,
      actualValue: 12,
      progressPct: 60,
      unit: '곳',
      category: 'OTHER',
      esgCategory: null,
      kind: 'STRATEGIC',
      status: 'ACTIVE',
      achievabilityNote: 'Q1 +6, Q2 +12 도전적',
    },
    {
      idx: 9002,
      name: '캠페인 런칭',
      ownerOrgId: 1,
      ownerOrgName: '한화 본사',
      ownerOrgType: 'HQ',
      periodType: 'QUARTERLY',
      periodCode: '2026-Q2',
      targetValue: 12,
      actualValue: 8,
      progressPct: 67,
      unit: '건',
      category: 'BRAND',
      kind: 'STRATEGIC',
      status: 'ACTIVE',
    },
    {
      idx: 9003,
      name: 'KPI 달성률',
      ownerOrgId: 1,
      ownerOrgName: '한화 본사',
      ownerOrgType: 'HQ',
      periodType: 'QUARTERLY',
      periodCode: '2026-Q2',
      targetValue: 85,
      actualValue: 73,
      progressPct: 86,
      unit: '%',
      category: 'CONVERSION',
      kind: 'STRATEGIC',
      status: 'ACTIVE',
    },
    {
      idx: 9101,
      name: '한화호텔 신규 제휴',
      ownerOrgId: 11,
      ownerOrgName: '한화호텔앤드리조트',
      ownerOrgType: 'AFFILIATE',
      parentKpiId: 9001,
      parentKpiName: '본사 신규 협력사',
      contributionToParent: 4,
      periodType: 'QUARTERLY',
      periodCode: '2026-Q2',
      targetValue: 4,
      actualValue: 3,
      progressPct: 75,
      unit: '곳',
      category: 'OTHER',
      kind: 'TACTICAL',
      status: 'ACTIVE',
    },
  ]
}

function buildMockTemplates() {
  return [
    { idx: 1, name: '신규 제휴사 (분기)', defaultUnit: '곳', defaultCategory: 'OTHER', scope: 'GLOBAL', usageCount: 12 },
    { idx: 2, name: '캠페인 런칭 (분기)', defaultUnit: '건', defaultCategory: 'BRAND', scope: 'GLOBAL', usageCount: 8 },
    { idx: 3, name: '브랜드 인지도 (연)', defaultUnit: '%', defaultCategory: 'BRAND', scope: 'GLOBAL', usageCount: 5 },
    { idx: 4, name: '매출 ROAS (분기)', defaultUnit: '%', defaultCategory: 'REVENUE', scope: 'GLOBAL', usageCount: 9 },
  ]
}
