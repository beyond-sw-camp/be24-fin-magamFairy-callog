package org.example.evaluation.service;

import java.util.List;
import java.util.NoSuchElementException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.model.EvaluationDto;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final RestClient restClient;

    @Value("${custom.n8n.webhook-url}${custom.n8n.evaluation-endpoint}")
    String n8nWebhookUrl;

    public void startEvaluation(EvaluationDto.StartEvaluationReq dto) {
        try {
            restClient.post()
                    .uri(n8nWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new RuntimeException("n8n 엔드포인트를 찾을 수 없습니다.");
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new RuntimeException("n8n 서버 내부 처리 중 오류가 발생했습니다.");
                    })
                    .body(String.class);

        } catch (RestClientException e) {
            throw new RuntimeException("n8n 서버와 연결할 수 없습니다.", e);
        }
    }
}

//    private final MongoTemplate mongoTemplate;
//
//    public List<EvaluationDto.MongoEvaluationRes> result(String publicId) {
//
//
//        Query query = new Query(Criteria.where("campaignIdx").is(campaignIdx));
//        List<EvaluationDocument> evalDocs = mongoTemplate.find(query, EvaluationDocument.class);
//
//        if (evalDocs.isEmpty()) {
//            throw new EntityNotFoundException("해당 캠페인에 대한 평가 정보가 없습니다. CampaignIdx: " + campaignIdx);
//        }
//
//        return documents.stream()
//                .map(EvaluationDto.MongoEvaluationRes::of)
//                .collect(Collectors.toList());
//
//    }
//}



