package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.matching.model.*;
import org.example.backend.matching.repository.AssetRepository;
import org.example.backend.matching.repository.BenefitRepository;
import org.example.backend.matching.repository.EvaluationRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;


@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final CampaignParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final BenefitRepository benefitRepository;
    private final RestClient restClient;

    @Value("${custom.n8n.webhook-url}")
    String n8nWebhookUrl;

    public void newEvaluation(EvaluationDto.NewEvaluationReq dto) throws Exception {

        MarketingAsset requiredAsset = assetRepository.findById(dto.getAssetIdx())
                .orElseThrow(() -> new EntityNotFoundException("해당 Asset을 찾을 수 없습니다. Asset ID: " + dto.getAssetIdx()));
        PartnerBenefits requiredBenefit = benefitRepository.findById(dto.getBenefitIdx())
                .orElseThrow(() -> new EntityNotFoundException("해당 Benefit을 찾을 수 없습니다. Benefit ID: " + dto.getBenefitIdx()));

        EvaluationDto.NewEvaluation eval;
        eval = EvaluationDto.NewEvaluation.builder()
                .kpi(dto.getKpi())
                .dependency(dto.getDependency())
                .asset(EvaluationDto.NewEvaluation.AssetRes.toDto(requiredAsset))
                .benefit(EvaluationDto.NewEvaluation.BenefitRes.toDto(requiredBenefit))
                .build();

        restClient.post()
                .uri(n8nWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "*/*") // 추가 1: 다 받을게
                .header(HttpHeaders.USER_AGENT, "PostmanRuntime/7.36.1") // 추가 2: 포스트맨인 척하기
                .header(HttpHeaders.CONNECTION, "keep-alive") // 추가 3: 연결 유
                .body(eval)
                .retrieve()
                .body(String.class);

    }

    public void collect(EvaluationDto.Collect dto, AuthUserDetails user) {

    }

    @Transactional(readOnly = true)
    public EvaluationDto.EvaluationRes getEvaluationRes(Long userIdx, Long campaignIdx) {
        // 1. 유저의 단체 정보 가져오기
        Organization organization = userRepository.findById(userIdx)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."))
                .getOrganization();

        Long organizationIdx = organization.getIdx();

        // 2. 매핑 테이블(CampaignParticipant)에서 존재 여부 확인
        boolean isParticipant = participantRepository.existsByCampaignIdxAndOrganizationIdx(
                campaignIdx,
                organizationIdx
        );

        if (!isParticipant) {
            // Spring Security를 쓰신다면 아래 예외, 아니면 RuntimeException 사용
            throw new org.springframework.security.access.AccessDeniedException("해당 캠페인 참여 권한이 없습니다.");
        }
        else {
            // 3. 평가 데이터 조회 및 반환
            Evaluation evaluation = evaluationRepository.findByCampaignIdxAndOrganizationIdx(campaignIdx, organizationIdx)
                    .orElseThrow(() -> new EntityNotFoundException("평가 데이터를 찾을 수 없습니다."));

            return EvaluationDto.EvaluationRes.toDto(evaluation);
        }
    }


}
