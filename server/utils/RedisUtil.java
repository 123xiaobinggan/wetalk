package com.wetalk.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, Long time) {
        redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
    }

    public Object get(String key) {
        if (redisTemplate.hasKey(key)) {
            return redisTemplate.opsForValue().get(key);
        }
        return null;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Set<Object> getSet(String key) {
        if (redisTemplate.hasKey(key)) {
            return redisTemplate.opsForSet().members(key);
        }
        return null;
    }

    public void setAdd(String key, Long time, Collection<?> values) {
        redisTemplate.opsForSet().add(key, values.toArray());
        redisTemplate.expire(key, time, TimeUnit.SECONDS);
    }

    public Long setRemove(String key, Long time, Object... values) {
        Long result = redisTemplate.opsForSet().remove(key, values);
        redisTemplate.expire(key, time, TimeUnit.SECONDS);
        return result;
    }
}
