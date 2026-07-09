package com.medicalbilling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicalBillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalBillingApplication.class, args);
    }
}
