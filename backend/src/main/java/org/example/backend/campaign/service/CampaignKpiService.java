package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.*;
import org.example.backend.campaign.repository.CampaignKpiRepository;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignKpiService {

    private final CampaignKpiRepository kpiRepository;
    private final CampaignMemberRepository memberRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final KpiFrameworkCatalog catalog;

    // ── 조회 ────────────────────────────────────────────────

    public CampaignKpiDto.ListRes listKpis(Long campaignId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = findMember(campaignId, caller.getIdx());
        Campaign campaign = findCampaign(campaignId);

        List<CampaignKpi> kpis = kpiRepository.findAllByCampaignIdxOrderByIdxAsc(campaignId);
        List<CampaignKpiDto.Res> items = kpis.stream().map(CampaignKpiDto.Res::from).toList();

        boolean isManager = me.getCampaignRole() == CampaignMemberRole.MANAGER
                || me.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER;
        boolean editable = isManager && !campaign.isClosed();

        return new CampaignKpiDto.ListRes(items, buildSummary(items), campaign.getKpiAnalysis(), editable);
    }

    private CampaignKpiDto.Summary buildSummary(List<CampaignKpiDto.Res> items) {
        int totalCount = items.size();
        int achievedCount = (int) items.stream()
                .filter(r -> r.status() == KpiStatus.ACHIEVED || r.status() == KpiStatus.OVER).count();
        int pendingCount = (int) items.stream().filter(r -> r.status() == KpiStatus.PENDING).count();

        List<CampaignKpiDto.Res> measured = items.stream()
                .filter(r -> r.achievementPercent() != null).toList();
        int overall = measured.isEmpty() ? 0 :
                (int) measured.stream().mapToInt(CampaignKpiDto.Res::achievementPercent).average().orElse(0);

        BigDecimal totalImpression = items.stream()
                .filter(r -> r.category() == KpiCategory.IMPRESSION && r.actualValue() != null)
                .map(CampaignKpiDto.Res::actualValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalClicks = items.stream()
                .filter(r -> r.category() == KpiCategory.ENGAGEMENT && r.actualValue() != null)
                .map(CampaignKpiDto.Res::actualValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CampaignKpiDto.Summary(overall, totalImpression, totalClicks,
                totalCount, achievedCount, pendingCount);
    }

    // ── 추가 ────────────────────────────────────────────────

    @Transactional
    public CampaignKpiDto.Res createKpi(Long campaignId, String callerLoginId, CampaignKpiDto.CreateReq req) {
        User caller = findUser(callerLoginId);
        CampaignMember me = findMember(campaignId, caller.getIdx());
        requireEditable(me);
        Campaign campaign = findCampaign(campaignId);
        requireOpen(campaign);

        User ownerUser = req.ownerUserIdx() != null
                ? userRepository.findById(req.ownerUserIdx())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "담당자를 찾을 수 없습니다."))
                : null;

        CampaignKpi kpi = CampaignKpi.builder()
                .campaign(campaign)
                .name(req.name())
                .category(req.category())
                .targetValue(req.targetValue())
                .unit(req.unit())
                .ownerLabel(req.ownerLabel())
                .ownerUser(ownerUser)
                .build();
        return CampaignKpiDto.Res.from(kpiRepository.save(kpi));
    }

    // ── 메타 수정 ────────────────────────────────────────────

    @Transactional
    public CampaignKpiDto.Res updateMeta(Long campaignId, Long kpiId, String callerLoginId,
                                         CampaignKpiDto.UpdateMetaReq req) {
        User caller = findUser(callerLoginId);
        CampaignMember me = findMember(campaignId, caller.getIdx());
        requireEditable(me);
        CampaignKpi kpi = findKpi(campaignId, kpiId);
        requireOpen(kpi.getCampaign());

        User ownerUser = req.ownerUserIdx() != null
                ? userRepository.findById(req.ownerUserIdx())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "담당자를 찾을 수 없습니다."))
                : null;

        kpi.setName(req.name());
        kpi.setCategory(req.category());
        kpi.setTargetValue(req.targetValue());
        kpi.setUnit(req.unit());
        kpi.setOwnerLabel(req.ownerLabel());
        kpi.setOwnerUser(ownerUser);
        return CampaignKpiDto.Res.from(kpi);
    }

    // ── 실적값 입력 ──────────────────────────────────────────

    @Transactional
    public CampaignKpiDto.Res updateActual(Long campaignId, Long kpiId, String callerLoginId,
                                           CampaignKpiDto.UpdateActualReq req) {
        User caller = findUser(callerLoginId);
        CampaignMember me = findMember(campaignId, caller.getIdx());
        requireEditable(me);
        CampaignKpi kpi = findKpi(campaignId, kpiId);
        requireOpen(kpi.getCampaign());

        kpi.setActualValue(req.actualValue());
        kpi.setMemo(req.memo());
        kpi.setNextAction(req.nextAction());
        kpi.setMeasuredAt(LocalDateTime.now());
        return CampaignKpiDto.Res.from(kpi);
    }

    // ── 삭제 ────────────────────────────────────────────────

    @Transactional
    public void deleteKpi(Long campaignId, Long kpiId, String callerLoginId) {
        User caller = findUser(callerLoginId);
        CampaignMember me = findMember(campaignId, caller.getIdx());
        requireEditable(me);
        CampaignKpi kpi = findKpi(campaignId, kpiId);
        requireOpen(kpi.getCampaign());
        kpiRepository.delete(kpi);
    }

    // ── 성과 분석 메모 ────────────────────────────────────────

    @Transactional
    public void updateAnalysis(Long campaignId, String callerLoginId, CampaignKpiDto.UpdateAnalysisReq req) {
        User caller = findUser(callerLoginId);
        CampaignMember me = findMember(campaignId, caller.getIdx());
        requireEditable(me);
        Campaign campaign = findCampaign(campaignId);
        requireOpen(campaign);
        campaign.updateKpiAnalysis(req.kpiAnalysis());
    }

    // ── 프레임워크 목록 ──────────────────────────────────────

    public List<CampaignKpiDto.FrameworkRes> listFrameworks() {
        return catalog.listAll();
    }

    // ── 프레임워크 일괄 등록 ──────────────────────────────────

    @Transactional
    public List<CampaignKpiDto.Res> importFramework(Long campaignId, String callerLoginId,
                                                     CampaignKpiDto.ImportFrameworkReq req) {
        User caller = findUser(callerLoginId);
        CampaignMember me = findMember(campaignId, caller.getIdx());
        requireEditable(me);
        Campaign campaign = findCampaign(campaignId);
        requireOpen(campaign);

        CampaignKpiDto.FrameworkRes fw = catalog.get(req.frameworkKey());
        List<CampaignKpi> created = fw.items().stream().map(item ->
            CampaignKpi.builder()
                .campaign(campaign)
                .name(item.name())
                .category(item.category())
                .targetValue(item.defaultTarget())
                .unit(item.unit())
                .build()
        ).toList();
        return kpiRepository.saveAll(created).stream().map(CampaignKpiDto.Res::from).toList();
    }

    // ── Helper ───────────────────────────────────────────────

    private User findUser(String loginId) {
        return userRepository.findUserById(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private CampaignMember findMember(Long campaignId, Long userIdx) {
        CampaignMember me = memberRepository
                .findByCampaignIdxAndUserIdx(campaignId, userIdx)
                .orElse(null);
        return CampaignMemberGuard.requireMember(me);
    }

    private Campaign findCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
    }

    private CampaignKpi findKpi(Long campaignId, Long kpiId) {
        CampaignKpi kpi = kpiRepository.findById(kpiId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KPI not found."));
        if (!Objects.equals(kpi.getCampaign().getIdx(), campaignId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KPI does not belong to this campaign.");
        }
        return kpi;
    }

    private void requireOpen(Campaign campaign) {
        if (campaign.isClosed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "종료된 캠페인은 수정할 수 없습니다.");
        }
    }

    private void requireEditable(CampaignMember me) {
        if (me.getCampaignRole() == CampaignMemberRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "편집 권한이 없습니다.");
        }
    }
}
