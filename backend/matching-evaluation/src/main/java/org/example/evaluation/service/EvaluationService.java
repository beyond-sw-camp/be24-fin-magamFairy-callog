package org.example.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.model.EvaluationDto;
import org.example.evaluation.repository.EvaluationMongoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final EvaluationMongoRepository evaluationMongoRepository;
    private final RestClient restClient;

    @Value("${custom.n8n.webhook-url}${custom.n8n.evaluation-endpoint}")
    private String n8nWebhookUrl;

    public void startEvaluation(EvaluationDto.StartEvaluationReq dto) {
        restClient.post()
                .uri(n8nWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    public EvaluationDocument save(EvaluationDto.SaveEvaluationReq dto) {
        return evaluationMongoRepository.save(dto.toDocument());
    }

    public EvaluationDocument getBySessionId(String sessionId) {
        return evaluationMongoRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new NoSuchElementException(
                        "해당 평가 결과를 찾을 수 없습니다. sessionId: " + sessionId
                ));
    }

    public List<EvaluationDto.MongoEvaluationRes> getByCampaignIdx(Long campaignIdx) {
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
