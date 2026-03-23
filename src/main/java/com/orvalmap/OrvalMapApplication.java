package com.orvalmap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class OrvalMapApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrvalMapApplication.class, args);
    }

    @Bean
    public CommandLineRunner debugConfig(Environment env) {
        return args -> {
            System.out.println("==========================================================================================");
            System.out.println("DEBUG CONFIGURATION:");
            System.out.println("DB_URL (Environment Variable): " + System.getenv("DB_URL"));
            System.out.println("spring.datasource.url (Spring Property): " + env.getProperty("spring.datasource.url"));
            System.out.println("DB_USER: " + System.getenv("DB_USER"));
            System.out.println("==========================================================================================");
        };
    }
}
