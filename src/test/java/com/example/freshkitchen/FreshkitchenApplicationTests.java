package com.example.freshkitchen;

import com.example.freshkitchen.support.RedisTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"ai.server.base-url=http://localhost:8000",
		"ai.server.token=test-token",
		"oauth.google.client-id=test-client-id"
})
class FreshkitchenApplicationTests extends RedisTestContainerSupport {

	@Test
	void contextLoads() {
	}

}
