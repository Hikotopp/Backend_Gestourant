package com.gestourant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GestourantApplication {
    public static void main(String[] args) { SpringApplication.run(GestourantApplication.class, args); }
}
