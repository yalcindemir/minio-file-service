package com.fileservice.minioservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "MinIO File Service API",
        version = "1.0",
        description = "File management microservice with MinIO integration",
        contact = @Contact(name = "File Service Team")
    )
)
public class MinioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinioServiceApplication.class, args);
    }
}
