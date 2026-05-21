package org.example.evaluation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.model.EvaluationDto;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final MongoTemplate mongoTemplate;

//    @Value("${custom.n8n.webhook-url}${custom.n8n.evaluation-endpoint}")
//    String n8nWebhookUrl;
//
//    public void startEvaluation(EvaluationDto.StartEvaluationReq dto) {
//
//        PartnerBenefits requiredBenefit = benefitRepository.findById(dto.getBenefitIdx())
//                .orElseThrow(() -> new EntityNotFoundException("해당 Benefit을 찾을 수 없습니다. Benefit ID: " + dto.getBenefitIdx()));
//        Long campaignIdx = requiredBenefit.getCampaign().getIdx();
//        Campaign campaign = campaignRepository.findById(campaignIdx)
//                .orElseThrow(() -> new EntityNotFoundException("해당 Campaign을 찾을 수 없습니다. Campaign ID: " + campaignIdx));
//
//        EvaluationDto.StartEvaluation eval;
//        eval = EvaluationDto.StartEvaluation.builder()
//                .campaign(CampaignDto.Res.from(campaign))
//                .benefit(MatchingDto.BenefitRes.toDto(requiredBenefit))
//                .build();
//
//        try {
//            restClient.post()
//                    .uri(n8nWebhookUrl)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .body(eval)
//                    .retrieve()
//                    // 1. 응답을 받았지만 실패한 경우 (상태 코드 기반 세밀한 번역)
//                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
//                        throw new RuntimeException("n8n 엔드포인트를 찾을 수 없습니다.");
//                    })
//                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
//                        throw new RuntimeException("n8n 서버 내부 처리 중 오류가 발생했습니다.");
//                    })
//                    .body(String.class);
//
//        } catch (RestClientException e) {
//            // 2. 서버가 꺼져있거나 타임아웃 등 아예 통신 자체가 실패한 경우 (또는 onStatus에서 잡지 못한 나머지 RestClient 예외)
//            throw new RuntimeException("n8n 서버와 연결할 수 없습니다.", e);
//        }
//    }

//    @Transactional
//    public void collect(EvaluationDto.CollectDto dto) {
//        PartnerBenefits benefits = benefitRepository.findById(dto.getBenefitIdx())
//                .orElseThrow(EntityNotFoundException::new);
//        Campaign campaign = campaignRepository.findById(dto.getCampaignIdx())
//                .orElseThrow(EntityNotFoundException::new);
//
//        String targetField = "evaluations." + dto.getCategory().toLowerCase();
//
//        Object evalData = switch (dto.getCategory()){
//            case "CUSTOMER" -> ((EvaluationDto.CollectDto.Customer) dto).toEntity();
//            case "REVENUE" -> ((EvaluationDto.CollectDto.Revenue) dto).toEntity();
//            case "COST" -> ((EvaluationDto.CollectDto.Cost) dto).toEntity();
//            case "OPERATION" -> ((EvaluationDto.CollectDto.Operation) dto).toEntity();
//            case "BRAND" -> ((EvaluationDto.CollectDto.Brand) dto).toEntity();
//            default -> throw new IllegalArgumentException("지원하지 않는 평가 카테고리입니다.");
//        };
//
//        Query query = new Query(Criteria.where("sessionID").is(dto.getUuid()));
//
//        Update update =  new Update()
//                .set(targetField, evalData)
//                .setOnInsert("sessionId", dto.getUuid())
//                .setOnInsert("campaignIdx", dto.getCampaignIdx())
//                .setOnInsert("benefitIdx", dto.getBenefitIdx())
//                .setOnInsert("goal",campaign.getGoals())
//                .setOnInsert("assetDescription",campaign.getAssetDescription())
//                .setOnInsert("title",benefits.getName())
//                .setOnInsert("target",benefits.getTargetAudience())
//                .setOnInsert("offer",benefits.getDescription())
//                .setOnInsert("partner", benefits.getOrganization().getName())
//                .setOnInsert("startedAt", LocalDateTime.now());
//
//        mongoTemplate.updateFirst(query, update, EvaluationDocument.class);
//
//        log.info("[Evaluation Collected] Session: {}, Category: {} updated", dto.getUuid(), dto.getCategory());
//
//        UpdateResult result = mongoTemplate.upsert(query, update, EvaluationDocument.class);
//
//        log.info("[MongoDB Upsert 영수증] Acknowledged: {}, Matched: {}, Modified: {}, UpsertedId: {}",
//                result.wasAcknowledged(),
//                result.getMatchedCount(),
//                result.getModifiedCount(),
//                result.getUpsertedId());
//    }

    public List<EvaluationDto.MongoEvaluationRes> result(String publicId) {

        Query query = new Query(Criteria.where("publicId").is(publicId));
        List<EvaluationDocument> evalDocs = mongoTemplate.find(query, EvaluationDocument.class);

        return evalDocs.stream()
                .map(EvaluationDto.MongoEvaluationRes::of)
                .collect(Collectors.toList());

    }
}



