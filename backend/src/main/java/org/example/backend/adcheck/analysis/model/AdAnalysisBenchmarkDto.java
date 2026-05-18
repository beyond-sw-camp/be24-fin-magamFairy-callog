package org.example.backend.adcheck.analysis.model;

import java.util.List;

public class AdAnalysisBenchmarkDto {
    public record SyntheticSeedRequest(
            Integer documents,
            Integer pagesPerDocument,
            Integer regionsPerPage,
            Integer keywordsPerDocument,
            Double issueRate,
            Boolean reset
    ) {
    }

    public record SyntheticSeedResult(
            long documents,
            long pages,
            long regions,
            long issues,
            long keywords,
            long elapsedMs
    ) {
    }

    public record BenchmarkRequest(
            Long documentId,
            String query,
            Integer sampleSize
    ) {
    }

    public record ReviewScenarioRequest(
            Long documentId,
            String query,
            Integer repeat,
            Integer issueRegionLookupLimit,
            Boolean includeSearch
    ) {
    }

    public record StepMetric(
            String name,
            long elapsedMs,
            long rowCount
    ) {
    }

    public record BenchmarkResult(
            Long documentId,
            String query,
            List<StepMetric> metrics,
            long totalElapsedMs,
            String note
    ) {
    }

    public record ReviewScenarioResult(
            Long documentId,
            String query,
            long pages,
            long regions,
            long issues,
            long keywords,
            long issueRegionLookups,
            long estimatedQuerySteps,
            List<StepMetric> metrics,
            long totalElapsedMs,
            String note
    ) {
    }
}
