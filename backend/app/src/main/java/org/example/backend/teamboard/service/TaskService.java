package org.example.backend.teamboard.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.activity.service.CampaignActivityService;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.model.CampaignMemberRole;
import org.example.backend.campaign.model.CampaignParticipant;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignParticipantRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.redis.CacheNames;
import org.example.backend.common.redis.DashboardCacheEvictor;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.notification.service.NotificationSseService;
import org.example.backend.teamboard.model.MileStones;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskDto;
import org.example.backend.teamboard.model.TaskParts;
import org.example.backend.teamboard.model.TaskStatus;
import org.example.backend.teamboard.repository.MileStonesRepository;
import org.example.backend.teamboard.repository.TaskPartsRepository;
import org.example.backend.teamboard.repository.TaskRepository;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskPartsRepository taskPartsRepository;
    private final MileStonesRepository mileStonesRepository;
    private final UserRepository userRepository;
    private final CampaignParticipantRepository participantRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignRepository campaignRepository;
    private final NotificationService notificationService;
    private final NotificationSseService sseService;
    private final DashboardCacheEvictor dashboardCacheEvictor;
    private final CampaignActivityService activityService;
    private final CacheManager cacheManager;

    /** 메인 팀 보드 - 내가 참여한 캠페인의 Task + 내 개인 업무(캠페인 무관). */
    public List<TaskDto.ResList> listAll(Long userIdx) {
        List<Long> campaignIds = campaignMemberRepository.findAllWithCampaignByUserIdx(userIdx)
                .stream()
                .map(cm -> cm.getCampaign().getIdx())
                .toList();
        List<Task> campaignTasks = campaignIds.isEmpty()
                ? List.of()
                : taskRepository.findAllByCampaignIdsDirectOrViaTaskPart(campaignIds);
        // 개인 업무: 캠페인 연결 전혀 없는 본인 담당 Task (캘린더 개인 일정)
        List<Task> personalTasks = taskRepository
                .findAllByAssignee_IdxAndCampaignIsNullAndParticipantIsNullAndMilestoneIsNullAndTaskPartIsNullOrderByIdxDesc(userIdx);
        return java.util.stream.Stream.concat(campaignTasks.stream(), personalTasks.stream())
                .map(TaskDto.ResList::from)
                .toList();
    }

    /** 개인 업무 생성 — 캠페인/참여사/마일스톤/업무파트 없이, 담당자는 항상 본인. */
    @Transactional
    public TaskDto.ResTask createPersonal(TaskDto.ReqTask req, AuthUserDetails authUser) {
        if (authUser == null || authUser.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        User assignee = getUserOrThrow(authUser.getIdx());
        Task saved = taskRepository.save(req.toEntity(null, null, null, assignee));
        sseService.broadcastCalendarRefresh(null, "task");
        evictTaskDashboard(null, null, null, assignee);
        return TaskDto.ResTask.from(saved);
    }

    /** 캠페인 팀 보드 - 캠페인 종속 Task (직접 campaign_id 또는 업무파트 경유) */
    @Cacheable(value = CacheNames.TASK_LIST, key = "#campaignIdx", unless = "#result == null")
    public List<TaskDto.ResList> listByCampaign(Long campaignIdx) {
        return taskRepository.findAllByCampaignDirectOrViaTaskPart(campaignIdx).stream()
                .map(TaskDto.ResList::from)
                .toList();
    }

    public TaskDto.ResTask getOne(Long taskIdx) {
        return TaskDto.ResTask.from(getTaskOrThrow(taskIdx));
    }

    @Transactional
    public TaskDto.ResTask create(Long campaignIdx, TaskDto.ReqTask req, AuthUserDetails authUser) {
        TaskParts taskPart = req.taskPartId() != null ? getTaskPartOrThrow(req.taskPartId()) : null;
        if (taskPart != null && !taskPart.getCampaign().getIdx().equals(campaignIdx)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "캠페인과 업무 파트가 일치하지 않습니다.");
        }

        MileStones milestone = req.milestoneId() != null ? getMilestoneOrThrow(req.milestoneId()) : null;
        User assignee = req.assigneeId() != null ? getUserOrThrow(req.assigneeId()) : null;
        CampaignParticipant participant = req.participantId() != null
                ? getParticipantOrThrow(req.participantId()) : null;

        Task entity = req.toEntity(participant, taskPart, milestone, assignee);
        // 1급화: 캠페인을 직접 연결 (taskPart 없이도 캠페인 소속이 되도록)
        campaignRepository.findById(campaignIdx).ifPresent(entity::setCampaign);
        Task saved = taskRepository.save(entity);
        User actor = findActor(authUser);
        notificationService.notifyTaskAssigned(saved, actor);
        // 활동 로그: 업무 생성
        activityService.record(resolveCampaign(saved), actor,
                "TASK_CREATE", "업무 '" + saved.getName() + "' 생성");
        sseService.broadcastCalendarRefresh(campaignIdx, "task");
        // Dashboard 캐시 무효화 (reviewQueue, summary.pending, blockers 영향)
        evictTaskDashboard(null, resolveCampaign(saved), null, assignee);
        evictTaskList(campaignIdx);
        return TaskDto.ResTask.from(saved);
    }

    @Transactional
    public TaskDto.ResTask update(Long taskIdx, TaskDto.ReqTask req, AuthUserDetails authUser) {
        Task task = getTaskOrThrow(taskIdx);
        User previousAssignee = task.getAssignee();
        Campaign previousCampaign = resolveCampaign(task);
        TaskStatus previousStatus = task.getStatus();
        String previousName = task.getName();
        CampaignParticipant previousParticipant = task.getParticipant();
        java.time.LocalDateTime previousDueDate = task.getDueDate();
        var previousTaskType = task.getTaskType();
        TaskParts previousTaskPart = task.getTaskPart();
        MileStones previousMilestone = task.getMilestone();
        var previousPriority = task.getPriority();
        String previousMemo = task.getMemo();

        TaskParts taskPart = req.taskPartId() != null ? getTaskPartOrThrow(req.taskPartId()) : task.getTaskPart();
        MileStones milestone = req.milestoneId() != null ? getMilestoneOrThrow(req.milestoneId()) : task.getMilestone();
        User assignee = req.assigneeId() != null ? getUserOrThrow(req.assigneeId()) : task.getAssignee();
        CampaignParticipant participant = req.participantId() != null
                ? getParticipantOrThrow(req.participantId()) : task.getParticipant();
        TaskStatus nextStatus = req.status() != null ? req.status() : task.getStatus();
        // startDate는 부분 수정(예: 드래그로 마감일만 변경) 시 기존 값 보존
        java.time.LocalDateTime nextStartDate = req.startDate() != null ? req.startDate() : task.getStartDate();

        task.update(
                req.name(),
                participant,
                nextStartDate,
                req.dueDate(),
                req.taskType(),
                nextStatus,
                taskPart,
                milestone,
                assignee,
                req.priority() != null ? req.priority() : task.getPriority(),
                req.memo()
        );
        User actor = findActor(authUser);
        boolean isAssigneeChanged = assignee != null && isDifferentUser(previousAssignee, assignee);
        boolean isStatusChanged = previousStatus != nextStatus;
        boolean isGeneralUpdate = hasGeneralUpdate(
                previousName,
                task.getName(),
                previousParticipant,
                task.getParticipant(),
                previousDueDate,
                task.getDueDate(),
                previousTaskType,
                task.getTaskType(),
                previousTaskPart,
                task.getTaskPart(),
                previousMilestone,
                task.getMilestone(),
                previousPriority,
                task.getPriority(),
                previousMemo,
                task.getMemo()
        );

        if (isAssigneeChanged) {
            notificationService.notifyTaskAssigned(task, actor);
        }
        notificationService.notifyTaskStatusChanged(task, previousStatus, nextStatus, actor);
        // 활동 로그: 상태 변경 (DONE 전환은 TASK_DONE, 그 외는 STATUS_CHANGE)
        if (isStatusChanged) {
            String actType = nextStatus == TaskStatus.DONE ? "TASK_DONE" : "STATUS_CHANGE";
            String desc = nextStatus == TaskStatus.DONE
                    ? "업무 '" + task.getName() + "' 완료"
                    : "업무 '" + task.getName() + "' 상태 변경 " + previousStatus + "→" + nextStatus;
            activityService.record(resolveCampaign(task), actor, actType, desc);
        }
        if (isGeneralUpdate && !isAssigneeChanged && !isStatusChanged) {
            notificationService.notifyTaskUpdated(task, actor, teamRecipients(task, actor));
        }
        Long campaignIdxForSse = task.getTaskPart() != null && task.getTaskPart().getCampaign() != null
                ? task.getTaskPart().getCampaign().getIdx() : null;
        sseService.broadcastCalendarRefresh(campaignIdxForSse, "task");
        // Dashboard 캐시 무효화 (status 변경 시 reviewQueue/blockers/passPct 모두 영향)
        Campaign nextCampaign = resolveCampaign(task);
        evictTaskDashboard(previousCampaign, nextCampaign, previousAssignee, task.getAssignee());
        evictTaskList(previousCampaign, nextCampaign);
        return TaskDto.ResTask.from(task);
    }

    @Transactional
    public void delete(Long taskIdx) {
        Task task = taskRepository.findById(taskIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "업무를 찾을 수 없습니다."));
        Long campaignIdxForSse = task.getTaskPart() != null && task.getTaskPart().getCampaign() != null
                ? task.getTaskPart().getCampaign().getIdx() : null;
        Campaign campaign = resolveCampaign(task);
        User assignee = task.getAssignee();
        taskRepository.deleteById(taskIdx);
        sseService.broadcastCalendarRefresh(campaignIdxForSse, "task");
        // Dashboard 캐시 무효화 (reviewQueue/blockers 에서 제거)
        evictTaskDashboard(campaign, null, assignee, null);
        evictTaskList(campaign);
    }

    /** 활동 로그용 캠페인 해석: 직접 campaign → 업무파트 경유 → 참여사 경유. 없으면 null(개인 업무). */
    private Campaign resolveCampaign(Task task) {
        if (task.getCampaign() != null) {
            return task.getCampaign();
        }
        if (task.getTaskPart() != null && task.getTaskPart().getCampaign() != null) {
            return task.getTaskPart().getCampaign();
        }
        if (task.getParticipant() != null && task.getParticipant().getCampaign() != null) {
            return task.getParticipant().getCampaign();
        }
        return null;
    }

    private void evictTaskDashboard(
            Campaign previousCampaign,
            Campaign nextCampaign,
            User previousAssignee,
            User nextAssignee
    ) {
        Set<Long> campaignIdxs = new LinkedHashSet<>();
        addCampaignIdx(campaignIdxs, previousCampaign);
        addCampaignIdx(campaignIdxs, nextCampaign);

        Set<Long> userIdxs = new LinkedHashSet<>();
        addUserIdx(userIdxs, previousAssignee);
        addUserIdx(userIdxs, nextAssignee);

        if (!campaignIdxs.isEmpty()) {
            dashboardCacheEvictor.evictCampaigns(campaignIdxs);
        }
        if (!userIdxs.isEmpty()) {
            dashboardCacheEvictor.evictUsers(userIdxs);
        }
    }

    private void addCampaignIdx(Set<Long> campaignIdxs, Campaign campaign) {
        if (campaign != null && campaign.getIdx() != null) {
            campaignIdxs.add(campaign.getIdx());
        }
    }

    private void addUserIdx(Set<Long> userIdxs, User user) {
        if (user != null && user.getIdx() != null) {
            userIdxs.add(user.getIdx());
        }
    }

    private void evictTaskList(Campaign... campaigns) {
        if (campaigns == null) {
            return;
        }
        for (Campaign campaign : campaigns) {
            evictTaskList(campaign == null ? null : campaign.getIdx());
        }
    }

    private void evictTaskList(Long campaignIdx) {
        if (campaignIdx == null) {
            return;
        }
        Cache cache = cacheManager.getCache(CacheNames.TASK_LIST);
        if (cache != null) {
            cache.evict(campaignIdx);
        }
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
    private User findActor(AuthUserDetails authUser) {
        if (authUser == null || authUser.getIdx() == null) {
            return null;
        }

        return userRepository.findById(authUser.getIdx()).orElse(null);
    }

    private boolean isDifferentUser(User previousAssignee, User nextAssignee) {
        Long previousIdx = previousAssignee != null ? previousAssignee.getIdx() : null;
        Long nextIdx = nextAssignee != null ? nextAssignee.getIdx() : null;
        return previousIdx == null || !previousIdx.equals(nextIdx);
    }

    private boolean hasGeneralUpdate(
            String previousName,
            String nextName,
            CampaignParticipant previousParticipant,
            CampaignParticipant nextParticipant,
            java.time.LocalDateTime previousDueDate,
            java.time.LocalDateTime nextDueDate,
            Object previousTaskType,
            Object nextTaskType,
            TaskParts previousTaskPart,
            TaskParts nextTaskPart,
            MileStones previousMilestone,
            MileStones nextMilestone,
            Object previousPriority,
            Object nextPriority,
            String previousMemo,
            String nextMemo
    ) {
        return !Objects.equals(previousName, nextName)
                || !sameEntity(previousParticipant, nextParticipant)
                || !Objects.equals(previousDueDate, nextDueDate)
                || !Objects.equals(previousTaskType, nextTaskType)
                || !sameEntity(previousTaskPart, nextTaskPart)
                || !sameEntity(previousMilestone, nextMilestone)
                || !Objects.equals(previousPriority, nextPriority)
                || !Objects.equals(previousMemo, nextMemo);
    }

    private boolean sameEntity(Object previous, Object next) {
        Long previousIdx = entityIdx(previous);
        Long nextIdx = entityIdx(next);

        return Objects.equals(previousIdx, nextIdx);
    }

    private Long entityIdx(Object entity) {
        if (entity instanceof CampaignParticipant participant) {
            return participant.getIdx();
        }
        if (entity instanceof TaskParts taskParts) {
            return taskParts.getIdx();
        }
        if (entity instanceof MileStones mileStones) {
            return mileStones.getIdx();
        }

        return null;
    }

    private List<User> teamRecipients(Task task, User actor) {
        Long campaignIdx = task.getTaskPart() != null && task.getTaskPart().getCampaign() != null
                ? task.getTaskPart().getCampaign().getIdx()
                : null;
        if (campaignIdx == null) {
            return List.of();
        }

        Long participantOrganizationIdx = task.getParticipant() != null
                && task.getParticipant().getOrganization() != null
                ? task.getParticipant().getOrganization().getIdx()
                : null;
        Long actorIdx = actor != null ? actor.getIdx() : null;

        return campaignMemberRepository.findAllByCampaignIdx(campaignIdx).stream()
                .filter(member -> shouldNotifyTaskUpdateMember(member, participantOrganizationIdx))
                .map(CampaignMember::getUser)
                .filter(user -> user != null && user.getIdx() != null && !user.getIdx().equals(actorIdx))
                .distinct()
                .toList();
    }

    private boolean shouldNotifyTaskUpdateMember(CampaignMember member, Long participantOrganizationIdx) {
        if (member.getCampaignRole() == CampaignMemberRole.MANAGER
                || member.getCampaignRole() == CampaignMemberRole.GENERAL_MANAGER) {
            return true;
        }

        return participantOrganizationIdx != null
                && member.getUser() != null
                && member.getUser().getOrganization() != null
                && participantOrganizationIdx.equals(member.getUser().getOrganization().getIdx());
    }
}
