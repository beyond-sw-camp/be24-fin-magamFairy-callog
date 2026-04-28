package org.example.backend.organization.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    public static final String HQ_CODE = "HQ";
    public static final String HQ_NAME = "Headquarters";

    private final OrganizationRepository organizationRepository;

    @Transactional
    public Organization ensureHeadquarters() {
        return organizationRepository.findByCode(HQ_CODE)
                .orElseGet(() -> organizationRepository.save(
                        Organization.builder()
                                .code(HQ_CODE)
                                .name(HQ_NAME)
                                .type(OrganizationType.HQ)
                                .canCreateCampaign(true)
                                .build()
                ));
    }
}
