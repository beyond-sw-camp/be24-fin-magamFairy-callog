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
    // 쓰기 작업은 mock fallback 금지 — 사용자가 실패를 명확히 인지해야 함
    const created = await orgKpiApi.CreateOrganizationKpi(payload)
    await fetch()
    return created
  }

  async function update(id, payload) {
    const updated = await orgKpiApi.UpdateOrganizationKpi(id, payload)
    await fetch()
    return updated
  }

  async function updateStatus(id, status) {
    const result = await orgKpiApi.UpdateOrganizationKpiStatus(id, status)
    await fetch()
    return result
  }

  async function fetchTemplates(params = {}) {
    try {
      const data = await templateApi.ListKpiTemplates(params)
      const list = Array.isArray(data) ? data : data?.items ?? []
      // 백엔드가 비어있을 때도 사용자 경험 위해 mock 보여줌
      templates.value = list.length > 0 ? list : buildMockTemplates()
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
  // 디지털 마케팅 5축 (노출/참여/전환/매출/브랜드) + 운영·ESG 기준 표준 템플릿.
  // backend KpiTemplateSeeder 와 동일한 catalog.
  return [
    // 노출
    { idx: 1, name: '순도달 (Reach) 500만 UU', defaultUnit: 'UU', defaultCategory: 'IMPRESSION', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 2, name: 'SOV (Share of Voice) 25%', defaultUnit: '%', defaultCategory: 'IMPRESSION', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 3, name: '광고 노출 1,000만 회', defaultUnit: '회', defaultCategory: 'IMPRESSION', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    // 참여
    { idx: 4, name: 'CTR 1.5% 유지', defaultUnit: '%', defaultCategory: 'ENGAGEMENT', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 5, name: '영상 완주율 (VTR) 50%', defaultUnit: '%', defaultCategory: 'ENGAGEMENT', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 6, name: 'SNS 팔로워 순증 1만 명', defaultUnit: '명', defaultCategory: 'ENGAGEMENT', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 7, name: 'UGC 생성 1,000건', defaultUnit: '건', defaultCategory: 'ENGAGEMENT', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    // 전환
    { idx: 8, name: 'CVR (전환율) 3%', defaultUnit: '%', defaultCategory: 'CONVERSION', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 9, name: '랜딩페이지 방문 10만 세션', defaultUnit: 'Sessions', defaultCategory: 'CONVERSION', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 10, name: '신규 회원가입 5,000명', defaultUnit: '명', defaultCategory: 'CONVERSION', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    // 매출
    { idx: 11, name: 'ROAS 400%', defaultUnit: '%', defaultCategory: 'REVENUE', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 12, name: 'CPA 25,000원 이하', defaultUnit: '원', defaultCategory: 'REVENUE', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 13, name: 'LTV:CAC 3배', defaultUnit: '배수', defaultCategory: 'REVENUE', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    // 브랜드
    { idx: 14, name: '브랜드 인지도 +5%p', defaultUnit: '%p', defaultCategory: 'BRAND', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 15, name: 'Branded Search +30%', defaultUnit: '%', defaultCategory: 'BRAND', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 16, name: '긍정 Sentiment 비율 70%', defaultUnit: '%', defaultCategory: 'BRAND', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 17, name: 'NPS 50점 이상', defaultUnit: '점', defaultCategory: 'BRAND', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    // 운영·기타
    { idx: 18, name: '신규 협력사 25곳 확보', defaultUnit: '곳', defaultCategory: 'OTHER', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 19, name: '캠페인 12건 런칭', defaultUnit: '건', defaultCategory: 'OTHER', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 20, name: '자산 LIVE 100건 유지', defaultUnit: '건', defaultCategory: 'OTHER', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    { idx: 21, name: '검수 패스율 90%', defaultUnit: '%', defaultCategory: 'OTHER', defaultKind: 'TACTICAL', scope: 'GLOBAL', usageCount: 0 },
    // ESG
    { idx: 22, name: '탄소 배출 10% 절감', defaultUnit: '%', defaultCategory: 'OTHER', defaultEsgCategory: 'ENVIRONMENTAL', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 23, name: '협력사 ESG 평가 80점 이상', defaultUnit: '점', defaultCategory: 'OTHER', defaultEsgCategory: 'GOVERNANCE', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
    { idx: 24, name: '사회 공헌 캠페인 4건', defaultUnit: '건', defaultCategory: 'OTHER', defaultEsgCategory: 'SOCIAL', defaultKind: 'STRATEGIC', scope: 'GLOBAL', usageCount: 0 },
  ]
}
