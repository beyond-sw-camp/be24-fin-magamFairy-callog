import api from '/plugins/interceptor.js'

function isSuccessResponse(payload) {
  return (
    payload?.success === true ||
    payload?.success === 'true' ||
    payload?.isSuccess === true ||
    payload?.isSuccess === 'true'
  )
}

function unwrapResponse(response) {
  const payload = response?.data

  if (!payload) {
    throw new Error('캠페인 응답이 비어있습니다.')
  }

  if (!isSuccessResponse(payload)) {
    throw new Error(payload?.message ?? '캠페인 요청이 실패했습니다.')
  }

  return payload.data
}

export const ListCampaign = async (params = {}) => {
  // params.scope: "mine" | "org" — 미지정 시 백엔드 default = mine
  return unwrapResponse(await api.get('/campaigns', { params }))
}

/**
 * 캠페인 디렉토리 — 검색·필터·태그·정렬 지원.
 * params: { q, orgType, status, tags(array), sort }
 *   - orgType: 'HQ' | 'AFFILIATE' | 'EXTERNAL_PARTNER' (or 'ALL')
 *   - status: 캠페인 status string
 *   - tags: 배열 — axios가 multi-value query param 으로 전송 (paramsSerializer 필요 시 별도 처리)
 *   - sort: 'latest' | 'deadline'
 */
export const ListCampaignDirectory = async (params = {}) => {
  return unwrapResponse(await api.get('/campaigns/directory', {
    params,
    paramsSerializer: { indexes: null },   // tags=[a,b] → tags=a&tags=b
  }))
}

/* ───── 썸네일 (Phase 3) ───── */

/** 1단계 — presigned PUT URL 발급. payload: { contentType, fileSize } */
export const CreateThumbnailUploadUrl = async (campaignId, payload) => {
  return unwrapResponse(
    await api.post(`/campaigns/${campaignId}/thumbnail/upload-url`, payload),
  )
}

/** 2단계 — S3에 직접 업로드 후 backend에 objectKey 저장. */
export const ConfirmThumbnail = async (campaignId, objectKey) => {
  return unwrapResponse(
    await api.patch(`/campaigns/${campaignId}/thumbnail`, { objectKey }),
  )
}

export const ClearThumbnail = async (campaignId) => {
  return unwrapResponse(await api.delete(`/campaigns/${campaignId}/thumbnail`))
}

/** AI 자동 재생성 (Phase 4) — 비동기. backend가 OpenAI 호출 후 S3 업로드. */
export const RegenerateThumbnail = async (campaignId) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/thumbnail/generate`))
}

/**
 * 썸네일 업로드 통합 helper — File 받아서 presigned URL 발급 → S3 직접 PUT → confirm.
 * 호출자: const url = await uploadCampaignThumbnail(campaignId, fileFromInput)
 */
export async function uploadCampaignThumbnail(campaignId, file) {
  if (!file) throw new Error('파일을 선택해주세요.')
  const { uploadUrl, objectKey, contentType } = await CreateThumbnailUploadUrl(campaignId, {
    contentType: file.type,
    fileSize: file.size,
  })
  // S3 직접 업로드 — presigned URL 사용
  const putRes = await fetch(uploadUrl, {
    method: 'PUT',
    body: file,
    headers: { 'Content-Type': contentType },
  })
  if (!putRes.ok) {
    throw new Error(`S3 업로드 실패 (${putRes.status})`)
  }
  await ConfirmThumbnail(campaignId, objectKey)
  return objectKey
}
export const GetCampaignDetails = async (taps) => {
  return unwrapResponse(
    await api.get('/campaigns', {
      params: {
        taps: taps // 키와 변수명이 같으면 그냥 { taps } 로 생략 가능
      }
    })
  );
};

/**
 * 캠페인 생성.
 *
 * payload는 기존 캠페인 필드(name, purpose, tags, startDate, endDate, partners, goals,
 * mainMessage, color)에 더해 KPI cascade 매핑을 위한 `contributions`를 포함할 수 있다.
 *
 *   contributions: [{ targetOrgKpiId: number, committedValue: number }]
 *
 * 매핑이 없으면 빈 배열을 보낸다.
 */
export const CreateCampaign = async (payload) => {
  return unwrapResponse(await api.post('/campaigns/new', payload))
}

export const UpdateCampaign = async (campaignId, payload) => {
  return unwrapResponse(await api.put(`/campaigns/${campaignId}`, payload))
}

export const UpdateCampaignStatus = async (campaignId, status) => {
  return unwrapResponse(await api.patch(`/campaigns/${campaignId}/status`, { status }))
}

export const GetCampaignIntro = async (campaignId) => {
  return unwrapResponse(await api.get(`/campaigns/${campaignId}/intro`))
}

export const UpdateCampaignIntro = async (campaignId, payload) => {
  return unwrapResponse(await api.patch(`/campaigns/${campaignId}/intro`, payload))
}

export const SubmitCampaignProposal = async (campaignId, payload) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/proposals`, payload))
}

export const InvitePartners = async (campaignId, partners) => {
  return unwrapResponse(await api.post(`/campaigns/${campaignId}/partners/invitations`, { partners }))
}

function extractFileName(headers, fallback) {
  const disposition = headers?.['content-disposition'] ?? ''
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
  if (utf8Match) {
    try { return decodeURIComponent(utf8Match[1]) } catch { /* fallthrough */ }
  }
  if (plainMatch) return plainMatch[1]
  return fallback
}

/** 캠페인 데이터를 CSV로 다운로드. sections는 ['campaign','members','tasks','kpi','esg'] 중 선택. */
export const ExportCampaignCsv = async (campaignId, sections) => {
  const response = await api.get(`/campaigns/${campaignId}/export.csv`, {
    params: { sections: (sections ?? []).join(',') },
    responseType: 'blob',
  })
  const fileName = extractFileName(response.headers, `campaign_${campaignId}_export.csv`)
  return { blob: response.data, fileName }
}

/** 캠페인 PDF 보고서 다운로드. type='summary'(1쪽 임원 요약) | 'full'(다중 페이지 상세). */
export const ExportCampaignPdf = async (campaignId, type = 'summary') => {
  const response = await api.get(`/campaigns/${campaignId}/export.pdf`, {
    params: { type },
    responseType: 'blob',
  })
  const fileName = extractFileName(
    response.headers,
    `campaign_${campaignId}_${type === 'full' ? 'report' : 'summary'}.pdf`,
  )
  return { blob: response.data, fileName }
}

export default {
  ListCampaign,
  GetCampaignDetails,
  CreateCampaign,
  UpdateCampaign,
  UpdateCampaignStatus,
  GetCampaignIntro,
  UpdateCampaignIntro,
  SubmitCampaignProposal,
  InvitePartners,
  ExportCampaignCsv,
  ExportCampaignPdf,
}
