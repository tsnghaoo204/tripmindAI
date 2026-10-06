package com.tripmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TripMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripMindApplication.class, args);
    }
}
