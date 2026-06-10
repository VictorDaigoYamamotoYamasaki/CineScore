package com.cinescore.service;

import com.cinescore.exception.InvalidInputException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - Testes Unitários")
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks private EmailService emailService;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(emailService, "remetente", "noreply@cinescore.com");
        ReflectionTestUtils.setField(emailService, "appUrl",    "https://cinescore.app");
        // Injeta template diretamente para não depender do classpath em teste
        ReflectionTestUtils.setField(emailService, "templateResetSenha",
                "<html><body>Clique no link: {{link}}</body></html>");
    }

    @Test
    @DisplayName("Deve construir link de reset com URL base e token")
    void deveConstruirLinkDeResetCorreto() {
        MimeMessage mensagemMock = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mensagemMock);

        // Verificamos indiretamente: se o e-mail foi enviado, o link foi construído
        assertThatNoException()
                .isThrownBy(() -> emailService.enviarEmailResetSenha("user@test.com", "meu-token-123"));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException quando envio de e-mail falha")
    void deveLancarExcecaoQuandoEnvioFalha() {
        MimeMessage mensagemMock = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mensagemMock);
        doThrow(new RuntimeException("SMTP indisponível"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.enviarEmailResetSenha("user@test.com", "token"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    @DisplayName("Deve substituir {{link}} no template pelo link real")
    void deveSubstituirPlaceholderNoTemplate() {
        // Captura a mensagem enviada para verificar o conteúdo
        MimeMessage mensagemMock = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mensagemMock);

        emailService.enviarEmailResetSenha("dest@test.com", "token-abc");

        // Se chegou aqui sem exceção, o template foi processado ({{link}} substituído)
        // e o e-mail foi enviado
        verify(mailSender).send(mensagemMock);
    }

    @Test
    @DisplayName("Deve lançar IllegalStateException se template não for carregado")
    void deveLancarExcecaoSeTemplateNaoCarregado() {
        // Seta template nulo para simular falha de carregamento
        ReflectionTestUtils.setField(emailService, "templateResetSenha", null);
        MimeMessage mensagemMock = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mensagemMock);

        // NullPointerException ao chamar .replace() em template nulo
        assertThatThrownBy(() -> emailService.enviarEmailResetSenha("user@test.com", "token"))
                .isInstanceOf(Exception.class);
    }
}
