package org.example.backend.matching.repository;

import org.example.backend.matching.model.MarketingAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<MarketingAsset, Long> {

    /**
     * 전체 자산 카테고리 카운트 (HQ scope 용). category 는 lowercase 정규화.
     * 풀스캔 회피용 GROUP BY — findAll() 후 stream filter 대비 수십~수백 배 빠름.
     */
    @Query("SELECT LOWER(a.category), COUNT(a) FROM MarketingAsset a " +
           "WHERE a.category IS NOT NULL " +
           "GROUP BY LOWER(a.category)")
    List<Object[]> countCategoriesAll();

    /**
     * visible 캠페인 + 본인 조직의 자산만 카테고리 GROUP BY 카운트.
     * AFFILIATE / EXTERNAL_PARTNER / STAFF scope 용.
     */
    @Query("SELECT LOWER(a.category), COUNT(a) FROM MarketingAsset a " +
           "WHERE a.category IS NOT NULL " +
           "  AND ((a.campaign.idx IN :campaignIds) " +
           "       OR (:ownerOrgId IS NOT NULL AND a.organization.idx = :ownerOrgId)) " +
           "GROUP BY LOWER(a.category)")
    List<Object[]> countCategoriesVisible(
            @Param("campaignIds") Collection<Long> campaignIds,
            @Param("ownerOrgId") Long ownerOrgId);

    /**
     * 자산 LIVE 카운트 — summary.miniStats[2] 용. findAll() 후 stream filter 대신.
     */
    @Query("SELECT COUNT(a) FROM MarketingAsset a")
    long countAllAssets();

    @Query("SELECT COUNT(a) FROM MarketingAsset a " +
           "WHERE (a.campaign.idx IN :campaignIds) " +
           "   OR (:ownerOrgId IS NOT NULL AND a.organization.idx = :ownerOrgId)")
    long countVisibleAssets(
            @Param("campaignIds") Collection<Long> campaignIds,
            @Param("ownerOrgId") Long ownerOrgId);
}
