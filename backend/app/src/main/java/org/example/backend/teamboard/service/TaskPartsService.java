package org.example.backend.teamboard.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.teamboard.model.MileStones;
import org.example.backend.teamboard.model.TaskParts;
import org.example.backend.teamboard.model.TaskPartsDto;
import org.example.backend.teamboard.repository.MileStonesRepository;
import org.example.backend.teamboard.repository.TaskPartsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskPartsService {

    private final TaskPartsRepository taskPartsRepository;
    private final MileStonesRepository mileStonesRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignParticipantRepository participantRepository;

    public List<TaskPartsDto.ResList> listByCampaign(Long campaignIdx) {
        return taskPartsRepository.findAllByCampaign_IdxOrderBySortOrderAscIdxAsc(campaignIdx).stream()
                .map(TaskPartsDto.ResList::from)
                .toList();
    }

    public TaskPartsDto.ResTaskParts getOne(Long taskPartIdx) {
        return TaskPartsDto.ResTaskParts.from(getTaskPartOrThrow(taskPartIdx));
    }

    @Transactional
    public TaskPartsDto.ResTaskParts create(
            Long campaignIdx,
            Long milestoneIdx,
            TaskPartsDto.ReqTaskParts req
    ) {
        Campaign campaign = getCampaignOrThrow(campaignIdx);
        MileStones milestone = getMilestoneOrThrow(milestoneIdx);
        CampaignParticipant participant = req.participantId() != null
                ? participantRepository.findById(req.participantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "참여사를 찾을 수 없습니다."))
                : null;

        TaskParts saved = taskPartsRepository.save(req.toEntity(campaign, milestone, participant));
        return TaskPartsDto.ResTaskParts.from(saved);
    }

    @Transactional
    public TaskPartsDto.ResTaskParts update(
            Long taskPartIdx,
            Long milestoneIdx,
            TaskPartsDto.ReqTaskParts req
    ) {
        TaskParts taskPart = getTaskPartOrThrow(taskPartIdx);
        MileStones milestone = milestoneIdx != null ? getMilestoneOrThrow(milestoneIdx) : taskPart.getMileStones();
        CampaignParticipant participant = req.participantId() != null
                ? participantRepository.findById(req.participantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "참여사를 찾을 수 없습니다."))
                : taskPart.getParticipant();

        taskPart.update(
                milestone,
                participant,
                req.name(),
                req.reviewFlow(),
                req.taskPriority(),
                req.dependency(),
                req.deliverable(),
                req.description()
        );
        return TaskPartsDto.ResTaskParts.from(taskPart);
    }

    @Transactional
    public void delete(Long taskPartIdx) {
        if (!taskPartsRepository.existsById(taskPartIdx)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "업무 파트를 찾을 수 없습니다.");
        }
        taskPartsRepository.deleteById(taskPartIdx);
    }

    private TaskParts getTaskPartOrThrow(Long taskPartIdx) {
        return taskPartsRepository.findById(taskPartIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "업무 파트를 찾을 수 없습니다."));
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
