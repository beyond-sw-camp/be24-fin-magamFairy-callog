package org.example.backend.kpi.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.kpi.dto.CreateOrganizationKpiRequest;
import org.example.backend.kpi.dto.OrganizationKpiDto;
import org.example.backend.kpi.dto.UpdateOrganizationKpiRequest;
import org.example.backend.kpi.model.GoalKind;
import org.example.backend.kpi.model.GoalPeriodType;
import org.example.backend.kpi.model.GoalStatus;
import org.example.backend.kpi.model.OrganizationKpi;
import org.example.backend.kpi.repository.CampaignKpiContributionRepository;
import org.example.backend.kpi.repository.OrganizationKpiRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.organization.model.OrganizationType;
import org.example.backend.organization.repository.OrganizationRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationKpiService {

    private final OrganizationKpiRepository kpiRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CampaignKpiContributionRepository contributionRepository;

    // ── 조회 ────────────────────────────────────────────────

    public List<OrganizationKpiDto> list(String periodCode, Long ownerOrgId, GoalStatus status) {
        return kpiRepository.findByFilters(ownerOrgId, normalize(periodCode), status).stream()
                .map(OrganizationKpiDto::from)
                .toList();
    }

    public OrganizationKpiDto get(Long id) {
        return OrganizationKpiDto.from(findKpi(id));
    }

    public List<OrganizationKpiDto> listParentCandidates(Long orgId) {
        return kpiRepository.findActiveParentCandidates(orgId).stream()
                .map(OrganizationKpiDto::from)
                .toList();
    }

    // ── 생성 ────────────────────────────────────────────────

    @Transactional
    public OrganizationKpiDto create(Long callerIdx, CreateOrganizationKpiRequest req) {
        User caller = findUser(callerIdx);

        if (req.ownerOrgId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerOrgId는 필수입니다.");
        }
        Organization owner = organizationRepository.findById(req.ownerOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization을 찾을 수 없습니다."));

        // 권한 검증: 자기 조직의 KPI만 생성 가능 (HQ는 자기 조직(=HQ) 것만 만들 수 있음)
        requireOwnOrganization(caller, owner.getIdx());

        // parent KPI 검증
        OrganizationKpi parent = null;
        if (req.parentKpiId() != null) {
            parent = kpiRepository.findById(req.parentKpiId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent KPI를 찾을 수 없습니다."));
            if (parent.getStatus() == GoalStatus.ARCHIVED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보관된(ARCHIVED) KPI는 parent로 사용할 수 없습니다.");
            }
        }

        // ─── 필수 검증 (4개 필수: name / targetValue / unit / periodCode) ───
        String name = normalizeRequired(req.name(), "KPI 이름은 필수입니다.");
        if (req.targetValue() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetValue는 필수입니다.");
        }
        String unit = normalizeRequired(req.unit(), "unit은 필수입니다.");
        String periodCode = normalize(req.periodCode());
        if (periodCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodCode는 필수입니다. 예: 2026-Q2");
        }

        // ─── 자동 추론 (옵션 필드 default) ───
        GoalPeriodType periodType = req.periodType() != null
                ? req.periodType() : inferPeriodType(periodCode);
        LocalDate periodStart = req.periodStart();
        LocalDate periodEnd = req.periodEnd();
        if (periodStart == null || periodEnd == null) {
            LocalDate[] inferred = inferPeriodDates(periodCode);
            if (periodStart == null) periodStart = inferred[0];
            if (periodEnd == null) periodEnd = inferred[1];
        }
        KpiCategory category = req.category() != null ? req.category() : KpiCategory.OTHER;
        GoalKind kind = req.kind() != null ? req.kind() : defaultKindFor(owner);

        OrganizationKpi kpi = OrganizationKpi.builder()
                .owner(owner)
                .parentKpi(parent)
                .contributionToParent(req.contributionToParent())
                .name(name)
                .periodType(periodType)
                .periodCode(periodCode)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .targetValue(req.targetValue())
                .unit(unit)
                .category(category)
                .esgCategory(req.esgCategory())
                .kind(kind)
                .status(req.status() == null ? GoalStatus.DRAFT : req.status())
                .achievabilityNote(normalize(req.achievabilityNote()))
                .templateId(req.templateId())
                .createdBy(caller.getIdx())
                .updatedBy(caller.getIdx())
                .build();

        return OrganizationKpiDto.from(kpiRepository.save(kpi));
    }

    // ── 수정 ────────────────────────────────────────────────

    @Transactional
    public OrganizationKpiDto update(Long callerIdx, Long id, UpdateOrganizationKpiRequest req) {
        User caller = findUser(callerIdx);
        OrganizationKpi kpi = findKpi(id);
        requireOwnOrganization(caller, kpi.getOwner().getIdx());

        boolean lockedByActiveCampaign = contributionRepository.existsActiveCampaignReferencing(kpi.getIdx());

        // 진행 중 캠페인이 참조하면 name/unit/category 변경 금지 (target만 허용)
        if (lockedByActiveCampaign) {
            if (req.name() != null && !req.name().equals(kpi.getName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "진행 중 캠페인이 참조 중입니다. name은 변경할 수 없습니다.");
            }
            if (req.unit() != null && !req.unit().equals(kpi.getUnit())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "진행 중 캠페인이 참조 중입니다. unit은 변경할 수 없습니다.");
            }
            if (req.category() != null && req.category() != kpi.getCategory()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "진행 중 캠페인이 참조 중입니다. category는 변경할 수 없습니다.");
            }
        }

        // parent 변경 시 cascade 루프 검증
        if (req.parentKpiId() != null) {
            if (Objects.equals(req.parentKpiId(), kpi.getIdx())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신을 parent로 지정할 수 없습니다.");
            }
            requireNoCascadeLoop(kpi.getIdx(), req.parentKpiId());
            OrganizationKpi parent = kpiRepository.findById(req.parentKpiId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent KPI를 찾을 수 없습니다."));
            kpi.setParentKpi(parent);
        }

        if (req.contributionToParent() != null) kpi.setContributionToParent(req.contributionToParent());
        if (req.name() != null) kpi.setName(req.name());
        if (req.periodType() != null) kpi.setPeriodType(req.periodType());
        if (req.periodCode() != null) kpi.setPeriodCode(req.periodCode());
        if (req.periodStart() != null) kpi.setPeriodStart(req.periodStart());
        if (req.periodEnd() != null) kpi.setPeriodEnd(req.periodEnd());
        if (req.targetValue() != null) kpi.setTargetValue(req.targetValue());
        if (req.actualValue() != null) kpi.setActualValue(req.actualValue());
        if (req.unit() != null) kpi.setUnit(req.unit());
        if (req.category() != null) kpi.setCategory(req.category());
        if (req.esgCategory() != null) kpi.setEsgCategory(req.esgCategory());
        if (req.kind() != null) kpi.setKind(req.kind());
        if (req.achievabilityNote() != null) kpi.setAchievabilityNote(req.achievabilityNote());
        kpi.setUpdatedBy(caller.getIdx());

        return OrganizationKpiDto.from(kpi);
    }

    // ── 상태 변경 ───────────────────────────────────────────

    @Transactional
    public OrganizationKpiDto updateStatus(Long callerIdx, Long id, GoalStatus newStatus) {
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status는 필수입니다.");
        }
        User caller = findUser(callerIdx);
        OrganizationKpi kpi = findKpi(id);
        requireOwnOrganization(caller, kpi.getOwner().getIdx());

        // 상태 전이 검증: DRAFT → ACTIVE → ARCHIVED 순서만 허용
        validateStatusTransition(kpi.getStatus(), newStatus);

        kpi.setStatus(newStatus);
        kpi.setUpdatedBy(caller.getIdx());
        return OrganizationKpiDto.from(kpi);
    }

    // ── Helper ──────────────────────────────────────────────

    private User findUser(Long userIdx) {
        return userRepository.findById(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private OrganizationKpi findKpi(Long id) {
        return kpiRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OrganizationKpi not found."));
    }

    /**
     * 권한 검증: 본인 조직만 CRUD 가능.
     * - HQ Admin은 본인 조직(HQ) 것만.
     * - PA Admin은 본인 조직(AFFILIATE) 것만.
     * - HQ ROLE_ADMIN은 모든 조직 KPI를 관리할 수 있도록 예외.
     */
    private void requireOwnOrganization(User caller, Long targetOrgIdx) {
        if (caller.getOrganization() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조직 정보가 없는 계정입니다.");
        }
        if (caller.getOrganization().getType() == OrganizationType.EXTERNAL_PARTNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "외부 파트너는 OrganizationKpi를 관리할 수 없습니다.");
        }
        // HQ 관리자는 모든 조직 KPI 관리 가능
        if (caller.getOrganization().getType() == OrganizationType.HQ
                && "ROLE_ADMIN".equals(caller.getRole())) {
            return;
        }
        if (!Objects.equals(caller.getOrganization().getIdx(), targetOrgIdx)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 조직의 KPI만 관리할 수 있습니다.");
        }
    }

    /**
     * cascade 루프 방지: newParentId가 currentKpiId 또는 그 후손이면 BadRequest.
     */
    private void requireNoCascadeLoop(Long currentKpiId, Long newParentId) {
        Set<Long> descendants = collectDescendants(currentKpiId);
        if (descendants.contains(newParentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "cascade 루프가 발생합니다. parent로 지정할 수 없습니다.");
        }
    }

    private Set<Long> collectDescendants(Long rootId) {
        Set<Long> visited = new HashSet<>();
        visited.add(rootId);
        collectDescendantsRecursive(rootId, visited);
        return visited;
    }

    private void collectDescendantsRecursive(Long parentId, Set<Long> visited) {
        List<OrganizationKpi> children = kpiRepository.findAllByParentKpi_Idx(parentId);
        for (OrganizationKpi child : children) {
            if (visited.add(child.getIdx())) {
                collectDescendantsRecursive(child.getIdx(), visited);
            }
        }
    }

    private void validateStatusTransition(GoalStatus from, GoalStatus to) {
        if (from == to) return;
        if (from == GoalStatus.DRAFT && to == GoalStatus.ACTIVE) return;
        if (from == GoalStatus.ACTIVE && to == GoalStatus.ARCHIVED) return;
        if (from == GoalStatus.DRAFT && to == GoalStatus.ARCHIVED) return;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "허용되지 않는 상태 전이입니다: " + from + " → " + to);
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeRequired(String value, String message) {
        String n = normalize(value);
        if (n == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return n;
    }

    // ── 자동 추론 헬퍼 ──────────────────────────────────────

    /** 조직 type → 기본 위계 (HQ→STRATEGIC, AFFILIATE→TACTICAL) */
    private GoalKind defaultKindFor(Organization org) {
        if (org == null || org.getType() == null) return GoalKind.STRATEGIC;
        return switch (org.getType()) {
            case HQ -> GoalKind.STRATEGIC;
            case AFFILIATE, EXTERNAL_PARTNER -> GoalKind.TACTICAL;
        };
    }

    private static final Pattern QUARTER_PATTERN = Pattern.compile("^(\\d{4})-Q([1-4])$");
    private static final Pattern ANNUAL_PATTERN = Pattern.compile("^(\\d{4})(?:-FY)?$");

    /** periodCode 패턴 → GoalPeriodType */
    private GoalPeriodType inferPeriodType(String periodCode) {
        if (periodCode == null) return GoalPeriodType.QUARTERLY;
        if (QUARTER_PATTERN.matcher(periodCode).matches()) return GoalPeriodType.QUARTERLY;
        if (ANNUAL_PATTERN.matcher(periodCode).matches()) return GoalPeriodType.ANNUAL;
        return GoalPeriodType.CUSTOM;
    }

    /** "2026-Q2" → [2026-04-01, 2026-06-30] / "2026" → [2026-01-01, 2026-12-31] */
    private LocalDate[] inferPeriodDates(String periodCode) {
        if (periodCode == null) return new LocalDate[]{null, null};
        Matcher q = QUARTER_PATTERN.matcher(periodCode);
        if (q.matches()) {
            int year = Integer.parseInt(q.group(1));
            int quarter = Integer.parseInt(q.group(2));
            int startMonth = (quarter - 1) * 3 + 1;
            LocalDate start = LocalDate.of(year, startMonth, 1);
            LocalDate end = start.plusMonths(2);
            end = end.withDayOfMonth(end.lengthOfMonth());
            return new LocalDate[]{start, end};
        }
        Matcher y = ANNUAL_PATTERN.matcher(periodCode);
        if (y.matches()) {
            int year = Integer.parseInt(y.group(1));
            return new LocalDate[]{LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)};
        }
        return new LocalDate[]{null, null};
    }
}
