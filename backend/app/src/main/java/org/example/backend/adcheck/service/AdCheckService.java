package org.example.backend.adcheck.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.client.AiJudgeClient;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdCheckService {

    private static final String CONTEXT_AD_CHECK_JOB_ID = "adCheckJobId";

    private final AiJudgeClient aiJudgeClient;
    private final NotificationService notificationService;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;

    public AdCheckDto.Res check(String copy) {
        return checkWithAiJudge(copy);
    }

    public AdCheckDto.Res checkWithAiJudge(String copy) {
        return aiJudgeClient.check(copy);
    }

    public AdCheckDto.FileCheckRes checkFile(MultipartFile file) {
        return checkFileWithAiJudge(file);
    }

    public AdCheckDto.FileCheckRes checkFileWithAiJudge(MultipartFile file) {
        return checkFileWithAiJudge(file, null, null);
    }

    public AdCheckDto.FileCheckRes checkFileWithAiJudge(
            MultipartFile file,
            AuthUserDetails requester,
            String campaignId
    ) {
        return checkFileWithAiJudge(file, requester, campaignId, Map.of());
    }

    public AdCheckDto.FileCheckRes checkFileWithAiJudge(
            MultipartFile file,
            AuthUserDetails requester,
            String campaignId,
            Map<String, Object> extraContext
    ) {
        if (campaignId != null && !campaignId.isBlank()) {
            requireCampaignAccess(campaignId, requester);
        }

        Map<String, Object> context = aiJudgeContext(requester, campaignId);
        if (extraContext != null && !extraContext.isEmpty()) {
            context.putAll(extraContext);
        }

        boolean shouldNotify = !isAsyncJobContext(context);
        try {
            AdCheckDto.FileCheckRes response = aiJudgeClient.checkFile(file, context);
            if (shouldNotify) {
                notifyAiJudgeResult(requester, response);
            }
            return response;
        } catch (AiJudgeClient.FileCheckRemoteException e) {
            if (shouldNotify) {
                notifyAiJudgeResult(requester, e.getResponse());
            }
            throw new FileCheckException(e.getMessage(), e.getResponse(), e);
        }
    }

    private boolean isAsyncJobContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return false;
        }
        Object jobId = context.get(CONTEXT_AD_CHECK_JOB_ID);
        return jobId != null && !String.valueOf(jobId).isBlank();
    }

    private void notifyAiJudgeResult(AuthUserDetails requester, AdCheckDto.FileCheckRes response) {
        if (requester == null || requester.getIdx() == null || response == null) {
            return;
        }

        notificationService.notifyAiJudgeResult(requester.getIdx(), response);
    }

    private Map<String, Object> aiJudgeContext(AuthUserDetails requester, String campaignId) {
        Map<String, Object> context = new HashMap<>();
        if (requester != null) {
            if (requester.getIdx() != null) {
                context.put("requesterUserIdx", requester.getIdx());
            }
            if (requester.getId() != null && !requester.getId().isBlank()) {
                context.put("requesterLoginId", requester.getId());
            }
            if (requester.getName() != null && !requester.getName().isBlank()) {
                context.put("requesterName", requester.getName());
            }
        }
        if (campaignId != null && !campaignId.isBlank()) {
            context.put("campaignId", campaignId.trim());
        }
        return context;
    }

    private Campaign requireCampaignAccess(String campaignId, AuthUserDetails requester) {
        if (requester == null || requester.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 사용자 정보가 필요합니다.");
        }

        Campaign campaign = resolveCampaign(campaignId);
        CampaignMemberGuard.requireMember(campaignMemberRepository
                .findByCampaignIdxAndUserIdx(campaign.getIdx(), requester.getIdx())
                .orElse(null));
        return campaign;
    }

    private Campaign resolveCampaign(String campaignId) {
        String normalized = campaignId == null ? null : campaignId.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "campaignId is required.");
        }

        Optional<Campaign> byPublicId = campaignRepository.findByPublicId(normalized);
        if (byPublicId.isPresent()) {
            return byPublicId.get();
        }

        try {
            return campaignRepository.findById(Long.parseLong(normalized))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found.");
        }
    }

    public static class FileCheckException extends RuntimeException {
        private final AdCheckDto.FileCheckRes response;

        public FileCheckException(String message, AdCheckDto.FileCheckRes response, Throwable cause) {
            super(message, cause);
            this.response = response;
        }

        public AdCheckDto.FileCheckRes getResponse() {
            return response;
        }
    }
}
