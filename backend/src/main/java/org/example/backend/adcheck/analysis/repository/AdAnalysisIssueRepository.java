package org.example.backend.adcheck.analysis.repository;

import org.example.backend.adcheck.analysis.model.AdAnalysisIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdAnalysisIssueRepository extends JpaRepository<AdAnalysisIssue, Long> {
    @Query("""
            select i
            from AdAnalysisIssue i
            where i.document.idx = :documentIdx
            order by i.pageNo asc, i.regionKey asc
            """)
    List<AdAnalysisIssue> findAllByDocumentIdx(@Param("documentIdx") Long documentIdx);

    @Query("select count(i) from AdAnalysisIssue i where i.document.idx = :documentIdx")
    long countByDocumentIdx(@Param("documentIdx") Long documentIdx);

    @Query("""
            select i
            from AdAnalysisIssue i
            where i.document.idx = :documentIdx
              and i.pageNo = :pageNo
            order by i.regionKey asc
            """)
    List<AdAnalysisIssue> findAllByDocumentIdxAndPageNo(
            @Param("documentIdx") Long documentIdx,
            @Param("pageNo") Integer pageNo
    );

    @Query(
            value = """
                    select distinct i.document_idx
                    from ad_analysis_issue i
                    where lower(coalesce(cast(i.target_text as char), '')) like lower(concat('%', :query, '%'))
                       or lower(coalesce(cast(i.reason as char), '')) like lower(concat('%', :query, '%'))
                       or lower(coalesce(cast(i.suggestion as char), '')) like lower(concat('%', :query, '%'))
                    """,
            countQuery = """
                    select count(*)
                    from (
                        select distinct i.document_idx
                        from ad_analysis_issue i
                        where lower(coalesce(cast(i.target_text as char), '')) like lower(concat('%', :query, '%'))
                           or lower(coalesce(cast(i.reason as char), '')) like lower(concat('%', :query, '%'))
                           or lower(coalesce(cast(i.suggestion as char), '')) like lower(concat('%', :query, '%'))
                    ) searched_documents
                    """,
            nativeQuery = true
    )
    Page<Long> searchDocumentIdsByIssueText(@Param("query") String query, Pageable pageable);
}
