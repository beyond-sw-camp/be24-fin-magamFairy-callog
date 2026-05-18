package org.example.backend.adcheck.analysis.repository;

import org.example.backend.adcheck.analysis.model.AdAnalysisKeyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdAnalysisKeywordRepository extends JpaRepository<AdAnalysisKeyword, Long> {
    @Query("""
            select k
            from AdAnalysisKeyword k
            where k.document.idx = :documentIdx
            order by k.weight desc
            """)
    List<AdAnalysisKeyword> findAllByDocumentIdx(@Param("documentIdx") Long documentIdx);

    @Query("""
            select distinct k.document.idx
            from AdAnalysisKeyword k
            where lower(k.keyword) like lower(concat('%', :query, '%'))
            """)
    Page<Long> searchDocumentIdsByKeyword(@Param("query") String query, Pageable pageable);
}
