package com.wetalk.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TimeUtils {
    public static LocalDateTime currentTime() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
