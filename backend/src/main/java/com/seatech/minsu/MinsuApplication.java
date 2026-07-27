package com.seatech.minsu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.seatech.minsu.mapper")
public class MinsuApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinsuApplication.class, args);
    }
}
