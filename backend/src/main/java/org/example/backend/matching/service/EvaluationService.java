package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.matching.model.*;
import org.example.backend.matching.model.evaluation.CustomerEval;
import org.example.backend.matching.model.evaluation.Evaluation;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.example.backend.matching.repository.*;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final CampaignParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final BenefitRepository benefitRepository;
    private final GoalRepository goalRepository;
    private final CampaignRepository campaignRepository;
    private final RestClient restClient;

    @Value("${custom.n8n.webhook-url}/evaluation")
    String n8nWebhookUrl;

    public void startEvaluation(EvaluationDto.StartEvaluationReq dto) {

        Campaign campaign = campaignRepository.findById(dto.getCampaignIdx())
                .orElseThrow(() -> new EntityNotFoundException("해당 Campaign을 찾을 수 없습니다. Campaign ID: " + dto.getCampaignIdx()));
        MarketingAsset requiredAsset = assetRepository.findById(dto.getAssetIdx())
                .orElseThrow(() -> new EntityNotFoundException("해당 Asset을 찾을 수 없습니다. Asset ID: " + dto.getAssetIdx()));
        PartnerBenefits requiredBenefit = benefitRepository.findById(dto.getBenefitIdx())
                .orElseThrow(() -> new EntityNotFoundException("해당 Benefit을 찾을 수 없습니다. Benefit ID: " + dto.getBenefitIdx()));
        CampaignGoal requiredGoal = goalRepository.findById(dto.getGoalIdx())
                .orElseThrow(() -> new EntityNotFoundException("해당 Goal을 찾을 수 없습니다. Goal ID: " + dto.getGoalIdx()));

        EvaluationDto.StartEvaluation eval;
        eval = EvaluationDto.StartEvaluation.builder()
                .dependency(dto.getDependency())
                .campaignIdx(dto.getCampaignIdx())
                .asset(MatchingDto.AssetRes.toDto(requiredAsset))
                .benefit(MatchingDto.BenefitRes.toDto(requiredBenefit))
                .goal(EvaluationDto.StartEvaluation.CampaignGoalRes.toDto(requiredGoal))
                .build();

        try {
            restClient.post()
                    .uri(n8nWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eval)
                    .retrieve()
                    // 1. 응답을 받았지만 실패한 경우 (상태 코드 기반 세밀한 번역)
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new RuntimeException("n8n 엔드포인트를 찾을 수 없습니다.");
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new RuntimeException("n8n 서버 내부 처리 중 오류가 발생했습니다.");
                    })
                    .body(String.class);

        } catch (RestClientException e) {
            // 2. 서버가 꺼져있거나 타임아웃 등 아예 통신 자체가 실패한 경우 (또는 onStatus에서 잡지 못한 나머지 RestClient 예외)
            throw new RuntimeException("n8n 서버와 연결할 수 없습니다.", e);
        }
    }

    @Transactional
    public void collect(EvaluationDto.CollectDto dto) {

        String category = dto.getCategory();
        // 1. 현재 세션(Evaluation) 조회
        Evaluation evaluation = evaluationRepository.findBySessionId(dto.getUuid())
                .orElseGet(() -> evaluationRepository.save(new Evaluation(dto.getUuid())));

        // 2. DTO를 해당 카테고리의 엔티티로 변환
        Object evalEntity = switch (category) {
            case "CUSTOMER" -> ((EvaluationDto.CollectDto.Customer) dto).toEntity();
            case "REVENUE" -> ((EvaluationDto.CollectDto.Revenue) dto).toEntity();
            case "COST" -> ((EvaluationDto.CollectDto.Cost) dto).toEntity();
            case "OPERATION" -> ((EvaluationDto.CollectDto.Operation) dto).toEntity();
            case "BRAND" -> ((EvaluationDto.CollectDto.Brand) dto).toEntity();
            default -> throw new IllegalArgumentException("잘못된 카테고리입니다.");
        };

        // 3. 세션 엔티티에 데이터 연결 (Dirty Checking에 의해 자동 업데이트)
        evaluation.updateEval(evalEntity, category);
    }


}
