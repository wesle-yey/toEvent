package com.wesleyey.toEvent.services;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailQuotaServiceTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    // 23h30 UTC = 20h30 em Brasília: o contador do dia segue o dia UTC
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T23:30:00Z"), ZoneOffset.UTC);
    private final EmailQuotaService service = new EmailQuotaService(mongoTemplate, 3, clock);

    @Test
    void allowsSendsUpToTheDailyLimit() {
        counterReturns(3);
        assertTrue(service.tryReserveSend());
    }

    @Test
    void blocksSendsAboveTheDailyLimit() {
        counterReturns(4);
        assertFalse(service.tryReserveSend());
    }

    @Test
    void usesOneCounterDocumentPerUtcDay() {
        counterReturns(1);
        service.tryReserveSend();

        var queryCaptor = org.mockito.ArgumentCaptor.forClass(Query.class);
        org.mockito.Mockito.verify(mongoTemplate).findAndModify(queryCaptor.capture(), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Document.class), eq(EmailQuotaService.COLLECTION));
        assertEquals("2026-11-24", queryCaptor.getValue().getQueryObject().get("_id"));
    }

    private void counterReturns(int count) {
        when(mongoTemplate.findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Document.class), eq(EmailQuotaService.COLLECTION)))
                .thenReturn(new Document("_id", "2026-11-24").append("count", count));
    }
}
