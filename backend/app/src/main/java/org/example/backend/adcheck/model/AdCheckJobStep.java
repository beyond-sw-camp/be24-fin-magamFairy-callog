package org.example.backend.adcheck.model;

import lombok.Getter;

@Getter
public enum AdCheckJobStep {
    QUEUED(0, "대기 중", "앞선 검수 작업을 기다리고 있습니다.", 5),
    DATA_EXTRACTION(1, "데이터 추출 중", "분석 서버에서 파일 데이터를 추출하고 있습니다.", 20),
    DATA_ANALYSIS(2, "데이터 분석 중", "AI 텍스트 분석을 진행하고 있습니다.", 35),
    RESULT_BUILDING(3, "결과 도출 중", "검수 결과를 정리하고 있습니다.", 95),
    COMPLETED(4, "검수 완료", "검수가 완료되었습니다.", 100),
    FAILED(5, "실패", "검수 중 오류가 발생했습니다.", 0),

    REQUEST_RECEIVED(0, "대기 중", "앞선 검수 작업을 기다리고 있습니다.", 5),
    STARTED(1, "데이터 추출 중", "분석 서버에서 파일 데이터를 추출하고 있습니다.", 20),
    TEXT_DETECTION(1, "데이터 추출 중", "분석 서버에서 파일 데이터를 추출하고 있습니다.", 20),
    LAYOUT_ANALYSIS(1, "데이터 추출 중", "분석 서버에서 파일 데이터를 추출하고 있습니다.", 20),
    OCR(1, "데이터 추출 중", "분석 서버에서 파일 데이터를 추출하고 있습니다.", 20),
    N8N_ANALYSIS(2, "데이터 분석 중", "AI 텍스트 분석을 진행하고 있습니다.", 35);

    private final int order;
    private final String label;
    private final String message;
    private final int progressPercent;

    AdCheckJobStep(int order, String label, String message, int progressPercent) {
        this.order = order;
        this.label = label;
        this.message = message;
        this.progressPercent = progressPercent;
    }
}
