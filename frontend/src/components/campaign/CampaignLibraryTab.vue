<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ListAdReviewRequests } from '@/api/adcheck/index.js'

const props = defineProps({
  campaignId: {
    type: [String, Number],
    required: true,
  },
})

const records = ref([])
const loading = ref(false)
const errorMessage = ref('')
const activeFilter = ref('processed')
const searchQuery = ref('')
const selectedRecordId = ref(null)

const filterOptions = [
  { id: 'processed', label: '처리 완료' },
  { id: 'APPROVED', label: '검수 통과' },
  { id: 'REJECTED', label: '반려' },
  { id: 'REQUESTED', label: '진행중' },
  { id: 'all', label: '전체' },
]

function normalizeStatus(status) {
  const value = String(status ?? '').trim().toUpperCase()
  if (['REQUESTED', 'APPROVED', 'REJECTED'].includes(value)) return value
  return 'REQUESTED'
}

function recordKey(record) {
  return String(record?.idx ?? record?.fileObjectKey ?? record?.fileName ?? '')
}

function normalizeText(value) {
  return String(value ?? '').replace(/\s+/g, ' ').trim()
}

function statusLabel(status) {
  const normalized = normalizeStatus(status)
  if (normalized === 'APPROVED') return '검수 통과'
  if (normalized === 'REJECTED') return '반려'
  return '진행중'
}

function statusTone(status) {
  const normalized = normalizeStatus(status)
  if (normalized === 'APPROVED') return 'approved'
  if (normalized === 'REJECTED') return 'rejected'
  return 'requested'
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
    normalizedStatus: normalizeStatus(record.requestStatus),
    archiveId: recordKey(record),
  })),
)

const summary = computed(() => {
  const total = normalizedRecords.value.length
  const approved = normalizedRecords.value.filter((record) => record.normalizedStatus === 'APPROVED').length
  const rejected = normalizedRecords.value.filter((record) => record.normalizedStatus === 'REJECTED').length
  const requested = normalizedRecords.value.filter((record) => record.normalizedStatus === 'REQUESTED').length
  return {
    total,
    processed: approved + rejected,
    approved,
    rejected,
    requested,
  }
})

const filteredRecords = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  return normalizedRecords.value
    .filter((record) => {
      if (activeFilter.value === 'processed') {
        return ['APPROVED', 'REJECTED'].includes(record.normalizedStatus)
      }
      if (activeFilter.value === 'all') return true
      return record.normalizedStatus === activeFilter.value
    })
    .filter((record) => {
      if (!query) return true
      const haystack = [
        record.fileName,
        record.requestMemo,
        record.requesterName,
        record.requesterLoginId,
        record.extractedText,
        record.law,
        record.violationText,
        record.reason,
        record.suggestion,
        record.rejectReason,
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

const selectedAiSummary = computed(() => {
  const record = selectedRecord.value
  if (!record) return []

  return [
    { label: '관련 법령', value: record.law },
    { label: '문제 표현', value: record.violationText },
    { label: '검수 사유', value: record.reason },
    { label: '수정 제안', value: record.suggestion },
    { label: '반려 사유', value: record.rejectReason },
  ].filter((item) => normalizeText(item.value))
})

async function loadLibrary() {
  if (!props.campaignId) return

  loading.value = true
  errorMessage.value = ''
  try {
    const response = await ListAdReviewRequests(props.campaignId)
    records.value = Array.isArray(response) ? response : []
  } catch (error) {
    records.value = []
    errorMessage.value = error?.message ?? '자료실 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function selectRecord(record) {
  selectedRecordId.value = record.archiveId
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
  () => props.campaignId,
  () => {
    records.value = []
    searchQuery.value = ''
    activeFilter.value = 'processed'
    selectedRecordId.value = null
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
        <span>처리 완료</span>
        <strong>{{ summary.processed }}</strong>
      </article>
      <article>
        <span>검수 통과</span>
        <strong>{{ summary.approved }}</strong>
      </article>
      <article>
        <span>반려</span>
        <strong>{{ summary.rejected }}</strong>
      </article>
      <article>
        <span>전체 요청</span>
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
          placeholder="파일명, 요청자, OCR 텍스트"
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
            <span :class="['library-status', `library-status--${statusTone(record.normalizedStatus)}`]">
              {{ statusLabel(record.normalizedStatus) }}
            </span>
            <strong>{{ record.fileName || '광고 소재' }}</strong>
            <p>{{ record.requestMemo || normalizeText(record.extractedText) || '검수 자료' }}</p>
          </div>
          <div class="library-item__meta">
            <span>{{ record.requesterName || record.requesterLoginId || '요청자 미상' }}</span>
            <time>{{ formatDateTime(record.createdAt) }}</time>
          </div>
        </article>
      </div>

      <aside v-if="selectedRecord" class="library-detail">
        <header class="library-detail__head">
          <div>
            <span :class="['library-status', `library-status--${statusTone(selectedRecord.normalizedStatus)}`]">
              {{ statusLabel(selectedRecord.normalizedStatus) }}
            </span>
            <h4>{{ selectedRecord.fileName || '광고 소재' }}</h4>
            <p>{{ selectedRecord.requesterName || selectedRecord.requesterLoginId || '요청자 미상' }} · {{ formatDateTime(selectedRecord.createdAt) }}</p>
          </div>
          <a
            v-if="selectedRecord.fileUrl"
            :href="selectedRecord.fileUrl"
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
            <dd>{{ selectedRecord.fileContentType || '-' }}</dd>
          </div>
          <div>
            <dt>파일 크기</dt>
            <dd>{{ formatFileSize(selectedRecord.fileSize) }}</dd>
          </div>
          <div>
            <dt>상태</dt>
            <dd>{{ statusLabel(selectedRecord.normalizedStatus) }}</dd>
          </div>
          <div>
            <dt>요청 번호</dt>
            <dd>{{ selectedRecord.idx ?? '-' }}</dd>
          </div>
        </dl>

        <section v-if="selectedRecord.requestMemo" class="library-detail__section">
          <h5>요청 메모</h5>
          <p>{{ selectedRecord.requestMemo }}</p>
        </section>

        <section v-if="selectedAiSummary.length" class="library-detail__section">
          <h5>검수 기록</h5>
          <dl class="library-review-note">
            <div v-for="item in selectedAiSummary" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value }}</dd>
            </div>
          </dl>
        </section>

        <section v-if="selectedRecord.extractedText" class="library-detail__section">
          <h5>OCR 텍스트</h5>
          <pre>{{ selectedRecord.extractedText }}</pre>
        </section>
      </aside>
    </section>

    <article v-else class="library-empty">
      <strong>표시할 자료가 없습니다.</strong>
      <p>{{ activeFilter === 'processed' ? '검수 통과 또는 반려 처리된 자료가 생기면 여기에 정리됩니다.' : '필터나 검색어를 조정해 보세요.' }}</p>
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
