import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as kpiApi from '@/api/campaignKpis/index.js'

export const useCampaignKpiStore = defineStore('campaignKpi', () => {
  const items = ref([])
  const summary = ref(null)
  const kpiAnalysis = ref('')
  const analysisDraft = ref('')
  const editable = ref(false)
  const loading = ref(false)
  const frameworks = ref([])

  async function fetch(campaignId) {
    loading.value = true
    try {
      const res = await kpiApi.getKpis(campaignId)
      items.value = res.items
      summary.value = res.summary
      kpiAnalysis.value = res.kpiAnalysis ?? ''
      analysisDraft.value = res.kpiAnalysis ?? ''
      editable.value = res.editable
    } finally {
      loading.value = false
    }
  }

  async function create(campaignId, payload) {
    const item = await kpiApi.createKpi(campaignId, payload)
    items.value.push(item)
    await fetch(campaignId)
  }

  async function updateMeta(campaignId, kpiId, payload) {
    await kpiApi.updateKpiMeta(campaignId, kpiId, payload)
    await fetch(campaignId)
  }

  async function updateActual(campaignId, kpiId, payload) {
    await kpiApi.updateKpiActual(campaignId, kpiId, payload)
    await fetch(campaignId)
  }

  async function remove(campaignId, kpiId) {
    await kpiApi.removeKpi(campaignId, kpiId)
    items.value = items.value.filter(i => i.idx !== kpiId)
    await fetch(campaignId)
  }

  async function saveAnalysis(campaignId) {
    await kpiApi.saveAnalysis(campaignId, analysisDraft.value)
    kpiAnalysis.value = analysisDraft.value
  }

  function setAnalysisDraft(value) {
    analysisDraft.value = value
  }

  async function fetchFrameworks() {
    if (frameworks.value.length > 0) return
    frameworks.value = await kpiApi.getFrameworks()
  }

  async function importFramework(campaignId, frameworkKey) {
    await kpiApi.importFramework(campaignId, frameworkKey)
    await fetch(campaignId)
  }

  return {
    items, summary, kpiAnalysis, analysisDraft, editable, loading, frameworks,
    fetch, create, updateMeta, updateActual, remove, saveAnalysis,
    fetchFrameworks, importFramework, setAnalysisDraft,
  }
})
