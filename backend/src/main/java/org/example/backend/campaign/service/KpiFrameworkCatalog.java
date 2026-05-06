package org.example.backend.campaign.service;

import org.example.backend.campaign.model.CampaignKpiDto;
import org.example.backend.campaign.model.KpiCategory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class KpiFrameworkCatalog {

    private static final Map<String, CampaignKpiDto.FrameworkRes> CATALOG = Map.of(
        "brand_awareness", new CampaignKpiDto.FrameworkRes(
            "brand_awareness", "브랜드 인지도 강화", "Reach·SOV·Brand Lift·Branded Search 기반 인지도 KPI 세트",
            List.of(
                new CampaignKpiDto.FrameworkItem("순도달 (Reach)",               KpiCategory.IMPRESSION, new BigDecimal("5000000"), "UU"),
                new CampaignKpiDto.FrameworkItem("SOV (Share of Voice)",         KpiCategory.IMPRESSION, new BigDecimal("25"),      "%"),
                new CampaignKpiDto.FrameworkItem("Viewability Rate",             KpiCategory.IMPRESSION, new BigDecimal("70"),      "%"),
                new CampaignKpiDto.FrameworkItem("Ad Recall Lift",               KpiCategory.BRAND,      new BigDecimal("10"),      "%p"),
                new CampaignKpiDto.FrameworkItem("브랜드 인지도 상승",             KpiCategory.BRAND,      new BigDecimal("5"),       "%p"),
                new CampaignKpiDto.FrameworkItem("Branded Search 증가율",         KpiCategory.BRAND,      new BigDecimal("30"),      "%"),
                new CampaignKpiDto.FrameworkItem("VTR (영상 완주율)",              KpiCategory.ENGAGEMENT, new BigDecimal("50"),      "%")
            )
        ),
        "conversion_funnel", new CampaignKpiDto.FrameworkRes(
            "conversion_funnel", "전환 퍼널 최적화", "CTR·CVR·CPA·ROAS·LTV:CAC 기반 퍼포먼스 KPI 세트",
            List.of(
                new CampaignKpiDto.FrameworkItem("CTR",                          KpiCategory.ENGAGEMENT, new BigDecimal("1.5"),    "%"),
                new CampaignKpiDto.FrameworkItem("랜딩페이지 방문",                KpiCategory.CONVERSION, new BigDecimal("100000"), "Sessions"),
                new CampaignKpiDto.FrameworkItem("CVR (전환율)",                  KpiCategory.CONVERSION, new BigDecimal("3"),      "%"),
                new CampaignKpiDto.FrameworkItem("Lead-to-MQL Rate",             KpiCategory.CONVERSION, new BigDecimal("30"),     "%"),
                new CampaignKpiDto.FrameworkItem("CPA",                          KpiCategory.REVENUE,    new BigDecimal("25000"),  "원"),
                new CampaignKpiDto.FrameworkItem("ROAS",                         KpiCategory.REVENUE,    new BigDecimal("400"),    "%"),
                new CampaignKpiDto.FrameworkItem("LTV:CAC 비율",                  KpiCategory.REVENUE,    new BigDecimal("3"),      "배수")
            )
        ),
        "viral_engagement", new CampaignKpiDto.FrameworkRes(
            "viral_engagement", "SNS 바이럴·인게이지먼트", "ER·UGC·Share/Save·Sentiment 기반 바이럴 KPI 세트",
            List.of(
                new CampaignKpiDto.FrameworkItem("Engagement Rate (ER)",         KpiCategory.ENGAGEMENT, new BigDecimal("5"),     "%"),
                new CampaignKpiDto.FrameworkItem("UGC 생성 수",                   KpiCategory.ENGAGEMENT, new BigDecimal("1000"),  "건"),
                new CampaignKpiDto.FrameworkItem("UGC Quality Rate (재활용 가능)", KpiCategory.ENGAGEMENT, new BigDecimal("30"),   "%"),
                new CampaignKpiDto.FrameworkItem("Share·Save 수",                KpiCategory.ENGAGEMENT, new BigDecimal("500"),   "건"),
                new CampaignKpiDto.FrameworkItem("팔로워 순증",                    KpiCategory.ENGAGEMENT, new BigDecimal("10000"), "명"),
                new CampaignKpiDto.FrameworkItem("긍정 Sentiment 비율",            KpiCategory.BRAND,      new BigDecimal("70"),    "%"),
                new CampaignKpiDto.FrameworkItem("해시태그 Velocity (일)",          KpiCategory.ENGAGEMENT, new BigDecimal("1000"),  "건/일")
            )
        ),
        "media_partnership", new CampaignKpiDto.FrameworkRes(
            "media_partnership", "미디어 제휴 집행", "GRP·Reach·Viewability·Brand Safety·IVT·SLA 기반 미디어 KPI 세트",
            List.of(
                new CampaignKpiDto.FrameworkItem("GRP",                          KpiCategory.IMPRESSION, new BigDecimal("300"),  "점"),
                new CampaignKpiDto.FrameworkItem("Reach 1+ 도달률",               KpiCategory.IMPRESSION, new BigDecimal("70"),   "%"),
                new CampaignKpiDto.FrameworkItem("CPM",                          KpiCategory.REVENUE,    new BigDecimal("5000"), "원"),
                new CampaignKpiDto.FrameworkItem("Viewability Rate",             KpiCategory.IMPRESSION, new BigDecimal("70"),   "%"),
                new CampaignKpiDto.FrameworkItem("Brand Safety Score",           KpiCategory.OTHER,      new BigDecimal("95"),   "%"),
                new CampaignKpiDto.FrameworkItem("IVT Rate (부정 트래픽)",        KpiCategory.IMPRESSION, new BigDecimal("3"),    "%"),
                new CampaignKpiDto.FrameworkItem("SLA 준수율 (집행 일정)",         KpiCategory.OTHER,      new BigDecimal("100"),  "%")
            )
        ),
        "esg_partnership", new CampaignKpiDto.FrameworkRes(
            "esg_partnership", "ESG 동반성장", "E(환경)·S(사회)·G(거버넌스) 균형 기반 제휴 ESG KPI 세트",
            List.of(
                new CampaignKpiDto.FrameworkItem("디지털 광고 비중 (E)",            KpiCategory.ESG, new BigDecimal("70"),  "%"),
                new CampaignKpiDto.FrameworkItem("친환경 인증 매체 비중 (E)",        KpiCategory.ESG, new BigDecimal("30"),  "%"),
                new CampaignKpiDto.FrameworkItem("그린워싱 리스크 점수 (E)",         KpiCategory.ESG, new BigDecimal("90"),  "점"),
                new CampaignKpiDto.FrameworkItem("중소·여성기업 협력사 비율 (S)",    KpiCategory.ESG, new BigDecimal("30"),  "%"),
                new CampaignKpiDto.FrameworkItem("공정 정산 준수율 60일 (S)",        KpiCategory.ESG, new BigDecimal("100"), "%"),
                new CampaignKpiDto.FrameworkItem("협력사 만족도 NPS (S)",            KpiCategory.ESG, new BigDecimal("30"),  "점"),
                new CampaignKpiDto.FrameworkItem("표준계약서 사용률 (G)",            KpiCategory.ESG, new BigDecimal("100"), "%"),
                new CampaignKpiDto.FrameworkItem("협력사 행동규범 서약률 (G)",        KpiCategory.ESG, new BigDecimal("100"), "%"),
                new CampaignKpiDto.FrameworkItem("광고법 준수율 #광고 표기 (G)",     KpiCategory.ESG, new BigDecimal("100"), "%")
            )
        )
    );

    public List<CampaignKpiDto.FrameworkRes> listAll() {
        return List.copyOf(CATALOG.values());
    }

    public CampaignKpiDto.FrameworkRes get(String key) {
        CampaignKpiDto.FrameworkRes fw = CATALOG.get(key);
        if (fw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 frameworkKey: " + key);
        }
        return fw;
    }
}
