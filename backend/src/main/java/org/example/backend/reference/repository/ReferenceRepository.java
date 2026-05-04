package org.example.backend.reference.repository;

import org.example.backend.reference.model.Reference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferenceRepository extends JpaRepository<Reference, Long> {
    List<Reference> findAllByOwnerLoginIdOrderByIdxDesc(String ownerLoginId);

    Optional<Reference> findByIdxAndOwnerLoginId(Long idx, String ownerLoginId);
}
