package com.fpoly.marcusstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MarcusStoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarcusStoreApiApplication.class, args);
	}
}
