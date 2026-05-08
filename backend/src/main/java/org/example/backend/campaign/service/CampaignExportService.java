package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignKpi;
import org.example.backend.campaign.model.CampaignKpiDto;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.repository.CampaignKpiRepository;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.repository.TaskRepository;
import org.example.backend.user.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignExportService {

    private final CampaignRepository campaignRepository;
    private final TaskRepository taskRepository;
    private final CampaignKpiRepository campaignKpiRepository;
    private final CampaignMemberRepository campaignMemberRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 캠페인 데이터를 CSV 형식으로 내보냄. sections에 포함된 섹션만 출력. */
    public byte[] exportCsv(Long campaignId, Set<String> sections) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM (Excel 한글 깨짐 방지)
        sb.append('﻿');

        if (sections.contains("campaign")) {
            writeCampaignSection(sb, campaign);
            sb.append("\r\n");
        }
        if (sections.contains("members")) {
            writeMembersSection(sb, campaignId);
            sb.append("\r\n");
        }
        if (sections.contains("tasks")) {
            writeTasksSection(sb, campaignId);
            sb.append("\r\n");
        }
        if (sections.contains("kpi")) {
            writeKpiSection(sb, campaignId, false);
            sb.append("\r\n");
        }
        if (sections.contains("esg")) {
            writeKpiSection(sb, campaignId, true);
            sb.append("\r\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private void writeCampaignSection(StringBuilder sb, Campaign c) {
        sb.append("[캠페인 정보]\r\n");
        appendRow(sb, "항목", "값");
        appendRow(sb, "캠페인명", nullSafe(c.getName()));
        appendRow(sb, "상태", nullSafe(c.getStatus()));
        appendRow(sb, "시작일", c.getStartDate() != null ? c.getStartDate().format(DATE_FMT) : "");
        appendRow(sb, "종료일", c.getEndDate() != null ? c.getEndDate().format(DATE_FMT) : "");
        appendRow(sb, "목적", nullSafe(c.getPurpose()));
        appendRow(sb, "목표", nullSafe(c.getGoals()));
        appendRow(sb, "메인 메시지", nullSafe(c.getMainMessage()));
        appendRow(sb, "태그", c.getTags() != null ? String.join(", ", c.getTags()) : "");
        appendRow(sb, "파트너사", c.getPartners() != null ? String.join(", ", c.getPartners()) : "");
    }

    private void writeMembersSection(StringBuilder sb, Long campaignId) {
        List<CampaignMember> members = campaignMemberRepository.findAllByCampaignIdx(campaignId);
        sb.append("[팀 멤버]\r\n");
        appendRow(sb, "이름", "이메일", "회사", "부서", "캠페인 역할", "글로벌 역할", "참여일");
        for (CampaignMember m : members) {
            User u = m.getUser();
            appendRow(sb,
                    nullSafe(u.getName()),
                    nullSafe(u.getEmail()),
                    nullSafe(u.getCompanyName()),
                    nullSafe(u.getDepartment()),
                    m.getCampaignRole() != null ? m.getCampaignRole().name() : "",
                    nullSafe(u.getRole()),
                    m.getJoinedAt() != null ? m.getJoinedAt().format(DATETIME_FMT) : ""
            );
        }
    }

    private void writeTasksSection(StringBuilder sb, Long campaignId) {
        List<Task> tasks = taskRepository.findAllByTaskPart_Campaign_IdxOrderByIdxDesc(campaignId);
        sb.append("[업무 목록]\r\n");
        appendRow(sb, "ID", "업무명", "상태", "우선순위", "유형", "마감일", "담당자", "업무 파트", "마일스톤", "참여사", "메모");
        for (Task t : tasks) {
            String partName = t.getTaskPart() != null ? t.getTaskPart().getName() : "";
            String milestoneName = t.getMilestone() != null ? t.getMilestone().getName() : "";
            String assigneeName = t.getAssignee() != null ? t.getAssignee().getName() : "";
            String orgName = "";
            if (t.getParticipant() != null && t.getParticipant().getOrganization() != null) {
                orgName = t.getParticipant().getOrganization().getName();
            }
            appendRow(sb,
                    String.valueOf(t.getIdx()),
                    nullSafe(t.getName()),
                    t.getStatus() != null ? t.getStatus().name() : "",
                    t.getPriority() != null ? t.getPriority().name() : "",
                    t.getTaskType() != null ? t.getTaskType().name() : "",
                    t.getDueDate() != null ? t.getDueDate().format(DATETIME_FMT) : "",
                    assigneeName,
                    partName,
                    milestoneName,
                    orgName,
                    nullSafe(t.getMemo())
            );
        }
    }

    private void writeKpiSection(StringBuilder sb, Long campaignId, boolean esgOnly) {
        List<CampaignKpi> kpis = campaignKpiRepository.findAllByCampaignIdxOrderByIdxAsc(campaignId);
        if (esgOnly) {
            // KpiCategory.ESG가 분리됨 → CampaignKpi.esgCategory 필드 기준 필터링
            kpis = kpis.stream()
                    .filter(k -> k.getEsgCategory() != null)
                    .toList();
        }
        sb.append(esgOnly ? "[ESG 지표]\r\n" : "[KPI 지표]\r\n");
        appendRow(sb, "ID", "지표명", "카테고리", "목표값", "실측값", "단위", "달성률(%)", "상태", "담당", "측정일", "메모");
        for (CampaignKpi k : kpis) {
            BigDecimal target = k.getTargetValue();
            BigDecimal actual = k.getActualValue();
            Integer pct = CampaignKpiDto.calcAchievement(actual, target);
            String status = CampaignKpiDto.calcStatus(actual, pct).name();
            String ownerName = k.getOwnerUser() != null ? k.getOwnerUser().getName()
                    : nullSafe(k.getOwnerLabel());
            appendRow(sb,
                    String.valueOf(k.getIdx()),
                    nullSafe(k.getName()),
                    k.getCategory() != null ? k.getCategory().name() : "",
                    target != null ? target.toPlainString() : "",
                    actual != null ? actual.toPlainString() : "",
                    nullSafe(k.getUnit()),
                    pct != null ? String.valueOf(pct) : "",
                    status,
                    ownerName,
                    k.getMeasuredAt() != null ? k.getMeasuredAt().format(DATETIME_FMT) : "",
                    nullSafe(k.getMemo())
            );
        }
    }

    private void appendRow(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escapeCsv(cells[i]));
        }
        sb.append("\r\n");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        boolean needQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    public String resolveFileName(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .map(Campaign::getName)
                .map(name -> sanitizeFileName(name) + "_export.csv")
                .orElse("campaign_" + campaignId + "_export.csv");
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
