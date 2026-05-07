package org.example.backend.adcheck.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.model.AdReviewRequest;
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
    private final AdCheckFileStorageService adCheckFileStorageService;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignParticipantRepository campaignParticipantRepository;
    private final UserRepository userRepository;

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
        requirePartnerOrganization(campaignId, requester);

        String aiStatus = normalize(req.status());
        aiStatus = aiStatus == null ? null : aiStatus.toLowerCase();
        if (!AI_STATUS_PASS.equals(aiStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI check must pass before creating review request.");
        }

        String fileObjectKey = normalize(req.fileObjectKey());
        if (!adCheckFileStorageService.isAdCheckFileKey(fileObjectKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid ad check file object key.");
        }

        String fileName = normalize(req.fileName());
        if (fileName == null) {
            fileName = "upload";
        }

        Organization organization = requester.getOrganization();
        AdReviewRequest saved = adReviewRequestRepository.save(AdReviewRequest.builder()
                .campaign(campaign)
                .fileName(fileName)
                .fileObjectKey(fileObjectKey)
                .fileContentType(normalize(req.fileContentType()))
                .fileSize(req.fileSize())
                .extractedText(req.extractedText())
                .requestStatus(REQUEST_STATUS_REQUESTED)
                .aiStatus(aiStatus)
                .law(normalize(req.law()))
                .violationText(normalize(req.violationText()))
                .reason(normalize(req.reason()))
                .suggestion(normalize(req.suggestion()))
                .requestMemo(normalize(req.requestMemo()))
                .requesterLoginId(requester.getId())
                .requesterName(requester.getName())
                .requesterOrganizationIdx(organization != null ? organization.getIdx() : null)
                .requesterOrganizationName(organization != null ? organization.getName() : null)
                .build());

        return toResponse(saved);
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
