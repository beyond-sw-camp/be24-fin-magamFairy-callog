package org.example.backend.matching.client;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.model.BaseResponse;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchingEvaluationClient {

    private final RestClient restClient;

    @Value("${msa.matching-evaluation.base-url}")
    private String baseUrl;

    public void startEvaluation(EvaluationDto.StartEvaluation request) {
        restClient.post()
                .uri(baseUrl + "/evaluation/start")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void collect(EvaluationDto.CollectDto request) {
        restClient.post()
                .uri(baseUrl + "/evaluation/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public List<EvaluationDto.MongoEvaluationRes> getResult(Long campaignIdx) {
        BaseResponse<List<EvaluationDto.MongoEvaluationRes>> response =
                restClient.get()
                        .uri(baseUrl + "/evaluation/result?campaignIdx={campaignIdx}", campaignIdx)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

        return response.getData();
    }
}