package org.example.evaluation.service;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.model.EvaluationDto;
import org.example.evaluation.repository.EvaluationMongoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMongoRepository evaluationMongoRepository;
    private final RestClient restClient;

    @Value("${custom.n8n.webhook-url}${custom.n8n.evaluation-endpoint}")
    private String n8nWebhookUrl;

    public void startEvaluation(EvaluationDto.StartEvaluationReq dto) {
        if (dto == null) {
            throw new IllegalArgumentException("평가 시작 요청이 비어 있습니다.");
        }

        try {
            restClient.post()
                    .uri(n8nWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND, (request, response) -> {
                        throw new RuntimeException("n8n 평가 웹훅을 찾을 수 없습니다.");
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new RuntimeException("n8n 서버에서 평가 처리 중 오류가 발생했습니다.");
                    })
                    .body(String.class);

            log.info("[Evaluation] n8n evaluation requested. campaignIdx={}", dto.getCampaignIdx());
        } catch (RestClientException e) {
            throw new RuntimeException("n8n 서버와 연결할 수 없습니다.", e);
        }
    }

    public EvaluationDocument save(EvaluationDto.SaveEvaluationReq dto) {
        if (dto == null) {
            throw new IllegalArgumentException("평가 저장 요청이 비어 있습니다.");
        }
        if (dto.getSessionId() == null || dto.getSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId 값은 필수입니다.");
        }

        EvaluationDocument saved = evaluationMongoRepository.save(dto.toDocument());
        log.info("[Evaluation] result saved. sessionId={}, campaignIdx={}",
                saved.getSessionId(), saved.getCampaignIdx());
        return saved;
    }

    public EvaluationDocument getBySessionId(String sessionId) {
        return evaluationMongoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "해당 평가 세션을 찾을 수 없습니다. sessionId: " + sessionId
                ));
    }

    public List<EvaluationDto.MongoEvaluationRes> getByCampaignIdx(String campaignIdx) {
        List<EvaluationDocument> documents = evaluationMongoRepository.findAllByCampaignIdx(campaignIdx);

        if (documents.isEmpty()) {
            throw new NoSuchElementException(
                    "해당 캠페인에 대한 평가 정보가 없습니다. campaignIdx: " + campaignIdx
            );
        }

        return documents.stream()
                .map(EvaluationDto.MongoEvaluationRes::of)
                .toList();
    }
}
