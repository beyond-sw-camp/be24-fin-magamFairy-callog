package org.example.backend.matching.repository;

import org.example.backend.matching.model.evaluation.EvaluationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EvaluationMongoRepository extends MongoRepository<EvaluationDocument, String> {
    Optional<EvaluationDocument> findBySessionId(String sessionId);
}
