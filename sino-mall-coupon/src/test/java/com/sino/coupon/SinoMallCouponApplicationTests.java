package com.sino.coupon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// @SpringBootTest
class SinoMallCouponApplicationTests {

    @Test
    void contextLoads() {
        LocalDate now = LocalDate.now();
        LocalDate localDate1 = now.plusDays(1);
        LocalDate localDate2 = now.plusDays(2);

        System.out.println("now = " + now);
        System.out.println("localDate1 = " + localDate1);
        System.out.println("localDate2 = " + localDate2);

        LocalTime min = LocalTime.MIN;
        LocalTime max = LocalTime.MAX;
        System.out.println(min);
        System.out.println(max);

        LocalDateTime start = LocalDateTime.of(now, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(localDate2, LocalTime.MAX);
        System.out.println(start);
        System.out.println(end);
    }

}
