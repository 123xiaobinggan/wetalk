package com.wetalk.utils;

public class SnowflakeIdWorker {

    // 起始时间戳（可以自定义）
    private final Long twepoch = 1672531200000L; // 2023-01-01

    // 各部分位数
    private final Long workerIdBits = 5L;
    private final Long datacenterIdBits = 5L;
    private final Long sequenceBits = 12L;

    // 最大值
    private final Long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final Long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    // 左移
    private final Long workerIdShift = sequenceBits;
    private final Long datacenterIdShift = sequenceBits + workerIdBits;
    private final Long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    private final Long sequenceMask = -1L ^ (-1L << sequenceBits);

    private Long workerId;
    private Long datacenterId;
    private Long sequence = 0L;
    private Long lastTimestamp = -1L;

    public SnowflakeIdWorker(Long workerId, Long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("workerId非法");
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId非法");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized Long nextId() {
        Long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private Long tilNextMillis(Long lastTimestamp) {
        Long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private Long timeGen() {
        return System.currentTimeMillis();
    }
}