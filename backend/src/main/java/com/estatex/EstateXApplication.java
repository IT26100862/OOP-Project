package com.estatex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EstateX Real Estate Property Listings Portal
 * Backend Application Entrypoint (Spring Boot & SQL Database)
 */
@SpringBootApplication
public class EstateXApplication {

    public static void main(String[] args) {
        SpringApplication.run(EstateXApplication.class, args);
        System.out.println("=========================================================");
        System.out.println("  EstateX Spring Boot Backend Started Successfully!");
        System.out.println("  Port: 8081");
        System.out.println("  API Base URL: http://localhost:8081/api");
        System.out.println("  H2 SQL Console: http://localhost:8081/h2-console");
        System.out.println("=========================================================");
    }
}
