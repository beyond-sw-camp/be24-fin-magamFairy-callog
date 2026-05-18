package org.example.backend.adcheck.analysis.repository;

import org.example.backend.adcheck.analysis.model.AdAnalysisRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdAnalysisRegionRepository extends JpaRepository<AdAnalysisRegion, Long> {
    @Query("""
            select r
            from AdAnalysisRegion r
            where r.page.document.idx = :documentIdx
            order by r.page.pageNo asc, r.orderIndex asc
            """)
    List<AdAnalysisRegion> findAllByDocumentIdx(@Param("documentIdx") Long documentIdx);

    @Query("select count(r) from AdAnalysisRegion r where r.page.document.idx = :documentIdx")
    long countByDocumentIdx(@Param("documentIdx") Long documentIdx);

    @Query("""
            select r
            from AdAnalysisRegion r
            where r.page.idx = :pageIdx
            order by r.orderIndex asc
            """)
    List<AdAnalysisRegion> findAllByPageIdx(@Param("pageIdx") Long pageIdx);

    @Query("""
            select r
            from AdAnalysisRegion r
            where r.page.document.idx = :documentIdx
              and r.regionKey = :regionKey
            """)
    List<AdAnalysisRegion> findAllByDocumentIdxAndRegionKey(
            @Param("documentIdx") Long documentIdx,
            @Param("regionKey") String regionKey
    );

    @Query(
            value = """
                    select distinct p.document_idx
                    from ad_analysis_region r
                    join ad_analysis_page p on p.idx = r.page_idx
                    where lower(coalesce(cast(r.extracted_text as char), '')) like lower(concat('%', :query, '%'))
                       or lower(coalesce(cast(r.labels_json as char), '')) like lower(concat('%', :query, '%'))
                    """,
            countQuery = """
                    select count(*)
                    from (
                        select distinct p.document_idx
                        from ad_analysis_region r
                        join ad_analysis_page p on p.idx = r.page_idx
                        where lower(coalesce(cast(r.extracted_text as char), '')) like lower(concat('%', :query, '%'))
                           or lower(coalesce(cast(r.labels_json as char), '')) like lower(concat('%', :query, '%'))
                    ) searched_documents
                    """,
            nativeQuery = true
    )
    Page<Long> searchDocumentIdsByTextOrLabels(@Param("query") String query, Pageable pageable);
}
