<template>
  <div class="kpi-tab">
    <!-- 상단 툴바 (editable일 때만) -->
    <div v-if="store.editable" class="kpi-tab__toolbar">
      <button class="btn btn--outline" @click="openFramework">프레임워크 불러오기</button>
      <button class="btn btn--primary" @click="showAddModal = true">지표 추가</button>
    </div>

    <!-- 요약 카드 -->
    <KpiSummaryCards :summary="store.summary" />

    <!-- 빈 상태 or 테이블 -->
    <div v-if="store.loading" class="kpi-tab__loading">불러오는 중…</div>
    <template v-else>
      <KpiEmptyState
        v-if="store.items.length === 0"
        :editable="store.editable"
        @add="showAddModal = true"
        @framework="openFramework"
      />
      <KpiTable
        v-else
        :items="store.items"
        :editable="store.editable"
        @editActual="openActual"
        @editMeta="openMeta"
        @delete="deleteKpi"
      />
    </template>

    <!-- 성과 분석 메모 -->
    <KpiAnalysisBox
      :model-value="store.analysisDraft"
      :editable="store.editable"
      @save="handleAnalysisSave"
    />

    <!-- 모달들 -->
    <KpiRowFormModal v-if="showAddModal" :edit-target="null" @close="showAddModal = false" @submit="handleCreate" />
    <KpiRowFormModal v-if="editMetaTarget" :edit-target="editMetaTarget" @close="editMetaTarget = null" @submit="handleMeta" />
    <KpiActualInputModal v-if="actualTarget" :kpi="actualTarget" @close="actualTarget = null" @submit="handleActual" />
    <KpiFrameworkPickerModal v-if="showFramework" :frameworks="store.frameworks" @close="showFramework = false" @apply="handleFramework" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useCampaignKpiStore } from '@/stores/campaignKpi.js'
import KpiSummaryCards         from './KpiSummaryCards.vue'
import KpiEmptyState           from './KpiEmptyState.vue'
import KpiTable                from './KpiTable.vue'
import KpiAnalysisBox          from './KpiAnalysisBox.vue'
import KpiRowFormModal         from './KpiRowFormModal.vue'
import KpiActualInputModal     from './KpiActualInputModal.vue'
import KpiFrameworkPickerModal from './KpiFrameworkPickerModal.vue'

const props = defineProps({ campaignId: { type: [Number, String], required: true } })
const store = useCampaignKpiStore()

const showAddModal   = ref(false)
const editMetaTarget = ref(null)
const actualTarget   = ref(null)
const showFramework  = ref(false)

onMounted(() => store.fetch(props.campaignId))

async function openFramework() {
  await store.fetchFrameworks()
  showFramework.value = true
}

async function handleCreate(payload) {
  await store.create(props.campaignId, payload)
  showAddModal.value = false
}

function openMeta(kpi) { editMetaTarget.value = kpi }
async function handleMeta(payload) {
  await store.updateMeta(props.campaignId, editMetaTarget.value.idx, payload)
  editMetaTarget.value = null
}

function openActual(kpi) { actualTarget.value = kpi }
async function handleActual(payload) {
  await store.updateActual(props.campaignId, actualTarget.value.idx, payload)
  actualTarget.value = null
}

async function deleteKpi(kpiId) {
  if (!confirm('KPI를 삭제하시겠습니까?')) return
  await store.remove(props.campaignId, kpiId)
}

async function handleFramework(key) {
  await store.importFramework(props.campaignId, key)
  showFramework.value = false
}

async function handleAnalysisSave(draftValue) {
  store.setAnalysisDraft(draftValue)
  await store.saveAnalysis(props.campaignId)
}
</script>

<style scoped>
.kpi-tab {
  padding: 20px 0;
}

.kpi-tab__toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 16px;
}

.btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.btn--outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn--outline:hover {
  border-color: var(--color-primary-500);
  color: var(--color-primary-500);
}

.btn--primary {
  background: var(--color-primary-500);
  border: 1px solid var(--color-primary-500);
  color: #fff;
}

.btn--primary:hover {
  background: var(--color-primary-600);
  border-color: var(--color-primary-600);
}

.kpi-tab__loading {
  padding: 32px 0;
  text-align: center;
  color: var(--muted-text);
  font-size: 13px;
}
</style>
