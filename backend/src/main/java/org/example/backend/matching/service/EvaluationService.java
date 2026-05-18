package org.example.backend.matching.service;

import com.mongodb.client.result.UpdateResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.matching.model.*;
import org.example.backend.matching.model.evaluation.EvaluationDocument;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.example.backend.matching.repository.*;
import org.example.backend.user.model.AuthUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final BenefitRepository benefitRepository;
    private final CampaignRepository campaignRepository;
    private final RestClient restClient;

    @Value("${custom.n8n.webhook-url}${custom.n8n.evaluation-endpoint}")
    String n8nWebhookUrl;

    public void startEvaluation(EvaluationDto.StartEvaluationReq dto) {

        PartnerBenefits requiredBenefit = benefitRepository.findById(dto.getBenefitIdx())
                .orElseThrow(() -> new EntityNotFoundException("해당 Benefit을 찾을 수 없습니다. Benefit ID: " + dto.getBenefitIdx()));
        Long campaignIdx = requiredBenefit.getCampaign().getIdx();
        Campaign campaign = campaignRepository.findById(campaignIdx)
                .orElseThrow(() -> new EntityNotFoundException("해당 Campaign을 찾을 수 없습니다. Campaign ID: " + campaignIdx));

        EvaluationDto.StartEvaluation eval;
        eval = EvaluationDto.StartEvaluation.builder()
                .campaign(CampaignDto.Res.from(campaign))
                .benefit(MatchingDto.BenefitRes.toDto(requiredBenefit))
                .partner(requiredBenefit.getOrganization().getName())
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

    private final MongoTemplate mongoTemplate;

    @Transactional
    public void collect(EvaluationDto.CollectDto dto) {

        String targetField = "evaluations." + dto.getCategory().toLowerCase();

        Object evalData = switch (dto.getCategory()){
            case "CUSTOMER" -> ((EvaluationDto.CollectDto.Customer) dto).toEntity();
            case "REVENUE" -> ((EvaluationDto.CollectDto.Revenue) dto).toEntity();
            case "COST" -> ((EvaluationDto.CollectDto.Cost) dto).toEntity();
            case "OPERATION" -> ((EvaluationDto.CollectDto.Operation) dto).toEntity();
            case "BRAND" -> ((EvaluationDto.CollectDto.Brand) dto).toEntity();
            default -> throw new IllegalArgumentException("지원하지 않는 평가 카테고리입니다.");
        };

        Query query = new Query(Criteria.where("sessionID").is(dto.getUuid()));

        Update update =  new Update()
                .set(targetField, evalData)
                .setOnInsert("sessionId", dto.getUuid())
                .setOnInsert("campaignIdx", dto.getCampaignIdx())
                .setOnInsert("benefitIdx", dto.getBenefitIdx())
                .setOnInsert("partner", dto.getPartner())
                .setOnInsert("startedAt", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, EvaluationDocument.class);

        log.info("[Evaluation Collected] Session: {}, Category: {} updated", dto.getUuid(), dto.getCategory());

        UpdateResult result = mongoTemplate.upsert(query, update, EvaluationDocument.class);

        log.info("[MongoDB Upsert 영수증] Acknowledged: {}, Matched: {}, Modified: {}, UpsertedId: {}",
                result.wasAcknowledged(),
                result.getMatchedCount(),
                result.getModifiedCount(),
                result.getUpsertedId());

//        String category = dto.getCategory();
//        // 1. 현재 세션(Evaluation) 조회
//        Evaluation evaluation = evaluationRepository.findBySessionId(dto.getUuid())
//                .orElseGet(() -> {
//                    Campaign campaign = campaignRepository.findById(dto.getCampaignIdx())
//                            .orElseThrow(() -> new EntityNotFoundException("해당 Campaign을 찾을 수 없습니다. Campaign ID: " + dto.getCampaignIdx()));
//                    PartnerBenefits benefits = benefitRepository.findById(dto.getBenefitIdx())
//                            .orElseThrow(() -> new EntityNotFoundException("해당 Benefit을 찾을 수 없습니다. Benefit ID: " + dto.getBenefitIdx()));
//                    Evaluation newEval = Evaluation.builder()
//                            .sessionId(dto.getUuid())
//                            .campaign(campaign)
//                            .benefits(benefits)
//                            .build();
//                    return evaluationRepository.save(newEval);
//                });
//
//        // 2. DTO를 해당 카테고리의 엔티티로 변환
//        Object evalEntity = switch (category) {
//            case "CUSTOMER" -> ((EvaluationDto.CollectDto.Customer) dto).toEntity();
//            case "REVENUE" -> ((EvaluationDto.CollectDto.Revenue) dto).toEntity();
//            case "COST" -> ((EvaluationDto.CollectDto.Cost) dto).toEntity();
//            case "OPERATION" -> ((EvaluationDto.CollectDto.Operation) dto).toEntity();
//            case "BRAND" -> ((EvaluationDto.CollectDto.Brand) dto).toEntity();
//            default -> throw new IllegalArgumentException("잘못된 카테고리입니다.");
//        };
//
//        // 3. 세션 엔티티에 데이터 연결 (Dirty Checking에 의해 자동 업데이트)
//        evaluation.updateEval(evalEntity, category);
    }

    public List<EvaluationDto.MongoEvaluationRes> result(String publicId) {

        Campaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("해당 캠페인을 찾을 수 없습니다."));
        Long campaignIdx = campaign.getIdx();

        Query query = new Query(Criteria.where("campaignIdx").is(campaignIdx));
        List<EvaluationDocument> evalDocs = mongoTemplate.find(query, EvaluationDocument.class);

        if (evalDocs.isEmpty()) {
            throw new EntityNotFoundException("해당 캠페인에 대한 평가 정보가 없습니다. CampaignID: " + campaignIdx);
        }

        return evalDocs.stream()
                .map(EvaluationDto.MongoEvaluationRes::of)
                .collect(Collectors.toList());

//        Long campaignIdx = campaignRepository.findByPublicId(publicId)
//                .orElseThrow(() -> new EntityNotFoundException("해당 캠페인을 찾을 수 없습니다. publicId: " + publicId))
//                .getIdx();
//
//       List<Evaluation> evaluations = evaluationRepository.findAllByCampaignIdx(campaignIdx);
//
//       if (evaluations.isEmpty()) {
//           throw new EntityNotFoundException(("해당 캠페인에 대한 평가 정보가 없습니다. CampaignID: " + campaignIdx));
//       }
//
//       return evaluations.stream()
//               .map(evaluation -> {
//                   PartnerBenefits benefits = evaluation.getBenefits();
//                   Campaign campaign = benefits.getCampaign();
//
//                   return EvaluationDto.EvaluationRes.toDto(
//                           campaign,
//                           benefits,
//                           evaluation,
//                           user.getCompanyName()
//                   );
//               })
//               .collect(Collectors.toList());

    }
}



