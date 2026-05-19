package com.example.adcheck.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AdCheckDto {

    public record Req(
            String copy
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Res(
            String status,
            String law,
            @JsonProperty("violation_text")
            String violationText,
            String reason,
            String suggestion
    ) {
    }
}
