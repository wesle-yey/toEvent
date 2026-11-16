package com.wesleyey.toEvent.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final String resendApiKey;
    private final String fromEmail;
    private final String frontendUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmailService(
            @Value("${resend.api.key:}") String resendApiKey,
            @Value("${resend.from.email:toEvent <onboarding@resend.dev>}") String fromEmail,
            @Value("${app.frontend.url:https://to-event.vercel.app}") String frontendUrl) {
        this.resendApiKey = resendApiKey != null ? resendApiKey.trim() : "";
        this.fromEmail = fromEmail;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendVerificationEmail(String toEmail, String userName, String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;

        if (resendApiKey.isEmpty()) {
            logger.info("==================================================================");
            logger.info("[DEV/TEST] RESEND_API_KEY não configurada. Simulação de envio de e-mail:");
            logger.info("Destinatário: {}", toEmail);
            logger.info("Link de ativação: {}", verificationUrl);
            logger.info("==================================================================");
            return;
        }

        try {
            String htmlContent = buildVerificationEmailHtml(userName, verificationUrl);

            Map<String, Object> payload = Map.of(
                    "from", fromEmail,
                    "to", List.of(toEmail),
                    "subject", "Confirme seu e-mail - toEvent",
                    "html", htmlContent
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("E-mail de verificação enviado com sucesso para {} via Resend. Status: {}", toEmail, response.statusCode());
            } else {
                logger.error("Falha ao enviar e-mail via Resend para {}. Status: {}, Resposta: {}", toEmail, response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            logger.error("Erro inesperado ao enviar e-mail para {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    private String buildVerificationEmailHtml(String userName, String verificationUrl) {
        String name = (userName != null && !userName.isBlank()) ? userName : "Usuário";
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <title>Confirme seu e-mail</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b;">
                    <div style="max-width: 560px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                        <div style="margin-bottom: 24px; text-align: center;">
                            <span style="display: inline-block; background-color: #4f46e5; color: #ffffff; font-weight: bold; font-size: 20px; width: 40px; height: 40px; line-height: 40px; border-radius: 8px; text-align: center;">t</span>
                            <h2 style="margin: 12px 0 0 0; color: #0f172a; font-size: 22px;">toEvent</h2>
                        </div>
                        <h3 style="color: #0f172a; margin-top: 0;">Olá, %s!</h3>
                        <p style="font-size: 15px; line-height: 1.6; color: #475569;">
                            Obrigado por criar sua conta no <strong>toEvent</strong>. Para começar a organizar seus eventos e ter acesso ao sistema, confirme seu endereço de e-mail clicando no botão abaixo:
                        </p>
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="background-color: #4f46e5; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 8px; font-weight: 600; font-size: 15px; display: inline-block; box-shadow: 0 2px 4px rgba(79, 70, 229, 0.2);">
                                Confirmar meu e-mail
                            </a>
                        </div>
                        <p style="font-size: 13px; line-height: 1.5; color: #64748b;">
                            Se o botão não funcionar, copie e cole o seguinte link no seu navegador:<br>
                            <a href="%s" style="color: #4f46e5; word-break: break-all;">%s</a>
                        </p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;">
                        <p style="font-size: 12px; color: #94a3b8; text-align: center; margin: 0;">
                            Este link é válido por 24 horas. Se você não solicitou este cadastro no toEvent, pode ignorar esta mensagem com segurança.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(name, verificationUrl, verificationUrl, verificationUrl);
    }
}
