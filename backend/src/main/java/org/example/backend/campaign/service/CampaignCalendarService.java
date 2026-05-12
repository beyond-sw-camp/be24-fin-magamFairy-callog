package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CalendarEventsDto;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.model.CampaignIntro;
import org.example.backend.campaign.repository.CampaignIntroRepository;
import org.example.backend.teamboard.model.MileStones;
import org.example.backend.teamboard.repository.MileStonesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 캘린더 페이지 일괄 조회.
 * - 기존: 캠페인 N개 → intro N회 + milestones N회 호출 (총 2N+1)
 * - 현재: 캠페인 1회 + intro batch 1회 + milestones batch 1회 = 총 3회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignCalendarService {

    private final CampaignService campaignService;
    private final CampaignIntroRepository introRepository;
    private final MileStonesRepository milestonesRepository;

    public CalendarEventsDto.Res loadEvents(Long userIdx, String scope) {
        // 1) 내 캠페인 목록
        List<CampaignDto.Res> campaigns = campaignService.listCampaigns(userIdx, scope);

        List<Long> campaignIds = campaigns.stream()
                .map(CampaignDto.Res::idx)
                .toList();

        // 캠페인 항목으로 변환 (권한 정보 포함, idx + publicId 둘 다 노출)
        List<CalendarEventsDto.CampaignItem> campaignItems = campaigns.stream()
                .map(c -> new CalendarEventsDto.CampaignItem(
                        c.idx(),
                        c.id(),  // publicId
                        c.name(),
                        c.startDate(),
                        c.endDate(),
                        c.status(),
                        c.ownerName(),
                        c.color(),
                        c.icon(),
                        c.myCampaignRole() == null ? null : c.myCampaignRole().name(),
                        c.organizationIsPm()
                ))
                .toList();

        if (campaignIds.isEmpty()) {
            return new CalendarEventsDto.Res(campaignItems, List.of(), List.of());
        }

        // 2) intro batch 조회 → recruitDeadline 추출
        List<CampaignIntro> intros = introRepository.findAllByCampaign_IdxIn(campaignIds);
        List<CalendarEventsDto.DeadlineItem> deadlines = intros.stream()
                .filter(i -> i.getRecruitDeadline() != null)
                .map(i -> new CalendarEventsDto.DeadlineItem(
                        i.getCampaign().getIdx(),
                        i.getCampaign().getPublicId(),
                        i.getCampaign().getName(),
                        i.getRecruitDeadline()
                ))
                .sorted(Comparator.comparing(CalendarEventsDto.DeadlineItem::recruitDeadline))
                .toList();

        // 3) milestones batch 조회
        List<MileStones> milestones = milestonesRepository.findAllByCampaign_IdxIn(campaignIds);
        List<CalendarEventsDto.MilestoneItem> milestoneItems = milestones.stream()
                .filter(m -> m.getStartDate() != null || m.getEndDate() != null)
                .map(m -> new CalendarEventsDto.MilestoneItem(
                        m.getIdx(),
                        m.getCampaign().getIdx(),
                        m.getCampaign().getPublicId(),
                        m.getCampaign().getName(),
                        m.getName(),
                        m.getStartDate(),
                        m.getEndDate()
                ))
                .toList();

        return new CalendarEventsDto.Res(campaignItems, deadlines, milestoneItems);
    }
}
