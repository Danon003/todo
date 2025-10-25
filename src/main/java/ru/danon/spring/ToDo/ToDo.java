package ru.danon.spring.ToDo;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.danon.spring.ToDo.repositories.jpa")
@EnableMongoRepositories(basePackages = "ru.danon.spring.ToDo.repositories.mongo")
@EntityScan(basePackages = "ru.danon.spring.ToDo.models")
@EnableScheduling
public class ToDo {

	public static void main(String[] args) {
		SpringApplication.run(ToDo.class, args);
	}

	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}
}
