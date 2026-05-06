package org.example.backend.campaign.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignKpi;
import org.example.backend.campaign.model.CampaignKpiDto;
import org.example.backend.campaign.model.CampaignMember;
import org.example.backend.campaign.repository.CampaignKpiRepository;
import org.example.backend.campaign.repository.CampaignMemberRepository;
import org.example.backend.campaign.repository.CampaignRepository;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignPdfReportService {

    private final CampaignRepository campaignRepository;
    private final TaskRepository taskRepository;
    private final CampaignKpiRepository campaignKpiRepository;
    private final CampaignMemberRepository campaignMemberRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generate(Long campaignId, String type) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "캠페인을 찾을 수 없습니다."));

        boolean fullReport = "full".equalsIgnoreCase(type);
        String html = buildHtml(campaign, fullReport);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("PDF 생성 실패: campaignId={}", campaignId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF 생성에 실패했습니다.");
        }
    }

    /** 한글 폰트 등록 우선순위:
     *  1) classpath:fonts/*.ttf|.otf (사용자가 추가한 것)
     *  2) OS 기본 한글 폰트 (Windows: Malgun Gothic / macOS: AppleSDGothicNeo / Linux: NanumGothic)
     */
    private void registerFonts(PdfRendererBuilder builder) {
        if (registerClasspathFonts(builder)) return;
        if (registerSystemKoreanFont(builder)) return;
        log.warn("PDF: 한글 폰트를 찾지 못했습니다. resources/fonts/ 에 .ttf 파일을 추가해주세요. 한글이 □로 표시될 수 있습니다.");
    }

    private boolean registerClasspathFonts(PdfRendererBuilder builder) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] fonts = resolver.getResources("classpath:fonts/*");
            int registered = 0;
            for (Resource r : fonts) {
                String name = r.getFilename();
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (!(lower.endsWith(".ttf") || lower.endsWith(".otf"))) continue;
                Path tempFile = copyToTempFile(r, lower.endsWith(".otf") ? ".otf" : ".ttf");
                builder.useFont(tempFile.toFile(), "ReportFont");
                registered++;
                log.info("PDF 폰트 등록 (classpath): {}", name);
            }
            return registered > 0;
        } catch (IOException e) {
            log.warn("PDF 폰트 디렉토리 스캔 실패: {}", e.getMessage());
            return false;
        }
    }

    private boolean registerSystemKoreanFont(PdfRendererBuilder builder) {
        String[] candidates = {
                "C:/Windows/Fonts/malgun.ttf",        // Malgun Gothic (Windows 한글 기본)
                "C:/Windows/Fonts/malgunsl.ttf",      // Malgun Gothic Semilight
                "C:/Windows/Fonts/NanumGothic.ttf",   // Nanum (설치된 경우)
                "/Library/Fonts/AppleSDGothicNeo.ttc",
                "/System/Library/Fonts/AppleSDGothicNeo.ttc",
                "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
        };
        for (String path : candidates) {
            java.io.File f = new java.io.File(path);
            if (f.exists() && f.canRead()) {
                try {
                    builder.useFont(f, "ReportFont");
                    log.info("PDF 폰트 등록 (system): {}", path);
                    return true;
                } catch (Exception e) {
                    log.debug("시스템 폰트 등록 실패: {} ({})", path, e.getMessage());
                }
            }
        }
        return false;
    }

    private Path copyToTempFile(Resource resource, String suffix) throws IOException {
        Path temp = Files.createTempFile("callog-pdf-font-", suffix);
        temp.toFile().deleteOnExit();
        try (InputStream in = resource.getInputStream()) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }

    /** ─── HTML 빌드 ─────────────────────────────────────── */
    private String buildHtml(Campaign campaign, boolean fullReport) {
        Long campaignId = campaign.getIdx();
        List<Task> tasks = taskRepository.findAllByTaskPart_Campaign_IdxOrderByIdxDesc(campaignId);
        List<CampaignKpi> kpis = campaignKpiRepository.findAllByCampaignIdxOrderByIdxAsc(campaignId);
        List<CampaignMember> members = campaignMemberRepository.findAllByCampaignIdx(campaignId);

        String summaryPage = buildSummaryPage(campaign, tasks, kpis);

        StringBuilder body = new StringBuilder();
        body.append(summaryPage);
        if (fullReport) {
            body.append(buildKpiDetailPage(campaign, kpis));
            body.append(buildTasksPage(campaign, tasks));
            body.append(buildTeamPage(campaign, members, kpis));
        }

        return wrapDocument(body.toString());
    }

    private String buildSummaryPage(Campaign c, List<Task> tasks, List<CampaignKpi> kpis) {
        // 업무 통계
        LocalDate today = LocalDate.now();
        long totalTasks = tasks.size();
        long doneTasks = tasks.stream()
                .filter(t -> t.getStatus() != null && "DONE".equals(t.getStatus().name()))
                .count();
        long reviewTasks = tasks.stream()
                .filter(t -> t.getStatus() != null && "REVIEW".equals(t.getStatus().name()))
                .count();
        long overdueTasks = tasks.stream()
                .filter(t -> t.getStatus() != null && !"DONE".equals(t.getStatus().name()))
                .filter(t -> t.getDueDate() != null && t.getDueDate().toLocalDate().isBefore(today))
                .count();

        // KPI 통계
        int totalKpis = kpis.size();
        int achievedCount = 0;
        int pendingCount = 0;
        int sumPct = 0;
        int measuredCount = 0;
        for (CampaignKpi k : kpis) {
            Integer pct = CampaignKpiDto.calcAchievement(k.getActualValue(), k.getTargetValue());
            String status = CampaignKpiDto.calcStatus(k.getActualValue(), pct).name();
            if ("PENDING".equals(status)) {
                pendingCount++;
            } else {
                if ("ACHIEVED".equals(status) || "OVER".equals(status)) achievedCount++;
                if (pct != null) {
                    sumPct += pct;
                    measuredCount++;
                }
            }
        }
        Integer overallPct = measuredCount > 0 ? sumPct / measuredCount : null;

        // ESG
        List<CampaignKpi> esgKpis = kpis.stream()
                .filter(k -> k.getCategory() != null && "ESG".equals(k.getCategory().name()))
                .toList();
        int esgTotal = esgKpis.size();
        int esgDone = (int) esgKpis.stream()
                .filter(k -> {
                    Integer pct = CampaignKpiDto.calcAchievement(k.getActualValue(), k.getTargetValue());
                    String s = CampaignKpiDto.calcStatus(k.getActualValue(), pct).name();
                    return "ACHIEVED".equals(s) || "OVER".equals(s);
                })
                .count();
        int esgSum = 0, esgMeasured = 0;
        for (CampaignKpi k : esgKpis) {
            Integer pct = CampaignKpiDto.calcAchievement(k.getActualValue(), k.getTargetValue());
            if (pct != null) { esgSum += Math.min(pct, 100); esgMeasured++; }
        }
        Integer esgScore = esgMeasured > 0 ? esgSum / esgMeasured : null;
        String esgRating = ratingOf(esgScore);

        // Top 3 KPI
        List<CampaignKpi> topKpis = kpis.stream()
                .filter(k -> k.getActualValue() != null)
                .sorted(Comparator.comparingInt(
                        (CampaignKpi k) -> {
                            Integer p = CampaignKpiDto.calcAchievement(k.getActualValue(), k.getTargetValue());
                            return p == null ? 0 : p;
                        }
                ).reversed())
                .limit(3)
                .toList();

        StringBuilder topHtml = new StringBuilder();
        if (topKpis.isEmpty()) {
            topHtml.append("<div class='empty'>측정된 KPI가 없습니다.</div>");
        } else {
            for (CampaignKpi k : topKpis) {
                Integer pct = CampaignKpiDto.calcAchievement(k.getActualValue(), k.getTargetValue());
                int safePct = pct == null ? 0 : Math.min(pct, 100);
                String fillClass = (pct != null && pct >= 100) ? "fill-success" : "fill-warn";
                String pctClass = (pct != null && pct >= 100) ? "pct-success" : "pct-warn";
                topHtml.append("<div class='top-row'>")
                        .append("<div class='top-info'>")
                        .append("<span class='top-name'>").append(esc(k.getName())).append("</span>")
                        .append("<span class='top-actual'>")
                        .append(formatNum(k.getActualValue())).append(esc(k.getUnit()))
                        .append(" / 목표 ").append(formatNum(k.getTargetValue())).append(esc(k.getUnit()))
                        .append("</span>")
                        .append("</div>")
                        .append("<div class='top-bar-wrap'>")
                        .append("<div class='top-bar'><div class='top-fill ").append(fillClass)
                        .append("' style='width:").append(safePct).append("%'></div></div>")
                        .append("<span class='top-pct ").append(pctClass).append("'>")
                        .append(pct == null ? "-" : (pct + "%")).append("</span>")
                        .append("</div>")
                        .append("</div>");
            }
        }

        return "<div class='page'>" +
                buildHeader(c) +
                "<section class='stats-row'>" +
                statCard("info", "전체 업무", totalTasks, totalTasks == 0 ? 0 : 100) +
                statCard("success", "완료", doneTasks, totalTasks == 0 ? 0 : (int) (doneTasks * 100 / totalTasks)) +
                statCard("primary", "검수 대기", reviewTasks, totalTasks == 0 ? 0 : (int) (reviewTasks * 100 / totalTasks)) +
                statCard("warning", "지연 업무", overdueTasks, totalTasks == 0 ? 0 : (int) (overdueTasks * 100 / totalTasks)) +
                "</section>" +
                "<section class='bento'>" +
                "<article class='panel kpi-panel'>" +
                "<div class='panel-head'><h2>KPI 성과 요약</h2></div>" +
                "<div class='kpi-hero'>" +
                "<div class='gauge-cell'>" + gaugeBox(overallPct, "전체 달성률", "kpi") + "</div>" +
                "<div class='kpi-meta'>" +
                metaRow("달성 지표", achievedCount + " / " + totalKpis + "건") +
                metaRow("측정 대기", pendingCount + "건") +
                "</div>" +
                "</div>" +
                "<div class='kpi-list'>" + topHtml + "</div>" +
                "</article>" +
                "<article class='panel esg-panel'>" +
                "<div class='panel-head'><h2>ESG 점수</h2></div>" +
                "<div class='esg-hero'>" +
                gaugeBox(esgScore, esgRating.isEmpty() ? "ESG" : esgRating, "esg") +
                "</div>" +
                "<div class='esg-foot'>" +
                "<div class='esg-foot-row'><span>달성</span><strong>" + esgDone + " / " + esgTotal + "건</strong></div>" +
                (esgTotal == 0
                        ? "<div class='empty'>등록된 ESG KPI가 없습니다.</div>"
                        : "") +
                "</div>" +
                "</article>" +
                "</section>" +
                buildPageFooter(c, "Executive Summary") +
                "</div>";
    }

    private String buildKpiDetailPage(Campaign c, List<CampaignKpi> kpis) {
        StringBuilder rows = new StringBuilder();
        if (kpis.isEmpty()) {
            rows.append("<tr><td colspan='6' class='empty-row'>등록된 KPI가 없습니다.</td></tr>");
        } else {
            for (CampaignKpi k : kpis) {
                Integer pct = CampaignKpiDto.calcAchievement(k.getActualValue(), k.getTargetValue());
                String status = CampaignKpiDto.calcStatus(k.getActualValue(), pct).name();
                String statusClass = switch (status) {
                    case "ACHIEVED", "OVER" -> "tag-success";
                    case "BEHIND" -> "tag-warn";
                    default -> "tag-mute";
                };
                rows.append("<tr>")
                        .append("<td class='td-name'>").append(esc(k.getName())).append("</td>")
                        .append("<td>").append(k.getCategory() != null ? k.getCategory().name() : "-").append("</td>")
                        .append("<td class='td-num'>").append(formatNum(k.getTargetValue())).append(esc(k.getUnit())).append("</td>")
                        .append("<td class='td-num'>").append(formatNum(k.getActualValue())).append(esc(k.getUnit())).append("</td>")
                        .append("<td class='td-num'><strong>").append(pct == null ? "-" : (pct + "%")).append("</strong></td>")
                        .append("<td><span class='tag ").append(statusClass).append("'>").append(statusKor(status)).append("</span></td>")
                        .append("</tr>");
            }
        }
        return "<div class='page'>" +
                buildHeader(c) +
                "<section class='detail-section'>" +
                "<h2 class='detail-title'>01. KPI 전체 지표</h2>" +
                "<table class='data-table'>" +
                "<thead><tr><th>지표명</th><th>카테고리</th><th>목표</th><th>실측</th><th>달성률</th><th>상태</th></tr></thead>" +
                "<tbody>" + rows + "</tbody>" +
                "</table>" +
                "</section>" +
                buildPageFooter(c, "KPI 상세") +
                "</div>";
    }

    private String buildTasksPage(Campaign c, List<Task> tasks) {
        StringBuilder rows = new StringBuilder();
        if (tasks.isEmpty()) {
            rows.append("<tr><td colspan='5' class='empty-row'>등록된 업무가 없습니다.</td></tr>");
        } else {
            int limit = Math.min(tasks.size(), 30);
            for (int i = 0; i < limit; i++) {
                Task t = tasks.get(i);
                String statusName = t.getStatus() != null ? t.getStatus().name() : "-";
                String statusClass = switch (statusName) {
                    case "DONE" -> "tag-success";
                    case "REVIEW" -> "tag-info";
                    case "BLOCKED" -> "tag-warn";
                    default -> "tag-mute";
                };
                String dueStr = t.getDueDate() != null ? t.getDueDate().toLocalDate().format(DATE_FMT) : "-";
                String partName = t.getTaskPart() != null ? t.getTaskPart().getName() : "-";
                String assigneeName = t.getAssignee() != null ? t.getAssignee().getName() : "-";
                rows.append("<tr>")
                        .append("<td class='td-name'>").append(esc(t.getName())).append("</td>")
                        .append("<td><span class='tag ").append(statusClass).append("'>").append(statusKor(statusName)).append("</span></td>")
                        .append("<td>").append(esc(partName)).append("</td>")
                        .append("<td>").append(esc(assigneeName)).append("</td>")
                        .append("<td>").append(dueStr).append("</td>")
                        .append("</tr>");
            }
            if (tasks.size() > 30) {
                rows.append("<tr><td colspan='5' class='empty-row'>외 ")
                        .append(tasks.size() - 30).append("건 (전체는 CSV 내보내기로 확인하세요)</td></tr>");
            }
        }
        return "<div class='page'>" +
                buildHeader(c) +
                "<section class='detail-section'>" +
                "<h2 class='detail-title'>02. 업무 목록</h2>" +
                "<table class='data-table'>" +
                "<thead><tr><th>업무명</th><th>상태</th><th>업무 파트</th><th>담당자</th><th>마감일</th></tr></thead>" +
                "<tbody>" + rows + "</tbody>" +
                "</table>" +
                "</section>" +
                buildPageFooter(c, "업무 목록") +
                "</div>";
    }

    private String buildTeamPage(Campaign c, List<CampaignMember> members, List<CampaignKpi> kpis) {
        StringBuilder mrows = new StringBuilder();
        if (members.isEmpty()) {
            mrows.append("<tr><td colspan='4' class='empty-row'>등록된 팀 멤버가 없습니다.</td></tr>");
        } else {
            for (CampaignMember m : members) {
                var u = m.getUser();
                mrows.append("<tr>")
                        .append("<td class='td-name'>").append(esc(u.getName())).append("</td>")
                        .append("<td>").append(esc(u.getCompanyName())).append("</td>")
                        .append("<td>").append(esc(u.getDepartment())).append("</td>")
                        .append("<td><span class='tag tag-info'>")
                        .append(m.getCampaignRole() != null ? m.getCampaignRole().name() : "-")
                        .append("</span></td>")
                        .append("</tr>");
            }
        }

        StringBuilder erows = new StringBuilder();
        List<CampaignKpi> esgKpis = kpis.stream()
                .filter(k -> k.getCategory() != null && "ESG".equals(k.getCategory().name()))
                .toList();
        if (esgKpis.isEmpty()) {
            erows.append("<tr><td colspan='4' class='empty-row'>등록된 ESG KPI가 없습니다.</td></tr>");
        } else {
            for (CampaignKpi k : esgKpis) {
                Integer pct = CampaignKpiDto.calcAchievement(k.getActualValue(), k.getTargetValue());
                String status = CampaignKpiDto.calcStatus(k.getActualValue(), pct).name();
                String statusClass = switch (status) {
                    case "ACHIEVED", "OVER" -> "tag-success";
                    case "BEHIND" -> "tag-warn";
                    default -> "tag-mute";
                };
                erows.append("<tr>")
                        .append("<td class='td-name'>").append(esc(k.getName())).append("</td>")
                        .append("<td class='td-num'>").append(formatNum(k.getTargetValue())).append(esc(k.getUnit())).append("</td>")
                        .append("<td class='td-num'>").append(formatNum(k.getActualValue())).append(esc(k.getUnit())).append("</td>")
                        .append("<td><span class='tag ").append(statusClass).append("'>").append(pct == null ? "-" : (pct + "%")).append("</span></td>")
                        .append("</tr>");
            }
        }

        return "<div class='page'>" +
                buildHeader(c) +
                "<section class='detail-section'>" +
                "<h2 class='detail-title'>03. 팀 멤버</h2>" +
                "<table class='data-table'>" +
                "<thead><tr><th>이름</th><th>회사</th><th>부서</th><th>역할</th></tr></thead>" +
                "<tbody>" + mrows + "</tbody>" +
                "</table>" +
                "</section>" +
                "<section class='detail-section'>" +
                "<h2 class='detail-title'>04. ESG 지표 상세</h2>" +
                "<table class='data-table'>" +
                "<thead><tr><th>지표명</th><th>목표</th><th>실측</th><th>달성률</th></tr></thead>" +
                "<tbody>" + erows + "</tbody>" +
                "</table>" +
                "</section>" +
                buildPageFooter(c, "팀 / ESG") +
                "</div>";
    }

    private String buildHeader(Campaign c) {
        String period = (c.getStartDate() != null ? c.getStartDate().format(DATE_FMT) : "-")
                + " ~ " + (c.getEndDate() != null ? c.getEndDate().format(DATE_FMT) : "-");
        String status = c.getStatus() == null ? "draft" : c.getStatus();
        String now = LocalDateTime.now().format(STAMP_FMT);
        return "<header class='cover'>" +
                "<div class='cover-eyebrow'>CAMPAIGN PERFORMANCE REPORT</div>" +
                "<h1 class='cover-title'>" + esc(c.getName()) + "</h1>" +
                "<div class='cover-meta'>" +
                "<span class='cover-period'>" + period + "</span>" +
                "<span class='cover-status'>" + esc(status) + "</span>" +
                "<span class='cover-stamp'>Generated " + now + "</span>" +
                "</div>" +
                "</header>";
    }

    private String buildPageFooter(Campaign c, String section) {
        return "<footer class='page-foot'>" +
                "<span>" + esc(c.getName()) + " · " + section + "</span>" +
                "<span class='page-foot-brand'>callog</span>" +
                "</footer>";
    }

    private String statCard(String tone, String label, long value, int barPct) {
        return "<article class='stat stat-" + tone + "'>" +
                "<div class='stat-label'>" + label + "</div>" +
                "<div class='stat-value'>" + value + "<small>건</small></div>" +
                "<div class='stat-bar'><div class='stat-fill' style='width:" + Math.min(barPct, 100) + "%'></div></div>" +
                "</article>";
    }

    /** PDF는 SVG가 어렵기 때문에 큰 숫자 + 가로 진행바로 대체 (PDF 친화적). */
    private String gaugeBox(Integer pct, String sub, String tone) {
        int safePct = pct == null ? 0 : Math.min(pct, 100);
        return "<div class='gauge gauge-" + tone + "'>" +
                "<div class='gauge-num'>" + (pct == null ? "-" : pct) + "<span class='gauge-num-unit'>" +
                ("kpi".equals(tone) ? "%" : "점") + "</span></div>" +
                "<div class='gauge-bar'><div class='gauge-bar-fill gauge-bar-fill-" + tone +
                "' style='width:" + safePct + "%'></div></div>" +
                "<div class='gauge-sub'>" + esc(sub) + "</div>" +
                "</div>";
    }

    private String metaRow(String label, String value) {
        return "<div class='meta-row'>" +
                "<span class='meta-label'>" + label + "</span>" +
                "<strong class='meta-value'>" + value + "</strong>" +
                "</div>";
    }

    private String wrapDocument(String body) {
        // 외부 stylesheet 인라인. ReportFont를 폰트 패밀리로 사용 (등록되지 않으면 sans-serif fallback).
        String css = readClasspathText("templates/pdf/report.css");
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'/>" +
                "<style>" + css + "</style>" +
                "</head><body>" + body + "</body></html>";
    }

    private String readClasspathText(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) return "";
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("템플릿 읽기 실패: {}", path, e);
            return "";
        }
    }

    private String formatNum(BigDecimal v) {
        if (v == null) return "-";
        return v.stripTrailingZeros().toPlainString();
    }

    private String statusKor(String status) {
        return switch (status) {
            case "DONE" -> "완료";
            case "REVIEW" -> "검수";
            case "IN_PROGRESS" -> "진행중";
            case "BACKLOG" -> "백로그";
            case "TODO" -> "예정";
            case "BLOCKED" -> "막힘";
            case "ACHIEVED" -> "달성";
            case "OVER" -> "초과달성";
            case "BEHIND" -> "미달";
            case "PENDING" -> "측정대기";
            default -> status;
        };
    }

    private String ratingOf(Integer score) {
        if (score == null) return "";
        if (score >= 90) return "AAA";
        if (score >= 80) return "AA";
        if (score >= 70) return "A";
        if (score >= 60) return "BBB";
        if (score >= 50) return "BB";
        if (score >= 40) return "B";
        return "CCC";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public String resolveFileName(Long campaignId, String type) {
        String suffix = "full".equalsIgnoreCase(type) ? "_report.pdf" : "_summary.pdf";
        return campaignRepository.findById(campaignId)
                .map(Campaign::getName)
                .map(name -> name.replaceAll("[\\\\/:*?\"<>|]", "_").trim() + suffix)
                .orElse("campaign_" + campaignId + suffix);
    }
}
