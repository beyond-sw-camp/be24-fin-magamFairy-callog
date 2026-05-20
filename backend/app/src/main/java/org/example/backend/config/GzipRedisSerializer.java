package org.example.backend.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP 압축 래퍼 직렬화기.
 *
 * <p>기존 JSON 직렬화기(delegate)가 만든 바이트를 한 번 더 GZIP 으로 압축하여 Redis 에 저장한다.
 * 대시보드 통합 응답(~17KB)처럼 큰 값에서 메모리/네트워크 전송량을 크게 줄인다 (대략 70% 감소).</p>
 *
 * <p>트레이드오프: 압축/해제 CPU 비용이 약간 추가된다. 작은 값(1KB 이하)에는 효과가 미미하므로
 * 큰 응답 위주인 대시보드 캐시에 적합하다.</p>
 *
 * <p>⚠️ 적용 시점 주의: 이미 비압축으로 저장된 기존 캐시는 해제(역직렬화)에서 실패한다.
 * 배포 직후 한 번 FLUSH 하거나, 짧은 TTL 의 자연 만료를 기다린다.</p>
 */
public class GzipRedisSerializer implements RedisSerializer<Object> {

    private final RedisSerializer<Object> delegate;

    public GzipRedisSerializer(RedisSerializer<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        byte[] json = delegate.serialize(value);
        if (json == null) {
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(json);
            gzip.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Redis GZIP 압축 실패", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return delegate.deserialize(gzip.readAllBytes());
        } catch (IOException e) {
            throw new SerializationException("Redis GZIP 해제 실패", e);
        }
    }
}
