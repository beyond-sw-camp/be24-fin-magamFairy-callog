<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DeleteAdCheckJob, GetAdCheckJobDetail, ListAdCheckJobs } from '@/api/adcheck/index.js'
import { getCampaignMembers } from '@/api/campaignMembers'
import AdCheckDetailModal from '@/components/adcheck/AdCheckDetailModal.vue'
import {
  AD_CHECK_VERDICT_LEVELS,
  getAdCheckDisplayVerdict,
  normalizeAdCheckResultStatus,
  normalizeAdCheckVerdictLevel,
} from '@/utils/adCheckVerdict'

const props = defineProps({
  campaignId: {
    type: [String, Number],
    required: true,
  },
})

const route = useRoute()
const router = useRouter()
const records = ref([])
const loading = ref(false)
const errorMessage = ref('')
const detailLoadingJobId = ref('')
const detailErrorMessage = ref('')
const detailsByJobId = ref({})
const thumbnailHydrationIds = ref(new Set())
const deletingJobIds = ref(new Set())
const currentUserIdx = ref(null)
const activeFilter = ref('all')
const searchQuery = ref('')
const selectedRecordId = ref(null)
const currentPage = ref(1)

const PAGE_SIZE = 6

const filterOptions = [
  { id: 'all', label: '전체' },
  { id: 'level-1', label: '완료' },
  { id: 'level-2', label: '확인 필요' },
  { id: 'level-3', label: '수정 제안' },
  { id: 'level-4', label: '수정 필요' },
  { id: 'level-5', label: '반려' },
]

function normalizeStatus(status) {
  const value = String(status ?? '').trim().toUpperCase()
  if (['QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED'].includes(value)) return value
  return 'QUEUED'
}

function normalizeResultStatus(status) {
  return normalizeAdCheckResultStatus(status)
}

function normalizeRecordVerdictLevel(record) {
  const level = normalizeAdCheckVerdictLevel(record?.verdictLevel ?? record?.riskLevel)
  if (level != null) return level

  const resultStatus = normalizeResultStatus(record?.resultStatus)
  return resultStatus === 'pass' ? 1 : null
}

function recordKey(record) {
  return String(record?.jobId ?? record?.mongoDocumentId ?? record?.fileName ?? '')
}

function normalizeNumericId(value) {
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

function normalizeText(value) {
  return String(value ?? '').replace(/\s+/g, ' ').trim()
}

function firstTextValue(...values) {
  return values.find((value) => typeof value === 'string' && value.trim())?.trim() ?? ''
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

function statusTone(record) {
  const normalizedStatus = normalizeStatus(record?.normalizedStatus ?? record?.status)
  const normalizedResult = normalizeResultStatus(record?.normalizedResultStatus ?? record?.resultStatus)
  if (normalizedStatus === 'FAILED' || normalizedStatus === 'CANCELED' || normalizedResult === 'failed') {
    return 'danger'
  }
  if (normalizedStatus === 'QUEUED' || normalizedStatus === 'RUNNING') {
    return 'requested'
  }
  return recordVerdict(record).tone
}

function recordBadgeLabel(record) {
  if (!record) return '대기 중'
  if (['QUEUED', 'RUNNING', 'FAILED', 'CANCELED'].includes(record.normalizedStatus)) {
    return statusLabel(record.normalizedStatus)
  }
  return recordVerdict(record).label
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

function recordVerdict(record) {
  return getAdCheckDisplayVerdict({
    jobStatus: record?.normalizedStatus ?? record?.status,
    status: record?.normalizedResultStatus ?? record?.resultStatus,
    verdictLevel: record?.verdictLevel,
    riskLevel: record?.riskLevel,
    summaryMessage: record?.summaryMessage,
  })
}

function firstImageUrl(images) {
  if (!Array.isArray(images)) return ''
  return images.find((image) => typeof image?.url === 'string' && image.url.trim())?.url ?? ''
}

function detailThumbnailUrl(detail) {
  const summary = detail?.summary ?? {}
  const detailResult = detail?.detail ?? {}
  const rawDocument = detail?.rawDocument ?? {}
  const contentType = firstTextValue(summary.fileContentType, detailResult.fileContentType)
  const fileUrl = firstTextValue(summary.fileUrl, detailResult.fileUrl)

  if (summary.thumbnailUrl) return summary.thumbnailUrl
  if (contentType.startsWith('image/') && fileUrl) return fileUrl

  return firstTextValue(
    firstImageUrl(detailResult.extractedImageAssets),
    firstImageUrl(rawDocument?.artifacts?.images),
    firstImageUrl(rawDocument?.recognizedTextResult?.images),
  )
}

function recordThumbnail(record) {
  if (record?.thumbnailUrl) return record.thumbnailUrl
  if (String(record?.fileContentType || '').startsWith('image/')) return record.fileUrl || ''
  return detailThumbnailUrl(detailsByJobId.value[record?.archiveId ?? recordKey(record)])
}

function recordUploaderLabel(record) {
  const name = firstTextValue(record?.requesterName, record?.requesterLoginId)
  const organization = firstTextValue(record?.requesterOrganizationName)
  const primary = name || '요청자'
  return organization ? `${primary} · ${organization}` : primary
}

function canDeleteRecord(record) {
  return Boolean(
    record?.archiveId
    && currentUserIdx.value
    && normalizeNumericId(record.requesterId) === currentUserIdx.value,
  )
}

const normalizedRecords = computed(() =>
  records.value.map((record) => ({
    ...record,
    normalizedStatus: normalizeStatus(record.status),
    normalizedResultStatus: normalizeResultStatus(record.resultStatus),
    verdictLevel: normalizeRecordVerdictLevel(record),
    requesterLoginId: record.requesterLoginId ?? '',
    requesterName: record.requesterName ?? '',
    requesterOrganizationName: record.requesterOrganizationName ?? '',
    fileUrl: record.fileUrl ?? '',
    fileContentType: record.fileContentType ?? '',
    fileSize: record.fileSize ?? null,
    thumbnailUrl: record.thumbnailUrl ?? '',
    archiveId: recordKey(record),
  })),
)

const summary = computed(() => {
  const total = normalizedRecords.value.length
  const countByLevel = AD_CHECK_VERDICT_LEVELS.reduce((acc, level) => {
    acc[level.level] = normalizedRecords.value.filter((record) => record.verdictLevel === level.level).length
    return acc
  }, {})

  return {
    total,
    level1: countByLevel[1] ?? 0,
    level2: countByLevel[2] ?? 0,
    level3: countByLevel[3] ?? 0,
    level4: countByLevel[4] ?? 0,
    level5: countByLevel[5] ?? 0,
  }
})

const filteredRecords = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  return normalizedRecords.value
    .filter((record) => {
      if (activeFilter.value === 'all') return true
      const levelMatch = activeFilter.value.match(/^level-([1-5])$/)
      if (levelMatch) return record.verdictLevel === Number(levelMatch[1])
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
        record.requesterName,
        record.requesterLoginId,
        record.requesterOrganizationName,
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
  if (!selectedId) return null
  return filteredRecords.value.find((record) => record.archiveId === selectedId) ?? null
})

const selectedDetail = computed(() => {
  const selectedId = selectedRecord.value?.archiveId
  return selectedId ? detailsByJobId.value[selectedId] ?? null : null
})

const isSelectedActive = computed(() => isActiveStatus(selectedRecord.value?.normalizedStatus))
const isSelectedDetailLoading = computed(() => detailLoadingJobId.value === selectedRecord.value?.archiveId)
const hasDetailModal = computed(() => Boolean(selectedRecord.value))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredRecords.value.length / PAGE_SIZE)))
const pagedRecords = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredRecords.value.slice(start, start + PAGE_SIZE)
})

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

async function loadMemberContext() {
  if (!props.campaignId) return

  try {
    const response = await getCampaignMembers(props.campaignId)
    currentUserIdx.value = normalizeNumericId(response?.data?.data?.me?.userIdx)
  } catch (error) {
    currentUserIdx.value = null
    console.warn('Campaign member context load failed.', error)
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

async function hydrateRecordThumbnail(record) {
  const jobId = record?.archiveId

  if (
    !jobId ||
    recordThumbnail(record) ||
    detailsByJobId.value[jobId] ||
    thumbnailHydrationIds.value.has(jobId) ||
    isActiveStatus(record.normalizedStatus)
  ) {
    return
  }

  const nextHydrationIds = new Set(thumbnailHydrationIds.value)
  nextHydrationIds.add(jobId)
  thumbnailHydrationIds.value = nextHydrationIds

  try {
    const detail = await GetAdCheckJobDetail(jobId)
    detailsByJobId.value = {
      ...detailsByJobId.value,
      [jobId]: detail,
    }

    const summary = detail?.summary ?? {}
    const detailResult = detail?.detail ?? {}
    const thumbnailUrl = detailThumbnailUrl(detail)

    records.value = records.value.map((item) => {
      if (recordKey(item) !== jobId) {
        return item
      }

      return {
        ...item,
        fileUrl: firstTextValue(item.fileUrl, summary.fileUrl, detailResult.fileUrl),
        fileContentType: firstTextValue(
          item.fileContentType,
          summary.fileContentType,
          detailResult.fileContentType,
        ),
        fileSize: item.fileSize ?? summary.fileSize ?? detailResult.fileSize ?? null,
        thumbnailUrl: firstTextValue(item.thumbnailUrl, summary.thumbnailUrl, thumbnailUrl),
      }
    })
  } catch (error) {
    console.warn('Ad check thumbnail hydration failed.', error)
  } finally {
    const nextIds = new Set(thumbnailHydrationIds.value)
    nextIds.delete(jobId)
    thumbnailHydrationIds.value = nextIds
  }
}

function selectRecord(record) {
  selectedRecordId.value = record.archiveId
}

function closeDetailModal() {
  const closingRecordId = selectedRecordId.value
  selectedRecordId.value = null
  detailErrorMessage.value = ''

  if (closingRecordId && String(route.query.adCheckJobId || '') === closingRecordId) {
    void router.replace({
      query: {
        ...route.query,
        adCheckJobId: undefined,
      },
    })
  }
}

async function deleteRecord(record) {
  if (!canDeleteRecord(record) || deletingJobIds.value.has(record.archiveId)) return
  if (!window.confirm('이 검수 자료를 삭제할까요? 삭제한 자료는 자료실에서 숨겨집니다.')) return

  const nextDeletingIds = new Set(deletingJobIds.value)
  nextDeletingIds.add(record.archiveId)
  deletingJobIds.value = nextDeletingIds
  errorMessage.value = ''

  try {
    await DeleteAdCheckJob(record.archiveId)
    records.value = records.value.filter((item) => recordKey(item) !== record.archiveId)
    const nextDetails = { ...detailsByJobId.value }
    delete nextDetails[record.archiveId]
    detailsByJobId.value = nextDetails
    if (selectedRecordId.value === record.archiveId) {
      selectedRecordId.value = null
    }
  } catch (error) {
    errorMessage.value = error?.message ?? '검수 자료 삭제에 실패했습니다.'
  } finally {
    const remainingDeletingIds = new Set(deletingJobIds.value)
    remainingDeletingIds.delete(record.archiveId)
    deletingJobIds.value = remainingDeletingIds
  }
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
    if (selectedRecordId.value && !rows.some((record) => record.archiveId === selectedRecordId.value)) {
      selectedRecordId.value = null
    }
    if (currentPage.value > totalPages.value) {
      currentPage.value = totalPages.value
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
  pagedRecords,
  (rows) => {
    rows.forEach((record) => {
      void hydrateRecordThumbnail(record)
    })
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
    currentPage.value = 1
    selectedRecordId.value = null
    detailsByJobId.value = {}
    detailErrorMessage.value = ''
    currentUserIdx.value = null
    void loadMemberContext()
    void loadLibrary()
  },
)

watch([activeFilter, searchQuery], () => {
  currentPage.value = 1
})

onMounted(() => {
  void loadMemberContext()
  void loadLibrary()
})
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
        <span>전체 자료</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article>
        <span>완료</span>
        <strong>{{ summary.level1 }}</strong>
      </article>
      <article>
        <span>확인 필요</span>
        <strong>{{ summary.level2 }}</strong>
      </article>
      <article>
        <span>수정 제안</span>
        <strong>{{ summary.level3 }}</strong>
      </article>
      <article>
        <span>수정 필요</span>
        <strong>{{ summary.level4 }}</strong>
      </article>
      <article>
        <span>반려</span>
        <strong>{{ summary.level5 }}</strong>
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

    <section v-else-if="filteredRecords.length" class="library-list" aria-label="자료 목록">
      <div class="library-list" aria-label="자료 목록">
        <article
          v-for="record in pagedRecords"
          :key="record.archiveId"
          class="library-item"
          :class="{ 'library-item--active': selectedRecord?.archiveId === record.archiveId }"
          role="button"
          tabindex="0"
          @click="selectRecord(record)"
          @keydown.enter="selectRecord(record)"
        >
          <figure class="library-item__preview">
            <img v-if="recordThumbnail(record)" :src="recordThumbnail(record)" :alt="`${record.fileName} 미리보기`" />
            <figcaption v-else>Preview</figcaption>
          </figure>
          <div class="library-item__main">
            <span :class="['library-status', `library-status--${statusTone(record)}`]">
              {{ recordBadgeLabel(record) }}
            </span>
            <strong>{{ record.fileName || '광고 소재' }}</strong>
            <p>{{ record.summaryMessage || '검수 결과 요약을 준비 중입니다.' }}</p>
          </div>
          <div class="library-item__meta">
            <span>{{ recordVerdict(record).title }}</span>
            <span>업로드 {{ recordUploaderLabel(record) }}</span>
            <time>{{ formatDateTime(record.createdAt) }}</time>
            <button type="button" class="library-detail-button" @click.stop="selectRecord(record)">
              상세 보기
            </button>
            <button
              v-if="canDeleteRecord(record)"
              type="button"
              class="library-delete-button"
              :disabled="deletingJobIds.has(record.archiveId)"
              @click.stop="deleteRecord(record)"
            >
              {{ deletingJobIds.has(record.archiveId) ? '삭제 중' : '삭제' }}
            </button>
          </div>
        </article>
      </div>

      <nav v-if="totalPages > 1" class="library-pagination" aria-label="자료실 페이지">
        <button type="button" :disabled="currentPage === 1" @click="currentPage -= 1">
          이전
        </button>
        <button
          v-for="page in totalPages"
          :key="page"
          type="button"
          :class="{ active: currentPage === page }"
          @click="currentPage = page"
        >
          {{ page }}
        </button>
        <button type="button" :disabled="currentPage === totalPages" @click="currentPage += 1">
          다음
        </button>
      </nav>
    </section>

    <article v-else class="library-empty">
      <strong>표시할 자료가 없습니다.</strong>
      <p>{{ activeFilter === 'all' ? '검사하기를 완료하면 AI 검수 자료가 여기에 정리됩니다.' : '필터나 검색어를 조정해 보세요.' }}</p>
    </article>

    <AdCheckDetailModal
      v-if="hasDetailModal"
      :summary="selectedRecord"
      :detail="selectedDetail"
      :loading="isSelectedDetailLoading"
      :error-message="detailErrorMessage"
      @close="closeDetailModal"
    />
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
.library-detail-button,
.library-delete-button {
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

.library-detail-button,
.library-delete-button {
  min-height: 28px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  padding: 0 9px;
}

.library-delete-button {
  border-color: color-mix(in srgb, var(--color-danger) 34%, var(--border-color));
  color: var(--color-danger-dark);
}

.library-delete-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.library-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
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

.library-list {
  display: grid;
  gap: 8px;
}

.library-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
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

.library-item__preview {
  display: grid;
  width: 88px;
  height: 70px;
  overflow: hidden;
  place-items: center;
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
}

.library-item__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.library-item__preview figcaption {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 850;
}

.library-item__main {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.library-item__main strong {
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

.library-pagination {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.library-pagination button {
  min-height: 30px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 0 10px;
}

.library-pagination button.active {
  border-color: color-mix(in srgb, var(--color-primary-500) 34%, var(--border-color));
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.library-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
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

.library-status--approved,
.library-status--pass {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.library-status--recheck {
  background: color-mix(in srgb, var(--color-success-light) 62%, var(--color-warning-light));
  color: var(--color-success-dark);
}

.library-status--suggestion {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.library-status--revision {
  background: color-mix(in srgb, var(--color-warning-light) 56%, var(--color-danger-light));
  color: var(--color-warning-dark);
}

.library-status--rejected,
.library-status--danger {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.library-status--requested {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.library-status--neutral {
  background: var(--panel-muted);
  color: var(--text-secondary);
}

.library-status--recheck {
  background: color-mix(in srgb, #84cc16 18%, transparent);
  color: #4d7c0f;
}

.library-status--suggestion {
  background: color-mix(in srgb, #f59e0b 18%, transparent);
  color: #92400e;
}

.library-status--revision {
  background: color-mix(in srgb, #f97316 18%, transparent);
  color: #9a3412;
}

:global(:root[data-theme='dark']) .library-status--pass {
  background: color-mix(in srgb, #10b981 16%, transparent);
  color: #6ee7b7;
}

:global(:root[data-theme='dark']) .library-status--recheck {
  background: color-mix(in srgb, #84cc16 18%, transparent);
  color: #bef264;
}

:global(:root[data-theme='dark']) .library-status--suggestion {
  background: color-mix(in srgb, #f59e0b 18%, transparent);
  color: #fcd34d;
}

:global(:root[data-theme='dark']) .library-status--revision {
  background: color-mix(in srgb, #f97316 18%, transparent);
  color: #fdba74;
}

:global(:root[data-theme='dark']) .library-status--danger {
  background: color-mix(in srgb, var(--color-danger) 18%, transparent);
  color: #fca5a5;
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
  .library-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .library-toolbar,
  .library-controls,
  .library-item {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .library-search,
  .library-button,
  .library-detail-button,
  .library-delete-button {
    width: 100%;
  }

  .library-item__meta {
    justify-items: start;
  }

  .library-stats {
    grid-template-columns: 1fr;
  }
}
</style>
