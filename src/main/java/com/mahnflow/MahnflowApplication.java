package com.mahnflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// app start point. @EnableScheduling turns on the outbox worker and dunning job.
@SpringBootApplication
@EnableScheduling
public class MahnflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MahnflowApplication.class, args);
    }
}
