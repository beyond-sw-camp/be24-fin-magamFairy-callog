package org.example.backend.teamboard.service;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import org.example.backend.campaign.model.CalendarEventsDto;
import org.example.backend.campaign.service.CampaignCalendarService;
import org.example.backend.teamboard.model.CalendarIoDto;
import org.example.backend.teamboard.model.Task;
import org.example.backend.teamboard.model.TaskDto;
import org.example.backend.teamboard.model.TaskPriority;
import org.example.backend.teamboard.model.TaskStatus;
import org.example.backend.teamboard.repository.TaskRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 구글 캘린더 형식(.ics, RFC 5545) 가져오기/내보내기.
 * - 내보내기: 내 일정(업무/마일스톤/모집마감) 중 날짜 범위·유형에 맞는 것을 VCALENDAR로 직렬화.
 * - 가져오기: VEVENT를 개인 업무로 생성. mode=overwrite 면 파일의 날짜 범위 내 내 개인 업무를 먼저 삭제.
 */
@Service
@RequiredArgsConstructor
public class CalendarIoService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskService taskService;
    private final CampaignCalendarService campaignCalendarService;

    private static final DateTimeFormatter ICS_LOCAL =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    // ===================== 내보내기 =====================

    @Transactional(readOnly = true)
    public byte[] export(Long userIdx, LocalDate from, LocalDate to, Set<String> types) {
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//callog//Calendar 1.0//KO"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        // 1) 업무(task)
        if (types.contains("task")) {
            for (TaskDto.ResList t : taskService.listAll(userIdx)) {
                LocalDateTime start = t.startDate() != null ? t.startDate() : t.dueDate();
                LocalDateTime end = t.dueDate() != null ? t.dueDate() : t.startDate();
                if (start == null) continue;
                if (!intersects(start, end, from, to)) continue;
                addEvent(calendar, t.name(), start, end, "task-" + t.idx() + "@callog");
            }
        }

        // 2) 마일스톤 / 모집마감 — 내 캠페인 범위
        CalendarEventsDto.Res events = campaignCalendarService.loadEvents(userIdx, "mine");

        if (types.contains("milestone")) {
            for (CalendarEventsDto.MilestoneItem m : events.milestones()) {
                LocalDateTime start = m.startDate() != null ? m.startDate() : m.endDate();
                LocalDateTime end = m.endDate() != null ? m.endDate() : m.startDate();
                if (start == null) continue;
                if (!intersects(start, end, from, to)) continue;
                String title = "🚩 " + (m.name() != null ? m.name() : "마일스톤");
                addEvent(calendar, title, start, end, "ms-" + m.idx() + "@callog");
            }
        }

        if (types.contains("deadline")) {
            for (CalendarEventsDto.DeadlineItem d : events.deadlines()) {
                LocalDateTime when = d.recruitDeadline();
                if (when == null) continue;
                if (!intersects(when, when, from, to)) continue;
                String title = "⏰ 모집 마감: " + (d.campaignName() != null ? d.campaignName() : "");
                addEvent(calendar, title, when, when, "dl-" + d.campaignIdx() + "@callog");
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            CalendarOutputter outputter = new CalendarOutputter(false);
            outputter.output(calendar, out);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ".ics 생성에 실패했습니다.");
        }
        return out.toByteArray();
    }

    private void addEvent(Calendar calendar, String summary, LocalDateTime start, LocalDateTime end, String uid) {
        try {
            // 시각을 그대로(floating local) 출력 — 서버 타임존에 영향받지 않게.
            DateTime dtStart = new DateTime(start.format(ICS_LOCAL));
            LocalDateTime safeEnd = (end != null && !end.isBefore(start)) ? end : start;
            DateTime dtEnd = new DateTime(safeEnd.format(ICS_LOCAL));
            VEvent event = new VEvent(dtStart, dtEnd, summary != null ? summary : "(제목 없음)");
            event.getProperties().add(new Uid(uid));
            event.getProperties().add(new DtStamp());
            calendar.getComponents().add(event);
        } catch (Exception ignored) {
            // 개별 이벤트 변환 실패는 건너뜀
        }
    }

    private boolean intersects(LocalDateTime evStart, LocalDateTime evEnd, LocalDate from, LocalDate to) {
        LocalDate s = evStart.toLocalDate();
        LocalDate e = (evEnd != null ? evEnd : evStart).toLocalDate();
        return !s.isAfter(to) && !e.isBefore(from);
    }

    // ===================== 가져오기 =====================

    @Transactional
    public CalendarIoDto.ImportResult importIcs(Long userIdx, MultipartFile file, String mode) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "가져올 .ics 파일이 없습니다.");
        }
        User owner = userRepository.findById(userIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        boolean overwrite = "overwrite".equalsIgnoreCase(mode);

        // 1) 파싱 → 후보 일정 추출
        List<Parsed> parsed = new ArrayList<>();
        try {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(file.getInputStream());
            for (Object obj : calendar.getComponents(Component.VEVENT)) {
                VEvent ev = (VEvent) obj;
                String summary = null;
                String dtStartRaw = null;
                String dtEndRaw = null;
                for (Object pObj : ev.getProperties()) {
                    Property p = (Property) pObj;
                    switch (p.getName()) {
                        case "SUMMARY" -> summary = p.getValue();
                        case "DTSTART" -> dtStartRaw = p.getValue();
                        case "DTEND" -> dtEndRaw = p.getValue();
                        default -> { /* 무시 */ }
                    }
                }
                boolean[] allDay = { false };
                LocalDateTime start = parseIcsDate(dtStartRaw, allDay);
                if (start == null) continue;
                LocalDateTime end = parseIcsDate(dtEndRaw, new boolean[]{ false });

                if (allDay[0]) {
                    // 종일: DTEND는 배타적(다음 날) → 하루 빼고 23:59
                    start = start.toLocalDate().atStartOfDay();
                    if (end != null) {
                        LocalDate endDate = end.toLocalDate().minusDays(1);
                        end = (endDate.isBefore(start.toLocalDate()) ? start.toLocalDate() : endDate).atTime(23, 59, 59);
                    } else {
                        end = start.toLocalDate().atTime(23, 59, 59);
                    }
                } else if (end == null || end.isBefore(start)) {
                    end = start;
                }
                parsed.add(new Parsed(summary != null && !summary.isBlank() ? summary : "(제목 없음)", start, end));
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 .ics 파일이 아닙니다.");
        }

        if (parsed.isEmpty()) {
            return new CalendarIoDto.ImportResult(0, 0, overwrite ? "overwrite" : "append");
        }

        // 2) 덮어쓰기: 파일의 날짜 범위 내 내 개인 업무 삭제
        int deleted = 0;
        if (overwrite) {
            LocalDateTime minStart = parsed.stream().map(Parsed::start).min(LocalDateTime::compareTo).orElseThrow();
            LocalDateTime maxEnd = parsed.stream().map(Parsed::end).max(LocalDateTime::compareTo).orElseThrow();
            LocalDateTime rangeFrom = minStart.toLocalDate().atStartOfDay();
            LocalDateTime rangeTo = maxEnd.toLocalDate().atTime(23, 59, 59);
            List<Task> existing = taskRepository
                    .findAllByAssignee_IdxAndCampaignIsNullAndParticipantIsNullAndMilestoneIsNullAndTaskPartIsNullAndDueDateBetween(
                            userIdx, rangeFrom, rangeTo);
            deleted = existing.size();
            if (!existing.isEmpty()) {
                taskRepository.deleteAll(existing);
            }
        }

        // 3) 개인 업무로 추가
        List<Task> toSave = new ArrayList<>();
        for (Parsed p : parsed) {
            toSave.add(Task.builder()
                    .name(p.summary())
                    .assignee(owner)
                    .startDate(p.start())
                    .dueDate(p.end())
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.MEDIUM)
                    .build());
        }
        taskRepository.saveAll(toSave);

        return new CalendarIoDto.ImportResult(toSave.size(), deleted, overwrite ? "overwrite" : "append");
    }

    /** "YYYYMMDD"(종일) 또는 "YYYYMMDDThhmmss[Z]" 를 LocalDateTime 으로. */
    private LocalDateTime parseIcsDate(String raw, boolean[] allDayOut) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        try {
            if (v.length() == 8) { // YYYYMMDD — 종일
                allDayOut[0] = true;
                LocalDate d = LocalDate.parse(v, DateTimeFormatter.BASIC_ISO_DATE);
                return d.atStartOfDay();
            }
            boolean utc = v.endsWith("Z");
            String core = utc ? v.substring(0, v.length() - 1) : v;
            LocalDateTime ldt = LocalDateTime.parse(core, ICS_LOCAL);
            if (utc) {
                ldt = ldt.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
            return ldt;
        } catch (Exception e) {
            return null;
        }
    }

    private record Parsed(String summary, LocalDateTime start, LocalDateTime end) {}
}
