package org.example.backend.campaign.model;

/**
 * KPI 상위 분류(성과 영역). 기존 세부(노출/참여/전환/매출/브랜드/ESG)를 넓은 5범주로 통합.
 *  - GROWTH         성장      : 노출·참여·전환 (마케팅 퍼널 선행지표)
 *  - FINANCIAL      재무      : 매출·ROI 등 결과 지표
 *  - BRAND          브랜드    : 인지도·평판 등 정성·장기 지표
 *  - OPERATIONAL    운영      : 집행 효율·SLA·품질 등 프로세스 지표
 *  - SUSTAINABILITY 지속가능성 : ESG (환경·사회·거버넌스)
 */
public enum KpiCategory {
    GROWTH,
    FINANCIAL,
    BRAND,
    OPERATIONAL,
    SUSTAINABILITY
}
