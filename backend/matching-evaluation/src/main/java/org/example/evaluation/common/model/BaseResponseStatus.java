package org.example.evaluation.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BaseResponseStatus {
    // 1000번대 진행중
    EVLUATION_STARTED(true, 1000,"평가가 시작되었습니다."),

    // 2000번대 성공
    SUCCESS(true, 2000, "요청이 성공했습니다."),
    LIST_SUCCESS(true, 2001, "목록 가져오기에 성공했습니다."),
    SUCCESSFULY_EVALUATED(true,2005 ,"평가 생성에 성공했습니다." ),


    // 3000번대 클라이언트 입력 오류, 입력값 검증 오류
    VALIDATION_ERROR(false, 3005, "입력값을 확인해주세요."),
    EMPTY_PAYLOAD(false, 3006 ,"입력값이 비었습니다." ),

    // 5000번대 실패
    FAIL(false, 5000, "요청이 실패했습니다."),
    NO_SUCH_ELEMENT(false, 5001, "요청하신 정보가 없습니다."),
    ACCESS_DENIED(false,5002 ,"접근할 수 없습니다." ),
    SERVER_NOT_RESPONDING(false,5003 ,"서버가 응답하지 않습니다." );

    private final boolean success;
    private final int code;
    private final String message;
}
