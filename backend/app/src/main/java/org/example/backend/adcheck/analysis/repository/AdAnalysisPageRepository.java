package org.example.backend.adcheck.analysis.repository;

import org.example.backend.adcheck.analysis.model.AdAnalysisPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdAnalysisPageRepository extends JpaRepository<AdAnalysisPage, Long> {
    @Query("select p from AdAnalysisPage p where p.document.idx = :documentIdx order by p.pageNo asc")
    List<AdAnalysisPage> findAllByDocumentIdx(@Param("documentIdx") Long documentIdx);
}
