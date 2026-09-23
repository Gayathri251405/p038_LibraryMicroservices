package com.example.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LibraryMicroservicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                LibraryMicroservicesApplication.class, args);
    }
}