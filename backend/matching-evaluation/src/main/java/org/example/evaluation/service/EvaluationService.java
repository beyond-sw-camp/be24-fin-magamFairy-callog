package org.example.evaluation.service;

import lombok.RequiredArgsConstructor;
import org.example.evaluation.model.EvaluationDocument;
import org.example.evaluation.model.EvaluationDto;
import org.example.evaluation.repository.EvaluationMongoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final EvaluationMongoRepository evaluationMongoRepository;

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
