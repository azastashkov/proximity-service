package com.proximityservice.lbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("com.proximityservice.common.model")
public class LbsApplication {
    public static void main(String[] args) {
        SpringApplication.run(LbsApplication.class, args);
    }
}
