package org.example.evaluation.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationCollectRequestedEvent {

    @JsonAlias({"uuid", "sessionId"})
    private String sessionId;

    @JsonAlias({"campaignIdx", "campaignPublicId", "publicId"})
    private String campaignPublicId;

    private Long benefitIdx;
    private String goal;
    private String title;
    private String partner;
    private String assetDescription;
    private String offer;
    private String target;
    private String category;
    private Integer overallScore;
    private List<String> improvementDirections;
    private JsonNode evaluations;

    public String key() {
        return sessionId;
    }
}
