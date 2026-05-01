package org.example.backend.teamboard.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.teamboard.model.MileStones;
import org.example.backend.teamboard.model.MileStonesDto;
import org.example.backend.teamboard.repository.MileStonesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MileStonesService {

    private final MileStonesRepository mileStonesRepository;
    private final CampaignRepository campaignRepository;

    public List<MileStonesDto.ResList> listByCampaign(Long campaignIdx) {
        return mileStonesRepository.findAllByCampaign_IdxOrderBySortOrderAscIdxAsc(campaignIdx).stream()
                .map(MileStonesDto.ResList::from)
                .toList();
    }

    public MileStonesDto.ResMileStones getOne(Long milestoneIdx) {
        return MileStonesDto.ResMileStones.from(getMilestoneOrThrow(milestoneIdx));
    }

    @Transactional
    public MileStonesDto.ResMileStones create(Long campaignIdx, MileStonesDto.ReqMileStones req) {
        Campaign campaign = getCampaignOrThrow(campaignIdx);
        MileStones saved = mileStonesRepository.save(req.toEntity(campaign));
        return MileStonesDto.ResMileStones.from(saved);
    }

    @Transactional
    public MileStonesDto.ResMileStones update(Long milestoneIdx, MileStonesDto.ReqMileStones req) {
        MileStones milestone = getMilestoneOrThrow(milestoneIdx);
        milestone.update(req.name(), req.startDate(), req.endDate(), req.description(), req.sortOrder());
        return MileStonesDto.ResMileStones.from(milestone);
    }

    @Transactional
    public void delete(Long milestoneIdx) {
        if (!mileStonesRepository.existsById(milestoneIdx)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "마일스톤을 찾을 수 없습니다.");
        }
        mileStonesRepository.deleteById(milestoneIdx);
    }

    private MileStones getMilestoneOrThrow(Long milestoneIdx) {
        return mileStonesRepository.findById(milestoneIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "마일스톤을 찾을 수 없습니다."));
    }

    private Campaign getCampaignOrThrow(Long campaignIdx) {
        return campaignRepository.findById(campaignIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));
    }
}
