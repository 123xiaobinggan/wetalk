package com.wetalk.utils;

import org.springframework.stereotype.Component;

import com.wetalk.utils.SnowflakeIdWorker;

@Component
public class IdGenerator {

    private final SnowflakeIdWorker worker;

    public IdGenerator() {
        this.worker = new SnowflakeIdWorker(1L, 1L);
    }

    public Long nextId() {
        return worker.nextId();
    }
}