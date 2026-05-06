package org.example.backend.matching.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.organization.model.Organization;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MatchingDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddBenefit {
        private Long campaignIdx;

        // [기본 정보]
        private String name;
        private String type;
        private String description;

        // [규모·재고]
        private Long quantity;
        private String quantityUnit;
        private Long valuePerPerson;
        private Long totalValue;

        // [기간]
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private Boolean alwaysNegotiable;
        private Integer prepDays;

        // [대상]
        private String targetAudience;
        private Long expectedReach;

        // [비용 부담]
        private String costBearer;
        private Integer costPartnerPercent;
        private Integer costOursPercent;
        private String costDetails;

        // [운영 조건]
        private String exposureChannels;
        private String requiredCollaborations;
        private String conditions;

        // [연결 자산]
        private String desiredAssets;
        private Boolean autoRecommend;

        // [담당자]
        private String managerName;
        private String managerEmail;
        private String managerPhone;

        // 상태
        private String status;

        public PartnerBenefits toEntity(Organization organization, Campaign campaign) {
            return PartnerBenefits.builder()
                    .organization(organization)
                    .campaign(campaign)
                    .name(this.name)
                    .type(this.type)
                    .description(this.description)
                    .quantity(this.quantity)
                    .quantityUnit(this.quantityUnit)
                    .valuePerPerson(this.valuePerPerson)
                    .totalValue(this.totalValue)
                    .periodStart(this.periodStart)
                    .periodEnd(this.periodEnd)
                    .alwaysNegotiable(this.alwaysNegotiable)
                    .prepDays(this.prepDays)
                    .targetAudience(this.targetAudience)
                    .expectedReach(this.expectedReach)
                    .costBearer(this.costBearer)
                    .costPartnerPercent(this.costPartnerPercent)
                    .costOursPercent(this.costOursPercent)
                    .costDetails(this.costDetails)
                    .exposureChannels(this.exposureChannels)
                    .requiredCollaborations(this.requiredCollaborations)
                    .conditions(this.conditions)
                    .desiredAssets(this.desiredAssets)
                    .autoRecommend(this.autoRecommend)
                    .managerName(this.managerName)
                    .managerEmail(this.managerEmail)
                    .managerPhone(this.managerPhone)
                    .status(this.status)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class BenefitList{
        private List<BenefitRes> benefitList;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;

        public static BenefitList toDto(Page<PartnerBenefits> result){
            return BenefitList.builder()
                    .benefitList(result.getContent().stream()
                            .map(BenefitRes::toDto)
                            .toList())
                    .page(result.getNumber())
                    .size(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class BenefitRes {
        private Long idx;
        private String affiliate; // Organization 이름
        private Long campaignIdx;

        // [기본 정보]
        private String name;
        private String type;
        private String description;

        // [규모·재고]
        private Long quantity;
        private String quantityUnit;
        private Long valuePerPerson;
        private Long totalValue;

        // [기간]
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private Boolean alwaysNegotiable;
        private Integer prepDays;

        // [대상]
        private String targetAudience;
        private Long expectedReach;

        // [비용 부담]
        private String costBearer;
        private Integer costPartnerPercent;
        private Integer costOursPercent;
        private String costDetails;

        // [운영 조건]
        private String exposureChannels;
        private String requiredCollaborations;
        private String conditions;

        // [연결 자산]
        private String desiredAssets;
        private Boolean autoRecommend;

        // [담당자]
        private String managerName;
        private String managerEmail;
        private String managerPhone;

        // 상태 및 생성일
        private String status;
        private LocalDateTime createdAt;

        public static BenefitRes toDto(PartnerBenefits entity) {
            return BenefitRes.builder()
                    .idx(entity.getIdx())
                    // null safe 처리 (Organization, Campaign 매핑이 없을 경우 대비)
                    .affiliate(entity.getOrganization() != null ? entity.getOrganization().getName() : null)
                    .campaignIdx(entity.getCampaign() != null ? entity.getCampaign().getIdx() : null)
                    .name(entity.getName())
                    .type(entity.getType())
                    .description(entity.getDescription())
                    .quantity(entity.getQuantity())
                    .quantityUnit(entity.getQuantityUnit())
                    .valuePerPerson(entity.getValuePerPerson())
                    .totalValue(entity.getTotalValue())
                    .periodStart(entity.getPeriodStart())
                    .periodEnd(entity.getPeriodEnd())
                    .alwaysNegotiable(entity.getAlwaysNegotiable())
                    .prepDays(entity.getPrepDays())
                    .targetAudience(entity.getTargetAudience())
                    .expectedReach(entity.getExpectedReach())
                    .costBearer(entity.getCostBearer())
                    .costPartnerPercent(entity.getCostPartnerPercent())
                    .costOursPercent(entity.getCostOursPercent())
                    .costDetails(entity.getCostDetails())
                    .exposureChannels(entity.getExposureChannels())
                    .requiredCollaborations(entity.getRequiredCollaborations())
                    .conditions(entity.getConditions())
                    .desiredAssets(entity.getDesiredAssets())
                    .autoRecommend(entity.getAutoRecommend())
                    .managerName(entity.getManagerName())
                    .managerEmail(entity.getManagerEmail())
                    .managerPhone(entity.getManagerPhone())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AddAsset {
        private String affiliate;
        private List<String> blockedPartners;
        private String category;
        private String conditions;
        private String customAffiliate;
        private String exposureValue;
        private String matchingStatus;
        private List<String> partnerFit;
        private String performance;
        private String publicStatus;
        private String scale;
        private String target;
        private String type;

        public MarketingAsset toEntity(Organization organization) {
            return MarketingAsset.builder()
                    .organization(organization)
                    .affiliate(this.affiliate)
                    .blockedPartners(this.blockedPartners)
                    .category(this.category)
                    .conditions(this.conditions)
                    .customAffiliate(this.customAffiliate)
                    .exposureValue(this.exposureValue)
                    .matchingStatus(this.matchingStatus)
                    .partnerFit(this.partnerFit)
                    .performance(this.performance)
                    .publicStatus(this.publicStatus)
                    .scale(this.scale)
                    .target(this.target)
                    .type(this.type)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AssetList{
        private List<AssetRes> assetList;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;

        public static AssetList toDto(Page<MarketingAsset> result){
            return AssetList.builder()
                    .assetList(result.getContent().stream()
                            .map(AssetRes::toDto)
                            .toList())
                    .page(result.getNumber())
                    .size(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AssetRes{
        private Long idx;
        private String owner;
        private String affiliate;
        private List<String> blockedPartners;
        private String category;
        private String conditions;
        private String customAffiliate;
        private String exposureValue;
        private String matchingStatus;
        private List<String> partnerFit;
        private String performance;
        private String publicStatus;
        private String scale;
        private String target;
        private String type;

        public static AssetRes toDto(MarketingAsset entity){
            return AssetRes.builder()
                    .idx(entity.getIdx())
                    .affiliate(entity.getAffiliate())
                    .blockedPartners(entity.getBlockedPartners())
                    .category(entity.getCategory())
                    .conditions(entity.getConditions())
                    .customAffiliate(entity.getCustomAffiliate())
                    .exposureValue(entity.getExposureValue())
                    .matchingStatus(entity.getMatchingStatus())
                    .partnerFit(entity.getPartnerFit())
                    .performance(entity.getPerformance())
                    .publicStatus(entity.getPublicStatus())
                    .scale(entity.getScale())
                    .target(entity.getTarget())
                    .type(entity.getType())
                    .owner(entity.getOrganization().getName())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddGoal {
        private String name;
        private GoalType primaryType;
        private GoalType secondaryType;
        private CampaignMethod campaignMethod;

        private String kpiPrimary;
        private String kpiSecondary;

        private String budgetLimit;
        private String effortLimit;

        private LocalDate periodStart;
        private LocalDate periodEnd;

        private Integer weightRevenue;
        private Integer weightEffort;
        private Integer weightBrand;

        private String ownerLabel;
        private String status;
    }

    // 응답 DTO
    @Getter
    @Builder
    public static class GoalRes {
        private Long idx;
        private String name;
        private GoalType primaryType;
        private GoalType secondaryType;
        private String kpiPrimary;
        private String kpiSecondary;
        private String budgetLimit;
        private String effortLimit;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private Integer weightRevenue;
        private Integer weightEffort;
        private Integer weightBrand;
        private String ownerLabel;
        private String ownerOrganization;
        private String status;
        private String createdAt;

        public static GoalRes toDto(CampaignGoal entity) {
            return GoalRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .primaryType(entity.getPrimaryType())
                    .secondaryType(entity.getSecondaryType())
                    .kpiPrimary(entity.getKpiPrimary())
                    .kpiSecondary(entity.getKpiSecondary())
                    .budgetLimit(entity.getBudgetLimit())
                    .effortLimit(entity.getEffortLimit())
                    .periodStart(entity.getPeriodStart())
                    .periodEnd(entity.getPeriodEnd())
                    .weightRevenue(entity.getWeightRevenue())
                    .weightEffort(entity.getWeightEffort())
                    .weightBrand(entity.getWeightBrand())
                    .ownerLabel(entity.getOwnerLabel())
                    .ownerOrganization(entity.getOwnerOrganization() == null ? null : entity.getOwnerOrganization().getName())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString())
                    .build();
        }
    }

    // 목록 DTO
    @Getter
    @Builder
    public static class GoalList {
        private List<GoalRes> goalList;
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;

        public static GoalList toDto(Page<CampaignGoal> result) {
            return GoalList.builder()
                    .goalList(result.getContent().stream().map(GoalRes::toDto).toList())
                    .page(result.getNumber())
                    .size(result.getSize())
                    .totalElements(result.getTotalElements())
                    .totalPages(result.getTotalPages())
                    .build();
        }
    }



}
