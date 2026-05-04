package org.example.backend.reference.model;

import lombok.Builder;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class ReferenceDto {
    public record UpsertReq(
            String type,
            String title,
            String url,
            String thumbnail,
            String description,
            List<String> tags,
            String channel,
            String objective,
            String status,
            LocalDate date
    ) {
    }

    @Builder
    public record Res(
            String id,
            Long idx,
            String type,
            String title,
            String url,
            String thumbnail,
            String description,
            List<String> tags,
            String channel,
            String objective,
            String status,
            LocalDate date,
            Date createdAt,
            Date updatedAt
    ) {
        public static Res from(Reference entity) {
            return Res.builder()
                    .id(String.valueOf(entity.getIdx()))
                    .idx(entity.getIdx())
                    .type(entity.getType())
                    .title(entity.getTitle())
                    .url(entity.getUrl())
                    .thumbnail(entity.getThumbnail())
                    .description(entity.getDescription())
                    .tags(List.copyOf(entity.getTags()))
                    .channel(entity.getChannel())
                    .objective(entity.getObjective())
                    .status(entity.getStatus())
                    .date(entity.getReferenceDate())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
}
