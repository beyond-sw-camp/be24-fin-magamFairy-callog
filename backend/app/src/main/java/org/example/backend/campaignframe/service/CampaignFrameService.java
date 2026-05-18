package org.example.backend.campaignframe.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaignframe.model.CampaignFrame;
import org.example.backend.campaignframe.model.CampaignFrameDto;
import org.example.backend.campaignframe.repository.CampaignFrameRepository;
import org.example.backend.user.model.User;
import org.example.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CampaignFrameService {
    private static final String DEFAULT_CATEGORY = "공통";
    private static final String DEFAULT_VERSION = "v1.0";
    private static final String DEFAULT_STATUS = "draft";

    private final CampaignFrameRepository campaignFrameRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CampaignFrameDto.FrameRes> getFrameList(Authentication authentication) {
        User owner = resolveAuthenticatedUser(authentication);
        return campaignFrameRepository.findAllByOwnerOrderByIdxDesc(owner).stream()
                .map(CampaignFrameDto.FrameRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignFrameDto.FrameRes getFrame(String frameId, Authentication authentication) {
        User owner = resolveAuthenticatedUser(authentication);
        return CampaignFrameDto.FrameRes.from(findOwnedFrame(owner, frameId));
    }

    @Transactional
    public CampaignFrameDto.FrameRes createFrame(CampaignFrameDto.CreateFrameReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        User owner = resolveAuthenticatedUser(authentication);
        CampaignFrameDto.UpsertReq normalized = normalizeCreateReq(dto);

        if (campaignFrameRepository.existsByOwnerAndId(owner, normalized.id())) {
            throw new IllegalArgumentException("이미 존재하는 프레임 ID입니다.");
        }

        CampaignFrame frame = CampaignFrame.builder()
                .id(normalized.id())
                .owner(owner)
                .category(normalized.category())
                .version(normalized.version())
                .title(normalized.title())
                .score(normalized.score())
                .status(normalized.status())
                .overview(normalized.overview())
                .requiredFields(normalized.requiredFields())
                .bannedExpressions(normalized.bannedExpressions())
                .recommendedExpressions(normalized.recommendedExpressions())
                .toneGuide(normalized.toneGuide())
                .approvalProcess(normalized.approvalProcess())
                .usageCount(normalized.performance().usageCount())
                .passRate(normalized.performance().passRate())
                .avgRevisions(normalized.performance().avgRevisions())
                .build();

        return CampaignFrameDto.FrameRes.from(campaignFrameRepository.save(frame));
    }

    @Transactional
    public CampaignFrameDto.FrameRes updateFrame(String frameId, CampaignFrameDto.UpdateFrameReq dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("request body is required.");
        }

        User owner = resolveAuthenticatedUser(authentication);
        CampaignFrame frame = findOwnedFrame(owner, frameId);
        CampaignFrameDto.UpsertReq normalized = normalizeUpdateReq(frame.getId(), dto);
        frame.update(normalized);

        return CampaignFrameDto.FrameRes.from(frame);
    }

    @Transactional
    public CampaignFrameDto.DeleteFrameRes deleteFrame(String frameId, Authentication authentication) {
        User owner = resolveAuthenticatedUser(authentication);
        CampaignFrame frame = findOwnedFrame(owner, frameId);
        CampaignFrameDto.DeleteFrameRes result = CampaignFrameDto.DeleteFrameRes.from(frame);
        campaignFrameRepository.delete(frame);
        return result;
    }

    private CampaignFrameDto.UpsertReq normalizeCreateReq(CampaignFrameDto.CreateFrameReq dto) {
        String title = requireText(dto.title(), "title");
        CampaignFrameDto.PerformanceReq performance = normalizePerformance(dto.performance(), dto.score());

        return CampaignFrameDto.UpsertReq.builder()
                .id(defaultIfBlank(dto.id(), UUID.randomUUID().toString()))
                .category(defaultIfBlank(dto.category(), DEFAULT_CATEGORY))
                .version(defaultIfBlank(dto.version(), DEFAULT_VERSION))
                .title(title)
                .score(clamp(defaultNumber(dto.score(), performance.passRate()), 0, 100))
                .status(defaultIfBlank(dto.status(), DEFAULT_STATUS))
                .overview(normalizeOptional(dto.overview()))
                .requiredFields(normalizeList(dto.requiredFields()))
                .bannedExpressions(normalizeList(dto.bannedExpressions()))
                .recommendedExpressions(normalizeList(dto.recommendedExpressions()))
                .toneGuide(normalizeOptional(dto.toneGuide()))
                .approvalProcess(normalizeList(dto.approvalProcess()))
                .performance(performance)
                .build();
    }

    private CampaignFrameDto.UpsertReq normalizeUpdateReq(String frameId, CampaignFrameDto.UpdateFrameReq dto) {
        CampaignFrameDto.PerformanceReq performance = normalizePerformance(dto.performance(), dto.score());

        return CampaignFrameDto.UpsertReq.builder()
                .id(frameId)
                .category(defaultIfBlank(dto.category(), DEFAULT_CATEGORY))
                .version(defaultIfBlank(dto.version(), DEFAULT_VERSION))
                .title(requireText(dto.title(), "title"))
                .score(clamp(defaultNumber(dto.score(), performance.passRate()), 0, 100))
                .status(defaultIfBlank(dto.status(), DEFAULT_STATUS))
                .overview(normalizeOptional(dto.overview()))
                .requiredFields(normalizeList(dto.requiredFields()))
                .bannedExpressions(normalizeList(dto.bannedExpressions()))
                .recommendedExpressions(normalizeList(dto.recommendedExpressions()))
                .toneGuide(normalizeOptional(dto.toneGuide()))
                .approvalProcess(normalizeList(dto.approvalProcess()))
                .performance(performance)
                .build();
    }

    private CampaignFrame findOwnedFrame(User owner, String frameId) {
        return campaignFrameRepository.findByOwnerAndId(owner, requireText(frameId, "frameId"))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프레임입니다."));
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("authentication is required.");
        }

        return findUserByIdOrEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("user not found."));
    }

    private Optional<User> findUserByIdOrEmail(String idOrEmail) {
        return userRepository.findUserById(idOrEmail)
                .or(() -> userRepository.findByEmail(idOrEmail));
    }

    private CampaignFrameDto.PerformanceReq normalizePerformance(CampaignFrameDto.PerformanceReq performance, Integer fallbackScore) {
        int passRate = clamp(defaultNumber(performance == null ? null : performance.passRate(), defaultNumber(fallbackScore, 0)), 0, 100);
        return new CampaignFrameDto.PerformanceReq(
                Math.max(0, defaultNumber(performance == null ? null : performance.usageCount(), 0)),
                passRate,
                Math.max(0.0, defaultDouble(performance == null ? null : performance.avgRevisions(), 0.0))
        );
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return Optional.ofNullable(normalizeOptional(value)).orElse(defaultValue);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int defaultNumber(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private double defaultDouble(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
