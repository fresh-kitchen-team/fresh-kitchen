package com.example.freshkitchen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FreshkitchenApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreshkitchenApplication.class, args);
	}

}
