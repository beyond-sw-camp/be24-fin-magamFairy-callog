package org.example.backend.matching.model;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.organization.model.Organization;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.example.backend.common.Constants.DEFAULT_ASSET_STATUS;

public class MatchingDto {

    @Getter
    @Builder
    public static class AddAsset {
        private String target;
        private String type;
        private String scale;
        private String conditions;

        public MarketingAsset toEntity(Organization organization) {
            return MarketingAsset.builder()
                    .organization(organization)
                    .target(this.target)
                    .scale(this.scale)
                    .type(this.type)
                    .conditions(this.conditions)
                    .isActive(DEFAULT_ASSET_STATUS)
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
        private String type;
        private String affiliate;
        private String target;
        private String conditions;
        private Boolean isActive;

        public static AssetRes toDto(MarketingAsset entity){
            return AssetRes.builder()
                    .idx(entity.getIdx())
                    .type(entity.getType())
                    .affiliate(entity.getOrganization().getName())
                    .target(entity.getTarget())
                    .conditions(entity.getConditions())
                    .isActive(entity.getIsActive())
                    .build();
        }
    }
}
