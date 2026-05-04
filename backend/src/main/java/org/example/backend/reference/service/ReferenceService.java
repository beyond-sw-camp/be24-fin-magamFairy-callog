package org.example.backend.reference.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.reference.model.Reference;
import org.example.backend.reference.model.ReferenceDto;
import org.example.backend.reference.repository.ReferenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReferenceService {
    private static final String DEFAULT_TYPE = "link";
    private static final String DEFAULT_CHANNEL = "공통";
    private static final String DEFAULT_OBJECTIVE = "참고";
    private static final String DEFAULT_STATUS = "참고";

    private final ReferenceRepository referenceRepository;

    public List<ReferenceDto.Res> getReferences(String ownerLoginId) {
        return referenceRepository.findAllByOwnerLoginIdOrderByIdxDesc(ownerLoginId).stream()
                .map(ReferenceDto.Res::from)
                .toList();
    }

    public ReferenceDto.Res getReference(String ownerLoginId, Long referenceId) {
        Reference reference = getOwnedReference(ownerLoginId, referenceId);
        return ReferenceDto.Res.from(reference);
    }

    @Transactional
    public ReferenceDto.Res createReference(String ownerLoginId, ReferenceDto.UpsertReq dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }

        String title = requireText(dto.title(), "title");
        String url = requireText(dto.url(), "url");
        String thumbnail = normalizeText(dto.thumbnail());

        Reference reference = Reference.builder()
                .ownerLoginId(ownerLoginId)
                .type(defaultIfBlank(dto.type(), DEFAULT_TYPE))
                .title(title)
                .url(url)
                .thumbnail(thumbnail.isBlank() ? url : thumbnail)
                .description(normalizeText(dto.description()))
                .tags(normalizeTags(dto.tags()))
                .channel(defaultIfBlank(dto.channel(), DEFAULT_CHANNEL))
                .objective(defaultIfBlank(dto.objective(), DEFAULT_OBJECTIVE))
                .status(defaultIfBlank(dto.status(), DEFAULT_STATUS))
                .referenceDate(defaultIfNull(dto.date()))
                .build();

        return ReferenceDto.Res.from(referenceRepository.save(reference));
    }

    @Transactional
    public ReferenceDto.Res updateReference(String ownerLoginId, Long referenceId, ReferenceDto.UpsertReq dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required.");
        }

        Reference reference = getOwnedReference(ownerLoginId, referenceId);
        String title = requireText(dto.title(), "title");
        String url = requireText(dto.url(), "url");
        String thumbnail = normalizeText(dto.thumbnail());

        reference.update(
                defaultIfBlank(dto.type(), DEFAULT_TYPE),
                title,
                url,
                thumbnail.isBlank() ? url : thumbnail,
                normalizeText(dto.description()),
                normalizeTags(dto.tags()),
                defaultIfBlank(dto.channel(), DEFAULT_CHANNEL),
                defaultIfBlank(dto.objective(), DEFAULT_OBJECTIVE),
                defaultIfBlank(dto.status(), DEFAULT_STATUS),
                defaultIfNull(dto.date())
        );

        return ReferenceDto.Res.from(reference);
    }

    @Transactional
    public void deleteReference(String ownerLoginId, Long referenceId) {
        Reference reference = getOwnedReference(ownerLoginId, referenceId);
        referenceRepository.delete(reference);
    }

    private Reference getOwnedReference(String ownerLoginId, Long referenceId) {
        return referenceRepository.findByIdxAndOwnerLoginId(referenceId, ownerLoginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "reference was not found."));
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return new ArrayList<>(tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .distinct()
                .toList());
    }

    private LocalDate defaultIfNull(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }
}
