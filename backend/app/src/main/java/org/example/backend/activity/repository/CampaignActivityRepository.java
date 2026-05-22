package org.example.backend.activity.repository;

import org.example.backend.activity.model.CampaignActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CampaignActivityRepository extends JpaRepository<CampaignActivity, Long> {

    /**
     * 주어진 캠페인들의 활동을 최신순으로 조회 (limit 은 Pageable 로 제어).
     * actor / campaign 을 fetch join 해서 피드 변환 시 N+1 방지.
     */
    @Query("SELECT a FROM CampaignActivity a "
            + "JOIN FETCH a.campaign c "
            + "LEFT JOIN FETCH a.actor "
            + "WHERE c.idx IN :campaignIds "
            + "ORDER BY a.createdAt DESC, a.idx DESC")
    List<CampaignActivity> findRecentByCampaignIdxIn(
            @Param("campaignIds") Collection<Long> campaignIds,
            Pageable pageable);
}
