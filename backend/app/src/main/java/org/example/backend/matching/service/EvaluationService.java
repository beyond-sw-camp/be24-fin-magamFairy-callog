package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.matching.client.MatchingEvaluationClient;
import org.example.backend.matching.event.EvaluationStartRequestedEvent;
import org.example.backend.matching.kafka.EvaluationKafkaProducer;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.model.PartnerBenefits;
import org.example.backend.matching.model.evaluation.EvaluationDto;
import org.example.backend.matching.repository.BenefitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final BenefitRepository benefitRepository;
    private final CampaignRepository campaignRepository;
    private final EvaluationKafkaProducer evaluationKafkaProducer;
    private final MatchingEvaluationClient matchingEvaluationClient;

    @Transactional(readOnly = true)
    public void startEvaluation(EvaluationDto.StartEvaluationReq dto) {
        PartnerBenefits benefit = benefitRepository.findById(dto.getBenefitIdx())
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 Benefit을 찾을 수 없습니다. Benefit ID: " + dto.getBenefitIdx()
                ));

        Campaign campaign = campaignRepository.findByPublicId(dto.getPublicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 Campaign을 찾을 수 없습니다. Campaign ID: " + benefit.getCampaign().getIdx()
                ));

        EvaluationDto.StartEvaluation request = EvaluationDto.StartEvaluation.builder()
                .campaignIdx(campaign.getPublicId())
                .campaign(CampaignDto.Res.from(campaign))
                .benefit(MatchingDto.BenefitRes.toDto(benefit))
                .build();

        log.info("[Evaluation MSA] start request forwarded. benefitIdx={}, campaignIdx={}",
                benefit.getIdx(), campaign.getIdx());

        evaluationKafkaProducer.sendStartReply(request);
    }

    public void collect(EvaluationDto.CollectDto dto) {
        log.info("[Evaluation MSA] collect request forwarded. uuid={}, category={}",
                dto.getUuid(), dto.getCategory());

        evaluationKafkaProducer.collect(dto);
    }

    public List<EvaluationDto.MongoEvaluationRes> result(String publicId) {
        Campaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 캠페인을 찾을 수 없습니다. publicId: " + publicId
                ));

        log.info("[Evaluation MSA] result request forwarded. publicId={}, campaignIdx={}",
                publicId, campaign.getIdx());

        return matchingEvaluationClient.getResult(campaign.getPublicId());
    }

    public void processAndReplyEvaluation(EvaluationStartRequestedEvent queryEvent) {
    }
}