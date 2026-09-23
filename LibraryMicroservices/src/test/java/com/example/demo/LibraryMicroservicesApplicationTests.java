package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import com.example.library.LibraryMicroservicesApplication;

@SpringBootTest(classes = LibraryMicroservicesApplication.class)
class LibraryMicroservicesApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	void shouldHaveEurekaClientConfiguration() {
		assertThat(environment.getProperty("eureka.client.service-url.defaultZone")).isEqualTo("http://localhost:8761/eureka/");
		assertThat(environment.getProperty("eureka.client.register-with-eureka")).isEqualTo("true");
	}

}
