package com.mahnflow.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

// makes sure the same request is not done twice.
// the client sends an "Idempotency-Key" header. we keep the first answer
// in redis, so if the same key comes again (network retry) we just give
// back the old answer instead of creating a second invoice.
//
// i got the whole idea from a stack overflow answer about stripe's
// idempotency keys. it's not perfect yet (see the TODO in runOnce), but it
// already stops the "double click creates two invoices" problem.
@Component
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "idem:";

    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    public <T> T runOnce(String key, Class<T> type, Supplier<T> action) {
        // no key given: just run normally, no de-dup.
        if (key == null || key.isBlank()) {
            return action.get();
        }

        String redisKey = PREFIX + key;
        String saved = redis.opsForValue().get(redisKey);
        if (saved != null) {
            log.info("idempotency hit for key {}", key);
            return readJson(saved, type);
        }

        T result = action.get();
        // TODO: this is the weak spot. i run the action FIRST and write the key after,
        //       so if two identical requests arrive at the exact same millisecond both
        //       can run before the key exists. setIfAbsent at least stops the second
        //       WRITE from overwriting, but the second action still ran. das ist noch
        //       ein bisschen hacky. proper fix = reserve a "pending" marker before the
        //       action and make the second request wait. on my todo list.
        redis.opsForValue().setIfAbsent(redisKey, writeJson(result), TTL);
        return result;
    }

    private <T> T readJson(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("could not read saved response", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("could not save response", e);
        }
    }
}
