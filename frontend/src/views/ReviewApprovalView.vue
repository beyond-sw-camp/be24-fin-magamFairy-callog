<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ApproveAdReviewRequest,
  CheckCampaignAdFileWithAiJudge,
  CreateAdReviewRequest,
  ListAdReviewRequests,
  RejectAdReviewRequest,
} from '@/api/adcheck/index.js'
import { getCampaignMembers } from '@/api/campaignMembers'
import AdCheckDetailModal from '@/components/adcheck/AdCheckDetailModal.vue'
import AdCheckJobProgress from '@/components/adcheck/AdCheckJobProgress.vue'
import { adCheckStatusLabel, isTerminalJobStatus, useAdCheckJobsStore } from '@/stores/adCheckJobs'
import {
  AD_CHECK_VERDICT_LEVELS,
  getAdCheckDisplayVerdict,
  getAdCheckVerdict,
  normalizeAdCheckResultStatus,
} from '@/utils/adCheckVerdict'

const props = defineProps({
  campaignId: {
    type: [String, Number],
    required: true,
  },
  viewMode: {
    type: String,
    default: 'all',
    validator: (value) => ['all', 'check', 'approval'].includes(value),
  },
})

const route = useRoute()
const router = useRouter()
const adCheckJobsStore = useAdCheckJobsStore()
const isAnalysisOpen = ref(false)
const selectedAnalysisFile = ref(null)
const analysisFileInput = ref(null)
const isUploadDragOver = ref(false)
const uploadDragDepth = ref(0)
const isAnalyzing = ref(false)
const activeAnalysisJobId = ref('')
const analysisResult = ref(null)
const analysisError = ref('')
const reviewRequestMemo = ref('')
const isSubmittingReviewRequest = ref(false)
const requestSubmitError = ref('')

const reviewRequests = ref([])
const reviewLoadError = ref('')
const reviewDecisionError = ref('')
const submittingDecisionId = ref(null)
const selectedDetailJobId = ref('')
const analysisDetailLoadingJobId = ref('')
const adCheckResultPage = ref(1)
const submittingReviewJobId = ref('')

const AD_CHECK_RESULT_PAGE_SIZE = 4

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
const currentUserIdx = computed(() => Number(memberContext.value?.me?.userIdx ?? 0) || null)
const showCheckWorkspace = computed(() => props.viewMode === 'all' || props.viewMode === 'check')
const showApprovalWorkspace = computed(() => props.viewMode === 'all' || props.viewMode === 'approval')
const reviewPanelEyebrow = computed(() => {
  if (props.viewMode === 'check') return 'AI Check Results'
  if (props.viewMode === 'approval') return 'Final Review'
  return 'Review Requests'
})
const reviewPanelTitle = computed(() => {
  if (props.viewMode === 'check') return 'AI 검수 자료'
  if (props.viewMode === 'approval') return '승인 대기 자료'
  return '승인 요청'
})

const ISSUE_HIGHLIGHT_RULES = [
  {
    tone: 'danger',
    icon: '🚫',
    label: '금지/반려 위험',
    keywords: ['제출 반려', '반려 대상', '사용 불가', '사용 금지', '불법', '허위', '기만'],
  },
  {
    tone: 'warning',
    icon: '⚠️',
    label: '중요 확인',
    keywords: ['한 번에 해결', '100%', '1위', '오해', '주의', '단정', '단정적', '과장', '보장', '확정', '절대', '최고', '유일', '소비자', '경고', '위험', '법적', '금지'],
  },
  {
    tone: 'recommend',
    icon: '💡',
    label: '추천 수정',
    keywords: ['수정', '제안', '완화', '근거', '보완', '추가', '대체', '추천', '표기'],
  },
]

const HIGHLIGHT_KEYWORDS = ISSUE_HIGHLIGHT_RULES
  .flatMap((rule) => rule.keywords)
  .sort((left, right) => right.length - left.length)

const HIGHLIGHT_PATTERN = new RegExp(
  `(${HIGHLIGHT_KEYWORDS.map(escapeRegExp).join('|')})`,
  'gi',
)

const normalizedAnalysisStatus = computed(() => normalizeAnalysisStatus(analysisResult.value?.status))
const analysisVerdict = computed(() =>
  analysisResult.value
    ? getAdCheckVerdict({
      ...analysisResult.value,
      status: normalizedAnalysisStatus.value,
    })
    : null,
)
const analysisVerdictClass = computed(() => {
  if (!analysisFileInfo.value || isAnalyzing.value) return 'analysis-verdict--empty'
  if (analysisError.value) return 'analysis-verdict--danger'
  return analysisVerdict.value ? `analysis-verdict--${analysisVerdict.value.tone}` : 'analysis-verdict--empty'
})
const activeAnalysisJob = computed(() =>
  activeAnalysisJobId.value ? adCheckJobsStore.findJob(activeAnalysisJobId.value) : null,
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
  const verdict = analysisVerdict.value ?? getAdCheckVerdict(analysisResult.value)
  const highlightSource = [
    violationText,
    issueSections.reason,
    issueSections.suggestion,
  ].join(' ')
  return [{
    title: verdict.title,
    verdict,
    source: law || 'AI 검수',
    target: violationText || '',
    targetTokens: tokenizeIssueText(violationText),
    reason: issueSections.reason,
    reasonBlocks: issueSections.reasonBlocks,
    suggestion: issueSections.suggestion,
    suggestionItems: issueSections.suggestionItems,
    highlightBadges: buildIssueHighlightBadges(highlightSource),
  }]
})

const analysisProcessingTimes = computed(() => analysisResult.value?.processingTimes ?? null)
const adCheckSummaries = computed(() => adCheckJobsStore.jobSummaries ?? [])
const selectedAdCheckSummary = computed(() =>
  adCheckSummaries.value.find((summary) => summary.jobId === selectedDetailJobId.value) ?? null,
)
const selectedJobDetail = computed(() =>
  selectedDetailJobId.value ? adCheckJobsStore.findJobDetail(selectedDetailJobId.value) : null,
)
const isSelectedDetailLoading = computed(() =>
  Boolean(selectedDetailJobId.value && adCheckJobsStore.detailLoadingJobId === selectedDetailJobId.value),
)
const adCheckTotalPages = computed(() =>
  Math.max(1, Math.ceil(adCheckSummaries.value.length / AD_CHECK_RESULT_PAGE_SIZE)),
)
const paginatedAdCheckSummaries = computed(() => {
  const start = (adCheckResultPage.value - 1) * AD_CHECK_RESULT_PAGE_SIZE
  return adCheckSummaries.value.slice(start, start + AD_CHECK_RESULT_PAGE_SIZE)
})
const requestedReviewJobIds = computed(() =>
  new Set(reviewRequests.value.map((request) => String(request.adCheckJobId || '')).filter(Boolean)),
)

const canCreateReviewRequest = computed(() =>
  Boolean(
    analysisFileInfo.value
    && props.campaignId
    && canRequestReview.value
    && activeAnalysisJobId.value
    && analysisResult.value?.fileObjectKey
    && !analysisError.value
    && !isAnalyzing.value
    && !isSubmittingReviewRequest.value,
  ),
)

function normalizeAnalysisStatus(status) {
  return normalizeAdCheckResultStatus(status)
}

function buildIssueSections(reason, suggestion) {
  const parsedReason = splitSuggestionMarker(reason)
  const cleanSuggestion = normalizeDisplayText(suggestion) || parsedReason.suggestion
  const cleanReason = parsedReason.reason

  return {
    reason: cleanReason,
    reasonBlocks: splitReasonBlocks(cleanReason),
    suggestion: cleanSuggestion,
    suggestionItems: splitSuggestionItems(cleanSuggestion).map((item) => ({
      text: item,
      tokens: tokenizeIssueText(item),
    })),
  }
}

function splitSuggestionMarker(value) {
  const text = normalizeDisplayText(value)
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
  const text = normalizeDisplayText(value)
    .replace(/^수정\s*제안\s*[:：]\s*/, '')
    .replace(/\s+예[:：]\s*/g, '\n예: ')
    .replace(/\s+또는\s+/g, '\n또는 ')
    .trim()
  if (!text) return []

  const bulletParts = text
    .split(/\n+|(?:^|\s)(?:\d+[.)]|[-*•])\s+/)
    .map((item) => item.trim())
    .filter(Boolean)

  if (bulletParts.length > 1) return bulletParts

  const sentenceParts = text
    .match(/[^.。]+(?:[.。]+|$)/g)
    ?.map((item) => item.trim())
    .filter(Boolean) ?? []

  return sentenceParts.length > 1 ? sentenceParts : [text]
}

function splitReasonBlocks(value) {
  const text = normalizeDisplayText(value)
  if (!text) return []

  return text
    .split(/\n+|(?<=[.。])\s+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => ({
      text: item,
      tokens: tokenizeIssueText(item),
    }))
}

function normalizeDisplayText(value) {
  return String(value ?? '')
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((line) => line.replace(/[ \t]+/g, ' ').trim())
    .filter(Boolean)
    .join('\n')
}

function normalizeText(value) {
  return String(value ?? '').replace(/\s+/g, ' ').trim()
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function resolveHighlightTone(text) {
  const normalized = String(text ?? '').toLowerCase()
  const rule = ISSUE_HIGHLIGHT_RULES.find((item) =>
    item.keywords.some((keyword) => normalized.includes(keyword.toLowerCase())),
  )
  return rule?.tone ?? ''
}

function tokenizeIssueText(value) {
  const text = normalizeDisplayText(value)
  if (!text) return []

  const tokens = []
  let lastIndex = 0
  HIGHLIGHT_PATTERN.lastIndex = 0

  for (const match of text.matchAll(HIGHLIGHT_PATTERN)) {
    const index = match.index ?? 0
    if (index > lastIndex) {
      tokens.push({ text: text.slice(lastIndex, index), tone: '' })
    }

    const matchedText = match[0]
    tokens.push({
      text: matchedText,
      tone: resolveHighlightTone(matchedText),
    })
    lastIndex = index + matchedText.length
  }

  if (lastIndex < text.length) {
    tokens.push({ text: text.slice(lastIndex), tone: '' })
  }

  return tokens
}

function buildIssueHighlightBadges(value) {
  const text = normalizeText(value).toLowerCase()
  return ISSUE_HIGHLIGHT_RULES
    .filter((rule) => rule.keywords.some((keyword) => text.includes(keyword.toLowerCase())))
    .map(({ tone, icon, label }) => ({ tone, icon, label }))
}

function analysisStatusLabel(status) {
  if (!status) return '대기'
  return getAdCheckVerdict({
    ...analysisResult.value,
    status,
  }).label
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

function adCheckVerdictOf(item) {
  return getAdCheckDisplayVerdict(item)
}

function adCheckVerdictChipClass(item) {
  return `status-chip status-chip--${adCheckVerdictOf(item).tone}`
}

function formatDate(value) {
  if (!value) return '요청일 없음'
  return String(value).slice(0, 10)
}

function formatDateTime(value) {
  if (!value) return '기록 없음'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function formatFileSize(bytes) {
  const size = Number(bytes)
  if (!Number.isFinite(size) || size <= 0) return '-'
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / 1024 / 1024).toFixed(1)}MB`
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

function summaryThumbnail(summary) {
  if (summary?.thumbnailUrl) return summary.thumbnailUrl
  if (String(summary?.fileContentType || '').startsWith('image/')) return summary.fileUrl || ''
  return ''
}

function requesterLabel(item) {
  const name = String(item?.requesterName ?? '').trim()
  const loginId = String(item?.requesterLoginId ?? '').trim()
  const organization = String(item?.requesterOrganizationName ?? '').trim()
  const primary = name || loginId || '요청자'
  return organization ? `${primary} · ${organization}` : primary
}

function isMyAdCheckSummary(summary) {
  return Boolean(
    summary?.requesterId
    && currentUserIdx.value
    && Number(summary.requesterId) === Number(currentUserIdx.value),
  )
}

function canRequestFinalReviewFromSummary(summary) {
  return Boolean(
    canRequestReview.value
    && isMyAdCheckSummary(summary)
    && String(summary?.status || '').toUpperCase() === 'SUCCEEDED'
    && !requestedReviewJobIds.value.has(String(summary.jobId || ''))
  )
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
    reviewLoadError.value = error?.message ?? '승인 요청 목록을 불러오지 못했습니다.'
  }
}

async function createReviewRequest() {
  requestSubmitError.value = ''

  if (!activeAnalysisJobId.value) {
    requestSubmitError.value = 'AI 검수 완료 자료를 먼저 선택해주세요.'
    return
  }

  if (!analysisResult.value?.fileObjectKey) {
    requestSubmitError.value = '업로드된 파일 정보를 확인할 수 없습니다.'
    return
  }

  isSubmittingReviewRequest.value = true
  try {
    const result = await CreateAdReviewRequest(props.campaignId, {
      jobId: activeAnalysisJobId.value,
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
      verdictLevel: analysisResult.value.verdictLevel,
      mongoDocumentId: analysisResult.value.analysisJobId,
      requestMemo: reviewRequestMemo.value.trim() || null,
    })

    replaceReviewRequest(result)
    resetAnalysisForm()
    closeAnalysisRequest()
  } catch (error) {
    requestSubmitError.value = error?.message ?? '승인 요청 생성에 실패했습니다.'
  } finally {
    isSubmittingReviewRequest.value = false
  }
}

async function createReviewRequestFromSummary(summary) {
  if (!canRequestFinalReviewFromSummary(summary) || submittingReviewJobId.value) return

  submittingReviewJobId.value = summary.jobId
  requestSubmitError.value = ''
  try {
    const result = await CreateAdReviewRequest(props.campaignId, {
      jobId: summary.jobId,
      requestMemo: reviewRequestMemo.value.trim() || null,
    })
    replaceReviewRequest(result)
    await loadReviewRequests()
  } catch (error) {
    requestSubmitError.value = error?.message ?? '승인 요청 생성에 실패했습니다.'
  } finally {
    submittingReviewJobId.value = ''
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
  if (!canUseAiJudge.value || !showCheckWorkspace.value) return
  isAnalysisOpen.value = true
}

function closeAnalysisRequest() {
  isAnalysisOpen.value = false
  if (route.query.adCheckJobId) {
    router.replace({
      query: {
        ...route.query,
        adCheckJobId: undefined,
      },
    })
  }
}

async function loadAdCheckSummaries() {
  if (!props.campaignId) return
  await adCheckJobsStore.loadJobSummaries({ campaignId: props.campaignId })
}

function resetAnalysisForm() {
  selectedAnalysisFile.value = null
  activeAnalysisJobId.value = ''
  isAnalyzing.value = false
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

function applyAnalysisJobResult(job) {
  if (!job) return

  if (job.status === 'SUCCEEDED') {
    if (!job.result) {
      analysisError.value = ''
      isAnalyzing.value = true
      void loadAnalysisJobDetail(job)
      return
    }

    analysisResult.value = job.result
    analysisError.value = normalizeAnalysisStatus(job.result?.status)
      ? ''
      : 'AI 검수 결과 형식이 올바르지 않습니다. 서버 응답을 확인해주세요.'
    isAnalyzing.value = false
    void loadAdCheckSummaries()
    return
  }

  if (job.status === 'FAILED') {
    if (job.result) {
      analysisResult.value = job.result
    }
    analysisError.value = job.errorMessage || 'AI 검수 요청에 실패했습니다.'
    isAnalyzing.value = false
    void loadAdCheckSummaries()
    return
  }

  if (job.status === 'CANCELED') {
    analysisError.value = job.errorMessage || '대기 중 검수 작업이 취소되었습니다.'
    isAnalyzing.value = false
    return
  }

  if (analysisResult.value && activeAnalysisJobId.value === job.jobId) {
    isAnalyzing.value = false
    return
  }

  analysisError.value = ''
  isAnalyzing.value = true
}

function openAnalysisJobResult(job) {
  if (job?.campaignId && job?.jobId) {
    router.push({
      name: 'campaign-detail',
      params: {
        campaignId: job.campaignId,
      },
      query: {
        tab: 'review',
        reviewTab: 'library',
        adCheckJobId: job.jobId,
      },
    })
    return
  }

  const targetUrl = job?.targetUrl
    || (job?.result?.analysisJobId
      ? `/references?analysisJobId=${encodeURIComponent(job.result.analysisJobId)}`
      : '')

  if (targetUrl) {
    router.push(targetUrl)
  }
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
  let directJob = null
  try {
    const directStart = await adCheckJobsStore.startDirectJob(file, { campaignId: props.campaignId })
    directJob = directStart?.job ?? null
    activeAnalysisJobId.value = directJob?.jobId ?? ''
    applyAnalysisJobResult(directJob)

    const result = await CheckCampaignAdFileWithAiJudge(props.campaignId, file, {
      context: directStart?.context ?? {},
    })
    analysisResult.value = result
    analysisError.value = normalizeAnalysisStatus(result?.status)
      ? ''
      : 'AI 검수 결과 형식이 올바르지 않습니다. 서버 응답을 확인해주세요.'
    isAnalyzing.value = false
    if (directJob?.jobId) {
      await adCheckJobsStore.fetchJob(directJob.jobId).catch(() => null)
    }
    isAnalyzing.value = false
    void loadAdCheckSummaries()
  } catch (error) {
    if (error?.data && typeof error.data === 'object') {
      analysisResult.value = error.data
    }
    analysisError.value = error?.message ?? 'AI 검수 요청에 실패했습니다.'
    isAnalyzing.value = false
    if (directJob?.jobId) {
      await adCheckJobsStore.fetchJob(directJob.jobId).catch(() => null)
      isAnalyzing.value = false
      void loadAdCheckSummaries()
    }
  }
}

async function openAnalysisJobFromRoute() {
  if (!showCheckWorkspace.value) return

  const jobId = String(route.query.adCheckJobId || '').trim()
  if (!jobId) {
    return
  }

  isAnalysisOpen.value = true
  activeAnalysisJobId.value = jobId

  let job = adCheckJobsStore.findJob(jobId)
  if (!job) {
    try {
      job = await adCheckJobsStore.fetchJob(jobId)
    } catch (error) {
      analysisError.value = error?.message ?? '진행 중인 검수 작업을 불러오지 못했습니다.'
      isAnalyzing.value = false
      return
    }
  }

  applyAnalysisJobResult(job)
}

async function loadAnalysisJobDetail(job) {
  if (!job?.jobId || analysisDetailLoadingJobId.value === job.jobId) {
    return
  }

  analysisDetailLoadingJobId.value = job.jobId
  try {
    const detail = await adCheckJobsStore.loadJobDetail(job.jobId)
    analysisResult.value = detail?.detail ?? null
    analysisError.value = normalizeAnalysisStatus(analysisResult.value?.status)
      ? ''
      : adCheckJobsStore.detailLoadError || '상세 자료를 불러오지 못했습니다.'
    void loadAdCheckSummaries()
  } finally {
    isAnalyzing.value = false
    analysisDetailLoadingJobId.value = ''
  }
}

async function openAdCheckDetail(summary) {
  if (!summary?.jobId) {
    return
  }

  selectedDetailJobId.value = summary.jobId
  if (!adCheckJobsStore.findJobDetail(summary.jobId)) {
    await adCheckJobsStore.loadJobDetail(summary.jobId)
  }
}

function closeAdCheckDetail() {
  selectedDetailJobId.value = ''
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
    loadAdCheckSummaries(),
  ])
}

onMounted(async () => {
  await loadPageData()
  await openAnalysisJobFromRoute()
})

watch(activeAnalysisJob, (job) => {
  if (job) {
    applyAnalysisJobResult(job)
  }
})

watch(adCheckSummaries, () => {
  if (adCheckResultPage.value > adCheckTotalPages.value) {
    adCheckResultPage.value = adCheckTotalPages.value
  }
})

watch(
  () => props.campaignId,
  () => {
    resetAnalysisForm()
    isAnalysisOpen.value = false
    reviewRequests.value = []
    selectedDetailJobId.value = ''
    adCheckResultPage.value = 1
    loadPageData()
  },
)

watch(
  () => props.viewMode,
  () => {
    if (!showCheckWorkspace.value) {
      isAnalysisOpen.value = false
      resetAnalysisForm()
    }
    if (showCheckWorkspace.value) {
      void openAnalysisJobFromRoute()
    }
  },
)

watch(
  () => route.query.adCheckJobId,
  () => {
    void openAnalysisJobFromRoute()
  },
)
</script>

<template>
  <section class="review-page">
    <section v-if="isAnalysisOpen && showCheckWorkspace" class="review-panel">
      <header class="review-panel__head">
        <div>
          <p>AI Risk Review</p>
          <h3>AI 검수 요청 생성</h3>
        </div>
        <button type="button" class="ghost-button" @click="closeAnalysisRequest">목록으로</button>
      </header>

      <section v-if="activeAnalysisJob" class="analysis-job-stage">
        <AdCheckJobProgress :job="activeAnalysisJob" />
        <button
          v-if="isTerminalJobStatus(activeAnalysisJob.status)
            && (activeAnalysisJob.targetUrl || activeAnalysisJob.result?.analysisJobId)"
          type="button"
          class="ghost-button"
          @click="openAnalysisJobResult(activeAnalysisJob)"
        >
          검수 결과 보기
        </button>
      </section>

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
              placeholder="PM에게 전달할 승인 요청 내용을 입력하세요."
            />
          </label>
        </section>

        <aside class="analysis-result">
          <article
            class="analysis-verdict"
            :class="analysisVerdictClass"
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
                      : analysisVerdict?.label || analysisStatusLabel(normalizedAnalysisStatus)
              }}
            </strong>
            <p v-if="analysisError">{{ analysisError }}</p>
            <p v-else-if="analysisVerdict">
              {{ analysisVerdict.description }} {{ analysisVerdict.guidance }}
            </p>
            <p v-else>파일을 업로드하면 검수 결과가 표시됩니다.</p>

            <ol
              v-if="analysisFileInfo && !isAnalyzing && !analysisError && analysisVerdict?.level"
              class="verdict-scale"
              aria-label="AI 검수 판단 등급"
            >
              <li
                v-for="level in AD_CHECK_VERDICT_LEVELS"
                :key="level.key"
                :class="[
                  `verdict-scale__item--${level.tone}`,
                  {
                    'is-active': analysisVerdict.key === level.key,
                    'is-before': level.level < analysisVerdict.level,
                  },
                ]"
              >
                <span>{{ level.level }}</span>
                <strong>{{ level.label }}</strong>
              </li>
            </ol>
          </article>

          
          <div v-if="analysisIssues.length" class="issue-list">
            <article
              v-for="issue in analysisIssues"
              :key="issue.title"
              class="issue-card"
              :class="`issue-card--${issue.verdict.tone}`"
            >
              <span>{{ issue.source }}</span>
              <strong>{{ issue.title }}</strong>
              <p class="issue-card__verdict">
                {{ issue.verdict.description }} {{ issue.verdict.guidance }}
              </p>

              <div v-if="issue.highlightBadges.length" class="issue-alert-badges">
                <span
                  v-for="badge in issue.highlightBadges"
                  :key="badge.label"
                  :class="`issue-alert-badge issue-alert-badge--${badge.tone}`"
                >
                  <b>{{ badge.icon }}</b>
                  {{ badge.label }}
                </span>
              </div>

              <blockquote v-if="issue.target" class="issue-target">
                <span
                  v-for="(token, index) in issue.targetTokens"
                  :key="`${token.text}-${index}`"
                  :class="token.tone ? `issue-highlight issue-highlight--${token.tone}` : ''"
                >
                  {{ token.text }}
                </span>
              </blockquote>

              <div v-if="issue.reason" class="issue-section">
                <span>위반 사유</span>
                <p v-for="block in issue.reasonBlocks" :key="block.text">
                  <span
                    v-for="(token, index) in block.tokens"
                    :key="`${token.text}-${index}`"
                    :class="token.tone ? `issue-highlight issue-highlight--${token.tone}` : ''"
                  >
                    {{ token.text }}
                  </span>
                </p>
              </div>

              <div v-if="issue.suggestion" class="issue-section issue-section--suggestion">
                <span>수정 제안</span>
                <ol class="suggestion-list">
                  <li v-for="item in issue.suggestionItems" :key="item.text">
                    <p>
                      <span
                        v-for="(token, index) in item.tokens"
                        :key="`${token.text}-${index}`"
                        :class="token.tone ? `issue-highlight issue-highlight--${token.tone}` : ''"
                      >
                        {{ token.text }}
                      </span>
                    </p>
                  </li>
                </ol>
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
            {{ isSubmittingReviewRequest ? '요청 중...' : '승인 요청하기' }}
          </button>
        </aside>
      </div>
    </section>

    <section v-else class="review-panel">
      <header class="review-panel__head">
        <div>
          <p>{{ reviewPanelEyebrow }}</p>
          <h3>{{ reviewPanelTitle }}</h3>
        </div>
        <button v-if="canUseAiJudge && showCheckWorkspace" type="button" class="primary-button" @click="openAnalysisRequest">
          AI 검수 요청하기
        </button>
      </header>

      <p v-if="memberContextError" class="form-error">{{ memberContextError }}</p>
      <p v-if="reviewLoadError" class="form-error">{{ reviewLoadError }}</p>
      <p v-if="reviewDecisionError" class="form-error">{{ reviewDecisionError }}</p>
      <p v-if="requestSubmitError" class="form-error">{{ requestSubmitError }}</p>

      <section v-if="showCheckWorkspace" class="ad-check-results">
        <header class="ad-check-results__head">
          <div>
            <p>AI Check Results</p>
            <h4>AI 검수 자료</h4>
          </div>
          <button type="button" class="ghost-button" @click="loadAdCheckSummaries">
            새로고침
          </button>
        </header>

        <p v-if="adCheckJobsStore.summaryLoadError" class="form-error">
          {{ adCheckJobsStore.summaryLoadError }}
        </p>

        <div v-if="adCheckJobsStore.isLoadingSummaries" class="ad-check-summary-list">
          <article v-for="index in 2" :key="index" class="ad-check-summary-card ad-check-summary-card--loading">
            <span></span>
            <strong></strong>
            <p></p>
          </article>
        </div>

        <div v-else-if="adCheckSummaries.length" class="ad-check-summary-list">
          <article
            v-for="summary in paginatedAdCheckSummaries"
            :key="summary.jobId"
            class="ad-check-summary-card"
          >
            <figure class="ad-check-summary-card__preview">
              <img v-if="summaryThumbnail(summary)" :src="summaryThumbnail(summary)" :alt="`${summary.fileName} 미리보기`" />
              <figcaption v-else>Preview</figcaption>
            </figure>

            <div class="ad-check-summary-card__main">
              <span>{{ adCheckStatusLabel(summary.status) }}</span>
              <strong>{{ summary.fileName }}</strong>
              <p>{{ summary.summaryMessage || '검수 결과 요약을 준비 중입니다.' }}</p>
            </div>

            <dl class="ad-check-summary-card__meta">
              <div>
                <dt>판단 등급</dt>
                <dd>
                  <em :class="adCheckVerdictChipClass(summary)">
                    {{ adCheckVerdictOf(summary).title }}
                  </em>
                </dd>
              </div>
              <div>
                <dt>업로드</dt>
                <dd>{{ requesterLabel(summary) }}</dd>
              </div>
              <div>
                <dt>조치</dt>
                <dd>{{ adCheckVerdictOf(summary).guidance }}</dd>
              </div>
              <div>
                <dt>요청</dt>
                <dd>{{ formatDateTime(summary.createdAt) }}</dd>
              </div>
              <div>
                <dt>완료</dt>
                <dd>{{ formatDateTime(summary.finishedAt) }}</dd>
              </div>
            </dl>

            <div class="ad-check-summary-card__actions">
              <button
                v-if="canRequestFinalReviewFromSummary(summary)"
                type="button"
                class="primary-button"
                :disabled="submittingReviewJobId === summary.jobId"
                @click="createReviewRequestFromSummary(summary)"
              >
                {{ submittingReviewJobId === summary.jobId ? '요청 중...' : '승인 요청하기' }}
              </button>
              <button
                type="button"
                class="ghost-button"
                :disabled="adCheckJobsStore.detailLoadingJobId === summary.jobId"
                @click="openAdCheckDetail(summary)"
              >
                {{ adCheckJobsStore.detailLoadingJobId === summary.jobId ? '불러오는 중...' : '상세 보기' }}
              </button>
            </div>
          </article>
        </div>

        <nav v-if="adCheckSummaries.length && adCheckTotalPages > 1" class="ad-check-pagination" aria-label="AI 검수 자료 페이지">
          <button type="button" :disabled="adCheckResultPage === 1" @click="adCheckResultPage -= 1">
            이전
          </button>
          <button
            v-for="page in adCheckTotalPages"
            :key="page"
            type="button"
            :class="{ active: adCheckResultPage === page }"
            @click="adCheckResultPage = page"
          >
            {{ page }}
          </button>
          <button
            type="button"
            :disabled="adCheckResultPage === adCheckTotalPages"
            @click="adCheckResultPage += 1"
          >
            다음
          </button>
        </nav>

        <article
          v-if="!adCheckJobsStore.isLoadingSummaries && !adCheckSummaries.length"
          class="empty-state empty-state--compact"
        >
          <strong>저장된 AI 검수 자료가 없습니다.</strong>
          <p>파일 검수를 완료하면 결과 요약이 이곳에 표시됩니다.</p>
        </article>
      </section>

      <div v-if="showApprovalWorkspace && reviewRequests.length" class="review-list">
        <article v-for="request in reviewRequests" :key="request.idx" class="review-card">
          <div class="review-card__main">
            <span class="review-card__type">검수</span>
            <strong>{{ request.fileName ?? '광고 소재 승인 요청' }}</strong>
            <p>{{ request.requestMemo || 'AI 1차 검수 후 생성된 승인 요청입니다.' }}</p>
          </div>

          <div class="review-card__meta">
            <span>{{ requesterLabel(request) }}</span>
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

      <article v-else-if="showApprovalWorkspace" class="empty-state">
        <strong>등록된 승인 요청이 없습니다.</strong>
        <p>협력사가 AI 1차 검수 자료를 승인 요청하면 이곳에 표시됩니다.</p>
      </article>
    </section>

    <AdCheckDetailModal
      v-if="selectedDetailJobId"
      :summary="selectedJobDetail?.summary || selectedAdCheckSummary"
      :detail="selectedJobDetail"
      :loading="isSelectedDetailLoading"
      :error-message="adCheckJobsStore.detailLoadError"
      @close="closeAdCheckDetail"
    />
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

.ad-check-results {
  display: grid;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-color);
}

.ad-check-results__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.ad-check-results__head p,
.ad-check-summary-card__main span,
.ad-check-summary-card__meta dt {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.ad-check-results__head h4 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 950;
}

.ad-check-summary-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.ad-check-summary-card {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  padding: 13px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.ad-check-summary-card__preview {
  display: grid;
  width: 96px;
  height: 82px;
  overflow: hidden;
  place-items: center;
  grid-row: 1 / span 3;
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
}

.ad-check-summary-card__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ad-check-summary-card__preview figcaption {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 850;
}

.ad-check-summary-card__main {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.ad-check-summary-card__main strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ad-check-summary-card__main p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.45;
}

.ad-check-summary-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.ad-check-summary-card__meta div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.ad-check-summary-card__meta dd {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ad-check-summary-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-self: end;
}

.ad-check-pagination {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.ad-check-pagination button {
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

.ad-check-pagination button.active {
  border-color: color-mix(in srgb, var(--color-primary-500) 34%, var(--border-color));
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.ad-check-pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.ad-check-summary-card--loading span,
.ad-check-summary-card--loading strong,
.ad-check-summary-card--loading p {
  display: block;
  height: 12px;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--border-color) 80%, var(--panel-color));
}

.ad-check-summary-card--loading strong {
  width: 62%;
  height: 16px;
}

.ad-check-summary-card--loading p {
  width: 82%;
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
.ghost-button:disabled,
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

.analysis-verdict--empty {
  border-color: var(--border-color);
}

.analysis-verdict--neutral,
.issue-card--neutral {
  border-color: var(--border-color);
  background: var(--panel-muted);
}

.analysis-verdict--pass,
.issue-card--pass {
  border-color: color-mix(in srgb, #10b981 42%, var(--border-color));
  background: color-mix(in srgb, #10b981 12%, var(--panel-color));
}

.analysis-verdict--recheck,
.issue-card--recheck {
  border-color: color-mix(in srgb, #84cc16 42%, var(--border-color));
  background: color-mix(in srgb, #84cc16 12%, var(--panel-color));
}

.analysis-verdict--suggestion,
.issue-card--suggestion {
  border-color: color-mix(in srgb, #f59e0b 42%, var(--border-color));
  background: color-mix(in srgb, #f59e0b 12%, var(--panel-color));
}

.analysis-verdict--revision,
.issue-card--revision {
  border-color: color-mix(in srgb, #f97316 46%, var(--border-color));
  background: color-mix(in srgb, #f97316 13%, var(--panel-color));
}

.analysis-verdict--danger,
.issue-card--danger {
  border-color: color-mix(in srgb, var(--color-danger) 46%, var(--border-color));
  background: var(--danger-surface);
}

.verdict-scale {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  margin: 6px 0 0;
  padding: 0;
  list-style: none;
}

.verdict-scale li {
  display: grid;
  min-width: 0;
  gap: 4px;
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-secondary);
  opacity: 0.62;
}

.verdict-scale li.is-before {
  opacity: 0.78;
}

.verdict-scale li.is-active {
  border-color: currentColor;
  box-shadow: inset 0 0 0 1px currentColor;
  opacity: 1;
}

.verdict-scale span {
  display: inline-grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: var(--radius-full);
  background: currentColor;
  color: #fff;
  font-size: 11px;
  font-weight: 950;
}

.verdict-scale strong {
  overflow-wrap: anywhere;
  color: currentColor;
  font-size: 11px;
  line-height: 1.25;
}

.verdict-scale__item--pass {
  color: #059669;
}

.verdict-scale__item--recheck {
  color: #65a30d;
}

.verdict-scale__item--suggestion {
  color: #b45309;
}

.verdict-scale__item--revision {
  color: #c2410c;
}

.verdict-scale__item--danger {
  color: var(--color-danger-dark);
}

.analysis-job-stage {
  display: grid;
  justify-items: center;
  gap: 10px;
}

.analysis-job-stage :deep(.ad-check-progress) {
  width: 100%;
  max-width: 1040px;
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

.issue-alert-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.issue-alert-badge {
  display: inline-flex;
  min-height: 26px;
  align-items: center;
  gap: 5px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 950;
  padding: 0 10px;
}

.issue-alert-badge b {
  font-size: 14px;
  line-height: 1;
}

.issue-alert-badge--danger {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.issue-alert-badge--warning {
  background: color-mix(in srgb, #f59e0b 20%, transparent);
  color: #92400e;
}

.issue-alert-badge--recommend {
  background: color-mix(in srgb, #10b981 16%, transparent);
  color: #047857;
}

.issue-card blockquote {
  margin: 0;
  padding: 10px 12px;
  border-left: 4px solid var(--color-danger);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 950;
  line-height: 1.55;
}

.issue-section {
  display: grid;
  gap: 8px;
  padding: 11px;
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
.issue-section ol {
  margin: 0;
}

.issue-section p {
  white-space: pre-wrap;
}

.suggestion-list {
  display: grid;
  gap: 8px;
  padding: 0;
  counter-reset: suggestion;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  list-style: none;
}

.suggestion-list li {
  position: relative;
  display: grid;
  min-height: 34px;
  align-items: start;
  padding: 8px 10px 8px 38px;
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 20%, var(--border-color));
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  counter-increment: suggestion;
}

.suggestion-list li::before {
  position: absolute;
  top: 8px;
  left: 10px;
  display: inline-grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--color-primary-500);
  color: #fff;
  content: counter(suggestion);
  font-size: 11px;
  font-weight: 950;
}

.issue-highlight {
  display: inline;
  border-radius: 5px;
  box-decoration-break: clone;
  font-weight: 950;
  padding: 1px 4px;
  -webkit-box-decoration-break: clone;
}

.issue-highlight--danger {
  background: color-mix(in srgb, var(--color-danger) 20%, transparent);
  color: var(--color-danger-dark);
}

.issue-highlight--warning {
  background: color-mix(in srgb, #f59e0b 22%, transparent);
  color: #92400e;
}

.issue-highlight--recommend {
  background: color-mix(in srgb, #10b981 18%, transparent);
  color: #047857;
}

.issue-section--suggestion {
  border-color: color-mix(in srgb, var(--color-primary-500) 28%, var(--border-color));
  background: color-mix(in srgb, var(--color-primary-100) 48%, var(--panel-color));
}

:global(:root[data-theme='dark']) .verdict-scale__item--pass,
:global(:root[data-theme='dark']) .status-chip--pass,
:global(:root[data-theme='dark']) .issue-alert-badge--recommend,
:global(:root[data-theme='dark']) .issue-highlight--recommend {
  color: #6ee7b7;
}

:global(:root[data-theme='dark']) .verdict-scale__item--recheck,
:global(:root[data-theme='dark']) .status-chip--recheck {
  color: #bef264;
}

:global(:root[data-theme='dark']) .verdict-scale__item--suggestion,
:global(:root[data-theme='dark']) .status-chip--suggestion,
:global(:root[data-theme='dark']) .issue-alert-badge--warning,
:global(:root[data-theme='dark']) .issue-highlight--warning {
  color: #fcd34d;
}

:global(:root[data-theme='dark']) .verdict-scale__item--revision,
:global(:root[data-theme='dark']) .status-chip--revision {
  color: #fdba74;
}

:global(:root[data-theme='dark']) .verdict-scale__item--danger,
:global(:root[data-theme='dark']) .status-chip--danger,
:global(:root[data-theme='dark']) .issue-alert-badge--danger,
:global(:root[data-theme='dark']) .issue-highlight--danger {
  color: #fca5a5;
}

:global(:root[data-theme='dark']) .analysis-verdict--pass,
:global(:root[data-theme='dark']) .issue-card--pass {
  background: color-mix(in srgb, #10b981 12%, var(--panel-color));
}

:global(:root[data-theme='dark']) .analysis-verdict--recheck,
:global(:root[data-theme='dark']) .issue-card--recheck {
  background: color-mix(in srgb, #84cc16 12%, var(--panel-color));
}

:global(:root[data-theme='dark']) .analysis-verdict--suggestion,
:global(:root[data-theme='dark']) .issue-card--suggestion {
  background: color-mix(in srgb, #f59e0b 12%, var(--panel-color));
}

:global(:root[data-theme='dark']) .analysis-verdict--revision,
:global(:root[data-theme='dark']) .issue-card--revision {
  background: color-mix(in srgb, #f97316 13%, var(--panel-color));
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

.status-chip--neutral {
  background: var(--panel-color);
  color: var(--text-secondary);
}

.status-chip--pass {
  background: color-mix(in srgb, #10b981 16%, transparent);
  color: #047857;
}

.status-chip--recheck {
  background: color-mix(in srgb, #84cc16 18%, transparent);
  color: #4d7c0f;
}

.status-chip--suggestion {
  background: color-mix(in srgb, #f59e0b 18%, transparent);
  color: #92400e;
}

.status-chip--revision {
  background: color-mix(in srgb, #f97316 18%, transparent);
  color: #9a3412;
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

.empty-state--compact {
  padding: 18px;
}

@media (max-width: 1180px) {
  .analysis-layout,
  .review-card,
  .ad-check-summary-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .review-panel__head,
  .ad-check-results__head,
  .review-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .ad-check-summary-card__meta {
    grid-template-columns: 1fr;
  }

  .ad-check-summary-card {
    grid-template-columns: 1fr;
  }

  .ad-check-summary-card__preview {
    width: 100%;
    height: 140px;
    grid-row: auto;
  }

  .ad-check-summary-card__actions {
    justify-self: stretch;
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
