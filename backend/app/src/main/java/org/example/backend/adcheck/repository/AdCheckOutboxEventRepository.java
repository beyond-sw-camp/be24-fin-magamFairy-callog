package org.example.backend.adcheck.repository;

import org.example.backend.adcheck.model.AdCheckOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdCheckOutboxEventRepository extends JpaRepository<AdCheckOutboxEvent, Long> {
    List<AdCheckOutboxEvent> findTop50ByStatusInOrderByCreatedAtAsc(Collection<String> statuses);
}
