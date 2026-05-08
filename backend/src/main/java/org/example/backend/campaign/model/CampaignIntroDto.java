package org.example.backend.campaign.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class CampaignIntroDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GetRes {
        private Long idx;
        private Long campaignIdx;
        private String campaignName;
        private String campaignSummary;
        private String campaignStatus;
        private String ownerLoginId;
        private String ownerName;          // 캠페인 생성자(=PM)의 user 이름
        private String ownerEmail;         // 캠페인 생성자의 이메일
        private String ownerDepartment;    // 캠페인 생성자의 부서
        private String rfpCode;
        private LocalDateTime recruitDeadline;
        private Map<String, Object> hanwhaAssets;
        private Map<String, Object> partnerRoles;
        private Map<String, Object> customerTags;
        private Map<String, Object> partnerValues;
        private Map<String, Object> timelineEvents;
        private Map<String, Object> submissionDocs;
        private Map<String, Object> attachedFiles;
        private Map<String, Object> contactInfo;
        private Integer weightCustomer;
        private Integer weightRevenue;
        private Integer weightCost;
        private Integer weightOperation;
        private Integer weightBrand;
        private Boolean canEdit;             // P0 — caller가 편집 가능한지 (PM 조직 + GM/MGR)
        private Boolean isInternalViewer;    // P1 — 내부 사용자(HQ/AFFILIATE)인지 → 가중치/심사 기준 노출
        private String visibility;           // PRIVATE/HQ_ONLY/HQ_AND_AFFILIATE/AFFILIATE_ONLY/EXTERNAL_ONLY/ALL

        public static GetRes toDto(CampaignIntro intro, Campaign campaign) {
            return toDto(intro, campaign, false, false, null);
        }

        public static GetRes toDto(CampaignIntro intro, Campaign campaign, boolean canEdit, boolean isInternalViewer,
                                   org.example.backend.user.model.User ownerUser) {
            return GetRes.builder()
                    .idx(intro != null ? intro.getIdx() : null)
                    .campaignIdx(campaign.getIdx())
                    .campaignName(campaign.getName())
                    .campaignSummary(campaign.getPurpose())
                    .campaignStatus(campaign.getStatus())
                    .ownerLoginId(campaign.getOwnerLoginId())
                    .ownerName(ownerUser != null ? ownerUser.getName() : null)
                    .ownerEmail(ownerUser != null ? ownerUser.getEmail() : null)
                    .ownerDepartment(ownerUser != null ? ownerUser.getDepartment() : null)
                    .rfpCode(intro != null ? intro.getRfpCode() : null)
                    .recruitDeadline(intro != null ? intro.getRecruitDeadline() : null)
                    .hanwhaAssets(intro != null ? intro.getHanwhaAssets() : null)
                    .partnerRoles(intro != null ? intro.getPartnerRoles() : null)
                    .customerTags(intro != null ? intro.getCustomerTags() : null)
                    .partnerValues(intro != null ? intro.getPartnerValues() : null)
                    .timelineEvents(intro != null ? intro.getTimelineEvents() : null)
                    .submissionDocs(intro != null ? intro.getSubmissionDocs() : null)
                    .attachedFiles(intro != null ? intro.getAttachedFiles() : null)
                    .contactInfo(intro != null ? intro.getContactInfo() : null)
                    .weightCustomer(intro != null ? intro.getWeightCustomer() : null)
                    .weightRevenue(intro != null ? intro.getWeightRevenue() : null)
                    .weightCost(intro != null ? intro.getWeightCost() : null)
                    .weightOperation(intro != null ? intro.getWeightOperation() : null)
                    .weightBrand(intro != null ? intro.getWeightBrand() : null)
                    .canEdit(canEdit)
                    .isInternalViewer(isInternalViewer)
                    .visibility(campaign.getVisibility() == null ? "PRIVATE" : campaign.getVisibility())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String rfpCode;
        private LocalDateTime recruitDeadline;
        private Map<String, Object> hanwhaAssets;
        private Map<String, Object> partnerRoles;
        private Map<String, Object> customerTags;
        private Map<String, Object> partnerValues;
        private Map<String, Object> timelineEvents;
        private Map<String, Object> submissionDocs;
        private Map<String, Object> attachedFiles;
        private Map<String, Object> contactInfo;
        private Integer weightCustomer;
        private Integer weightRevenue;
        private Integer weightCost;
        private Integer weightOperation;
        private Integer weightBrand;
        private String visibility;
    }
}
