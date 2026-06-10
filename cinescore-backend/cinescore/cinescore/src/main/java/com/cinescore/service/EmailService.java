package com.cinescore.service;

import com.cinescore.exception.InvalidInputException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String ARQUIVO_TEMPLATE_RESET = "templates/email-reset-senha.html";
    private static final String PLACEHOLDER_LINK       = "{{link}}";
    private static final String NOME_REMETENTE         = "CineScore";
    private static final String ASSUNTO_RESET          = "CineScore — Redefinição de senha";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    @Value("${app.url}")
    private String appUrl;

    private String templateResetSenha;

    @PostConstruct
    public void carregarTemplates() {
        templateResetSenha = carregarTemplate(ARQUIVO_TEMPLATE_RESET);
        log.info("Templates de e-mail carregados.");
    }

    public void enviarEmailResetSenha(String destinatario, String token) {
        String link  = construirLinkReset(token);
        String corpo = templateResetSenha.replace(PLACEHOLDER_LINK, link);
        enviarEmail(destinatario, ASSUNTO_RESET, corpo);
        log.info("E-mail de reset enviado para: {}", destinatario);
    }


    private String construirLinkReset(String token) {
        return appUrl + "/reset-password?token=" + token;
    }

    private String carregarTemplate(String caminho) {
        try {
            byte[] bytes = new ClassPathResource(caminho).getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Falha ao carregar template '{}': {}", caminho, e.getMessage());
            throw new IllegalStateException("Template de e-mail não encontrado: " + caminho);
        }
    }

    private void enviarEmail(String destinatario, String assunto, String corpo) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(remetente, NOME_REMETENTE);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo, true);

            mailSender.send(mensagem);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Falha ao enviar e-mail para {}: {}", destinatario, e.getMessage());
            throw new InvalidInputException("e-mail", "não foi possível enviar. Tente novamente.");
        }
    }
}
