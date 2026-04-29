package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.ApprovalStatus;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignAnalytics;
import org.example.backend.campaign.model.CampaignApprovalFlow;
import org.example.backend.campaign.model.CampaignComment;
import org.example.backend.campaign.model.CampaignContent;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.model.CampaignInvitation;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.model.CommentType;
import org.example.backend.campaign.model.ContentType;
import org.example.backend.campaign.model.InvitationStatus;
import org.example.backend.campaign.model.ParticipantStatus;
import org.example.backend.campaign.model.RiskLevel;
import org.example.backend.campaign.repository.CampaignAnalyticsRepository;
import org.example.backend.campaign.repository.CampaignApprovalFlowRepository;
import org.example.backend.campaign.repository.CampaignCommentRepository;
import org.example.backend.campaign.repository.CampaignContentRepository;
import org.example.backend.campaign.repository.CampaignInvitationRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.campaign.repository.CampaignVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {
    private static final String DEFAULT_COLOR = "#8B5CF6";
    private static final List<String> ALLOWED_STATUSES = List.of(
            "draft",
            "review",
            "in_review",
            "live",
            "partner_done",
            "paused",
            "completed"
    );

    private final CampaignRepository campaignRepository;
    private final CampaignInvitationRepository invitationRepository;
    private final CampaignParticipantRepository participantRepository;
    private final CampaignApprovalFlowRepository approvalFlowRepository;
    private final CampaignVersionRepository versionRepository;
    private final CampaignCommentRepository commentRepository;
    private final CampaignContentRepository contentRepository;
    private final CampaignAnalyticsRepository analyticsRepository;

    public List<CampaignDto.Res> getCampaigns(String ownerLoginId) {
        return getCampaigns(ownerLoginId, null, null, null, List.of(), null);
    }

    public List<CampaignDto.Res> getCampaigns(
            String ownerLoginId,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            List<String> tags,
            String keyword
    ) {
        List<String> normalizedTags = normalizeList(tags);
        String normalizedStatus = normalizeText(status);
        String normalizedKeyword = normalizeText(keyword).toLowerCase(Locale.ROOT);

        return campaignRepository.findAllByOwnerLoginIdOrderByIdxDesc(ownerLoginId).stream()
                .filter(campaign -> normalizedStatus.isBlank() || normalizedStatus.equals(campaign.getStatus()))
                .filter(campaign -> matchesDateRange(campaign, dateFrom, dateTo))
                .filter(campaign -> matchesTags(campaign, normalizedTags))
                .filter(campaign -> matchesKeyword(campaign, normalizedKeyword))
                .map(CampaignDto.Res::from)
                .toList();
    }

    @Transactional
    public CampaignDto.Res createCampaign(String ownerLoginId, CampaignDto.UpsertReq dto) {
        String name = normalizeRequired(dto.name(), "Campaign name is required.");

        Campaign campaign = Campaign.builder()
                .ownerLoginId(ownerLoginId)
                .name(name)
                .purpose(normalizeText(dto.purpose()))
                .tags(normalizeList(dto.tags()))
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .partners(normalizeList(dto.partners()))
                .goals(normalizeText(dto.goals()))
                .mainMessage(normalizeText(dto.mainMessage()))
                .status("draft")
                .initials(createInitials(name))
                .color(normalizeColor(dto.color()))
                .category(dto.category())
                .budget(dto.budget())
                .contactPerson(normalizeText(dto.contactPerson()))
                .contactEmail(normalizeText(dto.contactEmail()))
                .contactPhone(normalizeText(dto.contactPhone()))
                .riskLevel(defaultRiskLevel(dto.riskLevel()))
                .internalNotes(normalizeText(dto.internalNotes()))
                .build();

        Campaign savedCampaign = campaignRepository.save(campaign);
        saveCampaignVersion(savedCampaign, ownerLoginId, "Campaign created");
        return CampaignDto.Res.from(savedCampaign);
    }

    @Transactional
    public CampaignDto.Res updateCampaign(String ownerLoginId, Long campaignId, CampaignDto.UpsertReq dto) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        String name = normalizeRequired(dto.name(), "Campaign name is required.");

        campaign.updateDetails(
                name,
                normalizeText(dto.purpose()),
                normalizeList(dto.tags()),
                dto.startDate(),
                dto.endDate(),
                normalizeList(dto.partners()),
                normalizeText(dto.goals()),
                normalizeText(dto.mainMessage()),
                createInitials(name),
                normalizeColor(dto.color()),
                dto.category(),
                dto.budget(),
                normalizeText(dto.contactPerson()),
                normalizeText(dto.contactEmail()),
                normalizeText(dto.contactPhone()),
                defaultRiskLevel(dto.riskLevel()),
                normalizeText(dto.internalNotes())
        );
        saveCampaignVersion(campaign, ownerLoginId, "Campaign updated");

        return CampaignDto.Res.from(campaign);
    }

    @Transactional
    public CampaignDto.InvitationListRes invitePartners(
            String ownerLoginId,
            Long campaignId,
            CampaignDto.PartnerInviteReq dto
    ) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        List<String> partners = normalizeList(dto.partners());
        CampaignRole role = defaultRole(dto.role());
        List<CampaignDto.InvitationRes> invitations = new ArrayList<>();

        campaign.updatePartners(partners);
        for (String partner : partners) {
            CampaignInvitation invitation = CampaignInvitation.builder()
                    .campaign(campaign)
                    .invitee(partner)
                    .invitedBy(ownerLoginId)
                    .role(role)
                    .status(InvitationStatus.PENDING)
                    .expiryDate(LocalDate.now().plusDays(7))
                    .invitationToken(UUID.randomUUID().toString())
                    .build();
            CampaignInvitation savedInvitation = invitationRepository.save(invitation);
            invitations.add(CampaignDto.InvitationRes.from(savedInvitation));
        }

        saveCampaignVersion(campaign, ownerLoginId, "Campaign invitations created");
        return new CampaignDto.InvitationListRes(invitations);
    }

    @Transactional
    public CampaignDto.Res updateStatus(String ownerLoginId, Long campaignId, CampaignDto.StatusReq dto) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        String status = normalizeRequired(dto.status(), "Campaign status is required.");

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported campaign status.");
        }

        campaign.updateStatus(status);
        saveCampaignVersion(campaign, ownerLoginId, "Campaign status changed to " + status);
        return CampaignDto.Res.from(campaign);
    }

    public List<CampaignDto.InvitationRes> getInvitations(String ownerLoginId, Long campaignId) {
        getOwnedCampaign(ownerLoginId, campaignId);
        return invitationRepository.findByCampaignIdxOrderByIdxDesc(campaignId).stream()
                .map(CampaignDto.InvitationRes::from)
                .toList();
    }

    @Transactional
    public CampaignDto.InvitationRes acceptInvitation(String responderLoginId, Long campaignId, Long invitationId) {
        CampaignInvitation invitation = getInvitation(campaignId, invitationId);

        if (!invitation.isPending()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is not pending.");
        }

        if (invitation.isExpired()) {
            invitation.cancel();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired.");
        }

        invitation.accept(responderLoginId);
        CampaignParticipant participant = participantRepository
                .findByCampaignIdxAndUserLoginId(campaignId, responderLoginId)
                .orElseGet(() -> CampaignParticipant.builder()
                        .campaign(invitation.getCampaign())
                        .userLoginId(responderLoginId)
                        .campaignRole(invitation.getRole())
                        .participantStatus(ParticipantStatus.ACTIVE)
                        .joinedDate(LocalDate.now())
                        .build());
        participant.updateRole(invitation.getRole());
        participant.activate();
        participantRepository.save(participant);
        saveCampaignVersion(invitation.getCampaign(), responderLoginId, "Invitation accepted");
        return CampaignDto.InvitationRes.from(invitation);
    }

    @Transactional
    public CampaignDto.InvitationRes rejectInvitation(
            String responderLoginId,
            Long campaignId,
            Long invitationId,
            CampaignDto.InvitationDecisionReq dto
    ) {
        CampaignInvitation invitation = getInvitation(campaignId, invitationId);

        if (!invitation.isPending()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is not pending.");
        }

        invitation.reject(responderLoginId, normalizeText(dto.message()));
        saveCampaignVersion(invitation.getCampaign(), responderLoginId, "Invitation rejected");
        return CampaignDto.InvitationRes.from(invitation);
    }

    @Transactional
    public CampaignDto.InvitationRes cancelInvitation(String ownerLoginId, Long campaignId, Long invitationId) {
        getOwnedCampaign(ownerLoginId, campaignId);
        CampaignInvitation invitation = getInvitation(campaignId, invitationId);
        invitation.cancel();
        saveCampaignVersion(invitation.getCampaign(), ownerLoginId, "Invitation cancelled");
        return CampaignDto.InvitationRes.from(invitation);
    }

    public List<CampaignDto.ParticipantRes> getParticipants(String ownerLoginId, Long campaignId) {
        getOwnedCampaign(ownerLoginId, campaignId);
        return participantRepository.findByCampaignIdxOrderByIdxDesc(campaignId).stream()
                .map(CampaignDto.ParticipantRes::from)
                .toList();
    }

    @Transactional
    public CampaignDto.ParticipantRes updateParticipantRole(
            String ownerLoginId,
            Long campaignId,
            Long participantId,
            CampaignDto.RoleUpdateReq dto
    ) {
        getOwnedCampaign(ownerLoginId, campaignId);
        CampaignParticipant participant = participantRepository.findByIdxAndCampaignIdx(participantId, campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant was not found."));
        participant.updateRole(defaultRole(dto.role()));
        return CampaignDto.ParticipantRes.from(participant);
    }

    @Transactional
    public CampaignDto.ApprovalRes requestApproval(
            String ownerLoginId,
            Long campaignId,
            CampaignDto.ApprovalRequestReq dto
    ) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        String approverLoginId = normalizeRequired(dto.approverLoginId(), "Approver is required.");
        int sequence = (int) approvalFlowRepository.countByCampaignIdx(campaignId) + 1;
        CampaignApprovalFlow flow = CampaignApprovalFlow.builder()
                .campaign(campaign)
                .requestedBy(ownerLoginId)
                .requestedDate(LocalDate.now())
                .approverLoginId(approverLoginId)
                .status(ApprovalStatus.PENDING)
                .approverComment(normalizeText(dto.comment()))
                .sequence(sequence)
                .build();

        campaign.submitForApproval(approverLoginId);
        CampaignApprovalFlow savedFlow = approvalFlowRepository.save(flow);
        saveCampaignVersion(campaign, ownerLoginId, "Campaign approval requested");
        return CampaignDto.ApprovalRes.from(savedFlow);
    }

    public List<CampaignDto.ApprovalRes> getApprovals(String ownerLoginId, Long campaignId) {
        getOwnedCampaign(ownerLoginId, campaignId);
        return approvalFlowRepository.findByCampaignIdxOrderBySequenceAsc(campaignId).stream()
                .map(CampaignDto.ApprovalRes::from)
                .toList();
    }

    @Transactional
    public CampaignDto.ApprovalRes approveApproval(
            String currentLoginId,
            Long campaignId,
            Long approvalId,
            CampaignDto.ApprovalDecisionReq dto
    ) {
        CampaignApprovalFlow flow = getApproval(campaignId, approvalId);
        ensureApproverOrOwner(flow, currentLoginId);
        ensurePendingApproval(flow);

        flow.approve(normalizeText(dto.comment()));
        flow.getCampaign().markApproved(currentLoginId);
        saveCampaignVersion(flow.getCampaign(), currentLoginId, "Campaign approved");
        return CampaignDto.ApprovalRes.from(flow);
    }

    @Transactional
    public CampaignDto.ApprovalRes rejectApproval(
            String currentLoginId,
            Long campaignId,
            Long approvalId,
            CampaignDto.ApprovalDecisionReq dto
    ) {
        CampaignApprovalFlow flow = getApproval(campaignId, approvalId);
        ensureApproverOrOwner(flow, currentLoginId);
        ensurePendingApproval(flow);

        String comment = normalizeText(dto.comment());
        flow.reject(comment);
        flow.getCampaign().markRejected(currentLoginId, comment);
        saveCampaignVersion(flow.getCampaign(), currentLoginId, "Campaign rejected");
        return CampaignDto.ApprovalRes.from(flow);
    }

    public List<CampaignDto.CommentRes> getComments(String ownerLoginId, Long campaignId) {
        getOwnedCampaign(ownerLoginId, campaignId);
        return commentRepository.findByCampaignIdxOrderByIdxDesc(campaignId).stream()
                .map(CampaignDto.CommentRes::from)
                .toList();
    }

    @Transactional
    public CampaignDto.CommentRes addComment(String authorLoginId, Long campaignId, CampaignDto.CommentReq dto) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign was not found."));
        CampaignComment parent = dto.parentCommentId() == null
                ? null
                : commentRepository.findByIdxAndCampaignIdx(dto.parentCommentId(), campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment was not found."));
        CampaignComment comment = CampaignComment.builder()
                .campaign(campaign)
                .authorLoginId(authorLoginId)
                .content(normalizeRequired(dto.content(), "Comment content is required."))
                .commentType(dto.commentType() == null ? CommentType.GENERAL : dto.commentType())
                .targetField(normalizeText(dto.targetField()))
                .parentComment(parent)
                .build();

        return CampaignDto.CommentRes.from(commentRepository.save(comment));
    }

    @Transactional
    public CampaignDto.CommentRes resolveComment(
            String currentLoginId,
            Long campaignId,
            Long commentId
    ) {
        CampaignComment comment = commentRepository.findByIdxAndCampaignIdx(commentId, campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment was not found."));
        comment.markAsResolved(currentLoginId);
        return CampaignDto.CommentRes.from(comment);
    }

    public List<CampaignDto.VersionRes> getVersions(String ownerLoginId, Long campaignId) {
        getOwnedCampaign(ownerLoginId, campaignId);
        return versionRepository.findByCampaignIdxOrderByVersionNumberDesc(campaignId).stream()
                .map(CampaignDto.VersionRes::from)
                .toList();
    }

    public List<CampaignDto.ContentRes> getContents(String ownerLoginId, Long campaignId) {
        getOwnedCampaign(ownerLoginId, campaignId);
        return contentRepository.findByCampaignIdxOrderBySequenceAsc(campaignId).stream()
                .map(CampaignDto.ContentRes::from)
                .toList();
    }

    @Transactional
    public CampaignDto.ContentRes addContent(String ownerLoginId, Long campaignId, CampaignDto.ContentReq dto) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        CampaignContent content = CampaignContent.builder()
                .campaign(campaign)
                .contentId(dto.contentId())
                .contentType(dto.contentType() == null ? ContentType.DOCUMENT : dto.contentType())
                .sequence(dto.sequence() == null ? 1 : dto.sequence())
                .remarks(normalizeText(dto.remarks()))
                .build();

        saveCampaignVersion(campaign, ownerLoginId, "Campaign content added");
        return CampaignDto.ContentRes.from(contentRepository.save(content));
    }

    public CampaignDto.AnalyticsRes getAnalytics(String ownerLoginId, Long campaignId) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        CampaignAnalytics analytics = analyticsRepository.findByCampaignIdx(campaignId)
                .orElseGet(() -> CampaignAnalytics.builder()
                        .campaign(campaign)
                        .build());
        return CampaignDto.AnalyticsRes.from(analytics);
    }

    @Transactional
    public CampaignDto.AnalyticsRes updateAnalytics(
            String ownerLoginId,
            Long campaignId,
            CampaignDto.AnalyticsReq dto
    ) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        CampaignAnalytics analytics = analyticsRepository.findByCampaignIdx(campaignId)
                .orElseGet(() -> CampaignAnalytics.builder()
                        .campaign(campaign)
                        .build());
        analytics.updateMetrics(dto.impressions(), dto.clicks(), dto.conversions(), dto.totalSpent());
        analytics.updateProgress(dto.participantCount(), dto.completionCount());
        CampaignAnalytics savedAnalytics = analyticsRepository.save(analytics);
        saveCampaignVersion(campaign, ownerLoginId, "Campaign analytics updated");
        return CampaignDto.AnalyticsRes.from(savedAnalytics);
    }

    @Transactional
    public CampaignDto.Res cloneCampaign(String ownerLoginId, Long sourceCampaignId, CampaignDto.CloneReq dto) {
        Campaign source = getOwnedCampaign(ownerLoginId, sourceCampaignId);
        String name = normalizeRequired(dto.name(), "Campaign name is required.");
        Campaign clone = Campaign.builder()
                .ownerLoginId(ownerLoginId)
                .name(name)
                .purpose(source.getPurpose())
                .tags(new ArrayList<>(source.getTags()))
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .partners(new ArrayList<>(source.getPartners()))
                .goals(source.getGoals())
                .mainMessage(source.getMainMessage())
                .status("draft")
                .initials(createInitials(name))
                .color(source.getColor())
                .category(source.getCategory())
                .budget(source.getBudget())
                .contactPerson(source.getContactPerson())
                .contactEmail(source.getContactEmail())
                .contactPhone(source.getContactPhone())
                .approvalStatus(ApprovalStatus.PENDING)
                .riskLevel(source.getRiskLevel())
                .internalNotes(source.getInternalNotes())
                .templateSourceId(String.valueOf(sourceCampaignId))
                .build();

        Campaign savedClone = campaignRepository.save(clone);
        saveCampaignVersion(savedClone, ownerLoginId, "Campaign cloned from " + sourceCampaignId);
        return CampaignDto.Res.from(savedClone);
    }

    private Campaign getOwnedCampaign(String ownerLoginId, Long campaignId) {
        return campaignRepository.findByIdxAndOwnerLoginId(campaignId, ownerLoginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign was not found."));
    }

    private CampaignInvitation getInvitation(Long campaignId, Long invitationId) {
        return invitationRepository.findByIdxAndCampaignIdx(invitationId, campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation was not found."));
    }

    private CampaignApprovalFlow getApproval(Long campaignId, Long approvalId) {
        return approvalFlowRepository.findByIdxAndCampaignIdx(approvalId, campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval was not found."));
    }

    private void ensureApproverOrOwner(CampaignApprovalFlow flow, String currentLoginId) {
        if (currentLoginId.equals(flow.getApproverLoginId())
                || currentLoginId.equals(flow.getCampaign().getOwnerLoginId())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approval permission is required.");
    }

    private void ensurePendingApproval(CampaignApprovalFlow flow) {
        if (flow.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval is not pending.");
        }
    }

    private void saveCampaignVersion(Campaign campaign, String changedBy, String changeDescription) {
        List<org.example.backend.campaign.model.CampaignVersion> versions =
                versionRepository.findByCampaignIdxOrderByVersionNumberDesc(campaign.getIdx());
        int nextVersion = versions == null || versions.isEmpty()
                ? 1
                : versions.get(0).getVersionNumber() + 1;

        org.example.backend.campaign.model.CampaignVersion version =
                org.example.backend.campaign.model.CampaignVersion.builder()
                        .campaign(campaign)
                        .versionNumber(nextVersion)
                        .changedBy(changedBy)
                        .changedDate(LocalDate.now())
                        .changeDescription(changeDescription)
                        .campaignSnapshot(createCampaignSnapshot(campaign))
                        .changeLog(changeDescription)
                        .build();
        versionRepository.save(version);
    }

    private String createCampaignSnapshot(Campaign campaign) {
        return "idx=" + campaign.getIdx()
                + ";name=" + campaign.getName()
                + ";status=" + campaign.getStatus()
                + ";approvalStatus=" + campaign.getApprovalStatus();
    }

    private static boolean matchesDateRange(Campaign campaign, LocalDate dateFrom, LocalDate dateTo) {
        LocalDate startDate = campaign.getStartDate();
        LocalDate endDate = campaign.getEndDate();

        if (dateFrom != null && startDate != null && startDate.isBefore(dateFrom)) {
            return false;
        }

        return dateTo == null || endDate == null || !endDate.isAfter(dateTo);
    }

    private static boolean matchesTags(Campaign campaign, List<String> tags) {
        if (tags.isEmpty()) {
            return true;
        }

        List<String> campaignTags = campaign.getTags().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        return tags.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(campaignTags::contains);
    }

    private static boolean matchesKeyword(Campaign campaign, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        return containsIgnoreCase(campaign.getName(), keyword)
                || containsIgnoreCase(campaign.getPurpose(), keyword)
                || containsIgnoreCase(campaign.getGoals(), keyword)
                || containsIgnoreCase(campaign.getMainMessage(), keyword);
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static String normalizeRequired(String value, String message) {
        String normalized = normalizeText(value);

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return new ArrayList<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private static String normalizeColor(String color) {
        String normalized = normalizeText(color);
        return normalized.isBlank() ? DEFAULT_COLOR : normalized;
    }

    private static CampaignRole defaultRole(CampaignRole role) {
        return role == null ? CampaignRole.PARTNER : role;
    }

    private static RiskLevel defaultRiskLevel(RiskLevel riskLevel) {
        return riskLevel == null ? RiskLevel.LOW : riskLevel;
    }

    private static String createInitials(String name) {
        String normalized = normalizeText(name);

        if (normalized.isBlank()) {
            return "CP";
        }

        String[] words = normalized.split("\\s+");
        String initials = words.length > 1
                ? words[0].substring(0, 1) + words[1].substring(0, 1)
                : normalized.substring(0, Math.min(2, normalized.length()));

        return initials.toUpperCase(Locale.ROOT);
    }
}
