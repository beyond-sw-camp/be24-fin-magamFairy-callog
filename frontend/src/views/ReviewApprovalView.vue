<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  ApproveAdReviewRequest,
  CheckAdFileWithAiJudge, //CheckAdFileWithAiJudge 로 변경시 8081
  CreateAdReviewRequest,
  ListAdReviewRequests,
  RejectAdReviewRequest,
} from '@/api/adcheck/index.js'
import { getCampaignMembers } from '@/api/campaignMembers'

const props = defineProps({
  campaignId: {
    type: [String, Number],
    required: true,
  },
})

const isAnalysisOpen = ref(false)
const selectedAnalysisFile = ref(null)
const analysisFileInput = ref(null)
const isUploadDragOver = ref(false)
const uploadDragDepth = ref(0)
const isAnalyzing = ref(false)
const analysisResult = ref(null)
const analysisError = ref('')
const reviewRequestMemo = ref('')
const isSubmittingReviewRequest = ref(false)
const requestSubmitError = ref('')

const reviewRequests = ref([])
const reviewLoadError = ref('')
const reviewDecisionError = ref('')
const submittingDecisionId = ref(null)

const memberContext = ref(null)
const memberContextError = ref('')

const myCampaignRole = computed(() => memberContext.value?.me?.campaignRole ?? null)
const organizationIsPm = computed(() => Boolean(memberContext.value?.organizationIsPm))
const canUseAiJudge = computed(() => Boolean(props.campaignId))
const canRequestReview = computed(() => Boolean(memberContext.value && !organizationIsPm.value))
const canFinalReview = computed(() =>
  organizationIsPm.value
  && ['MANAGER', 'GENERAL_MANAGER'].includes(myCampaignRole.value),
)

const normalizedAnalysisStatus = computed(() => normalizeAnalysisStatus(analysisResult.value?.status))
const normalizedAnalysisPassed = computed(() =>
  normalizedAnalysisStatus.value === 'pass' && !analysisError.value,
)

const analysisFileInfo = computed(() => {
  if (!selectedAnalysisFile.value) return null

  const fileSize = selectedAnalysisFile.value.size / 1024 / 1024
  return {
    name: selectedAnalysisFile.value.name,
    size: `${fileSize.toFixed(fileSize >= 1 ? 1 : 2)}MB`,
    type: selectedAnalysisFile.value.type || '파일 형식 자동 감지',
  }
})

const analysisIssues = computed(() => {
  if (!analysisResult.value || normalizedAnalysisStatus.value === 'pass') return []

  const { law, violationText, reason, suggestion } = analysisResult.value
  const issueSections = buildIssueSections(reason, suggestion)
  return [{
    title: normalizedAnalysisStatus.value === 'violation'
      ? '광고법 위반 표현 발견'
      : '주의가 필요한 표현 발견',
    source: law || 'AI 검수',
    target: violationText || '',
    reason: issueSections.reason,
    suggestion: issueSections.suggestion,
    suggestionItems: issueSections.suggestionItems,
  }]
})

const analysisProcessingTimes = computed(() => analysisResult.value?.processingTimes ?? null)

const canCreateReviewRequest = computed(() =>
  Boolean(
    analysisFileInfo.value
    && props.campaignId
    && canRequestReview.value
    && analysisResult.value?.fileObjectKey
    && normalizedAnalysisPassed.value
    && !isAnalyzing.value
    && !isSubmittingReviewRequest.value,
  ),
)

function normalizeAnalysisStatus(status) {
  const value = String(status ?? '').trim().toLowerCase()
  if (['violation', 'warning', 'pass'].includes(value)) return value
  return ''
}

function buildIssueSections(reason, suggestion) {
  const parsedReason = splitSuggestionMarker(reason)
  const cleanSuggestion = normalizeText(suggestion) || parsedReason.suggestion

  return {
    reason: parsedReason.reason,
    suggestion: cleanSuggestion,
    suggestionItems: splitSuggestionItems(cleanSuggestion),
  }
}

function splitSuggestionMarker(value) {
  const text = normalizeText(value)
  const marker = text.match(/(?:^|[\s·-])수정\s*제안\s*[:：]\s*/)
  if (!marker || marker.index === undefined) {
    return { reason: text, suggestion: '' }
  }

  return {
    reason: text.slice(0, marker.index).replace(/[·\s-]+$/, '').trim(),
    suggestion: text.slice(marker.index + marker[0].length).trim(),
  }
}

function splitSuggestionItems(value) {
  const text = normalizeText(value).replace(/^수정\s*제안\s*[:：]\s*/, '').trim()
  if (!text) return []

  const bulletParts = text
    .split(/\n+|[·•]\s+/)
    .map((item) => item.trim())
    .filter(Boolean)

  if (bulletParts.length > 1) return bulletParts

  const sentenceParts = text
    .match(/[^.!?。]+(?:[.!?。]+|$)/g)
    ?.map((item) => item.trim())
    .filter(Boolean) ?? []

  return sentenceParts.length > 1 ? sentenceParts : [text]
}

function normalizeText(value) {
  return String(value ?? '').replace(/\s+/g, ' ').trim()
}

function analysisStatusLabel(status) {
  if (status === 'pass') return '이상 없음'
  if (status === 'warning') return '주의 필요'
  if (status === 'violation') return '위반 의심'
  return '대기'
}

function reviewStatusLabel(status) {
  if (status === 'REQUESTED') return '요청'
  if (status === 'APPROVED') return '검수통과'
  if (status === 'REJECTED') return '반려'
  return status ?? '요청'
}

function reviewStatusTone(status) {
  if (status === 'APPROVED') return 'ready'
  if (status === 'REJECTED') return 'danger'
  return 'approval'
}

function formatDate(value) {
  if (!value) return '요청일 없음'
  return String(value).slice(0, 10)
}

function extractionModeLabel(mode) {
  if (mode === 'plain_text') return 'TXT 직접 읽기'
  if (mode === 'pdf_embedded_text') return 'PDF 내장 텍스트'
  if (mode === 'pdf_layout_ocr') return 'PDF Layout + OCR'
  if (mode === 'image_layout_ocr') return '이미지 Layout + OCR'
  if (mode === 'pdf_ocr_fallback') return 'PDF OCR fallback'
  if (mode === 'image_ocr_fallback') return '이미지 OCR fallback'
  if (mode === 'pdf_ocr') return 'PDF OCR'
  if (mode === 'image_ocr') return '이미지 OCR'
  return mode || '대기'
}

function formatDuration(millis) {
  if (millis === null || millis === undefined) return '-'
  if (millis < 1000) return `${millis}ms`
  return `${(millis / 1000).toFixed(2)}s`
}

async function loadMemberContext() {
  memberContextError.value = ''
  if (!props.campaignId) return

  try {
    const response = await getCampaignMembers(props.campaignId)
    memberContext.value = response?.data?.data ?? null
  } catch (error) {
    memberContext.value = null
    memberContextError.value = error?.message ?? '캠페인 권한 정보를 불러오지 못했습니다.'
  }
}

async function loadReviewRequests() {
  reviewLoadError.value = ''
  if (!props.campaignId) return

  try {
    const requests = await ListAdReviewRequests(props.campaignId)
    reviewRequests.value = Array.isArray(requests) ? requests : []
  } catch (error) {
    reviewRequests.value = []
    reviewLoadError.value = error?.message ?? '검수 요청 목록을 불러오지 못했습니다.'
  }
}

async function createReviewRequest() {
  requestSubmitError.value = ''

  if (!normalizedAnalysisPassed.value) {
    requestSubmitError.value = 'AI 1차 검수 통과 후 검수 요청을 생성할 수 있습니다.'
    return
  }

  if (!analysisResult.value?.fileObjectKey) {
    requestSubmitError.value = '업로드된 파일 정보를 확인할 수 없습니다.'
    return
  }

  isSubmittingReviewRequest.value = true
  try {
    const result = await CreateAdReviewRequest(props.campaignId, {
      fileName: analysisResult.value.fileName ?? analysisFileInfo.value?.name ?? 'upload',
      fileObjectKey: analysisResult.value.fileObjectKey,
      fileContentType: analysisResult.value.fileContentType ?? selectedAnalysisFile.value?.type ?? null,
      fileSize: analysisResult.value.fileSize ?? selectedAnalysisFile.value?.size ?? null,
      extractedText: analysisResult.value.extractedText ?? '',
      status: analysisResult.value.status,
      law: analysisResult.value.law,
      violationText: analysisResult.value.violationText,
      reason: analysisResult.value.reason,
      suggestion: analysisResult.value.suggestion,
      requestMemo: reviewRequestMemo.value.trim() || null,
    })

    replaceReviewRequest(result)
    resetAnalysisForm()
    isAnalysisOpen.value = false
  } catch (error) {
    requestSubmitError.value = error?.message ?? '검수 요청 생성에 실패했습니다.'
  } finally {
    isSubmittingReviewRequest.value = false
  }
}

async function approveReviewRequest(request) {
  if (!canFinalReview.value || !request?.idx || submittingDecisionId.value) return

  const memo = window.prompt('검수통과 메모를 입력하세요. 비워두어도 됩니다.', '') ?? ''
  submittingDecisionId.value = request.idx
  reviewDecisionError.value = ''
  try {
    const result = await ApproveAdReviewRequest(props.campaignId, request.idx, { memo })
    replaceReviewRequest(result)
  } catch (error) {
    reviewDecisionError.value = error?.message ?? '검수통과 처리에 실패했습니다.'
  } finally {
    submittingDecisionId.value = null
  }
}

async function rejectReviewRequest(request) {
  if (!canFinalReview.value || !request?.idx || submittingDecisionId.value) return

  const reason = window.prompt('반려 사유를 입력하세요.', '')
  if (!reason || !reason.trim()) return

  submittingDecisionId.value = request.idx
  reviewDecisionError.value = ''
  try {
    const result = await RejectAdReviewRequest(props.campaignId, request.idx, { reason: reason.trim() })
    replaceReviewRequest(result)
  } catch (error) {
    reviewDecisionError.value = error?.message ?? '반려 처리에 실패했습니다.'
  } finally {
    submittingDecisionId.value = null
  }
}

function replaceReviewRequest(request) {
  const index = reviewRequests.value.findIndex((item) => item.idx === request.idx)
  if (index >= 0) {
    reviewRequests.value.splice(index, 1, request)
    return
  }
  reviewRequests.value.unshift(request)
}

function openAnalysisRequest() {
  if (!canUseAiJudge.value) return
  isAnalysisOpen.value = true
}

function closeAnalysisRequest() {
  isAnalysisOpen.value = false
}

function resetAnalysisForm() {
  selectedAnalysisFile.value = null
  analysisResult.value = null
  analysisError.value = ''
  requestSubmitError.value = ''
  reviewRequestMemo.value = ''
  isUploadDragOver.value = false
  uploadDragDepth.value = 0
  if (analysisFileInput.value) {
    analysisFileInput.value.value = ''
  }
}

async function handleAnalysisFileChange(event) {
  const [file] = event.target.files ?? []
  await processAnalysisFile(file)
}

function handleUploadDragEnter() {
  if (isAnalyzing.value) return
  uploadDragDepth.value += 1
  isUploadDragOver.value = true
}

function handleUploadDragLeave() {
  uploadDragDepth.value = Math.max(0, uploadDragDepth.value - 1)
  if (uploadDragDepth.value === 0) {
    isUploadDragOver.value = false
  }
}

function handleUploadDragOver(event) {
  if (isAnalyzing.value) return
  event.dataTransfer.dropEffect = 'copy'
  isUploadDragOver.value = true
}

async function handleUploadDrop(event) {
  uploadDragDepth.value = 0
  isUploadDragOver.value = false
  if (isAnalyzing.value) return

  const [file] = event.dataTransfer?.files ?? []
  await processAnalysisFile(file)
}

async function processAnalysisFile(file) {
  resetAnalysisForm()
  if (!file) return

  if (!isSupportedAnalysisFile(file)) {
    analysisError.value = 'TXT, PDF, 이미지 파일만 업로드할 수 있습니다.'
    return
  }

  selectedAnalysisFile.value = file
  isAnalyzing.value = true
  try {
    const result = await CheckAdFileWithAiJudge(file) //CheckAdFileWithAiJudge 로 변경시 8081
    analysisResult.value = result
    if (!normalizeAnalysisStatus(result?.status)) {
      analysisError.value = 'AI 검수 결과 형식이 올바르지 않습니다. 서버 응답을 확인해주세요.'
    }
  } catch (error) {
    if (error?.data && typeof error.data === 'object') {
      analysisResult.value = error.data
    }
    analysisError.value = error?.message ?? 'AI 검수 요청에 실패했습니다.'
  } finally {
    isAnalyzing.value = false
  }
}

function isSupportedAnalysisFile(file) {
  const name = String(file?.name ?? '').toLowerCase()
  const type = String(file?.type ?? '').toLowerCase()
  return (
    type.startsWith('image/')
    || type === 'application/pdf'
    || type === 'text/plain'
    || name.endsWith('.txt')
    || name.endsWith('.pdf')
  )
}

async function loadPageData() {
  await Promise.all([
    loadMemberContext(),
    loadReviewRequests(),
  ])
}

onMounted(loadPageData)

watch(
  () => props.campaignId,
  () => {
    resetAnalysisForm()
    isAnalysisOpen.value = false
    reviewRequests.value = []
    loadPageData()
  },
)
</script>

<template>
  <section class="review-page">
    <section v-if="isAnalysisOpen" class="review-panel">
      <header class="review-panel__head">
        <div>
          <p>AI Risk Review</p>
          <h3>검수 요청 생성</h3>
        </div>
        <button type="button" class="ghost-button" @click="closeAnalysisRequest">목록으로</button>
      </header>

      <div class="analysis-layout">
        <section class="analysis-upload">
          <label
            for="analysisFile"
            class="upload-box"
            :class="{ 'upload-box--dragover': isUploadDragOver }"
            @dragenter.prevent="handleUploadDragEnter"
            @dragover.prevent="handleUploadDragOver"
            @dragleave.prevent="handleUploadDragLeave"
            @drop.prevent="handleUploadDrop"
          >
            <input
              id="analysisFile"
              ref="analysisFileInput"
              type="file"
              accept=".txt,.pdf,image/*"
              :disabled="isAnalyzing"
              @change="handleAnalysisFileChange"
            />
            <strong>검수할 파일 업로드</strong>
            <span>클릭하거나 TXT, PDF, 이미지 파일을 끌어다 놓으면 AI 1차 검수를 진행합니다.</span>
          </label>

          <article class="file-summary">
            <template v-if="analysisFileInfo">
              <span>업로드 파일</span>
              <strong>{{ analysisFileInfo.name }}</strong>
              <p>{{ analysisFileInfo.size }} · {{ analysisFileInfo.type }}</p>
            </template>
            <template v-else>
              <span>대기 중</span>
              <strong>선택된 파일이 없습니다.</strong>
              <p>파일을 선택하면 AI 검수 결과가 표시됩니다.</p>
            </template>
          </article>

          <label v-if="canRequestReview" class="memo-field">
            <span>요청 메모</span>
            <textarea
              v-model="reviewRequestMemo"
              rows="4"
              placeholder="PM에게 전달할 검수 요청 내용을 입력하세요."
            />
          </label>
        </section>

        <aside class="analysis-result">
          <article
            class="analysis-verdict"
            :class="{
              'analysis-verdict--empty': !analysisFileInfo || isAnalyzing,
              'analysis-verdict--error': analysisError || analysisIssues.length,
              'analysis-verdict--clear': analysisFileInfo && !isAnalyzing && normalizedAnalysisStatus === 'pass',
            }"
          >
            <span>AI 1차 판단</span>
            <strong>
              {{
                !analysisFileInfo
                  ? '파일 업로드 대기'
                  : isAnalyzing
                    ? 'AI 분석 중'
                    : analysisError
                      ? '검수 실패'
                      : analysisStatusLabel(normalizedAnalysisStatus)
              }}
            </strong>
            <p v-if="analysisError">{{ analysisError }}</p>
            <p v-else-if="normalizedAnalysisStatus === 'pass'">AI 1차 검수에서 문제 항목이 발견되지 않았습니다.</p>
            <p v-else-if="analysisIssues.length">아래 항목을 확인한 뒤 수정 후 다시 요청해주세요.</p>
            <p v-else>파일을 업로드하면 검수 결과가 표시됩니다.</p>
          </article>

          <article v-if="analysisProcessingTimes" class="analysis-timing">
            <header class="analysis-timing__head">
              <span>Processing Time</span>
              <strong>{{ extractionModeLabel(analysisResult?.extractionMode) }}</strong>
            </header>
            <dl>
              <div>
                <dt>PDF/TXT</dt>
                <dd>{{ formatDuration(analysisProcessingTimes.textExtractionMillis) }}</dd>
              </div>
              <div>
                <dt>Layout</dt>
                <dd>{{ formatDuration(analysisProcessingTimes.layoutMillis) }}</dd>
              </div>
              <div>
                <dt>OCR</dt>
                <dd>{{ formatDuration(analysisProcessingTimes.ocrMillis) }}</dd>
              </div>
              <div>
                <dt>AI</dt>
                <dd>{{ formatDuration(analysisProcessingTimes.aiAnalysisMillis) }}</dd>
              </div>
              <div>
                <dt>Total</dt>
                <dd>{{ formatDuration(analysisProcessingTimes.totalMillis) }}</dd>
              </div>
            </dl>
          </article>

          <div v-if="analysisIssues.length" class="issue-list">
            <article v-for="issue in analysisIssues" :key="issue.title" class="issue-card">
              <span>{{ issue.source }}</span>
              <strong>{{ issue.title }}</strong>
              <blockquote v-if="issue.target">{{ issue.target }}</blockquote>
              <div v-if="issue.reason" class="issue-section">
                <span>위반 사유</span>
                <p>{{ issue.reason }}</p>
              </div>
              <div v-if="issue.suggestion" class="issue-section issue-section--suggestion">
                <span>수정 제안</span>
                <ul v-if="issue.suggestionItems.length > 1">
                  <li v-for="item in issue.suggestionItems" :key="item">{{ item }}</li>
                </ul>
                <p v-else>{{ issue.suggestion }}</p>
              </div>
            </article>
          </div>

          <article v-if="requestSubmitError" class="form-error">
            {{ requestSubmitError }}
          </article>

          <button
            v-if="canRequestReview"
            type="button"
            class="primary-button"
            :disabled="!canCreateReviewRequest"
            @click="createReviewRequest"
          >
            {{ isSubmittingReviewRequest ? '생성 중...' : '검수 요청 생성' }}
          </button>
        </aside>
      </div>
    </section>

    <section v-else class="review-panel">
      <header class="review-panel__head">
        <div>
          <p>Review Requests</p>
          <h3>검수 요청</h3>
        </div>
        <button v-if="canUseAiJudge" type="button" class="primary-button" @click="openAnalysisRequest">
          검수 요청
        </button>
      </header>

      <p v-if="memberContextError" class="form-error">{{ memberContextError }}</p>
      <p v-if="reviewLoadError" class="form-error">{{ reviewLoadError }}</p>
      <p v-if="reviewDecisionError" class="form-error">{{ reviewDecisionError }}</p>

      <div v-if="reviewRequests.length" class="review-list">
        <article v-for="request in reviewRequests" :key="request.idx" class="review-card">
          <div class="review-card__main">
            <span class="review-card__type">검수</span>
            <strong>{{ request.fileName ?? '광고 소재 검수 요청' }}</strong>
            <p>{{ request.requestMemo || 'AI 1차 검수 통과 후 생성된 검수 요청입니다.' }}</p>
          </div>

          <div class="review-card__meta">
            <span>{{ request.requesterName ?? request.requesterLoginId ?? '요청자' }}</span>
            <strong>{{ formatDate(request.createdAt) }}</strong>
          </div>

          <div class="review-card__actions">
            <a
              v-if="request.fileUrl"
              :href="request.fileUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              파일 확인
            </a>
            <button
              v-if="canFinalReview && request.requestStatus === 'REQUESTED'"
              type="button"
              :disabled="submittingDecisionId === request.idx"
              @click="approveReviewRequest(request)"
            >
              검수통과
            </button>
            <button
              v-if="canFinalReview && request.requestStatus === 'REQUESTED'"
              type="button"
              :disabled="submittingDecisionId === request.idx"
              @click="rejectReviewRequest(request)"
            >
              반려
            </button>
          </div>

          <em :class="`status-chip status-chip--${reviewStatusTone(request.requestStatus)}`">
            {{ reviewStatusLabel(request.requestStatus) }}
          </em>

          <details v-if="request.extractedText" class="review-card__text">
            <summary>OCR 텍스트</summary>
            <pre>{{ request.extractedText }}</pre>
          </details>

          <p v-if="request.rejectReason" class="review-card__reason">
            반려 사유: {{ request.rejectReason }}
          </p>
        </article>
      </div>

      <article v-else class="empty-state">
        <strong>등록된 검수 요청이 없습니다.</strong>
        <p>협력사가 AI 1차 검수를 통과한 파일을 요청하면 이곳에 표시됩니다.</p>
      </article>
    </section>
  </section>
</template>

<style scoped>
.review-page {
  display: grid;
  width: 100%;
  max-width: 1180px;
  gap: 16px;
  margin: 0 auto;
}

.review-panel {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.review-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.review-panel__head p,
.file-summary span,
.memo-field span,
.analysis-verdict span,
.issue-card span,
.review-card__meta span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.review-panel__head h3 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 950;
}

.primary-button,
.ghost-button,
.review-card__actions a,
.review-card__actions button {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
  white-space: nowrap;
}

.primary-button {
  background: var(--color-primary-500);
  color: #fff;
}

.ghost-button,
.review-card__actions a,
.review-card__actions button {
  border-color: var(--border-color);
  background: var(--panel-color);
  color: var(--text-primary);
}

.primary-button:disabled,
.review-card__actions button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.analysis-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(360px, 1.05fr);
  gap: 16px;
}

.analysis-upload,
.analysis-result {
  display: grid;
  align-content: start;
  gap: 12px;
}

.upload-box {
  display: grid;
  min-height: 220px;
  place-items: center;
  align-content: center;
  gap: 9px;
  padding: 28px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  text-align: center;
  transition: background-color 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.upload-box--dragover {
  border-color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-100) 58%, var(--panel-color));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--color-primary-500) 30%, transparent);
  color: var(--color-primary-700);
}

.upload-box input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.upload-box strong,
.file-summary strong,
.analysis-verdict strong,
.issue-card strong,
.review-card__main strong,
.empty-state strong {
  color: var(--text-primary);
  font-weight: 950;
}

.upload-box span,
.file-summary p,
.analysis-verdict p,
.issue-card p,
.review-card__main p,
.empty-state p,
.review-card__reason {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.file-summary,
.analysis-verdict,
.issue-card,
.review-card,
.empty-state {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.file-summary,
.analysis-verdict,
.issue-card,
.empty-state {
  display: grid;
  gap: 6px;
  padding: 14px;
}

.memo-field {
  display: grid;
  gap: 7px;
}

.memo-field textarea {
  width: 100%;
  resize: vertical;
  padding: 11px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.55;
  outline: none;
}

.analysis-verdict--clear {
  border-color: color-mix(in srgb, var(--color-success) 34%, var(--border-color));
  background: color-mix(in srgb, var(--color-success) 12%, var(--panel-color));
}

.analysis-verdict--error {
  border-color: color-mix(in srgb, var(--color-danger) 34%, var(--border-color));
  background: var(--danger-surface);
}

.analysis-verdict--empty {
  border-color: var(--border-color);
}

.analysis-timing {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.analysis-timing__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.analysis-timing__head span,
.analysis-timing dt {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.analysis-timing__head strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.analysis-timing dl {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.analysis-timing div {
  display: grid;
  gap: 3px;
}

.analysis-timing dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.issue-list {
  display: grid;
  gap: 8px;
}

.issue-card blockquote {
  margin: 0;
  padding: 8px 10px;
  border-left: 3px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
}

.issue-section {
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
}

.issue-section > span {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 950;
}

.issue-section p,
.issue-section ul {
  margin: 0;
}

.issue-section ul {
  display: grid;
  gap: 6px;
  padding-left: 18px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.issue-section--suggestion {
  border-color: color-mix(in srgb, var(--color-primary-500) 28%, var(--border-color));
  background: color-mix(in srgb, var(--color-primary-100) 48%, var(--panel-color));
}

.form-error {
  margin: 0;
  color: var(--color-danger-dark);
  font-size: 13px;
  font-weight: 850;
}

.review-list {
  display: grid;
  gap: 8px;
}

.review-card {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(120px, 0.32fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.review-card__main,
.review-card__meta {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.review-card__type {
  width: fit-content;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  background: var(--panel-color);
  color: var(--color-primary-700);
  font-size: 11px;
  font-weight: 950;
}

.review-card__main strong,
.review-card__meta strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-card__actions {
  display: inline-flex;
  gap: 6px;
}

.status-chip {
  display: inline-flex;
  width: fit-content;
  min-height: 25px;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-style: normal;
  font-weight: 950;
  white-space: nowrap;
}

.status-chip--ready {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.status-chip--approval {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.status-chip--danger {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.review-card__text,
.review-card__reason {
  grid-column: 1 / -1;
}

.review-card__text summary {
  cursor: pointer;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.review-card__text pre {
  max-height: 180px;
  overflow: auto;
  margin: 8px 0 0;
  padding: 10px;
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  font-family: inherit;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-state {
  padding: 28px;
  text-align: center;
}

@media (max-width: 1180px) {
  .analysis-layout,
  .review-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .review-panel__head,
  .review-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .analysis-timing dl {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .primary-button,
  .ghost-button,
  .review-card__actions a,
  .review-card__actions button {
    width: 100%;
  }
}
</style>
