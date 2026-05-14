package org.example.backend.adcheck.analysis.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.BenchmarkRequest;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.BenchmarkResult;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.ReviewScenarioRequest;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.ReviewScenarioResult;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.StepMetric;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.SyntheticSeedRequest;
import org.example.backend.adcheck.analysis.model.AdAnalysisBenchmarkDto.SyntheticSeedResult;
import org.example.backend.adcheck.analysis.model.AdAnalysisDocument;
import org.example.backend.adcheck.analysis.model.AdAnalysisIssue;
import org.example.backend.adcheck.analysis.model.AdAnalysisKeyword;
import org.example.backend.adcheck.analysis.model.AdAnalysisPage;
import org.example.backend.adcheck.analysis.model.AdAnalysisRegion;
import org.example.backend.adcheck.analysis.repository.AdAnalysisDocumentRepository;
import org.example.backend.adcheck.analysis.repository.AdAnalysisIssueRepository;
import org.example.backend.adcheck.analysis.repository.AdAnalysisKeywordRepository;
import org.example.backend.adcheck.analysis.repository.AdAnalysisPageRepository;
import org.example.backend.adcheck.analysis.repository.AdAnalysisRegionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class AdAnalysisBenchmarkService {
    private static final List<String> KEYWORDS = List.of(
            "discount", "benefit", "guarantee", "before-after", "eco", "product",
            "medical", "finance", "event", "sns", "banner", "campaign"
    );
    private static final List<String> LABELS = List.of(
            "person", "product", "logo", "package", "medicine", "card", "graph", "food"
    );

    private final AdAnalysisDocumentRepository documentRepository;
    private final AdAnalysisPageRepository pageRepository;
    private final AdAnalysisRegionRepository regionRepository;
    private final AdAnalysisIssueRepository issueRepository;
    private final AdAnalysisKeywordRepository keywordRepository;

    @Transactional
    public SyntheticSeedResult seed(SyntheticSeedRequest request) {
        SyntheticSeedRequest req = request == null
                ? new SyntheticSeedRequest(null, null, null, null, null, null)
                : request;
        int documents = clamp(defaultInt(req.documents(), 100), 1, 2_000);
        int pagesPerDocument = clamp(defaultInt(req.pagesPerDocument(), 8), 1, 100);
        int regionsPerPage = clamp(defaultInt(req.regionsPerPage(), 32), 1, 300);
        int keywordsPerDocument = clamp(defaultInt(req.keywordsPerDocument(), 12), 1, 100);
        double issueRate = Math.max(0.0, Math.min(1.0, req.issueRate() == null ? 0.12 : req.issueRate()));
        boolean reset = Boolean.TRUE.equals(req.reset());

        long started = System.nanoTime();
        if (reset) {
            keywordRepository.deleteAllInBatch();
            issueRepository.deleteAllInBatch();
            regionRepository.deleteAllInBatch();
            pageRepository.deleteAllInBatch();
            documentRepository.deleteAllInBatch();
        }

        Random random = new Random(20260514L);
        long pageCount = 0;
        long regionCount = 0;
        long issueCount = 0;
        long keywordCount = 0;

        for (int docNo = 1; docNo <= documents; docNo++) {
            int expectedRegions = pagesPerDocument * regionsPerPage;
            int expectedIssues = Math.max(1, (int) Math.round(expectedRegions * issueRate));
            AdAnalysisDocument document = documentRepository.save(AdAnalysisDocument.builder()
                    .reviewRequestIdx((long) docNo)
                    .campaignIdx((long) ((docNo % 10) + 1))
                    .fileName("synthetic-ad-" + docNo + ".pdf")
                    .fileObjectKey("benchmark/synthetic-ad-" + docNo + ".pdf")
                    .analysisStatus(expectedIssues > 0 ? "VIOLATION" : "PASS")
                    .totalPages(pagesPerDocument)
                    .totalRegions(expectedRegions)
                    .totalIssues(expectedIssues)
                    .layoutModel("layoutparser-benchmark")
                    .ocrModel("ocr-benchmark")
                    .detectorModel("gpt-vision-benchmark")
                    .llmModel("llm-benchmark")
                    .summary("Synthetic AI analysis document for relational storage benchmark.")
                    .rawPayload(buildRawPayload(docNo, pagesPerDocument, regionsPerPage))
                    .build());

            List<AdAnalysisKeyword> keywords = new ArrayList<>();
            for (int k = 0; k < keywordsPerDocument; k++) {
                keywords.add(AdAnalysisKeyword.builder()
                        .document(document)
                        .keyword(KEYWORDS.get((docNo + k) % KEYWORDS.size()))
                        .source(k % 2 == 0 ? "OCR" : "GPT_VISION")
                        .weight(1.0 - (k * 0.01))
                        .build());
            }
            keywordRepository.saveAll(keywords);
            keywordCount += keywords.size();

            for (int pageNo = 1; pageNo <= pagesPerDocument; pageNo++) {
                AdAnalysisPage page = pageRepository.save(AdAnalysisPage.builder()
                        .document(document)
                        .pageNo(pageNo)
                        .width(1080)
                        .height(1440)
                        .thumbnailObjectKey("benchmark/thumbs/synthetic-ad-" + docNo + "-p" + pageNo + ".png")
                        .build());
                pageCount++;

                List<AdAnalysisRegion> regions = new ArrayList<>();
                List<AdAnalysisIssue> issues = new ArrayList<>();
                for (int order = 1; order <= regionsPerPage; order++) {
                    boolean textRegion = order % 3 != 0;
                    String regionKey = (textRegion ? "t-" : "i-") + docNo + "-" + pageNo + "-" + order;
                    double x = round(random.nextDouble() * 0.75);
                    double y = round(random.nextDouble() * 0.85);
                    double w = round(0.08 + random.nextDouble() * 0.18);
                    double h = round(0.03 + random.nextDouble() * 0.12);
                    String riskToken = order % 11 == 0 ? " guarantee" : "";

                    regions.add(AdAnalysisRegion.builder()
                            .page(page)
                            .regionKey(regionKey)
                            .regionType(textRegion ? "TEXT" : "IMAGE")
                            .orderIndex(order)
                            .x(x)
                            .y(y)
                            .width(w)
                            .height(h)
                            .confidence(round(0.72 + random.nextDouble() * 0.27))
                            .extractedText(textRegion ? buildRegionText(docNo, pageNo, order, riskToken) : null)
                            .labelsJson(textRegion ? null : buildLabelsJson(docNo, pageNo, order))
                            .build());

                    if (random.nextDouble() < issueRate) {
                        issues.add(AdAnalysisIssue.builder()
                                .document(document)
                                .pageNo(pageNo)
                                .regionKey(regionKey)
                                .issueType(textRegion ? "TEXT" : "IMAGE")
                                .severity(order % 5 == 0 ? "HIGH" : "MEDIUM")
                                .issueStatus("OPEN")
                                .x(x)
                                .y(y)
                                .width(w)
                                .height(h)
                                .targetText(textRegion ? "guarantee expression" : "sensitive image composition")
                                .law("benchmark-policy")
                                .reason("Synthetic issue generated to measure normalized relational lookup cost.")
                                .suggestion("Replace absolute claims with softer wording or remove risky visual context.")
                                .build());
                    }
                }
                regionRepository.saveAll(regions);
                issueRepository.saveAll(issues);
                regionCount += regions.size();
                issueCount += issues.size();
            }
        }

        return new SyntheticSeedResult(
                documents,
                pageCount,
                regionCount,
                issueCount,
                keywordCount,
                elapsedMs(started)
        );
    }

    @Transactional(readOnly = true)
    public BenchmarkResult run(BenchmarkRequest request) {
        BenchmarkRequest req = request == null ? new BenchmarkRequest(null, null, null) : request;
        int sampleSize = clamp(defaultInt(req.sampleSize(), 30), 1, 200);
        String query = req.query() == null || req.query().isBlank() ? "guarantee" : req.query().trim();
        Long documentId = req.documentId() != null
                ? req.documentId()
                : documentRepository.findAllByOrderByIdxDesc(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(AdAnalysisDocument::getIdx)
                .orElse(null);

        List<StepMetric> metrics = new ArrayList<>();
        long totalStarted = System.nanoTime();

        metrics.add(measure("list_documents_page", () ->
                (long) documentRepository.findAllByOrderByIdxDesc(PageRequest.of(0, sampleSize)).getNumberOfElements()
        ));

        if (documentId != null) {
            metrics.add(measure("load_pages", () -> (long) pageRepository.findAllByDocumentIdx(documentId).size()));
            metrics.add(measure("load_regions", () -> (long) regionRepository.findAllByDocumentIdx(documentId).size()));
            metrics.add(measure("load_issues", () -> (long) issueRepository.findAllByDocumentIdx(documentId).size()));
            metrics.add(measure("load_keywords", () -> (long) keywordRepository.findAllByDocumentIdx(documentId).size()));
        }

        metrics.add(measure("search_regions_text_or_labels", () ->
                (long) regionRepository.searchDocumentIdsByTextOrLabels(query, PageRequest.of(0, sampleSize)).getNumberOfElements()
        ));
        metrics.add(measure("search_issue_text", () ->
                (long) issueRepository.searchDocumentIdsByIssueText(query, PageRequest.of(0, sampleSize)).getNumberOfElements()
        ));
        metrics.add(measure("search_keywords", () ->
                (long) keywordRepository.searchDocumentIdsByKeyword(query, PageRequest.of(0, sampleSize)).getNumberOfElements()
        ));

        return new BenchmarkResult(
                documentId,
                query,
                metrics,
                elapsedMs(totalStarted),
                "Dev-only MariaDB normalized-storage benchmark. Use the same seed shape for a MongoDB document benchmark."
        );
    }

    @Transactional(readOnly = true)
    public ReviewScenarioResult runReviewScenario(ReviewScenarioRequest request) {
        ReviewScenarioRequest req = request == null
                ? new ReviewScenarioRequest(null, null, null, null, null)
                : request;
        int repeat = clamp(defaultInt(req.repeat(), 1), 1, 30);
        int issueLookupLimit = clamp(defaultInt(req.issueRegionLookupLimit(), 80), 0, 1_000);
        boolean includeSearch = Boolean.TRUE.equals(req.includeSearch());
        String query = req.query() == null || req.query().isBlank() ? "guarantee" : req.query().trim();
        Long documentId = req.documentId() != null
                ? req.documentId()
                : documentRepository.findAllByOrderByIdxDesc(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(AdAnalysisDocument::getIdx)
                .orElse(null);

        long totalStarted = System.nanoTime();
        List<StepMetric> metrics = new ArrayList<>();
        ScenarioCounter counter = new ScenarioCounter();

        if (documentId == null) {
            return new ReviewScenarioResult(
                    null,
                    query,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    metrics,
                    elapsedMs(totalStarted),
                    "No benchmark document exists. Seed synthetic data before running the review scenario."
            );
        }

        for (int iteration = 1; iteration <= repeat; iteration++) {
            long iterationStarted = System.nanoTime();
            documentRepository.findById(documentId).ifPresent(document -> counter.documents++);
            List<AdAnalysisPage> pages = pageRepository.findAllByDocumentIdx(documentId);
            counter.pages += pages.size();

            long issueLookupsThisIteration = 0;
            for (AdAnalysisPage page : pages) {
                List<AdAnalysisRegion> pageRegions = regionRepository.findAllByPageIdx(page.getIdx());
                List<AdAnalysisIssue> pageIssues = issueRepository.findAllByDocumentIdxAndPageNo(
                        documentId,
                        page.getPageNo()
                );
                counter.regions += pageRegions.size();
                counter.issues += pageIssues.size();

                for (AdAnalysisIssue issue : pageIssues) {
                    if (issueLookupsThisIteration >= issueLookupLimit) {
                        break;
                    }
                    List<AdAnalysisRegion> matchedRegions = regionRepository.findAllByDocumentIdxAndRegionKey(
                            documentId,
                            issue.getRegionKey()
                    );
                    counter.issueRegionLookups++;
                    counter.regionMatches += matchedRegions.size();
                    issueLookupsThisIteration++;
                }
            }

            List<AdAnalysisKeyword> keywords = keywordRepository.findAllByDocumentIdx(documentId);
            counter.keywords += keywords.size();

            if (includeSearch) {
                counter.searchHits += regionRepository
                        .searchDocumentIdsByTextOrLabels(query, PageRequest.of(0, 30))
                        .getNumberOfElements();
                counter.searchHits += issueRepository
                        .searchDocumentIdsByIssueText(query, PageRequest.of(0, 30))
                        .getNumberOfElements();
                counter.searchHits += keywordRepository
                        .searchDocumentIdsByKeyword(query, PageRequest.of(0, 30))
                        .getNumberOfElements();
            }

            metrics.add(new StepMetric(
                    "review_feedback_screen_iteration_" + iteration,
                    elapsedMs(iterationStarted),
                    pages.size()
            ));
        }

        long estimatedQuerySteps = repeat;
        estimatedQuerySteps += repeat;
        estimatedQuerySteps += counter.pages;
        estimatedQuerySteps += counter.pages;
        estimatedQuerySteps += counter.issueRegionLookups;
        estimatedQuerySteps += repeat;
        if (includeSearch) {
            estimatedQuerySteps += repeat * 3L;
        }

        return new ReviewScenarioResult(
                documentId,
                query,
                counter.pages,
                counter.regions,
                counter.issues,
                counter.keywords,
                counter.issueRegionLookups,
                estimatedQuerySteps,
                metrics,
                elapsedMs(totalStarted),
                "Simulates the author feedback screen after AI review: document, pages, page regions, page issues, issue-to-region lookup, keywords, and optional search."
        );
    }

    private StepMetric measure(String name, Supplier<Long> operation) {
        long started = System.nanoTime();
        long rows = operation.get();
        return new StepMetric(name, elapsedMs(started), rows);
    }

    private String buildRegionText(int docNo, int pageNo, int order, String riskToken) {
        return "campaign document " + docNo
                + " page " + pageNo
                + " region " + order
                + " product benefit discount event sns banner" + riskToken;
    }

    private String buildLabelsJson(int docNo, int pageNo, int order) {
        String first = LABELS.get((docNo + order) % LABELS.size());
        String second = LABELS.get((pageNo + order + 3) % LABELS.size());
        return "[\"" + first + "\",\"" + second + "\"]";
    }

    private String buildRawPayload(int docNo, int pages, int regionsPerPage) {
        StringBuilder builder = new StringBuilder(4096);
        builder.append("{\"document\":").append(docNo).append(",\"pages\":[");
        for (int page = 1; page <= pages; page++) {
            if (page > 1) {
                builder.append(',');
            }
            builder.append("{\"pageNo\":").append(page).append(",\"regions\":").append(regionsPerPage).append('}');
        }
        builder.append("],\"note\":\"Synthetic raw model payload kept to represent changing AI response shapes.\"}");
        return builder.toString();
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }

    private static class ScenarioCounter {
        private long documents;
        private long pages;
        private long regions;
        private long issues;
        private long keywords;
        private long issueRegionLookups;
        private long regionMatches;
        private long searchHits;
    }
}
