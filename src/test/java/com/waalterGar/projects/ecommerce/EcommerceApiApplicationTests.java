package com.waalterGar.projects.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE) // use the container DataSource, not an in-memory one
class EcommerceApiApplicationTests {

	@Container
	@ServiceConnection // Boot 3.1+ will auto-map spring.datasource.* from this container
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.37");

	@Test
	void contextLoads() {
		// boots the whole Spring context against the containerized MySQL
	}
}