package org.example.evaluation.repository;

import org.example.evaluation.model.EvaluationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;


public interface EvaluationMongoRepository extends MongoRepository<EvaluationDocument, String> {
    Optional<EvaluationDocument> findBySessionId(String sessionId);
    List<EvaluationDocument> findAllByCampaignIdx(Long campaignIdx);
}
