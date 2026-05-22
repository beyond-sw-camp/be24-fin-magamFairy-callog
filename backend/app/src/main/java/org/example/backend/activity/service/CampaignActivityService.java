package org.example.backend.activity.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.activity.model.CampaignActivity;
import org.example.backend.activity.repository.CampaignActivityRepository;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캠페인 활동 로그 기록 헬퍼.
 * 업무/상태 변경 등 도메인 서비스에서 record(...) 를 호출해 1행 write.
 * 호출자(예: TaskService) 의 트랜잭션에 join 되어 함께 commit/rollback.
 */
@Service
@RequiredArgsConstructor
public class CampaignActivityService {

    private final CampaignActivityRepository activityRepository;

    @Transactional
    public void record(Campaign campaign, User actor, String type, String description) {
        if (campaign == null) return; // 캠페인 미연결(개인 업무) 활동은 기록하지 않음
        activityRepository.save(CampaignActivity.builder()
                .campaign(campaign)
                .actor(actor)
                .type(type)
                .description(description)
                .build());
    }
}
