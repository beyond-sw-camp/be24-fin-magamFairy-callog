package org.example.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.model.AdReviewRequest;
import org.example.backend.campaign.model.CampaignInvitation;
import org.example.backend.campaign.model.CampaignInvitationStatus;
import org.example.backend.campaign.model.CampaignInvitationType;
import org.example.backend.campaign.model.CampaignMemberDto;
import org.example.backend.campaign.repository.CampaignInvitationRepository;
import org.example.backend.notification.model.Notification;
import org.example.backend.notification.model.NotificationDto;
import org.example.backend.notification.model.NotificationSeverity;
import org.example.backend.notification.model.NotificationType;
import org.example.backend.notification.repository.NotificationRepository;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskStatus;
import org.example.backend.user.model.User;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.UserAccountStatus;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final int MAX_LIST_LIMIT = 100;
    private static final String REFERENCE_CAMPAIGN_INVITATION = "CAMPAIGN_INVITATION";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CampaignInvitationRepository campaignInvitationRepository;
    private final NotificationSseService notificationSseService;
    private final NotificationPreferenceResolver preferenceResolver;

    public NotificationDto.ListRes list(Long recipientIdx, Integer count) {
        int limit = count == null ? DEFAULT_LIST_LIMIT : Math.min(Math.max(count, 1), MAX_LIST_LIMIT);
        List<Notification> raw = notificationRepository
                .findAllByRecipient_IdxOrderByCreatedAtDesc(recipientIdx, PageRequest.of(0, limit));
        List<NotificationDto.Res> notifications = raw.stream()
                .map(this::toRes)
                .toList();
        long unreadCount = notificationRepository.countByRecipient_IdxAndIsReadFalse(recipientIdx);
        return new NotificationDto.ListRes(notifications, unreadCount);
    }

    private NotificationDto.Res toRes(Notification notification) {
        if (REFERENCE_CAMPAIGN_INVITATION.equals(notification.getReferenceType())
                && notification.getReferenceId() != null) {
            return campaignInvitationRepository.findById(notification.getReferenceId())
                    .filter(inv -> inv.getType() == CampaignInvitationType.GROUP)
                    .map(this::toGroupPreview)
                    .map(preview -> NotificationDto.Res.from(notification, preview))
                    .orElseGet(() -> NotificationDto.Res.from(notification));
        }
        return NotificationDto.Res.from(notification);
    }

    private CampaignMemberDto.GroupPreview toGroupPreview(CampaignInvitation invitation) {
        Organization inviteeOrg = invitation.getInviteeOrganization();
        if (inviteeOrg == null) return null;
        List<User> users = userRepository.findAllByOrganization_IdxAndAccountStatus(
                inviteeOrg.getIdx(), UserAccountStatus.ACTIVE);
        List<CampaignMemberDto.GroupPreview.GroupPreviewMember> members = users.stream()
                .map(u -> CampaignMemberDto.GroupPreview.GroupPreviewMember.builder()
                        .name(u.getName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .toList();
        return CampaignMemberDto.GroupPreview.builder()
                .organizationName(inviteeOrg.getName())
                .members(members)
                .build();
    }

    @Transactional
    public NotificationDto.Res confirm(Long recipientIdx, Long notificationIdx) {
        Notification notification = notificationRepository.findByIdxAndRecipient_Idx(notificationIdx, recipientIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notification not found."));

        notification.markAsRead();
        return NotificationDto.Res.from(notification);
    }

    @Transactional
    public NotificationDto.ListRes confirmAll(Long recipientIdx) {
        notificationRepository.findAllByRecipient_IdxAndIsReadFalse(recipientIdx)
                .forEach(Notification::markAsRead);

        return list(recipientIdx, DEFAULT_LIST_LIMIT);
    }

    @Transactional
    public NotificationDto.Res create(NotificationDto.CreateReq request, User sender) {
        if (request == null || request.recipientIdx() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientIdx is required.");
        }

        User recipient = userRepository.findById(request.recipientIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "recipient not found."));

        return create(
                recipient,
                sender,
                request.type(),
                request.severity(),
                request.title(),
                request.message(),
                request.detail(),
                request.targetLabel(),
                request.targetUrl()
        );
    }

    @Transactional
    public NotificationDto.Res create(
            User recipient,
            User sender,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl
    ) {
        return create(
                recipient,
                sender,
                type,
                severity,
                title,
                message,
                detail,
                targetLabel,
                targetUrl,
                null,
                null,
                null,
                null,
                false
        );
    }

    @Transactional
    public NotificationDto.Res create(
            User recipient,
            User sender,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl,
            String dedupeKey,
            String referenceType,
            Long referenceId,
            String referenceStatus,
            boolean force
    ) {
        if (recipient == null || recipient.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipient is required.");
        }

        NotificationType nextType = type == null ? NotificationType.SYSTEM : type;
        NotificationSeverity nextSeverity = severity == null ? NotificationSeverity.NORMAL : severity;
        String nextDedupeKey = normalize(dedupeKey);

        if (nextDedupeKey != null) {
            Notification existing = notificationRepository.findByDedupeKey(nextDedupeKey).orElse(null);
            if (existing != null) {
                return NotificationDto.Res.from(existing);
            }
        }

        if (!force && !preferenceResolver.shouldCreate(recipient, nextType, nextSeverity)) {
            return null;
        }

        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .type(nextType)
                .severity(nextSeverity)
                .title(nonBlank(title, "Notification"))
                .message(nonBlank(message, "New notification has arrived."))
                .detail(detail)
                .targetLabel(targetLabel)
                .targetUrl(targetUrl)
                .dedupeKey(nextDedupeKey)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceStatus(referenceStatus)
                .build());
        NotificationDto.Res response = NotificationDto.Res.from(notification);
        notificationSseService.sendToUser(recipient.getIdx(), response);

        return response;
    }

    @Transactional
    public void createForRecipients(
            Collection<User> recipients,
            User sender,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl
    ) {
        createForRecipients(recipients, sender, type, severity, title, message, detail, targetLabel, targetUrl, null);
    }

    @Transactional
    public void createForRecipients(
            Collection<User> recipients,
            User sender,
            NotificationType type,
            NotificationSeverity severity,
            String title,
            String message,
            String detail,
            String targetLabel,
            String targetUrl,
            Function<User, String> dedupeKeyFactory
    ) {
        uniqueUsers(recipients).forEach(recipient ->
                create(
                        recipient,
                        sender,
                        type,
                        severity,
                        title,
                        message,
                        detail,
                        targetLabel,
                        targetUrl,
                        dedupeKeyFactory == null ? null : dedupeKeyFactory.apply(recipient),
                        null,
                        null,
                        null,
                        false
                ));
    }

    @Transactional
    public void notifyTaskAssigned(Task task, User sender) {
        if (task == null || task.getAssignee() == null) {
            return;
        }

        create(
                task.getAssignee(),
                sender,
                NotificationType.TASK_ASSIGNED,
                NotificationSeverity.NORMAL,
                "새 업무가 배정되었습니다.",
                task.getName(),
                "배정된 업무의 담당자, 마감일, 우선순위를 확인해 주세요.",
                "업무 보드로 이동",
                "/team-board"
        );
    }

    @Transactional
    public void notifyTaskStatusChanged(Task task, TaskStatus previousStatus, TaskStatus nextStatus, User sender) {
        if (task == null || task.getAssignee() == null || previousStatus == nextStatus) {
            return;
        }

        create(
                task.getAssignee(),
                sender,
                NotificationType.TASK_STATUS_CHANGED,
                NotificationSeverity.NORMAL,
                "업무 상태가 변경되었습니다.",
                task.getName() + " : " + previousStatus + " -> " + nextStatus,
                "해당 업무의 진행 상태가 변경되었습니다.",
                "업무 보드로 이동",
                "/team-board"
        );
    }

    @Transactional
    public void notifyTaskUpdated(Task task, User sender, Collection<User> recipients) {
        if (task == null) {
            return;
        }

        createForRecipients(
                recipients,
                sender,
                NotificationType.TASK_UPDATED,
                NotificationSeverity.NORMAL,
                "팀 업무가 수정되었습니다.",
                task.getName(),
                senderName(sender) + "님이 업무 정보를 수정했습니다.",
                "업무 보드로 이동",
                "/team-board"
        );
    }

    @Transactional
    public void notifyReviewRequested(AdReviewRequest request, User requester, Collection<User> reviewers) {
        if (request == null || request.getCampaign() == null) {
            return;
        }

        createForRecipients(
                reviewers,
                requester,
                NotificationType.REVIEW_REQUESTED,
                NotificationSeverity.HIGH,
                "검수 요청이 도착했습니다.",
                request.getFileName(),
                "새로운 검수 요청이 생성되었습니다. 요청 자료와 메모를 확인해 주세요.",
                "검수 요청 보기",
                "/campaigns/" + request.getCampaign().getIdx()
        );
    }

    @Transactional
    public void notifyReviewDecision(AdReviewRequest request, User reviewer, User requester, boolean approved) {
        if (request == null || requester == null || request.getCampaign() == null) {
            return;
        }

        create(
                requester,
                reviewer,
                approved ? NotificationType.REVIEW_APPROVED : NotificationType.REVIEW_REJECTED,
                approved ? NotificationSeverity.NORMAL : NotificationSeverity.HIGH,
                approved ? "검수 요청이 승인되었습니다." : "검수 요청이 반려되었습니다.",
                request.getFileName(),
                approved ? "검수 요청이 승인되었습니다." : "검수 요청이 반려되었습니다. 반려 사유를 확인해 주세요.",
                "검수 결과 보기",
                "/campaigns/" + request.getCampaign().getIdx()
        );
    }

    @Transactional
    public void notifyAiJudgeResult(Long requesterIdx, AdCheckDto.FileCheckRes response) {
        if (requesterIdx == null || response == null) {
            return;
        }

        User requester = userRepository.findById(requesterIdx).orElse(null);
        if (requester == null) {
            return;
        }

        NotificationType type = resolveAiJudgeNotificationType(response);
        NotificationSeverity severity = type == NotificationType.AI_JUDGE_COMPLETED
                ? NotificationSeverity.NORMAL
                : NotificationSeverity.HIGH;

        create(
                requester,
                null,
                type,
                severity,
                aiJudgeTitle(type),
                nonBlank(response.getFileName(), "업로드 파일"),
                aiJudgeDetail(response, type),
                "검수 결과 보기",
                aiJudgeTargetUrl(response.getAnalysisJobId()),
                aiJudgeDedupeKey(response.getAnalysisJobId(), requesterIdx),
                "AI_JUDGE_ANALYSIS",
                null,
                type.name(),
                false
        );
    }

    @Transactional
    public void notifyAiJudgeJobFailure(
            Long requesterIdx,
            String jobId,
            String fileName,
            String errorMessage,
            String targetUrl
    ) {
        if (requesterIdx == null) {
            return;
        }

        User requester = userRepository.findById(requesterIdx).orElse(null);
        if (requester == null) {
            return;
        }

        create(
                requester,
                null,
                NotificationType.AI_JUDGE_FAILED,
                NotificationSeverity.HIGH,
                "AI 검수 처리에 실패했습니다",
                nonBlank(fileName, "업로드 파일"),
                nonBlank(errorMessage, "AI 검수 처리 중 오류가 발생했습니다."),
                "검수 결과 보기",
                nonBlank(targetUrl, "/references"),
                aiJudgeDedupeKey(nonBlank(jobId, null), requesterIdx),
                "AI_JUDGE_ANALYSIS",
                null,
                NotificationType.AI_JUDGE_FAILED.name(),
                false
        );
    }

    @Transactional
    public void notifyDeadline(Task task, Collection<User> recipients, NotificationType type, String dedupePrefix) {
        if (task == null || type == null) {
            return;
        }

        NotificationSeverity severity = type == NotificationType.DEADLINE_OVERDUE
                ? NotificationSeverity.CRITICAL
                : NotificationSeverity.HIGH;
        String title = switch (type) {
            case DEADLINE_24H -> "업무 마감이 24시간 남았습니다.";
            case DEADLINE_1H -> "업무 마감이 1시간 남았습니다.";
            case DEADLINE_OVERDUE -> "업무 마감이 초과되었습니다.";
            default -> "업무 마감 알림";
        };

        createForRecipients(
                recipients,
                null,
                type,
                severity,
                title,
                task.getName(),
                "업무 마감일과 현재 진행 상태를 확인해 주세요.",
                "업무 보드로 이동",
                "/team-board",
                recipient -> dedupePrefix + ":user:" + recipient.getIdx()
        );
    }

    @Transactional
    public void notifyCampaignInvitation(CampaignInvitation invitation) {
        if (invitation == null || invitation.getInvitee() == null || invitation.getCampaign() == null) {
            return;
        }

        String campaignName = nonBlank(invitation.getCampaign().getName(), "캠페인");
        create(
                invitation.getInvitee(),
                invitation.getInviter(),
                NotificationType.CAMPAIGN_INVITED,
                NotificationSeverity.HIGH,
                "협력사 초대가 도착했습니다",
                senderName(invitation.getInviter()) + "님이 [" + campaignName + "] 협력사 GM으로 초대했습니다. 승인하거나 반려해 주세요.",
                null,
                "초대 확인하기",
                "/campaigns/" + invitation.getCampaign().getPublicId(),
                "campaign-invitation:" + invitation.getIdx() + ":invitee:" + invitation.getInvitee().getIdx(),
                REFERENCE_CAMPAIGN_INVITATION,
                invitation.getIdx(),
                invitation.getStatus().name(),
                true
        );
    }

    @Transactional
    public void notifyCampaignGroupInvitation(
            org.example.backend.campaign.model.CampaignInvitation invitation,
            int eligibleCount
    ) {
        if (invitation == null || invitation.getInvitee() == null || invitation.getCampaign() == null) {
            return;
        }
        String campaignName = nonBlank(invitation.getCampaign().getName(), "캠페인");
        String orgName = invitation.getInviteeOrganization() != null
                ? invitation.getInviteeOrganization().getName()
                : "협력사";
        create(
                invitation.getInvitee(),
                invitation.getInviter(),
                NotificationType.CAMPAIGN_INVITED,
                NotificationSeverity.HIGH,
                "협력사 그룹 초대가 도착했습니다",
                senderName(invitation.getInviter()) + "님이 [" + campaignName + "]에 "
                        + orgName + "(" + eligibleCount + "명) 그룹 초대를 보냈습니다. 수락 시 우리 조직 인원이 함께 합류합니다.",
                null,
                "초대 확인하기",
                "/campaigns/" + invitation.getCampaign().getPublicId(),
                "campaign-invitation:" + invitation.getIdx() + ":invitee:" + invitation.getInvitee().getIdx(),
                REFERENCE_CAMPAIGN_INVITATION,
                invitation.getIdx(),
                invitation.getStatus().name(),
                true
        );
    }

    @Transactional
    public void notifyCampaignInvitationDecision(CampaignInvitation invitation) {
        if (invitation == null || invitation.getInviter() == null || invitation.getCampaign() == null) {
            return;
        }

        boolean accepted = invitation.getStatus() == CampaignInvitationStatus.ACCEPTED;
        create(
                invitation.getInviter(),
                invitation.getInvitee(),
                accepted ? NotificationType.CAMPAIGN_INVITATION_ACCEPTED : NotificationType.CAMPAIGN_INVITATION_REJECTED,
                accepted ? NotificationSeverity.NORMAL : NotificationSeverity.HIGH,
                accepted ? "캠페인 초대가 승인되었습니다." : "캠페인 초대가 반려되었습니다.",
                invitation.getCampaign().getName(),
                senderName(invitation.getInvitee()) + "님이 캠페인 초대를 "
                        + (accepted ? "승인했습니다." : "반려했습니다."),
                "캠페인 보기",
                "/campaigns/" + invitation.getCampaign().getPublicId(),
                "campaign-invitation:" + invitation.getIdx() + ":inviter:" + invitation.getInviter().getIdx(),
                REFERENCE_CAMPAIGN_INVITATION,
                invitation.getIdx(),
                invitation.getStatus().name(),
                true
        );

        updateReferenceStatus(REFERENCE_CAMPAIGN_INVITATION, invitation.getIdx(), invitation.getStatus().name());
    }

    @Transactional
    public void notifyCampaignMemberAdded(User recipient, User sender, String campaignName, String targetUrl) {
        if (recipient == null) {
            return;
        }

        create(
                recipient,
                sender,
                NotificationType.CAMPAIGN_MEMBER_ADDED,
                NotificationSeverity.NORMAL,
                "캠페인 구성원으로 추가되었습니다",
                senderName(sender) + "님이 [" + nonBlank(campaignName, "캠페인") + "]에 당신을 구성원으로 추가했습니다.",
                null,
                "캠페인 열기",
                targetUrl
        );
    }

    @Transactional
    public void updateReferenceStatus(String referenceType, Long referenceId, String referenceStatus) {
        if (referenceType == null || referenceId == null) {
            return;
        }

        notificationRepository.findAllByReferenceTypeAndReferenceId(referenceType, referenceId)
                .forEach(notification -> notification.updateReferenceStatus(referenceStatus));
    }

    private NotificationType resolveAiJudgeNotificationType(AdCheckDto.FileCheckRes response) {
        if (response.getErrorMessage() != null && !response.getErrorMessage().isBlank()) {
            return NotificationType.AI_JUDGE_FAILED;
        }

        return "pass".equalsIgnoreCase(normalize(response.getStatus()))
                ? NotificationType.AI_JUDGE_COMPLETED
                : NotificationType.AI_JUDGE_REVIEW_REQUIRED;
    }

    private String aiJudgeTitle(NotificationType type) {
        return switch (type) {
            case AI_JUDGE_COMPLETED -> "AI 검수가 완료되었습니다";
            case AI_JUDGE_REVIEW_REQUIRED -> "AI 검수 결과 확인이 필요합니다";
            case AI_JUDGE_FAILED -> "AI 검수 처리에 실패했습니다";
            default -> "AI 검수 알림";
        };
    }

    private String aiJudgeDetail(AdCheckDto.FileCheckRes response, NotificationType type) {
        if (type == NotificationType.AI_JUDGE_FAILED) {
            return nonBlank(response.getErrorMessage(), "AI 검수 처리 중 오류가 발생했습니다.");
        }

        StringBuilder detail = new StringBuilder();
        appendDetailLine(detail, "판정", response.getStatus());
        appendDetailLine(detail, "관련 법령", response.getLaw());
        appendDetailLine(detail, "문제 문구", response.getViolationText());
        appendDetailLine(detail, "사유", response.getReason());
        appendDetailLine(detail, "수정 제안", response.getSuggestion());

        if (detail.isEmpty()) {
            return type == NotificationType.AI_JUDGE_COMPLETED
                    ? "AI 검수가 정상 완료되었습니다."
                    : "AI 검수 결과를 확인해 주세요.";
        }

        return detail.toString();
    }

    private void appendDetailLine(StringBuilder builder, String label, String value) {
        String nextValue = normalize(value);
        if (nextValue == null) {
            return;
        }

        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(nextValue);
    }

    private String aiJudgeTargetUrl(String analysisJobId) {
        String normalizedId = normalize(analysisJobId);
        if (normalizedId == null) {
            return "/references";
        }

        return "/references?analysisJobId=" + URLEncoder.encode(normalizedId, StandardCharsets.UTF_8);
    }

    private String aiJudgeDedupeKey(String analysisJobId, Long requesterIdx) {
        String normalizedId = normalize(analysisJobId);
        if (normalizedId == null || requesterIdx == null) {
            return null;
        }

        return "ai-judge:" + normalizedId + ":recipient:" + requesterIdx;
    }

    private List<User> uniqueUsers(Collection<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        Map<Long, User> unique = new LinkedHashMap<>();
        users.stream()
                .filter(user -> user != null && user.getIdx() != null)
                .forEach(user -> unique.putIfAbsent(user.getIdx(), user));
        return List.copyOf(unique.values());
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String senderName(User sender) {
        if (sender == null || sender.getName() == null || sender.getName().isBlank()) {
            return "시스템";
        }

        return sender.getName();
    }
}
