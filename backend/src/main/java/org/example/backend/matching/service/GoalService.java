package org.example.backend.matching.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.backend.matching.model.CampaignGoal;
import org.example.backend.matching.model.MatchingDto;
import org.example.backend.matching.repository.GoalRepository;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalService {
    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public MatchingDto.GoalRes getGoal(Long idx) {
        return MatchingDto.GoalRes.toDto(
                goalRepository.findById(idx).orElseThrow(EntityNotFoundException::new)
        );
    }

    public MatchingDto.GoalList getGoalList(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CampaignGoal> result = goalRepository.findAll(pageRequest);
        return MatchingDto.GoalList.toDto(result);
    }

    @Transactional
    public void addGoal(MatchingDto.AddGoal dto, AuthUserDetails user) {
        validateWeights(dto);
        validatePeriod(dto);

        User userEntity = userRepository.getReferenceById(user.getIdx());

        CampaignGoal goal = CampaignGoal.builder()
                .name(dto.getName())
                .primaryType(dto.getPrimaryType())
                .secondaryType(dto.getSecondaryType())
                .kpiPrimary(dto.getKpiPrimary())
                .kpiSecondary(dto.getKpiSecondary())
                .budgetLimit(dto.getBudgetLimit())
                .effortLimit(dto.getEffortLimit())
                .periodStart(dto.getPeriodStart())
                .periodEnd(dto.getPeriodEnd())
                .weightRevenue(dto.getWeightRevenue())
                .weightEffort(dto.getWeightEffort())
                .weightBrand(dto.getWeightBrand())
                .owner(userEntity)
                .ownerOrganization(userEntity.getOrganization())
                .ownerLabel(dto.getOwnerLabel())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        goalRepository.save(goal);
    }

    @Transactional
    public void updateGoal(Long idx, MatchingDto.AddGoal dto) {
        validateWeights(dto);
        validatePeriod(dto);

        CampaignGoal goal = goalRepository.findById(idx).orElseThrow(EntityNotFoundException::new);
        goal.update(dto);
    }

    @Transactional
    public void deleteGoal(Long idx) {
        if (!goalRepository.existsById(idx)) {
            throw new EntityNotFoundException();
        }

        goalRepository.deleteById(idx);
    }

    private void validateWeights(MatchingDto.AddGoal dto) {
        Integer revenue = dto.getWeightRevenue();
        Integer effort = dto.getWeightEffort();
        Integer brand = dto.getWeightBrand();

        if (revenue == null || effort == null || brand == null) {
            throw new IllegalArgumentException("가중치를 모두 입력해야 합니다.");
        }

        if (revenue + effort + brand != 100) {
            throw new IllegalArgumentException("가중치 합계는 100이어야 합니다.");
        }
    }

    private void validatePeriod(MatchingDto.AddGoal dto) {
        if (dto.getPeriodStart() == null || dto.getPeriodEnd() == null) {
            throw new IllegalArgumentException("시작일과 종료일을 입력해야 합니다.");
        }

        if (dto.getPeriodEnd().isBefore(dto.getPeriodStart())) {
            throw new IllegalArgumentException("종료일은 시작일 이후여야 합니다.");
        }
    }
}
