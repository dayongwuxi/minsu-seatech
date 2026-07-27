package com.seatech.minsu.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/** 单号生成器：prefix + yyyyMMdd + 4位随机数字，如 DD202607070042 */
public final class NoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String gen(String prefix) {
        return prefix + LocalDate.now().format(FMT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private NoGenerator() {
    }
}
