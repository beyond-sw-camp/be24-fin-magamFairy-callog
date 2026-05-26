package org.example.evaluation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.evaluation.event.EvaluationCollectRequestedEvent;
import org.example.evaluation.event.EvaluationStartRequestedEvent;
import org.example.evaluation.kafka.EvaluationKafkaProducer;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.model.EvaluationDto;
import org.example.evaluation.model.N8nEvaluationPayloadDto;
import org.example.evaluation.repository.EvaluationMongoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final EvaluationMongoRepository evaluationMongoRepository;
    private final EvaluationKafkaProducer evaluationKafkaProducer;

    @Value("${custom.n8n.webhook-url}${custom.n8n.evaluation-endpoint}")
    private String n8nWebhookUrl;

//    public void requestStartEvaluation(EvaluationDto.StartEvaluationReq dto) {
//        EvaluationStartRequestedEvent event = EvaluationStartRequestedEvent.builder()
//                .campaignPublicId(dto.getCampaignIdx())
//                .campaign(dto.getCampaign())
//                .benefit(dto.getBenefit())
//                .build();
//
//        evaluationKafkaProducer.sendStart(event);
//    }

    public void requestStartEvaluation(EvaluationDto.StartEvaluationReq dto) {
        EvaluationStartRequestedEvent event = EvaluationStartRequestedEvent.builder()
                .campaignPublicId(dto.getCampaignIdx())
                .benefitIdx(dto.getBenefitIdx())
                .build();

        log.info("[Evaluation MSA] 메인 모듈에 데이터 조회 요청 전송. publicId={}, benefitIdx={}",
                dto.getCampaignIdx(), dto.getBenefitIdx());

        evaluationKafkaProducer.sendStart(event);
    }

    public void requestCollectEvaluation(EvaluationDto.SaveEvaluationReq dto) {
        EvaluationCollectRequestedEvent event = EvaluationCollectRequestedEvent.builder()
                .sessionId(dto.getSessionId())
                .campaignPublicId(dto.getPublicId())
                .goal(dto.getGoal())
                .title(dto.getTitle())
                .partner(dto.getPartner())
                .assetDescription(dto.getAssetDescription())
                .offer(dto.getOffer())
                .target(dto.getTarget())
                .evaluations(objectMapper.valueToTree(dto.getEvaluations()))
                .build();

        evaluationKafkaProducer.sendCollect(event);
    }

    public void startEvaluation(EvaluationDto.StartEvaluation dto) {
        try {
            restClient.post()
                    .uri(n8nWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new RuntimeException("n8n endpoint was not found.");
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new RuntimeException("n8n failed while processing the request.");
                    })
                    .body(String.class);

        } catch (RestClientException e) {
            throw new RuntimeException("Could not connect to n8n.", e);
        }
    }

    public EvaluationDocument save(EvaluationCollectRequestedEvent event) {
        try {
            EvaluationDocument existing = evaluationMongoRepository.findBySessionId(event.getSessionId())
                    .orElse(null);

            EvaluationDocument document = EvaluationDocument.builder()
                    .id(existing != null ? existing.getId() : null)
                    .sessionId(event.getSessionId())
                    .publicId(event.getCampaignPublicId())
                    .goal(event.getGoal())
                    .title(event.getTitle())
                    .partner(event.getPartner())
                    .assetDescription(event.getAssetDescription())
                    .offer(event.getOffer())
                    .target(event.getTarget())
                    .evaluations(event.getEvaluations() == null
                            ? null
                            : objectMapper.treeToValue(event.getEvaluations(), EvaluationDocument.Evaluations.class))
                    .startedAt(existing != null ? existing.getStartedAt() : null)
                    .build();

            return evaluationMongoRepository.save(document);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Evaluation result payload conversion failed.", e);
        }
    }

    public List<EvaluationDto.MongoEvaluationRes> result(String publicId) {
        List<EvaluationDocument> documents = evaluationMongoRepository.findAllByPublicId(publicId);

        if (documents.isEmpty()) {
            throw new NoSuchElementException("No evaluation result found. publicId: " + publicId);
        }

        return documents.stream()
                .map(EvaluationDto.MongoEvaluationRes::of)
                .collect(Collectors.toList());
    }
    private final MongoTemplate mongoTemplate;
    public void collect(N8nEvaluationPayloadDto payload) {
        Query query = new Query(Criteria.where("sessionId").is(payload.getUuid()));
        Update update = new Update();
        // 2. 공통 기본 인프라 정보 업데이트 설정
        update.set("publicId", String.valueOf(payload.getPublicId()));

        // 3. 카테고리별 분기 및 객체 바인딩 수행 [cite: 5, 10, 20]
        String category = payload.getCategory().toUpperCase();
        switch (category) {
            case "BRAND":
                EvaluationDocument.Brand brand = EvaluationDocument.Brand.builder()
                        .overallScore(payload.getOverallScore())
                        .improvementDirections(payload.getImprovementDirections())
                        .brandTone(payload.getBrandTone())
                        .priceRange(payload.getPriceRange())
                        .customerExperience(payload.getCustomerExperience())
                        .brandTrust(payload.getBrandTrust())
                        .reputationRisk(payload.getReputationRisk())
                        .hanwhaImageConsistency(payload.getHanwhaImageConsistency())
                        .build();
                update.set("evaluations.brand", brand);
                break;

            case "COST":
                EvaluationDocument.Cost cost = EvaluationDocument.Cost.builder()
                        .overallScore(payload.getOverallScore())
                        .improvementDirections(payload.getImprovementDirections())
                        .partnerSampleScale(payload.getPartnerSampleScale())
                        .partnerDiscountCostBurden(payload.getPartnerDiscountCostBurden())
                        .coProductionCostSharing(payload.getCoProductionCostSharing())
                        .hanwhaDirectCostBurden(payload.getHanwhaDirectCostBurden())
                        .existingHanwhaChannelUtilization(payload.getExistingHanwhaChannelUtilization())
                        .build();
                update.set("evaluations.cost", cost);
                break;

            case "CUSTOMER":
                EvaluationDocument.Customer customer = EvaluationDocument.Customer.builder()
                        .overallScore(payload.getOverallScore())
                        .improvementDirections(payload.getImprovementDirections())
                        .customerAgeGroup(payload.getCustomerAgeGroup())
                        .customerSpendingPatterns(payload.getCustomerSpendingPatterns())
                        .membershipTier(payload.getMembershipTier())
                        .usageChannel(payload.getUsageChannel())
                        .benefitCategory(payload.getBenefitCategory())
                        .build();
                update.set("evaluations.customer", customer);
                break;

            case "OPERATION":
                EvaluationDocument.Operation operation = EvaluationDocument.Operation.builder()
                        .overallScore(payload.getOverallScore())
                        .improvementDirections(payload.getImprovementDirections())
                        .approvalStepsCount(payload.getApprovalStepsCount())
                        .legalReviewRequired(payload.getLegalReviewRequired())
                        .brandReviewRequired(payload.getBrandReviewRequired())
                        .deliverablesCount(payload.getDeliverablesCount())
                        .participatingDeptsAndPartners(payload.getParticipatingDeptsAndPartners())
                        .scheduleUrgency(payload.getScheduleUrgency())
                        .offlineOrOnsiteStaffRequired(payload.getOfflineOrOnsiteStaffRequired())
                        .build();
                update.set("evaluations.operation", operation);
                break;

            case "REVENUE":
                EvaluationDocument.Revenue revenue = EvaluationDocument.Revenue.builder()
                        .overallScore(payload.getOverallScore())
                        .improvementDirections(payload.getImprovementDirections())
                        .purchaseConversionProbability(payload.getPurchaseConversionProbability())
                        .roomReservationIncreaseProbability(payload.getRoomReservationIncreaseProbability())
                        .appRegistrationIncreaseProbability(payload.getAppRegistrationIncreaseProbability())
                        .membershipRegistrationRevisitProbability(payload.getMembershipRegistrationRevisitProbability())
                        .alignmentwithCampaignGoalsandKPIs(payload.getAlignmentwithCampaignGoalsandKPIs())
                        .build();
                update.set("evaluations.revenue", revenue);
                break;

            default:
                log.error("Unknown evaluation category type: {}", category);
                throw new IllegalArgumentException("Unknown evaluation category type: " + category);
        }

        // 4. 원자적 Upsert 실행 (@EnableMongoAuditing이 적용되어 있다면 내부적으로 startedAt/endedAt도 제어됨)
        mongoTemplate.upsert(query, update, EvaluationDocument.class);
        log.info("Successfully upserted data to MongoDB for sessionId: {}", payload.getUuid());
    }
}
