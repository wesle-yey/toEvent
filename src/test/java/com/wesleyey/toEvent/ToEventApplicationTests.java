package com.wesleyey.toEvent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest(properties = {
        "MONGODB_URI=mongodb://localhost:27017/toevent-test",
        "MONGODB_DATABASE=toevent-test",
        "AUTH_TOKEN_SECRET=test-only-secret",
        "spring.data.mongodb.auto-index-creation=false"
})
class ToEventApplicationTests {
	@Test
	void contextLoads() {
	}
}
