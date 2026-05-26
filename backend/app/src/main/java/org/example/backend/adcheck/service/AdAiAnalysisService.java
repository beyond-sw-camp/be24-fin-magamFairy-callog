package org.example.backend.adcheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.client.AiJudgeClient;
import org.example.backend.adcheck.model.AdAiAnalysis;
import org.example.backend.adcheck.model.AdAiAnalysisDto;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.repository.AdAiAnalysisRepository;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.common.security.RoleGuard;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdAiAnalysisService {
    private static final String EVENT_COMPLETED = "AI_JUDGE_COMPLETED";
    private static final String EVENT_FAILED = "AI_JUDGE_FAILED";

    private final AdAiAnalysisRepository adAiAnalysisRepository;
    private final AiJudgeClient aiJudgeClient;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final UserRepository userRepository;

    public AdCheckDto.FileCheckRes checkFile(
            String campaignPublicId,
            MultipartFile file,
            AuthUserDetails authUser
    ) {
        User author = findUser(authUser);
        Campaign campaign = findCampaign(campaignPublicId);
        requireCampaignMember(campaign, author);

        String analysisJobId = UUID.randomUUID().toString();
        AdAiAnalysis analysis = adAiAnalysisRepository.save(AdAiAnalysis.builder()
                .analysisJobId(analysisJobId)
                .campaign(campaign)
                .authorIdx(author.getIdx())
                .authorLoginId(author.getId())
                .authorName(author.getName())
                .fileName(file == null ? null : normalize(file.getOriginalFilename()))
                .fileContentType(file == null ? null : normalize(file.getContentType()))
                .fileSize(file == null ? null : file.getSize())
                .analysisStatus(AdAiAnalysis.STATUS_PENDING)
                .build());

        try {
            AdCheckDto.FileCheckRes response = aiJudgeClient.checkFile(file, analysisJobId);
            analysis.acceptResponse(response);
            adAiAnalysisRepository.save(analysis);
            return response;
        } catch (RuntimeException e) {
            analysis.failRequest(e.getMessage());
            adAiAnalysisRepository.save(analysis);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<AdAiAnalysisDto.SummaryRes> list(String campaignPublicId, AuthUserDetails authUser) {
        User caller = findUser(authUser);
        Campaign campaign = findCampaign(campaignPublicId);
        requireCampaignMember(campaign, caller);
        return adAiAnalysisRepository.findAllByCampaignIdxOrderByIdxDesc(campaign.getIdx())
                .stream()
                .map(AdAiAnalysisDto.SummaryRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdAiAnalysisDto.DetailRes detail(
            String campaignPublicId,
            Long analysisId,
            AuthUserDetails authUser
    ) {
        User caller = findUser(authUser);
        Campaign campaign = findCampaign(campaignPublicId);
        requireCampaignMember(campaign, caller);
        AdAiAnalysis analysis = adAiAnalysisRepository.findByIdxAndCampaignIdx(analysisId, campaign.getIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AI analysis not found."));

        try {
            return AdAiAnalysisDto.DetailRes.available(
                    analysis,
                    aiJudgeClient.getAnalysisDetail(analysis.getAnalysisJobId())
            );
        } catch (AiJudgeClient.DetailUnavailableException e) {
            log.warn("AI judge analysis detail is unavailable. analysisJobId={}", analysis.getAnalysisJobId(), e);
            return AdAiAnalysisDto.DetailRes.unavailable(
                    analysis,
                    "AI 검수 상세 결과를 일시적으로 불러올 수 없습니다."
            );
        }
    }

    @Transactional
    public void applyAiJudgeEvent(JsonNode event) {
        String analysisJobId = text(event, "analysisJobId");
        if (analysisJobId == null) {
            log.warn("Skipped ai-judge event without analysisJobId.");
            return;
        }

        AdAiAnalysis analysis = adAiAnalysisRepository.findByAnalysisJobId(analysisJobId).orElse(null);
        if (analysis == null) {
            log.warn("Skipped ai-judge event because metadata is missing. analysisJobId={}", analysisJobId);
            return;
        }

        String eventType = text(event, "eventType");
        if (EVENT_COMPLETED.equals(eventType)) {
            analysis.completeFromEvent(
                    text(event, "fileName"),
                    text(event, "fileObjectKey"),
                    text(event, "fileContentType"),
                    longValue(event, "fileSize"),
                    text(event, "aiStatus")
            );
            return;
        }

        if (EVENT_FAILED.equals(eventType)) {
            analysis.failFromEvent(
                    text(event, "fileName"),
                    text(event, "fileObjectKey"),
                    text(event, "fileContentType"),
                    longValue(event, "fileSize"),
                    text(event, "aiStatus"),
                    text(event, "errorMessage")
            );
            return;
        }

        log.warn("Skipped unsupported ai-judge event. analysisJobId={}, eventType={}", analysisJobId, eventType);
    }

    private Campaign findCampaign(String campaignPublicId) {
        String publicId = normalize(campaignPublicId);
        if (publicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "campaign id is required.");
        }
        return campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
    }

    private User findUser(AuthUserDetails authUser) {
        RoleGuard.requireAuthenticated(authUser);
        return userRepository.findById(authUser.getIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found."));
    }

    private void requireCampaignMember(Campaign campaign, User user) {
        CampaignMemberGuard.requireMember(
                campaignMemberRepository.findByCampaignIdxAndUserIdx(campaign.getIdx(), user.getIdx()).orElse(null)
        );
    }

    private String text(JsonNode node, String name) {
        if (node == null || node.path(name).isMissingNode() || node.path(name).isNull()) {
            return null;
        }
        return normalize(node.path(name).asText(null));
    }

    private Long longValue(JsonNode node, String name) {
        if (node == null || node.path(name).isMissingNode() || node.path(name).isNull()) {
            return null;
        }
        return node.path(name).canConvertToLong() ? node.path(name).asLong() : null;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
