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
      items.value = []
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
      templates.value = list
    } catch (error) {
      lastError.value = error?.message ?? String(error)
      templates.value = []
    }
  }

  async function fetchParentCandidates(orgId) {
    try {
      const data = await orgKpiApi.ListParentKpiCandidates(orgId)
      parentCandidates.value = Array.isArray(data) ? data : data?.items ?? []
    } catch (error) {
      lastError.value = error?.message ?? String(error)
      parentCandidates.value = []
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

