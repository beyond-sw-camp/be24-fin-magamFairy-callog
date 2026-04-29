package org.example.backend.campaign.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class CampaignDto {
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public record UpsertReq(
            String name,
            String purpose,
            List<String> tags,
            LocalDate startDate,
            LocalDate endDate,
            List<String> partners,
            String goals,
            String mainMessage,
            String color,
            CampaignCategory category,
            BigDecimal budget,
            String contactPerson,
            String contactEmail,
            String contactPhone,
            RiskLevel riskLevel,
            String internalNotes
    ) {
    }

    public record PartnerInviteReq(
            List<String> partners,
            CampaignRole role
    ) {
    }

    public record StatusReq(String status) {
    }

    public record CloneReq(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record InvitationDecisionReq(String message) {
    }

    public record RoleUpdateReq(CampaignRole role) {
    }

    public record ApprovalRequestReq(
            String approverLoginId,
            String comment
    ) {
    }

    public record ApprovalDecisionReq(String comment) {
    }

    public record CommentReq(
            String content,
            CommentType commentType,
            String targetField,
            Long parentCommentId
    ) {
    }

    public record ContentReq(
            Long contentId,
            ContentType contentType,
            Integer sequence,
            String remarks
    ) {
    }

    public record AnalyticsReq(
            Long impressions,
            Long clicks,
            Long conversions,
            BigDecimal totalSpent,
            Integer participantCount,
            Integer completionCount
    ) {
    }

    @Builder
    public record Res(
            String id,
            Long idx,
            String name,
            String purpose,
            List<String> tags,
            LocalDate startDate,
            LocalDate endDate,
            String period,
            List<String> partners,
            String goals,
            String mainMessage,
            String status,
            String initials,
            String color,
            CampaignCategory category,
            BigDecimal budget,
            String contactPerson,
            String contactEmail,
            String contactPhone,
            ApprovalStatus approvalStatus,
            String approverId,
            LocalDate approvalDate,
            String rejectionReason,
            RiskLevel riskLevel,
            String internalNotes,
            String templateSourceId,
            Date createdAt,
            Date updatedAt
    ) {
        public static Res from(Campaign entity) {
            return Res.builder()
                    .id(String.valueOf(entity.getIdx()))
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .purpose(entity.getPurpose())
                    .tags(List.copyOf(entity.getTags()))
                    .startDate(entity.getStartDate())
                    .endDate(entity.getEndDate())
                    .period(formatPeriod(entity.getStartDate(), entity.getEndDate()))
                    .partners(List.copyOf(entity.getPartners()))
                    .goals(entity.getGoals())
                    .mainMessage(entity.getMainMessage())
                    .status(entity.getStatus())
                    .initials(entity.getInitials())
                    .color(entity.getColor())
                    .category(entity.getCategory())
                    .budget(entity.getBudget())
                    .contactPerson(entity.getContactPerson())
                    .contactEmail(entity.getContactEmail())
                    .contactPhone(entity.getContactPhone())
                    .approvalStatus(entity.getApprovalStatus())
                    .approverId(entity.getApproverId())
                    .approvalDate(entity.getApprovalDate())
                    .rejectionReason(entity.getRejectionReason())
                    .riskLevel(entity.getRiskLevel())
                    .internalNotes(entity.getInternalNotes())
                    .templateSourceId(entity.getTemplateSourceId())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    public record InvitationListRes(List<InvitationRes> invitations) {
    }

    @Builder
    public record InvitationRes(
            Long idx,
            Long campaignIdx,
            String invitee,
            String invitedBy,
            CampaignRole role,
            InvitationStatus status,
            LocalDate expiryDate,
            LocalDate respondedDate,
            String respondedBy,
            String responseMessage,
            String invitationToken,
            Date createdAt,
            Date updatedAt
    ) {
        public static InvitationRes from(CampaignInvitation entity) {
            return InvitationRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .invitee(entity.getInvitee())
                    .invitedBy(entity.getInvitedBy())
                    .role(entity.getRole())
                    .status(entity.getStatus())
                    .expiryDate(entity.getExpiryDate())
                    .respondedDate(entity.getRespondedDate())
                    .respondedBy(entity.getRespondedBy())
                    .responseMessage(entity.getResponseMessage())
                    .invitationToken(entity.getInvitationToken())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    @Builder
    public record ParticipantRes(
            Long idx,
            Long campaignIdx,
            Long organizationIdx,
            String userLoginId,
            CampaignRole role,
            ParticipantStatus status,
            LocalDate joinedDate,
            LocalDate leftDate,
            String remarks
    ) {
        public static ParticipantRes from(CampaignParticipant entity) {
            Long organizationIdx = entity.getOrganization() == null ? null : entity.getOrganization().getIdx();
            return ParticipantRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .organizationIdx(organizationIdx)
                    .userLoginId(entity.getUserLoginId())
                    .role(entity.getCampaignRole())
                    .status(entity.getParticipantStatus())
                    .joinedDate(entity.getJoinedDate())
                    .leftDate(entity.getLeftDate())
                    .remarks(entity.getRemarks())
                    .build();
        }
    }

    @Builder
    public record ApprovalRes(
            Long idx,
            Long campaignIdx,
            String requestedBy,
            LocalDate requestedDate,
            String approverLoginId,
            ApprovalStatus status,
            LocalDate approvedDate,
            String approverComment,
            String rejectionReason,
            Integer sequence
    ) {
        public static ApprovalRes from(CampaignApprovalFlow entity) {
            return ApprovalRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .requestedBy(entity.getRequestedBy())
                    .requestedDate(entity.getRequestedDate())
                    .approverLoginId(entity.getApproverLoginId())
                    .status(entity.getStatus())
                    .approvedDate(entity.getApprovedDate())
                    .approverComment(entity.getApproverComment())
                    .rejectionReason(entity.getRejectionReason())
                    .sequence(entity.getSequence())
                    .build();
        }
    }

    @Builder
    public record CommentRes(
            Long idx,
            Long campaignIdx,
            String authorLoginId,
            String content,
            CommentType commentType,
            String targetField,
            Long parentCommentIdx,
            Boolean resolved,
            String resolvedBy,
            Date createdAt,
            Date updatedAt
    ) {
        public static CommentRes from(CampaignComment entity) {
            Long parentCommentIdx = entity.getParentComment() == null ? null : entity.getParentComment().getIdx();
            return CommentRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .authorLoginId(entity.getAuthorLoginId())
                    .content(entity.getContent())
                    .commentType(entity.getCommentType())
                    .targetField(entity.getTargetField())
                    .parentCommentIdx(parentCommentIdx)
                    .resolved(entity.getResolved())
                    .resolvedBy(entity.getResolvedBy())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    @Builder
    public record VersionRes(
            Long idx,
            Long campaignIdx,
            Integer versionNumber,
            String changedBy,
            LocalDate changedDate,
            String changeDescription,
            String campaignSnapshot,
            String changeLog
    ) {
        public static VersionRes from(CampaignVersion entity) {
            return VersionRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .versionNumber(entity.getVersionNumber())
                    .changedBy(entity.getChangedBy())
                    .changedDate(entity.getChangedDate())
                    .changeDescription(entity.getChangeDescription())
                    .campaignSnapshot(entity.getCampaignSnapshot())
                    .changeLog(entity.getChangeLog())
                    .build();
        }
    }

    @Builder
    public record ContentRes(
            Long idx,
            Long campaignIdx,
            Long contentId,
            ContentType contentType,
            Integer sequence,
            String remarks
    ) {
        public static ContentRes from(CampaignContent entity) {
            return ContentRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .contentId(entity.getContentId())
                    .contentType(entity.getContentType())
                    .sequence(entity.getSequence())
                    .remarks(entity.getRemarks())
                    .build();
        }
    }

    @Builder
    public record AnalyticsRes(
            Long idx,
            Long campaignIdx,
            Long impressions,
            Long clicks,
            Long conversions,
            BigDecimal clickThroughRate,
            BigDecimal conversionRate,
            BigDecimal totalSpent,
            BigDecimal costPerClick,
            BigDecimal returnOnAdSpend,
            Integer participantCount,
            Integer completionCount
    ) {
        public static AnalyticsRes from(CampaignAnalytics entity) {
            return AnalyticsRes.builder()
                    .idx(entity.getIdx())
                    .campaignIdx(entity.getCampaign().getIdx())
                    .impressions(entity.getImpressions())
                    .clicks(entity.getClicks())
                    .conversions(entity.getConversions())
                    .clickThroughRate(entity.getClickThroughRate())
                    .conversionRate(entity.getConversionRate())
                    .totalSpent(entity.getTotalSpent())
                    .costPerClick(entity.getCostPerClick())
                    .returnOnAdSpend(entity.getReturnOnAdSpend())
                    .participantCount(entity.getParticipantCount())
                    .completionCount(entity.getCompletionCount())
                    .build();
        }
    }

    private static String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "";
        }

        String start = startDate == null ? "" : startDate.format(PERIOD_FORMATTER);
        String end = endDate == null ? "" : endDate.format(PERIOD_FORMATTER);
        return start + " - " + end;
    }
}
