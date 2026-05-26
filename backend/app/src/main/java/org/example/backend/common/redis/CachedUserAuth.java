package org.example.backend.common.redis;

import org.example.backend.user.model.User;
import org.example.backend.user.model.UserAccountStatus;

/**
 * UserAuthCache 전용 경량 DTO.
 * <p>
 * User 엔티티 전체(password 포함)를 Redis에 직렬화하는 대신
 * JwtFilter 에서 실제로 사용하는 필드만 추출해 저장한다.
 * <ul>
 *   <li>password 가 Redis에 노출되지 않음 (보안 개선)</li>
 *   <li>직렬화 크기 축소 (Organization 연관 포함 엔티티 그래프 제외)</li>
 *   <li>엔티티 필드 추가/변경 시 역직렬화 실패 위험 대폭 감소</li>
 * </ul>
 * </p>
 */
public record CachedUserAuth(
        Long idx,
        String name,
        Boolean enable,
        UserAccountStatus accountStatus
) {
    public static CachedUserAuth from(User user) {
        return new CachedUserAuth(
                user.getIdx(),
                user.getName(),
                user.getEnable(),
                user.getAccountStatus() == null ? UserAccountStatus.ACTIVE : user.getAccountStatus()
        );
    }
}
