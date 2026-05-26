package org.example.backend.adcheck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.adcheck.analysis.service.AdCheckAnalysisMongoStorageService;
import org.example.backend.adcheck.model.AdCheckDto;
import org.example.backend.adcheck.model.AdCheckJob;
import org.example.backend.adcheck.model.AdCheckJobDto;
import org.example.backend.adcheck.model.AdCheckOutboxEvent;
import org.example.backend.adcheck.model.AdCheckJobStatus;
import org.example.backend.adcheck.model.AdCheckJobStep;
import org.example.backend.adcheck.repository.AdCheckJobRepository;
import org.example.backend.adcheck.repository.AdCheckOutboxEventRepository;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.common.security.CampaignMemberGuard;
import org.example.backend.notification.service.NotificationService;
import org.example.backend.notification.service.NotificationSseService;
import org.example.backend.user.model.AuthUserDetails;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdCheckJobService {
    private static final String CONTEXT_JOB_ID = "adCheckJobId";
    private static final String CONTEXT_PROGRESS_TOKEN = "adCheckProgressToken";
    private static final String CONTEXT_PROGRESS_CALLBACK_URL = "adCheckProgressCallbackUrl";
    private static final String CONTEXT_MODE = "adCheckMode";
    private static final String CONTEXT_MODE_DIRECT = "direct-aijudge";
    private static final List<AdCheckJobStatus> ACTIVE_STATUSES = List.of(
            AdCheckJobStatus.RUNNING,
            AdCheckJobStatus.QUEUED
    );
    private static final Set<AdCheckJobStep> CALLBACK_STEPS = Set.of(
            AdCheckJobStep.DATA_EXTRACTION,
            AdCheckJobStep.DATA_ANALYSIS,
            AdCheckJobStep.RESULT_BUILDING
    );
    private static final int SUMMARY_SAVE_RETRY_COUNT = 3;

    private final AdCheckJobRepository adCheckJobRepository;
    private final UserRepository userRepository;
    private final AdCheckService adCheckService;
    private final AdCheckAnalysisMongoStorageService adCheckAnalysisMongoStorageService;
    private final AdCheckFileStorageService adCheckFileStorageService;
    private final AdCheckOutboxEventRepository adCheckOutboxEventRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationSseService notificationSseService;
    private final TransactionTemplate transactionTemplate;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final Set<String> queuedJobIds = ConcurrentHashMap.newKeySet();

    @Value("${app.ad-check.progress-callback-url:http://localhost:8083/ad/check/jobs/internal/progress}")
    private String progressCallbackUrl;

    private volatile boolean acceptingJobs = true;
    private Thread workerThread;

    @PostConstruct
    public void startWorker() {
        recoverActiveJobs();
        workerThread = new Thread(this::workerLoop, "ad-check-job-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Scheduled(fixedDelay = 5000L)
    public void requeueWaitingJobs() {
        ensureWorkerRunning();

        List<String> waitingJobIds = transactionTemplate.execute(status -> adCheckJobRepository
                .findAllByStatusInOrderByCreatedAtAsc(List.of(AdCheckJobStatus.QUEUED))
                .stream()
                .filter(this::hasStoredFileBytes)
                .map(AdCheckJob::getJobId)
                .toList());

        if (waitingJobIds == null || waitingJobIds.isEmpty()) {
            return;
        }

        waitingJobIds.forEach(this::enqueueJob);
    }

    @PreDestroy
    public void stopWorker() {
        acceptingJobs = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    public AdCheckJobDto.JobRes createJob(MultipartFile file, AuthUserDetails requester, String campaignId) {
        if (requester == null || requester.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 사용자 정보가 필요합니다.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검수 파일이 필요합니다.");
        }

        byte[] fileBytes = readFileBytes(file);
        User requesterEntity = userRepository.findByIdx(requester.getIdx())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));
        String jobId = "ai-judge-" + UUID.randomUUID();
        String progressToken = UUID.randomUUID().toString();

        AdCheckJobDto.JobRes createdJob = transactionTemplate.execute(status -> {
            AdCheckJob job = AdCheckJob.queued(
                    jobId,
                    progressToken,
                    requesterEntity,
                    campaignId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    fileBytes
            );
            AdCheckJob savedJob = adCheckJobRepository.save(job);
            return toDto(savedJob);
        });

        emitJobEvent(createdJob, "ad-check.job.created");
        AdCheckJobDto.JobRes queuedJob = updateJob(jobId, AdCheckJob::markQueued);
        emitJobEvent(queuedJob, "ad-check.job.updated");
        enqueueJob(jobId);
        return queuedJob;
    }

    public AdCheckJobDto.DirectJobRes createDirectJob(
            AdCheckJobDto.DirectJobCreateReq request,
            AuthUserDetails requester
    ) {
        Long requesterIdx = requesterIdx(requester);
        User requesterEntity = userRepository.findByIdx(requesterIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "?ъ슜???뺣낫瑜?李얠쓣 ???놁뒿?덈떎."));

        String campaignId = request == null ? "" : firstText(request.campaignId());
        if (!campaignId.isBlank()) {
            requireCampaignAccess(campaignId, requesterIdx);
        }

        String jobId = "ai-judge-" + UUID.randomUUID();
        String progressToken = UUID.randomUUID().toString();
        String fileName = firstText(request == null ? null : request.fileName(), "upload");
        String fileContentType = firstText(request == null ? null : request.fileContentType());
        Long fileSize = request == null ? null : request.fileSize();

        AdCheckJobDto.JobRes createdJob = transactionTemplate.execute(status -> {
            AdCheckJob job = AdCheckJob.queued(
                    jobId,
                    progressToken,
                    requesterEntity,
                    campaignId,
                    fileName,
                    fileContentType,
                    fileSize,
                    null
            );
            job.markRunning();
            AdCheckJob savedJob = adCheckJobRepository.save(job);
            return toDto(savedJob);
        });

        emitJobEvent(createdJob, "ad-check.job.created");
        emitJobEvent(createdJob, "ad-check.step.changed");
        return new AdCheckJobDto.DirectJobRes(createdJob, directContext(createdJob));
    }

    public AdCheckJobDto.JobRes updateProgress(AdCheckJobDto.ProgressReq request) {
        if (request == null
                || request.jobId() == null || request.jobId().isBlank()
                || request.token() == null || request.token().isBlank()
                || request.step() == null || request.step().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ad check job progress request is invalid.");
        }

        AdCheckJobStep step = parseCallbackStep(request.step());
        AdCheckJobDto.JobRes updatedJob = transactionTemplate.execute(status -> {
            AdCheckJob job = adCheckJobRepository.findByJobId(request.jobId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ad check job not found."));

            if (!request.token().equals(job.getProgressToken())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ad check job progress token is invalid.");
            }
            if (job.getStatus() != AdCheckJobStatus.RUNNING) {
                return toDto(job);
            }
            if (job.getCurrentStep() != null && step.getOrder() < job.getCurrentStep().getOrder()) {
                return toDto(job);
            }

            job.changeStep(step);
            return toDto(job);
        });

        emitJobEvent(updatedJob, "ad-check.step.changed");
        return updatedJob;
    }

    public AdCheckJobDto.JobRes applyDirectAiJudgeEvent(AdCheckDto.FileCheckRes response, boolean failed) {
        if (response == null || !CONTEXT_MODE_DIRECT.equals(textValue(response.getContext(), CONTEXT_MODE))) {
            return null;
        }

        String jobId = textValue(response.getContext(), CONTEXT_JOB_ID);
        if (jobId.isBlank()) {
            return null;
        }

        String resultPayload = serializeResult(response);
        String errorMessage = failed
                ? firstText(response.getErrorMessage(), "AI judge processing failed.")
                : null;

        AdCheckJobDto.JobRes updatedJob = executeWithRetry(jobId, "direct ai-judge event save", () ->
                transactionTemplate.execute(status -> {
                    AdCheckJob job = adCheckJobRepository.findByJobId(jobId).orElse(null);
                    if (job == null) {
                        log.warn("Direct ai-judge event has no matching ad check job. jobId={}, analysisJobId={}",
                                jobId, response.getAnalysisJobId());
                        return null;
                    }
                    if (job.isTerminal()) {
                        return null;
                    }

                    if (failed) {
                        job.markFailed(
                                errorMessage,
                                resultPayload,
                                response.getAnalysisJobId(),
                                normalizeResultStatus(firstText(response.getStatus(), "failed")),
                                resolveRiskLevel(response, errorMessage),
                                resolveSummaryMessage(response, errorMessage)
                        );
                        saveOutboxEvents(job, response, "ad-check.summary-updated");
                    } else {
                        job.markSucceeded(
                                resultPayload,
                                response.getAnalysisJobId(),
                                normalizeResultStatus(response.getStatus()),
                                resolveRiskLevel(response, null),
                                resolveSummaryMessage(response, null)
                        );
                        saveOutboxEvents(job, response, "ad-check.detail-saved", "ad-check.summary-created", "ad-check.result-ready");
                    }

                    return toDto(job);
                })
        );

        if (failed) {
            emitJobEvent(updatedJob, "ad-check.failed");
            notifyFailedJob(updatedJob);
        } else {
            emitJobEvent(updatedJob, "ad-check.completed");
            notifyCompletedJob(updatedJob, response);
        }
        return updatedJob;
    }

    public AdCheckJobDto.JobRes getJob(String jobId, AuthUserDetails requester) {
        return transactionTemplate.execute(status -> toDto(findAccessibleJob(jobId, requester)));
    }

    public AdCheckJobDto.JobSummaryRes getJobSummary(String jobId, AuthUserDetails requester) {
        return transactionTemplate.execute(status -> toSummaryDto(findAccessibleJob(jobId, requester)));
    }

    public List<AdCheckJobDto.JobSummaryRes> listJobs(AuthUserDetails requester, String campaignId) {
        Long requesterIdx = requesterIdx(requester);
        return transactionTemplate.execute(status -> {
            List<AdCheckJob> jobs;
            if (campaignId == null || campaignId.isBlank()) {
                jobs = adCheckJobRepository.findAllByRequester_IdxOrderByCreatedAtDesc(requesterIdx);
            } else {
                Campaign campaign = requireCampaignAccess(campaignId, requesterIdx);
                jobs = adCheckJobRepository.findAllByCampaignIdInOrderByCreatedAtDesc(campaignIdsOf(campaign));
            }
            return jobs.stream()
                    .filter(job -> !job.isDeleted())
                    .map(this::toSummaryDto)
                    .toList();
        });
    }

    public AdCheckJobDto.JobDetailRes getJobDetail(String jobId, AuthUserDetails requester) {
        AdCheckJobDto.JobSummaryRes summary = transactionTemplate.execute(status -> toSummaryDto(findAccessibleJob(jobId, requester)));
        AdCheckDto.FileCheckRes detail = findMongoDetail(summary, jobId)
                .map(this::refreshStorageUrls)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상세 자료를 불러오지 못했습니다."));
        Map<String, Object> rawDocument = findRawMongoDocument(summary, jobId).orElse(Map.of());
        String documentId = firstText(summary.mongoDocumentId(), detail.getAnalysisJobId(), jobId);
        return new AdCheckJobDto.JobDetailRes(summary, documentId, detail, rawDocument);
    }

    public List<AdCheckJobDto.JobRes> getActiveJobs(AuthUserDetails requester) {
        Long requesterIdx = requesterIdx(requester);
        return transactionTemplate.execute(status -> adCheckJobRepository
                .findAllByRequester_IdxAndStatusInOrderByCreatedAtDesc(requesterIdx, ACTIVE_STATUSES)
                .stream()
                .filter(job -> !job.isDeleted())
                .map(this::toDto)
                .toList());
    }

    public AdCheckJobDto.JobRes cancelJob(String jobId, AuthUserDetails requester) {
        Long requesterIdx = requesterIdx(requester);
        AdCheckJobDto.JobRes canceledJob = transactionTemplate.execute(status -> {
            AdCheckJob job = adCheckJobRepository.findByJobIdAndRequester_Idx(jobId, requesterIdx)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "검수 작업을 찾을 수 없습니다."));

            if (job.getStatus() != AdCheckJobStatus.QUEUED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대기 중인 검수 작업만 취소할 수 있습니다.");
            }

            job.cancel();
            return toDto(job);
        });

        emitJobEvent(canceledJob, "ad-check.job.updated");
        return canceledJob;
    }

    public void deleteJob(String jobId, AuthUserDetails requester) {
        Long requesterIdx = requesterIdx(requester);
        AdCheckJobDto.JobRes deletedJob = transactionTemplate.execute(status -> {
            AdCheckJob job = adCheckJobRepository.findByJobIdAndRequester_Idx(jobId, requesterIdx)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ad check job not found."));

            if (job.isDeleted()) {
                return toDto(job);
            }
            if (!job.isTerminal()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "active ad check job cannot be deleted.");
            }

            job.delete(requester.getId());
            return toDto(job);
        });

        emitJobEvent(deletedJob, "ad-check.job.updated");
    }

    private void recoverActiveJobs() {
        List<String> jobIds = transactionTemplate.execute(status -> adCheckJobRepository
                .findAllByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES)
                .stream()
                .peek(job -> {
                    if (job.getStatus() == AdCheckJobStatus.RUNNING && hasStoredFileBytes(job)) {
                        job.markQueued();
                    }
                })
                .filter(this::hasStoredFileBytes)
                .map(AdCheckJob::getJobId)
                .toList());

        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }

        jobIds.forEach(this::enqueueJob);
    }

    private boolean hasStoredFileBytes(AdCheckJob job) {
        return job != null && job.getFileBytes() != null && job.getFileBytes().length > 0;
    }

    private void workerLoop() {
        while (acceptingJobs) {
            String jobId = null;
            try {
                jobId = queue.take();
                queuedJobIds.remove(jobId);
                processJob(jobId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Ad check job worker failed. jobId={}", jobId, e);
                failActiveJob(jobId, e);
            }
        }
    }

    private void processJob(String jobId) {
        WorkItem workItem = loadWorkItem(jobId);
        if (workItem == null) {
            failMissingWorkItem(jobId);
            return;
        }

        AdCheckJobDto.JobRes runningJob = updateJob(jobId, AdCheckJob::markRunning);
        if (runningJob == null || !"RUNNING".equals(runningJob.status())) {
            return;
        }
        emitJobEvent(runningJob, "ad-check.step.changed");

        StoredMultipartFile multipartFile = new StoredMultipartFile(
                workItem.fileName(),
                workItem.contentType(),
                workItem.fileBytes()
        );

        try {
            AdCheckDto.FileCheckRes result = adCheckService.checkFileWithAiJudge(
                    multipartFile,
                    workItem.requester(),
                    workItem.campaignId(),
                    progressContext(workItem)
            );
            completeJob(jobId, result);
        } catch (Exception e) {
            failJob(jobId, e);
        }
    }

    private void completeJob(String jobId, AdCheckDto.FileCheckRes result) {
        String mongoDocumentId = adCheckAnalysisMongoStorageService.saveDetailOrThrow(result);
        AdCheckJobDto.JobRes completedJob = executeWithRetry(jobId, "ad check summary save", () -> transactionTemplate.execute(status -> {
            AdCheckJob job = adCheckJobRepository.findByJobId(jobId).orElse(null);
            if (job == null) {
                return null;
            }
            job.markSucceeded(
                    null,
                    mongoDocumentId,
                    normalizeResultStatus(result == null ? null : result.getStatus()),
                    resolveRiskLevel(result, null),
                    resolveSummaryMessage(result, null)
            );
            saveOutboxEvents(job, result, "ad-check.detail-saved", "ad-check.summary-created", "ad-check.result-ready");
            return toDto(job);
        }));
        emitJobEvent(completedJob, "ad-check.completed");
        notifyCompletedJob(completedJob, result);
    }

    private void failJob(String jobId, Throwable error) {
        AdCheckDto.FileCheckRes partialResponse = null;
        if (error instanceof AdCheckService.FileCheckException fileCheckException) {
            partialResponse = fileCheckException.getResponse();
        }

        String errorMessage = firstText(
                partialResponse == null ? null : partialResponse.getErrorMessage(),
                error == null ? null : error.getMessage(),
                "검수 중 오류가 발생했습니다."
        );
        String mongoDocumentId = null;
        if (partialResponse != null && partialResponse.getAnalysisJobId() != null && !partialResponse.getAnalysisJobId().isBlank()) {
            try {
                mongoDocumentId = adCheckAnalysisMongoStorageService.saveDetailOrThrow(partialResponse);
            } catch (Exception e) {
                log.warn("Ad check failed detail MongoDB save failed. jobId={}", jobId, e);
            }
        }
        String finalMongoDocumentId = mongoDocumentId;
        AdCheckDto.FileCheckRes finalPartialResponse = partialResponse;
        AdCheckJobDto.JobRes failedJob = transactionTemplate.execute(status -> {
            AdCheckJob job = adCheckJobRepository.findByJobId(jobId).orElse(null);
            if (job == null) {
                return null;
            }
            job.markFailed(
                    errorMessage,
                    null,
                    finalMongoDocumentId,
                    normalizeResultStatus(finalPartialResponse == null ? "failed" : finalPartialResponse.getStatus()),
                    resolveRiskLevel(finalPartialResponse, errorMessage),
                    resolveSummaryMessage(finalPartialResponse, errorMessage)
            );
            saveOutboxEvents(job, finalPartialResponse, "ad-check.summary-updated");
            return toDto(job);
        });
        emitJobEvent(failedJob, "ad-check.failed");
        notifyFailedJob(failedJob);
    }

    private WorkItem loadWorkItem(String jobId) {
        return transactionTemplate.execute(status -> adCheckJobRepository.findByJobId(jobId)
                .filter(job -> !job.isTerminal())
                .filter(job -> job.getFileBytes() != null && job.getFileBytes().length > 0)
                .map(job -> new WorkItem(
                        job.getJobId(),
                        job.getProgressToken(),
                        job.getCampaignId(),
                        job.getFileName(),
                        job.getFileContentType(),
                        job.getFileBytes(),
                        AuthUserDetails.from(job.getRequester())
                ))
                .orElse(null));
    }

    private Map<String, Object> progressContext(WorkItem workItem) {
        return progressContext(workItem.jobId(), workItem.progressToken());
    }

    private Map<String, Object> progressContext(String jobId, String progressToken) {
        Map<String, Object> context = new HashMap<>();
        context.put(CONTEXT_JOB_ID, jobId);
        context.put(CONTEXT_PROGRESS_TOKEN, progressToken);
        if (progressCallbackUrl != null && !progressCallbackUrl.isBlank()) {
            context.put(CONTEXT_PROGRESS_CALLBACK_URL, progressCallbackUrl.trim());
        }
        return context;
    }

    private Map<String, Object> directContext(AdCheckJobDto.JobRes job) {
        Map<String, Object> context = progressContext(job.jobId(), findProgressToken(job.jobId()));
        context.put(CONTEXT_MODE, CONTEXT_MODE_DIRECT);
        if (job.campaignId() != null && !job.campaignId().isBlank()) {
            context.put("campaignId", job.campaignId());
        }
        if (job.requesterId() != null) {
            context.put("requesterUserIdx", job.requesterId());
        }
        if (job.requesterLoginId() != null && !job.requesterLoginId().isBlank()) {
            context.put("requesterLoginId", job.requesterLoginId());
        }
        if (job.requesterName() != null && !job.requesterName().isBlank()) {
            context.put("requesterName", job.requesterName());
        }
        return context;
    }

    private String findProgressToken(String jobId) {
        return transactionTemplate.execute(status -> adCheckJobRepository.findByJobId(jobId)
                .map(AdCheckJob::getProgressToken)
                .orElse(jobId));
    }

    private AdCheckJobStep parseCallbackStep(String rawStep) {
        try {
            AdCheckJobStep step = AdCheckJobStep.valueOf(rawStep.trim());
            if (CALLBACK_STEPS.contains(step)) {
                return step;
            }
        } catch (IllegalArgumentException ignored) {
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ad check job progress step is invalid.");
    }

    private AdCheckJobDto.JobRes updateJob(String jobId, JobUpdater updater) {
        return transactionTemplate.execute(status -> {
            AdCheckJob job = adCheckJobRepository.findByJobId(jobId).orElse(null);
            if (job == null) {
                return null;
            }
            updater.update(job);
            return toDto(job);
        });
    }

    private void enqueueJob(String jobId) {
        if (!acceptingJobs || jobId == null || jobId.isBlank()) {
            return;
        }
        if (!queuedJobIds.add(jobId)) {
            return;
        }
        queue.offer(jobId);
    }

    private void ensureWorkerRunning() {
        if (!acceptingJobs || (workerThread != null && workerThread.isAlive())) {
            return;
        }

        workerThread = new Thread(this::workerLoop, "ad-check-job-worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void failActiveJob(String jobId, Throwable error) {
        if (jobId == null || jobId.isBlank()) {
            return;
        }

        AdCheckJobDto.JobRes failedJob = transactionTemplate.execute(status -> adCheckJobRepository.findByJobId(jobId)
                .filter(job -> !job.isTerminal())
                .map(job -> {
                    String errorMessage = firstText(
                            error == null ? null : error.getMessage(),
                            "검수 작업 처리 중 오류가 발생했습니다."
                    );
                    job.markFailed(
                            errorMessage,
                            null,
                            null,
                            "failed",
                            "HIGH",
                            errorMessage
                    );
                    return toDto(job);
                })
                .orElse(null));

        emitJobEvent(failedJob, "ad-check.failed");
        notifyFailedJob(failedJob);
    }

    private void failMissingWorkItem(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return;
        }

        AdCheckJobDto.JobRes failedJob = transactionTemplate.execute(status -> adCheckJobRepository.findByJobId(jobId)
                .filter(job -> !job.isTerminal())
                .filter(job -> job.getFileBytes() == null || job.getFileBytes().length == 0)
                .map(job -> {
                    String errorMessage = "검수 파일 데이터가 없어 작업을 시작할 수 없습니다.";
                    job.markFailed(
                            errorMessage,
                            null,
                            null,
                            "failed",
                            "HIGH",
                            errorMessage
                    );
                    return toDto(job);
                })
                .orElse(null));

        emitJobEvent(failedJob, "ad-check.failed");
        notifyFailedJob(failedJob);
    }

    private void emitJobEvent(AdCheckJobDto.JobRes job, String eventName) {
        if (job == null || job.requesterId() == null) {
            return;
        }
        notificationSseService.sendToUser(job.requesterId(), eventName, job);
    }

    private void notifyCompletedJob(AdCheckJobDto.JobRes job, AdCheckDto.FileCheckRes result) {
        if (job == null || job.requesterId() == null || result == null) {
            return;
        }
        notificationService.notifyAiJudgeResult(job.requesterId(), result, jobTargetUrl(job));
    }

    private void notifyFailedJob(AdCheckJobDto.JobRes job) {
        if (job == null || job.requesterId() == null) {
            return;
        }
        notificationService.notifyAiJudgeJobFailure(
                job.requesterId(),
                job.jobId(),
                job.fileName(),
                firstText(job.errorMessage(), job.summaryMessage(), "AI 검수 처리 중 오류가 발생했습니다."),
                jobTargetUrl(job)
        );
    }

    private String jobTargetUrl(AdCheckJobDto.JobRes job) {
        if (job == null) {
            return "/campaign-folder";
        }
        if (job.campaignId() != null && !job.campaignId().isBlank()) {
            return "/campaigns/" + job.campaignId() + "?tab=review&adCheckJobId=" + job.jobId();
        }
        return "/campaign-folder";
    }

    private AdCheckJobDto.JobSummaryRes toSummaryDto(AdCheckJob job) {
        AdCheckDto.FileCheckRes result = parseResultPayload(job.getResultPayload());
        AdCheckJobDto.JobSummaryRes summary = AdCheckJobDto.JobSummaryRes.from(job, objectMapper);
        return refreshSummaryStorageUrls(summary, result);
    }

    private AdCheckJobDto.JobRes toDto(AdCheckJob job) {
        return AdCheckJobDto.JobRes.from(job, objectMapper);
    }

    private AdCheckDto.FileCheckRes parseResultPayload(String resultPayload) {
        if (resultPayload == null || resultPayload.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(resultPayload, AdCheckDto.FileCheckRes.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private AdCheckJobDto.JobSummaryRes refreshSummaryStorageUrls(
            AdCheckJobDto.JobSummaryRes summary,
            AdCheckDto.FileCheckRes result
    ) {
        if (summary == null || result == null) {
            return summary;
        }

        String fileUrl = createFreshViewUrl(
                result.getFileObjectKey(),
                result.getFileContentType(),
                summary.fileUrl()
        );
        String thumbnailUrl = result.getFileContentType() != null
                && result.getFileContentType().startsWith("image/")
                ? fileUrl
                : refreshedFirstImageUrl(result.getExtractedImageAssets(), summary.thumbnailUrl());

        return summary.withStorageUrls(fileUrl, thumbnailUrl);
    }

    private AdCheckDto.FileCheckRes refreshStorageUrls(AdCheckDto.FileCheckRes detail) {
        if (detail == null) {
            return null;
        }

        List<AdCheckDto.FileArtifact> imageAssets = detail.getExtractedImageAssets() == null
                ? List.of()
                : detail.getExtractedImageAssets().stream()
                        .map(this::refreshImageArtifactUrl)
                        .toList();

        return detail.toBuilder()
                .fileUrl(createFreshViewUrl(
                        detail.getFileObjectKey(),
                        detail.getFileContentType(),
                        detail.getFileUrl()
                ))
                .extractedImageAssets(imageAssets)
                .build();
    }

    private AdCheckDto.FileArtifact refreshImageArtifactUrl(AdCheckDto.FileArtifact artifact) {
        if (artifact == null) {
            return null;
        }

        return AdCheckDto.FileArtifact.builder()
                .type(artifact.getType())
                .targetId(artifact.getTargetId())
                .page(artifact.getPage())
                .readingOrder(artifact.getReadingOrder())
                .objectKey(artifact.getObjectKey())
                .url(createFreshViewUrl(artifact.getObjectKey(), artifact.getContentType(), artifact.getUrl()))
                .contentType(artifact.getContentType())
                .fileSize(artifact.getFileSize())
                .build();
    }

    private String refreshedFirstImageUrl(List<AdCheckDto.FileArtifact> imageAssets, String fallbackUrl) {
        if (imageAssets == null || imageAssets.isEmpty()) {
            return fallbackUrl;
        }

        return imageAssets.stream()
                .filter(artifact -> artifact != null)
                .map(artifact -> createFreshViewUrl(
                        artifact.getObjectKey(),
                        artifact.getContentType(),
                        artifact.getUrl()
                ))
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(fallbackUrl);
    }

    private String createFreshViewUrl(String objectKey, String contentType, String fallbackUrl) {
        if (objectKey == null || objectKey.isBlank() || !adCheckFileStorageService.isAdCheckFileKey(objectKey)) {
            return fallbackUrl;
        }

        try {
            return adCheckFileStorageService.createViewUrl(objectKey, contentType);
        } catch (RuntimeException e) {
            log.warn("Ad check file view URL refresh failed. objectKey={}", objectKey, e);
            return fallbackUrl;
        }
    }

    private Long requesterIdx(AuthUserDetails requester) {
        if (requester == null || requester.getIdx() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 사용자 정보가 필요합니다.");
        }
        return requester.getIdx();
    }

    private AdCheckJob findAccessibleJob(String jobId, AuthUserDetails requester) {
        Long requesterIdx = requesterIdx(requester);
        AdCheckJob job = adCheckJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "검수 작업을 찾을 수 없습니다."));

        if (job.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "검수 작업을 찾을 수 없습니다.");
        }
        if (job.getRequester() != null && requesterIdx.equals(job.getRequester().getIdx())) {
            return job;
        }
        if (job.getCampaignId() != null && !job.getCampaignId().isBlank()) {
            requireCampaignAccess(job.getCampaignId(), requesterIdx);
            return job;
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "검수 작업을 찾을 수 없습니다.");
    }

    private Campaign requireCampaignAccess(String campaignId, Long requesterIdx) {
        Campaign campaign = resolveCampaign(campaignId);
        CampaignMemberGuard.requireMember(campaignMemberRepository
                .findByCampaignIdxAndUserIdx(campaign.getIdx(), requesterIdx)
                .orElse(null));
        return campaign;
    }

    private Campaign resolveCampaign(String campaignId) {
        String normalized = campaignId == null ? null : campaignId.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "campaignId is required.");
        }

        Optional<Campaign> byPublicId = campaignRepository.findByPublicId(normalized);
        if (byPublicId.isPresent()) {
            return byPublicId.get();
        }

        try {
            return campaignRepository.findById(Long.parseLong(normalized))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found."));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found.");
        }
    }

    private List<String> campaignIdsOf(Campaign campaign) {
        LinkedHashSet<String> campaignIds = new LinkedHashSet<>();
        if (campaign.getPublicId() != null && !campaign.getPublicId().isBlank()) {
            campaignIds.add(campaign.getPublicId());
        }
        if (campaign.getIdx() != null) {
            campaignIds.add(String.valueOf(campaign.getIdx()));
        }
        return campaignIds.stream().toList();
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검수 파일을 읽을 수 없습니다.", e);
        }
    }

    private String serializeResult(AdCheckDto.FileCheckRes result) {
        if (result == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Ad check job result serialization failed.", e);
            return null;
        }
    }

    private <T> T executeWithRetry(String jobId, String action, Supplier<T> supplier) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= SUMMARY_SAVE_RETRY_COUNT; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                lastError = e;
                log.warn("Ad check {} failed. jobId={}, attempt={}/{}",
                        action, jobId, attempt, SUMMARY_SAVE_RETRY_COUNT, e);
                sleepBeforeRetry(attempt);
            }
        }

        throw lastError == null
                ? new IllegalStateException("Ad check " + action + " failed.")
                : lastError;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Math.min(1000L, 150L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ad check retry interrupted.", e);
        }
    }

    private String normalizeResultStatus(String status) {
        String normalized = firstText(status);
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.trim().toLowerCase();
    }

    private String resolveRiskLevel(AdCheckDto.FileCheckRes result, String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            return "HIGH";
        }

        Integer verdictLevel = result == null ? null : result.getVerdictLevel();
        if (verdictLevel != null && verdictLevel >= 1 && verdictLevel <= 5) {
            return String.valueOf(verdictLevel);
        }

        String status = normalizeResultStatus(result == null ? null : result.getStatus());
        if ("pass".equals(status)) {
            return "LOW";
        }
        if ("warning".equals(status)) {
            return "MEDIUM";
        }
        if ("violation".equals(status) || "failed".equals(status)) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private String resolveSummaryMessage(AdCheckDto.FileCheckRes result, String errorMessage) {
        if (errorMessage != null && !errorMessage.isBlank()) {
            return errorMessage.trim();
        }

        String status = normalizeResultStatus(result == null ? null : result.getStatus());
        if ("pass".equals(status)) {
            return "AI 검수가 완료되었습니다.";
        }
        if ("warning".equals(status) || "violation".equals(status)) {
            return firstText(
                    result == null ? null : result.getReason(),
                    result == null ? null : result.getViolationText(),
                    "AI 검수 결과 확인이 필요합니다."
            );
        }
        return "AI 검수 결과를 확인해 주세요.";
    }

    private Optional<AdCheckDto.FileCheckRes> findMongoDetail(AdCheckJobDto.JobSummaryRes summary, String jobId) {
        String mongoDocumentId = summary == null ? null : summary.mongoDocumentId();
        Optional<AdCheckDto.FileCheckRes> byDocumentId = adCheckAnalysisMongoStorageService.findDetail(mongoDocumentId);
        if (byDocumentId.isPresent()) {
            return byDocumentId;
        }
        return adCheckAnalysisMongoStorageService.findDetailByJobId(jobId);
    }

    private Optional<Map<String, Object>> findRawMongoDocument(AdCheckJobDto.JobSummaryRes summary, String jobId) {
        String mongoDocumentId = summary == null ? null : summary.mongoDocumentId();
        Optional<Map<String, Object>> byDocumentId = adCheckAnalysisMongoStorageService.findRawDocument(mongoDocumentId);
        if (byDocumentId.isPresent()) {
            return byDocumentId;
        }
        return adCheckAnalysisMongoStorageService.findRawDocumentByJobId(jobId);
    }

    private void saveOutboxEvents(AdCheckJob job, AdCheckDto.FileCheckRes result, String... eventTypes) {
        if (job == null || eventTypes == null || eventTypes.length == 0) {
            return;
        }

        for (String eventType : eventTypes) {
            if (eventType == null || eventType.isBlank()) {
                continue;
            }
            adCheckOutboxEventRepository.save(AdCheckOutboxEvent.pending(
                    job.getJobId(),
                    eventType,
                    serializeOutboxPayload(job, result, eventType)
            ));
        }
    }

    private String serializeOutboxPayload(AdCheckJob job, AdCheckDto.FileCheckRes result, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", eventType);
        payload.put("jobId", job.getJobId());
        payload.put("campaignId", job.getCampaignId());
        payload.put("requesterId", job.getRequester() == null ? null : job.getRequester().getIdx());
        payload.put("fileName", job.getFileName());
        payload.put("status", job.getStatus() == null ? null : job.getStatus().name());
        payload.put("resultStatus", job.getResultStatus());
        payload.put("riskLevel", job.getRiskLevel());
        payload.put("verdictLevel", result == null ? null : result.getVerdictLevel());
        payload.put("summaryMessage", job.getSummaryMessage());
        payload.put("mongoDocumentId", job.getMongoDocumentId());
        payload.put("analysisJobId", firstText(
                job.getMongoDocumentId(),
                result == null ? null : result.getAnalysisJobId()
        ));
        payload.put("errorMessage", job.getErrorMessage());
        payload.put("createdAt", job.getCreatedAt());
        payload.put("finishedAt", job.getFinishedAt());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Ad check outbox payload serialization failed. jobId={}, eventType={}",
                    job.getJobId(), eventType, e);
            return "{}";
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String textValue(Map<String, Object> values, String key) {
        if (values == null || key == null || !values.containsKey(key)) {
            return "";
        }
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    @FunctionalInterface
    private interface JobUpdater {
        void update(AdCheckJob job);
    }

    private record WorkItem(
            String jobId,
            String progressToken,
            String campaignId,
            String fileName,
            String contentType,
            byte[] fileBytes,
            AuthUserDetails requester
    ) {
    }

    private static class StoredMultipartFile implements MultipartFile {
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private StoredMultipartFile(String originalFilename, String contentType, byte[] bytes) {
            this.originalFilename = originalFilename == null || originalFilename.isBlank() ? "upload" : originalFilename;
            this.contentType = contentType;
            this.bytes = bytes == null ? new byte[0] : bytes;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            org.springframework.util.FileCopyUtils.copy(bytes, dest);
        }
    }
}
