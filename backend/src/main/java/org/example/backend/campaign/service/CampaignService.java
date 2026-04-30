package org.example.backend.campaign.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.campaign.model.Campaign;
import org.example.backend.campaign.model.CampaignDto;
import org.example.backend.campaign.repository.CampaignRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignService {
    private static final String DEFAULT_COLOR = "#8B5CF6";
    private static final List<String> ALLOWED_STATUSES = List.of("draft", "review", "live", "paused", "completed");

    private final CampaignRepository campaignRepository;

    public List<CampaignDto.Res> listCampaigns(String ownerLoginId) {
        return campaignRepository.findAllByOwnerLoginIdOrderByIdxDesc(ownerLoginId).stream()
                .map(CampaignDto.Res::from)
                .toList();
    }

    @Transactional
    public CampaignDto.Res createCampaign(String ownerLoginId, CampaignDto.UpsertReq dto) {
        String name = normalizeRequired(dto.name(), "Campaign name is required.");

        Campaign campaign = Campaign.builder()
                .ownerLoginId(ownerLoginId)
                .name(name)
                .purpose(normalizeText(dto.purpose()))
                .tags(normalizeList(dto.tags()))
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .partners(normalizeList(dto.partners()))
                .goals(normalizeText(dto.goals()))
                .mainMessage(normalizeText(dto.mainMessage()))
                .status("draft")
                .initials(createInitials(name))
                .color(normalizeColor(dto.color()))
                .build();

        return CampaignDto.Res.from(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignDto.Res updateCampaign(String ownerLoginId, Long campaignId, CampaignDto.UpsertReq dto) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        String name = normalizeRequired(dto.name(), "Campaign name is required.");

        campaign.updateDetails(
                name,
                normalizeText(dto.purpose()),
                normalizeList(dto.tags()),
                dto.startDate(),
                dto.endDate(),
                normalizeList(dto.partners()),
                normalizeText(dto.goals()),
                normalizeText(dto.mainMessage()),
                createInitials(name),
                normalizeColor(dto.color())
        );

        return CampaignDto.Res.from(campaign);
    }

    @Transactional
    public CampaignDto.Res invitePartners(String ownerLoginId, Long campaignId, CampaignDto.PartnerInviteReq dto) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        campaign.updatePartners(normalizeList(dto.partners()));
        return CampaignDto.Res.from(campaign);
    }

    @Transactional
    public CampaignDto.Res updateStatus(String ownerLoginId, Long campaignId, CampaignDto.StatusReq dto) {
        Campaign campaign = getOwnedCampaign(ownerLoginId, campaignId);
        String status = normalizeRequired(dto.status(), "Campaign status is required.");

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported campaign status.");
        }

        campaign.updateStatus(status);
        return CampaignDto.Res.from(campaign);
    }

    private Campaign getOwnedCampaign(String ownerLoginId, Long campaignId) {
        return campaignRepository.findByIdxAndOwnerLoginId(campaignId, ownerLoginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign was not found."));
    }

    private static String normalizeRequired(String value, String message) {
        String normalized = normalizeText(value);

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return new ArrayList<>(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private static String normalizeColor(String color) {
        String normalized = normalizeText(color);
        return normalized.isBlank() ? DEFAULT_COLOR : normalized;
    }

    private static String createInitials(String name) {
        String normalized = normalizeText(name);

        if (normalized.isBlank()) {
            return "CP";
        }

        String[] words = normalized.split("\\s+");
        String initials = words.length > 1
                ? words[0].substring(0, 1) + words[1].substring(0, 1)
                : normalized.substring(0, Math.min(2, normalized.length()));

        return initials.toUpperCase();
    }
}
