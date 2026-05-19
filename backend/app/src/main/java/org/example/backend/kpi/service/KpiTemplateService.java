package org.example.backend.kpi.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.redis.CacheNames;
import org.example.backend.kpi.dto.CreateKpiTemplateRequest;
import org.example.backend.kpi.dto.KpiTemplateDto;
import org.example.backend.kpi.dto.OrganizationKpiDto;
import org.example.backend.kpi.model.GoalStatus;
import org.example.backend.kpi.model.KpiTemplate;
import org.example.backend.kpi.model.OrganizationKpi;
import org.example.backend.kpi.model.TemplateScope;
import org.example.backend.kpi.repository.KpiTemplateRepository;
import org.example.backend.kpi.repository.OrganizationKpiRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.organization.repository.OrganizationRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiTemplateService {

    private final KpiTemplateRepository templateRepository;
    private final OrganizationKpiRepository kpiRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    /**
     * 템플릿 목록 — 거의 안 바뀌고 read-heavy 라 캐싱 (TTL 1시간).
     * 캐시 key 는 (scope, orgId) 조합. null 안전하게.
     */
    @Cacheable(value = CacheNames.KPI_TEMPLATES,
            key = "(#scope == null ? 'ALL' : #scope.name()) + ':' + (#orgId == null ? 'X' : #orgId)")
    public List<KpiTemplateDto> list(TemplateScope scope, Long orgId) {
        return templateRepository.findByFilters(scope, orgId).stream()
                .map(KpiTemplateDto::from)
                .toList();
    }

    @Transactional
    @CacheEvict(value = CacheNames.KPI_TEMPLATES, allEntries = true)   // 새 템플릿 생성 시 모든 목록 캐시 무효화
    public KpiTemplateDto create(Long callerIdx, CreateKpiTemplateRequest req) {
        User caller = findUser(callerIdx);
        requireAdminLike(caller);

        TemplateScope scope = req.scope() == null ? TemplateScope.ORG_ONLY : req.scope();

        Organization ownerOrg = null;
        if (scope == TemplateScope.ORG_ONLY) {
            Long ownerOrgId = req.ownerOrgId();
            if (ownerOrgId == null) {
                if (caller.getOrganization() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "ORG_ONLY 템플릿에는 ownerOrgId가 필요합니다.");
                }
                ownerOrgId = caller.getOrganization().getIdx();
            }
            ownerOrg = organizationRepository.findById(ownerOrgId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found."));
            // 본인 조직 외 ORG_ONLY 템플릿은 HQ 관리자만 생성 가능
            if (caller.getOrganization() != null
                    && !Objects.equals(caller.getOrganization().getIdx(), ownerOrg.getIdx())
                    && !(caller.getOrganization().getType() == OrganizationType.HQ
                            && "ROLE_ADMIN".equals(caller.getRole()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "다른 조직의 ORG_ONLY 템플릿은 생성할 수 없습니다.");
            }
        } else {
            // GLOBAL 템플릿은 HQ 관리자만 생성 가능
            if (caller.getOrganization() == null
                    || caller.getOrganization().getType() != OrganizationType.HQ) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "GLOBAL 템플릿은 HQ만 생성할 수 있습니다.");
            }
        }

        KpiTemplate template = KpiTemplate.builder()
                .name(normalizeRequired(req.name(), "템플릿 이름은 필수입니다."))
                .defaultUnit(req.defaultUnit())
                .defaultCategory(req.defaultCategory())
                .defaultEsgCategory(req.defaultEsgCategory())
                .defaultKind(req.defaultKind())
                .scope(scope)
                .ownerOrg(ownerOrg)
                .createdBy(caller.getIdx())
                .usageCount(0)
                .build();

        return KpiTemplateDto.from(templateRepository.save(template));
    }

    /**
     * 템플릿으로 새 OrganizationKpi 인스턴스 생성.
     * 호출자 조직을 owner로 함. period/target 등은 호출 시점에 기본값으로 채움.
     */
    @Transactional
    @CacheEvict(value = CacheNames.KPI_TEMPLATES, allEntries = true)   // usageCount 증가로 목록 응답이 변하므로
    public OrganizationKpiDto instantiate(Long callerIdx, Long templateId) {
        User caller = findUser(callerIdx);
        requireAdminLike(caller);

        if (caller.getOrganization() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조직 정보가 없는 계정입니다.");
        }

        KpiTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found."));

        // ORG_ONLY 템플릿은 본인 조직 것만 사용 가능
        if (template.getScope() == TemplateScope.ORG_ONLY
                && template.getOwnerOrg() != null
                && !Objects.equals(template.getOwnerOrg().getIdx(), caller.getOrganization().getIdx())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "다른 조직의 ORG_ONLY 템플릿은 사용할 수 없습니다.");
        }

        OrganizationKpi kpi = OrganizationKpi.builder()
                .owner(caller.getOrganization())
                .name(template.getName())
                .targetValue(BigDecimal.ZERO)
                .unit(template.getDefaultUnit() == null ? "" : template.getDefaultUnit())
                .category(template.getDefaultCategory())
                .esgCategory(template.getDefaultEsgCategory())
                .kind(template.getDefaultKind())
                .status(GoalStatus.DRAFT)
                .templateId(template.getIdx())
                .createdBy(caller.getIdx())
                .updatedBy(caller.getIdx())
                .periodStart(LocalDate.now())
                .build();

        OrganizationKpi saved = kpiRepository.save(kpi);
        template.incrementUsageCount();

        return OrganizationKpiDto.from(saved);
    }

    // ── Helper ──────────────────────────────────────────────

    private User findUser(Long userIdx) {
        return userRepository.findById(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private void requireAdminLike(User caller) {
        if (caller.getOrganization() != null
                && caller.getOrganization().getType() == OrganizationType.EXTERNAL_PARTNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "외부 파트너는 템플릿을 관리할 수 없습니다.");
        }
        String role = caller.getRole();
        if (!"ROLE_ADMIN".equals(role)
                && !"ROLE_GENERAL_MANAGER".equals(role)
                && !"ROLE_MANAGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "템플릿 관리 권한이 없습니다.");
        }
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
