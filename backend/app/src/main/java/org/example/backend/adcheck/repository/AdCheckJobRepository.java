package org.example.backend.adcheck.repository;

import org.example.backend.adcheck.model.AdCheckJob;
import org.example.backend.adcheck.model.AdCheckJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdCheckJobRepository extends JpaRepository<AdCheckJob, Long> {
    Optional<AdCheckJob> findByJobId(String jobId);

    Optional<AdCheckJob> findByJobIdAndRequester_Idx(String jobId, Long requesterIdx);

    List<AdCheckJob> findAllByRequester_IdxAndStatusInOrderByCreatedAtDesc(
            Long requesterIdx,
            Collection<AdCheckJobStatus> statuses
    );

    List<AdCheckJob> findAllByStatusInOrderByCreatedAtAsc(Collection<AdCheckJobStatus> statuses);
}
