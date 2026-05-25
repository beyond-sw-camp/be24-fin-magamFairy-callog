<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { GetAdCheckJobDetail, ListAdCheckJobs } from '@/api/adcheck/index.js'

const props = defineProps({
  campaignId: {
    type: [String, Number],
    required: true,
  },
})

const route = useRoute()
const records = ref([])
const loading = ref(false)
const errorMessage = ref('')
const detailLoadingJobId = ref('')
const detailErrorMessage = ref('')
const detailsByJobId = ref({})
const activeFilter = ref('all')
const searchQuery = ref('')
const selectedRecordId = ref(null)

const filterOptions = [
  { id: 'all', label: '전체' },
  { id: 'completed', label: '완료' },
  { id: 'review', label: '확인 필요' },
  { id: 'failed', label: '실패' },
  { id: 'active', label: '진행중' },
]

function normalizeStatus(status) {
  const value = String(status ?? '').trim().toUpperCase()
  if (['QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED'].includes(value)) return value
  return 'QUEUED'
}

function normalizeResultStatus(status) {
  const value = String(status ?? '').trim().toLowerCase()
  if (['pass', 'warning', 'violation', 'failed'].includes(value)) return value
  return ''
}

function recordKey(record) {
  return String(record?.jobId ?? record?.mongoDocumentId ?? record?.fileName ?? '')
}

function normalizeText(value) {
  return String(value ?? '').replace(/\s+/g, ' ').trim()
}

function statusLabel(status) {
  const normalized = normalizeStatus(status)
  if (normalized === 'QUEUED') return '대기 중'
  if (normalized === 'RUNNING') return '처리 중'
  if (normalized === 'SUCCEEDED') return '완료'
  if (normalized === 'FAILED') return '실패'
  if (normalized === 'CANCELED') return '취소'
  return '대기 중'
}

function resultStatusLabel(status) {
  const normalized = normalizeResultStatus(status)
  if (normalized === 'pass') return '검수 완료'
  if (normalized === 'warning') return '확인 필요'
  if (normalized === 'violation') return '위반 의심'
  if (normalized === 'failed') return '검수 실패'
  return '결과 대기'
}

function riskLevelLabel(level) {
  const normalized = String(level ?? '').toUpperCase()
  if (normalized === 'LOW') return '낮음'
  if (normalized === 'MEDIUM') return '중간'
  if (normalized === 'HIGH') return '높음'
  if (normalized === 'NORMAL') return '보통'
  return '미정'
}

function statusTone(record) {
  const normalizedStatus = normalizeStatus(record?.normalizedStatus ?? record?.status)
  const normalizedResult = normalizeResultStatus(record?.normalizedResultStatus ?? record?.resultStatus)
  if (normalizedStatus === 'FAILED' || normalizedStatus === 'CANCELED' || normalizedResult === 'failed') {
    return 'rejected'
  }
  if (normalizedStatus === 'QUEUED' || normalizedStatus === 'RUNNING') {
    return 'requested'
  }
  if (normalizedResult === 'pass') return 'approved'
  if (normalizedResult === 'violation') return 'rejected'
  return 'requested'
}

function recordBadgeLabel(record) {
  if (!record) return '대기 중'
  if (['QUEUED', 'RUNNING', 'FAILED', 'CANCELED'].includes(record.normalizedStatus)) {
    return statusLabel(record.normalizedStatus)
  }
  return resultStatusLabel(record.normalizedResultStatus)
}

function isActiveStatus(status) {
  return ['QUEUED', 'RUNNING'].includes(normalizeStatus(status))
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
  const pad = (number) => String(number).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatFileSize(size) {
  const bytes = Number(size)
  if (!Number.isFinite(bytes) || bytes <= 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let unitIndex = 0
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${value.toFixed(unitIndex === 0 ? 0 : 1)}${units[unitIndex]}`
}

const normalizedRecords = computed(() =>
  records.value.map((record) => ({
    ...record,
    normalizedStatus: normalizeStatus(record.status),
    normalizedResultStatus: normalizeResultStatus(record.resultStatus),
    archiveId: recordKey(record),
  })),
)

const summary = computed(() => {
  const total = normalizedRecords.value.length
  const completed = normalizedRecords.value.filter((record) => record.normalizedStatus === 'SUCCEEDED').length
  const review = normalizedRecords.value.filter((record) =>
    ['warning', 'violation'].includes(record.normalizedResultStatus),
  ).length
  const failed = normalizedRecords.value.filter((record) =>
    record.normalizedStatus === 'FAILED' || record.normalizedResultStatus === 'failed',
  ).length
  const active = normalizedRecords.value.filter((record) => isActiveStatus(record.normalizedStatus)).length
  return {
    total,
    completed,
    review,
    failed,
    active,
  }
})

const filteredRecords = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  return normalizedRecords.value
    .filter((record) => {
      if (activeFilter.value === 'all') return true
      if (activeFilter.value === 'completed') return record.normalizedStatus === 'SUCCEEDED'
      if (activeFilter.value === 'review') return ['warning', 'violation'].includes(record.normalizedResultStatus)
      if (activeFilter.value === 'failed') {
        return record.normalizedStatus === 'FAILED' || record.normalizedResultStatus === 'failed'
      }
      if (activeFilter.value === 'active') return isActiveStatus(record.normalizedStatus)
      return true
    })
    .filter((record) => {
      if (!query) return true
      const haystack = [
        record.fileName,
        record.summaryMessage,
        record.resultStatus,
        record.riskLevel,
        record.status,
        record.jobId,
        record.mongoDocumentId,
      ]
        .map(normalizeText)
        .join(' ')
        .toLowerCase()

      return haystack.includes(query)
    })
    .sort((a, b) => new Date(b.createdAt ?? 0) - new Date(a.createdAt ?? 0))
})

const selectedRecord = computed(() => {
  const selectedId = selectedRecordId.value
  return filteredRecords.value.find((record) => record.archiveId === selectedId) ?? filteredRecords.value[0] ?? null
})

const selectedDetail = computed(() => {
  const selectedId = selectedRecord.value?.archiveId
  return selectedId ? detailsByJobId.value[selectedId] ?? null : null
})

const selectedRawDocument = computed(() => selectedDetail.value?.rawDocument ?? {})
const selectedDetailData = computed(() => selectedDetail.value?.detail ?? null)
const selectedOriginalUrl = computed(() =>
  selectedDetailData.value?.fileUrl || detailValue(['file', 'url']) || '',
)
const isSelectedActive = computed(() => isActiveStatus(selectedRecord.value?.normalizedStatus))
const isSelectedDetailLoading = computed(() => detailLoadingJobId.value === selectedRecord.value?.archiveId)

const selectedAiSummary = computed(() => {
  const record = selectedRecord.value
  if (!record) return []

  return [
    { label: '최종 판정', value: resultStatusLabel(record.normalizedResultStatus) },
    { label: '위험도', value: riskLevelLabel(record.riskLevel) },
    { label: '요약', value: record.summaryMessage },
    { label: '관련 법령', value: selectedDetailData.value?.law },
    { label: '문제 표현', value: selectedDetailData.value?.violationText },
    { label: '검수 사유', value: selectedDetailData.value?.reason },
    { label: '수정 제안', value: selectedDetailData.value?.suggestion },
    { label: '오류 상세', value: detailValue(['errorDetail']) },
  ].filter((item) => normalizeText(item.value))
})

function detailValue(path, fallback = '') {
  return path.reduce((current, key) => current?.[key], selectedRawDocument.value) ?? fallback
}

function formatJson(value) {
  if (!value || (typeof value === 'object' && !Object.keys(value).length)) {
    return ''
  }

  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

async function loadLibrary() {
  if (!props.campaignId) return

  loading.value = true
  errorMessage.value = ''
  try {
    const response = await ListAdCheckJobs({ campaignId: props.campaignId })
    records.value = Array.isArray(response) ? response : []
    applyRouteSelection()
  } catch (error) {
    records.value = []
    errorMessage.value = error?.message ?? '자료실 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadDetail(jobId) {
  if (!jobId || detailsByJobId.value[jobId] || isSelectedActive.value) {
    return
  }

  detailLoadingJobId.value = jobId
  detailErrorMessage.value = ''
  try {
    const detail = await GetAdCheckJobDetail(jobId)
    detailsByJobId.value = {
      ...detailsByJobId.value,
      [jobId]: detail,
    }
  } catch (error) {
    detailErrorMessage.value = error?.message ?? '상세 자료를 불러오지 못했습니다.'
  } finally {
    detailLoadingJobId.value = ''
  }
}

function selectRecord(record) {
  selectedRecordId.value = record.archiveId
}

function applyRouteSelection() {
  const routeJobId = String(route.query.adCheckJobId || '').trim()
  if (routeJobId && normalizedRecords.value.some((record) => record.archiveId === routeJobId)) {
    selectedRecordId.value = routeJobId
  }
}

watch(
  filteredRecords,
  (rows) => {
    if (!rows.some((record) => record.archiveId === selectedRecordId.value)) {
      selectedRecordId.value = rows[0]?.archiveId ?? null
    }
  },
  { immediate: true },
)

watch(
  () => selectedRecord.value?.archiveId,
  (jobId) => {
    if (jobId) {
      void loadDetail(jobId)
    }
  },
  { immediate: true },
)

watch(
  () => route.query.adCheckJobId,
  applyRouteSelection,
)

watch(
  () => props.campaignId,
  () => {
    records.value = []
    searchQuery.value = ''
    activeFilter.value = 'all'
    selectedRecordId.value = null
    detailsByJobId.value = {}
    detailErrorMessage.value = ''
    void loadLibrary()
  },
)

onMounted(loadLibrary)
</script>

<template>
  <section class="library-page">
    <header class="library-toolbar">
      <div>
        <p>Campaign Library</p>
        <h3>자료실</h3>
      </div>
      <button type="button" class="library-button" :disabled="loading" @click="loadLibrary">
        {{ loading ? '새로고침 중' : '새로고침' }}
      </button>
    </header>

    <div class="library-stats" aria-label="자료실 요약">
      <article>
        <span>완료</span>
        <strong>{{ summary.completed }}</strong>
      </article>
      <article>
        <span>확인 필요</span>
        <strong>{{ summary.review }}</strong>
      </article>
      <article>
        <span>실패</span>
        <strong>{{ summary.failed }}</strong>
      </article>
      <article>
        <span>전체 자료</span>
        <strong>{{ summary.total }}</strong>
      </article>
    </div>

    <div class="library-controls">
      <div class="library-segments" aria-label="자료 상태 필터">
        <button
          v-for="option in filterOptions"
          :key="option.id"
          type="button"
          :class="{ active: activeFilter === option.id }"
          @click="activeFilter = option.id"
        >
          {{ option.label }}
        </button>
      </div>

      <label class="library-search">
        <span>검색</span>
        <input
          v-model="searchQuery"
          type="search"
          autocomplete="off"
          placeholder="파일명, 요약, 판정"
        />
      </label>
    </div>

    <p v-if="errorMessage" class="library-error">{{ errorMessage }}</p>

    <article v-if="loading" class="library-empty">
      <strong>자료를 불러오는 중입니다.</strong>
    </article>

    <section v-else-if="filteredRecords.length" class="library-layout">
      <div class="library-list" aria-label="자료 목록">
        <article
          v-for="record in filteredRecords"
          :key="record.archiveId"
          class="library-item"
          :class="{ 'library-item--active': selectedRecord?.archiveId === record.archiveId }"
          role="button"
          tabindex="0"
          @click="selectRecord(record)"
          @keydown.enter="selectRecord(record)"
        >
          <div class="library-item__main">
            <span :class="['library-status', `library-status--${statusTone(record)}`]">
              {{ recordBadgeLabel(record) }}
            </span>
            <strong>{{ record.fileName || '광고 소재' }}</strong>
            <p>{{ record.summaryMessage || '검수 결과 요약을 준비 중입니다.' }}</p>
          </div>
          <div class="library-item__meta">
            <span>{{ riskLevelLabel(record.riskLevel) }}</span>
            <time>{{ formatDateTime(record.createdAt) }}</time>
          </div>
        </article>
      </div>

      <aside v-if="selectedRecord" class="library-detail">
        <header class="library-detail__head">
          <div>
            <span :class="['library-status', `library-status--${statusTone(selectedRecord)}`]">
              {{ recordBadgeLabel(selectedRecord) }}
            </span>
            <h4>{{ selectedRecord.fileName || '광고 소재' }}</h4>
            <p>요청 {{ formatDateTime(selectedRecord.createdAt) }} · 완료 {{ formatDateTime(selectedRecord.finishedAt) }}</p>
          </div>
          <a
            v-if="selectedOriginalUrl"
            :href="selectedOriginalUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="library-file-link"
          >
            파일 열기
          </a>
        </header>

        <dl class="library-detail__grid">
          <div>
            <dt>콘텐츠 유형</dt>
            <dd>{{ selectedDetailData?.fileContentType || detailValue(['file', 'contentType'], '-') }}</dd>
          </div>
          <div>
            <dt>파일 크기</dt>
            <dd>{{ formatFileSize(selectedDetailData?.fileSize || detailValue(['file', 'size'])) }}</dd>
          </div>
          <div>
            <dt>상태</dt>
            <dd>{{ statusLabel(selectedRecord.normalizedStatus) }}</dd>
          </div>
          <div>
            <dt>Mongo ID</dt>
            <dd>{{ selectedRecord.mongoDocumentId || selectedDetail?.mongoDocumentId || '-' }}</dd>
          </div>
        </dl>

        <section v-if="isSelectedActive" class="library-detail__section">
          <h5>진행 상태</h5>
          <p>검수 작업이 아직 진행 중입니다. 완료되면 상세 분석 자료가 이곳에 표시됩니다.</p>
        </section>

        <section v-else-if="isSelectedDetailLoading" class="library-detail__section">
          <h5>상세 자료</h5>
          <p>상세 자료를 불러오고 있습니다.</p>
        </section>

        <section v-else-if="detailErrorMessage && !selectedDetail" class="library-detail__section">
          <h5>상세 자료</h5>
          <p class="library-error">{{ detailErrorMessage }}</p>
        </section>

        <section v-if="selectedAiSummary.length" class="library-detail__section">
          <h5>검수 요약</h5>
          <dl class="library-review-note">
            <div v-for="item in selectedAiSummary" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </section>

        <section v-if="selectedDetailData?.extractedText" class="library-detail__section">
          <h5>추출된 전체 텍스트</h5>
          <pre>{{ selectedDetailData.extractedText }}</pre>
        </section>

        <section v-if="formatJson(detailValue(['documentStructureResult']))" class="library-detail__section">
          <h5>문서 구조 분석 결과</h5>
          <pre>{{ formatJson(detailValue(['documentStructureResult'])) }}</pre>
        </section>

        <section v-if="formatJson(detailValue(['recognizedTextResult']))" class="library-detail__section">
          <h5>글자 인식 결과</h5>
          <pre>{{ formatJson(detailValue(['recognizedTextResult'])) }}</pre>
        </section>

        <section v-if="formatJson(detailValue(['textRiskAnalysisResult']))" class="library-detail__section">
          <h5>문구 위험도 분석 결과</h5>
          <pre>{{ formatJson(detailValue(['textRiskAnalysisResult'])) }}</pre>
        </section>

        <section v-if="formatJson(detailValue(['finalResult']))" class="library-detail__section">
          <h5>최종 상세 판정</h5>
          <pre>{{ formatJson(detailValue(['finalResult'])) }}</pre>
        </section>
      </aside>
    </section>

    <article v-else class="library-empty">
      <strong>표시할 자료가 없습니다.</strong>
      <p>{{ activeFilter === 'all' ? '검사하기를 완료하면 AI 검수 자료가 여기에 정리됩니다.' : '필터나 검색어를 조정해 보세요.' }}</p>
    </article>
  </section>
</template>

<style scoped>
.library-page {
  display: grid;
  width: 100%;
  max-width: 1180px;
  gap: 16px;
  margin: 0 auto;
}

.library-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 16px;
  box-shadow: var(--shadow-sm);
}

.library-toolbar p,
.library-stats span,
.library-item__meta span,
.library-detail__head p,
.library-detail__grid dt,
.library-detail__section h5,
.library-review-note dt,
.library-search span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.library-toolbar h3 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 950;
}

.library-button,
.library-file-link {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  padding: 0 12px;
  text-decoration: none;
  white-space: nowrap;
}

.library-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.library-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.library-stats article {
  display: grid;
  gap: 5px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 14px;
}

.library-stats strong {
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 950;
  font-variant-numeric: tabular-nums;
}

.library-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.library-segments {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}

.library-segments button {
  min-height: 34px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 850;
  padding: 0 12px;
}

.library-segments button.active {
  border-color: color-mix(in srgb, var(--color-primary-500) 34%, var(--border-color));
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.library-search {
  display: grid;
  min-width: min(340px, 100%);
  gap: 6px;
}

.library-search input {
  min-height: 36px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  padding: 0 12px;
}

.library-search input:focus {
  border-color: var(--color-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary-500) 16%, transparent);
}

.library-error {
  margin: 0;
  color: var(--color-danger-dark);
  font-size: 13px;
  font-weight: 850;
}

.library-layout {
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(360px, 1.1fr);
  gap: 14px;
  align-items: start;
}

.library-list {
  display: grid;
  gap: 8px;
}

.library-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  cursor: pointer;
  padding: 12px;
  transition: border-color 160ms ease, background-color 160ms ease, box-shadow 160ms ease;
}

.library-item:hover,
.library-item--active {
  border-color: color-mix(in srgb, var(--color-primary-500) 34%, var(--border-color));
  background: color-mix(in srgb, var(--color-primary-100) 42%, var(--panel-color));
  box-shadow: 0 8px 22px color-mix(in srgb, var(--color-primary-500) 12%, transparent);
}

.library-item:focus-visible {
  outline: 2px solid var(--color-primary-500);
  outline-offset: 2px;
}

.library-item__main {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.library-item__main strong,
.library-detail__head h4 {
  overflow: hidden;
  color: var(--text-primary);
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-item__main p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.library-item__meta {
  display: grid;
  gap: 5px;
  justify-items: end;
  min-width: 116px;
}

.library-item__meta time {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 850;
  white-space: nowrap;
}

.library-status {
  display: inline-flex;
  width: fit-content;
  min-height: 24px;
  align-items: center;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 950;
  padding: 0 9px;
  white-space: nowrap;
}

.library-status--approved {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.library-status--rejected {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.library-status--requested {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.library-detail {
  display: grid;
  gap: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 16px;
  box-shadow: var(--shadow-sm);
}

.library-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 14px;
}

.library-detail__head > div {
  display: grid;
  min-width: 0;
  gap: 7px;
}

.library-detail__head h4 {
  margin: 0;
  font-size: 18px;
}

.library-detail__head p {
  margin: 0;
}

.library-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.library-detail__grid div {
  display: grid;
  gap: 5px;
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  padding: 10px;
}

.library-detail__grid dd,
.library-review-note dd {
  min-width: 0;
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 750;
  line-height: 1.5;
  word-break: break-word;
}

.library-detail__section {
  display: grid;
  gap: 8px;
}

.library-detail__section h5 {
  margin: 0;
  text-transform: uppercase;
}

.library-detail__section p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.library-detail__section pre {
  max-height: 260px;
  overflow: auto;
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-primary);
  font-family: inherit;
  font-size: 12px;
  line-height: 1.55;
  padding: 11px;
  white-space: pre-wrap;
  word-break: break-word;
}

.library-review-note {
  display: grid;
  gap: 8px;
  margin: 0;
}

.library-review-note div {
  display: grid;
  gap: 4px;
  border-left: 3px solid var(--color-primary-500);
  background: var(--panel-muted);
  padding: 9px 11px;
}

.library-empty {
  display: grid;
  gap: 6px;
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 28px;
  text-align: center;
}

.library-empty strong {
  color: var(--text-primary);
  font-weight: 950;
}

.library-empty p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .library-layout {
    grid-template-columns: 1fr;
  }

  .library-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .library-toolbar,
  .library-controls,
  .library-detail__head,
  .library-item {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .library-search,
  .library-button,
  .library-file-link {
    width: 100%;
  }

  .library-item__meta {
    justify-items: start;
  }

  .library-detail__grid,
  .library-stats {
    grid-template-columns: 1fr;
  }
}
</style>
