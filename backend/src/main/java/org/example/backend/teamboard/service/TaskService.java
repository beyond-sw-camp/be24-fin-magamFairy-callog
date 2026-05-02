package org.example.backend.teamboard.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.teamboard.model.MileStones;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskDto;
import org.example.backend.teamboard.model.TaskParts;
import org.example.backend.teamboard.repository.MileStonesRepository;
import org.example.backend.teamboard.repository.TaskPartsRepository;
import org.example.backend.teamboard.repository.TaskRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskPartsRepository taskPartsRepository;
    private final MileStonesRepository mileStonesRepository;
    private final UserRepository userRepository;
    private final CampaignParticipantRepository participantRepository;

    /** 메인 팀 보드 - 모든 Task */
    public List<TaskDto.ResList> listAll() {
        return taskRepository.findAllByOrderByIdxDesc().stream()
                .map(TaskDto.ResList::from)
                .toList();
    }

    /** 캠페인 팀 보드 - 캠페인 종속 Task */
    public List<TaskDto.ResList> listByCampaign(Long campaignIdx) {
        return taskRepository.findAllByTaskPart_Campaign_IdxOrderByIdxDesc(campaignIdx).stream()
                .map(TaskDto.ResList::from)
                .toList();
    }

    public TaskDto.ResTask getOne(Long taskIdx) {
        return TaskDto.ResTask.from(getTaskOrThrow(taskIdx));
    }

    @Transactional
    public TaskDto.ResTask create(Long campaignIdx, TaskDto.ReqTask req) {
        TaskParts taskPart = req.taskPartId() != null ? getTaskPartOrThrow(req.taskPartId()) : null;
        if (taskPart != null && !taskPart.getCampaign().getIdx().equals(campaignIdx)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "캠페인과 업무 파트가 일치하지 않습니다.");
        }

        MileStones milestone = req.milestoneId() != null ? getMilestoneOrThrow(req.milestoneId()) : null;
        User assignee = req.assigneeId() != null ? getUserOrThrow(req.assigneeId()) : null;
        CampaignParticipant participant = req.participantId() != null
                ? getParticipantOrThrow(req.participantId()) : null;

        Task saved = taskRepository.save(req.toEntity(participant, taskPart, milestone, assignee));
        return TaskDto.ResTask.from(saved);
    }

    @Transactional
    public TaskDto.ResTask update(Long taskIdx, TaskDto.ReqTask req) {
        Task task = getTaskOrThrow(taskIdx);

        TaskParts taskPart = req.taskPartId() != null ? getTaskPartOrThrow(req.taskPartId()) : task.getTaskPart();
        MileStones milestone = req.milestoneId() != null ? getMilestoneOrThrow(req.milestoneId()) : task.getMilestone();
        User assignee = req.assigneeId() != null ? getUserOrThrow(req.assigneeId()) : task.getAssignee();
        CampaignParticipant participant = req.participantId() != null
                ? getParticipantOrThrow(req.participantId()) : task.getParticipant();

        task.update(
                req.name(),
                participant,
                req.dueDate(),
                req.taskType(),
                req.status() != null ? req.status() : task.getStatus(),
                taskPart,
                milestone,
                assignee,
                req.priority() != null ? req.priority() : task.getPriority(),
                req.memo()
        );
        return TaskDto.ResTask.from(task);
    }

    @Transactional
    public void delete(Long taskIdx) {
        if (!taskRepository.existsById(taskIdx)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "업무를 찾을 수 없습니다.");
        }
        taskRepository.deleteById(taskIdx);
    }

    private Task getTaskOrThrow(Long taskIdx) {
        return taskRepository.findById(taskIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "업무를 찾을 수 없습니다."));
    }

    private TaskParts getTaskPartOrThrow(Long taskPartIdx) {
        return taskPartsRepository.findById(taskPartIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "업무 파트를 찾을 수 없습니다."));
    }

    private MileStones getMilestoneOrThrow(Long milestoneIdx) {
        return mileStonesRepository.findById(milestoneIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "마일스톤을 찾을 수 없습니다."));
    }

    private User getUserOrThrow(Long userIdx) {
        return userRepository.findById(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private CampaignParticipant getParticipantOrThrow(Long participantIdx) {
        return participantRepository.findById(participantIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "참여사를 찾을 수 없습니다."));
    }
}
