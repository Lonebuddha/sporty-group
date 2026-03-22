package com.sportygroup.assignment.eventsbetsmatching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class EventsBetsMatchingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventsBetsMatchingServiceApplication.class, args);
    }
}
