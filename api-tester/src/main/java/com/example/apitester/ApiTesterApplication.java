package com.example.apitester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiTesterApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiTesterApplication.class, args);
    }
}
