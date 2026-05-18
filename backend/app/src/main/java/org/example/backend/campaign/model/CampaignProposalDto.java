package org.example.backend.campaign.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CampaignProposalDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitReq {
        private AssetReq asset;
        private BenefitReq benefit;
        private String message;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssetReq {
        private String target;
        private String type;
        private String scale;
        private String conditions;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BenefitReq {
        private String name;
        private String type;
        private String scale;
        private String target;
        private String cost;
        private String status;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitRes {
        private Long assetIdx;
        private Long benefitIdx;
    }
}
