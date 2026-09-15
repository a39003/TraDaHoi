package com.trasua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TeaAttendanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeaAttendanceApplication.class, args);
    }
}
