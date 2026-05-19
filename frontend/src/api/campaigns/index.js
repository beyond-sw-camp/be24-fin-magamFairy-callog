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

/** 캘린더 일괄 조회 — { campaigns, deadlines, milestones } 한번에 반환 */
export const ListCalendarEvents = async (params = {}) => {
  return unwrapResponse(await api.get('/campaigns/calendar-events', { params }))
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
 * payload는 기존 캠페인 필드(name, purpose, tags, startDate, endDate, partners,
 * goals, color)에 더해 KPI cascade 매핑을 위한 `contributions`, 팀원 자동 등록을
 * 위한 `ownerUserIdxs` 를 포함할 수 있다.
 *
 *   contributions: [{ targetOrgKpiId: number, committedValue: number }]
 *   ownerUserIdxs: number[]
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
/** 캠페인 멤버 목록 — { members, me, organizationIsPm, pmOrganizationIdx } */
export const ListCampaignMembers = async (publicId) => {
  return unwrapResponse(await api.get(`/campaigns/${publicId}/members`))
}

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
  ListCampaignMembers,
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
