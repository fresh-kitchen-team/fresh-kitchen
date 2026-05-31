package com.example.freshkitchen;

import com.example.freshkitchen.support.RedisTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"ai.server.base-url=http://localhost:8000",
		"ai.server.token=test-token",
		"oauth.google.client-id=test-client-id",
		"oauth.kakao.client-id=test-kakao-client-id",
		"oauth.kakao.app-key=test-kakao-app-key"
})
class FreshkitchenApplicationTests extends RedisTestContainerSupport {

	@Test
	void contextLoads() {
	}

}
