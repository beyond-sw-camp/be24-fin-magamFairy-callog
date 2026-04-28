package org.example.backend.organization.repository;

import org.example.backend.organization.model.Organization;
import org.example.backend.organization.model.OrganizationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByCode(String code);

    boolean existsByCode(String code);

    List<Organization> findAllByType(OrganizationType type);
}
