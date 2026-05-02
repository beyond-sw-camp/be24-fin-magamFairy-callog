package org.example.backend.organization.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

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

    @Transactional
    public Organization ensureAffiliateOrganization(String companyName) {
        return organizationRepository.findByNameIgnoreCase(companyName)
                .orElseGet(() -> {
                    String base = companyName.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
                    if (base.length() > 20) {
                        base = base.substring(0, 20);
                    }
                    String code = base;
                    int suffix = 2;
                    while (organizationRepository.existsByCode(code)) {
                        code = base + suffix;
                        suffix += 1;
                    }
                    return organizationRepository.save(
                            Organization.builder()
                                    .code(code)
                                    .name(companyName)
                                    .type(OrganizationType.AFFILIATE)
                                    .canCreateCampaign(false)
                                    .build()
                    );
                });
    }

    @Transactional
    public Organization createPartnerOrganization(String companyName) {
        String base = companyName.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        String code = base;
        int suffix = 2;
        while (organizationRepository.existsByCode(code)) {
            code = base + suffix;
            suffix += 1;
        }
        return organizationRepository.save(
                Organization.builder()
                        .code(code)
                        .name(companyName)
                        .type(OrganizationType.EXTERNAL_PARTNER)
                        .canCreateCampaign(false)
                        .build()
        );
    }
}
