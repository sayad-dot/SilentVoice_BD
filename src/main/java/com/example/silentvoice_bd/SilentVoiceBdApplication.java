package com.example.silentvoice_bd;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;

@SpringBootApplication
@EnableAsync
public class SilentVoiceBdApplication {

    public static void main(String[] args) {
        SpringApplication.run(SilentVoiceBdApplication.class, args);
    }

}
