package com.wesleyey.toEvent;

import com.wesleyey.toEvent.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.index.Indexed;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIndexTest {

    @Test
    void emailFieldDeclaresUniqueMongoIndex() throws NoSuchFieldException {
        Indexed index = User.class.getDeclaredField("email").getAnnotation(Indexed.class);

        assertNotNull(index);
        assertTrue(index.unique());
    }
}
