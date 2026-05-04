package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.matching.model.Evaluation;
import org.example.backend.matching.model.EvaluationDto;
import org.example.backend.matching.repository.EvaluationRepository;
import org.example.backend.organization.model.Organization;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final CampaignParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public EvaluationDto.EvaluationRes getEvaluationRes(Long userIdx, Long campaignIdx) {
        // 1. 유저의 단체 정보 가져오기
        Organization organization = userRepository.findById(userIdx)
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."))
                .getOrganization();

        Long organizationIdx = organization.getIdx();

        // 2. 매핑 테이블(CampaignParticipant)에서 존재 여부 확인
        boolean isParticipant = participantRepository.existsByCampaignIdxAndOrganizationIdx(
                campaignIdx,
                organizationIdx
        );

        if (!isParticipant) {
            // Spring Security를 쓰신다면 아래 예외, 아니면 RuntimeException 사용
            throw new org.springframework.security.access.AccessDeniedException("해당 캠페인 참여 권한이 없습니다.");
        }
        else {
            // 3. 평가 데이터 조회 및 반환
            Evaluation evaluation = evaluationRepository.findByCampaignIdxAndOrganizationIdx(campaignIdx, organizationIdx)
                    .orElseThrow(() -> new EntityNotFoundException("평가 데이터를 찾을 수 없습니다."));

            return EvaluationDto.EvaluationRes.toDto(evaluation);
        }
    }

    public void newEvaluation(EvaluationDto.NewEvaluation dto, AuthUserDetails user) {
    }
}
