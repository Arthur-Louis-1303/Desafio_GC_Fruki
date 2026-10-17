package com.fruki.recomenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FrukiApplication {
    public static void main(String[] args) {
        SpringApplication.run(FrukiApplication.class, args);
    }
}
