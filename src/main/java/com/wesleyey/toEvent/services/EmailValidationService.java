package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.exception.DisposableEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

@Service
public class EmailValidationService {
    private static final Logger log = LoggerFactory.getLogger(EmailValidationService.class);

    private final ResourceLoader resourceLoader;
    private final boolean checkMx;
    private final Set<String> disposableDomains = Collections.synchronizedSet(new HashSet<>());

    public EmailValidationService(
            ResourceLoader resourceLoader,
            @Value("${mail.validation.check-mx:false}") boolean checkMx) {
        this.resourceLoader = resourceLoader;
        this.checkMx = checkMx;
    }

    @PostConstruct
    public void loadDisposableDomains() {
        try {
            Resource resource = resourceLoader.getResource("classpath:disposable-domains.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String domain = line.trim().toLowerCase();
                        if (!domain.isEmpty() && !domain.startsWith("#")) {
                            disposableDomains.add(domain);
                        }
                    }
                }
                log.info("Carregados {} domínios de e-mail descartáveis para bloqueio.", disposableDomains.size());
            } else {
                log.warn("Arquivo disposable-domains.txt não encontrado no classpath.");
            }
        } catch (Exception e) {
            log.error("Erro ao carregar lista de domínios descartáveis: {}", e.getMessage());
        }
    }

    public void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new DisposableEmailException("Formato de e-mail inválido.");
        }

        String domain = email.substring(email.lastIndexOf('@') + 1).trim().toLowerCase();
        if (domain.isEmpty()) {
            throw new DisposableEmailException("Domínio de e-mail inválido.");
        }

        if (isDisposable(domain)) {
            log.warn("Tentativa de cadastro com e-mail temporário bloqueada: {}", email);
            throw new DisposableEmailException("O uso de e-mails temporários ou descartáveis não é permitido. Por favor, use um e-mail válido.");
        }

        if (checkMx && !hasMxRecord(domain)) {
            log.warn("Domínio sem registro MX detectado: {}", domain);
            throw new DisposableEmailException("O domínio do e-mail informado não possui um servidor de e-mail válido.");
        }
    }

    public boolean isDisposable(String domain) {
        if (domain == null) return false;
        String cleanDomain = domain.trim().toLowerCase();
        return disposableDomains.contains(cleanDomain);
    }

    public boolean hasMxRecord(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns://");
        env.put("com.sun.jndi.dns.timeout.initial", "2000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");

        try {
            DirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});
            Attribute attr = attrs.get("MX");

            if (attr != null && attr.size() > 0) {
                return true;
            }

            // Fallback: alguns domínios usam registro A direto como fallback para e-mail
            Attributes aAttrs = ictx.getAttributes(domain, new String[]{"A"});
            Attribute aAttr = aAttrs.get("A");
            return aAttr != null && aAttr.size() > 0;
        } catch (Exception e) {
            log.debug("Falha na checagem MX de {}: {}", domain, e.getMessage());
            return false;
        }
    }

    public Set<String> getDisposableDomains() {
        return Collections.unmodifiableSet(disposableDomains);
    }
}
