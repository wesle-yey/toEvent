package com.wesleyey.toEvent;

import com.wesleyey.toEvent.DTOs.EventDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EventDtoValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsEventNameAndDescriptionAtMaximumLengths() {
        EventDTO event = new EventDTO("a".repeat(120), "b".repeat(500), LocalDate.now());

        assertTrue(validator.validate(event).isEmpty());
    }

    @Test
    void rejectsEventNameAndDescriptionOverMaximumLengths() {
        EventDTO event = new EventDTO("a".repeat(121), "b".repeat(501), LocalDate.now());

        assertFalse(validator.validate(event).isEmpty());
    }
}
