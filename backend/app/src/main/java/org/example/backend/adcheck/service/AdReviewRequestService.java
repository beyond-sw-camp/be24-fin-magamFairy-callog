package org.example.backend.adcheck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.backend.activity.service.CampaignActivityService;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.model.AdCheckJob;
import org.example.backend.adcheck.model.AdCheckJobStatus;
import org.example.backend.adcheck.model.AdReviewRequest;
import org.example.backend.adcheck.repository.AdCheckJobRepository;
import org.example.backend.adcheck.repository.AdReviewRequestRepository;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignRole;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdReviewRequestService {
    private static final String REQUEST_STATUS_REQUESTED = "REQUESTED";
    private static final String AI_STATUS_PASS = "pass";

    private final AdReviewRequestRepository adReviewRequestRepository;
    private final AdCheckJobRepository adCheckJobRepository;
    private final AdCheckFileStorageService adCheckFileStorageService;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CampaignActivityService activityService;
    private final ObjectMapper objectMapper;

    public List<AdCheckDto.ReviewRequestRes> list(Long campaignId, AuthUserDetails authUser) {
        User caller = findUser(authUser);
        CampaignMember member = requireCampaignMember(campaignId, caller);

        List<AdReviewRequest> requests;
        if (canFinalReview(campaignId, authUser, caller, member)) {
            requests = adReviewRequestRepository.findAllByCampaignIdxOrderByIdxDesc(campaignId);
        } else if (caller.getOrganization() != null) {
            requests = adReviewRequestRepository.findAllByCampaignIdxAndRequesterOrganizationIdxOrderByIdxDesc(
                    campaignId,
                    caller.getOrganization().getIdx()
            );
        } else {
            requests = adReviewRequestRepository.findAllByCampaignIdxAndRequesterLoginIdOrderByIdxDesc(
                    campaignId,
                    caller.getId()
            );
        }

        return requests.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdCheckDto.ReviewRequestRes create(
            Long campaignId,
            AdCheckDto.ReviewRequestCreateReq req,
            AuthUserDetails authUser
    ) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }

        User requester = findUser(authUser);
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
        requireCampaignMember(campaignId, requester);

        String adCheckJobId = normalize(req.jobId());
        AdCheckJob sourceJob = null;
        AdCheckDto.FileCheckRes sourceResult = null;
        if (adCheckJobId != null) {
            sourceJob = findReviewSourceJob(adCheckJobId, requester, campaign);
            sourceResult = parseJobResult(sourceJob);
            if (adReviewRequestRepository.existsByCampaignIdxAndAdCheckJobId(campaignId, adCheckJobId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "review request already exists for this AI check.");
            }
        } else {
            requirePartnerOrganization(campaignId, requester);
        }

        String aiStatus = normalize(firstText(req.status(), sourceResult == null ? null : sourceResult.getStatus()));
        aiStatus = aiStatus == null ? null : aiStatus.toLowerCase();
        if (adCheckJobId == null && !AI_STATUS_PASS.equals(aiStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI check must pass before creating review request.");
        }
        if (adCheckJobId != null && aiStatus == null) {
            aiStatus = "completed";
        }

        String fileObjectKey = normalize(firstText(
                req.fileObjectKey(),
                sourceResult == null ? null : sourceResult.getFileObjectKey()
        ));
        if (!adCheckFileStorageService.isAdCheckFileKey(fileObjectKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid ad check file object key.");
        }

        String fileName = normalize(firstText(
                req.fileName(),
                sourceResult == null ? null : sourceResult.getFileName(),
                sourceJob == null ? null : sourceJob.getFileName()
        ));
        if (fileName == null) {
            fileName = "upload";
        }
        String mongoDocumentId = normalize(firstText(
                req.mongoDocumentId(),
                sourceJob == null ? null : sourceJob.getMongoDocumentId(),
                sourceResult == null ? null : sourceResult.getAnalysisJobId()
        ));
        Integer verdictLevel = firstLevel(req.verdictLevel(), sourceResult == null ? null : sourceResult.getVerdictLevel());

        Organization organization = requester.getOrganization();
        AdReviewRequest saved = adReviewRequestRepository.save(AdReviewRequest.builder()
                .campaign(campaign)
                .fileName(fileName)
                .fileObjectKey(fileObjectKey)
                .fileContentType(normalize(firstText(
                        req.fileContentType(),
                        sourceResult == null ? null : sourceResult.getFileContentType(),
                        sourceJob == null ? null : sourceJob.getFileContentType()
                )))
                .fileSize(req.fileSize() != null
                        ? req.fileSize()
                        : sourceResult == null ? null : sourceResult.getFileSize())
                .extractedText(firstText(req.extractedText(), sourceResult == null ? null : sourceResult.getExtractedText()))
                .requestStatus(REQUEST_STATUS_REQUESTED)
                .aiStatus(aiStatus)
                .law(normalize(firstText(req.law(), sourceResult == null ? null : sourceResult.getLaw())))
                .violationText(normalize(firstText(
                        req.violationText(),
                        sourceResult == null ? null : sourceResult.getViolationText()
                )))
                .reason(normalize(firstText(req.reason(), sourceResult == null ? null : sourceResult.getReason())))
                .suggestion(normalize(firstText(
                        req.suggestion(),
                        sourceResult == null ? null : sourceResult.getSuggestion()
                )))
                .adCheckJobId(adCheckJobId)
                .mongoDocumentId(mongoDocumentId)
                .verdictLevel(verdictLevel)
                .requestMemo(normalize(req.requestMemo()))
                .requesterLoginId(requester.getId())
                .requesterName(requester.getName())
                .requesterOrganizationIdx(organization != null ? organization.getIdx() : null)
                .requesterOrganizationName(organization != null ? organization.getName() : null)
                .build());
        notificationService.notifyReviewRequested(saved, requester, findFinalReviewers(campaignId));
        activityService.record(campaign, requester, "REVIEW_SUBMIT", "검수 요청 제출: " + fileName);

        return toResponse(saved);
    }

    private AdCheckJob findReviewSourceJob(String jobId, User requester, Campaign campaign) {
        AdCheckJob job = adCheckJobRepository.findByJobIdAndRequester_Idx(jobId, requester.getIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI check job not found."));
        if (job.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI check job not found.");
        }
        if (job.getStatus() != AdCheckJobStatus.SUCCEEDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI check must be completed first.");
        }
        if (!belongsToCampaign(job, campaign)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI check job does not belong to this campaign.");
        }
        return job;
    }

    private boolean belongsToCampaign(AdCheckJob job, Campaign campaign) {
        String jobCampaignId = normalize(job.getCampaignId());
        if (jobCampaignId == null || campaign == null) {
            return false;
        }
        if (campaign.getPublicId() != null && jobCampaignId.equals(campaign.getPublicId())) {
            return true;
        }
        return campaign.getIdx() != null && jobCampaignId.equals(String.valueOf(campaign.getIdx()));
    }

    private AdCheckDto.FileCheckRes parseJobResult(AdCheckJob job) {
        if (job == null || job.getResultPayload() == null || job.getResultPayload().isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(job.getResultPayload(), AdCheckDto.FileCheckRes.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI check result payload is invalid.");
        }
    }

    @Transactional
    public AdCheckDto.ReviewRequestRes approve(
            Long campaignId,
            Long requestId,
            AdCheckDto.ReviewDecisionReq req,
            AuthUserDetails authUser
    ) {
        User reviewer = requireFinalReviewer(campaignId, authUser);
        AdReviewRequest request = findReviewRequest(campaignId, requestId);
        ensureRequested(request);

        request.approve(
                reviewer.getId(),
                reviewer.getName(),
                normalize(req != null ? req.memo() : null)
        );
        notificationService.notifyReviewDecision(request, reviewer, findRequester(request), true);
        activityService.record(request.getCampaign(), reviewer, "REVIEW_APPROVE", "검수 승인: " + request.getFileName());
        return toResponse(request);
    }

    @Transactional
    public AdCheckDto.ReviewRequestRes reject(
            Long campaignId,
            Long requestId,
            AdCheckDto.ReviewDecisionReq req,
            AuthUserDetails authUser
    ) {
        User reviewer = requireFinalReviewer(campaignId, authUser);
        AdReviewRequest request = findReviewRequest(campaignId, requestId);
        ensureRequested(request);

        String reason = normalize(req != null ? req.reason() : null);
        if (reason == null) {
            reason = normalize(req != null ? req.memo() : null);
        }
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reject reason is required.");
        }

        request.reject(reviewer.getId(), reviewer.getName(), reason);
        notificationService.notifyReviewDecision(request, reviewer, findRequester(request), false);
        activityService.record(request.getCampaign(), reviewer, "REVIEW_REJECT", "검수 반려: " + request.getFileName());
        return toResponse(request);
    }

    private AdCheckDto.ReviewRequestRes toResponse(AdReviewRequest request) {
        return AdCheckDto.ReviewRequestRes.from(
                request,
                adCheckFileStorageService.createViewUrl(request.getFileObjectKey(), request.getFileContentType())
        );
    }

    private AdReviewRequest findReviewRequest(Long campaignId, Long requestId) {
        AdReviewRequest request = adReviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "review request not found."));
        if (request.getCampaign() == null || !request.getCampaign().getIdx().equals(campaignId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "review request does not belong to this campaign.");
        }
        return request;
    }

    private void ensureRequested(AdReviewRequest request) {
        String status = normalize(request.getRequestStatus());
        if (!REQUEST_STATUS_REQUESTED.equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "only requested review can be approved or rejected. currentStatus=" + status
            );
        }
    }

    private User requireFinalReviewer(Long campaignId, AuthUserDetails authUser) {
        RoleGuard.requireManager(authUser);
        User reviewer = findUser(authUser);
        CampaignMember member = requireCampaignMember(campaignId, reviewer);
        if (!canFinalReview(campaignId, authUser, reviewer, member)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "campaign PM manager can review this request.");
        }
        return reviewer;
    }

    private CampaignMember requireCampaignMember(Long campaignId, User user) {
        CampaignMember member = campaignMemberRepository.findByCampaignIdxAndUserIdx(campaignId, user.getIdx())
                .orElse(null);
        return CampaignMemberGuard.requireMember(member);
    }

    private void requirePartnerOrganization(Long campaignId, User user) {
        Organization organization = user.getOrganization();
        if (organization == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "user organization is required.");
        }
        boolean isPartner = campaignParticipantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaignId,
                organization.getIdx(),
                CampaignRole.PARTNER
        );
        if (!isPartner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only campaign partner organization can request review.");
        }
    }

    private boolean canFinalReview(
            Long campaignId,
            AuthUserDetails authUser,
            User user,
            CampaignMember member
    ) {
        if (authUser == null
                || (!RoleGuard.ROLE_MANAGER.equals(authUser.getRole())
                && !RoleGuard.ROLE_GENERAL_MANAGER.equals(authUser.getRole()))) {
            return false;
        }
        if (member.getCampaignRole() != CampaignMemberRole.MANAGER
                && member.getCampaignRole() != CampaignMemberRole.GENERAL_MANAGER) {
            return false;
        }
        Organization organization = user.getOrganization();
        if (organization == null) {
            return false;
        }
        return campaignParticipantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                campaignId,
                organization.getIdx(),
                CampaignRole.PM
        );
    }

    private User findUser(AuthUserDetails authUser) {
        RoleGuard.requireAuthenticated(authUser);
        return userRepository.findById(authUser.getIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found."));
    }

    private List<User> findFinalReviewers(Long campaignId) {
        return campaignMemberRepository.findAllByCampaignIdx(campaignId)
                .stream()
                .filter(member -> member.getCampaignRole() == CampaignMemberRole.MANAGER
                        || member.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER)
                .map(CampaignMember::getUser)
                .filter(user -> user != null && user.getOrganization() != null)
                .filter(user -> campaignParticipantRepository.existsByCampaignIdxAndOrganizationIdxAndCampaignRole(
                        campaignId,
                        user.getOrganization().getIdx(),
                        CampaignRole.PM
                ))
                .toList();
    }

    private User findRequester(AdReviewRequest request) {
        String requesterLoginId = normalize(request.getRequesterLoginId());
        if (requesterLoginId == null) {
            return null;
        }

        return userRepository.findUserById(requesterLoginId).orElse(null);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private Integer firstLevel(Integer... levels) {
        if (levels == null) {
            return null;
        }
        for (Integer level : levels) {
            if (level != null && level >= 1 && level <= 5) {
                return level;
            }
        }
        return null;
    }
}
