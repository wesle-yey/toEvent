package com.wesleyey.toEvent.services;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Limite global de e-mails enviados por dia (UTC), abaixo da cota do provedor (Brevo gratuito: 300/dia).
 * O contador fica no MongoDB para sobreviver a reinícios da API e valer entre instâncias.
 */
@Service
public class EmailQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(EmailQuotaService.class);
    static final String COLLECTION = "email_daily_quota";

    private final MongoTemplate mongoTemplate;
    private final int dailyLimit;
    private final Clock clock;

    @Autowired
    public EmailQuotaService(MongoTemplate mongoTemplate,
            @Value("${mail.daily-limit:250}") int dailyLimit) {
        this(mongoTemplate, dailyLimit, Clock.systemUTC());
    }

    EmailQuotaService(MongoTemplate mongoTemplate, int dailyLimit, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.dailyLimit = dailyLimit;
        this.clock = clock;
    }

    /**
     * Reserva um envio no dia atual. Retorna false quando o limite diário já foi atingido.
     * O incremento é atômico (findAndModify com upsert), então pedidos simultâneos não furam o limite.
     */
    public boolean tryReserveSend() {
        String today = LocalDate.now(clock.withZone(ZoneOffset.UTC)).toString();
        Document counter = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(today)),
                new Update().inc("count", 1),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                Document.class,
                COLLECTION);

        int count = counter == null ? 1 : ((Number) counter.get("count")).intValue();
        if (count > dailyLimit) {
            logger.error("Limite diário de e-mails atingido ({}/{}). Envio bloqueado.", count, dailyLimit);
            return false;
        }
        if (count >= dailyLimit * 0.8) {
            logger.warn("E-mails enviados hoje: {}/{} (acima de 80% do limite diário).", count, dailyLimit);
        }
        return true;
    }
}
