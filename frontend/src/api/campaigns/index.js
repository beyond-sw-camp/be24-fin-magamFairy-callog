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

export const ListCampaign = async () => {
  return unwrapResponse(await api.get('/campaigns'))
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

export const CreateCampaign = async (payload) => {
  return unwrapResponse(await api.post('/campaigns/new', payload))
}

export const UpdateCampaign = async (campaignId, payload) => {
  return unwrapResponse(await api.put(`/campaigns/${campaignId}`, payload))
}

export const UpdateCampaignStatus = async (campaignId, status) => {
  return unwrapResponse(await api.patch(`/campaigns/${campaignId}/status`, { status }))
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
  InvitePartners,
  ExportCampaignCsv,
  ExportCampaignPdf,
}
