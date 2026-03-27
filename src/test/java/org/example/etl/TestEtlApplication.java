package org.example.etl;

import org.springframework.boot.SpringApplication;

public class TestEtlApplication {

    public static void main(String[] args) {
        SpringApplication.from(Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
