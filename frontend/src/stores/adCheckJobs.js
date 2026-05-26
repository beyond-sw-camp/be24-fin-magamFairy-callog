import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  CancelAdCheckJob,
  CreateAdCheckJob,
  DeleteAdCheckJob,
  GetAdCheckJob,
  GetAdCheckJobDetail,
  ListAdCheckJobs,
  ListActiveAdCheckJobs,
} from '@/api/adcheck/index.js'
import { normalizeAdCheckVerdictLevel } from '@/utils/adCheckVerdict'

export const AD_CHECK_JOB_STEPS = [
  {
    key: 'QUEUED',
    order: 0,
    label: '대기 중',
    currentLabel: '대기 중',
    doneLabel: '작업 시작',
    message: '앞선 검수 작업을 기다리고 있습니다.',
    currentMessage: '앞선 검수 작업을 기다리고 있습니다.',
    doneMessage: '작업이 시작되었습니다.',
    pendingMessage: '요청 후 순서대로 시작됩니다.',
    progressPercent: 5,
  },
  {
    key: 'DATA_EXTRACTION',
    order: 1,
    label: '데이터 추출',
    currentLabel: '데이터 추출 중',
    doneLabel: '데이터 추출 완료',
    message: '분석 서버에서 파일 데이터를 추출하고 있습니다.',
    currentMessage: '분석 서버에서 파일 데이터를 추출하고 있습니다.',
    doneMessage: '데이터 추출이 완료되었습니다.',
    pendingMessage: '작업이 시작되면 데이터를 추출합니다.',
    progressPercent: 20,
  },
  {
    key: 'DATA_ANALYSIS',
    order: 2,
    label: '데이터 분석',
    currentLabel: '데이터 분석 중',
    doneLabel: '데이터 분석 완료',
    message: 'AI 텍스트 분석을 진행하고 있습니다.',
    currentMessage: 'AI 텍스트 분석을 진행하고 있습니다.',
    doneMessage: '데이터 분석이 완료되었습니다.',
    pendingMessage: '데이터 추출 후 AI 분석을 진행합니다.',
    progressPercent: 35,
  },
  {
    key: 'RESULT_BUILDING',
    order: 3,
    label: '결과 도출',
    currentLabel: '결과 도출 중',
    doneLabel: '결과 도출 완료',
    message: '검수 결과를 정리하고 있습니다.',
    currentMessage: '검수 결과를 정리하고 있습니다.',
    doneMessage: '검수 결과 정리가 완료되었습니다.',
    pendingMessage: 'AI 분석 후 결과를 정리합니다.',
    progressPercent: 95,
  },
  {
    key: 'COMPLETED',
    order: 4,
    label: '완료',
    currentLabel: '검수 완료',
    doneLabel: '검수 완료',
    message: '검수가 완료되었습니다.',
    currentMessage: '검수가 완료되었습니다.',
    doneMessage: '검수가 완료되었습니다.',
    pendingMessage: '모든 단계 완료 후 표시됩니다.',
    progressPercent: 100,
  },
]

const ACTIVE_STATUSES = new Set(['QUEUED', 'RUNNING'])
const TERMINAL_STATUSES = new Set(['SUCCEEDED', 'FAILED', 'CANCELED'])
const JOB_POLL_INTERVAL_MS = 2500
const STEP_ALIASES = {
  REQUEST_RECEIVED: 'QUEUED',
  STARTED: 'DATA_EXTRACTION',
  TEXT_DETECTION: 'DATA_EXTRACTION',
  LAYOUT_ANALYSIS: 'DATA_EXTRACTION',
  OCR: 'DATA_EXTRACTION',
  N8N_ANALYSIS: 'DATA_ANALYSIS',
}

export function isActiveJobStatus(status) {
  return ACTIVE_STATUSES.has(String(status || '').toUpperCase())
}

export function isTerminalJobStatus(status) {
  return TERMINAL_STATUSES.has(String(status || '').toUpperCase())
}

export function adCheckStatusLabel(status) {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'QUEUED') return '대기 중'
  if (normalized === 'RUNNING') return '처리 중'
  if (normalized === 'SUCCEEDED') return '완료'
  if (normalized === 'FAILED') return '실패'
  if (normalized === 'CANCELED') return '취소'
  return '대기'
}

function normalizeDateValue(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toISOString()
}

function normalizeJob(rawJob) {
  const job = rawJob ?? {}
  const status = String(job.status || 'QUEUED').toUpperCase()
  const rawStep = String(job.currentStep || 'QUEUED').toUpperCase()
  const currentStep = STEP_ALIASES[rawStep] ?? rawStep
  const stepMeta = AD_CHECK_JOB_STEPS.find((step) => step.key === currentStep)
  const fallbackProgressPercent = status === 'SUCCEEDED'
    ? 100
    : stepMeta?.progressPercent ?? 0
  const rawProgressPercent = Number(job.progressPercent ?? fallbackProgressPercent)
  const progressPercent = isActiveJobStatus(status) && stepMeta
    ? stepMeta.progressPercent
    : rawProgressPercent

  return {
    ...job,
    jobId: String(job.jobId ?? ''),
    requesterId: job.requesterId ?? null,
    requesterLoginId: job.requesterLoginId ?? '',
    requesterName: job.requesterName ?? '',
    requesterOrganizationName: job.requesterOrganizationName ?? '',
    campaignId: job.campaignId ?? null,
    fileName: job.fileName ?? 'upload',
    status,
    currentStep,
    currentStepLabel: resolveCurrentStepLabel(status, stepMeta, job.currentStepLabel),
    currentStepMessage: resolveCurrentStepMessage(status, stepMeta, job.currentStepMessage),
    currentStepOrder: Number(stepMeta?.order ?? job.currentStepOrder ?? 0),
    progressPercent: Number.isFinite(progressPercent)
      ? Math.max(0, Math.min(100, progressPercent))
      : 0,
    errorMessage: job.errorMessage ?? '',
    verdictLevel: normalizeAdCheckVerdictLevel(job.verdictLevel ?? job.result?.verdictLevel ?? job.riskLevel),
    targetUrl: job.targetUrl ?? '',
    result: job.result ?? null,
    createdAt: normalizeDateValue(job.createdAt),
    updatedAt: normalizeDateValue(job.updatedAt),
    startedAt: normalizeDateValue(job.startedAt),
    finishedAt: normalizeDateValue(job.finishedAt),
  }
}

function normalizeJobSummary(rawSummary) {
  const summary = rawSummary ?? {}
  return {
    ...summary,
    jobId: String(summary.jobId ?? ''),
    requesterId: summary.requesterId ?? null,
    requesterLoginId: summary.requesterLoginId ?? '',
    requesterName: summary.requesterName ?? '',
    requesterOrganizationName: summary.requesterOrganizationName ?? '',
    campaignId: summary.campaignId ?? null,
    fileName: summary.fileName ?? 'upload',
    status: String(summary.status || 'QUEUED').toUpperCase(),
    resultStatus: String(summary.resultStatus || '').toLowerCase(),
    riskLevel: String(summary.riskLevel || '').toUpperCase(),
    verdictLevel: normalizeAdCheckVerdictLevel(summary.verdictLevel ?? summary.riskLevel),
    summaryMessage: summary.summaryMessage ?? '',
    mongoDocumentId: summary.mongoDocumentId ?? '',
    fileUrl: summary.fileUrl ?? '',
    fileContentType: summary.fileContentType ?? '',
    fileSize: summary.fileSize ?? null,
    thumbnailUrl: summary.thumbnailUrl ?? '',
    targetUrl: summary.targetUrl ?? '',
    createdAt: normalizeDateValue(summary.createdAt),
    updatedAt: normalizeDateValue(summary.updatedAt),
    finishedAt: normalizeDateValue(summary.finishedAt),
  }
}

function normalizeJobDetail(rawDetail) {
  const detail = rawDetail ?? {}
  return {
    ...detail,
    summary: normalizeJobSummary(detail.summary),
    mongoDocumentId: detail.mongoDocumentId ?? detail.summary?.mongoDocumentId ?? '',
    detail: detail.detail ?? null,
    verdictLevel: normalizeAdCheckVerdictLevel(
      detail.detail?.verdictLevel ?? detail.summary?.verdictLevel ?? detail.summary?.riskLevel,
    ),
    rawDocument: detail.rawDocument ?? {},
  }
}

function resolveCurrentStepLabel(status, stepMeta, fallbackLabel) {
  if (status === 'FAILED') {
    return fallbackLabel ?? '실패'
  }
  if (status === 'SUCCEEDED') {
    return stepMeta?.doneLabel ?? stepMeta?.label ?? fallbackLabel ?? '완료'
  }
  return stepMeta?.currentLabel ?? stepMeta?.label ?? fallbackLabel ?? '대기'
}

function resolveCurrentStepMessage(status, stepMeta, fallbackMessage) {
  if (status === 'FAILED') {
    return fallbackMessage ?? '검수 중 오류가 발생했습니다.'
  }
  if (status === 'SUCCEEDED') {
    return stepMeta?.doneMessage ?? stepMeta?.message ?? fallbackMessage ?? '검수가 완료되었습니다.'
  }
  return stepMeta?.currentMessage ?? stepMeta?.message ?? fallbackMessage ?? ''
}

function sortJobs(items) {
  return [...items].sort((left, right) => {
    const leftActive = isActiveJobStatus(left.status) ? 1 : 0
    const rightActive = isActiveJobStatus(right.status) ? 1 : 0
    if (leftActive !== rightActive) return rightActive - leftActive

    const leftRunning = left.status === 'RUNNING' ? 1 : 0
    const rightRunning = right.status === 'RUNNING' ? 1 : 0
    if (leftRunning !== rightRunning) return rightRunning - leftRunning

    return new Date(right.updatedAt || right.createdAt).getTime()
      - new Date(left.updatedAt || left.createdAt).getTime()
  })
}

export const useAdCheckJobsStore = defineStore('adCheckJobs', () => {
  const jobs = ref([])
  const jobSummaries = ref([])
  const jobDetailsById = ref({})
  const dismissedJobIds = ref(new Set())
  const isLoadingActive = ref(false)
  const isLoadingSummaries = ref(false)
  const detailLoadingJobId = ref('')
  const loadError = ref('')
  const summaryLoadError = ref('')
  const detailLoadError = ref('')
  const pollingTimers = new Map()

  const visibleJobs = computed(() =>
    jobs.value.filter((job) => !dismissedJobIds.value.has(job.jobId)),
  )
  const activeJobs = computed(() => visibleJobs.value.filter((job) => isActiveJobStatus(job.status)))
  const runningJobs = computed(() => activeJobs.value.filter((job) => job.status === 'RUNNING'))
  const queuedJobs = computed(() => activeJobs.value.filter((job) => job.status === 'QUEUED'))
  const terminalJobs = computed(() => visibleJobs.value.filter((job) => isTerminalJobStatus(job.status)))
  const floatingJob = computed(() => runningJobs.value[0] ?? queuedJobs.value[0] ?? terminalJobs.value[0] ?? null)

  function upsertJob(rawJob) {
    const job = normalizeJob(rawJob)
    if (!job.jobId) {
      return null
    }

    const nextJobs = jobs.value.filter((item) => item.jobId !== job.jobId)
    nextJobs.push(job)
    jobs.value = sortJobs(nextJobs)

    if (isActiveJobStatus(job.status)) {
      scheduleJobPoll(job.jobId)
    } else {
      clearJobPoll(job.jobId)
    }

    return job
  }

  function handleJobEvent(rawJob) {
    return upsertJob(rawJob)
  }

  async function startJob(file, options = {}) {
    const job = await CreateAdCheckJob(file, options)
    const normalizedJob = upsertJob(job)
    scheduleJobPoll(normalizedJob?.jobId)
    return normalizedJob
  }

  async function fetchJob(jobId) {
    const job = await GetAdCheckJob(jobId)
    return upsertJob(job)
  }

  async function loadJobSummaries(options = {}) {
    isLoadingSummaries.value = true
    summaryLoadError.value = ''

    try {
      const summaries = await ListAdCheckJobs(options)
      const normalizedSummaries = Array.isArray(summaries)
        ? summaries.map((summary) => normalizeJobSummary(summary)).filter((summary) => summary.jobId)
        : []
      jobSummaries.value = normalizedSummaries
      return normalizedSummaries
    } catch (error) {
      summaryLoadError.value = error?.message ?? '검수 자료 목록을 불러오지 못했습니다.'
      return []
    } finally {
      isLoadingSummaries.value = false
    }
  }

  async function loadJobDetail(jobId) {
    if (!jobId) {
      return null
    }

    detailLoadingJobId.value = jobId
    detailLoadError.value = ''

    try {
      const detail = normalizeJobDetail(await GetAdCheckJobDetail(jobId))
      jobDetailsById.value = {
        ...jobDetailsById.value,
        [detail.summary.jobId || jobId]: detail,
      }
      return detail
    } catch (error) {
      detailLoadError.value = error?.message ?? '상세 자료를 불러오지 못했습니다.'
      return null
    } finally {
      detailLoadingJobId.value = ''
    }
  }

  async function loadActiveJobs() {
    isLoadingActive.value = true
    loadError.value = ''

    try {
      const activeList = await ListActiveAdCheckJobs()
      const normalizedActiveJobs = Array.isArray(activeList)
        ? activeList.map((job) => normalizeJob(job)).filter((job) => job.jobId)
        : []
      const activeJobIds = new Set(normalizedActiveJobs.map((job) => job.jobId))
      const preservedJobs = jobs.value.filter((job) => !activeJobIds.has(job.jobId))

      jobs.value = sortJobs([...preservedJobs, ...normalizedActiveJobs])
      normalizedActiveJobs.forEach((job) => scheduleJobPoll(job.jobId))
      return normalizedActiveJobs
    } catch (error) {
      loadError.value = error?.message ?? '진행 중인 검수 작업을 불러오지 못했습니다.'
      return []
    } finally {
      isLoadingActive.value = false
    }
  }

  async function cancelJob(jobId) {
    const job = await CancelAdCheckJob(jobId)
    return upsertJob(job)
  }

  async function deleteJob(jobId) {
    await DeleteAdCheckJob(jobId)
    jobs.value = jobs.value.filter((job) => job.jobId !== jobId)
    jobSummaries.value = jobSummaries.value.filter((summary) => summary.jobId !== jobId)
    const nextDetails = { ...jobDetailsById.value }
    delete nextDetails[jobId]
    jobDetailsById.value = nextDetails
    clearJobPoll(jobId)
  }

  function dismissJob(jobId) {
    if (!jobId) {
      return
    }
    const nextDismissedIds = new Set(dismissedJobIds.value)
    nextDismissedIds.add(jobId)
    dismissedJobIds.value = nextDismissedIds
  }

  function findJob(jobId) {
    return jobs.value.find((job) => job.jobId === jobId) ?? null
  }

  function findJobDetail(jobId) {
    if (!jobId) {
      return null
    }
    return jobDetailsById.value[jobId] ?? null
  }

  function scheduleJobPoll(jobId) {
    if (!jobId || typeof window === 'undefined' || pollingTimers.has(jobId)) {
      return
    }

    const timerId = window.setTimeout(async () => {
      pollingTimers.delete(jobId)
      const currentJob = findJob(jobId)

      if (!currentJob || !isActiveJobStatus(currentJob.status)) {
        return
      }

      try {
        await fetchJob(jobId)
      } catch (error) {
        console.warn('Ad check job polling failed.', error)
      } finally {
        const nextJob = findJob(jobId)
        if (nextJob && isActiveJobStatus(nextJob.status)) {
          scheduleJobPoll(jobId)
        }
      }
    }, JOB_POLL_INTERVAL_MS)

    pollingTimers.set(jobId, timerId)
  }

  function clearJobPoll(jobId) {
    if (!jobId || typeof window === 'undefined') {
      return
    }

    const timerId = pollingTimers.get(jobId)
    if (!timerId) {
      return
    }

    window.clearTimeout(timerId)
    pollingTimers.delete(jobId)
  }

  return {
    jobs,
    jobSummaries,
    jobDetailsById,
    visibleJobs,
    activeJobs,
    runningJobs,
    queuedJobs,
    terminalJobs,
    floatingJob,
    isLoadingActive,
    isLoadingSummaries,
    detailLoadingJobId,
    loadError,
    summaryLoadError,
    detailLoadError,
    upsertJob,
    handleJobEvent,
    startJob,
    fetchJob,
    loadJobSummaries,
    loadJobDetail,
    loadActiveJobs,
    cancelJob,
    deleteJob,
    dismissJob,
    findJob,
    findJobDetail,
  }
})
