export const AD_CHECK_VERDICT_LEVELS = [
  {
    key: 'pass',
    level: 1,
    tone: 'pass',
    label: '통과',
    title: '통과 (문제 없음)',
    description: 'AI 검수에서 문제 항목이 발견되지 않았습니다.',
    guidance: '추가 수정 없이 제출할 수 있습니다.',
  },
  {
    key: 'recheck',
    level: 2,
    tone: 'recheck',
    label: '재확인 필요',
    title: '재확인 필요',
    description: '심각한 문제는 아니지만 오해의 소지가 있을 수 있습니다.',
    guidance: '의도한 표현이고 근거가 충분하다면 사용 가능합니다.',
  },
  {
    key: 'suggestion',
    level: 3,
    tone: 'suggestion',
    label: '수정 제안',
    title: '수정 제안',
    description: '필수 수정 단계는 아니지만 표현을 다듬는 것을 권장합니다.',
    guidance: '오해를 줄일 수 있도록 보완 문구나 근거를 함께 제시해 주세요.',
  },
  {
    key: 'revision',
    level: 4,
    tone: 'revision',
    label: '수정 필요',
    title: '수정 필요',
    description: '현재 표현은 제출 전 반드시 수정이 필요합니다.',
    guidance: '문구를 다른 표현으로 수정한 뒤 다시 검수해 주세요.',
  },
  {
    key: 'danger',
    level: 5,
    tone: 'danger',
    label: '위험',
    title: '위험 (제출 반려)',
    description: '위험도가 높아 현재 상태로는 제출 반려 대상입니다.',
    guidance: '해당 표현을 제거하거나 근거와 표현 방식을 전면 수정해 주세요.',
  },
]

const VERDICT_BY_LEVEL = AD_CHECK_VERDICT_LEVELS.reduce((acc, item) => {
  acc[item.level] = item
  return acc
}, {})

export const AD_CHECK_FAILED_VERDICT = {
  key: 'failed',
  level: null,
  tone: 'danger',
  label: '검수 실패',
  title: '검수 실패',
  description: '검수 처리 중 오류가 발생했습니다.',
  guidance: '오류 내용을 확인한 뒤 다시 요청해 주세요.',
}

export const AD_CHECK_PENDING_VERDICT = {
  key: 'pending',
  level: null,
  tone: 'neutral',
  label: '판정 대기',
  title: '판정 대기',
  description: 'AI 검수 판단 단계가 아직 전달되지 않았습니다.',
  guidance: 'AI가 내려준 1~5단계 값을 수신하면 표시됩니다.',
}

const RESULT_STATUS_ALIASES = {
  pass: 'pass',
  passed: 'pass',
  success: 'pass',
  ok: 'pass',
  normal: 'pass',
  clear: 'pass',
  warning: 'warning',
  caution: 'warning',
  review: 'warning',
  review_required: 'warning',
  needs_review: 'warning',
  violation: 'violation',
  reject: 'violation',
  rejected: 'violation',
  danger: 'violation',
  failed: 'failed',
  fail: 'failed',
  error: 'failed',
}

function normalizeText(value) {
  return String(value ?? '').replace(/\s+/g, ' ').trim()
}

export function normalizeAdCheckResultStatus(status) {
  const normalized = normalizeText(status).toLowerCase()
  return RESULT_STATUS_ALIASES[normalized] ?? ''
}

export function normalizeAdCheckVerdictLevel(value) {
  if (value == null || value === '') return null

  if (typeof value === 'number' && Number.isFinite(value)) {
    const level = Math.trunc(value)
    return level >= 1 && level <= 5 ? level : null
  }

  const text = normalizeText(value)
  const exactNumber = Number(text)
  if (Number.isFinite(exactNumber)) {
    const level = Math.trunc(exactNumber)
    return level >= 1 && level <= 5 ? level : null
  }

  const match = text.match(/(?:^|\D)([1-5])(?:\D|$)/)
  return match ? Number(match[1]) : null
}

function resolveIncomingVerdictLevel(input = {}) {
  return [
    input.verdictLevel,
    input.verdict_level,
    input.reviewLevel,
    input.review_level,
    input.riskLevel,
    input.risk_level,
    input.level,
    input.grade,
  ]
    .map(normalizeAdCheckVerdictLevel)
    .find((level) => level != null) ?? null
}

export function getAdCheckVerdict(input = {}) {
  const incomingLevel = resolveIncomingVerdictLevel(input)
  if (incomingLevel != null) {
    return VERDICT_BY_LEVEL[incomingLevel]
  }

  return AD_CHECK_PENDING_VERDICT
}

export function getAdCheckDisplayVerdict(input = {}) {
  const jobStatus = normalizeText(input.jobStatus ?? input.status).toUpperCase()
  const resultStatus = normalizeAdCheckResultStatus(input.resultStatus)
    || normalizeAdCheckResultStatus(input.status)

  if (['FAILED', 'CANCELED'].includes(jobStatus) || resultStatus === 'failed') {
    return AD_CHECK_FAILED_VERDICT
  }

  return getAdCheckVerdict(input)
}
