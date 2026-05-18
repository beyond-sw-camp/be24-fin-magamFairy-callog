package org.example.backend.adcheck.analysis.repository;

import org.example.backend.adcheck.analysis.model.AdAnalysisDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdAnalysisDocumentRepository extends JpaRepository<AdAnalysisDocument, Long> {
    Page<AdAnalysisDocument> findAllByOrderByIdxDesc(Pageable pageable);
}
