<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  getAdCheckDisplayVerdict,
  normalizeAdCheckResultStatus,
  normalizeAdCheckVerdictLevel,
} from '@/utils/adCheckVerdict'

const props = defineProps({
  summary: {
    type: Object,
    default: null,
  },
  detail: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['close'])

const activeTab = ref('summary')
const imagePageIndex = ref(0)

const summaryData = computed(() => props.detail?.summary ?? props.summary ?? {})
const detailData = computed(() => props.detail?.detail ?? null)
const rawDocument = computed(() => props.detail?.rawDocument ?? {})
const hasDetail = computed(() => Boolean(props.detail))
const processingTimes = computed(() => detailData.value?.processingTimes ?? rawDocument.value?.processingTimes ?? null)

function firstTextValue(...values) {
  return values.find((value) => typeof value === 'string' && value.trim())?.trim() ?? ''
}

const fileInfo = computed(() => ({
  name: detailData.value?.fileName || summaryData.value?.fileName || '광고 소재',
  url: detailData.value?.fileUrl || detailValue(['file', 'url']) || summaryData.value?.fileUrl || '',
  contentType: detailData.value?.fileContentType
    || detailValue(['file', 'contentType'])
    || summaryData.value?.fileContentType
    || '',
  size: detailData.value?.fileSize || detailValue(['file', 'size']) || summaryData.value?.fileSize || null,
}))

const uploaderLabel = computed(() => {
  const name = firstTextValue(summaryData.value?.requesterName, summaryData.value?.requesterLoginId)
  const organization = firstTextValue(summaryData.value?.requesterOrganizationName)
  const primary = name || '요청자'
  return organization ? `${primary} · ${organization}` : primary
})

const verdict = computed(() =>
  getAdCheckDisplayVerdict({
    jobStatus: summaryData.value?.status,
    status: normalizeAdCheckResultStatus(summaryData.value?.resultStatus ?? detailData.value?.status),
    verdictLevel: normalizeAdCheckVerdictLevel(
      detailData.value?.verdictLevel ?? props.detail?.verdictLevel ?? summaryData.value?.verdictLevel,
    ),
    riskLevel: summaryData.value?.riskLevel,
    summaryMessage: summaryData.value?.summaryMessage,
    law: detailData.value?.law,
    violationText: detailData.value?.violationText,
    reason: detailData.value?.reason,
    suggestion: detailData.value?.suggestion,
  }),
)

const extractedImages = computed(() => {
  const candidates = [
    detailData.value?.extractedImageAssets,
    rawDocument.value?.artifacts?.images,
    rawDocument.value?.recognizedTextResult?.images,
  ]

  const images = candidates.find((items) => Array.isArray(items) && items.length) ?? []
  return images
    .map((image, index) => ({
      id: image.targetId || image.objectKey || image.url || `image-${index}`,
      type: image.type || '추출 이미지',
      page: Number.isFinite(Number(image.page)) ? Number(image.page) : 1,
      readingOrder: Number.isFinite(Number(image.readingOrder)) ? Number(image.readingOrder) : index,
      url: image.url || '',
      contentType: image.contentType || '',
      size: image.fileSize ?? image.size ?? null,
    }))
    .filter((image) => image.url)
    .sort((left, right) => {
      const pageDiff = normalizedPage(left.page) - normalizedPage(right.page)
      if (pageDiff !== 0) return pageDiff
      return left.readingOrder - right.readingOrder
    })
})

const previewUrl = computed(() => {
  if (summaryData.value?.thumbnailUrl) return summaryData.value.thumbnailUrl
  if (isImageContent(fileInfo.value.contentType) && fileInfo.value.url) return fileInfo.value.url
  return extractedImages.value[0]?.url || fileInfo.value.url || ''
})

const imagePages = computed(() => {
  const pageMap = new Map()

  extractedImages.value.forEach((image) => {
    const page = normalizedPage(image.page)
    const items = pageMap.get(page) ?? []
    items.push(image)
    pageMap.set(page, items)
  })

  return [...pageMap.entries()]
    .sort(([left], [right]) => left - right)
    .map(([page, images], index) => ({
      key: `page-${page}`,
      pageNo: page <= 0 ? index + 1 : page,
      images: images.slice(0, 2),
      totalImageCount: images.length,
    }))
})

const currentImagePage = computed(() => imagePages.value[imagePageIndex.value] ?? null)

const summarySections = computed(() =>
  [
    readableSection('summary', '요약', summaryData.value?.summaryMessage),
    readableSection('law', '관련 법령', detailData.value?.law),
    readableSection('violation', '문제 표현', detailData.value?.violationText, 'phrases'),
    readableSection('reason', '검수 사유', detailData.value?.reason),
    readableSection('suggestion', '수정 제안', detailData.value?.suggestion, 'steps'),
    readableSection('error', '오류 상세', detailData.value?.errorMessage || detailValue(['errorDetail'])),
  ].filter((section) => section.blocks.length),
)

const documentStructureStats = computed(() => {
  const structure = rawDocument.value?.documentStructureResult ?? {}
  return [
    { label: '문서 구조 분석 시간', value: formatDuration(structure.layoutMillis ?? processingTimes.value?.layoutMillis) },
    { label: '추출 이미지', value: `${extractedImages.value.length}개` },
    { label: '분석 페이지', value: imagePages.value.length ? `${imagePages.value.length}페이지` : '확인 중' },
  ].filter((item) => item.value && item.value !== '-')
})

const ocrStats = computed(() => [
  { label: 'OCR 처리 시간', value: formatDuration(processingTimes.value?.ocrMillis) },
  { label: '인식 텍스트', value: textLengthLabel(detailData.value?.extractedText) },
  { label: '이미지 영역', value: extractedImages.value.length ? `${extractedImages.value.length}개` : '없음' },
])

const riskSections = computed(() => resultSections(rawDocument.value?.textRiskAnalysisResult ?? detailData.value))
const finalSections = computed(() => resultSections(rawDocument.value?.finalResult ?? detailData.value))
const extractedTextBlocks = computed(() => splitTextBlocks(detailData.value?.extractedText))

watch(
  () => summaryData.value?.jobId,
  () => {
    activeTab.value = 'summary'
    imagePageIndex.value = 0
  },
)

watch(
  () => imagePages.value.length,
  (pageCount) => {
    if (imagePageIndex.value >= pageCount) {
      imagePageIndex.value = Math.max(0, pageCount - 1)
    }
  },
)

function normalizedPage(page) {
  const value = Number(page)
  if (!Number.isFinite(value)) return 1
  return value <= 0 ? 1 : value
}

function detailValue(path, fallback = '') {
  return path.reduce((current, key) => current?.[key], rawDocument.value) ?? fallback
}

function normalizeReadableText(value) {
  return String(value ?? '')
    .replace(/\r\n?/g, '\n')
    .replace(/[ \t]+/g, ' ')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .join('\n')
}

function splitTextBlocks(value) {
  const text = normalizeReadableText(value)
  if (!text) return []

  return text
    .split('\n')
    .flatMap((line) => line.match(/[^.!?。！？]+(?:[.!?。！？]+|$)/g) ?? [line])
    .map((line) => line.trim())
    .filter(Boolean)
}

function splitPhraseItems(value) {
  const text = normalizeReadableText(value)
  if (!text) return []

  return text
    .split(/\n+|[,，、]+/)
    .map((item) => item.replace(/^[\s"'“”‘’]+|[\s"'“”‘’]+$/g, '').trim())
    .filter(Boolean)
}

function splitSuggestionItems(value) {
  const text = normalizeReadableText(value)
  if (!text) return []

  const prepared = text
    .replace(/\s*예시\s*수정안\s*[:：]\s*/g, '\n')
    .replace(/\s*수정\s*제안\s*[:：]\s*/g, '\n')
    .replace(/\s*[-•]\s+/g, '\n')
    .replace(/\s+(?=\d+[.)]\s+)/g, '\n')

  const items = prepared
    .split('\n')
    .map((item) => item.replace(/^\d+[.)]\s*/, '').trim())
    .filter(Boolean)

  return items.length > 1 ? items : splitTextBlocks(text)
}

function readableSection(key, label, value, type = 'paragraphs') {
  const blocks =
    type === 'phrases'
      ? splitPhraseItems(value)
      : type === 'steps'
        ? splitSuggestionItems(value)
        : splitTextBlocks(value)

  return { key, label, type, blocks }
}

function resultSections(result) {
  if (!result) return []
  return [
    readableSection('law', '관련 법령', result.law),
    readableSection('violation', '문제 표현', result.violationText, 'phrases'),
    readableSection('reason', '판단 근거', result.reason),
    readableSection('suggestion', '수정 제안', result.suggestion, 'steps'),
    readableSection('error', '오류 상세', result.errorMessage),
  ].filter((section) => section.blocks.length)
}

function isImageContent(contentType) {
  return String(contentType || '').toLowerCase().startsWith('image/')
}

function statusLabel(status) {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'QUEUED') return '대기 중'
  if (normalized === 'RUNNING') return '처리 중'
  if (normalized === 'SUCCEEDED') return verdict.value.label
  if (normalized === 'FAILED') return '검수 실패'
  if (normalized === 'CANCELED') return '취소'
  return verdict.value.label
}

function statusTone(status) {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'FAILED' || normalized === 'CANCELED') return 'danger'
  if (normalized === 'QUEUED' || normalized === 'RUNNING') return 'requested'
  return verdict.value.tone
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

function formatDuration(millis) {
  const value = Number(millis)
  if (!Number.isFinite(value) || value < 0) return '-'
  if (value < 1000) return `${value}ms`
  return `${(value / 1000).toFixed(2)}s`
}

function textLengthLabel(value) {
  const length = normalizeReadableText(value).length
  return length ? `${length.toLocaleString('ko-KR')}자` : '없음'
}

function setImagePage(index) {
  imagePageIndex.value = Math.max(0, Math.min(index, imagePages.value.length - 1))
}

function handleEscape(event) {
  if (event.key === 'Escape') {
    emit('close')
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEscape)
})
</script>

<template>
  <teleport to="body">
    <div class="ad-detail-backdrop" role="presentation" @click.self="emit('close')">
      <section class="ad-detail-modal" role="dialog" aria-modal="true" aria-labelledby="adCheckDetailTitle">
        <header class="ad-detail-modal__head">
          <div>
            <span :class="['ad-detail-status', `ad-detail-status--${statusTone(summaryData.status)}`]">
              {{ statusLabel(summaryData.status) }}
            </span>
            <h4 id="adCheckDetailTitle">{{ fileInfo.name }}</h4>
            <p>요청 {{ formatDateTime(summaryData.createdAt) }} · 완료 {{ formatDateTime(summaryData.finishedAt) }}</p>
          </div>
          <div class="ad-detail-modal__actions">
            <a
              v-if="fileInfo.url"
              :href="fileInfo.url"
              target="_blank"
              rel="noopener noreferrer"
              class="ad-detail-button"
            >
              파일 열기
            </a>
            <button type="button" class="ad-detail-close" aria-label="닫기" @click="emit('close')">×</button>
          </div>
        </header>

        <div class="ad-detail-tabs" aria-label="검수 상세 보기 방식">
          <button type="button" :class="{ active: activeTab === 'summary' }" @click="activeTab = 'summary'">
            요약 보기
          </button>
          <button type="button" :class="{ active: activeTab === 'full' }" @click="activeTab = 'full'">
            전체 내용 보기
          </button>
        </div>

        <div class="ad-detail-body">
          <section v-if="loading" class="ad-detail-state">
            상세 자료를 불러오고 있습니다.
          </section>

          <section v-else-if="errorMessage && !hasDetail" class="ad-detail-state ad-detail-state--danger">
            {{ errorMessage }}
          </section>

          <section v-else-if="activeTab === 'summary'" class="ad-detail-summary">
            <div class="ad-detail-overview">
              <figure class="ad-detail-preview">
                <img v-if="previewUrl" :src="previewUrl" :alt="`${fileInfo.name} 미리보기`" />
                <figcaption v-else>미리보기 없음</figcaption>
              </figure>

              <dl class="ad-detail-meta">
                <div>
                  <dt>판단 등급</dt>
                  <dd>
                    <span :class="['ad-detail-verdict', `ad-detail-verdict--${verdict.tone}`]">
                      {{ verdict.title }}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>조치 안내</dt>
                  <dd>{{ verdict.guidance }}</dd>
                </div>
                <div>
                  <dt>업로드</dt>
                  <dd>{{ uploaderLabel }}</dd>
                </div>
                <div>
                  <dt>콘텐츠 유형</dt>
                  <dd>{{ fileInfo.contentType || '-' }}</dd>
                </div>
                <div>
                  <dt>파일 크기</dt>
                  <dd>{{ formatFileSize(fileInfo.size) }}</dd>
                </div>
              </dl>
            </div>

            <section v-if="summarySections.length" class="ad-readable-section-list">
              <article
                v-for="section in summarySections"
                :key="section.key"
                :class="['ad-readable-section', `ad-readable-section--${section.type}`]"
              >
                <h5>{{ section.label }}</h5>
                <div v-if="section.type === 'phrases'" class="ad-phrase-list">
                  <span v-for="item in section.blocks" :key="item">{{ item }}</span>
                </div>
                <ol v-else-if="section.type === 'steps'" class="ad-step-list">
                  <li v-for="item in section.blocks" :key="item">{{ item }}</li>
                </ol>
                <div v-else class="ad-readable-text">
                  <p v-for="block in section.blocks" :key="block">{{ block }}</p>
                </div>
              </article>
            </section>
          </section>

          <section v-else class="ad-detail-full">
            <article class="ad-clean-section">
              <h5>문서 구조 분석 결과</h5>
              <dl class="ad-clean-grid">
                <div v-for="item in documentStructureStats" :key="item.label">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                </div>
              </dl>
            </article>

            <article class="ad-clean-section">
              <header class="ad-clean-section__head">
                <div>
                  <h5>페이지별 추출 이미지</h5>
                  <p>페이지당 최대 2개 영역만 작게 표시합니다.</p>
                </div>
                <div v-if="imagePages.length > 1" class="ad-page-controls">
                  <button type="button" :disabled="imagePageIndex === 0" @click="setImagePage(imagePageIndex - 1)">
                    이전
                  </button>
                  <button
                    v-for="(page, index) in imagePages"
                    :key="page.key"
                    type="button"
                    :class="{ active: imagePageIndex === index }"
                    @click="setImagePage(index)"
                  >
                    {{ page.pageNo }}
                  </button>
                  <button
                    type="button"
                    :disabled="imagePageIndex >= imagePages.length - 1"
                    @click="setImagePage(imagePageIndex + 1)"
                  >
                    다음
                  </button>
                </div>
              </header>

              <div v-if="currentImagePage" class="ad-image-page">
                <div class="ad-image-page__label">
                  {{ currentImagePage.pageNo }}페이지 · {{ currentImagePage.totalImageCount }}개 중 최대 2개 표시
                </div>
                <figure v-for="image in currentImagePage.images" :key="image.id" class="ad-extracted-image">
                  <img :src="image.url" :alt="`${currentImagePage.pageNo}페이지 추출 이미지`" />
                  <figcaption>{{ image.type }} · {{ formatFileSize(image.size) }}</figcaption>
                </figure>
              </div>

              <p v-else class="ad-detail-empty">표시할 추출 이미지가 없습니다.</p>
            </article>

            <article class="ad-clean-section">
              <h5>글자 인식 결과</h5>
              <dl class="ad-clean-grid">
                <div v-for="item in ocrStats" :key="item.label">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ item.value }}</dd>
                </div>
              </dl>
              <div v-if="extractedTextBlocks.length" class="ad-text-preview">
                <p v-for="block in extractedTextBlocks" :key="block">{{ block }}</p>
              </div>
              <p v-else class="ad-detail-empty">인식된 텍스트가 없습니다.</p>
            </article>

            <article class="ad-clean-section">
              <h5>문구 위험도 분석 결과</h5>
              <section v-if="riskSections.length" class="ad-readable-section-list">
                <article
                  v-for="section in riskSections"
                  :key="section.key"
                  :class="['ad-readable-section', `ad-readable-section--${section.type}`]"
                >
                  <h5>{{ section.label }}</h5>
                  <div v-if="section.type === 'phrases'" class="ad-phrase-list">
                    <span v-for="item in section.blocks" :key="item">{{ item }}</span>
                  </div>
                  <ol v-else-if="section.type === 'steps'" class="ad-step-list">
                    <li v-for="item in section.blocks" :key="item">{{ item }}</li>
                  </ol>
                  <div v-else class="ad-readable-text">
                    <p v-for="block in section.blocks" :key="block">{{ block }}</p>
                  </div>
                </article>
              </section>
              <p v-else class="ad-detail-empty">분석 결과가 없습니다.</p>
            </article>

            <article class="ad-clean-section">
              <h5>최종 상세 판정</h5>
              <div class="ad-final-verdict">
                <span :class="['ad-detail-verdict', `ad-detail-verdict--${verdict.tone}`]">
                  {{ verdict.title }}
                </span>
                <p>{{ verdict.guidance }}</p>
              </div>
              <section v-if="finalSections.length" class="ad-readable-section-list">
                <article
                  v-for="section in finalSections"
                  :key="section.key"
                  :class="['ad-readable-section', `ad-readable-section--${section.type}`]"
                >
                  <h5>{{ section.label }}</h5>
                  <div v-if="section.type === 'phrases'" class="ad-phrase-list">
                    <span v-for="item in section.blocks" :key="item">{{ item }}</span>
                  </div>
                  <ol v-else-if="section.type === 'steps'" class="ad-step-list">
                    <li v-for="item in section.blocks" :key="item">{{ item }}</li>
                  </ol>
                  <div v-else class="ad-readable-text">
                    <p v-for="block in section.blocks" :key="block">{{ block }}</p>
                  </div>
                </article>
              </section>
            </article>
          </section>
        </div>
      </section>
    </div>
  </teleport>
</template>

<style scoped>
.ad-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: grid;
  place-items: center;
  padding: 24px;
  background: color-mix(in srgb, var(--text-primary) 34%, transparent);
}

:global(:root[data-theme='dark']) .ad-detail-backdrop {
  background: rgba(0, 0, 0, 0.58);
}

.ad-detail-modal {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  width: min(920px, 100%);
  height: min(800px, calc(100vh - 48px));
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-lg);
}

.ad-detail-modal__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--border-color);
  padding: 18px 18px 0;
}

.ad-detail-modal__head > div {
  display: grid;
  min-width: 0;
  gap: 7px;
}

.ad-detail-modal__head h4 {
  overflow: hidden;
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ad-detail-modal__head p,
.ad-detail-meta dt,
.ad-readable-section h5,
.ad-clean-section h5,
.ad-clean-grid dt,
.ad-image-page__label {
  margin: 0;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.ad-detail-modal__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ad-detail-button,
.ad-detail-close,
.ad-page-controls button {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  padding: 0 12px;
  text-decoration: none;
  white-space: nowrap;
}

.ad-detail-close {
  width: 34px;
  padding: 0;
  font-size: 20px;
}

.ad-detail-tabs {
  display: inline-flex;
  gap: 6px;
  padding: 14px 18px 0;
}

.ad-detail-tabs button {
  min-height: 34px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  padding: 0 12px;
}

.ad-detail-tabs button.active,
.ad-page-controls button.active {
  border-color: color-mix(in srgb, var(--color-primary-500) 34%, var(--border-color));
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.ad-page-controls button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.ad-detail-body {
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 14px 18px 18px;
}

.ad-detail-summary,
.ad-detail-full {
  display: grid;
  gap: 12px;
}

.ad-detail-overview {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 12px;
}

.ad-detail-preview {
  display: grid;
  min-height: 160px;
  overflow: hidden;
  place-items: center;
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.ad-detail-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.ad-detail-preview figcaption {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 850;
}

.ad-detail-meta,
.ad-clean-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.ad-detail-meta div,
.ad-clean-grid div {
  display: grid;
  min-width: 0;
  gap: 5px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  padding: 10px;
}

.ad-detail-meta dd,
.ad-clean-grid dd {
  min-width: 0;
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.5;
  word-break: break-word;
}

.ad-detail-status,
.ad-detail-verdict {
  display: inline-flex;
  width: fit-content;
  min-height: 25px;
  align-items: center;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 950;
  padding: 0 9px;
  white-space: nowrap;
}

.ad-detail-status--requested {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.ad-detail-status--pass,
.ad-detail-verdict--pass {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.ad-detail-status--recheck,
.ad-detail-verdict--recheck {
  background: color-mix(in srgb, #84cc16 18%, transparent);
  color: #4d7c0f;
}

.ad-detail-status--suggestion,
.ad-detail-verdict--suggestion {
  background: color-mix(in srgb, var(--color-warning) 18%, transparent);
  color: var(--color-warning-dark);
}

.ad-detail-status--revision,
.ad-detail-verdict--revision {
  background: color-mix(in srgb, #f97316 18%, transparent);
  color: #9a3412;
}

.ad-detail-status--danger,
.ad-detail-verdict--danger {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.ad-detail-status--neutral,
.ad-detail-verdict--neutral {
  background: var(--panel-muted);
  color: var(--text-secondary);
}

.ad-readable-section-list,
.ad-readable-section,
.ad-clean-section {
  display: grid;
  gap: 10px;
}

.ad-readable-section,
.ad-clean-section {
  border-left: 3px solid var(--color-primary-500);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  padding: 12px 14px;
}

.ad-readable-section--phrases {
  border-left-color: var(--color-danger);
}

.ad-readable-section--steps {
  border-left-color: var(--color-warning);
}

.ad-readable-text {
  display: grid;
  gap: 7px;
}

.ad-readable-text p,
.ad-text-preview p,
.ad-final-verdict p,
.ad-detail-empty {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 750;
  line-height: 1.65;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.ad-phrase-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.ad-phrase-list span {
  display: inline-flex;
  align-items: center;
  border: 1px solid color-mix(in srgb, var(--color-danger) 28%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-danger) 10%, var(--panel-color));
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 850;
  line-height: 1.35;
  padding: 5px 9px;
}

.ad-step-list {
  counter-reset: suggestion;
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.ad-step-list li {
  position: relative;
  min-height: 30px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 750;
  line-height: 1.6;
  padding: 8px 10px 8px 40px;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.ad-step-list li::before {
  counter-increment: suggestion;
  content: counter(suggestion);
  position: absolute;
  top: 9px;
  left: 11px;
  display: inline-flex;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--color-primary-500);
  color: var(--text-inverse);
  font-size: 11px;
  font-weight: 950;
}

.ad-clean-section {
  border-left-color: var(--border-color);
}

.ad-clean-section__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.ad-clean-section__head h5,
.ad-clean-section__head p {
  margin: 0;
}

.ad-clean-section__head p {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
}

.ad-page-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.ad-page-controls button {
  min-height: 28px;
  padding: 0 9px;
  font-size: 12px;
}

.ad-image-page {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ad-image-page__label {
  grid-column: 1 / -1;
}

.ad-extracted-image {
  display: grid;
  overflow: hidden;
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
}

.ad-extracted-image img {
  width: 100%;
  height: 136px;
  object-fit: contain;
  background: var(--panel-muted);
}

.ad-extracted-image figcaption {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 750;
  padding: 7px 9px;
}

.ad-text-preview {
  display: grid;
  max-height: 220px;
  overflow: auto;
  gap: 7px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  padding: 10px;
}

.ad-final-verdict {
  display: grid;
  gap: 8px;
}

.ad-detail-state {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 850;
  padding: 18px;
}

.ad-detail-state--danger {
  border-color: color-mix(in srgb, var(--color-danger) 36%, var(--border-color));
  color: var(--color-danger-dark);
}

:global(:root[data-theme='dark']) .ad-detail-status--pass,
:global(:root[data-theme='dark']) .ad-detail-verdict--pass {
  background: color-mix(in srgb, #10b981 16%, transparent);
  color: #6ee7b7;
}

:global(:root[data-theme='dark']) .ad-detail-status--recheck,
:global(:root[data-theme='dark']) .ad-detail-verdict--recheck {
  background: color-mix(in srgb, #84cc16 18%, transparent);
  color: #bef264;
}

:global(:root[data-theme='dark']) .ad-detail-status--suggestion,
:global(:root[data-theme='dark']) .ad-detail-verdict--suggestion {
  background: color-mix(in srgb, var(--color-warning) 18%, transparent);
  color: #fcd34d;
}

:global(:root[data-theme='dark']) .ad-detail-status--revision,
:global(:root[data-theme='dark']) .ad-detail-verdict--revision {
  background: color-mix(in srgb, #f97316 18%, transparent);
  color: #fdba74;
}

:global(:root[data-theme='dark']) .ad-detail-status--danger,
:global(:root[data-theme='dark']) .ad-detail-verdict--danger {
  background: color-mix(in srgb, var(--color-danger) 18%, transparent);
  color: #fca5a5;
}

@media (max-width: 760px) {
  .ad-detail-backdrop {
    padding: 12px;
  }

  .ad-detail-modal {
    height: calc(100vh - 24px);
  }

  .ad-detail-modal__head,
  .ad-clean-section__head {
    align-items: stretch;
    flex-direction: column;
  }

  .ad-detail-modal__actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .ad-detail-button {
    width: 100%;
  }

  .ad-detail-overview,
  .ad-detail-meta,
  .ad-clean-grid,
  .ad-image-page {
    grid-template-columns: 1fr;
  }
}
</style>
