package com.example.freshkitchen;

import com.example.freshkitchen.support.PostgreSqlTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"ai.server.base-url=http://localhost:8000",
		"ai.server.token=test-token",
		"oauth.google.client-id=test-client-id",
	"oauth.kakao.client-id=test-client-id"
})
class FreshkitchenApplicationTests extends PostgreSqlTestContainerSupport {

	@Test
	void contextLoads() {
	}

}
