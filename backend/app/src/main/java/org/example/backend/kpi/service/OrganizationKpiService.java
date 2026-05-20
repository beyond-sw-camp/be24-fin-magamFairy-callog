package org.example.backend.kpi.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.KpiCategory;
import org.example.backend.common.redis.DashboardCacheEvictor;
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
    private final DashboardCacheEvictor dashboardCacheEvictor;

    // ── 조회 ────────────────────────────────────────────────

    private static final java.util.regex.Pattern FY_PATTERN =
            java.util.regex.Pattern.compile("^(\\d{4})(?:-FY)?$");

    public List<OrganizationKpiDto> list(Long callerIdx, String periodCode, Long ownerOrgId, GoalStatus status) {
        User caller = callerIdx == null ? null : userRepository.findById(callerIdx).orElse(null);
        String normalizedPeriod = normalize(periodCode);

        List<OrganizationKpi> kpis;
        if (normalizedPeriod != null) {
            java.util.regex.Matcher m = FY_PATTERN.matcher(normalizedPeriod);
            if (m.matches()) {
                // "2026-FY" 또는 "2026" → 해당 연도 전체 분기 조회
                kpis = kpiRepository.findByFiltersForYear(ownerOrgId, m.group(1), status);
            } else {
                kpis = kpiRepository.findByFilters(ownerOrgId, normalizedPeriod, status);
            }
        } else {
            kpis = kpiRepository.findByFilters(ownerOrgId, null, status);
        }

        return kpis.stream()
                .filter(kpi -> isVisibleTo(kpi, caller))
                .map(OrganizationKpiDto::from)
                .toList();
    }

    /**
     * 가시성 규칙:
     * - HQ: 모든 OrganizationKpi 조회 가능
     * - AFFILIATE / EXTERNAL_PARTNER: 본사(HQ) KPI + 자기 조직 KPI만 조회 가능
     * - caller 정보가 없거나 조직 정보 없으면 본사 KPI만 (read-only fallback)
     */
    private boolean isVisibleTo(OrganizationKpi kpi, User caller) {
        Organization owner = kpi.getOwner();
        if (owner == null) return false;
        boolean ownerIsHq = owner.getType() == OrganizationType.HQ;

        if (caller == null || caller.getOrganization() == null) {
            // 인증 정보 없을 때는 본사 KPI만 노출 (방어적 default)
            return ownerIsHq;
        }
        // EXTERNAL_PARTNER: KPI 조회 전면 차단 (Task #3 — KPI 탭/데이터 완전 격리)
        if (caller.getOrganization().getType() == OrganizationType.EXTERNAL_PARTNER) {
            return false;
        }
        // 자기 조직 KPI는 항상 조회 가능 (HQ 자기 KPI, 계열사 자기 KPI)
        if (Objects.equals(owner.getIdx(), caller.getOrganization().getIdx())) {
            return true;
        }
        // 본사(HQ) caller: 자기 본사 KPI 에 매핑된(자식) 계열사 KPI 도 조회 가능 (읽기 전용 모니터링).
        // → "매핑 시킨 계열사 KPI만" 보임. 수정은 백엔드 requireOwnOrganization 으로 차단됨.
        if (caller.getOrganization().getType() == OrganizationType.HQ
                && kpi.getParentKpi() != null
                && kpi.getParentKpi().getOwner() != null
                && Objects.equals(kpi.getParentKpi().getOwner().getIdx(), caller.getOrganization().getIdx())) {
            return true;
        }
        // 본사(HQ) KPI는 '계열사 노출 ON(visibleToAffiliate)'인 것만 계열사에게 노출 (Task #4).
        // → HQ 전사 조회 제거 + 계열사는 매핑 가능한 본사 KPI만 봄.
        return ownerIsHq && Boolean.TRUE.equals(kpi.getVisibleToAffiliate());
    }

    public OrganizationKpiDto get(Long id) {
        return OrganizationKpiDto.from(findKpi(id));
    }

    public List<OrganizationKpiDto> listParentCandidates(Long callerIdx, Long orgId) {
        User caller = findUser(callerIdx);
        OrganizationType callerType = caller.getOrganization() != null
                ? caller.getOrganization().getType() : null;
        // 계열사: 본사가 노출(ON)한 HQ KPI 만 매핑 대상으로 제공
        List<OrganizationKpi> candidates = callerType == OrganizationType.AFFILIATE
                ? kpiRepository.findVisibleHqParentCandidates()
                : kpiRepository.findActiveParentCandidates(orgId);
        return candidates.stream()
                .map(OrganizationKpiDto::from)
                .toList();
    }

    // ── 생성 ────────────────────────────────────────────────

    @Transactional
    public OrganizationKpiDto create(Long callerIdx, CreateOrganizationKpiRequest req) {
        User caller = findUser(callerIdx);

        // ownerOrgId 미지정 시 caller 조직을 default로 사용
        // — frontend가 user.organization 정보 없이 요청해도 정상 동작
        Long resolvedOwnerOrgId = req.ownerOrgId();
        if (resolvedOwnerOrgId == null) {
            if (caller.getOrganization() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "ownerOrgId가 없고 caller 조직 정보도 없습니다.");
            }
            resolvedOwnerOrgId = caller.getOrganization().getIdx();
        }
        Organization owner = organizationRepository.findById(resolvedOwnerOrgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization을 찾을 수 없습니다."));

        // parent KPI 검증 (권한 검증보다 먼저 — AFFILIATE GM cascade-to-EXTERNAL 분기에 사용)
        OrganizationKpi parent = null;
        if (req.parentKpiId() != null) {
            parent = kpiRepository.findById(req.parentKpiId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent KPI를 찾을 수 없습니다."));
            if (parent.getStatus() == GoalStatus.ARCHIVED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보관된(ARCHIVED) KPI는 parent로 사용할 수 없습니다.");
            }
        }

        // 권한 검증: 자기 조직 KPI만 생성 가능. AFFILIATE GM이 parent 있는 경우 EXTERNAL_PARTNER cascade 허용.
        requireOwnOrganization(caller, owner.getIdx(), parent);

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
                // 계열사 노출: HQ 소유 KPI에서만 의미 (그 외는 false 고정)
                .visibleToAffiliate(
                        owner.getType() == OrganizationType.HQ
                                && Boolean.TRUE.equals(req.visibleToAffiliate()))
                .createdBy(caller.getIdx())
                .updatedBy(caller.getIdx())
                .build();

        OrganizationKpiDto result = OrganizationKpiDto.from(kpiRepository.save(kpi));
        // Dashboard 캐시 무효화 (quarterGoals 데이터 소스, summary.companyAvgPct 영향)
        dashboardCacheEvictor.evictAll();
        return result;
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
        // 계열사 노출 토글 — HQ 소유 KPI에서만 의미
        if (req.visibleToAffiliate() != null && kpi.getOwner().getType() == OrganizationType.HQ) {
            kpi.setVisibleToAffiliate(req.visibleToAffiliate());
        }
        kpi.setUpdatedBy(caller.getIdx());

        // Dashboard 캐시 무효화 (target 변경 시 quarterGoals 의 퍼센트 계산 영향)
        dashboardCacheEvictor.evictAll();
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
        // Dashboard 캐시 무효화 (ACTIVE 필터 조건 — quarterGoals 표시 여부 결정)
        dashboardCacheEvictor.evictAll();
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
     * - AFFILIATE GM이 EXTERNAL_PARTNER에 child KPI(cascade)를 만드는 경우 허용 ─ {@link #allowAffiliateCascadeToExternal}.
     */
    private void requireOwnOrganization(User caller, Long targetOrgIdx) {
        requireOwnOrganization(caller, targetOrgIdx, null);
    }

    private void requireOwnOrganization(User caller, Long targetOrgIdx, OrganizationKpi parentKpi) {
        if (caller.getOrganization() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "조직 정보가 없는 계정입니다.");
        }
        // ROLE_USER 차단 — KPI 관리는 GM/MANAGER/ADMIN만
        String role = caller.getRole();
        if ("ROLE_USER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "KPI 관리 권한이 없습니다. (GENERAL_MANAGER / MANAGER / ADMIN 필요)");
        }
        // HQ 관리자는 모든 조직 KPI 관리 가능
        if (caller.getOrganization().getType() == OrganizationType.HQ
                && "ROLE_ADMIN".equals(role)) {
            return;
        }
        if (Objects.equals(caller.getOrganization().getIdx(), targetOrgIdx)) {
            return;
        }
        // AFFILIATE GM이 자기 조직 KPI를 parent로 EXTERNAL_PARTNER에 child를 cascade 하는 경우 허용
        if (allowAffiliateCascadeToExternal(caller, targetOrgIdx, parentKpi)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 조직의 KPI만 관리할 수 있습니다.");
    }

    private boolean allowAffiliateCascadeToExternal(User caller, Long targetOrgIdx, OrganizationKpi parentKpi) {
        if (parentKpi == null) return false;
        if (caller.getOrganization().getType() != OrganizationType.AFFILIATE) return false;
        String role = caller.getRole();
        if (!"ROLE_GENERAL_MANAGER".equals(role) && !"ROLE_ADMIN".equals(role)) return false;
        // parent는 caller 자기 조직의 KPI여야 함
        if (parentKpi.getOwner() == null
                || !Objects.equals(parentKpi.getOwner().getIdx(), caller.getOrganization().getIdx())) {
            return false;
        }
        // target 조직은 EXTERNAL_PARTNER 여야 함
        Organization targetOrg = organizationRepository.findById(targetOrgIdx).orElse(null);
        return targetOrg != null && targetOrg.getType() == OrganizationType.EXTERNAL_PARTNER;
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
